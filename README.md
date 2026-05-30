## Config

```yml
network:
  port: 8080
  user: 
  password: 

```

## Fileformat
```
Bounding Box: 201x191x221.
Total blocks in payload: 8484411 (2169791 explicit, 6314620 skipped).
Palette size: 1 unique materials.

 57M output_file.csv
101K output_file.bin

¯\_(ツ)_/¯
```

### csv
```csv
x,y,z,material
0,-54,0,minecraft:stone
1,-54,1,minecraft:dirt
2,-54,2,minecraft:diamond_ore
3,-54,3,minecraft:grass_block
```

### binary
```
================================ HEADER =================================
 [4 bytes]                  Compressed chunk length
======================= ZLIB COMPRESSION ENVELOPE =======================
 [magic_number: 4 bytes]    "BLKS"
 [byte: format_version]     Currently 1 (0x01)
 [int: origin_x]            Minimum X coordinate in the world
 [int: origin_y]            Minimum Y coordinate in the world
 [int: origin_z]            Minimum Z coordinate in the world
 [int: size_x]              Width of the structure
 [int: size_y]              Height of the structure
 [int: size_z]              Depth of the structure
 [int: palette_length]      Number of unique materials in the palette

 --- PALETTE DATA (Repeats `palette_length` times) ---
 [short: string_length]     Byte length of the following string
 [string: material_name]    UTF-8 encoded string (e.g., "minecraft:stone")
 ... (loops for each material)

 --- BLOCK DATA (Implicit length based on size_x * size_y * size_z) ---
 [short: palette_index]     The uncompressed palette indices for the grid.
                            Parsed sequentially until the total 3D grid 
                            volume is satisfied.
=========================================================================

```
#### Notes:
- **Endianness**: All numbers (int, short) must be encoded in Big-Endian

- **Block Iteration Order**: The 1D block array maps to the 3D grid with **X changing fastest**, then Z, then Y.

- **Skip Flag**: A short value of -1 (0xFFFF) for empty space where the world should not be modified

- **Air vs. Skip**: To replace a block with Air, minecraft:air must be added to the palette, and its corresponding valid index (e.g., 0) should be written to the Block Data

Example generator/encoder: [Example-binary-generator](./utils/binary_generator.py)

Example grabber/decoder: [Example-binary-generator](./utils/grab_chunks.py)

## API

Some examples: [api.http](./api.http)

### Get list of worlds 
Needed for blocks destination

#### Request
```
GET <HOST>:<PORT>/worlds
```
#### Response
```
["world", "world_a", "world_b"]
```

### Set blocks 

#### Request
```
POST <HOST>:<PORT>/setblock
Authorization: Basic # user:password from config
Content-Type: application/octet-stream
X-Payload-Type: # "bin" or "csv"
X-World-Name: # destination world
```

### Get blocks

#### Request
```
GET <HOST>:<PORT>/getblock
?world_name= # source world
&minx= # int
&minz= #int
&sizex= # int
&sizez= # int
Authorization: Basic user:password
X-Payload-Type: # "bin" or "csv"
```