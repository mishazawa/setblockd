package setblockd.data_utils.parsers;

import org.bukkit.Material;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvParser implements StructureParser {

  @Override
  public Map<ChunkPos, List<StructureBlock>> parse(InputStream input) throws Exception {
    Map<ChunkPos, List<StructureBlock>> chunkMap = new HashMap<>();

    try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String line;
      boolean isFirstLine = true;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty())
          continue;

        if (isFirstLine && line.toLowerCase().contains("x")) {
          isFirstLine = false;
          continue;
        }

        String[] parts = line.split(",");
        if (parts.length != 4) {
          throw new IllegalArgumentException(String.format(
              "CSV Corrupted: Expected 4 columns (x,y,z,material), but found %d columns.", parts.length));
        }

        int x, y, z;
        try {
          // Check 2: Data Type Corruption
          x = Integer.parseInt(parts[0].trim());
          y = Integer.parseInt(parts[1].trim());
          z = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(String.format(
              "CSV Corrupted: Coordinates must be valid integers. Found: %s, %s, %s",
              parts[0], parts[1], parts[2]));
        }
        Material material = Material.matchMaterial(parts[3].trim());
        // Errors
        if (material == null || !material.isBlock()) {
          material = Material.BEDROCK;
        }

        var block = new StructureBlock(x, y, z, material);
        ChunkPos chunkPos = new ChunkPos(block.x() >> 4, block.z() >> 4);
        chunkMap.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(block);
      }
    }
    return chunkMap;
  }
}