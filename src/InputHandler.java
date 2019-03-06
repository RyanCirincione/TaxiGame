import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class InputHandler implements KeyListener, MouseListener, MouseMotionListener {
	public boolean right = false, up = false, left = false, down = false, help = false;
	public Vector mouse = new Vector();

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_D:
			right = true;
			break;
		case KeyEvent.VK_W:
			up = true;
			break;
		case KeyEvent.VK_A:
			left = true;
			break;
		case KeyEvent.VK_S:
			if (!down && TaxiGame.taxi.velocity.length() < 0.0001 && !TaxiGame.paused) {
				TaxiGame.cameraAngle += Math.PI;
			}
			down = true;
			break;
		case KeyEvent.VK_H:
			help = true;
			break;
		}
	}

	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_D:
			right = false;
			break;
		case KeyEvent.VK_W:
			up = false;
			break;
		case KeyEvent.VK_A:
			left = false;
			break;
		case KeyEvent.VK_S:
			down = false;
			break;
		case KeyEvent.VK_H:
			help = false;
			break;
		case KeyEvent.VK_ESCAPE:
			TaxiGame.paused = !TaxiGame.paused;
			break;
		case KeyEvent.VK_SPACE:
			if (TaxiGame.taxi.velocity.length() < 0.0000001 && TaxiGame.taxi.gas < 0.000001) {
				TaxiGame.mainMenu = true;
			}
			break;
		case KeyEvent.VK_R:
			System.out.println("Restart");
			TaxiGame.mainMenu = false;
			synchronized (TaxiGame.generationLock) {
				TaxiGame.startNewGame();
			}
			break;
		case KeyEvent.VK_BACK_SPACE:
			TaxiGame.mainMenu = true;
			break;
		case KeyEvent.VK_Z:
			if (TaxiGame.zoom < TaxiGame.MAX_ZOOM) TaxiGame.zoom += 0.25;
			break;
		case KeyEvent.VK_X:
			if (TaxiGame.zoom > TaxiGame.MIN_ZOOM) TaxiGame.zoom -= 0.25;
			break;
		case KeyEvent.VK_C:
			TaxiGame.zoom = 1;
			break;
		case KeyEvent.VK_V:
			TaxiGame.zoom = TaxiGame.MIN_ZOOM;
			break;
		case KeyEvent.VK_I:
			TaxiGame.trackStock++;
			break;
		case KeyEvent.VK_O:
			TaxiGame.taxi.gas = TaxiGame.taxi.maxGas;
			break;
		case KeyEvent.VK_P:
			TaxiGame.money += 100;
			break;
		}
	}

	public void keyTyped(KeyEvent e) {

	}

	public void mouseClicked(MouseEvent e) {

	}

	public void mouseEntered(MouseEvent e) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {

	}

	public void mouseReleased(MouseEvent e) {
		if (TaxiGame.mainMenu && TaxiGame.newGameButton.contains(e.getX(), e.getY())) {
			TaxiGame.mainMenu = false;
			synchronized (TaxiGame.generationLock) {
				TaxiGame.startNewGame();
			}
		}
	}

	public void mouseDragged(MouseEvent e) {
		this.mouseMoved(e);
	}

	public void mouseMoved(MouseEvent e) {
		mouse.set(e.getX(), e.getY());
	}

	public String toString() {
		return "Right: " + right + "\nUp: " + up + "\nLeft: " + left + "\nDown: " + down;
	}
}
