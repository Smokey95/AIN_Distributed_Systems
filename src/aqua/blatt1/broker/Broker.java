package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;

public class Broker {
  
  Endpoint endpoint;
  ClientCollection<InetSocketAddress> cc_list;

  public Broker() {
    this.endpoint = new Endpoint(4711);
    this.cc_list = new ClientCollection<>();
    

  }

  public void broker() {
    Message currentMsg;
    int curr_client_count = 0;

    while(true) {
      currentMsg = this.endpoint.blockingReceive();
      
      if(currentMsg.getPayload() instanceof RegisterRequest) {
        cc_list.add("tank" + (curr_client_count), currentMsg.getSender());
        endpoint.send(cc_list.getClient(curr_client_count), new RegisterResponse("tank" + (curr_client_count)));
        curr_client_count++;
      }
      
      if(currentMsg.getPayload() instanceof DeregisterRequest) {
        int index = cc_list.indexOf(currentMsg.getSender());
        cc_list.remove(index);
      }
      
      if(currentMsg.getPayload() instanceof HandoffRequest) {
        int curr_client = cc_list.indexOf(currentMsg.getSender());
        int next_client = (curr_client + 1) % cc_list.size();
        
        endpoint.send(cc_list.getClient(next_client), new HandoffRequest(((HandoffRequest) currentMsg.getPayload()).getFish()));
      }
      
    }
  }
  
  public static void main(String[] args) {
    Broker broker = new Broker();
    broker.broker();
  }
}
