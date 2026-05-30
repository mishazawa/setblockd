package setblockd.data_utils.encoders;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import org.bukkit.Material;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public class BinaryEncoder implements StructureEncoder {
  private static final byte[] MAGIC_NUMBER = "BLKS".getBytes(StandardCharsets.US_ASCII);
  private static final byte FORMAT_VERSION = 1;
  private static final short SKIP_FLAG = -1;
  private static final int WORLD_MIN_Y = -64;
  private static final int WORLD_HEIGHT = 384;

  @Override
  public void encode(OutputStream out, ChunkPos chunk, List<StructureBlock> data) throws Exception {
    byte[] compressedChunkBytes;

    try (
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
        DeflaterOutputStream zlibOut = new DeflaterOutputStream(chunkBuffer);
        DataOutputStream dos = new DataOutputStream(zlibOut);) {

      // palette

      Map<Material, Integer> materialToId = new HashMap<>();
      List<Material> idToMaterial = new ArrayList<>();

      for (StructureBlock block : data) {
        Material material = block.material();
        if (!materialToId.containsKey(material)) {
          materialToId.put(material, materialToId.size());
          idToMaterial.add(material);
        }
      }

      // bounds

      int minY = WORLD_MIN_Y;
      int sizeY = WORLD_HEIGHT;

      int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
      int sizeX = Integer.MIN_VALUE, sizeZ = Integer.MIN_VALUE;

      for (StructureBlock block : data) {
        minX = Math.min(minX, block.x());
        minZ = Math.min(minZ, block.z());
        sizeX = Math.max(sizeX, block.x());
        sizeZ = Math.max(sizeZ, block.z());
      }

      sizeX = data.isEmpty() ? 0 : (sizeX - minX + 1);
      sizeY = data.isEmpty() ? 0 : (sizeY - minY + 1);
      sizeZ = data.isEmpty() ? 0 : (sizeZ - minZ + 1);

      // header
      dos.write(MAGIC_NUMBER);
      dos.writeByte(FORMAT_VERSION);
      dos.writeInt(minX);
      dos.writeInt(minY);
      dos.writeInt(minZ);
      dos.writeInt(sizeX);
      dos.writeInt(sizeY);
      dos.writeInt(sizeZ);

      // palette table
      dos.writeInt(idToMaterial.size());
      for (Material material : idToMaterial) {
        byte[] materialBytes = material.toString().getBytes(StandardCharsets.UTF_8);
        dos.writeShort((short) materialBytes.length);
        dos.write(materialBytes);
      }

      // grid
      int totalBlocks = sizeX * sizeY * sizeZ;
      short[] blockGrid = new short[totalBlocks];
      java.util.Arrays.fill(blockGrid, SKIP_FLAG);

      for (StructureBlock block : data) {
        int localX = block.x() - minX;
        int localY = block.y() - minY;
        int localZ = block.z() - minZ;

        int index = localX + (localZ * sizeX) + (localY * sizeX * sizeZ);

        short paletteId = materialToId.get(block.material()).shortValue();
        blockGrid[index] = paletteId;
      }

      for (short blockVal : blockGrid) {
        dos.writeShort(blockVal);
      }

      dos.flush();
      zlibOut.finish();
      compressedChunkBytes = chunkBuffer.toByteArray();
    }

    DataOutputStream httpDos = new DataOutputStream(out);
    httpDos.writeInt(compressedChunkBytes.length);
    httpDos.write(compressedChunkBytes);
    httpDos.flush(); // leave open
  }

}
