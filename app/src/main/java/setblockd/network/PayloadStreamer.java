package setblockd.network;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import setblockd.data_utils.StreamContext;

@FunctionalInterface
public interface PayloadStreamer {
  CompletableFuture<Void> streamPayload(OutputStream output, StreamContext ctx) throws Exception;
}