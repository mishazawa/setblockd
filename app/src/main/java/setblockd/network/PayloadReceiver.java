package setblockd.network;

import java.util.List;
import java.util.Map;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

@FunctionalInterface
public interface PayloadReceiver {
  /**
   * Called by a Virtual Thread when a new payload is parsed.
   * 
   * @param payload Map<ChunkPos, List<StructureBlock>>
   */
  void processPayload(String world, Map<ChunkPos, List<StructureBlock>> payload) throws Exception;
}