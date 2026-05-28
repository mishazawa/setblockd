package setblockd;

import net.kyori.adventure.text.Component;
import setblockd.network.NetworkServer;
import setblockd.world.BlockGrabber;
import setblockd.world.BlockPlacer;
import setblockd.world.Tasks;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SetBlockPlugin extends JavaPlugin implements Listener {
    private NetworkServer server;
    private BlockPlacer placer;
    private BlockGrabber grabber;

    @Override
    public void onEnable() {
        Tasks.init(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        var logger = getSLF4JLogger();

        this.placer = new BlockPlacer(getLogger());
        this.grabber = new BlockGrabber(getLogger());
        saveDefaultConfig();

        int port = getConfig().getInt("network.port", 8080);
        String user = getConfig().getString("network.user", null);
        String pass = getConfig().getString("network.password", null);

        if (user == null || pass == null) {
            logger.error("==================================================");
            logger.error("   [SetBlockPlugin] CONFIGURATION ERROR!");
            logger.error("   'user' or 'password' is missing from config.yml");
            logger.error("   The API server cannot start without credentials.");
            logger.error("==================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String credentials = user + ":" + pass;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        String expectedAuthHeader = "Basic " + encodedCredentials;

        server = new NetworkServer(logger, port, expectedAuthHeader, placer, grabber);

        try {
            server.start();
        } catch (Exception e) {
            logger.error("Failed to start the Structure Network Server!", e);
            getServer().getPluginManager().disablePlugin(this);
        }

    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }
}