package aqua.blatt1.rmi;

import aqua.blatt1.common.FishModel;

import java.rmi.*;
import java.net.*;

public interface AquaClient extends Remote {
    public void receiveFish(FishModel fish) throws RemoteException;
}