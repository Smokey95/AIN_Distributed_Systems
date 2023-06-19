package aqua.blatt1.broker;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import aqua.blatt1.client.TankModel;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.SecureEndpoint;

import aqua.blatt1.rmi.AquaBroker;
import aqua.blatt1.rmi.AquaClient;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Broker implements AquaBroker{
  
  SecureEndpoint endpoint;

  List<AquaClient> client_list;
  // Initial lease time for a client in milliseconds
  long init_lease_time = 10000;
  
  // Thread List
  List<Thread> clientThreads = new LinkedList<Thread>();
  
  int curr_client_count = 0;
  
  /**
   * Default constructor.
   * Initializes a broker with port 4711
   */
  public Broker() {
    this.endpoint = new SecureEndpoint(4711);
    this.client_list = new LinkedList<>();
  }
  
  
  /**
   * Constructor with variable port.
   * @param port Port to use for the broker
   */
  public Broker(int port) {
    this.endpoint = new SecureEndpoint(port);
    this.client_list = new LinkedList<>();
  }
  
  
  public static void main(String[] args) {

    try {      
      Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
      
      // Create a new Broker object
      AquaBroker stub = (AquaBroker) UnicastRemoteObject.exportObject(new Broker(), 0);
      
      // Bind the remote object's stub in the registry
      registry.rebind(Properties.BROKER_NAME, stub);
      
      System.out.println("AquaBrokerServer wurde gestartet." + stub.toString());
      
      //print more information
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    while(true) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }


  @Override
  public void register(AquaClient client) throws RemoteException {
    System.out.println("Client register request received");

    client_list.add(client);
    curr_client_count++;
  }


  @Override
  public void deregister(TankModel client) throws RemoteException {
    client_list.remove(client);
    curr_client_count--;
  }


  @Override
  public void handOffFish(FishModel fish, AquaClient client) throws RemoteException {
      System.out.println("Handoff request received");
      System.out.println(client_list);
      int currentIndex = client_list.indexOf(client);
      int nextIndex = (currentIndex + 1) % client_list.size();
      System.out.println("Current Client: " + currentIndex + " Index: " + currentIndex % client_list.size());
      System.out.println("Next Client: " + nextIndex + " Index: " + nextIndex % client_list.size());
      if (fish.getDirection() == Direction.LEFT) {
          int previousIndex = (currentIndex - 1 + client_list.size()) % client_list.size();
          client_list.get(previousIndex).receiveFish(fish);
      } else {
          client_list.get(nextIndex).receiveFish(fish);
      }
  }


  @Override
  public void resolveName(String tankId, String requestId) throws RemoteException {
    throw new UnsupportedOperationException("Unimplemented method 'resolveName'");
  }
}