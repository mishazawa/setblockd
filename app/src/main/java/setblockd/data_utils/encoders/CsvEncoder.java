package setblockd.data_utils.encoders;

import java.io.OutputStream;
import java.util.List;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public class CsvEncoder implements StructureEncoder {

  @Override
  public void encode(OutputStream out, ChunkPos chunk, List<StructureBlock> data) throws Exception {
    java.io.BufferedWriter writer = new java.io.BufferedWriter(
        new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8));

    writer.write("x,y,z,material\n");

    for (StructureBlock block : data) {
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
