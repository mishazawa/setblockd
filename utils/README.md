# Utils folder

## Generate palette

```sh
make palette
```

This command will download sprites from minecraft wiki and will generate average color for each sprite for easier mapping later in Houdini.

Deps: go and python.

Usage reference [makefile](./../Makefile)

Example file:
```sh
# concrete.txt
white_concrete
...
red_concrete
black_concrete
```

## Binary generator

Generates test data in binary format and useful for reference implementation.

## Chunk grabber

Grab world region and generate csv. For debug and reference.

## Palette transfer utils

This code map point color to defined palette using CIELAB formula. To be run in Python SOP.

