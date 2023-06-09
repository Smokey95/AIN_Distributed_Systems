
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
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.LocationUpdate;
import aqua.blatt1.common.msgtypes.LocationRequest;
import aqua.blatt1.common.SecureEndpoint;
import aqua.blatt2.PoisonPill;

public class ClientCommunicator {
	
	private final SecureEndpoint endpoint;
	
	public ClientCommunicator() {
		endpoint = new SecureEndpoint();
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
		
		public synchronized void sendSnapshotMarker(InetSocketAddress neighbor) {
			endpoint.send(neighbor, new SnapshotMarker());
		}

		public synchronized void sendSnapshotToken(SnapshotToken token, InetSocketAddress neighbor) {
			endpoint.send(neighbor, token);
		}
		
		public synchronized void sendNameResolutionRequest(String tankId, String requestId) {
			endpoint.send(broker, new NameResolutionRequest(tankId, requestId));
		}
		
		public synchronized void sendLocationUpdate(String fishId, InetSocketAddress homeAgentAddress, InetSocketAddress newTankAddress) {
			endpoint.send(homeAgentAddress, new LocationUpdate(fishId, newTankAddress));
		}
		
		public synchronized void locateFishie(InetSocketAddress neighbor, String fish) {
			endpoint.send(neighbor, new LocationRequest(fish));
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

				if (msg.getPayload() instanceof RegisterResponse) {
					tankModel.onRegistration((RegisterResponse) msg.getPayload());
				}

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

				if (msg.getPayload() instanceof SnapshotMarker)
					if(msg.getSender().equals(tankModel.left_neighbor)){
						tankModel.receiveSnapshotMarker(Direction.LEFT);
					} else {
						tankModel.receiveSnapshotMarker(Direction.RIGHT);
					}

				if (msg.getPayload() instanceof SnapshotToken) {
					tankModel.receiveSnapshotToken((SnapshotToken) msg.getPayload());
				}
					
				if (msg.getPayload() instanceof NameResolutionResponse) {
					NameResolutionResponse response = (NameResolutionResponse) msg.getPayload();
					tankModel.receiveNameResolutionResponse(response.getRequestId(), response.getAddress());
				}

				if (msg.getPayload() instanceof LocationRequest) {
					LocationRequest req = (LocationRequest) msg.getPayload();
					tankModel.locateFishGlobally(req.getFish());
				}
				
				if (msg.getPayload() instanceof LocationUpdate) {
					LocationUpdate update = (LocationUpdate) msg.getPayload();
					tankModel.receiveLocationUpdate(update.getFishId(), update.newTankAddress());
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
