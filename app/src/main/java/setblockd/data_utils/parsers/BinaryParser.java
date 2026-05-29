package setblockd.data_utils.parsers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.bukkit.Material;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public class BinaryParser implements StructureParser {
  private static final int MAGIC_NUMBER = 0x424C4B53;

  @Override
  public Map<ChunkPos, List<StructureBlock>> parse(InputStream input) throws Exception {

    byte[] payload = input.readAllBytes();
    ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);

    int fileSignature = buffer.getInt();
    if (fileSignature != MAGIC_NUMBER) {
      throw new IllegalArgumentException(
          "Invalid file format! Expected binary structure file (BLKS), but received unknown data.");
    }

    byte version = buffer.get();
    if (version != 1) {
      throw new IllegalArgumentException("Unsupported format version: " + version);
    }

    // 3. Origin Coordinates
    int originX = buffer.getInt();
    int originY = buffer.getInt();
    int originZ = buffer.getInt();

    if (originY < -64 || originY > 319) {
      throw new IllegalArgumentException("Invalid origin value: " + originY);
    }

    // 4. Structure Dimensions
    int sizeX = buffer.getInt();
    int sizeY = buffer.getInt();
    int sizeZ = buffer.getInt();

    int paletteLength = buffer.getInt();
    Material[] palette = new Material[paletteLength];

    for (int i = 0; i < paletteLength; i++) {
      short stringLength = buffer.getShort();
      byte[] stringBytes = new byte[stringLength];
      buffer.get(stringBytes);

      String materialName = new String(stringBytes, StandardCharsets.UTF_8);

      Material mat = Material.matchMaterial(materialName);
      if (mat == null || !mat.isBlock()) {
        mat = Material.BEDROCK;
      }
      palette[i] = mat;
    }

    int compressedLength = buffer.getInt();
    byte[] compressedBytes = new byte[compressedLength];
    buffer.get(compressedBytes);

    short[] blocks = this.decompressGrid(compressedBytes);
    long totalBlocks = (long) sizeX * sizeY * sizeZ;
    int layerSize = sizeX * sizeZ;
    Map<ChunkPos, List<StructureBlock>> chunkMap = new HashMap<>();

    for (int i = 0; i < totalBlocks; i++) {
      short paletteIndex = blocks[i];
      if (paletteIndex == -1) {
        continue;
      }

      if (paletteIndex < 0 || paletteIndex >= paletteLength) {
        throw new IllegalArgumentException("Corrupt payload: palette index out of bounds");
      }

      // Unpack 1D index back to 3D coordinate space
      int yOffset = (int) (i / layerSize);
      int remainder = (int) (i % layerSize);
      int zOffset = remainder / sizeX;
      int xOffset = remainder % sizeX;

      int worldX = originX + xOffset;
      int worldY = originY + yOffset;
      int worldZ = originZ + zOffset;

      var block = new StructureBlock(worldX, worldY, worldZ, palette[paletteIndex]);
      ChunkPos chunkPos = new ChunkPos(block.x() >> 4, block.z() >> 4);
      chunkMap.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(block);
    }

    return chunkMap;
  }

  private short[] decompressGrid(byte[] data) throws IOException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
        GZIPInputStream gzipis = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];

      int bytesRead;
      while ((bytesRead = gzipis.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }

      byte[] uncompressedBytes = baos.toByteArray();

      int totalBlocks = uncompressedBytes.length / 2;
      short[] grid = new short[totalBlocks];

      ByteBuffer byteBuffer = ByteBuffer.wrap(uncompressedBytes);
      byteBuffer.order(ByteOrder.BIG_ENDIAN); // python '>' format

      for (int i = 0; i < totalBlocks; i++) {
        grid[i] = byteBuffer.getShort();
      }

      return grid;
    }
  }
}
