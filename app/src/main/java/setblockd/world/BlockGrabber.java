package setblockd.world;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.logging.Logger;

import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.EncoderContext;
import setblockd.data_utils.GrabberContext;
import setblockd.data_utils.StructureBlock;
import setblockd.network.PayloadStreamer;

public class BlockGrabber implements PayloadStreamer {
  private Logger logger;

  public BlockGrabber(Logger logger) {
    this.logger = logger;
  }

  @Override
  public CompletableFuture<Void> streamPayload(OutputStream output, GrabberContext context, EncoderContext encoder)
      throws Exception {
    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
    World world = Bukkit.getWorld(context.world());
    if (world == null) {
      logger.warning("World not found. Use /worlds endpoint for list of worlds.");
      return chain;
    }

    final AtomicReference<Throwable> abortReason = new AtomicReference<>(null);

    logger.info("Grabbing from " + context.world() + "...");
    int minChunkX = context.minX() >> 4;
    int maxChunkX = context.maxX() >> 4;
    int minChunkZ = context.minZ() >> 4;
    int maxChunkZ = context.maxZ() >> 4;
    logger.info("Starting sequential chunk-by-chunk stream...");

    for (int cx = minChunkX; cx <= maxChunkX; cx++) {
      for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {

        final int fcx = cx;
        final int fcz = cz;
        ChunkPos chunkPos = new ChunkPos(cx, cz);

        chain = chain.thenCompose(v -> {
          // abort if previous chunk fail
          if (abortReason.get() != null) {
            return CompletableFuture.failedFuture(abortReason.get());
          }
          return world.getChunkAtAsync(fcx, fcz, true);
        }).thenCompose(chunk -> {
          // same: abort if previous chunk fail
          if (abortReason.get() != null) {
            return CompletableFuture.failedFuture(abortReason.get());
          }

          int bottom = chunk.getWorld().getMinHeight();
          int top = chunk.getWorld().getMaxHeight();

          ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false, false);

          CompletableFuture<Void> chunkProcessingFuture = new CompletableFuture<>();
          Tasks.async(() -> {
            try {
              List<StructureBlock> blocks = grabChunkData(snapshot, context, bottom, top);
              encoder.encode(output, chunkPos, blocks);
              chunkProcessingFuture.complete(null);
            } catch (Throwable t) {
              abortReason.compareAndSet(null, t);
              chunkProcessingFuture.completeExceptionally(t);
            }
          });
          return chunkProcessingFuture;
        });
      }
    }
    return chain;
  }

  private List<StructureBlock> grabChunkData(ChunkSnapshot snapshot, GrabberContext context, int bottom, int top) {
    List<StructureBlock> capturedBlocks = new ArrayList<>();
    int startX = snapshot.getX() << 4;
    int startZ = snapshot.getZ() << 4;

    for (int y = bottom; y < top; y++) {

      for (int z = 0; z < 16; z++) {
        int absoluteZ = startZ + z;
        if (absoluteZ < context.minZ() || absoluteZ > context.maxZ()) {
          continue;
        }

        for (int x = 0; x < 16; x++) {
          int absoluteX = startX + x;
          if (absoluteX < context.minX() || absoluteX > context.maxX()) {
            continue;
          }
          Material material = snapshot.getBlockType(x, y, z);

          if (material.isAir()) {
            continue;
          }

          StructureBlock structureBlock = new StructureBlock(
              absoluteX,
              y,
              absoluteZ,
              material);

          capturedBlocks.add(structureBlock);
        }
      }
    }
    return capturedBlocks;
  }

}
