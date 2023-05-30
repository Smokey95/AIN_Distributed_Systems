package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;

import aqua.blatt1.client.ClientCommunicator.ClientReceiver;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.SnapshotToken;

import aqua.blatt1.common.msgtypes.SnapshotToken;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;																										//! Width of the tank				
	public static final int HEIGHT = 350;																										//! Height of the tank	
	protected static final int MAX_FISHIES = 5;																							//! Maximum number of fish in the tank
	protected static final Random rand = new Random(); 																			//! Random number generator
	
	protected volatile String id = null;																										//! ID of the tank
	protected final Set<FishModel> fishies;																									//! Set of fish in the tank
	protected final Map<String, InetSocketAddress> homeAgent;																//! Map of home agents
	protected int fishCounter = 0;
	protected int fadingFishCounter = 0;

	protected final ClientCommunicator.ClientForwarder forwarder;
	protected InetSocketAddress left_neighbor = null;
	protected InetSocketAddress right_neighbor = null;
	
	protected boolean token = false;
	protected Timer timer = new Timer();

	protected Timer leaseTimer = new Timer();

	protected SnapshotState snapshotState = SnapshotState.IDLE;
	protected boolean isInitializer = false;
	protected int localSnapshotCounter = 0;
	public int globalSnapshotCounter = 0;
	
	protected boolean snapshotInProgress = false;

	protected final Map<String, FishState> fishiesMasterList;

	enum SnapshotState {
		IDLE,
		LEFT,
		RIGHT,
		BOTH
	}
	
	enum FishState {
		HERE,
		LEFT,
		RIGHT
	}
	
	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.homeAgent = new ConcurrentHashMap<String, InetSocketAddress>();
		this.fishiesMasterList = new ConcurrentHashMap<String, FishState>();
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(RegisterResponse response) {
		
		// Check if this is the first registration
		if(this.id == null){
			this.id = response.getId();
			newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
		}
		
		//Create new timer for lease
		TimerTask lease_task = new TimerTask() {
			@Override
			public void run() {
				forwarder.register();
			}
		};
		
		//Schedule timer
		leaseTimer.schedule(lease_task, response.getLeaseTime());
	}

	/**
	 * Adds a fish to the tank at the specified position.
	 * @param x x-coordinate of the new fish
	 * @param y y-coordinate of the new fish
	 */
	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);																																	//! Add fish to tank
			homeAgent.put(fish.getId(), null);																						//! Add fish to home agent map
			fishiesMasterList.put(fish.getId(), FishState.HERE);																//! Add fish to master list
		}
	}

	/**
	 * Receives a fish from another tank.
	 * @param fish the fish to receive
	 */
	synchronized void receiveFish(FishModel fish) {
		
		// Check if there is a snapshot in progress
		if(snapshotState.equals(SnapshotState.LEFT)) {
			if (fish.getDirection().equals(Direction.LEFT)) {
				localSnapshotCounter++;
			}
		} else if(snapshotState.equals(SnapshotState.RIGHT)) {
			if (fish.getDirection().equals(Direction.RIGHT)) {
				localSnapshotCounter++;
			}
		} else if(snapshotState.equals(SnapshotState.BOTH) 
		|| snapshotState.equals(SnapshotState.IDLE)) {
			localSnapshotCounter++;
		}
		
		// Check if the fish is home based in this tank and update its position (null cause it is home based)
		if(homeAgent.containsKey(fish.getId())) {
			homeAgent.put(fish.getId(), null);
		} else {
			// Request home agent of fish and update its position
			forwarder.sendNameResolutionRequest(fish.getTankId(), fish.getId());
		}
		
		fish.setToStart();
		fishies.add(fish);
		fishiesMasterList.put(fish.getId(), FishState.HERE);
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge() && token) {
				forwarder.handOff(fish, this);
				
				if(fish.getDirection().equals(Direction.LEFT)) {
					fishiesMasterList.put(fish.getId(), FishState.LEFT);
				} else if(fish.getDirection().equals(Direction.RIGHT)) {
					fishiesMasterList.put(fish.getId(), FishState.RIGHT);
				}

				fadingFishCounter++;
			}
			else if(fish.hitsEdge() && !token)
				fish.reverse();

			if (fish.disappears()){
				it.remove();
				fadingFishCounter--;
			}
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}


	/**
	 * Runs the tank model. This method is called by the client receiver thread.
	 * @param cr the client receiver to check for termination
	 */
	protected void run(ClientReceiver cr) {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted() && cr.isAlive()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
			System.out.println(id + "Terminated");
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}
	
	public synchronized void neighborUpdate(InetSocketAddress left_neighbor, InetSocketAddress right_neighbor) {
		this.left_neighbor = left_neighbor;
		this.right_neighbor = right_neighbor;
	}
	
	public synchronized void setLeftTank(InetSocketAddress left_neighbor) {
		this.left_neighbor = left_neighbor;
	}
	
	public synchronized void setRightTank(InetSocketAddress right_neighbor) {
		this.right_neighbor = right_neighbor;
	}
	
	public synchronized InetSocketAddress getLeftTank() {
		return left_neighbor;
	}
	
	public synchronized InetSocketAddress getRightTank() {
		return right_neighbor;
	}
	
	public synchronized boolean hasToken() {
		return token;
	}
	
	public synchronized void receiveToken() {
		token = true;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				sendToken();
			}
		};
		timer.schedule(task, 2000);
	}
	
	private synchronized void sendToken() {
		token = false;
		forwarder.sendToken(right_neighbor);
	}
	
	public synchronized void initiateSnapshot() {
		this.snapshotState = SnapshotState.BOTH;
		this.snapshotInProgress = true;

		this.isInitializer = true;
		this.localSnapshotCounter = fishies.size() - fadingFishCounter;

		forwarder.sendSnapshotMarker(left_neighbor);
		forwarder.sendSnapshotMarker(right_neighbor);		
	}

	public synchronized void receiveSnapshotMarker(Direction dir) {
		//case idle
		if(this.snapshotState.equals(SnapshotState.IDLE)) {

			this.localSnapshotCounter = fishies.size() - fadingFishCounter;

			if(dir.equals(Direction.LEFT)){
				this.snapshotState = SnapshotState.RIGHT;
				forwarder.sendSnapshotMarker(right_neighbor);
			}else{
				this.snapshotState = SnapshotState.LEFT;
				forwarder.sendSnapshotMarker(left_neighbor);
			}

		// case both
		} else if(this.snapshotState.equals(SnapshotState.BOTH)) {
			if(dir.equals(Direction.LEFT)){
				this.snapshotState = SnapshotState.RIGHT;
			}else{
				this.snapshotState = SnapshotState.LEFT;
			}

		//case left or right
		} else {
			this.snapshotState = SnapshotState.IDLE;
			if(!this.isInitializer) {
				forwarder.sendSnapshotMarker(dir.equals(Direction.LEFT) ? right_neighbor : left_neighbor);
				System.out.println("Snapshot complete (Non-Initializer), Fishcount: " + this.localSnapshotCounter);
			} else {
				//Snapshot complete
				//this.isInitializer = false;
				forwarder.sendSnapshotToken(new SnapshotToken(), left_neighbor);
				System.out.println("Snapshot complete (Initializer), Fishcount: " + this.localSnapshotCounter);

			}
		}
	}

	public synchronized void receiveSnapshotToken(SnapshotToken token) {
		System.out.println(token.getGlobalCounter());
		if (!this.isInitializer) {
			token.addGlobalCounter(this.localSnapshotCounter);
			forwarder.sendSnapshotToken(token, left_neighbor);
		} else if (this.isInitializer) {
			token.addGlobalCounter(this.localSnapshotCounter);
			this.globalSnapshotCounter = token.getGlobalCounter();
			this.snapshotInProgress = false;	
			this.isInitializer = false;
		}
	}
	
	public synchronized void receiveNameResolutionResponse(String requestID, InetSocketAddress homeAgentAddress) {
		
		// inform home agent of fish
		// @TODO: Implement

	}
	
	public synchronized void locateFishGlobally(String fishId) {
		
		FishModel fish_tmp = null;
		
		//check if fishID is present (FishState.HERE)
		if(fishiesMasterList.containsKey(fishId)) {
			//if present, return fish
			if(fishiesMasterList.get(fishId).equals(FishState.HERE)) {
				System.out.println("Fish " + fishId + " is here");
				for(FishModel fish : fishies) {
					if(fish.getId().equals(fishId)) {
						fish.toggle();
					}
				}
			} else if(fishiesMasterList.get(fishId).equals(FishState.LEFT)) {
				System.out.println("Fish " + fishId + " was swimming out of the pot to the left");
				forwarder.locateFishie(left_neighbor, fishId);
			} else if(fishiesMasterList.get(fishId).equals(FishState.RIGHT)) {
				System.out.println("Fish " + fishId + " was swimming out of the pot to the right");
				forwarder.locateFishie(right_neighbor, fishId);
			}
			
		} else {
			System.out.println("This should not happen, fish was never seen");
		}
	}
}