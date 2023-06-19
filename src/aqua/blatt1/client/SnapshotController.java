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

		System.out.println("Global Snapshot requested.");
	}
}
