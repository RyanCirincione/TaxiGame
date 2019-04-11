import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class Customer {
	public Vector position, destination, originalPosition;
	public boolean pickedUp, droppedOff, goldMember, justSpawned;
	public int anger, seatPosition;
	public double visualFade, radiusShrink, fillRadius, fillOpacity;
	public static double PICKUP_RADIUS = TaxiGame.TILE_SIZE * 5 / 8;

	public Customer(Vector pos, Vector dest, boolean p, boolean d, boolean gold) {
		position = pos;
		originalPosition = position.clone();
		destination = dest;
		pickedUp = p;
		droppedOff = d;
		goldMember = gold;
		justSpawned = true;
		visualFade = 255;
		radiusShrink = 0;
		fillRadius = 0;
		fillOpacity = 0;
		anger = 0;
		seatPosition = 0;
	}

	public void update() {
		if (justSpawned) {
			radiusShrink += 0.05;
			if (radiusShrink >= 1) {
				radiusShrink = 1;
				justSpawned = false;
			}
		} else {
			int carrying = 0;
			for(int i = 0; i < TaxiGame.customers.size(); i++) {
				if(TaxiGame.customers.get(i).pickedUp) {
					carrying++;
				}
			}
			
			double d = TaxiGame.taxi.location.distance2(position);		
			
			// Pick up logic
			if (TaxiGame.taxi.velocity.length() < 0.5 && !pickedUp && !droppedOff && carrying < TaxiGame.taxi.maxCustomers) {
				d = TaxiGame.taxi.location.distance2(position);
				if (d < Math.pow(PICKUP_RADIUS, 2)) {
					if (d < 5 * 5) {
						anger = 0;
						pickedUp = true;
						radiusShrink = 0;
						fillOpacity = 0.5;
						justSpawned = true;
						for (int i=0; i < TaxiGame.taxi.maxCustomers; i++) {
							if (TaxiGame.myCustomers[i] == null) {
								seatPosition = i;
								TaxiGame.myCustomers[i] = this;
								TaxiGame.myCustomersGold[i] = goldMember;
								break;
							}
						}
	
						// Occasionally, create a destination slightly outside the city to force the
						// player to expand
						if (Math.random() < 0.3) {
							Vector newDestination = new Vector(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length,
									Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
							int expansionRange = 2 + (int) (Math.random() * 2);
	
							while (!isWithinRange((int) (newDestination.x / TaxiGame.TILE_SIZE), (int) (newDestination.y / TaxiGame.TILE_SIZE), expansionRange)
									|| TaxiGame.tracks[(int) (newDestination.x / TaxiGame.TILE_SIZE)][(int) (newDestination.y / TaxiGame.TILE_SIZE)] != null
											&& !isPointNearTrack(newDestination)) {
								newDestination.set(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
							}
	
							destination = newDestination;
						}
					} else {
						position.set(position.lerp(TaxiGame.taxi.location, 1));
						radiusShrink = position.distance2(TaxiGame.taxi.location)/originalPosition.distance2(TaxiGame.taxi.location);
					}
				}
			}
			
			// if taxi doesnt fully pick up, reset radiusShrink and originalPosition
			if (!droppedOff && (TaxiGame.taxi.velocity.length() >= 0.5 || TaxiGame.carrying >= TaxiGame.taxi.maxCustomers)) {
				if (radiusShrink < 1) {
					radiusShrink += 0.1;
					if (radiusShrink > 1) radiusShrink = 1;
				}
				originalPosition = position.clone();
			}
			
			// manage fill
			if (!pickedUp) {
				if (d < Math.pow(PICKUP_RADIUS, 2)) {
					if (fillRadius < 1) {
						fillRadius += 0.05;
						if (fillRadius > 1) fillRadius = 1;
						double targetOpacity = fillRadius / 2;
						if (fillOpacity < targetOpacity) {
							fillOpacity += 0.05;
							if (fillOpacity > targetOpacity) fillOpacity = targetOpacity;
						}
					}
				}
				else {
					if (fillRadius < 1) {
						fillRadius += 0.0075;
						fillOpacity = fillRadius / 4;
					}
					else {
						fillRadius = 1;
						fillOpacity -= 0.0025;
						if (fillOpacity <= 0) {
							fillRadius = 0;
							fillOpacity = 0;
						}
					}
				}
			}
	
			// Drop off logic
			if (pickedUp && !droppedOff) {
				d = TaxiGame.taxi.location.distance2(destination);
				if (d < Math.pow(PICKUP_RADIUS, 2)) {
					if (fillOpacity < 1) {
						fillOpacity += 0.05;
					}
					if (TaxiGame.taxi.velocity.length() < 0.5) {
						double earnings = (int) (Math.random() * (5 + 10 * TaxiGame.rating / TaxiGame.MAX_RATING)) + 5 + 20 * TaxiGame.rating / TaxiGame.MAX_RATING;
						
						TaxiGame.myCustomers[seatPosition] = null;
						pickedUp = false;
						droppedOff = true;
						position.set(TaxiGame.taxi.location);
						TaxiGame.income += earnings;
		
						if (anger < 300) {
							TaxiGame.rating += 0.2;
						} else if (anger < 900) {
							TaxiGame.rating += 0.07;
						} else if (anger < 1800) {
							TaxiGame.rating += 0.03;
						} else if (anger < 3600) {
							TaxiGame.rating += 0.011;
						}
		
						for (int i = 0; i < (int) earnings; i++) {
							Vector v = new Vector(Math.random() * 2 - 1, Math.random() * 2 - 1);
							v.setLength(4 + 5 * Math.random());
		
							TaxiGame.particles.add(new Particle(new Vector(TaxiGame.S_WIDTH/2, TaxiGame.S_HEIGHT/2), true) {
								Vector vel;
		
								{
									vel = v;
								}
		
								public void update() {
									pos = pos.plus(vel);
									if (age > 15) {
										vel = vel.setLength(vel.length()*0.92).plus(new Vector(30-pos.x, 30-pos.y).setLength(age/20.0));
									}
									
									if(pos.x < 30 && pos.y < 30) {
										remove = true;
										TaxiGame.income++;
										if(Math.random() < 0.5) {
											TaxiGame.sound.playSound("money");
										}
									}
								}
		
								public void paint(Graphics2D g) {
									g.setColor(new Color(0, 230, 0, 255 - age / 15));
									g.setFont(new Font("Times New Roman", Font.BOLD, 10));
									g.drawString("$", (int) (pos.x), (int) (pos.y));
								}
							});
						}
					}
				} else {
					if (fillOpacity > 0.5) {
						fillOpacity -= 0.05;
					}
				}
			}
	
			anger++;
			if (!pickedUp && anger > 3600) {
				TaxiGame.rating -= goldMember ? 0.75 : 0.01;
				visualFade = -1; // Hacky way to get this customer removed
			}
			if (anger > 7200 && pickedUp) {
				TaxiGame.rating -= 0.5 / 3600 * (goldMember ? 3 : 1);
			} else if (anger > 3600 && pickedUp) {
				TaxiGame.rating -= 0.35 / 3600 * (goldMember ? 3 : 1);
			}
	
			if (droppedOff) {
				position = position.lerp(destination, 0.3);
				visualFade -= 2.5;
				radiusShrink *= 0.9;
				fillOpacity = 1;
			}
		}
	}

	private static boolean isWithinRange(int x, int y, int range) {
		if (range <= 0) {
			return false;
		}

		if (TaxiGame.plannedTracks[x][y].right && TaxiGame.tracks[x + 1][y] != null) {
			return true;
		}
		if (TaxiGame.plannedTracks[x][y].up && TaxiGame.tracks[x][y - 1] != null) {
			return true;
		}
		if (TaxiGame.plannedTracks[x][y].left && TaxiGame.tracks[x - 1][y] != null) {
			return true;
		}
		if (TaxiGame.plannedTracks[x][y].down && TaxiGame.tracks[x][y + 1] != null) {
			return true;
		}

		if (TaxiGame.plannedTracks[x][y].right) {
			return isWithinRange(x + 1, y, range - 1);
		}
		if (TaxiGame.plannedTracks[x][y].up) {
			return isWithinRange(x, y - 1, range - 1);
		}
		if (TaxiGame.plannedTracks[x][y].left) {
			return isWithinRange(x - 1, y, range - 1);
		}
		if (TaxiGame.plannedTracks[x][y].down) {
			return isWithinRange(x, y + 1, range - 1);
		}

		return false;
	}

	public static Customer generateCustomer() {
		Vector pos = new Vector(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length),
				dest = new Vector(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);

		while (TaxiGame.tracks[(int) pos.x / TaxiGame.TILE_SIZE][(int) pos.y / TaxiGame.TILE_SIZE] == null || !isPointNearTrack(pos)) {
			pos.set(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
		}
		while (TaxiGame.tracks[(int) dest.x / TaxiGame.TILE_SIZE][(int) dest.y / TaxiGame.TILE_SIZE] == null || !isPointNearTrack(dest)) {
			dest.set(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
		}

		return new Customer(pos, dest, false, false, Math.random() < 0.09);
	}

	public static boolean isPointNearTrack(Vector v) {
		int tx = (int) (v.x / TaxiGame.TILE_SIZE), ty = (int) (v.y / TaxiGame.TILE_SIZE);

		return v.distance(new Vector((tx + 0.5) * TaxiGame.TILE_SIZE, (ty + 0.5) * TaxiGame.TILE_SIZE)) < PICKUP_RADIUS - 8;
	}
}
