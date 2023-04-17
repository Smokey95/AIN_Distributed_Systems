package aqua.blatt1.client;

import java.net.InetSocketAddress;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.Token;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.SnapshotToken;
import aqua.blatt2.PoisonPill;

public class ClientCommunicator {
	
	private final Endpoint endpoint;
	
	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	
	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public synchronized void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public synchronized void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public synchronized void handOff(FishModel fish, TankModel tankModel) {
			Direction direction = fish.getDirection();
			
			if (direction == Direction.LEFT)
				endpoint.send(tankModel.getLeftTank(), new HandoffRequest(fish));
			else if (direction == Direction.RIGHT)
				endpoint.send(tankModel.getRightTank(), new HandoffRequest(fish));
		}
		
		public synchronized void sendToken(InetSocketAddress neighbor) {
			endpoint.send(neighbor, new Token());
		}
		
		public synchronized void sendSnapshot(InetSocketAddress neighbor, SnapshotMarker snapshotMarker) {
			endpoint.send(neighbor, snapshotMarker);
		}
		
		public synchronized void sendSnapToken(InetSocketAddress neighbor, SnapshotToken snapshotToken) {
			endpoint.send(neighbor, snapshotToken);
		}
	}

	
	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());
				
				if (msg.getPayload() instanceof NeighborUpdate) {
					NeighborUpdate neighborUpdate = (NeighborUpdate) msg.getPayload();
					
					if (neighborUpdate.getDirection() == Direction.LEFT)
						tankModel.setLeftTank(neighborUpdate.getNeighbor());
					else if (neighborUpdate.getDirection() == Direction.RIGHT)
						tankModel.setRightTank(neighborUpdate.getNeighbor());
				}
				
				if (msg.getPayload() instanceof Token)
					tankModel.receiveToken();
				
				// If the message is a PoisonPill, stop the receiver
				if (msg.getPayload() instanceof PoisonPill)
					break;

				if (msg.getPayload() instanceof SnapshotMarker) {
					tankModel.receiveSnapshot((SnapshotMarker) msg.getPayload() , msg.getSender());
				}
				
				if (msg.getPayload() instanceof SnapshotToken) {
					tankModel.receiveSnapToken((SnapshotToken) msg.getPayload());
				}
			}
			
			System.out.println("Receiver stopped.");
			
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
