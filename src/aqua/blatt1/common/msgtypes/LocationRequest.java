package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import aqua.blatt1.common.FishModel;

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
