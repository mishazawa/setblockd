package setblockd.experimental;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import setblockd.data_utils.ChunkPos;
import setblockd.data_utils.StructureBlock;

public class SetBlockCommand {

  public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
    return Commands.literal("setblockd")
        .then(Commands.argument("a", FloatArgumentType.floatArg(0, 1.0f))
            .executes(SetBlockCommand::runSetBlockLogic));
  }

  private static int runSetBlockLogic(CommandContext<CommandSourceStack> ctx) {

    CommandSender sender = ctx.getSource().getSender();
    Entity executor = ctx.getSource().getExecutor();

    if (!(executor instanceof Player player)) {
      sender.sendPlainMessage("Only players can fly!");
      return Command.SINGLE_SUCCESS;
    }

    List<StructureBlock> blocksToPlace = generateDummyStructure();
    player.sendMessage("Processing " + blocksToPlace.size() + " blocks...");

    Map<ChunkPos, List<StructureBlock>> blocksByChunk = new HashMap<>();

    for (StructureBlock block : blocksToPlace) {
      // (>> 4) = 16 (chunk size)
      ChunkPos chunkPos = new ChunkPos(block.x() >> 4, block.z() >> 4);
      blocksByChunk.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(block);
    }

    var world = player.getWorld();

    for (var entry : blocksByChunk.entrySet()) {
      ChunkPos chunkPos = entry.getKey();
      List<StructureBlock> blocksInChunk = entry.getValue();

      world.getChunkAtAsync(chunkPos.x(), chunkPos.z(), true)
          .thenAccept(chunk -> {
            for (StructureBlock b : blocksInChunk) {
              world.getBlockAt(b.x(), b.y(), b.z()).setType(b.material(), false);
            }
          })
          .exceptionally(ex -> {
            player.sendMessage("An error occurred while loading the chunk.");
            ex.printStackTrace();
            return null;
          });
    }
    player.sendMessage("Structure scheduled for placement!");
    return Command.SINGLE_SUCCESS;
  }

  private static List<StructureBlock> generateDummyStructure() {
    List<StructureBlock> blocks = new ArrayList<>();

    int startX = 0;
    int startY = 0;
    int startZ = 0;

    for (int x = 0; x < 10; x++) {
      for (int y = 0; y < 2; y++) {
        for (int z = 0; z < 10; z++) {
          blocks.add(new StructureBlock(
              startX + x,
              startY + y,
              startZ + z,
              Material.GLASS));
        }
      }
    }
    return blocks;
  }
}
