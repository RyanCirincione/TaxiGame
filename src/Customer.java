
public class Customer {
	public Vector position, destination;
	public boolean pickedUp, droppedOff, goldMember;
	public int anger;
	public double visualFade;
	public static double PICKUP_RADIUS = TaxiGame.TILE_SIZE * 5 / 8;

	public Customer(Vector pos, Vector dest, boolean p, boolean d, boolean gold) {
		position = pos;
		destination = dest;
		pickedUp = p;
		droppedOff = d;
		goldMember = gold;
		visualFade = 255;
		anger = 0;
	}

	public void update() {
		// Pick up logic
		if (TaxiGame.taxiVelocity.length() < 0.5 && !pickedUp && !droppedOff) {
			double d = TaxiGame.taxiLocation.distance2(position);
			if (d < Math.pow(PICKUP_RADIUS, 2)) {
				if (d < 5 * 5) {
					anger = 0;
					pickedUp = true;

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
					position.set(position.lerp(TaxiGame.taxiLocation, 1));
				}
			}
		}

		// Drop off logic
		if (TaxiGame.taxiVelocity.length() < 0.5 && pickedUp && !droppedOff) {
			double d = TaxiGame.taxiLocation.distance2(destination);
			if (d < Math.pow(PICKUP_RADIUS, 2)) {
				pickedUp = false;
				droppedOff = true;
				position.set(TaxiGame.taxiLocation);
				TaxiGame.income += (int) (Math.random() * (5 + 10 * TaxiGame.rating / TaxiGame.MAX_RATING)) + 5 + 20 * TaxiGame.rating / TaxiGame.MAX_RATING;

				if (anger < 300) {
					TaxiGame.rating += 0.2;
				} else if (anger < 900) {
					TaxiGame.rating += 0.07;
				} else if (anger < 1800) {
					TaxiGame.rating += 0.03;
				} else if (anger < 3600) {
					TaxiGame.rating += 0.011;
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

		return v.distance(new Vector((tx + 0.5) * TaxiGame.TILE_SIZE, (ty + 0.5) * TaxiGame.TILE_SIZE)) < PICKUP_RADIUS - 5;
	}
}
