package setblockd.data_utils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import setblockd.data_utils.parsers.StructureParser;

public class ParserContext {
  private StructureParser currentStrategy;

  public ParserContext(StructureParser defaultStrategy) {
    this.currentStrategy = defaultStrategy;
  }

  public void setStrategy(StructureParser strategy) {
    this.currentStrategy = strategy;
  }

  public Map<ChunkPos, List<StructureBlock>> parse(InputStream payload) throws Exception {
    if (currentStrategy == null) {
      throw new IllegalStateException("Parsing strategy is not initialized.");
    }
    return currentStrategy.parse(payload);
  }
}