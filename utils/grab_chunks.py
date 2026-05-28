import argparse
import csv
import gzip
import io
import struct
import requests

MAGIC_NUMBER = b"BLKS"
FORMAT_VERSION = 1


class BlockStreamClient:
    """Handles the HTTP streaming connection and authentication."""

    def __init__(self, url, user, password, query_params):
        self.url = url
        self.auth = (user, password)
        self.query_params = query_params
        self.headers = {"X-Payload-Type": "bin"}

    def connect(self):
        """Connects to the stream and returns the stream."""
        print(f"Connecting to stream: {self.url}")
        response = requests.get(
            self.url,
            auth=self.auth,
            headers=self.headers,
            params=self.query_params,
            stream=True,
        )
        response.raise_for_status()
        return response.raw


class BlockBinaryParser:
    """Parses the binary format from a stream and yields individual blocks."""

    def __init__(self, raw_stream):
        self.raw_stream = raw_stream
        self.file_count = 0

    def parse(self):
        """Generates blocks one by one: yields (x, y, z, material)."""
        while True:
            magic = self.raw_stream.read(4)
            if not magic:
                print("\nReached end of stream.")
                break

            if magic != MAGIC_NUMBER:
                raise ValueError(f"Invalid magic number: {magic}.")

            self.file_count += 1
            print(f"Parsing file #{self.file_count}...", end="\r")

            # Parse metadata
            version = struct.unpack(">B", self.raw_stream.read(1))[0]
            if version != FORMAT_VERSION:
                raise ValueError(f"Unsupported format version: {version}")

            metadata_bytes = self.raw_stream.read(28)
            origin_x, origin_y, origin_z, size_x, size_y, size_z, palette_length = (
                struct.unpack(">7i", metadata_bytes)
            )

            # Parse palette
            palette = {}
            for _ in range(palette_length):
                str_len = struct.unpack(">h", self.raw_stream.read(2))[0]
                material_name = self.raw_stream.read(str_len).decode("utf-8")
                palette[len(palette)] = material_name

            # Decompress block payloads
            compressed_size = struct.unpack(">i", self.raw_stream.read(4))[0]
            gzip_payload = self.raw_stream.read(compressed_size)

            with gzip.GzipFile(fileobj=io.BytesIO(gzip_payload), mode="rb") as f:
                decompressed_block_data = f.read()

            num_blocks = len(decompressed_block_data) // 2
            block_ids = struct.unpack(f">{num_blocks}h", decompressed_block_data)

            # Process 3D Grid coordinates
            idx = 0
            for y in range(size_y):
                world_y = origin_y + y
                for z in range(size_z):
                    world_z = origin_z + z
                    for x in range(size_x):
                        block_id = block_ids[idx]
                        idx += 1

                        if block_id == -1:  # Skip empty blocks
                            continue

                        material = palette.get(block_id)
                        if not material:
                            raise ValueError(f"Invalid block id {block_id}")

                        yield {
                            "x": origin_x + x,
                            "y": world_y,
                            "z": world_z,
                            "material": material,
                        }


class CSVBlockWriter:
    """Manages writing parsed block data out to a CSV file."""

    def __init__(self, file_path):
        self.file_path = file_path
        self.fieldnames = ["x", "y", "z", "material"]

    def write_blocks(self, block_generator):
        """Consumes a block generator and writes the contents to CSV."""
        with open(self.file_path, mode="w", newline="", encoding="utf-8") as csv_file:
            writer = csv.DictWriter(csv_file, fieldnames=self.fieldnames)
            writer.writeheader()

            for block in block_generator:
                writer.writerow(block)


def main(args):
    query_params = {
        "world_name": args.world_name,
        "minx": args.minx,
        "minz": args.minz,
        "sizex": args.sizex,
        "sizez": args.sizez,
    }

    try:
        client = BlockStreamClient(args.url, args.user, args.password, query_params)
        raw_stream = client.connect()

        parser = BlockBinaryParser(raw_stream)
        block_generator = parser.parse()

        writer = CSVBlockWriter(args.output)
        writer.write_blocks(block_generator)

    except Exception as e:
        print(f"\nAn error occurred during streaming: {e}")
        raise


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Stream binary block data to CSV.")
    parser.add_argument("--url", required=True, help="Server stream URL")
    parser.add_argument("--user", required=True, help="Basic Auth Username")
    parser.add_argument("--password", required=True, help="Basic Auth Password")
    parser.add_argument(
        "--output",
        default="output.csv",
        help="Output CSV path (default: output.csv)",
    )
    parser.add_argument(
        "--world_name", required=True, type=str, help="Name of the world"
    )
    parser.add_argument("--minx", required=True, type=int, help="Minimum X coordinate")
    parser.add_argument("--minz", required=True, type=int, help="Minimum Z coordinate")
    parser.add_argument("--sizex", required=True, type=int, help="Width size X")
    parser.add_argument("--sizez", required=True, type=int, help="Depth size Z")

    args = parser.parse_args()

    try:
        main(args)
    except KeyboardInterrupt:
        print("\nProcess interrupted by user. Exiting safely.")
