package aqua.blatt1.common.msgtypes;
import java.io.Serializable;

import java.security.PublicKey;


public class KeyExchangeMessage implements Serializable {

    PublicKey key;
    Boolean   isClient;

    public KeyExchangeMessage(PublicKey key, Boolean isClient) {
        this.key = key;
        this.isClient = isClient;
    }
    
    public PublicKey getKey() {
        return key;
    }

    public Boolean getIsClient() {
        return isClient;
    }
}

