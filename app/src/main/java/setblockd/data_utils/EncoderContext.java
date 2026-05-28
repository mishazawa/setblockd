package setblockd.data_utils;

import java.io.OutputStream;
import java.util.List;

import setblockd.data_utils.encoders.StructureEncoder;

public class EncoderContext {
  private StructureEncoder currentStrategy;

  public EncoderContext(StructureEncoder defaultStrategy) {
    this.currentStrategy = defaultStrategy;
  }

  public void setStrategy(StructureEncoder strategy) {
    this.currentStrategy = strategy;
  }

  public void encode(OutputStream out, ChunkPos chunk, List<StructureBlock> blocks) throws Exception {
    if (currentStrategy == null) {
      throw new IllegalStateException("Encoding strategy is not initialized.");
    }
    currentStrategy.encode(out, chunk, blocks);
  }
}