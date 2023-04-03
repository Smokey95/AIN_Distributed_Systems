package aqua.blatt1.client;

import javax.swing.SwingUtilities;

import aqua.blatt1.client.ClientCommunicator.ClientReceiver;

public class Aqualife {

	public static void main(String[] args) {
		
		ClientCommunicator communicator = new ClientCommunicator();
		
		TankModel tankModel = new TankModel(communicator.newClientForwarder());
		
		// Start ClientReceiver
		ClientReceiver currentClient = communicator.newClientReceiver(tankModel);
		currentClient.start();
			
		SwingUtilities.invokeLater(new AquaGui(tankModel));		
		
		// Run the tank gui. Pass the ClientCommunicator to the tank gui.
		tankModel.run(currentClient);
		
		// If the tank gui is closed, stop the ClientReceiver and exit the program.
		System.out.println("Client stopped.");	
		System.exit(0);

	}
}
