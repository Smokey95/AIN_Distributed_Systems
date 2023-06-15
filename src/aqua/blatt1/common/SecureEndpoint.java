package aqua.blatt1.common;

import messaging.Message;
import javax.crypto.spec.SecretKeySpec;
import messaging.Endpoint;
import java.security.Key;
import javax.crypto.Cipher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public class SecureEndpoint extends Endpoint implements Serializable {
  
  private Cipher encryptor;
  private Cipher decryptor;

  private String ecrypt_string = "CAFEBABECAFEBABE";
  private byte[] byteArray = ecrypt_string.getBytes();
  private Endpoint endpoint;
  private Key key;
  
  public SecureEndpoint(){
    
    this.endpoint = new Endpoint();
    this.key = new SecretKeySpec(this.byteArray, "AES");
    
    try {
      this.encryptor = Cipher.getInstance("AES");
      this.encryptor.init(Cipher.ENCRYPT_MODE, this.key);
      
      this.decryptor = Cipher.getInstance("AES");
      this.decryptor.init(Cipher.DECRYPT_MODE, this.key);
    } catch (Exception e) {
      System.out.println("Exception in SecureEndpoint: " + e);
      throw new RuntimeException(e);
    }
  }
  
  
  public SecureEndpoint(int port) {
    
    this.endpoint = new Endpoint(port);
    this.key = new SecretKeySpec(this.byteArray, "AES");
    
    try {
      this.encryptor = Cipher.getInstance("AES");
      this.encryptor.init(Cipher.ENCRYPT_MODE, this.key);
      
      this.decryptor = Cipher.getInstance("AES");
      this.decryptor.init(Cipher.DECRYPT_MODE, this.key);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

    /* Infos for dummies
     *   Send Method:
     *     Convert the payload object into a byte array:
     *         Open a ByteArrayOutputStream (byteOut) to collect the bytes.
     *         Create an ObjectOutputStream (objectOut) to write the payload object into byteOut.
     *         Write the payload object to objectOut.
     *         Flush the objectOut stream to ensure all data is written.
     *         Get the byte array representation of the serialized object from byteOut using toByteArray().
     *     Encrypt the payload:
     *         Use the encryptor cipher to perform encryption on the byte array obtained from serialization.
     *     Send the encrypted payload to the receiver using the endpoint object.
     *
     * Blocking Receive Method:
     *     Receive a message from the endpoint object.
     *     Decrypt the received payload:
     *         Use the decryptor cipher to decrypt the byte array received in the message.
     *     Deserialize the decrypted payload:
     *         Create a ByteArrayInputStream (byteIn) with the decrypted byte array to read from it.
     *         Create an ObjectInputStream (objectIn) to read the serialized object from byteIn.
     *         Read the deserialized object from objectIn.
     *     Create and return a new Message object with the deserialized payload and the original sender.
    */


  @Override
  public void send(java.net.InetSocketAddress receiver, java.io.Serializable payload) {
      try {
          // Create a byte array output stream
          ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

          // Create an object output stream
          ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);

          // Write the payload object to the object output stream
          objectOut.writeObject(payload);
          objectOut.flush();

          // Get the byte array of the serialized object
          byte[] payloadBytes = byteOut.toByteArray();

          // Encrypt the payload
          byte[] encrypted = this.encryptor.doFinal(payloadBytes);

          // Send the encrypted payload
          this.endpoint.send(receiver, encrypted);
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
  }

  @Override
  public Message blockingReceive() {
      Message message = this.endpoint.blockingReceive();
      try {
          // Decrypt the received payload
          byte[] decrypted = this.decryptor.doFinal((byte[]) message.getPayload());

          // Create a byte array input stream
          ByteArrayInputStream byteIn = new ByteArrayInputStream(decrypted);

          // Create an object input stream
          ObjectInputStream objectIn = new ObjectInputStream(byteIn);

          // Read the deserialized object from the object input stream
          Serializable payload = (Serializable) objectIn.readObject();

          return new Message(payload, message.getSender());
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
  }


  
  @Override
  public Message nonBlockingReceive() {
    Message message = this.endpoint.blockingReceive();
    try {
      // Decrypt the received payload
      byte[] decrypted = this.decryptor.doFinal((byte[]) message.getPayload());

      // Create a byte array input stream
      ByteArrayInputStream byteIn = new ByteArrayInputStream(decrypted);

      // Create an object input stream
      ObjectInputStream objectIn = new ObjectInputStream(byteIn);

      // Read the deserialized object from the object input stream
      Serializable payload = (Serializable) objectIn.readObject();

      return new Message(payload, message.getSender());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
