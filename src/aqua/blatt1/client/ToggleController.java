/**
 * ToggleController.java
 * This class is used to toggle the location of a fish between the two tanks.
 */
package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
