package aqua.blatt2;

import java.net.InetSocketAddress;

import javax.swing.JOptionPane;

import messaging.Endpoint;
import aqua.blatt1.common.Properties;

public class Poisoner {
	
	// Poisoner Endpoint
	private final Endpoint endpoint;
	
	// Broker Address
	private final InetSocketAddress broker;

	/*
	 * Constructor
	 */
	public Poisoner() {
		this.endpoint = new Endpoint();
		this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
	}

	/*
	 * Sends a PoisonPill to the broker.
	 * This will cause the broker to stop.
	 */
	public void sendPoison() {
		endpoint.send(broker, new PoisonPill());
	}

	public static void main(String[] args) {
		JOptionPane.showMessageDialog(null, "Press OK button to poison server.");
		new Poisoner().sendPoison();
	}
}
