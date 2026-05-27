package setblockd.data_utils.encoders;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

import java.io.OutputStream;
import java.util.List;

public interface StructureEncoder {
  void encode(OutputStream out, ChunkPos chunk, List<StructureBlock> data) throws Exception;
}