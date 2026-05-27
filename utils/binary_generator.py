import struct
import os
import gzip

blocks_to_save = [
    (0, -54, 0, "minecraft:stone"),
    (1, -54, 1, "minecraft:dirt"),
    (2, -54, 2, "minecraft:diamond_ore"),
    (3, -54, 3, "minecraft:grass_block"),
]

# Ensure directory exists
os.makedirs("./test_data", exist_ok=True)
file_path = "./test_data/2.dat"

# 1. Calculate Origin and Dimensions dynamically
xs = [b[0] for b in blocks_to_save]
ys = [b[1] for b in blocks_to_save]
zs = [b[2] for b in blocks_to_save]

origin_x, origin_y, origin_z = min(xs), min(ys), min(zs)
size_x = max(xs) - origin_x + 1
size_y = max(ys) - origin_y + 1
size_z = max(zs) - origin_z + 1

# 2. Build Palette and Map materials to Indices
palette = []
material_to_idx = {}

for _, _, _, mat in blocks_to_save:
    if mat not in material_to_idx:
        material_to_idx[mat] = len(palette)
        palette.append(mat)

# 3. Create the 1D Array Grid (Pre-filled with -1 / Skip)
total_blocks = size_x * size_y * size_z
grid = [-1] * total_blocks

# Populate the grid with actual block palette indices
for x, y, z, mat in blocks_to_save:
    x_off = x - origin_x
    y_off = y - origin_y
    z_off = z - origin_z

    # Mathematical flattening of 3D to 1D matching Java's unpack logic
    # Order: X changes fastest, then Z, then Y
    idx = y_off * (size_x * size_z) + z_off * size_x + x_off
    grid[idx] = material_to_idx[mat]

# 4. Write Binary File
with open(file_path, "wb") as f:
    # Magic Number
    f.write(b"BLKS")

    # Version (1 byte, unsigned)
    f.write(struct.pack(">B", 1))

    # Origin (3x int, 12 bytes)
    f.write(struct.pack(">iii", origin_x, origin_y, origin_z))

    # Sizes (3x int, 12 bytes)
    f.write(struct.pack(">iii", size_x, size_y, size_z))

    # Palette Length (1x int, 4 bytes)
    f.write(struct.pack(">i", len(palette)))

    # Palette Entries
    for mat in palette:
        mat_bytes = mat.encode("utf-8")
        # Unsigned short (>H) for string length, then string bytes
        f.write(struct.pack(">H", len(mat_bytes)))
        f.write(mat_bytes)

    binary_bytes = struct.pack(f">{len(grid)}h", *grid)  # format >...h for short
    compressed_bytes = gzip.compress(binary_bytes)
    # write length of compressed voxels
    f.write(struct.pack(">I", len(compressed_bytes)))
    # write binary
    f.write(compressed_bytes)

print(f"OK. Bounding Box: {size_x}x{size_y}x{size_z}.")
print(
    f"Total blocks in payload: {total_blocks} ({len(blocks_to_save)} explicit, {total_blocks - len(blocks_to_save)} skipped)."
)
print(f"Palette size: {len(palette)} unique materials.")
