package setblockd.data_utils.parsers;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public class BinaryParser implements StructureParser {
  private static final int MAGIC_NUMBER = 0x424C4B53;

  @Override
  public Map<ChunkPos, List<StructureBlock>> parse(InputStream input) throws Exception {
    Map<ChunkPos, List<StructureBlock>> chunkMap = new HashMap<>();
    try (var dataInput = new DataInputStream(input)) {
      int fileSignature = dataInput.readInt();

      if (fileSignature != MAGIC_NUMBER) {
        throw new IllegalArgumentException(
            "Invalid file format! Expected binary structure file (BLKS), but received unknown data.");
      }

      int totalBlocks = dataInput.readInt();
      for (int i = 0; i < totalBlocks; i++) {
        try {
          int x = dataInput.readInt();
          int y = dataInput.readInt();
          int z = dataInput.readInt();

          int stringLength = dataInput.readUnsignedShort();
          byte[] stringBytes = new byte[stringLength];
          dataInput.readFully(stringBytes);
          String matName = new String(stringBytes, StandardCharsets.UTF_8);

          Material material = Material.matchMaterial(matName);

          // Errors
          if (material == null || !material.isBlock()) {
            material = Material.BEDROCK;
          }

          var block = new StructureBlock(x, y, z, material);
          ChunkPos chunkPos = new ChunkPos(block.x() >> 4, block.z() >> 4);
          chunkMap.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(block);
        } catch (EOFException e) {
          throw new IllegalArgumentException("EOFException at block index " + i + " out of " + totalBlocks +
              ". The stream ran out of bytes early. Check data alignment and string format!", e);
        }
      }
    }

    return chunkMap;
  }

}
