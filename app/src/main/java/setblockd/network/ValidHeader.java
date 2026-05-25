package setblockd.network;

import java.util.Set;
import java.util.Arrays;

public enum ValidHeader {
  PAYLOAD_TYPE("X-Payload-Type", "csv", "bin");

  private final String headerName;
  private final Set<String> allowedValues;

  ValidHeader(String headerName, String... values) {
    this.headerName = headerName;
    this.allowedValues = Set.copyOf(Arrays.asList(values));
  }

  public String getHeaderName() {
    return headerName;
  }

  public static boolean isEmpty(String incomingValue) {
    return (incomingValue == null);
  }

  public boolean isValid(String incomingValue) {
    return !isEmpty(incomingValue) && allowedValues.contains(incomingValue.toLowerCase().trim());
  }
}