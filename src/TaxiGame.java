import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
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

	public static final int S_WIDTH = 1000, S_HEIGHT = 800, TILE_SIZE = 64, TRACK_PRICE = 25;
	public static final double CURVE_RADIUS = TILE_SIZE / 2.5;
	public static final double MAX_SPEED = 2.0, ACCELERATION = 0.03, SCREEN_SCALE = 1.75, MAX_GAS = 20.0, MAX_RATING = 5.0;
	public static Track[][] tracks, plannedTracks;
	public static Vector camera;
	public static double cameraAngle, visualCameraAngle, gas, rating;
	public static int money, income, trackInvestment, trackStock;
	public static boolean paused, mainMenu;
	InputHandler input;
	public static Vector taxiLocation, taxiVelocity;
	public static ArrayList<Vector> trackShops, gasStations;
	public static ArrayList<Customer> customers;
	public static Rectangle newGameButton;

	public TaxiGame() {
		newGameButton = new Rectangle(S_WIDTH / 2 - 100, S_HEIGHT / 2 - 50, 200, 100);
		mainMenu = true;
		input = new InputHandler();

		this.setFocusable(true);
		this.requestFocus();
		this.addKeyListener(input);
		this.addMouseListener(input);
		this.addMouseMotionListener(input);

		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}

	public static void startNewGame() {
		paused = false;
		rating = 3.0;
		trackStock = 0;
		cameraAngle = 0;
		trackShops = new ArrayList<Vector>();
		gasStations = new ArrayList<Vector>();
		income = money = trackInvestment = 100;
		customers = new ArrayList<Customer>();
		taxiLocation = new Vector(5.5 * TILE_SIZE, 5.5 * TILE_SIZE);
		taxiVelocity = new Vector();
		camera = taxiLocation.clone();
		tracks = new Track[30][30];
		plannedTracks = new Track[30][30];
		gas = MAX_GAS;

		trackShops.add(new Vector(5.5 * TILE_SIZE - 15, 5.5 * TILE_SIZE - 15));
		gasStations.add(new Vector(6.5 * TILE_SIZE + 15, 6.5 * TILE_SIZE + 15));

		generateCity(plannedTracks);
		for (int x = 5; x <= 7; x++) {
			for (int y = 5; y <= 7; y++) {
				tracks[x][y] = plannedTracks[x][y];
			}
		}
	}

	public void tick() {
		if (paused || mainMenu) return;

		try {
			movementController();
		} catch (NullPointerException e) {
			System.out.println("Flew off the rail again!");
			taxiLocation.set(5.5 * TILE_SIZE, 5.5 * TILE_SIZE);
		}

		// Always have 4 clients to pick up
		while (customers.size() < 4) {
			customers.add(Customer.generateCustomer());
		}

		// Update customers
		Iterator<Customer> iter = customers.iterator();
		while (iter.hasNext()) {
			Customer c = iter.next();
			c.update();
			if (c.visualFade <= 0) {
				iter.remove();
			}
		}

		// Update camera angle
		if (taxiVelocity.length() > 0.00001) {
			cameraAngle = -Math.atan(taxiVelocity.y / taxiVelocity.x) - Math.PI / 2 - (taxiVelocity.x < 0 ? Math.PI : 0);
		}
		visualCameraAngle = (visualCameraAngle + cameraAngle % 360) / 2;

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

		// Buy gas
		for (Vector v : gasStations) {
			if (taxiLocation.distance2(v) <= 25 * 25 && taxiVelocity.length() < 0.5) {
				if (money > 0 && gas < MAX_GAS - 0.5) {
					money--;
					gas += 0.3;
				}
			}
		}

		if (trackInvestment >= TRACK_PRICE) {
			trackInvestment -= TRACK_PRICE;
			trackStock++;
		}

		if (gas < 0) {
			gas = 0;
		}
		if (rating < 0) {
			rating = 0;
		} else if (rating > MAX_RATING) {
			rating = MAX_RATING;
		}
	}

	// FIXME Occasional Screen flicker when turning
	public void paintComponent(Graphics gr) {
		Graphics2D g = (Graphics2D) gr;
		super.paintComponent(g);

		// Draw main menu
		if (mainMenu) {
			Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(3));
			g.setColor(Color.green);
			g.drawRect(newGameButton.x, newGameButton.y, newGameButton.width, newGameButton.height);
			g.drawString("START GAME", newGameButton.x + newGameButton.width / 2 - 42, newGameButton.y + newGameButton.height / 2 + 6);
			g.setStroke(oldStroke);

			return;
		}

		// Rotate the camera
		g.translate(S_WIDTH / 2, S_HEIGHT / 2);
		g.rotate(visualCameraAngle);
		g.scale(SCREEN_SCALE, SCREEN_SCALE);

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
		for (Customer cust : customers) {
			Vector c = cust.position, d = cust.destination;

			if (cust.droppedOff) {
				g.setColor(new Color(255, 165, 0, (int) cust.visualFade));

				g.fillOval((int) (c.x - 2 - camera.x), (int) (c.y - 2 - camera.y), 5, 5);
			} else if (cust.pickedUp) {
				g.setColor(new Color(200, 0, 200, 128));

				g.fillOval((int) (d.x - TILE_SIZE / 1.5 - camera.x), (int) (d.y - TILE_SIZE / 1.5 - camera.y), (int) (TILE_SIZE / 1.5 * 2), (int) (TILE_SIZE / 1.5 * 2));
			} else {
				g.setColor(cust.goldMember ? new Color(255, 235, 95) : new Color(245, 170, 30));

				g.fillOval((int) (c.x - 2 - camera.x), (int) (c.y - 2 - camera.y), 5, 5);
				g.drawOval((int) (c.x - TILE_SIZE * 3 / 4 - camera.x), (int) (c.y - TILE_SIZE * 3 / 4 - camera.y), TILE_SIZE * 3 / 2, TILE_SIZE * 3 / 2);
			}
		}

		// Draw shops
		g.setColor(new Color(25, 0, 255));
		for (Vector v : trackShops) {
			g.fillOval((int) (v.x - 5 - camera.x), (int) (v.y - 5 - camera.y), 10, 10);
			g.drawOval((int) (v.x - 25 - camera.x), (int) (v.y - 25 - camera.y), 50, 50);

			if (taxiLocation.distance2(v) < 150 * 150) {
				g.setColor(new Color(25, 0, 255, (int) (63 + 192 * (1 - taxiLocation.distance(v) / 150))));
				g.drawString("$" + trackInvestment + "/$25", (int) (v.x - 20 - camera.x), (int) (v.y - 8 - camera.y));
			}
		}
		g.setColor(new Color(175, 150, 50));
		for (Vector v : gasStations) {
			g.fillOval((int) (v.x - 5 - camera.x), (int) (v.y - 5 - camera.y), 10, 10);
			g.drawOval((int) (v.x - 25 - camera.x), (int) (v.y - 25 - camera.y), 50, 50);
		}

		// Unrotate the camera
		g.scale(1 / SCREEN_SCALE, 1 / SCREEN_SCALE);
		g.rotate(-visualCameraAngle);
		g.translate(-S_WIDTH / 2, -S_HEIGHT / 2);

		// Draw money
		g.setColor(new Color(20, 20, 20));
		g.drawString("$" + money, 5, 13);

		// Draw track stock
		g.drawString("" + trackStock, 5, 25);

		// Draw rating
		BufferedImage ratingStars = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
		Graphics starGraphic = ratingStars.getGraphics();
		for (int i = 0; i < 100; i += 20) {
			starGraphic.setColor(Color.yellow.darker());
			starGraphic.fillPolygon(new int[] { i + 10, i + 13, i + 20, i + 14, i + 17, i + 10, i + 3, i + 6, i, i + 7 }, new int[] { 0, 7, 7, 12, 20, 15, 20, 12, 7, 7 }, 10);
		}
		g.drawImage(ratingStars, 45, 5, 45 + (int) (100 * rating / MAX_RATING), 55, 0, 0, (int) (100 * rating / MAX_RATING), 50, null);

		// Draw gas
		g.setColor(Color.gray);
		g.fillRoundRect(10, S_HEIGHT - 80, 100, 70, 10, 10);
		g.setColor(Color.black);
		g.drawString("E", 20, S_HEIGHT - 20);
		g.drawString("F", 94, S_HEIGHT - 20);
		g.setColor(Color.red);
		g.drawLine(60, S_HEIGHT - 20, (int) (40 * Math.cos((MAX_GAS - gas) / MAX_GAS * Math.PI)) + 60, (int) -(40 * Math.sin((MAX_GAS - gas) / MAX_GAS * Math.PI)) + S_HEIGHT - 20);

		// Draw Game Over
		if (taxiVelocity.length() < 0.0000001 && gas < 0.000001) {
			g.setColor(Color.red);
			g.drawString("Game Over (Press SPACE to return to menu)", S_WIDTH / 2 - 140, 20);
		}

		// Draw pause screen
		if (paused) {
			g.setColor(new Color(64, 64, 64, 128));
			g.fillRect(0, 0, S_WIDTH, S_HEIGHT);

			g.setColor(Color.white);
			g.setFont(new Font("Comic Sans", Font.BOLD, 48));
			g.drawString("PAUSED", S_WIDTH / 2 - 100, S_HEIGHT / 2 + 5);
		}
	}

	private void movementController() {
		// If the taxi ever flies off the rail, make these decimals even smaller as long
		// as the game still functions
		final boolean HORIZONTALLY_ALIGNED = Math.abs(taxiLocation.y % TILE_SIZE - TILE_SIZE / 2) < 0.0000000001;
		final boolean VERTICALLY_ALIGNED = Math.abs(taxiLocation.x % TILE_SIZE - TILE_SIZE / 2) < 0.0000000001;
		boolean ON_CURVE = !(HORIZONTALLY_ALIGNED || VERTICALLY_ALIGNED);
		int tx = (int) (taxiLocation.x / TILE_SIZE);
		int ty = (int) (taxiLocation.y / TILE_SIZE);
		Vector taxiModTile = new Vector(taxiLocation.x % TILE_SIZE, taxiLocation.y % TILE_SIZE);

		double l = taxiVelocity.length();
		if (input.up && gas > 0) {
			if (l < MAX_SPEED - ACCELERATION) {
				gas -= 0.02;
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

		Vector destination = taxiLocation.plus(taxiVelocity);
		if (tracks[(int) (destination.x / TILE_SIZE)][(int) (destination.y / TILE_SIZE)] == null) {
			if (trackStock > 0) {
				tracks[(int) (destination.x / TILE_SIZE)][(int) (destination.y / TILE_SIZE)] = plannedTracks[(int) (destination.x / TILE_SIZE)][(int) (destination.y / TILE_SIZE)];
				trackStock--;
			} else {
				taxiVelocity.setLength(0);
				destination = taxiLocation;
			}
		}
		taxiLocation = destination;
	}

	private static void generateCity(Track[][] tracks) {
		for (int x = 0; x < tracks.length; x++) {
			for (int y = 0; y < tracks[0].length; y++) {
				tracks[x][y] = new Track(x + 1 < tracks.length, y > 0, x > 0, y + 1 < tracks[0].length);
			}
		}

		generateCity(tracks, 0, 0, tracks.length - 1, tracks[0].length - 1);

		for (int x = 0; x < tracks.length; x++) {
			for (int y = 0; y < tracks[0].length; y++) {
				while ((tracks[x][y].right ? 1 : 0) + (tracks[x][y].up ? 1 : 0) + (tracks[x][y].left ? 1 : 0) + (tracks[x][y].down ? 1 : 0) <= 1) {
					switch ((int) (Math.random() * 4)) {
					case 0:
						if (x + 1 < tracks.length) {
							tracks[x][y].right = true;
							tracks[x + 1][y].left = true;
						}
						break;
					case 1:
						if (y > 0) {
							tracks[x][y].up = true;
							tracks[x][y - 1].down = true;
						}
						break;
					case 2:
						if (x > 0) {
							tracks[x][y].left = true;
							tracks[x - 1][y].right = true;
						}
						break;
					case 3:
						if (y + 1 < tracks[0].length) {
							tracks[x][y].down = true;
							tracks[x][y + 1].up = true;
						}
						break;
					}
				}
			}
		}
	}

	private static void generateCity(Track[][] tracks, int x, int y, int x2, int y2) {
		if (x == x2 || y == y2) {
			return;
		}

		int wallX = (int) ((Math.random() + Math.random() + Math.random()) / 3 * (x2 - x) + x), wallY = (int) ((Math.random() + Math.random() + Math.random()) / 3 * (y2 - y) + y);
		int holeX1 = (int) (Math.random() * (wallX - x) + x), holeX2 = (int) (Math.random() * (x2 - wallX + 1) + wallX), holeY1 = (int) (Math.random() * (wallY - y) + y),
				holeY2 = (int) (Math.random() * (y2 - wallY + 1) + wallY);

		for (int i = y; i <= y2; i++) {
			if (i == holeY1 || i == holeY2) {
				continue;
			}

			tracks[wallX][i].right = false;
			tracks[wallX + 1][i].left = false;
		}
		for (int i = x; i <= x2; i++) {
			if (i == holeX1 || i == holeX2) {
				continue;
			}

			tracks[i][wallY].down = false;
			tracks[i][wallY + 1].up = false;
		}

		generateCity(tracks, x, y, wallX, wallY);
		generateCity(tracks, wallX + 1, y, x2, wallY);
		generateCity(tracks, x, wallY + 1, wallX, y2);
		generateCity(tracks, wallX + 1, wallY + 1, x2, y2);
	}
}
