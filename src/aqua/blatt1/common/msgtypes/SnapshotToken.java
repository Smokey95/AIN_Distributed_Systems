package aqua.blatt1.common.msgtypes;
import java.io.Serializable;
import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;

@SuppressWarnings("serial")
public class SnapshotToken implements Serializable{
  
  private int globalSnapCount;
  
  public SnapshotToken() {
    this.globalSnapCount = 0;
  }
  
  public void addLocalCount(int localCount) {
    this.globalSnapCount += localCount;
  }
  
  public int getGlobalSnapCount() {
    return this.globalSnapCount;
  }
}
