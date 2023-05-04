package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class SnapshotToken implements Serializable {
    
    private int globalCounter;

    public SnapshotToken() {
        this.globalCounter = 0;
    }

    public int getGlobalCounter() {
        return globalCounter;
    }
    
    public void addGlobalCounter(int localCount) {
        this.globalCounter += localCount;
    }
}
