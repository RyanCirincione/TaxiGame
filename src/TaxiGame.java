import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
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

	public static final int S_WIDTH = 800, S_HEIGHT = 600, TILE_SIZE = 64, TRACK_PRICE = 25;
	public static Track[][] tracks;
	public static int[] trackInventory;
	public static Vector camera;
	int money, income, trackInvestment;
	InputHandler input;
	BufferedImage tracksImg;
	Vector taxiLocation, taxiVelocity;
	ArrayList<Vector> clients, destinations, trackShops;
	ArrayList<Vector[]> completedClients;

	public TaxiGame() {
		trackInventory = new int[5];
		trackShops = new ArrayList<Vector>();
		income = money = trackInvestment = 100;
		completedClients = new ArrayList<Vector[]>();
		destinations = new ArrayList<Vector>();
		clients = new ArrayList<Vector>();
		taxiLocation = new Vector(2.5 * TILE_SIZE, 2.5 * TILE_SIZE);
		taxiVelocity = new Vector();
		camera = taxiLocation.clone();
		input = new InputHandler();
		tracks = new Track[40][40];
		int[][] literalTrack = new int[][] { { 0, 0, 0, 1, 1, 1, 0, 0, 0 }, { 0, 0, 1, 1, 0, 1, 0, 0, 0 },
				{ 0, 0, 1, 0, 1, 1, 1, 0, 0 }, { 0, 0, 1, 0, 1, 0, 1, 1, 1 }, { 0, 1, 1, 0, 1, 0, 1, 0, 1 },
				{ 0, 1, 0, 0, 1, 1, 1, 1, 1 }, { 0, 1, 0, 0, 0, 0, 1, 0, 0 }, { 0, 1, 1, 1, 1, 0, 1, 0, 0 },
				{ 0, 0, 1, 0, 1, 1, 1, 1, 0 }, { 0, 0, 1, 1, 1, 0, 0, 1, 0 }, { 0, 0, 0, 0, 1, 1, 1, 1, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0 } };

		for (int x = 0; x < literalTrack.length; x++) {
			for (int y = 0; y < literalTrack[x].length; y++) {
				if (literalTrack[x][y] != 0) {
					tracks[x][y] = new Track(x + 1 < literalTrack.length && literalTrack[x + 1][y] == 1,
							y - 1 >= 0 && literalTrack[x][y - 1] == 1, x - 1 >= 0 && literalTrack[x - 1][y] == 1,
							y + 1 < literalTrack[x].length && literalTrack[x][y + 1] == 1);
				}
			}
		}

		tracks[10][7] = new Track(true, true, true, true);
		trackShops.add(new Vector(11 * TILE_SIZE / 2 - 15, 13 * TILE_SIZE / 2 - 15));

		try {
			tracksImg = ImageIO.read(new File("res/tracks.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.setFocusable(true);
		this.requestFocus();
		this.addKeyListener(input);
		this.addMouseListener(input);
		this.addMouseMotionListener(input);

		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}

	public void tick() {
		// This method is for shoving all the hell that is and will be the
		// controlling of the taxi's position relative to the tracks
		movementHell();

		// Always have 4 clients to pick up
		while (clients.size() + destinations.size() < 4) {
			Vector v = new Vector(Math.random() * tracks.length * TILE_SIZE,
					Math.random() * tracks[0].length * TILE_SIZE);
			if (tracks[(int) (v.x / TILE_SIZE)][(int) (v.y / TILE_SIZE)] != null) {
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
							if (tracks[(int) (v.x / TILE_SIZE)][(int) (v.y / TILE_SIZE)] != null) {
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
					income += (int) (Math.random() * 6) + 15;
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

		// Adjust camera
		camera = camera.add(taxiLocation.minus(camera).scale(0.05));

		// Receive money
		money += Math.signum(income -= Math.signum(income));

		// Put money in track shops
		for (Vector v : trackShops) {
			if (taxiLocation.distance2(v) <= 25 * 25 && taxiVelocity.length() < 0.5) {
				if (money > 0) {
					money--;
					trackInvestment++;
				}
			}
		}

		if (trackInvestment >= TRACK_PRICE) {
			trackInvestment -= TRACK_PRICE;
			trackInventory[(int)(Math.random()*trackInventory.length)]++;
		}
	}

	public void paintComponent(Graphics gr) {
		Graphics2D g = (Graphics2D) gr;
		super.paintComponent(g);

		// Draw tracks
		for (int x = 0; x < tracks.length; x++) {
			for (int y = 0; y < tracks[x].length; y++) {
				if (tracks[x][y] != null) {
					int sx = ((tracks[x][y].up ? 2 : 0) + (tracks[x][y].right ? 1 : 0));
					int sy = ((tracks[x][y].left ? 2 : 0) + (tracks[x][y].down ? 1 : 0));
					g.drawImage(tracksImg, (int) (x * TILE_SIZE + S_WIDTH / 2 - camera.x),
							(int) (y * TILE_SIZE + S_HEIGHT / 2 - camera.y),
							(int) ((x + 1) * TILE_SIZE + S_WIDTH / 2 - camera.x),
							(int) ((y + 1) * TILE_SIZE + S_HEIGHT / 2 - camera.y), sx * TILE_SIZE, sy * TILE_SIZE,
							(sx + 1) * TILE_SIZE, (sy + 1) * TILE_SIZE, null);
				}
			}
		}

		// Draw hover track
		int x = (int) ((input.mouse.x + camera.x - S_WIDTH / 2) / TILE_SIZE),
				y = (int) ((input.mouse.y + camera.y - S_HEIGHT / 2) / TILE_SIZE), sx = 0, sy = 0;
		int xm = (int) (input.mouse.x + TaxiGame.camera.x - TaxiGame.S_WIDTH / 2) % TILE_SIZE,
				ym = (int) (input.mouse.y + TaxiGame.camera.y - TaxiGame.S_HEIGHT / 2) % TILE_SIZE;

		switch (input.selectedTrack) {
		case 0:
			sx = ((xm >= ym && xm + ym < TILE_SIZE ? 2 : 0) + (xm >= ym && xm + ym >= TILE_SIZE ? 1 : 0));
			sy = ((xm < ym && xm + ym < TILE_SIZE ? 2 : 0) + (xm < ym && xm + ym >= TILE_SIZE ? 1 : 0));
			break;
		case 1:
			boolean right = xm >= ym && xm + ym >= TILE_SIZE, up = xm >= ym && xm + ym < TILE_SIZE,
					left = xm < ym && xm + ym < TILE_SIZE, down = xm < ym && xm + ym >= TILE_SIZE;
			sx = ((up || down ? 2 : 0) + (right || left ? 1 : 0));
			sy = ((right || left ? 2 : 0) + (up || down ? 1 : 0));
			break;
		case 2:
			sx = ((ym < TILE_SIZE / 2 ? 2 : 0) + (xm >= TILE_SIZE / 2 ? 1 : 0));
			sy = ((xm < TILE_SIZE / 2 ? 2 : 0) + (ym >= TILE_SIZE / 2 ? 1 : 0));
			break;
		case 3:
			sx = ((xm >= ym || xm + ym < TILE_SIZE ? 2 : 0) + (xm >= ym || xm + ym >= TILE_SIZE ? 1 : 0));
			sy = ((xm < ym || xm + ym < TILE_SIZE ? 2 : 0) + (xm < ym || xm + ym >= TILE_SIZE ? 1 : 0));
			break;
		case 4:
			sx = 3;
			sy = 3;
			break;
		}

		if (input.selectedTrack != -1 && x >= 0 && y >= 0 && x < tracks.length && y < tracks[x].length
				&& tracks[x][y] == null) {
			Composite comp = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g.drawImage(tracksImg, (int) (x * TILE_SIZE + S_WIDTH / 2 - camera.x),
					(int) (y * TILE_SIZE + S_HEIGHT / 2 - camera.y),
					(int) ((x + 1) * TILE_SIZE + S_WIDTH / 2 - camera.x),
					(int) ((y + 1) * TILE_SIZE + S_HEIGHT / 2 - camera.y), sx * TILE_SIZE, sy * TILE_SIZE,
					(sx + 1) * TILE_SIZE, (sy + 1) * TILE_SIZE, null);
			g.setComposite(comp);
		}

		// Draw taxi
		g.setColor(Color.yellow);
		g.fillOval((int) (taxiLocation.x - 5 + S_WIDTH / 2 - camera.x),
				(int) (taxiLocation.y - 5 + S_HEIGHT / 2 - camera.y), 10, 10);

		// Draw clients
		g.setColor(Color.orange);
		for (Vector c : clients) {
			g.fillOval((int) (c.x - 2 + S_WIDTH / 2 - camera.x), (int) (c.y - 2 + S_HEIGHT / 2 - camera.y), 5, 5);
			g.drawOval((int) (c.x - TILE_SIZE * 3 / 4 + S_WIDTH / 2 - camera.x),
					(int) (c.y - TILE_SIZE * 3 / 4 + S_HEIGHT / 2 - camera.y), TILE_SIZE * 3 / 2, TILE_SIZE * 3 / 2);
		}

		// Draw completed clients
		for (Vector[] c : completedClients) {
			g.setColor(new Color(255, 165, 0, (int) c[2].x));
			g.fillOval((int) (c[0].x - 2 + S_WIDTH / 2 - camera.x), (int) (c[0].y - 2 + S_HEIGHT / 2 - camera.y), 5, 5);
		}

		// Draw destinations
		g.setColor(new Color(200, 0, 200, 128));
		for (Vector d : destinations) {
			g.fillOval((int) (d.x - TILE_SIZE / 1.5 + S_WIDTH / 2 - camera.x),
					(int) (d.y - TILE_SIZE / 1.5 + S_HEIGHT / 2 - camera.y), (int) (TILE_SIZE / 1.5 * 2),
					(int) (TILE_SIZE / 1.5 * 2));
		}

		// Draw shops
		g.setColor(new Color(25, 0, 255));
		for (Vector v : trackShops) {
			g.fillOval((int) (v.x - 5 + S_WIDTH / 2 - camera.x), (int) (v.y - 5 + S_HEIGHT / 2 - camera.y), 10, 10);
			g.drawOval((int) (v.x - 25 + S_WIDTH / 2 - camera.x), (int) (v.y - 25 + S_HEIGHT / 2 - camera.y), 50, 50);

			//TODO Fix design problem: "How do I get the $25?"
			if (taxiLocation.distance2(v) < 150 * 150) {
				g.setColor(new Color(25, 0, 255, (int) (63 + 192 * (1 - taxiLocation.distance(v) / 150))));
				g.drawString("$" + (TRACK_PRICE - trackInvestment), (int) (v.x - 15 + S_WIDTH / 2 - camera.x),
						(int) (v.y - 8 + S_HEIGHT / 2 - camera.y));
			}
		}

		// Draw money
		g.setColor(new Color(20, 20, 20));
		g.drawString("$" + money, 5, 13);
		
		// Draw track inventory
		g.drawString("1: " + trackInventory[0], 5, 30);
		g.drawString("2: " + trackInventory[1], 5, 42);
		g.drawString("3: " + trackInventory[2], 5, 54);
		g.drawString("4: " + trackInventory[3], 5, 66);
		g.drawString("5: " + trackInventory[4], 5, 78);
	}

	private void movementHell() {
		int tx = (int) (taxiLocation.x / TILE_SIZE);
		int ty = (int) (taxiLocation.y / TILE_SIZE);

		if (input.right) {
			// Taxi is on horizontal track (or switching to), and can move right
			if (((taxiLocation.y - TILE_SIZE / 2 + 1) % TILE_SIZE < 2 || (int) ((taxiLocation.y - TILE_SIZE / 2 - 1)
					/ TILE_SIZE) != (int) ((taxiLocation.y + taxiVelocity.y - TILE_SIZE / 2) / TILE_SIZE))
					&& (taxiLocation.x % TILE_SIZE < TILE_SIZE / 2 && tracks[tx][ty].left
							|| taxiLocation.x % TILE_SIZE >= TILE_SIZE / 2 && tracks[tx][ty].right)) {
				// Taxi is currently moving vertically
				if (Math.abs(taxiVelocity.y) > 0.00001) {
					taxiLocation.y = ty * TILE_SIZE + TILE_SIZE / 2;
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
					&& (taxiLocation.y % TILE_SIZE <= TILE_SIZE / 2 && tracks[tx][ty].up
							|| taxiLocation.y % TILE_SIZE > TILE_SIZE / 2 && tracks[tx][ty].down)) {
				// Taxi is currently moving horizontally
				if (Math.abs(taxiVelocity.x) > 0.00001) {
					taxiLocation.x = tx * TILE_SIZE + TILE_SIZE / 2;
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
					&& (taxiLocation.x % TILE_SIZE <= TILE_SIZE / 2 && tracks[tx][ty].left
							|| taxiLocation.x % TILE_SIZE > TILE_SIZE / 2 && tracks[tx][ty].right)) {
				// Taxi is currently moving vertically
				if (Math.abs(taxiVelocity.y) > 0.00001) {
					taxiLocation.y = ty * TILE_SIZE + TILE_SIZE / 2;
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
					&& (taxiLocation.y % TILE_SIZE < TILE_SIZE / 2 && tracks[tx][ty].up
							|| taxiLocation.y % TILE_SIZE >= TILE_SIZE / 2 && tracks[tx][ty].down)) {
				// Taxi is currently moving horizontally
				if (Math.abs(taxiVelocity.x) > 0.00001) {
					taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
					taxiVelocity.y = Math.abs(taxiVelocity.x);
					taxiVelocity.x = 0;
				}
				taxiVelocity.y += 0.05;
			}
		}

		if (taxiVelocity.x > 0) {
			// Block movement right if track is not right
			if ((taxiLocation.x + taxiVelocity.x) % TILE_SIZE > TILE_SIZE / 2 && !tracks[tx][ty].right) {
				taxiVelocity.x = 0;
				taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}

			// Block movement right if there is no track to the right
			if (tracks[(int) (taxiLocation.x + taxiVelocity.x) / TILE_SIZE][ty] == null) {
				taxiVelocity.x = 0;
				taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE - 0.00001;
			}
		}

		if (taxiVelocity.y < 0) {
			// Block movement up if track is not up
			if ((taxiLocation.y + taxiVelocity.y) % TILE_SIZE < TILE_SIZE / 2 && !tracks[tx][ty].up) {
				taxiVelocity.y = 0;
				taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}

			// Block movement up if there is no track to the up
			if (tracks[tx][(int) (taxiLocation.y + taxiVelocity.y) / TILE_SIZE] == null) {
				taxiVelocity.y = 0;
				taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE;
			}
		}

		if (taxiVelocity.x < 0) {
			// Block movement left if track is not left
			if ((taxiLocation.x + taxiVelocity.x) % TILE_SIZE < TILE_SIZE / 2 && !tracks[tx][ty].left) {
				taxiVelocity.x = 0;
				taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}

			// Block movement left if there is no track to the left
			if (tracks[(int) (taxiLocation.x + taxiVelocity.x) / TILE_SIZE][ty] == null) {
				taxiVelocity.x = 0;
				taxiLocation.x = (int) (taxiLocation.x / TILE_SIZE) * TILE_SIZE;
			}
		}

		if (taxiVelocity.y > 0) {
			// Block movement down if track is not down
			if ((taxiLocation.y + taxiVelocity.y) % TILE_SIZE > TILE_SIZE / 2 && !tracks[tx][ty].down) {
				taxiVelocity.y = 0;
				taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE + TILE_SIZE / 2;
			}

			// Block movement down if there is no track to the down
			if (tracks[tx][(int) (taxiLocation.y + taxiVelocity.y) / TILE_SIZE] == null) {
				taxiVelocity.y = 0;
				taxiLocation.y = (int) (taxiLocation.y / TILE_SIZE) * TILE_SIZE + TILE_SIZE - 0.00001;
			}
		}

		if (taxiVelocity.length() > 4) {
			taxiVelocity.setLength(4);
		}
		taxiLocation = taxiLocation.add(taxiVelocity);
	}
}
