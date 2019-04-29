import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
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
				synchronized (TaxiGame.generationLock) {
					panel.tick();
				}
				panel.repaint();
			}
		}, 0, 1000 / 60);
	}

	public static Sound sound = new Sound();
	public static final int S_WIDTH = 1000, S_HEIGHT = 800, TILE_SIZE = 64, TRACK_PRICE = 25, MONEY_SPEND_SPEED = 3, SHOP_RADIUS = 25;
	public static final double CURVE_RADIUS = TILE_SIZE / 2.5;
	public static double SCREEN_SCALE = 1.75, MAX_RATING = 5.0;
	public static int money_in_engine = 0, money_in_gas = 0, money_in_friction = 0, money_in_capacity = 0, moneySpendCooldown;
	public static Track[][] tracks, plannedTracks;
	public static boolean predictStartLoop;
	public static boolean[][] predictTracks;
	public static Vector[] predictTracksArray;
	public static int[][] predictEnters, predictExits;
	public static int predictThickness = 4;
	public static double predictOpacity = 0.5;
	public static boolean predictOpacityUp = true;
	public static Vector nextTrack;
	public static Vector camera;
	public static double cameraAngle, visualCameraAngle, rating;
	public static int money, income, trackInvestment, trackStock, numTracks;
	public static boolean paused, mainMenu;
	public static InputHandler input;
	public static Vector taxiPredVelocity = new Vector(0, 1), taxiTile;
	public static ArrayList<Vector> trackShops, gasStations, upgradeShops;
	public static ArrayList<ArrayList<Vector>> locationsOfInterest;
	public static ArrayList<Particle> particles;
	public static ArrayList<Customer> customers;
	public static Customer[] myCustomers;
	public static boolean[] myCustomersGold;
	public static double[] myCustomersOpacity;
	public static ArrayList<Hotdog> hotdogs;
	public static Rectangle newGameButton;
	public static Object generationLock;
	public static Taxi taxi;
	public static int carrying;
	public static Color backColor = Color.black, frontColor = Color.white;

	// zoom variables
	public static double zoom = 1, visualZoom;
	public static final double MIN_ZOOM = 0.25, MAX_ZOOM = 2;
	// clouds
	public static Cloud[] clouds;

	public TaxiGame() {
		generationLock = new Object();
		newGameButton = new Rectangle(S_WIDTH / 2 - 100, S_HEIGHT / 2 - 50, 200, 100);
		mainMenu = true;
		input = new InputHandler();
		sound.startSound("music");

		this.setFocusable(true);
		this.requestFocus();
		this.addKeyListener(input);
		this.addMouseListener(input);
		this.addMouseMotionListener(input);

		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}

	public static void startNewGame() {
		taxi = new Taxi();
		visualZoom = 100.0;
		paused = false;
		rating = 2.0;
		trackStock = 0;
		cameraAngle = 0;
		particles = new ArrayList<Particle>();
		trackShops = new ArrayList<Vector>();
		gasStations = new ArrayList<Vector>();
		upgradeShops = new ArrayList<Vector>();
		locationsOfInterest = new ArrayList<ArrayList<Vector>>();
		locationsOfInterest.add(trackShops);
		locationsOfInterest.add(gasStations);
		locationsOfInterest.add(upgradeShops);
		moneySpendCooldown = 0;
		income = 50;
		money = 0;
		trackInvestment = 0;
		numTracks = 0;
		customers = new ArrayList<Customer>();

		taxi.location = new Vector(5.5 * TILE_SIZE, 5.5 * TILE_SIZE);
		taxi.start();
		camera = taxi.location.clone();
		tracks = new Track[30][30];
		plannedTracks = new Track[30][30];
		clouds = new Cloud[30];
		predictStartLoop = false;
		predictTracks = new boolean[30][30];
		predictTracksArray = new Vector[5];
		predictEnters = new int[30][30];
		predictExits = new int[30][30];
		nextTrack = new Vector(0, 0);
		for (int i = 0; i < predictTracksArray.length; i++) {
			predictTracksArray[i] = null;
		}
		for (int i = 0; i < predictEnters.length; i++) {
			for (int j = 0; j < predictEnters[0].length; j++) {
				predictEnters[i][j] = -1;
				predictExits[i][j] = -1;
			}
		}

		generateCity(plannedTracks);
		plannedTracks[5][5].up = true;
		plannedTracks[5][5].down = true;
		plannedTracks[5][6].up = true;
		plannedTracks[5][4].down = true;
		tracks[5][5] = plannedTracks[5][5];
		ArrayList<Point> possibilities = new ArrayList<Point>();
		if (plannedTracks[5][5].up) possibilities.add(new Point(5, 4));
		if (plannedTracks[5][5].right) possibilities.add(new Point(6, 5));
		if (plannedTracks[5][5].down) possibilities.add(new Point(5, 6));
		if (plannedTracks[5][5].left) possibilities.add(new Point(4, 5));

		// Start with 16 tracks
		for (int i = 0; i < 14; i++) {
			Point p = possibilities.remove((int) (Math.random() * possibilities.size()));
			addTrack(p.x, p.y);
			if (plannedTracks[p.x][p.y].up && tracks[p.x][p.y - 1] == null) possibilities.add(new Point(p.x, p.y - 1));
			if (plannedTracks[p.x][p.y].right && tracks[p.x + 1][p.y] == null) possibilities.add(new Point(p.x + 1, p.y));
			if (plannedTracks[p.x][p.y].down && tracks[p.x][p.y + 1] == null) possibilities.add(new Point(p.x, p.y + 1));
			if (plannedTracks[p.x][p.y].left && tracks[p.x - 1][p.y] == null) possibilities.add(new Point(p.x - 1, p.y));
		}

		generateClouds();

		myCustomers = new Customer[Taxi.MAX_MAX_CAPACITY];
		myCustomersGold = new boolean[Taxi.MAX_MAX_CAPACITY];
		myCustomersOpacity = new double[Taxi.MAX_MAX_CAPACITY];
		for (int i = 0; i < taxi.maxCustomers; i++) {
			myCustomers[i] = null;
			myCustomersGold[i] = false;
			myCustomersOpacity[i] = 0;
		}

		hotdogs = new ArrayList<Hotdog>();
		/*
		 * hotdogs.add(new Hotdog()); hotdogs.get(0).location = new Vector(5.5 *
		 * TILE_SIZE, 4.5 * TILE_SIZE); hotdogs.get(0).spawn();
		 */
	}

	public void tick() {
		if (paused || mainMenu) return;

		taxi.tick();

		// Hotdogs
		for (Hotdog hd : hotdogs) {
			hd.tick();
		}

		// Always have 4 clients to pick up
		while (customers.size() < Math.pow(numTracks, 0.58) - 1) {
			customers.add(Customer.generateCustomer());
		}

		// Update customers
		Iterator<Customer> iter = customers.iterator();
		while (iter.hasNext()) {
			Customer c = iter.next();
			c.update();
			if (c.visualFade <= 0) {
				for (int i = 0; i < taxi.maxCustomers; i++) {
					if (myCustomers[i] == c) {
						myCustomers[i] = null;
					}
				}
				iter.remove();
			}
		}

		// Update camera angle
		if (taxi.velocity.length() > 0.00001) {
			cameraAngle = -Math.atan(taxi.velocity.y / taxi.velocity.x) - Math.PI / 2 - (taxi.velocity.x < 0 ? Math.PI : 0);
		}

		// Deal with angle bug
		double camAdd = ((cameraAngle - visualCameraAngle + Math.PI) % (2 * Math.PI) - Math.PI);
		if (camAdd > Math.PI) {
			camAdd -= 2 * Math.PI;
		} else if (camAdd < -Math.PI) {
			camAdd += 2 * Math.PI;
		}

		// Update visual camera angle
		visualCameraAngle += camAdd / 16;

		// Adjust camera
		camera = camera.plus(taxi.location.minus(camera).scale(0.05));

		// Update visual zoom
		visualZoom += (zoom - visualZoom) / 4;

		// Clouds and birds
		for (int i = 0; i < clouds.length; i++) {
			clouds[i].Update();
		}

		// Update taxiTile
		taxiTile = new Vector((int) (taxi.location.x / TILE_SIZE), (int) (taxi.location.y / TILE_SIZE));

		// PredictOpacity
		if (predictOpacityUp) {
			predictOpacity += 0.0075;
			if (predictOpacity >= 0.75) predictOpacityUp = false;
		} else {
			predictOpacity -= 0.0075;
			if (predictOpacity <= 0.25) predictOpacityUp = true;
		}

		// Receive money
		money += Math.signum(income -= Math.signum(income));

		// Put money in track shops
		for (Vector v : trackShops) {
			if (taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
				if (money > 0 && moneySpendCooldown == 0) {
					money--;
					moneySpendCooldown = MONEY_SPEND_SPEED;
					trackInvestment++;
				}
			}
		}

		// Buy gas
		for (Vector v : gasStations) {
			if (taxi.location.distance2(v) <= 30 * 30 && taxi.velocity.length() < 0.5) {
				if (money > 0 && taxi.gas < taxi.maxGas - 0.5 && moneySpendCooldown == 0) {
					money--;
					moneySpendCooldown = MONEY_SPEND_SPEED;
					taxi.gas += 0.3;

					if (Math.random() < 0.7) {
						Vector velo = new Vector(Math.random() * 2 - 1, Math.random() * 2 - 1);
						velo.setLength(4 + 5 * Math.random());

						particles.add(new Particle.GasBlob(new Vector(S_WIDTH / 2, S_HEIGHT / 2), velo));
					}
				}
			}
		}

		// Buy upgrades
		for (int i = 0; i < upgradeShops.size(); i++) {
			Vector v = upgradeShops.get(i);
			if (money > 0 && moneySpendCooldown == 0 && taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
				money--;
				moneySpendCooldown = MONEY_SPEND_SPEED;
				if (i % 3 == 0) {// Engine
					money_in_engine++;
					if (money_in_engine % 30 == 0) {
						taxi.maxSpeed = -(Taxi.MAX_MAX_SPEED - Taxi.START_MAX_SPEED) / (.0005 * money_in_engine + 1) + Taxi.MAX_MAX_SPEED;
						taxi.acceleration = -(Taxi.MAX_ACCELERATION - Taxi.START_ACCELERATION) / (.01 * money_in_engine + 1) + Taxi.MAX_ACCELERATION;
						for (int p = 0; p < 30; p++) {
							Vector ve = new Vector(Math.random() * 2 - 1, Math.random() * 2 - 1);
							ve.setLength(ve.length());

							particles.add(new Particle.BrakeSpark(taxi.location.clone(), ve));
							particles.add(new Particle.Upgrade(taxi.location.clone(), ve, Color.green));
						}
					}
				} else if (i % 3 == 1) {// Fuel tank
					money_in_gas++;
					if (money_in_gas % 30 == 0) {
						taxi.maxGas = -(Taxi.MAX_MAX_GAS - Taxi.START_MAX_GAS) / (.005 * money_in_gas + 1) + Taxi.MAX_MAX_GAS;
						for (int p = 0; p < 30; p++) {
							Vector ve = new Vector(Math.random() * 2 - 1, Math.random() * 2 - 1);
							ve.setLength(ve.length());

							particles.add(new Particle.BrakeSpark(taxi.location.clone(), ve));
							particles.add(new Particle.Upgrade(taxi.location.clone(), ve, new Color(175, 150, 50)));
						}
					}
				} else if (i % 3 == 2) {// Max customers
					money_in_capacity++;
					if (money_in_capacity % 30 == 0) {
						taxi.maxCustomers++;
						for (int p = 0; p < 30; p++) {
							Vector ve = new Vector(Math.random() * 2 - 1, Math.random() * 2 - 1);
							ve.setLength(ve.length());

							particles.add(new Particle.BrakeSpark(taxi.location.clone(), ve));
							particles.add(new Particle.Upgrade(taxi.location.clone(), ve, new Color(245, 170, 30)));
						}
					}
				}
			}
		}

		if (trackInvestment >= TRACK_PRICE) {
			trackInvestment -= TRACK_PRICE;
			trackStock++;
			sound.playSound("res/money.wav");
		}

		if (taxi.gas < 0) {
			taxi.gas = 0;
		}
		if (rating < 0) {
			rating = 0;
		} else if (rating > MAX_RATING) {
			rating = MAX_RATING;
		}

		if (moneySpendCooldown > 0) {
			moneySpendCooldown--;
		}

		// Create spark particles when breaking
		if (input.down && Math.random() < 0.3 && taxi.velocity.length() > 1) {
			Vector ve = taxi.velocity.scale(-1).plus(Math.random() * 2 - 1, Math.random() * 2 - 1);
			ve.setLength(ve.length() * 0.6);

			particles.add(new Particle.BrakeSpark(taxi.location.clone(), ve));
		}

		Iterator<Particle> iterP = particles.iterator();
		while (iterP.hasNext()) {
			Particle p = iterP.next();

			p.age++;
			p.update();
			if (p.remove) {
				iterP.remove();
			}
		}
	}

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
		} else {
			// Draw background
			g.setColor(backColor);
			g.fillRect(0, 0, S_WIDTH, S_HEIGHT);
		}

		// Manipulate rendering camera in space
		g.translate(S_WIDTH / 2, S_HEIGHT / 2);
		g.rotate(visualCameraAngle);
		g.scale(SCREEN_SCALE, SCREEN_SCALE);
		g.scale(visualZoom, visualZoom);
		g.translate(-camera.x, -camera.y);

		// Predicted path
		for (int i = 0; i < predictTracksArray.length; i++) {
			if (predictTracksArray[i] == null) break;
			predictTracks[(int) (predictTracksArray[i].x)][(int) (predictTracksArray[i].y)] = false;
			predictTracksArray[i] = null;
		}

		Vector prevTile, currentTile;
		int dir = getNextDir();
		currentTile = taxiTile.clone();
		int curx = (int) (currentTile.x);
		int cury = (int) (currentTile.y);
		predictTracks[curx][cury] = true;
		predictEnters[curx][cury] = 0;
		predictExits[curx][cury] = dir;
		predictTracksArray[0] = currentTile.clone();
		prevTile = currentTile.clone();

		int iter = 1;
		while (iter < predictTracksArray.length) {
			dir = predictExits[(int) (prevTile.x)][(int) (prevTile.y)];
			if (dir == 0)
				currentTile = new Vector(prevTile.x + 1, prevTile.y);
			else if (dir == 1)
				currentTile = new Vector(prevTile.x, prevTile.y - 1);
			else if (dir == 2)
				currentTile = new Vector(prevTile.x - 1, prevTile.y);
			else
				currentTile = new Vector(prevTile.x, prevTile.y + 1);
			curx = (int) (currentTile.x);
			cury = (int) (currentTile.y);
			if (predictTracks[curx][cury] && !(predictTracksArray[0].x == curx && predictTracksArray[0].y == cury)) break;
			predictTracks[curx][cury] = true;
			predictEnters[curx][cury] = (dir + 2) % 4;
			predictExits[curx][cury] = getTrackDirection(plannedTracks[curx][cury], dir);
			predictTracksArray[iter] = currentTile.clone();
			if (predictTracksArray[0].x == curx && predictTracksArray[0].y == cury) {
				predictStartLoop = true;
				break;
			}
			if (tracks[curx][cury] == null) break;
			prevTile = currentTile.clone();
			iter++;
		}

		// Draw tracks
		g.setColor(frontColor);
		g.setStroke(new BasicStroke(2));
		final double TS = TILE_SIZE;
		final double CR = CURVE_RADIUS;
		for (int x = 0; x < tracks.length; x++) {
			for (int y = 0; y < tracks[x].length; y++) {
				boolean drawThis = false;
				double drawX1 = x * TS + TS / 2;
				double drawY1 = y * TS + TS / 2;
				if (predictTracks[x][y] && tracks[x][y] == null) {
					drawThis = true;
					g.setColor(new Color(0, 0, 0, 100));
				} else if (tracks[x][y] != null) {
					drawThis = true;
					g.setColor(frontColor);
					g.setStroke(new BasicStroke((int) (2 * visualZoom)));
				}
				if (drawThis) {
					if (plannedTracks[x][y].right) drawMapLine(g, drawX1 + CR, drawY1, (x + 1) * TS, drawY1);
					if (plannedTracks[x][y].up) drawMapLine(g, drawX1, drawY1 - CR, drawX1, y * TS);
					if (plannedTracks[x][y].left) drawMapLine(g, drawX1 - CR, drawY1, x * TS, drawY1);
					if (plannedTracks[x][y].down) drawMapLine(g, drawX1, drawY1 + CR, drawX1, (y + 1) * TS);
					if (plannedTracks[x][y].right && plannedTracks[x][y].left) drawMapLine(g, drawX1 + CR, drawY1, drawX1 - CR, drawY1);
					if (plannedTracks[x][y].up && plannedTracks[x][y].down) drawMapLine(g, drawX1, drawY1 - CR, drawX1, drawY1 + CR);
					if (plannedTracks[x][y].right && plannedTracks[x][y].up) drawMapArc(g, drawX1, drawY1 - CR * 2, CR * 2, CR * 2, -90, -90);
					if (plannedTracks[x][y].up && plannedTracks[x][y].left) drawMapArc(g, drawX1 - CR * 2, drawY1 - CR * 2, CR * 2, CR * 2, 0, -90);
					if (plannedTracks[x][y].left && plannedTracks[x][y].down) drawMapArc(g, drawX1 - CR * 2, drawY1, CR * 2, CR * 2, 90, -90);
					if (plannedTracks[x][y].down && plannedTracks[x][y].right) drawMapArc(g, drawX1, drawY1, CR * 2, CR * 2, 180, -90);
				}
				if (predictTracks[x][y]) {
					if (x == taxiTile.x && y == taxiTile.y) {
						g.setColor(new Color(255, 0, 0, (int) (255 * predictOpacity)));
						g.setStroke(new BasicStroke((int) (predictThickness * visualZoom)));
						Vector taxiModTile = new Vector(taxi.location.x % TS, taxi.location.y % TS);
						double taxiX = taxi.location.x, taxiY = taxi.location.y;
						double taxiTileX = (taxiTile.x + 0.5) * TS, taxiTileY = (taxiTile.y + 0.5) * TS;
						double TS2 = TS / 2;
						double startAng = 0, ang = 0;
						switch (predictExits[(int) (taxiTile.x)][(int) (taxiTile.y)]) {
						case 0:
							// Exiting to right
							if (taxiModTile.x > TS2 + CR) {
								// Up to the last bit
								drawMapLine(g, taxiX, taxiY, taxiTileX + TS2, taxiY);
							} else if (taxiModTile.y == TS2) {
								// Straight
								drawMapLine(g, taxiX, taxiY, taxiTileX + TS2, taxiY);
							} else {
								// Not up to last bit
								drawMapLine(g, taxiTileX + CR, taxiTileY, taxiTileX + TS2, taxiTileY);
								if (taxiModTile.y < TS2) {
									// Entering from up
									if (taxiModTile.y < TS2 - CR) {
										// On up-most bit
										drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY - CR);
										startAng = 180;
										ang = 90;
									} else {
										Vector centerPoint = new Vector(TS2 + CR, TS2 - CR);
										ang = Math.toDegrees(Math.atan((taxiModTile.x - centerPoint.x) / -(taxiModTile.y - centerPoint.y)));
										startAng = 180 + 90 - ang;
									}
									drawMapArc(g, drawX1, drawY1 - CR * 2, CR * 2, CR * 2, startAng, ang);
								} else {
									// Entering from down
									if (taxiModTile.y > TS2 + CR) {
										// On down-most bit
										drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY + CR);
										startAng = 180;
										ang = -90;
									} else {
										Vector centerPoint = new Vector(TS2 + CR, TS2 + CR);
										ang = Math.toDegrees(Math.atan((taxiModTile.x - centerPoint.x) / -(taxiModTile.y - centerPoint.y)));
										startAng = 180 - 90 - ang;
									}
									drawMapArc(g, drawX1, drawY1, CR * 2, CR * 2, startAng, ang);
								}
							}
							break;
						case 1:
							// Exiting to up
							if (taxiModTile.y < TS2 - CR) {
								// Up to last bit
								drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY - TS2);
							} else if (taxiModTile.x == TS2) {
								// Straight
								drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY - TS2);
							} else {
								// Not up to last bit
								drawMapLine(g, taxiTileX, taxiTileY - CR, taxiTileX, taxiTileY - TS2);
								if (taxiModTile.x < TS2) {
									// Entering from left
									if (taxiModTile.x < TS2 - CR) {
										// On leftmost bit
										drawMapLine(g, taxiX, taxiY, taxiTileX - CR, taxiY);
										startAng = 270;
										ang = 90;
									} else {
										Vector centerPoint = new Vector(TS2 - CR, TS2 - CR);
										ang = -Math.toDegrees(Math.atan(-(taxiModTile.y - centerPoint.y) / (taxiModTile.x - centerPoint.x)));
										startAng = 270 + 90 - ang;
									}
									drawMapArc(g, drawX1 - CR * 2, drawY1 - CR * 2, CR * 2, CR * 2, startAng, ang);
								} else {
									// Entering from right
									if (taxiModTile.x > TS2 + CR) {
										// On rightmost bit
										drawMapLine(g, taxiX, taxiY, taxiTileX + CR, taxiY);
										startAng = 270;
										ang = -90;
									} else {
										Vector centerPoint = new Vector(TS2 + CR, TS2 - CR);
										ang = -Math.toDegrees(Math.atan(-(taxiModTile.y - centerPoint.y) / (taxiModTile.x - centerPoint.x)));
										startAng = 270 - 90 - ang;
									}
									drawMapArc(g, drawX1, drawY1 - CR * 2, CR * 2, CR * 2, startAng, ang);
								}
							}
							break;
						case 2:
							// Exiting to left
							if (taxiModTile.x < TS2 - CR) {
								// Up to last bit
								drawMapLine(g, taxiX, taxiY, taxiTileX - TS2, taxiY);
							} else if (taxiModTile.y == TS2) {
								// Straight
								drawMapLine(g, taxiX, taxiY, taxiTileX - TS2, taxiY);
							} else {
								// Not up to last bit
								drawMapLine(g, taxiTileX - CR, taxiTileY, taxiTileX - TS2, taxiTileY);
								if (taxiModTile.y > TS2) {
									// Entering from down
									if (taxiModTile.y > TS2 + CR) {
										// On down-most bit
										drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY + CR);
										startAng = 0;
										ang = 90;
									} else {
										Vector centerPoint = new Vector(TS2 - CR, TS2 + CR);
										ang = Math.toDegrees(Math.atan((taxiModTile.x - centerPoint.x) / -(taxiModTile.y - centerPoint.y)));
										startAng = 0 + 90 - ang;
									}
									drawMapArc(g, drawX1 - CR * 2, drawY1, CR * 2, CR * 2, startAng, ang);
								} else {
									// Entering from up
									if (taxiModTile.y < TS2 - CR) {
										// On up-most bit
										drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY - CR);
										startAng = 0;
										ang = -90;
									} else {
										Vector centerPoint = new Vector(TS2 - CR, TS2 - CR);
										ang = Math.toDegrees(Math.atan((taxiModTile.x - centerPoint.x) / -(taxiModTile.y - centerPoint.y)));
										startAng = 0 - 90 - ang;
									}
									drawMapArc(g, drawX1 - CR * 2, drawY1 - CR * 2, CR * 2, CR * 2, startAng, ang);
								}
							}
							break;
						case 3:
							// Exiting to down
							if (taxiModTile.y > TS2 + CR) {
								// Up to last bit
								drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY + TS2);
							} else if (taxiModTile.x == TS2) {
								// Straight
								drawMapLine(g, taxiX, taxiY, taxiX, taxiTileY + TS2);
							} else {
								// Not up to last bit
								drawMapLine(g, taxiTileX, taxiTileY + CR, taxiTileX, taxiTileY + TS2);
								if (taxiModTile.x > TS2) {
									// Entering from right
									if (taxiModTile.x > TS2 + CR) {
										// On rightmost bit
										drawMapLine(g, taxiX, taxiY, taxiTileX + CR, taxiY);
										startAng = 90;
										ang = 90;
									} else {
										Vector centerPoint = new Vector(TS2 + CR, TS2 + CR);
										ang = -Math.toDegrees(Math.atan(-(taxiModTile.y - centerPoint.y) / (taxiModTile.x - centerPoint.x)));
										startAng = 90 + 90 - ang;
									}
									drawMapArc(g, drawX1, drawY1, CR * 2, CR * 2, startAng, ang);
								} else {
									// Entering from left
									if (taxiModTile.x < TS2 - CR) {
										// On leftmost bit
										drawMapLine(g, taxiX, taxiY, taxiTileX - CR, taxiY);
										startAng = 90;
										ang = -90;
									} else {
										Vector centerPoint = new Vector(TS2 - CR, TS2 + CR);
										ang = -Math.toDegrees(Math.atan(-(taxiModTile.y - centerPoint.y) / (taxiModTile.x - centerPoint.x)));
										startAng = 90 - 90 - ang;
									}
									drawMapArc(g, drawX1 - CR * 2, drawY1, CR * 2, CR * 2, startAng, ang);
								}
							}
							break;
						}
					} else if ((x == taxiTile.x && y == taxiTile.y && predictStartLoop) || (!(x == taxiTile.x && y == taxiTile.y) && tracks[x][y] != null)) {
						g.setColor(new Color(255, 0, 0, (int) (255 * predictOpacity)));
						g.setStroke(new BasicStroke((int) (predictThickness * visualZoom)));
						boolean right = predictEnters[x][y] == 0 || predictExits[x][y] == 0;
						boolean up = predictEnters[x][y] == 1 || predictExits[x][y] == 1;
						boolean left = predictEnters[x][y] == 2 || predictExits[x][y] == 2;
						boolean down = predictEnters[x][y] == 3 || predictExits[x][y] == 3;
						if (right) drawMapLine(g, drawX1 + CR, drawY1, (x + 1) * TS, drawY1);
						if (up) drawMapLine(g, drawX1, drawY1 - CR, drawX1, y * TS);
						if (left) drawMapLine(g, drawX1 - CR, drawY1, x * TS, drawY1);
						if (down) drawMapLine(g, drawX1, drawY1 + CR, drawX1, (y + 1) * TS);
						if (right && left) drawMapLine(g, drawX1 + CR, drawY1, drawX1 - CR, drawY1);
						if (up && down) drawMapLine(g, drawX1, drawY1 - CR, drawX1, drawY1 + CR);
						if (right && up) drawMapArc(g, drawX1, drawY1 - CR * 2, CR * 2, CR * 2, -90, -90);
						if (up && left) drawMapArc(g, drawX1 - CR * 2, drawY1 - CR * 2, CR * 2, CR * 2, 0, -90);
						if (left && down) drawMapArc(g, drawX1 - CR * 2, drawY1, CR * 2, CR * 2, 90, -90);
						if (down && right) drawMapArc(g, drawX1, drawY1, CR * 2, CR * 2, 180, -90);
					}
				}
			}
		}
		g.setStroke(new BasicStroke((int) (2 * visualZoom)));

		// Draw taxi
		g.setColor(Color.yellow);
		drawMapOval(g, taxi.location.x, taxi.location.y, 10, 10, true);

		// Draw clients
		for (int i = 0; i < customers.size(); i++) {
			Customer cust = customers.get(i);
			Vector c = cust.position, d = cust.destination;
			// draw dropping off
			if (cust.droppedOff) {
				g.setColor(new Color(255, 165, 0, (int) (255 * cust.visualFade)));
				drawMapOval(g, c.x, c.y, 5, 5, true);
			} else if (cust.pickedUp) {

			} else {
				int cR = cust.goldMember ? 255 : 245;
				int cG = cust.goldMember ? 235 : 170;
				int cB = cust.goldMember ? 95 : 30;
				double diameter = Customer.PICKUP_RADIUS * 2 * cust.radiusShrink;
				// draw static fill
				g.setColor(new Color(cR, cG, cB, (int) (255 * cust.staticFillOpacity)));
				drawMapOval(g, c.x, c.y, diameter, diameter, true);
				// draw changing fill
				g.setColor(new Color(cR, cG, cB, (int) (255 * cust.fillOpacity)));
				drawMapOval(g, c.x, c.y, Customer.PICKUP_RADIUS * 2 * cust.fillRadius, Customer.PICKUP_RADIUS * 2 * cust.fillRadius, true);
				// draw customer
				g.setColor(new Color(cR, cG, cB, (int) (255 * cust.angerBlink)));
				drawMapOval(g, c.x, c.y, 5, 5, true);
				// draw radius
				g.setColor(new Color(cR, cG, cB, (int) (125 * cust.angerBlink)));
				g.setStroke(new BasicStroke((int) (2 * visualZoom)));
				drawMapOval(g, c.x, c.y, diameter, diameter, false);
				// draw anger
				double angerRatio = cust.anger;
				angerRatio /= cust.maxAnger;
				cG -= cG * angerRatio;
				cB -= cB * angerRatio;
				g.setColor(new Color(cR, cG, cB, (int) (255 * cust.angerBlink)));
				drawMapArc(g, c.x - diameter / 2, c.y - diameter / 2, diameter, diameter, 90, 360 * (cust.maxAnger - cust.anger) / cust.maxAnger, 1);
			}
			// draw destination
			if (cust.pickedUp || cust.droppedOff) {
				g.setColor(new Color(200, 0, 200, (int) (128 * cust.radiusShrink * cust.fillOpacity)));
				drawMapOval(g, d.x, d.y, Customer.PICKUP_RADIUS * 2 * (1 / cust.radiusShrink), TILE_SIZE / 1.5 * 2 * (1 / cust.radiusShrink), true);
			}
		}

		// Draw hotdogs
		for (Hotdog hd : hotdogs) {
			Vector c = hd.location;
			g.setColor(Color.orange);
			drawMapOval(g, c.x, c.y, hd.radius, hd.radius, true);
		}

		// Draw shops
		for (Vector v : trackShops) {
			g.setColor(new Color(25, 0, 255));
			drawMapOval(g, v.x, v.y, 10, 10, true);
			drawMapOval(g, v.x, v.y, SHOP_RADIUS * 2, SHOP_RADIUS * 2, false);

			double d = taxi.location.distance(v);
			g.setColor(new Color(25, 0, 255, (int) (64 + (d < 150 ? 96 * (1 - d / 150) : 1))));
			g.fillArc((int) (v.x - SHOP_RADIUS), (int) (v.y - SHOP_RADIUS), SHOP_RADIUS * 2, SHOP_RADIUS * 2, 0, (int) (360.0 * trackInvestment / 25));
		}
		g.setColor(new Color(175, 150, 50));
		for (Vector v : gasStations) {
			g.fillRect((int) v.x - 5, (int) v.y - 5, 10, 10);
			drawMapOval(g, v.x, v.y, 60, 60, false);
		}
		// upgrade shops
		for (int i = 0; i < upgradeShops.size(); i++) {
			Vector v = upgradeShops.get(i);
			if (i % 3 == 0) {
				g.setColor(Color.green);
				drawMapOval(g, v.x, v.y, 10, 10, true);
				drawMapOval(g, v.x, v.y, SHOP_RADIUS * 2, SHOP_RADIUS * 2, false);

				double d = taxi.location.distance(v);
				g.setColor(new Color(0, 255, 0, (int) (64 + (d < 150 ? 96 * (1 - d / 150) : 1))));
				g.fillArc((int) (v.x - SHOP_RADIUS), (int) (v.y - SHOP_RADIUS), SHOP_RADIUS * 2, SHOP_RADIUS * 2, 0, (int) (360.0 * (money_in_engine % 30) / 25));
			} else if (i % 3 == 1) {
				g.setColor(new Color(175, 150, 50));
				drawMapOval(g, v.x, v.y, 10, 10, true);
				drawMapOval(g, v.x, v.y, SHOP_RADIUS * 2, SHOP_RADIUS * 2, false);

				double d = taxi.location.distance(v);
				g.setColor(new Color(175, 150, 50, (int) (64 + (d < 150 ? 96 * (1 - d / 150) : 1))));
				g.fillArc((int) (v.x - SHOP_RADIUS), (int) (v.y - SHOP_RADIUS), SHOP_RADIUS * 2, SHOP_RADIUS * 2, 0, (int) (360.0 * (money_in_gas % 30) / 25));
			} else if (i % 3 == 2) {
				g.setColor(new Color(245, 170, 30));
				drawMapOval(g, v.x, v.y, 10, 10, true);
				drawMapOval(g, v.x, v.y, SHOP_RADIUS * 2, SHOP_RADIUS * 2, false);

				double d = taxi.location.distance(v);
				g.setColor(new Color(245, 170, 30, (int) (64 + (d < 150 ? 96 * (1 - d / 150) : 1))));
				g.fillArc((int) (v.x - SHOP_RADIUS), (int) (v.y - SHOP_RADIUS), SHOP_RADIUS * 2, SHOP_RADIUS * 2, 0, (int) (360.0 * (money_in_capacity % 30) / 25));
			}
		}

		// Draw world particles
		for (Particle p : particles) {
			if (!p.UI) {
				p.paint(g);
			}
		}

		// fun stuff
		for (int i = 0; i < clouds.length; i++) {
			if (clouds[i].actuallyBird) {
				g.setColor(new Color(0, 0, 0, 255));
			} else {
				g.setColor(new Color(255, 255, 255, 150));
			}
			drawMapOval(g, clouds[i].x, clouds[i].y, clouds[i].size, clouds[i].size, clouds[i].myZoom, true);
		}

		// Unmanipulate the camera in space
		g.translate(camera.x, camera.y);
		g.scale(1 / visualZoom, 1 / visualZoom);
		g.scale(1 / SCREEN_SCALE, 1 / SCREEN_SCALE);
		g.rotate(-visualCameraAngle);
		g.translate(-S_WIDTH / 2, -S_HEIGHT / 2);

		// Draw money
		g.setFont(new Font("Dialog", Font.PLAIN, 12));
		g.setColor(frontColor);
		g.drawString("$" + money, 5, 13);

		for (Hotdog h : hotdogs) {
			g.drawString(Boolean.toString(h.collision), 50, 50);
		}

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

		// Draw upgradeable stats
		drawString(g, "Max Speed: " + taxi.maxSpeed + "\nAcceleration: " + taxi.acceleration + "\nMAX_GAS: " + taxi.maxGas, 5, 38);

		// Draw customer capacity
		carrying = 0;
		for (int i = 0; i < customers.size(); i++) {
			if (customers.get(i).pickedUp) {
				carrying++;
			}
		}
		g.setFont(new Font("Dialog", Font.PLAIN, 18));
		drawString(g, "Customers: " + carrying + "/" + taxi.maxCustomers, 5, 80);
		g.setFont(new Font("Dialog", Font.PLAIN, 12));

		// Draw gas
		g.setColor(new Color(175, 150, 50));
		g.fillRoundRect(10, S_HEIGHT - 80, 100, 70, 10, 10);
		if (taxi.gas < taxi.blinkGasThreshold) {
			g.setColor(new Color(255, 0, 0, (int) (255 * taxi.gasBlink)));
			g.fillRoundRect(10, S_HEIGHT - 80, 100, 70, 10, 10);
		}
		g.setColor(Color.black);
		g.setFont(new Font("Dialog", Font.PLAIN, 12));
		g.drawString("E", 20, S_HEIGHT - 20);
		g.drawString("F", 94, S_HEIGHT - 20);
		g.setColor(Color.red);
		g.drawLine(60, S_HEIGHT - 20, (int) (40 * Math.cos((taxi.maxGas - taxi.gas) / taxi.maxGas * Math.PI)) + 60,
				(int) -(40 * Math.sin((taxi.maxGas - taxi.gas) / taxi.maxGas * Math.PI)) + S_HEIGHT - 20);

		// Draw carrying
		for (int i = 0; i < taxi.maxCustomers; i++) {
			Customer cust = myCustomers[i];
			g.setColor(new Color(255, 255, 255, 100));
			g.drawOval(10 + i * 20, S_HEIGHT - 100, 20, 20);
			double mco = myCustomersOpacity[i];
			if (cust == null) {
				if (mco > 0) {
					myCustomersOpacity[i] -= 0.05;
				}
			} else {
				if (mco < 1) {
					myCustomersOpacity[i] += 0.05;
				}
				if (mco > 0) {
					int cR = cust.goldMember ? 255 : 245;
					int cG = cust.goldMember ? 235 : 170;
					int cB = cust.goldMember ? 95 : 30;
					double mcoAnger = mco;
					if (cust.anger > cust.blinkAngerThreshold) {
						mcoAnger *= cust.angerBlink;
					}
					if (!myCustomersGold[i]) {
						g.setColor(new Color(cR, cG, cB, (int) (255 * mcoAnger)));
					} else {
						g.setColor(new Color(cR, cG, cB, (int) (255 * mcoAnger)));
					}
					g.fillOval(10 + i * 20, (int) (S_HEIGHT - 150 + (50 * mco)), 20, 20);
					double angerRatio = cust.anger;
					angerRatio /= cust.maxAnger;
					cG -= cG * angerRatio;
					cB -= cB * angerRatio;
					g.setColor(new Color(cR, cG, cB, (int) (255 * cust.angerBlink)));
					g.drawArc(10 + i * 20, (int) (S_HEIGHT - 150 + (50 * mco)), 20, 20, 90, 360 * (cust.maxAnger - cust.anger) / cust.maxAnger);
				}
			}
		}

		// Draw compass
		int startLineX = S_WIDTH - 70;
		int startLineY = S_HEIGHT - 70;
		int endLineX = (int) (Math.round(S_WIDTH - 70 + 50 * Math.sin(visualCameraAngle)));
		int endLineY = (int) (Math.round(S_HEIGHT - 70 + 50 * -Math.cos(visualCameraAngle)));
		g.setColor(frontColor);
		g.setStroke(new BasicStroke(2));
		g.drawLine(startLineX, startLineY, endLineX, endLineY);
		startLineX = (int) (Math.round(S_WIDTH - 70 + 5 * Math.cos(visualCameraAngle)));
		startLineY = (int) (Math.round(S_HEIGHT - 70 + 5 * Math.sin(visualCameraAngle)));
		g.drawLine(startLineX, startLineY, endLineX, endLineY);
		startLineX = (int) (Math.round(S_WIDTH - 70 - 5 * Math.cos(visualCameraAngle)));
		startLineY = (int) (Math.round(S_HEIGHT - 70 - 5 * Math.sin(visualCameraAngle)));
		g.drawLine(startLineX, startLineY, endLineX, endLineY);
		g.setColor(Color.blue);
		g.drawString("N", (int) (Math.round(S_WIDTH - 70 + 50 * Math.sin(visualCameraAngle))), (int) (Math.round(S_HEIGHT - 70 + 50 * -Math.cos(visualCameraAngle))));

		// Draw UI particles
		for (Particle p : particles) {
			if (p.UI) {
				p.paint(g);
			}
		}

		// Draw Game Over
		g.setFont(new Font("Dialog", Font.PLAIN, 12));
		if (taxi.velocity.length() < 0.0000001 && taxi.gas < 0.000001) {
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

		// Draw help
		g.setColor(Color.black);
		g.setFont(new Font("Dialog", Font.PLAIN, 12));
		g.drawString("Hold H to show controls", 150, 12);
		if (input.help) {
			g.setColor(new Color(255, 255, 255, 200));
			g.fillRect(100, 100, S_WIDTH - 200, S_HEIGHT - 200);
			g.setColor(Color.black);
			g.drawRect(100, 100, S_WIDTH - 200, S_HEIGHT - 200);
			drawString(g, "WASD - movement\nR - restart\nBACKSPACE - exit to menu\nZXCV - zoom controls\nIOP - god mode controls", 150, 150);
		}
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

	private static void generateClouds() {
		for (int i = 0; i < clouds.length; i++) {
			clouds[i] = new Cloud();
		}
	}

	private int getNextDir() {
		Vector taxiModTile = new Vector(taxi.location.x % TILE_SIZE, taxi.location.y % TILE_SIZE);
		int hts = TILE_SIZE / 2;
		int dir = (int) (mod(Math.round(cameraAngle / (Math.PI / 2)) + 1, 4));

		if (dir == 0 && taxiModTile.x < hts - 25 || dir == 1 && taxiModTile.y > hts + 25 || dir == 2 && taxiModTile.x > hts + 25 || dir == 3 && taxiModTile.y < hts - 25) {
			dir = getTrackDirection(tracks[(int) (taxiTile.x)][(int) (taxiTile.y)], dir);
		} else {
			if (taxiModTile.x == hts || taxiModTile.y == hts) {
			} else if (taxiModTile.x < hts && taxiModTile.y < hts) {
				if (dir == 0 || dir == 1)
					dir = 1;
				else
					dir = 2;
			} else if (taxiModTile.x > hts && taxiModTile.y < hts) {
				if (dir == 1 || dir == 2)
					dir = 1;
				else
					dir = 0;
			} else if (taxiModTile.x < hts && taxiModTile.y > hts) {
				if (dir == 1 || dir == 2)
					dir = 2;
				else
					dir = 3;
			} else {
				if (dir == 0 || dir == 1)
					dir = 0;
				else
					dir = 3;
			}
		}
		return dir;
	}

	private int getTrackDirection(Track track, int startDir) {
		boolean[] trackDirs = new boolean[4];
		trackDirs[0] = track.right;
		trackDirs[1] = track.up;
		trackDirs[2] = track.left;
		trackDirs[3] = track.down;
		if (input.left) {
			if (trackDirs[(int) (mod((startDir + 1), 4))])
				return (int) (mod((startDir + 1), 4));
			else {
				if (trackDirs[startDir])
					return (startDir);
				else
					return (int) (mod((startDir - 1), 4));
			}
		} else if (input.right) {
			if (trackDirs[(int) (mod((startDir - 1), 4))])
				return (int) (mod((startDir - 1), 4));
			else {
				if (trackDirs[startDir])
					return (startDir);
				else
					return (int) (mod((startDir + 1), 4));
			}
		} else {
			if (trackDirs[startDir])
				return (startDir);
			else if (trackDirs[(int) (mod((startDir - 1), 4))])
				return (int) (mod((startDir - 1), 4));
			else
				return (int) (mod((startDir + 1), 4));
		}
	}

	private static void drawMapLine(Graphics2D graphics, double lineX1, double lineY1, double lineX2, double lineY2) {
		drawMapLine(graphics, lineX1, lineY1, lineX2, lineY2, 1);
	}

	private static void drawMapLine(Graphics2D graphics, double lineX1, double lineY1, double lineX2, double lineY2, double zoomLevel) {
		if (visualZoom > zoomLevel && !(visualZoom > 1 && zoomLevel == 1)) return;
		double finalZoom = zoomLevel;
		graphics.drawLine((int) Math.round(finalZoom * lineX1), (int) Math.round(finalZoom * lineY1), (int) Math.round(finalZoom * lineX2), (int) Math.round(finalZoom * lineY2));
	}

	private static void drawMapArc(Graphics2D graphics, double arcX, double arcY, double arcW, double arcH, double startAng, double endAng) {
		drawMapArc(graphics, arcX, arcY, arcW, arcH, startAng, endAng, 1);
	}

	private static void drawMapArc(Graphics2D graphics, double arcX, double arcY, double arcW, double arcH, double startAng, double endAng, double zoomLevel) {
		if (visualZoom > zoomLevel && !(visualZoom > 1 && zoomLevel == 1)) return;
		double finalZoom = zoomLevel;
		graphics.drawArc((int) Math.round(finalZoom * arcX), (int) Math.round(finalZoom * arcY), (int) Math.round(finalZoom * arcW), (int) Math.round(finalZoom * arcH),
				(int) (startAng), (int) (endAng));
	}

	// for ovals, ovalX and ovalY are the center of the oval. They also take a
	// boolean "fill" that determines if the oval is filled in or just an outline
	private static void drawMapOval(Graphics2D graphics, double ovalX, double ovalY, double ovalW, double ovalH, boolean fill) {
		drawMapOval(graphics, ovalX, ovalY, ovalW, ovalH, 1, fill);
	}

	private static void drawMapOval(Graphics2D graphics, double ovalX, double ovalY, double ovalW, double ovalH, double zoomLevel, boolean fill) {
		if (visualZoom > zoomLevel && !(visualZoom > 1 && zoomLevel == 1)) return;
		double finalZoom = zoomLevel;
		if (fill)
			graphics.fillOval((int) (finalZoom * (ovalX - ovalW / 2)), (int) (finalZoom * (ovalY - ovalH / 2)), (int) (finalZoom * ovalW / 2 * 2),
					(int) (finalZoom * ovalH / 2 * 2));
		else
			graphics.drawOval((int) (finalZoom * (ovalX - ovalW / 2)), (int) (finalZoom * (ovalY - ovalH / 2)), (int) (finalZoom * ovalW / 2 * 2),
					(int) (finalZoom * ovalH / 2 * 2));
	}

	private void drawString(Graphics g, String text, int x, int y) {
		for (String line : text.split("\n"))
			g.drawString(line, x, y += g.getFontMetrics().getHeight());
	}

	private double mod(double a, double b) {
		// this function is just the modulus operator but it works with negative numbers
		// too, always producing a positive number
		double ret = a % b;
		while (ret < 0) {
			ret += b;
		}
		return ret;
	}

	public static void addTrack(int x, int y) {
		tracks[x][y] = plannedTracks[x][y];
		numTracks++;

		double leftRight = Math.signum(Math.random() - 0.5), upDown = Math.signum(Math.random() - 0.5);
		// If the location would spawn out of reach on curved tracks, put it where the
		// center of the track would be
		if (leftRight < 0 && upDown < 0 && !tracks[x][y].left && !tracks[x][y].up) leftRight = upDown = 0;
		if (leftRight < 0 && upDown > 0 && !tracks[x][y].left && !tracks[x][y].down) leftRight = upDown = 0;
		if (leftRight > 0 && upDown < 0 && !tracks[x][y].right && !tracks[x][y].up) leftRight = upDown = 0;
		if (leftRight > 0 && upDown > 0 && !tracks[x][y].right && !tracks[x][y].down) leftRight = upDown = 0;
		Vector spawnLocation = new Vector((x + 0.5) * TILE_SIZE + 15 * leftRight, (y + 0.5) * TILE_SIZE + 15 * upDown);

		if (numTracks == 2) {
			trackShops.add(spawnLocation);
		} else if (numTracks == 8) {
			gasStations.add(spawnLocation);
		} else if (numTracks % 20 == 7) {
			upgradeShops.add(spawnLocation);
		} else if (numTracks % 20 == 14) {
			upgradeShops.add(spawnLocation);
		} else if (numTracks % 20 == 19) {
			upgradeShops.add(spawnLocation);
		}
	}
}