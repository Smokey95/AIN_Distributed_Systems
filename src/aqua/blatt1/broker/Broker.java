package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import javax.swing.*;

import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.client.Aqualife;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt2.PoisonPill;
import aqua.blatt2.Poisoner;


public class Broker {
  
  Endpoint endpoint;
  
  // The list of clients uses InetSocketAddress as type for the key and String as type for the value.
  // InetSocketAddress is used as key because it is unique for each client and can be used to identify a client.
  ClientCollection<InetSocketAddress> cc_list;
  
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
    this.endpoint = new Endpoint(4711);
    
    // Create a new ClientCollection to store the clients
    this.cc_list = new ClientCollection<>();
  }
  
  
  /**
   * Constructor with variable port.
   * @param port Port to use for the broker
   */
  public Broker(int port) {
    this.lock = new ReentrantReadWriteLock();
    this.endpoint = new Endpoint(port);
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
    }
    
    /**
     * Registers a new client
     * @param currentMsg
     */
    private void register(Message currentMsg) {
      lock.writeLock().lock();
      try{
        cc_list.add("tank" + (curr_client_count), currentMsg.getSender());
        endpoint.send(cc_list.getClient(curr_client_count), new RegisterResponse("tank" + (curr_client_count)));       
        System.out.println("Registered client " + curr_client_count);    
        curr_client_count++;
      } finally {
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
        System.out.println("Deregistered client " + index);
        cc_list.remove(index);
      } finally {
        lock.writeLock().unlock();
      }
    }
    
    /**
     * Hands off a fish to the next/previous client
     * @param currentMsg
     */
    private void handoffFish(Message currentMsg) {
      lock.readLock().lock();
      int curr_client = cc_list.indexOf(currentMsg.getSender());
      int next_client = (curr_client + 1) % cc_list.size();
        
      System.out.println("Handed off fish from client " + curr_client + " to client " + next_client);
        
      endpoint.send(cc_list.getClient(next_client), new HandoffRequest(((HandoffRequest) currentMsg.getPayload()).getFish()));
      lock.readLock().unlock();
    }
  }
  
  
  /**
   * Broker-Loop. Receives messages from clients and handles them in a BrokerTask thread.
   */
  private void broker() {
    
    System.out.println("Broker started");
    
    // Create a new thread for the shutdown pane
    new Thread(() -> Poisoner.main(null)).start();
    
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
    Broker broker = new Broker();
    broker.broker();
  }
}