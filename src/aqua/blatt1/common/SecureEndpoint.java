package aqua.blatt1.common;

import messaging.Message;
import messaging.Endpoint;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.net.InetSocketAddress;

import javax.crypto.Cipher;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;

import java.security.PrivateKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public class SecureEndpoint extends Endpoint implements Serializable {
  
  private Endpoint endpoint;
  private PublicKey publicKey;
  private PrivateKey privateKey;
  
  private HashMap<InetSocketAddress, PublicKey> knownPublicKeys;

  public SecureEndpoint(){
    
    this.endpoint = new Endpoint();
    
    try {
      // Create a KeyPairGenerator instance
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

      // Initialize the KeyPairGenerator with a key size
      keyPairGenerator.initialize(4080);                                // You can choose a different key size if needed

      // Generate the key pair
      KeyPair keyPair = keyPairGenerator.generateKeyPair();

      // Get the public and private keysW
      this.publicKey = keyPair.getPublic();
      this.privateKey = keyPair.getPrivate();
      
      this.knownPublicKeys = new HashMap<InetSocketAddress, PublicKey>();

      // Print the keys
      //System.out.println("Public Key: " + publicKey);
      //System.out.println("Private Key: " + privateKey);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }
  
  
  public SecureEndpoint(int port) {
    
    this.endpoint = new Endpoint(port);
    
    try {
      // Create a KeyPairGenerator instance
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

      // Initialize the KeyPairGenerator with a key size
      keyPairGenerator.initialize(2048 * 2);                                // You can choose a different key size if needed

      // Generate the key pair
      KeyPair keyPair = keyPairGenerator.generateKeyPair();

      // Get the public and private keysW
      this.publicKey = keyPair.getPublic();
      this.privateKey = keyPair.getPrivate();
      
      this.knownPublicKeys = new HashMap<InetSocketAddress, PublicKey>();

      // Print the keys
      //System.out.println("Public Key: " + publicKey);
      //System.out.println("Private Key: " + privateKey);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }


  @Override
  public void send(java.net.InetSocketAddress receiver, java.io.Serializable payload) {
    // check if public key is known
    if(this.knownPublicKeys.containsKey(receiver)){
      // add encryption here  
      this.endpoint.send(receiver, encryptMessage(payload, this.knownPublicKeys.get(receiver)));
    } else {
      // send public key to receiver
      //System.out.println("Sending public [" + this.publicKey + "] to " + receiver);
      this.endpoint.send(receiver, new KeyExchangeMessage(this.publicKey, false));
      
      // wait for public key from receiver
      while(!this.knownPublicKeys.containsKey(receiver)){
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      
      if(!this.knownPublicKeys.containsKey(receiver)){
        System.out.println("ERROR: Public key not received");
      } else {
        // add encryption here
        //System.out.println("Sending encrypted message of type" + payload.getClass() + " to " + receiver);
        this.endpoint.send(receiver, encryptMessage(payload, this.knownPublicKeys.get(receiver)));
      }
    }
  }
  
  private byte[] encryptMessage(Serializable payload, PublicKey publicKey){
    // add encryption here
    try {
      // Create Cipher instance
      Cipher cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);

      // Create a byte array output stream
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

      // Create an object output stream
      ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);

      // Write the payload object to the object output stream
      objectOut.writeObject(payload);
      objectOut.flush();
      
      // Get the byte array of the serialized object
      byte[] payloadBytes = byteOut.toByteArray();

      // Encrypt the payload bytes
      byte[] encryptedBytes = cipher.doFinal(payloadBytes);

      // Encode encrypted bytes to base64 string
      //return Base64.getEncoder().encodeToString(encryptedBytes);
      return encryptedBytes;  
    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    }
  }
  
  private Message decryptMessage(Message encryptedPayload, PrivateKey privateKey) {
      try {
        // Create Cipher instance
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // Decrypt the payload bytes
        byte[] decryptedBytes = cipher.doFinal((byte[]) encryptedPayload.getPayload());

        // Create a byte array input stream
        ByteArrayInputStream byteIn = new ByteArrayInputStream(decryptedBytes);

        // Create an object input stream
        ObjectInputStream objectIn = new ObjectInputStream(byteIn);

        // Read the deserialized object from the object input stream
        Serializable payload = (Serializable) objectIn.readObject();

        return new Message(payload, encryptedPayload.getSender());
      } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException(e);
      }
  }


  @Override
  public Message blockingReceive() {
    Message message = this.endpoint.blockingReceive();
    
    System.out.println("Received message with a size of " + (message.getPayload().toString().length()) + " bytes from " + message.getSender());
    
    // check if message is a public key
    if(message.getPayload() instanceof KeyExchangeMessage){
      // add public key to known public keys
      KeyExchangeMessage keyExchangeMessage = (KeyExchangeMessage) message.getPayload();
      
      //System.out.println("Received public [" + keyExchangeMessage.getKey() + "] from " + message.getSender());
      
      if(keyExchangeMessage.getIsClient()){
        this.knownPublicKeys.put(message.getSender(), keyExchangeMessage.getKey());
        return message;
      } else {
        this.knownPublicKeys.put(message.getSender(), keyExchangeMessage.getKey());
        this.endpoint.send(message.getSender(),  new KeyExchangeMessage(this.publicKey, true));
        return message;
      }
    } else {
      // decrypt message
      return decryptMessage(message, this.privateKey);
    }
    
  }


  
  //@Override
  //public Message nonBlockingReceive() {
//
  //}
}
