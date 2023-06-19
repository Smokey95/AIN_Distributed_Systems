package aqua.blatt1.client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Timer;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.rmi.AquaBroker;
import aqua.blatt1.rmi.AquaClient;

public class TankModel extends Observable implements Iterable<FishModel>, AquaClient{

	public static final int WIDTH = 600;																										//! Width of the tank				
	public static final int HEIGHT = 350;																										//! Height of the tank	
	protected static final int MAX_FISHIES = 5;																							//! Maximum number of fish in the tank
	protected static final Random rand = new Random(); 																			//! Random number generator
	
	protected volatile String id = null;																										//! ID of the tank
	protected final Set<FishModel> fishies;																									//! Set of fish in the tank
	protected int fishCounter = 0;
	protected int fadingFishCounter = 0;
	protected boolean token = false;
	protected Timer timer = new Timer();

	protected Timer leaseTimer = new Timer();
		
	protected AquaBroker broker;
	public AquaClient client;
	
	public TankModel(AquaBroker broker) throws RemoteException {

		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		
		this.broker = broker;
		
		this.client = (AquaClient) UnicastRemoteObject.exportObject(this, 0);
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
			//fishiesMasterList.put(fish.getId(), FishState.HERE);																//! Add fish to master list
		}
	}

	/**
	 * Receives a fish from another tank.
	 * @param fish the fish to receive
	 */
	@Override
	public void receiveFish(FishModel fish) throws RemoteException {
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

			if (fish.hitsEdge()) {
				//forwarder.handOff(fish, this);
				//TODO handoff fish via broker instance

				//if(fish.getDirection().equals(Direction.LEFT)) {
				//	fishiesMasterList.put(fish.getId(), FishState.LEFT);
				//} else if(fish.getDirection().equals(Direction.RIGHT)) {
				//	fishiesMasterList.put(fish.getId(), FishState.RIGHT);
				//}
				try{
					broker.handOffFish(fish, this);
				} catch (RemoteException e) {
					e.printStackTrace();
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

	protected void run() {
		// Register with the AquaBroker
		try {
			System.out.println("Registering with broker");
			System.out.println(this.getClass().toString());
			this.broker.register(this.client);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
			System.out.println(id + "Terminated");
		}
	}
}