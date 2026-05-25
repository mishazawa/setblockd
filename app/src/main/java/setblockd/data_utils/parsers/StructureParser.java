package setblockd.data_utils.parsers;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public interface StructureParser {
  /**
   * Parses an input payload and groups the structure blocks by their chunk key.
   *
   * @param input The incoming network payload stream
   * @return A map of ChunkPos to a List of StructureBlocks
   * @throws Exception if parsing fails
   */
  Map<ChunkPos, List<StructureBlock>> parse(InputStream input) throws Exception;
}