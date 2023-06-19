package aqua.blatt1.rmi;

import java.rmi.*;

import aqua.blatt1.common.FishModel;
import aqua.blatt1.client.TankModel;

import java.net.*;

public interface AquaBroker extends Remote {
  
    // register method requires client stub to be passed as parameter
    public void register(AquaClient client) throws RemoteException;

    public void deregister(TankModel client) throws RemoteException;

    public void handOffFish(FishModel fish) throws RemoteException;
    
    public void resolveName(String tankId, String requestId) throws RemoteException;
}