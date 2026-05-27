package setblockd.network;

import java.util.HashMap;
import java.util.Map;

public class UrlUtils {
  public static Map<String, String> parseQueryString(String query) {
    Map<String, String> result = new HashMap<>();
    if (query == null || query.isEmpty())
      return result;

    for (String param : query.split("&")) {
      String[] pair = param.split("=", 2);
      if (pair.length > 1) {
        result.put(pair[0].toLowerCase(), pair[1]);
      } else {
        result.put(pair[0].toLowerCase(), "");
      }
    }
    return result;
  }
}
