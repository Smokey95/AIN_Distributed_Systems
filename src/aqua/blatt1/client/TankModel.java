package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Timer;
import java.util.TimerTask;

import aqua.blatt1.client.ClientCommunicator.ClientReceiver;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.SnapshotToken;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected InetSocketAddress left_neighbor = null;
	protected InetSocketAddress right_neighbor = null;
	
	protected boolean token = false;
	protected Timer timer = new Timer();

	protected int localSnapshot = 0; // !< current fish count
	protected boolean isSnapshot = false; // is a snapshot currently running?
	
	protected int globalSnapshot = 0; // !< current fish count
	protected boolean initGlobalSnapshot = false; // !< is a global snapshot currently running?
	protected SnapshotToken snapshotToken = null; // !< snapshot token
	
	enum SnapshotState {
		IDLE, 	// !< no snapshot running
		LEFT, 	// !< snapshot running, waiting for left neighbor
		RIGHT, 	// !< snapshot running, waiting for right neighbor
		BOTH 		// !< snapshot running, waiting for both neighbors
	}
	
	protected SnapshotState snapshotState = SnapshotState.IDLE;

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		
		if(snapshotState == SnapshotState.LEFT) {
			if(fish.getDirection() != Direction.RIGHT) {
				localSnapshot += 1;
			}
		} else if(snapshotState == SnapshotState.RIGHT) {
			if(fish.getDirection() != Direction.LEFT) {
				localSnapshot += 1;
			}
		} else if(snapshotState == SnapshotState.BOTH) {
			localSnapshot += 1;
		}
		
		fish.setToStart();
		fishies.add(fish);
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

			if (fish.hitsEdge() && token)
				forwarder.handOff(fish, this);
			else if(fish.hitsEdge() && !token)
				fish.reverse();

			if (fish.disappears())
				it.remove();
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
	
	// global snapshot
	public synchronized void initiateSnapshot() {
		localSnapshot = fishies.size();
		isSnapshot = true;
		snapshotState = SnapshotState.BOTH;
		initGlobalSnapshot = true;
		
		forwarder.sendSnapshot(right_neighbor, new SnapshotMarker());
		forwarder.sendSnapshot(left_neighbor, new SnapshotMarker());
		forwarder.sendSnapToken(left_neighbor, new SnapshotToken());
	}
	
	public synchronized void receiveSnapshot(SnapshotMarker marker, InetSocketAddress sender) {
		
		System.out.println("Received snapshot from " + sender);
		
		if(!isSnapshot) {
			isSnapshot = true;
			localSnapshot = fishies.size();
			
			if(sender.equals(left_neighbor)) {
				snapshotState = SnapshotState.RIGHT;
				forwarder.sendSnapshot(right_neighbor, new SnapshotMarker());
			} else if(sender.equals(right_neighbor)) {
				snapshotState = SnapshotState.LEFT;
				forwarder.sendSnapshot(left_neighbor, new SnapshotMarker());
			}
			
		} else {
			
			Direction direction = sender == left_neighbor ? Direction.LEFT : Direction.RIGHT;
			
			if(snapshotState == SnapshotState.RIGHT && direction == Direction.LEFT) {
				System.out.println("Both snapshots received 1");
				snapshotState = SnapshotState.IDLE;
				isSnapshot = false;
			}
			
			if(snapshotState == SnapshotState.LEFT && direction == Direction.RIGHT) {
				System.out.println("Both snapshots received 2");
				snapshotState = SnapshotState.IDLE;
				isSnapshot = false;
			}
			
			if(snapshotState == SnapshotState.BOTH && direction == Direction.LEFT) {
				System.out.println("Left snapshot received");
				snapshotState = SnapshotState.RIGHT;
			} else if(snapshotState == SnapshotState.BOTH && direction == Direction.RIGHT) {
				System.out.println("Right snapshot received");
				snapshotState = SnapshotState.LEFT;
			}
			
			if(snapshotState == SnapshotState.IDLE) {
				System.out.println("[TankID: " + id + "]" +  "Local snapshot: " + localSnapshot );
				if(snapshotToken != null) {
					snapshotToken.addLocalCount(localSnapshot);
					localSnapshot = 0;
					forwarder.sendSnapToken(left_neighbor, snapshotToken);
				}
			}
		} 
	}
	
	public boolean isSnapshot() {
		return isSnapshot;
	}
	
	public synchronized void receiveSnapToken(SnapshotToken token){
		
		if(this.initGlobalSnapshot && snapshotState == SnapshotState.IDLE) {		
			this.initGlobalSnapshot = false;
			token.addLocalCount(localSnapshot);
			globalSnapshot = token.getGlobalSnapCount();
		} else {
			this.snapshotToken = token;
		}
		
		if(snapshotState == SnapshotState.IDLE) {
			token.addLocalCount(localSnapshot);
			localSnapshot = 0;
			forwarder.sendSnapToken(left_neighbor, token);
		}
	}
	
	public int getGlobalSnapshot() {
		return globalSnapshot;
	}
}