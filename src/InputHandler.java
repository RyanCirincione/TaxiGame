import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class InputHandler implements KeyListener, MouseListener, MouseMotionListener {
	public boolean right = false, up = false, left = false, down = false;
	public int selectedTrack = -1;
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
			down = true;
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
		case KeyEvent.VK_1:
			if (selectedTrack != 0 && TaxiGame.trackInventory[0] > 0) {
				selectedTrack = 0;
			} else {
				selectedTrack = -1;
			}
			break;
		case KeyEvent.VK_2:
			if (selectedTrack != 1 && TaxiGame.trackInventory[1] > 0) {
				selectedTrack = 1;
			} else {
				selectedTrack = -1;
			}
			break;
		case KeyEvent.VK_3:
			if (selectedTrack != 2 && TaxiGame.trackInventory[2] > 0) {
				selectedTrack = 2;
			} else {
				selectedTrack = -1;
			}
			break;
		case KeyEvent.VK_4:
			if (selectedTrack != 3 && TaxiGame.trackInventory[3] > 0) {
				selectedTrack = 3;
			} else {
				selectedTrack = -1;
			}
			break;
		case KeyEvent.VK_5:
			if (selectedTrack != 4 && TaxiGame.trackInventory[4] > 0) {
				selectedTrack = 4;
			} else {
				selectedTrack = -1;
			}
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
		int x = (int) (e.getX() + TaxiGame.camera.x - TaxiGame.S_WIDTH / 2),
				y = (int) (e.getY() + TaxiGame.camera.y - TaxiGame.S_HEIGHT / 2), TILE = TaxiGame.TILE_SIZE;
		int xm = x % TILE, ym = y % TILE, xt = x / TILE, yt = y / TILE;

		if (selectedTrack != -1 && TaxiGame.tracks[xt][yt] == null) {
			Track t = null;

			switch (selectedTrack) {
			case 0:
				t = new Track(xm >= ym && xm + ym >= TILE, xm >= ym && xm + ym < TILE, xm < ym && xm + ym < TILE,
						xm < ym && xm + ym >= TILE);
				break;
			case 1:
				t = new Track(xm >= ym && xm + ym >= TILE, xm >= ym && xm + ym < TILE, xm < ym && xm + ym < TILE,
						xm < ym && xm + ym >= TILE);
				t.left = t.right = t.right || t.left;
				t.down = t.up = t.up || t.down;
				break;
			case 2:
				t = new Track(xm >= TILE / 2, ym < TILE / 2, xm < TILE / 2, ym >= TILE / 2);
				break;
			case 3:
				t = new Track(xm >= ym || xm + ym >= TILE, xm >= ym || xm + ym < TILE, xm < ym || xm + ym < TILE,
						xm < ym || xm + ym >= TILE);
				break;
			case 4:
				t = new Track(true, true, true, true);
				break;
			}

			// if ((t.right == (xt + 1 < TaxiGame.tracks.length
			// && (TaxiGame.tracks[xt + 1][yt] == null || TaxiGame.tracks[xt +
			// 1][yt].left)))
			// && (t.up == (yt - 1 >= 0
			// && (TaxiGame.tracks[xt][yt - 1] == null || TaxiGame.tracks[xt][yt
			// - 1].down)))
			// && (t.left == (xt - 1 >= 0
			// && (TaxiGame.tracks[xt - 1][yt] == null || TaxiGame.tracks[xt -
			// 1][yt].right)))
			// && (t.down == (yt + 1 <= TaxiGame.tracks[xt].length
			// && (TaxiGame.tracks[xt][yt + 1] == null || TaxiGame.tracks[xt][yt
			// + 1].up)))) {
			// TaxiGame.tracks[xt][yt] = t;
			// }

			if (((xt + 1 >= TaxiGame.tracks.length && !t.right) || (xt + 1 < TaxiGame.tracks.length
					&& (TaxiGame.tracks[xt + 1][yt] == null || t.right == TaxiGame.tracks[xt + 1][yt].left)))
					&& ((yt - 1 < 0 && !t.up) || (yt - 1 >= 0
							&& (TaxiGame.tracks[xt][yt - 1] == null || t.up == TaxiGame.tracks[xt][yt - 1].down)))
					&& ((xt - 1 < 0 && !t.left) || (xt - 1 >= 0
							&& (TaxiGame.tracks[xt - 1][yt] == null || t.left == TaxiGame.tracks[xt - 1][yt].right)))
					&& ((yt + 1 >= TaxiGame.tracks[xt].length && !t.down) || (yt + 1 < TaxiGame.tracks[xt].length
							&& (TaxiGame.tracks[xt][yt + 1] == null || t.down == TaxiGame.tracks[xt][yt + 1].up)))) {
				TaxiGame.tracks[xt][yt] = t;
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
