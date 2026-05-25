## Config

```yml
network:
  port: 8080
  user: 
  password: 

```

## Fileformat

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
[magic_number: BLKS]
[int: blocks number]
[int: x][int: y][int: z]
[short: string len][string: material]
...
...
```
Example generator code:

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
POST <HOST>:<PORT>/setblockd
Authorization: Basic # user:password from config
Content-Type: application/octet-stream
X-Payload-Type: # "bin" or "csv"
X-World-Name: # destination world
```