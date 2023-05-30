/*
 * NameResolutionResponse
 * This message type contains the address of the requested 
 * aquarium and the unchanged request ID of the request.
 */

package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class NameResolutionResponse implements Serializable{
  
  private final String requestId;
  private final InetSocketAddress address;
  
  // Constructor
  public NameResolutionResponse(String requestId, InetSocketAddress address) {
    this.requestId = requestId;
    this.address = address;
  }
  
  // Returns the request ID of the request
  public String getRequestId() {
    return requestId;
  }
  
  // Returns the address of the requested aquarium
  public InetSocketAddress getAddress() {
    return address;
  }
}
