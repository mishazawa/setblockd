package setblockd.data_utils.parsers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import org.bukkit.Material;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public class BinaryParser implements StructureParser {
  private static final int MAGIC_NUMBER = 0x424C4B53;

  @Override
  public Map<ChunkPos, List<StructureBlock>> parse(InputStream input) throws Exception {

    byte[] lengthBytes = new byte[4];
    int headerBytesRead = input.readNBytes(lengthBytes, 0, 4);
    if (headerBytesRead < 4) {
      throw new IllegalArgumentException("Failed to read the 4-byte protocol length header.");
    }

    int expectedCompressedLength = ByteBuffer.wrap(lengthBytes).getInt();

    byte[] compressedPayload = input.readNBytes(expectedCompressedLength);
    if (compressedPayload.length != expectedCompressedLength) {
      throw new IllegalArgumentException(String.format(
          "Protocol mismatch! Expected %d compressed bytes, but stream ended after %d bytes.",
          expectedCompressedLength, compressedPayload.length));
    }

    byte[] decompressedPayload;
    try (ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedPayload);
        InflaterInputStream zlibStream = new InflaterInputStream(byteStream)) {
      decompressedPayload = zlibStream.readAllBytes();
    }

    ByteBuffer buffer = ByteBuffer.wrap(decompressedPayload).order(ByteOrder.BIG_ENDIAN);

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

    long totalBlocks = (long) sizeX * sizeY * sizeZ;
    int layerSize = sizeX * sizeZ;
    Map<ChunkPos, List<StructureBlock>> chunkMap = new HashMap<>();

    for (int i = 0; i < totalBlocks; i++) {
      if (!buffer.hasRemaining()) {
        throw new IllegalArgumentException("Corrupt payload: Unexpected end of block data stream.");
      }

      short paletteIndex = buffer.getShort();
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

}
