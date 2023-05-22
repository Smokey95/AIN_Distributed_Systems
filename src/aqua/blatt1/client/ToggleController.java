package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class ToggleController implements ActionListener {
	private final String fish_id;
    private final TankModel tankmodel;
	
	public ToggleController(String fish_id, TankModel tankmodel) {
		this.fish_id = fish_id;
        this.tankmodel = tankmodel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		tankmodel.locateFishGlobally(fish_id);
	}
}
