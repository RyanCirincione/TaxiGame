import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
	public static final double CURVE_RADIUS = TILE_SIZE / 2.5;
	public static final double MAX_SPEED = 3.0, ACCELERATION = 0.05;
	public static Track[][] tracks;
	public static Vector camera;
	public static double cameraAngle;
	int money, income, trackInvestment;
	InputHandler input;
	public static Vector taxiLocation, taxiVelocity;
	ArrayList<Vector> clients, destinations, trackShops;
	ArrayList<Vector[]> completedClients;

	public TaxiGame() {
		cameraAngle = 0;
		trackShops = new ArrayList<Vector>();
		income = money = trackInvestment = 100;
		completedClients = new ArrayList<Vector[]>();
		destinations = new ArrayList<Vector>();
		clients = new ArrayList<Vector>();
		taxiLocation = new Vector(4.5 * TILE_SIZE, 2.5 * TILE_SIZE);
		taxiVelocity = new Vector();
		camera = taxiLocation.clone();
		input = new InputHandler();
		tracks = new Track[40][40];
		// @formatter:off
		int[][] literalTrack = new int[][] {
			{ 0, 0, 0, 1, 1, 1, 0, 0, 0 },
			{ 0, 0, 1, 1, 0, 1, 0, 0, 0 },
			{ 0, 0, 1, 0, 1, 1, 1, 0, 0 },
			{ 0, 0, 1, 0, 1, 0, 1, 1, 1 },
			{ 1, 1, 1, 1, 1, 0, 1, 0, 1 },
			{ 1, 0, 1, 0, 1, 1, 1, 1, 1 },
			{ 1, 0, 1, 0, 0, 0, 1, 0, 0 },
			{ 1, 1, 1, 1, 1, 0, 1, 0, 0 },
			{ 0, 0, 1, 0, 1, 1, 1, 1, 0 },
			{ 0, 0, 1, 1, 1, 0, 0, 1, 0 },
			{ 0, 0, 0, 0, 1, 1, 1, 1, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 }
		};
//		int[][] literalTrack = new int[][] {
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 },
//			{ 1, 1, 1, 1, 1, 1, 1, 1, 1 }
//		};
//		int[][] literalTrack = new int[][] {
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 1, 1, 1, 0, 0, 0, 0, 0 },
//			{ 0, 1, 0, 1, 0, 0, 0, 0, 0 },
//			{ 0, 1, 1, 1, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 },
//			{ 0, 0, 0, 0, 0, 0, 0, 0, 0 }
//		};
		// @formatter:on

		for (int x = 0; x < literalTrack.length; x++) {
			for (int y = 0; y < literalTrack[x].length; y++) {
				if (literalTrack[x][y] != 0) {
					tracks[x][y] = new Track(x + 1 < literalTrack.length && literalTrack[x + 1][y] == 1, y - 1 >= 0 && literalTrack[x][y - 1] == 1,
							x - 1 >= 0 && literalTrack[x - 1][y] == 1, y + 1 < literalTrack[x].length && literalTrack[x][y + 1] == 1);
				}
			}
		}

		trackShops.add(new Vector(11 * TILE_SIZE / 2 - 15, 13 * TILE_SIZE / 2 - 15));

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
		try {
			movementController();
		} catch (NullPointerException e) {
			System.out.println("Flew off the rail again!");
			taxiLocation.set(4.5 * TILE_SIZE, 2.5 * TILE_SIZE);
		}

		// Always have 4 clients to pick up
		while (clients.size() + destinations.size() < 4) {
			Vector v = new Vector(Math.random() * tracks.length * TILE_SIZE, Math.random() * tracks[0].length * TILE_SIZE);
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
							Vector v = new Vector(Math.random() * tracks.length * TILE_SIZE, Math.random() * tracks[0].length * TILE_SIZE);
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
					completedClients.add(new Vector[] { taxiLocation.clone(), destinations.get(i).clone().minus(taxiLocation).setLength(0.3), new Vector(255, 0) });
					destinations.remove(i--);
					income += (int) (Math.random() * 6) + 15;
				}
			}
		}

		// Update camera angle
		if (taxiVelocity.length() > 0.00001) {
			cameraAngle = -Math.atan(taxiVelocity.y / taxiVelocity.x) - Math.PI / 2 - (taxiVelocity.x < 0 ? Math.PI : 0);
		}

		// Update completed clients
		for (int i = 0; i < completedClients.size(); i++) {
			completedClients.get(i)[0] = completedClients.get(i)[0].plus(completedClients.get(i)[1]);

			completedClients.get(i)[2].x -= 2.5;
			if (completedClients.get(i)[2].x <= 0) {
				completedClients.remove(i--);
			}
		}

		// Adjust camera
		camera = camera.plus(taxiLocation.minus(camera).scale(0.05));

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
			// TODO Increment track "stock"
		}
	}

	//FIXME Occasional Screen flicker when turning
	public void paintComponent(Graphics gr) {
		Graphics2D g = (Graphics2D) gr;
		super.paintComponent(g);

		// Rotate the camera
		g.translate(S_WIDTH / 2, S_HEIGHT / 2);
		g.rotate(cameraAngle);

		// Draw tracks
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(2));
		for (int x = 0; x < tracks.length; x++) {
			for (int y = 0; y < tracks[x].length; y++) {
				if (tracks[x][y] != null) {
					final int TS = TILE_SIZE;
					if (tracks[x][y].right) g.drawLine((int) (x * TS + TS / 2 + CURVE_RADIUS - camera.x), (int) (y * TS + TS / 2 - camera.y), (int) ((x + 1) * TS - camera.x),
							(int) (y * TS + TS / 2 - camera.y));
					if (tracks[x][y].up) g.drawLine((int) (x * TS + TS / 2 - camera.x), (int) (y * TS + TS / 2 - CURVE_RADIUS - camera.y), (int) (x * TS + TS / 2 - camera.x),
							(int) (y * TS - camera.y));
					if (tracks[x][y].left) g.drawLine((int) (x * TS + TS / 2 - CURVE_RADIUS - camera.x), (int) (y * TS + TS / 2 - camera.y), (int) (x * TS - camera.x),
							(int) (y * TS + TS / 2 - camera.y));
					if (tracks[x][y].down) g.drawLine((int) (x * TS + TS / 2 - camera.x), (int) (y * TS + TS / 2 + CURVE_RADIUS - camera.y), (int) (x * TS + TS / 2 - camera.x),
							(int) ((y + 1) * TS - camera.y));
					if (tracks[x][y].right && tracks[x][y].left) g.drawLine((int) (x * TS + TS / 2 + CURVE_RADIUS - camera.x), (int) (y * TS + TS / 2 - camera.y),
							(int) (x * TS + TS / 2 - CURVE_RADIUS - camera.x), (int) (y * TS + TS / 2 - camera.y));
					if (tracks[x][y].up && tracks[x][y].down) g.drawLine((int) (x * TS + TS / 2 - camera.x), (int) (y * TS + TS / 2 - CURVE_RADIUS - camera.y),
							(int) (x * TS + TS / 2 - camera.x), (int) (y * TS + TS / 2 + CURVE_RADIUS - camera.y));
					if (tracks[x][y].right && tracks[x][y].up) g.drawArc((int) (x * TS + TS / 2 - camera.x), (int) (y * TS + TS / 2 - CURVE_RADIUS * 2 - camera.y),
							(int) (CURVE_RADIUS * 2), (int) (CURVE_RADIUS * 2), -90, -90);
					if (tracks[x][y].up && tracks[x][y].left) g.drawArc((int) (x * TS + TS / 2 - CURVE_RADIUS * 2 - camera.x),
							(int) (y * TS + TS / 2 - CURVE_RADIUS * 2 - camera.y), (int) (CURVE_RADIUS * 2), (int) (CURVE_RADIUS * 2), 0, -90);
					if (tracks[x][y].left && tracks[x][y].down) g.drawArc((int) (x * TS + TS / 2 - CURVE_RADIUS * 2 - camera.x), (int) (y * TS + TS / 2 - camera.y),
							(int) (CURVE_RADIUS * 2), (int) (CURVE_RADIUS * 2), 90, -90);
					if (tracks[x][y].down && tracks[x][y].right)
						g.drawArc((int) (x * TS + TS / 2 - camera.x), (int) (y * TS + TS / 2 - camera.y), (int) (CURVE_RADIUS * 2), (int) (CURVE_RADIUS * 2), 180, -90);
				}
			}
		}

		// Draw taxi
		g.setColor(Color.yellow);
		g.fillOval((int) (taxiLocation.x - 5 - camera.x), (int) (taxiLocation.y - 5 - camera.y), 10, 10);

		// Draw clients
		g.setColor(Color.orange);
		for (Vector c : clients) {
			g.fillOval((int) (c.x - 2 - camera.x), (int) (c.y - 2 - camera.y), 5, 5);
			g.drawOval((int) (c.x - TILE_SIZE * 3 / 4 - camera.x), (int) (c.y - TILE_SIZE * 3 / 4 - camera.y), TILE_SIZE * 3 / 2, TILE_SIZE * 3 / 2);
		}

		// Draw completed clients
		for (Vector[] c : completedClients) {
			g.setColor(new Color(255, 165, 0, (int) c[2].x));
			g.fillOval((int) (c[0].x - 2 - camera.x), (int) (c[0].y - 2 - camera.y), 5, 5);
		}

		// Draw destinations
		g.setColor(new Color(200, 0, 200, 128));
		for (Vector d : destinations) {
			g.fillOval((int) (d.x - TILE_SIZE / 1.5 - camera.x), (int) (d.y - TILE_SIZE / 1.5 - camera.y), (int) (TILE_SIZE / 1.5 * 2), (int) (TILE_SIZE / 1.5 * 2));
		}

		// Draw shops
		g.setColor(new Color(25, 0, 255));
		for (Vector v : trackShops) {
			g.fillOval((int) (v.x - 5 - camera.x), (int) (v.y - 5 - camera.y), 10, 10);
			g.drawOval((int) (v.x - 25 - camera.x), (int) (v.y - 25 - camera.y), 50, 50);

			// TODO Fix design problem: "How do I get the $25?"
			if (taxiLocation.distance2(v) < 150 * 150) {
				g.setColor(new Color(25, 0, 255, (int) (63 + 192 * (1 - taxiLocation.distance(v) / 150))));
				g.drawString("$" + (TRACK_PRICE - trackInvestment), (int) (v.x - 15 - camera.x), (int) (v.y - 8 - camera.y));
			}
		}

		// Unrotate the camera
		g.rotate(-cameraAngle);
		g.translate(-S_WIDTH / 2, -S_HEIGHT / 2);

		// Draw money
		g.setColor(new Color(20, 20, 20));
		g.drawString("$" + money, 5, 13);
	}

	private void movementController() {
		// If the taxi ever flies off the rail, make these decimals even smaller as long as the game still functions
		final boolean HORIZONTALLY_ALIGNED = Math.abs(taxiLocation.y % TILE_SIZE - TILE_SIZE / 2) < 0.00000001;
		final boolean VERTICALLY_ALIGNED = Math.abs(taxiLocation.x % TILE_SIZE - TILE_SIZE / 2) < 0.00000001;
		boolean ON_CURVE = !(HORIZONTALLY_ALIGNED || VERTICALLY_ALIGNED);
		int tx = (int) (taxiLocation.x / TILE_SIZE);
		int ty = (int) (taxiLocation.y / TILE_SIZE);
		Vector taxiModTile = new Vector(taxiLocation.x % TILE_SIZE, taxiLocation.y % TILE_SIZE);

		double l = taxiVelocity.length();
		if (input.up) {
			if (l < MAX_SPEED - ACCELERATION) {
				taxiVelocity.setLength(l + ACCELERATION);
			} else {
				taxiVelocity.setLength(MAX_SPEED);
			}

			// If velocity is zero, we need a direction to increase length toward
			if (taxiVelocity.length() < 0.01) {
				taxiVelocity.set(Math.cos(cameraAngle + Math.PI / 2), Math.sin(cameraAngle - Math.PI / 2));
				taxiVelocity.setLength(ACCELERATION);
			}
		}
		if (input.down) {
			if (l > ACCELERATION) {
				taxiVelocity.setLength(l - ACCELERATION);
			} else {
				taxiVelocity.setLength(0);
			}
		}

		// Rotate velocity to be a chord on the circular curve
		if (ON_CURVE) {
			double theta = Math.asin(taxiVelocity.length() / CURVE_RADIUS);
			if (taxiModTile.x > TILE_SIZE / 2 && taxiVelocity.y > 0 || taxiModTile.x < TILE_SIZE / 2 && taxiVelocity.y < 0) {
				// When going counter clockwise (around center of tile), theta needs to be
				// inverted
				theta *= -1;
			}

			double x = taxiVelocity.x, y = taxiVelocity.y;
			taxiVelocity.set(x * Math.cos(theta) - y * Math.sin(theta), x * Math.sin(theta) + y * Math.cos(theta));
		} else if (HORIZONTALLY_ALIGNED) {
			if (taxiModTile.x < TILE_SIZE / 2 - CURVE_RADIUS && taxiModTile.x + taxiVelocity.x > TILE_SIZE / 2 - CURVE_RADIUS) {
				if (input.left && tracks[tx][ty].up || !tracks[tx][ty].down && !tracks[tx][ty].right) {
					taxiVelocity.y = Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + taxiVelocity.x - (TILE_SIZE / 2 - CURVE_RADIUS), 2)) - CURVE_RADIUS;
				} else if (input.right && tracks[tx][ty].down || !tracks[tx][ty].right) {
					taxiVelocity.y = -Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + taxiVelocity.x - (TILE_SIZE / 2 - CURVE_RADIUS), 2)) + CURVE_RADIUS;
				}
			} else if (taxiModTile.x > TILE_SIZE / 2 + CURVE_RADIUS && taxiModTile.x + taxiVelocity.x < TILE_SIZE / 2 + CURVE_RADIUS) {
				if (input.left && tracks[tx][ty].down || !tracks[tx][ty].up && !tracks[tx][ty].left) {
					taxiVelocity.y = -Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + taxiVelocity.x - (TILE_SIZE / 2 + CURVE_RADIUS), 2)) + CURVE_RADIUS;
				} else if (input.right && tracks[tx][ty].up || !tracks[tx][ty].left) {
					taxiVelocity.y = Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + taxiVelocity.x - (TILE_SIZE / 2 + CURVE_RADIUS), 2)) - CURVE_RADIUS;
				}
			} else if (!VERTICALLY_ALIGNED) {
				taxiVelocity.y = 0;
			}
		} else if (VERTICALLY_ALIGNED) {
			if (taxiModTile.y < TILE_SIZE / 2 - CURVE_RADIUS && taxiModTile.y + taxiVelocity.y > TILE_SIZE / 2 - CURVE_RADIUS) {
				if (input.left && tracks[tx][ty].right || !tracks[tx][ty].left && !tracks[tx][ty].down) {
					taxiVelocity.x = -Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + taxiVelocity.y - (TILE_SIZE / 2 - CURVE_RADIUS), 2)) + CURVE_RADIUS;
				} else if (input.right && tracks[tx][ty].left || !tracks[tx][ty].down) {
					taxiVelocity.x = Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + taxiVelocity.y - (TILE_SIZE / 2 - CURVE_RADIUS), 2)) - CURVE_RADIUS;
				}
			} else if (taxiModTile.y > TILE_SIZE / 2 + CURVE_RADIUS && taxiModTile.y + taxiVelocity.y < TILE_SIZE / 2 + CURVE_RADIUS) {
				if (input.left && tracks[tx][ty].left || !tracks[tx][ty].right && !tracks[tx][ty].up) {
					taxiVelocity.x = Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + taxiVelocity.y - (TILE_SIZE / 2 + CURVE_RADIUS), 2)) - CURVE_RADIUS;
				} else if (input.right && tracks[tx][ty].right || !tracks[tx][ty].up) {
					taxiVelocity.x = -Math.sqrt(Math.pow(CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + taxiVelocity.y - (TILE_SIZE / 2 + CURVE_RADIUS), 2)) + CURVE_RADIUS;
				}
			} else {
				taxiVelocity.x = 0;
			}
		}

		// If taxi is leaving curve, it must snap back to horizontal/vertical alignment
		if (taxiModTile.x > TILE_SIZE / 2 - CURVE_RADIUS && taxiModTile.x < TILE_SIZE / 2 + CURVE_RADIUS && taxiModTile.y > TILE_SIZE / 2 - CURVE_RADIUS
				&& taxiModTile.y < TILE_SIZE / 2 + CURVE_RADIUS) {
			if (taxiModTile.x + taxiVelocity.x < TILE_SIZE / 2 - CURVE_RADIUS) {
				taxiVelocity.y = TILE_SIZE / 2 - taxiModTile.y;
			} else if (taxiModTile.x + taxiVelocity.x > TILE_SIZE / 2 + CURVE_RADIUS) {
				taxiVelocity.y = TILE_SIZE / 2 - taxiModTile.y;
			} else if (taxiModTile.y + taxiVelocity.y < TILE_SIZE / 2 - CURVE_RADIUS) {
				taxiVelocity.x = TILE_SIZE / 2 - taxiModTile.x;
			} else if (taxiModTile.y + taxiVelocity.y > TILE_SIZE / 2 + CURVE_RADIUS) {
				taxiVelocity.x = TILE_SIZE / 2 - taxiModTile.x;
			}
		}

		taxiLocation = taxiLocation.plus(taxiVelocity);
	}
}
