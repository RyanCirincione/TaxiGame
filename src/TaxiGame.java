import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TaxiGame extends JPanel {
	private static final long serialVersionUID = -8396459457708163217L;
	public static JFrame frame;

	public static void main(String[] args) {
		frame = new JFrame("Taxi Game");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		TaxiGame panel = new TaxiGame();
		frame.getContentPane().add(panel);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				panel.tick();
				panel.repaint();
			}
		}, 0, 1000 / 60);
	}

	public static final int S_WIDTH = 800, S_HEIGHT = 600, TILE_SIZE = 64;
	public static boolean[][] tracks;
	InputHandler input;
	BufferedImage tracksImg;
	Vector taxiLocation, taxiVelocity;
	ArrayList<Vector> clients, destinations;
	ArrayList<Vector[]> completedClients;

	public TaxiGame() {
		completedClients = new ArrayList<Vector[]>();
		destinations = new ArrayList<Vector>();
		clients = new ArrayList<Vector>();
		taxiLocation = new Vector(2.5 * TILE_SIZE, 2.5 * TILE_SIZE);
		taxiVelocity = new Vector();
		input = new InputHandler();
		tracks = new boolean[12][9];
		int[][] literalTrack = new int[][] { { 0, 0, 0, 1, 1, 1, 0, 0, 0 }, { 0, 0, 1, 1, 0, 1, 0, 0, 0 },
				{ 0, 0, 1, 0, 1, 1, 1, 0, 0 }, { 0, 0, 1, 0, 1, 0, 1, 1, 1 }, { 0, 1, 1, 0, 1, 0, 1, 0, 1 },
				{ 0, 1, 0, 0, 1, 1, 1, 1, 1 }, { 0, 1, 0, 0, 0, 0, 1, 0, 0 }, { 0, 1, 1, 1, 1, 0, 1, 0, 0 },
				{ 0, 0, 1, 0, 1, 1, 1, 1, 0 }, { 0, 0, 1, 1, 1, 0, 0, 1, 0 }, { 0, 0, 0, 0, 1, 1, 1, 1, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0 } };

		for (int x = 0; x < literalTrack.length; x++) {
			for (int y = 0; y < literalTrack[x].length; y++) {
				if (literalTrack[x][y] != 0) {
					tracks[x][y] = true;
				}
			}
		}

		try {
			tracksImg = ImageIO.read(new File("res/tracks.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.setFocusable(true);
		this.requestFocus();
		this.addKeyListener(input);

		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}

	public void tick() {
		// This method is for shoving all the hell that is and will be the
		// controlling of the taxi's position relative to the tracks
		movementHell();

		// Always have 3 clients to pick up
		while (clients.size() + destinations.size() < 4) {
			Vector v = new Vector(Math.random() * tracks.length * TILE_SIZE,
					Math.random() * tracks[0].length * TILE_SIZE);
			if (tracks[(int) (v.x / TILE_SIZE)][(int) (v.y / TILE_SIZE)]) {
				clients.add(v);
			}
		}

		// Pick up clients (when moving slowly)
		if (taxiVelocity.length() < 0.5) {
			for (int i = 0; i < clients.size(); i++) {
				double d = taxiLocation.distance2(clients.get(i));
				if (d < Math.pow(TILE_SIZE * 3 / 4, 2)) {
					if (d < 5 * 5) {
						clients.remove(i--);
						while (true) {
							Vector v = new Vector(Math.random() * tracks.length * TILE_SIZE,
									Math.random() * tracks[0].length * TILE_SIZE);
							if (tracks[(int) (v.x / TILE_SIZE)][(int) (v.y / TILE_SIZE)]) {
								destinations.add(v);
								break;
							}
						}
					} else {
						clients.get(i).set(clients.get(i).lerp(taxiLocation, 1));
					}
				}
			}
		}

		// Drop off clients (when moving slowly)
		if (taxiVelocity.length() < 0.5) {
			for (int i = 0; i < destinations.size(); i++) {
				double d = taxiLocation.distance2(destinations.get(i));
				if (d < Math.pow(TILE_SIZE / 1.5, 2)) {
					completedClients.add(new Vector[] { taxiLocation.clone(),
							destinations.get(i).clone().minus(taxiLocation).setLength(0.3), new Vector(255, 0) });
					destinations.remove(i--);
				}
			}
		}

		// Update completed clients
		for (int i = 0; i < completedClients.size(); i++) {
			completedClients.get(i)[0] = completedClients.get(i)[0].add(completedClients.get(i)[1]);

			completedClients.get(i)[2].x -= 2.5;
			if (completedClients.get(i)[2].x <= 0) {
				completedClients.remove(i--);
			}
		}
	}

	public void paintComponent(Graphics gr) {
		Graphics2D g = (Graphics2D) gr;
		super.paintComponent(g);

		// Draw tracks
		g.setColor(Color.white);
		for (int x = 0; x < tracks.length; x++) {
			for (int y = 0; y < tracks[x].length; y++) {
				if (tracks[x][y]) {
					int sx = ((y > 0 && tracks[x][y - 1] ? 2 : 0)
							+ (x + 1 < tracks.length && tracks[x + 1][y] ? 1 : 0));
					int sy = ((x > 0 && tracks[x - 1][y] ? 2 : 0)
							+ (y + 1 < tracks[x].length && tracks[x][y + 1] ? 1 : 0));
					g.drawImage(tracksImg, x * TILE_SIZE, y * TILE_SIZE, (x + 1) * TILE_SIZE, (y + 1) * TILE_SIZE,
							sx * TILE_SIZE, sy * TILE_SIZE, (sx + 1) * TILE_SIZE, (sy + 1) * TILE_SIZE, null);
				} else {
					g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
				}
			}
		}

		// Draw taxi
		g.setColor(Color.yellow);
		g.fillOval((int) (taxiLocation.x - 5), (int) (taxiLocation.y - 5), 10, 10);

		// Draw clients
		g.setColor(Color.orange);
		for (Vector c : clients) {
			g.fillOval((int) (c.x - 2), (int) (c.y - 2), 5, 5);
			g.drawOval((int) (c.x - TILE_SIZE * 3 / 4), (int) (c.y - TILE_SIZE * 3 / 4), TILE_SIZE * 3 / 2,
					TILE_SIZE * 3 / 2);
		}

		// Draw completed clients
		for (Vector[] c : completedClients) {
			g.setColor(new Color(255, 165, 0, (int) c[2].x));
			g.fillOval((int) (c[0].x - 2), (int) (c[0].y - 2), 5, 5);
		}

		// Draw destinations
		g.setColor(new Color(200, 0, 200, 128));
		for (Vector d : destinations) {
			g.fillOval((int) (d.x - TILE_SIZE / 1.5), (int) (d.y - TILE_SIZE / 1.5), (int) (TILE_SIZE / 1.5 * 2),
					(int) (TILE_SIZE / 1.5 * 2));
		}
	}

	private static boolean inBounds(double x, double y) {
		return x >= 0 && y >= 0 && x / TILE_SIZE < tracks.length
				&& y / TILE_SIZE < tracks[(int) (x / TILE_SIZE)].length;
	}

	private void movementHell() {
		if (input.right) {
			// Taxi is on horizontal track (or switching to), and can move right
			if (((taxiLocation.y - TILE_SIZE / 2 + 1) % TILE_SIZE < 2 || (int) ((taxiLocation.y - TILE_SIZE / 2 - 1)
					/ TILE_SIZE) != (int) ((taxiLocation.y + taxiVelocity.y - TILE_SIZE / 2) / TILE_SIZE))
					&& inBounds(taxiLocation.x + TILE_SIZE / 2, taxiLocation.y)
					&& tracks[(int) ((taxiLocation.x + TILE_SIZE / 2) / TILE_SIZE)][(int) (taxiLocation.y
							/ TILE_SIZE)]) {
				// Taxi is currently moving vertically
				if (Math.abs(taxiVelocity.y) > 0.00001) {
					taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
					taxiVelocity.x = Math.abs(taxiVelocity.y);
					taxiVelocity.y = 0;
				}
				taxiVelocity.x += 0.05;
			}
		}

		if (input.up) {
			// Taxi is on vertical track (or switching to), and can move up
			if (((taxiLocation.x - TILE_SIZE / 2 + 1) % TILE_SIZE < 2 || (int) ((taxiLocation.x - TILE_SIZE / 2 - 1)
					/ TILE_SIZE) != (int) ((taxiLocation.x + taxiVelocity.x - TILE_SIZE / 2) / TILE_SIZE))
					&& inBounds(taxiLocation.x, taxiLocation.y - TILE_SIZE / 2 - 1)
					&& tracks[(int) (taxiLocation.x / TILE_SIZE)][(int) ((taxiLocation.y - TILE_SIZE / 2 - 1)
							/ TILE_SIZE)]) {
				// Taxi is currently moving horizontally
				if (Math.abs(taxiVelocity.x) > 0.00001) {
					taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
					taxiVelocity.y = -Math.abs(taxiVelocity.x);
					taxiVelocity.x = 0;
				}
				taxiVelocity.y -= 0.05;
			}
		}
		if (input.left) {
			// Taxi is on horizontal track (or switching to), and can move left
			if (((taxiLocation.y - TILE_SIZE / 2 + 1) % TILE_SIZE < 2 || (int) ((taxiLocation.y - TILE_SIZE / 2 - 1)
					/ TILE_SIZE) != (int) ((taxiLocation.y + taxiVelocity.y - TILE_SIZE / 2) / TILE_SIZE))
					&& inBounds(taxiLocation.x - TILE_SIZE / 2 - 1, taxiLocation.y)
					&& tracks[(int) ((taxiLocation.x - TILE_SIZE / 2 - 1) / TILE_SIZE)][(int) (taxiLocation.y
							/ TILE_SIZE)]) {
				// Taxi is currently moving vertically
				if (Math.abs(taxiVelocity.y) > 0.00001) {
					taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
					taxiVelocity.x = -Math.abs(taxiVelocity.y);
					taxiVelocity.y = 0;
				}
				taxiVelocity.x -= 0.05;
			}
		}

		if (input.down) {
			// Taxi is on vertical track (or switching to), and can move down
			if (((taxiLocation.x - TILE_SIZE / 2 + 1) % TILE_SIZE < 2 || (int) ((taxiLocation.x - TILE_SIZE / 2 - 1)
					/ TILE_SIZE) != (int) ((taxiLocation.x + taxiVelocity.x - TILE_SIZE / 2) / TILE_SIZE))
					&& inBounds(taxiLocation.x, taxiLocation.y + TILE_SIZE / 2)
					&& tracks[(int) (taxiLocation.x / TILE_SIZE)][(int) ((taxiLocation.y + TILE_SIZE / 2)
							/ TILE_SIZE)]) {
				// Taxi is currently moving horizontally
				if (Math.abs(taxiVelocity.x) > 0.00001) {
					taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
					taxiVelocity.y = Math.abs(taxiVelocity.x);
					taxiVelocity.x = 0;
				}
				taxiVelocity.y += 0.05;
			}
		}

		// Block movement right if there is no track to the right
		if (taxiVelocity.x > 0) {
			if (!(inBounds(taxiLocation.x + taxiVelocity.x + TILE_SIZE / 2, taxiLocation.y)
					&& tracks[(int) ((taxiLocation.x + taxiVelocity.x + TILE_SIZE / 2)
							/ TILE_SIZE)][(int) (taxiLocation.y / TILE_SIZE)])) {
				taxiVelocity.x = 0;
				taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}
		}

		// Block movement up if there is no track up
		if (taxiVelocity.y < 0) {
			if (!(inBounds(taxiLocation.x, taxiLocation.y + taxiVelocity.y - TILE_SIZE / 2)
					&& tracks[(int) ((taxiLocation.x)
							/ TILE_SIZE)][(int) ((taxiLocation.y + taxiVelocity.y - TILE_SIZE / 2) / TILE_SIZE)])) {
				taxiVelocity.y = 0;
				taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}
		}

		// Block movement left if there is no track to the left
		if (taxiVelocity.x < 0) {
			if (!(inBounds(taxiLocation.x + taxiVelocity.x - TILE_SIZE / 2, taxiLocation.y)
					&& tracks[(int) ((taxiLocation.x + taxiVelocity.x - TILE_SIZE / 2)
							/ TILE_SIZE)][(int) (taxiLocation.y / TILE_SIZE)])) {
				taxiVelocity.x = 0;
				taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}
		}

		// Block movement down if there is no track down
		if (taxiVelocity.y > 0) {
			if (!(inBounds(taxiLocation.x, taxiLocation.y + taxiVelocity.y + TILE_SIZE / 2)
					&& tracks[(int) ((taxiLocation.x)
							/ TILE_SIZE)][(int) ((taxiLocation.y + taxiVelocity.y + TILE_SIZE / 2) / TILE_SIZE)])) {
				taxiVelocity.y = 0;
				taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}
		}

		if (taxiVelocity.length() > 4) {
			taxiVelocity.setLength(4);
		}
		taxiLocation = taxiLocation.add(taxiVelocity);
	}
}
