import argparse
import os
import sys
import requests


def download_minecraft_images(file_path, output_folder):
    if not os.path.isfile(file_path):
        print(f"Error: The file '{file_path}' does not exist.")
        sys.exit(1)

    print(f"Reading block list from: {file_path}")
    with open(file_path, "r") as file:
        blocks = [line.strip() for line in file if line.strip()]

    if not blocks:
        return

    os.makedirs(output_folder, exist_ok=True)
    print(f"Saving images to folder: '{output_folder}'\n")

    for block in blocks:
        url_name = block.replace("_", "-")
        url = f"https://minecraft.wiki/images/BlockSprite_{url_name}.png"
        file_path = os.path.join(output_folder, f"{block}.png")

        try:
            response = requests.get(url, stream=True)

            if response.status_code == 200:
                with open(file_path, "wb") as f:
                    for chunk in response.iter_content(chunk_size=1024):
                        if chunk:
                            f.write(chunk)
                print(f"Downloaded: {block}.png")
            else:
                print(f"Failed: {block} (Status Code: {response.status_code})")

        except Exception as e:
            print(f"Error downloading {block}: {e}")

    print("\nProcess complete!")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Download Minecraft block sprite images from a text file list."
    )

    parser.add_argument(
        "file_path",
        type=str,
        help="Path to the text file containing the list of blocks (one per line).",
    )

    parser.add_argument(
        "-o",
        "--output",
        type=str,
        default="downloaded_blocks",
        help="Output directory for the downloaded images (default: downloaded_blocks)",
    )

    args = parser.parse_args()

    download_minecraft_images(args.file_path, args.output)
