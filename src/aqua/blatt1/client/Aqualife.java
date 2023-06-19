package aqua.blatt1.client;

import aqua.blatt1.rmi.AquaBroker;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.SwingUtilities;
import aqua.blatt1.common.Properties;

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