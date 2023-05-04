package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {
	
  private final InetSocketAddress neighbor;
  private final Direction direction;
  
	public NeighborUpdate(InetSocketAddress neighbor, Direction direction) {
    this.neighbor = neighbor;
    this.direction = direction;
  }
  
  public InetSocketAddress getNeighbor() {
    return neighbor;
  }
  
  public Direction getDirection() {
    return direction;
  }
}
