package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import javax.swing.*;

import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.client.Aqualife;
import aqua.blatt1.client.TankModel;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.SecureEndpoint;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.Token;
import aqua.blatt2.PoisonPill;
import aqua.blatt2.Poisoner;

import aqua.blatt1.rmi.AquaBroker;
import aqua.blatt1.rmi.AquaClient;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Broker implements AquaBroker{
  
  SecureEndpoint endpoint;
  
  // The list of clients uses InetSocketAddress as type for the key and String as type for the value.
  // InetSocketAddress is used as key because it is unique for each client and can be used to identify a client.
  ClientCollection<InetSocketAddress> cc_list;

  List<AquaClient> client_list;

  private Timer leaseTimeCheck = new Timer();
  
  // Initial lease time for a client in milliseconds
  long init_lease_time = 10000;
  
  // Thread List
  List<Thread> clientThreads = new LinkedList<Thread>();
  
  int curr_client_count = 0;
  
  private static int thread_pool_size = 10;
  
  private ReentrantReadWriteLock lock;

  private static ExecutorService executor = Executors.newFixedThreadPool(thread_pool_size);
  
  // Flag to stop the broker (needs to be volatile, see script "Nebenläufigkeit 3.1")
  private static volatile boolean stopRequested = false;

  
  /**
   * Default constructor.
   * Initializes a broker with port 4711
   */
  public Broker() {
    this.lock = new ReentrantReadWriteLock();
    //this.endpoint = new Endpoint(4711);
    this.endpoint = new SecureEndpoint(4711);
    
    // Create a new ClientCollection to store the clients
    this.cc_list = new ClientCollection<>();

    this.client_list = new LinkedList<>();
  }
  
  
  /**
   * Constructor with variable port.
   * @param port Port to use for the broker
   */
  public Broker(int port) {
    this.lock = new ReentrantReadWriteLock();
    //this.endpoint = new Endpoint(port);
    this.endpoint = new SecureEndpoint(port);
    this.cc_list = new ClientCollection<>();
  }
  
  
  /**
   * BrokerTask class. Handles a message from a client.
   */
  private class BrokerTask implements Runnable {
  
    Message message;

    public BrokerTask(Message message) {
        this.message = message;
    }

  
    @Override
    public void run() {
      var payload = message.getPayload();
      if (payload instanceof RegisterRequest){
        register(message);
      }
      if (payload instanceof DeregisterRequest){
        deregister(message);
      }
      if (payload instanceof HandoffRequest){
        handoffFish(message);
      }
      if (payload instanceof NameResolutionRequest){
        resolveName(message);
      }
    }
    
    /**
     * Registers a new client
     * @param currentMsg
     */
    private void register(Message currentMsg) {
      lock.writeLock().lock();
      try{
        
        // Check if client is already Registered
        // An empty client list will cause an IndexOutOfBoundsException
        InetSocketAddress currClient = null;
        try {
          currClient = cc_list.getClient(cc_list.indexOf(currentMsg.getSender()));
        } catch (IndexOutOfBoundsException e) {
          System.out.println("Client list is empty");
        }
        
        // Check if client is already registered
        if(currClient != null) {
          
          System.out.println("Client with ID " + cc_list.getId(cc_list.indexOf(currentMsg.getSender())) + " already registered");
          
          // Update client in the client list
          cc_list.updateClient(cc_list.getId(cc_list.indexOf(currentMsg.getSender())), 
                               currentMsg.getSender(), 
                               System.currentTimeMillis());
                               
          /*
           * Resend lease time to client again will cause the client to reset its lease timer.
           * Also it is possible to send a new lease time to the client.
          */ 
          endpoint.send(currentMsg.getSender(), 
                        new RegisterResponse(cc_list.getId(cc_list.indexOf(currentMsg.getSender())), init_lease_time));
                        
        } else {
          
          System.out.println("Registered new client with ID: tank" + curr_client_count);
          
          // Add client to the client list
          cc_list.add("tank" + (curr_client_count), currentMsg.getSender(), System.currentTimeMillis());
          
          // Send the new client its neighbors
          InetSocketAddress left_neighbor = cc_list.getLeftNeighorOf(curr_client_count);
          InetSocketAddress right_neighbor = cc_list.getRightNeighorOf(curr_client_count);
          endpoint.send(left_neighbor, new NeighborUpdate(currentMsg.getSender(), Direction.RIGHT));
          endpoint.send(right_neighbor, new NeighborUpdate(currentMsg.getSender(), Direction.LEFT));
          endpoint.send(currentMsg.getSender(), new NeighborUpdate(left_neighbor, Direction.LEFT));
          endpoint.send(currentMsg.getSender(), new NeighborUpdate(right_neighbor, Direction.RIGHT));
          
          // Send the new client its ID
          endpoint.send(cc_list.getClient(curr_client_count), 
                        new RegisterResponse("tank" + (curr_client_count), init_lease_time));       
          
          curr_client_count++;
        }
      } finally {
        if(curr_client_count == 1)
          endpoint.send(currentMsg.getSender(), new Token());
        lock.writeLock().unlock();
      }
    }
    
    /**
     * Deregistera client
     * @param currentMsg
     */
    private void deregister(Message currentMsg) {
      
      lock.writeLock().lock();
      
      try{
        int index = cc_list.indexOf(currentMsg.getSender());
        
        InetSocketAddress left_neighbor = cc_list.getLeftNeighorOf(index);
        InetSocketAddress right_neighbor = cc_list.getRightNeighorOf(index);
        
        endpoint.send(left_neighbor, new NeighborUpdate(right_neighbor, Direction.RIGHT));
        endpoint.send(right_neighbor, new NeighborUpdate(left_neighbor, Direction.LEFT));
        
        System.out.println("Deregistered client " + index);
        cc_list.remove(index);
        curr_client_count--;
      } catch (Exception e) {
        System.out.println("Exception in deregister");
      } finally {
        lock.writeLock().unlock();
      }
    }
    
    /**
     * Hands off a fish to the next/previous client
     * @param currentMsg
     */
    private void handoffFish(Message currentMsg) {
      System.out.println("DEPRECATED ! Handing off fish in broker");
      lock.readLock().lock();
      int curr_client = cc_list.indexOf(currentMsg.getSender());
      int next_client = (curr_client + 1) % cc_list.size();
        
      System.out.println("Handed off fish from client " + curr_client + " to client " + next_client);
        
      endpoint.send(cc_list.getClient(next_client), new HandoffRequest(((HandoffRequest) currentMsg.getPayload()).getFish()));
      lock.readLock().unlock();
    }
    
    /**
     * Determines the InetSocketAddress of the according TankID and returns it to sender
     * @param currentMsg
     */
    private void resolveName(Message currentMsg) {
      lock.readLock().lock();
      try {
        int index = cc_list.indexOf(currentMsg.getSender());
        String tankID = ((NameResolutionRequest) currentMsg.getPayload()).getTankId();
        String requestID = ((NameResolutionRequest) currentMsg.getPayload()).getRequestId();
        InetSocketAddress addr = cc_list.getClient(cc_list.indexOf(tankID));
        endpoint.send(cc_list.getClient(index), new NameResolutionResponse(requestID, addr));
      } catch (Exception e) {
        System.out.println("Exception in resolveName");
      } finally {
        lock.readLock().unlock();
      }
    }
  }
  
  
  private class LeasesTask implements Runnable {
      
      @Override
      public void run() {
        
        System.out.println("LeasesTask started, will check for disconnected clients every " + init_lease_time * 2 + "ms");
        
        while(!stopRequested) {
          
          try {
            Thread.sleep(init_lease_time * 2);
            //Thread.sleep(1000);           // For testing purposes
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          
          lock.writeLock().lock();
          
          try {
            System.out.println("Checking for disconnected clients");
            for(int i = 0; i < cc_list.size(); i++) {
              
              //if(System.currentTimeMillis() - cc_list.getTimestamp(i) > init_lease_time || i == 0) {  // For testing purposes (force disconnect client 0)
              if(System.currentTimeMillis() - cc_list.getTimestamp(i) > init_lease_time) {
                System.out.println("Client " + cc_list.getId(i) + " has been removed due to timeout");
                
                InetSocketAddress left_neighbor = cc_list.getLeftNeighorOf(i);
                InetSocketAddress right_neighbor = cc_list.getRightNeighorOf(i);
        
                endpoint.send(left_neighbor, new NeighborUpdate(right_neighbor, Direction.RIGHT));
                endpoint.send(right_neighbor, new NeighborUpdate(left_neighbor, Direction.LEFT));
        
                // Send a PoisonPill to the client
                endpoint.send(cc_list.getClient(i), new PoisonPill());
                
                // Remove the client from the client list
                cc_list.remove(i);
                curr_client_count--;
              }
            }
          } catch (Exception e) {
            System.out.println("Exception in LeasesTask");
          } finally {
            lock.writeLock().unlock();
          }
        }
      }
  }
  
  /**
   * Broker-Loop. Receives messages from clients and handles them in a BrokerTask thread.
   */
  private void broker() {
    
    System.out.println("Broker started");
    
    // Create a new thread for the shutdown pane
    new Thread(() -> Poisoner.main(null)).start();
    
    // Create a new thread for the lease time check
    new Thread(new LeasesTask()).start();

    // Broker-Loop in a seperate thread
    while (true) {
      
      // Receive a message from clients. Reads and returns a message. If no message currently waits for reading, 
      // this operation blocks until a message arrives at this endpoint. return message
      Message message = endpoint.blockingReceive();
      
      // Check if the message is a PoisonPill. If so, send a PoisonPill to all clients and break the loop.
      if(message.getPayload() instanceof PoisonPill) {
        for(int i = 0; i < cc_list.size(); i++) {
          endpoint.send(cc_list.getClient(i), new PoisonPill());
        }
        
        // Set the stopRequested flag to true to stop other broker threads (like the LeasesTask)
        stopRequested = true;
        
        break;
      }
      
      // Handle the message in a separated BrokerTask thread
      executor.execute(new BrokerTask(message));
      
      
    }
    
    // Executor-Service has to be shutdown to terminate the program
    System.out.println("Broker stopped");
    executor.shutdown();
  }
  
  public static void main(String[] args) {
    // Broker broker = new Broker();
    // broker.broker();

    try {
      
      //System.setProperty("java.rmi.server.hostname", "localhost");
      
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
      // Wait for the shutdown pane to send a PoisonPill
      
      // check if new client has connecte
    }
  }


  @Override
  public void register(AquaClient client) throws RemoteException {
    
    System.out.println("Client register request received");
    
    client_list.add(client);
    System.out.println("Client count: " + cc_list.size());
    curr_client_count++;
  }


  @Override
  public void deregister(TankModel client) throws RemoteException {
    // TODO Auto-generated method stub
    client_list.remove(client);
    curr_client_count--;
  }


  @Override
  public void handOffFish(FishModel fish) throws RemoteException {
    System.out.println("Handoff request received");
    if(fish.getDirection() == Direction.LEFT) {
      client_list.get(0).receiveFish(fish);
    } else {
      client_list.get(0).receiveFish(fish);
    }
  }


  @Override
  public void resolveName(String tankId, String requestId) throws RemoteException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'resolveName'");
  }
}