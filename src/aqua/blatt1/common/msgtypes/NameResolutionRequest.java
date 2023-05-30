/*
 * NameResolutionRequest 
 * This message type transports the tank ID of the aquarium whose address is 
 * to be found, as well as a request ID that helps the requesting 
 * client to match request and response.
 */

package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NameResolutionRequest implements Serializable {
  
  private final String tankId;
  private final String requestId;
  
  // Constructor
  public NameResolutionRequest(String tankId, String requestId) {
    this.tankId = tankId;
    this.requestId = requestId;
  }
  
  // Returns the tank ID of the aquarium whose address is to be found
  public String getTankId() {
    return tankId;
  }
  
  // Returns the request ID that helps the requesting client to match request and response
  public String getRequestId() {
    return requestId;
  }
}
