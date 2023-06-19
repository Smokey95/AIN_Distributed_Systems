package aqua.blatt1.client;
import aqua.blatt1.rmi.AquaClient;
import aqua.blatt1.rmi.AquaBroker;

import java.net.InetSocketAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.SwingUtilities;

import aqua.blatt1.client.ClientCommunicator.ClientReceiver;
import aqua.blatt1.common.Properties;

import java.rmi.*;

public class Aqualife {
	
	public AquaBroker broker;

	public TankModel tankmodel;

	public Aqualife() {
		try {
			System.out.println("Client connecting to broker.");
			Registry registry = LocateRegistry.getRegistry();
			
			this.broker = (AquaBroker) registry.lookup(Properties.BROKER_NAME);
			
			//this.broker = (AquaBroker) Naming.lookup(Properties.BROKER_NAME);
			System.out.println("Client connected to broker: " + this.broker.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) throws RemoteException {
		
		Aqualife aqualife = new Aqualife();
		
		TankModel tankModel = new TankModel(aqualife.broker);
		
		SwingUtilities.invokeLater(new AquaGui(tankModel));
		
		System.out.println("Client started.");
		
		tankModel.run();
		
		System.out.println("Client stopped.");
		System.exit(0);
	}
}

//public static void main_old(String[] args) {
//	
//	ClientCommunicator communicator = new ClientCommunicator();
//	
//	TankModel tankModel = new TankModel(communicator.newClientForwarder());
//	
//	// Start ClientReceiver
//	ClientReceiver currentClient = communicator.newClientReceiver(tankModel);
//	currentClient.start();
//		
//	SwingUtilities.invokeLater(new AquaGui(tankModel));		
//	
//	// Run the tank gui. Pass the ClientCommunicator to the tank gui.
//	tankModel.run(currentClient);
//	
//	// If the tank gui is closed, stop the ClientReceiver and exit the program.
//	System.out.println("Client stopped.");	
//	System.exit(0);
//}