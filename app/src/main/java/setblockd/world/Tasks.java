package setblockd.world;

import setblockd.SetBlockPlugin;

public class Tasks {
  private static SetBlockPlugin plugin;

  public static void init(SetBlockPlugin instance) {
    plugin = instance;
  }

  public static void ouputToRegion(org.bukkit.Location loc, Runnable runnable) {
    org.bukkit.Bukkit.getRegionScheduler().run(plugin, loc, task -> runnable.run());
  }

  public static void async(Runnable runnable) {
    org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
  }
}
