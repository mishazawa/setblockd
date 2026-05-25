package setblockd.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import setblockd.data_utils.ParserContext;
import setblockd.data_utils.parsers.BinaryParser;
import setblockd.data_utils.parsers.CsvParser;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.slf4j.Logger;

public class NetworkServer {
  private final Logger logger;
  private final int port;
  private final PayloadReceiver payloadReceiver;

  private HttpServer server;
  private ExecutorService virtualThreadExecutor;
  private final String expectedAuthHeader;

  public NetworkServer(Logger logger, int port, String expectedAuthHeader, PayloadReceiver payloadReceiver) {
    this.logger = logger;
    this.port = port;
    this.payloadReceiver = payloadReceiver;
    this.expectedAuthHeader = expectedAuthHeader;
  }

  public void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress(port), 0);

    virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(virtualThreadExecutor);

    server.createContext("/worlds", this::getWorldsNames);
    server.createContext("/setblockd", this::receiveBlocks);

    server.start();
    logger.info("listening on port " + port);
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
    }
    if (virtualThreadExecutor != null) {
      virtualThreadExecutor.shutdown();
    }
    logger.info("Server stopped.");
  }

  private void receiveBlocks(HttpExchange exchange) throws IOException {
    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(405, -1);
      exchange.close();
      return;
    }

    String providedAuth = exchange.getRequestHeaders().getFirst("Authorization");
    if (providedAuth == null || !isAuthorized(providedAuth)) {
      exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"setblockd\"");
      exchange.sendResponseHeaders(401, -1);
      exchange.close();
      logger.warn("Unauthorized structure upload attempt from {}", exchange.getRemoteAddress());
      return;
    }

    String payloadType = exchange.getRequestHeaders().getFirst("X-Payload-Type");

    if (!ValidHeader.PAYLOAD_TYPE.isValid(payloadType)) {
      exchange.sendResponseHeaders(400, -1);
      exchange.close();
      return;
    }

    var pc = new ParserContext(null);

    if ("csv".equals(payloadType)) {
      pc.setStrategy(new CsvParser());
    }

    if ("bin".equals(payloadType)) {
      pc.setStrategy(new BinaryParser());
    }

    String worldName = exchange.getRequestHeaders().getFirst("X-World-Name");
    if (ValidHeader.isEmpty(worldName)) {
      exchange.sendResponseHeaders(400, -1);
      exchange.close();
      return;
    }

    try (InputStream is = exchange.getRequestBody()) {
      // PROCESSING
      var data = pc.parse(is);
      payloadReceiver.processPayload(worldName, data);

      String response = "Accepted for processing.";
      exchange.sendResponseHeaders(200, response.length());
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes());
      }

      logger.info("[/set] Successfully received and routed payload.");
    } catch (IllegalArgumentException e) {
      logger.error("[/set] Error processing incoming payload", e);
      exchange.sendResponseHeaders(400, -1);
    } catch (Exception e) {
      logger.error("[/set] Error processing incoming payload", e);
      exchange.sendResponseHeaders(500, -1);
    } finally {
      exchange.close();
    }
  }

  private boolean isAuthorized(String providedAuth) {
    byte[] expected = expectedAuthHeader.getBytes(StandardCharsets.UTF_8);
    byte[] provided = providedAuth.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, provided);
  }

  private void getWorldsNames(HttpExchange exchange) throws IOException {
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(405, -1);
      exchange.close();
      return;
    }

    List<String> worldNames = Bukkit.getWorlds().stream()
        .map(World::getName)
        .collect(Collectors.toList());

    String jsonResponse = worldNames.stream()
        .map(name -> "\"" + name + "\"")
        .collect(Collectors.joining(",", "[", "]"));

    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, jsonResponse.length());

    try (OutputStream os = exchange.getResponseBody()) {
      os.write(jsonResponse.getBytes());
    }
  }
}
