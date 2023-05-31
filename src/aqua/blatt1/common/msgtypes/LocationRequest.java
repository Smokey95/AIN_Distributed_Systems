/**
 * LocationRequest.java
 * Message type for requesting the location of a fish.
 */

package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LocationRequest implements Serializable{
  private final String fish;
  
  public LocationRequest(String fish) {
    this.fish = fish;
  }
  
  public String getFish() {
    return fish;
  }
}
