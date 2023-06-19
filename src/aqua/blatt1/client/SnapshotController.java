package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class SnapshotController implements ActionListener {
	private final TankModel tankmodel;
	
	public SnapshotController(TankModel tankmodel) {
		this.tankmodel = tankmodel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		//wait for snapshot to finish
		while(tankmodel.snapshotInProgress) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		JOptionPane.showMessageDialog(null, "Snapshotcount: " + this.tankmodel.globalSnapshotCounter, "Snapshot finished", JOptionPane.INFORMATION_MESSAGE);
	}
}
