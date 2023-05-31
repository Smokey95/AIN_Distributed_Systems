/*
 * LocationUpdate
 * This message type is used to inform the home agent of a fish about its new tank location.
 * Home agent is the tank where the fish is home based. It will receive the InetSocketAddress
 */

package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class LocationUpdate implements Serializable{
  
  private final String fishId;
  private final InetSocketAddress newTankAddress;
  
  // Constructor
  public LocationUpdate(String fishId, InetSocketAddress newTankAddress) {
    this.fishId = fishId;
    this.newTankAddress = newTankAddress;
  }
  
  // Returns the fish ID of the fish whose address is to be updated
  public String getFishId() {
    return fishId;
  }
  
  // Returns the address of the current tank
  public InetSocketAddress newTankAddress() {
    return newTankAddress;
  }
}
