package setblockd.world;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;
import setblockd.network.PayloadReceiver;

public class BlockPlacer implements PayloadReceiver {
  private final Logger logger;

  public BlockPlacer(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void processPayload(String worldName, Map<ChunkPos, List<StructureBlock>> payload) throws Exception {
    if (payload.isEmpty()) {
      logger.warning("Payload was empty. Nothing to process.");
      return;
    }
    logger.info("Received " + payload.size() + " chunk groups.");

    World world = Bukkit.getWorld(worldName);

    if (world == null) {
      logger.warning("World not found. Use /worlds endpoint for list of worlds.");
      return;
    }

    for (var entry : payload.entrySet()) {
      ChunkPos chunkPos = entry.getKey();
      List<StructureBlock> blocksInChunk = entry.getValue();

      world.getChunkAtAsync(chunkPos.x(), chunkPos.z(), true)
          .thenAccept(chunk -> {
            for (StructureBlock b : blocksInChunk) {
              world.getBlockAt(b.x(), b.y(), b.z()).setType(b.material(), false);
            }
          })
          .exceptionally(ex -> {
            logger.warning("Error");
            ex.printStackTrace();
            return null;
          });
    }

  }
}