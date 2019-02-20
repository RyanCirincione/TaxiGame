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

	public static final int S_WIDTH = 1000, S_HEIGHT = 800, TILE_SIZE = 64, TRACK_PRICE = 25;
	public static final double CURVE_RADIUS = TILE_SIZE / 2.5;
	public static double SCREEN_SCALE = 1.75, MAX_RATING = 5.0;
	public static int money_in_speed = 0, money_in_acceleration = 0, money_in_gas = 0, money_in_friction = 0;
	public static Track[][] tracks, plannedTracks;
	public static boolean predictStartLoop;
	public static boolean[][] predictTracks;
	public static Vector[] predictTracksArray;
	public static int[][] predictEnters, predictExits;
	public static double predictOpacity = 0.5;
	public static boolean predictOpacityUp = true;
	public static Vector nextTrack;
	public static Vector camera;
	public static double cameraAngle, visualCameraAngle, rating;
	public static int money, income, trackInvestment, trackStock;
	public static boolean paused, mainMenu;
	public static InputHandler input;
	public static Vector taxiPredVelocity = new Vector(0, 1), taxiTile;
	public static int upgradeShopCount = 4;
	public static ArrayList<Vector> trackShops, gasStations, upgradeShops;
	public static ArrayList<ArrayList<Vector>> locationsOfInterest;
	public static ArrayList<Customer> customers;
	public static Rectangle newGameButton;
	public static Object generationLock;
	public static Taxi taxi = new Taxi();

	// zoom variables
	public static double zoom = 1, visualZoom = 100;
	public static final double MIN_ZOOM = 0.25, MAX_ZOOM = 2;
	// clouds
	public static Cloud[] clouds;

	public TaxiGame() {
		generationLock = new Object();
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
		upgradeShops = new ArrayList<Vector>();
		locationsOfInterest = new ArrayList<ArrayList<Vector>>();
		locationsOfInterest.add(trackShops);
		locationsOfInterest.add(gasStations);
		locationsOfInterest.add(upgradeShops);
		income = 50;
		money = 0;
		trackInvestment = 0;
		customers = new ArrayList<Customer>();
		taxi.location = new Vector(5.5 * TILE_SIZE, 5.5 * TILE_SIZE);
		taxi.start();
		camera = taxi.location.clone();
		tracks = new Track[30][30];
		plannedTracks = new Track[30][30];
		clouds = new Cloud[30];
		predictStartLoop = false;
		predictTracks = new boolean[30][30];
		predictTracksArray = new Vector[30];
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
			tracks[p.x][p.y] = plannedTracks[p.x][p.y];
			if (plannedTracks[p.x][p.y].up && tracks[p.x][p.y - 1] == null) possibilities.add(new Point(p.x, p.y - 1));
			if (plannedTracks[p.x][p.y].right && tracks[p.x + 1][p.y] == null) possibilities.add(new Point(p.x + 1, p.y));
			if (plannedTracks[p.x][p.y].down && tracks[p.x][p.y + 1] == null) possibilities.add(new Point(p.x, p.y + 1));
			if (plannedTracks[p.x][p.y].left && tracks[p.x - 1][p.y] == null) possibilities.add(new Point(p.x - 1, p.y));

			if (i == 2) {
				trackShops.add(new Vector((p.x + 0.5) * TILE_SIZE - 15, (p.y + 0.5) * TILE_SIZE - 15));
			}

			if (i == 8) {
				gasStations.add(new Vector((p.x + 0.5) * TILE_SIZE + 15, (p.y + 0.5) * TILE_SIZE + 15));
			}
		}

		generateClouds();
		generateUpgradeShops();
	}

	public void tick() {
		if (paused || mainMenu) return;

		taxi.tick();

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
		visualCameraAngle += camAdd / 4;

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
			predictOpacity += 0.005;
			if (predictOpacity >= 0.75) predictOpacityUp = false;
		} else {
			predictOpacity -= 0.005;
			if (predictOpacity <= 0.25) predictOpacityUp = true;
		}

		// Receive money
		money += Math.signum(income -= Math.signum(income));

		// Put money in track shops
		for (Vector v : trackShops) {
			if (taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
				if (money > 0) {
					money--;
					trackInvestment++;
				}
			}
		}

		// Buy gas
		for (Vector v : gasStations) {
			if (taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
				if (money > 0 && taxi.gas < taxi.maxGas - 0.5) {
					money--;
					taxi.gas += 0.3;
				}
			}
		}

		// Buy upgrades
		Vector v;
		// top speed
		v = upgradeShops.get(0);
		if (money > 0 && taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
			money--;
			money_in_speed++;
			taxi.maxSpeed = -(Taxi.MAX_MAX_SPEED - Taxi.START_MAX_SPEED) / (.01 * money_in_speed + 1) + Taxi.MAX_MAX_SPEED;
		}
		// acceleration
		v = upgradeShops.get(1);
		if (money > 0 && taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
			money--;
			money_in_acceleration++;
			taxi.acceleration = -(Taxi.MAX_ACCELERATION - Taxi.START_ACCELERATION) / (.01 * money_in_acceleration + 1) + Taxi.MAX_ACCELERATION;
		}
		// max gas
		v = upgradeShops.get(2);
		if (money > 0 && taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
			money--;
			money_in_gas++;
			taxi.maxGas = -(Taxi.MAX_MAX_GAS - Taxi.START_MAX_GAS) / (.01 * money_in_gas + 1) + Taxi.MAX_MAX_GAS;
		}
		// friction
		v = upgradeShops.get(3);
		if (money > 0 && taxi.location.distance2(v) <= 25 * 25 && taxi.velocity.length() < 0.5) {
			money--;
			money_in_friction++;
			taxi.friction = -(Taxi.MAX_FRICTION - Taxi.START_FRICTION) / (.01 * money_in_friction + 1) + Taxi.MAX_FRICTION;
		}

		if (trackInvestment >= TRACK_PRICE) {
			trackInvestment -= TRACK_PRICE;
			trackStock++;
		}

		if (taxi.gas < 0) {
			taxi.gas = 0;
		}
		if (rating < 0) {
			rating = 0;
		} else if (rating > MAX_RATING) {
			rating = MAX_RATING;
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
		g.setColor(Color.black);
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
					g.setColor(Color.black);
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
						g.setStroke(new BasicStroke((int) (1 * visualZoom)));
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
					}
					if ((x == taxiTile.x && y == taxiTile.y && predictStartLoop) || (!(x == taxiTile.x && y == taxiTile.y) && tracks[x][y] != null)) {
						g.setColor(new Color(255, 0, 0, (int) (255 * predictOpacity)));
						g.setStroke(new BasicStroke((int) (1 * visualZoom)));
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
		for (Customer cust : customers) {
			Vector c = cust.position, d = cust.destination;

			if (cust.droppedOff) {
				g.setColor(new Color(255, 165, 0, (int) cust.visualFade));
				drawMapOval(g, c.x, c.y, 5, 5, true);
			} else if (cust.pickedUp) {
				g.setColor(new Color(200, 0, 200, 128));
				drawMapOval(g, d.x, d.y, TILE_SIZE / 1.5 * 2, TILE_SIZE / 1.5 * 2, true);
			} else {
				g.setColor(cust.goldMember ? new Color(255, 235, 95) : new Color(245, 170, 30));
				drawMapOval(g, c.x, c.y, 5, 5, true);
				drawMapOval(g, c.x, c.y, Customer.PICKUP_RADIUS * 2, Customer.PICKUP_RADIUS * 2, false);
			}
		}

		// Draw shops
		g.setColor(new Color(25, 0, 255));
		for (Vector v : trackShops) {
			drawMapOval(g, v.x, v.y, 10, 10, true);
			drawMapOval(g, v.x, v.y, 50, 50, false);

			if (taxi.location.distance2(v) < 150 * 150) {
				g.setColor(new Color(25, 0, 255, (int) (63 + 192 * (1 - taxi.location.distance(v) / 150))));
				g.drawString("$" + trackInvestment + "/$25", (int) (v.x - 20), (int) (v.y - 8));
			}
		}
		g.setColor(new Color(175, 150, 50));
		for (Vector v : gasStations) {
			drawMapOval(g, v.x, v.y, 10, 10, true);
			drawMapOval(g, v.x, v.y, 50, 50, false);
		}
		// upgrade shops
		Vector v;
		// top speed
		v = upgradeShops.get(0);
		g.setColor(Color.green);
		drawMapOval(g, v.x, v.y, 10, 10, true);
		drawMapOval(g, v.x, v.y, 50, 50, false);
		// acceleration
		v = upgradeShops.get(1);
		g.setColor(Color.red);
		drawMapOval(g, v.x, v.y, 10, 10, true);
		drawMapOval(g, v.x, v.y, 50, 50, false);
		// max gas
		v = upgradeShops.get(2);
		g.setColor(Color.pink);
		drawMapOval(g, v.x, v.y, 10, 10, true);
		drawMapOval(g, v.x, v.y, 50, 50, false);
		// friction
		v = upgradeShops.get(3);
		g.setColor(Color.magenta);
		drawMapOval(g, v.x, v.y, 10, 10, true);
		drawMapOval(g, v.x, v.y, 50, 50, false);

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

		// Draw upgradeable stats
		drawString(g, "Max Speed: " + taxi.maxSpeed + "\nAcceleration: " + taxi.acceleration + "\nMAX_GAS: " + taxi.maxGas + "\nFRICTION: " + taxi.friction, 5, 38);

		// Draw gas
		g.setColor(Color.gray);
		g.fillRoundRect(10, S_HEIGHT - 80, 100, 70, 10, 10);
		g.setColor(Color.black);
		g.drawString("E", 20, S_HEIGHT - 20);
		g.drawString("F", 94, S_HEIGHT - 20);
		g.setColor(Color.red);
		g.drawLine(60, S_HEIGHT - 20, (int) (40 * Math.cos((taxi.maxGas - taxi.gas) / taxi.maxGas * Math.PI)) + 60,
				(int) -(40 * Math.sin((taxi.maxGas - taxi.gas) / taxi.maxGas * Math.PI)) + S_HEIGHT - 20);

		// Draw compass
		int startLineX = S_WIDTH - 70;
		int startLineY = S_HEIGHT - 70;
		int endLineX = (int) (Math.round(S_WIDTH - 70 + 50 * Math.sin(visualCameraAngle)));
		int endLineY = (int) (Math.round(S_HEIGHT - 70 + 50 * -Math.cos(visualCameraAngle)));
		g.setColor(Color.black);
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

		// Draw Game Over
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

	private static void generateUpgradeShops() {
		int lowerXY = 4;
		int higherXY = 8;
		for (int i = 0; i < upgradeShopCount; i++) {
			boolean validVector = false;
			Vector v = null;
			while (!validVector) {
				double randX = ((double) ((int) (Math.random() * (higherXY - lowerXY)) + lowerXY) + 0.5) * TILE_SIZE + (Math.random() < 0.5 ? 15 : -15);
				double randY = ((double) ((int) (Math.random() * (higherXY - lowerXY)) + lowerXY) + 0.5) * TILE_SIZE + (Math.random() < 0.5 ? 15 : -15);
				v = new Vector(randX, randY);
				validVector = true;
				for (int j = 0; j < locationsOfInterest.size(); j++) {
					for (int k = 0; k < locationsOfInterest.get(j).size(); k++) {
						if (v.distance2(locationsOfInterest.get(j).get(k)) < 50 * 50) {
							validVector = false;
						}
					}
				}
				if (v.distance2(taxi.location) < 50 * 50) validVector = false;
			}
			upgradeShops.add(v);
		}
	}

	private int getNextDir() {
		Vector taxiModTile = new Vector(taxi.location.x % TILE_SIZE, taxi.location.y % TILE_SIZE);
		int hts = TILE_SIZE / 2;
		int dir = (int) (mod(Math.round(visualCameraAngle / (Math.PI / 2)) + 1, 4));
		if (dir == 0 && taxiModTile.x < hts - 20 || dir == 1 && taxiModTile.y > hts + 20 || dir == 2 && taxiModTile.x > hts + 20 || dir == 3 && taxiModTile.y < hts - 20) {
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
		graphics.drawLine((int) (finalZoom * lineX1), (int) (finalZoom * lineY1), (int) (finalZoom * lineX2), (int) (finalZoom * lineY2));
	}

	private static void drawMapArc(Graphics2D graphics, double arcX, double arcY, double arcW, double arcH, double startAng, double endAng) {
		drawMapArc(graphics, arcX, arcY, arcW, arcH, startAng, endAng, 1);
	}

	private static void drawMapArc(Graphics2D graphics, double arcX, double arcY, double arcW, double arcH, double startAng, double endAng, double zoomLevel) {
		if (visualZoom > zoomLevel && !(visualZoom > 1 && zoomLevel == 1)) return;
		double finalZoom = zoomLevel;
		graphics.drawArc((int) (finalZoom * arcX), (int) (finalZoom * arcY), (int) (finalZoom * arcW), (int) (finalZoom * arcH), (int) (startAng), (int) (endAng));
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

}