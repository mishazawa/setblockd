package setblockd.world;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

            int minHeight = chunk.getWorld().getMinHeight();
            int maxHeight = chunk.getWorld().getMaxHeight();
            Location chunkLocation = chunk.getBlock(0, minHeight, 0).getLocation();

            Tasks.ouputToRegion(chunkLocation, () -> {
              for (StructureBlock b : blocksInChunk) {

                int targetY = b.y();

                // skip the block if it's out of bounds
                if (targetY < minHeight || targetY >= maxHeight) {
                  continue;
                }

                // relative coordinates
                int relX = b.x() & 15;
                int relZ = b.z() & 15;
                chunk.getBlock(relX, targetY, relZ).setType(b.material(), false);
              }

              world.refreshChunk(chunkPos.x(), chunkPos.z());
            });

          })
          .exceptionally(ex -> {
            logger.warning("Error");
            ex.printStackTrace();
            return null;
          });
    }

  }
}