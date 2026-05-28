package setblockd.network;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import setblockd.data_utils.EncoderContext;
import setblockd.data_utils.GrabberContext;

@FunctionalInterface
public interface PayloadStreamer {
  CompletableFuture<Void> streamPayload(OutputStream output, GrabberContext ctx, EncoderContext encoder)
      throws Exception;
}