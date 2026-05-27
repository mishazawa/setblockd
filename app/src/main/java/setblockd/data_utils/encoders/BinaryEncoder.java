package setblockd.data_utils.encoders;

import java.io.OutputStream;
import java.util.List;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public class BinaryEncoder implements StructureEncoder {

  @Override
  public void encode(OutputStream out, ChunkPos chunk, List<StructureBlock> data) throws Exception {
    java.io.BufferedWriter writer = new java.io.BufferedWriter(
        new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8));

    // Write a header indicating the start of a new chunk to verify sequence in your
    // tests
    writer
        .write(String.format("# --- [CHUNK START] X: %d, Z: %d (Blocks: %d) ---\n", chunk.x(), chunk.z(), data.size()));
    writer.write("X,Y,Z,Material,BlockData\n");

    // Loop through the blocks and write them in CSV format
    for (StructureBlock block : data) {
      // Adjust these getter names to match your actual StructureBlock class methods
      String line = String.format("%d,%d,%d,%s\n",
          block.x(),
          block.y(),
          block.z(),
          block.material().name());
      writer.write(line);
    }

    // CRITICAL: Flush the writer so the data is instantly forced into the
    // OutputStream.
    // Do NOT close the writer here, or it will prematurely close your main 'out'
    // stream!
    writer.flush();
  }

}
