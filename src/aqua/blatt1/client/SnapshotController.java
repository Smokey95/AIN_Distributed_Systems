package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class SnapshotController implements ActionListener {
	
	private final AquaGui parent;

	public SnapshotController(AquaGui parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		parent.getTankModel().initiateSnapshot();
		
		while(!parent.getTankModel().isSnapshot()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		
		//JOptionPane.showMessageDialog(parent, "Snapshot Count: " + parent.getTankModel().getGlobalSnapshot(), "Snapshot", JOptionPane.INFORMATION_MESSAGE);
		//System.out.println("Snapshot Count: " + parent.getTankModel().getGlobalSnapshot());
	}
}
