import argparse
import csv
import struct
import zlib
import requests

# Global Constants matching our new format
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
    """Parses the new length-prefixed Zlib format from a stream and yields blocks."""

    def __init__(self, raw_stream):
        self.raw_stream = raw_stream
        self.file_count = 0

    def parse(self):
        """Generates blocks one by one: yields (x, y, z, material)."""
        while True:
            # 1. Read the Strategy A Strategy Header (4-byte unsigned int size prefix)
            size_header = self.raw_stream.read(4)
            if not size_header:
                print("\nReached end of stream.")
                break

            chunk_size = struct.unpack(">I", size_header)[0]

            self.file_count += 1
            print(
                f"Parsing file #{self.file_count}... (Chunk size: {chunk_size} bytes)",
                end="\r",
            )

            # 2. Read exactly chunk_size bytes to guarantee we don't bleed into the next chunk
            compressed_payload = self.raw_stream.read(chunk_size)
            if len(compressed_payload) < chunk_size:
                raise ValueError(
                    "Stream cut short: Received fewer bytes than chunk_size specified."
                )

            # 3. Decompress the entire file envelope using Zlib
            decompressed_data = zlib.decompress(compressed_payload)

            # Using byte offsets to parse values out of the uncompressed memory block
            offset = 0

            # 4. Parse Header Magic & Version
            magic = decompressed_data[offset : offset + 4]
            offset += 4
            if magic != MAGIC_NUMBER:
                raise ValueError(f"Invalid magic number: {magic}.")

            version = struct.unpack_from(">B", decompressed_data, offset)[0]
            offset += 1
            if version != FORMAT_VERSION:
                raise ValueError(f"Unsupported format version: {version}")

            # 5. Parse Structural Geometry Bounds
            origin_x, origin_y, origin_z, size_x, size_y, size_z, palette_length = (
                struct.unpack_from(">7i", decompressed_data, offset)
            )
            offset += 28

            # 6. Parse Palette Table
            palette = {}
            for _ in range(palette_length):
                str_len = struct.unpack_from(">H", decompressed_data, offset)[
                    0
                ]  # Unsigned short matching java writeShort
                offset += 2

                material_name = decompressed_data[offset : offset + str_len].decode(
                    "utf-8"
                )
                offset += str_len

                palette[len(palette)] = material_name

            # 7. Unpack Block Grid Data
            # Total size is calculated explicitly from bounding values
            total_blocks = size_x * size_y * size_z
            block_ids = struct.unpack_from(
                f">{total_blocks}h", decompressed_data, offset
            )
            # offset += total_blocks * 2 (not strictly needed since we are at the end)

            # 8. Process 3D Grid coordinates (Matches Java's flattening layer loop)
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
