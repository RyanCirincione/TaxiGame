public class TrackObject {
	Vector location = new Vector(0, 0), velocity = new Vector(0, 0), destination = new Vector(0, 0);
	// If the taxi ever flies off the rail, make these decimals even smaller as long
	// as the game still functions
	double maxSpeed = 2.0, friction = .997;

	public void tick() {
		final boolean HORIZONTALLY_ALIGNED = Math.abs(location.y % TaxiGame.TILE_SIZE - TaxiGame.TILE_SIZE / 2) < 0.0000000001;
		final boolean VERTICALLY_ALIGNED = Math.abs(location.x % TaxiGame.TILE_SIZE - TaxiGame.TILE_SIZE / 2) < 0.0000000001;
		boolean ON_CURVE = !(HORIZONTALLY_ALIGNED || VERTICALLY_ALIGNED);
		int tx = (int) (location.x / TaxiGame.TILE_SIZE);
		int ty = (int) (location.y / TaxiGame.TILE_SIZE);
		Vector taxiModTile = new Vector(location.x % TaxiGame.TILE_SIZE, location.y % TaxiGame.TILE_SIZE);

		// friction
		velocity.setLength(velocity.length() * friction);

		// Rotate velocity to be a chord on the circular curve
		if (ON_CURVE) {
			double theta = Math.asin(velocity.length() / TaxiGame.CURVE_RADIUS);
			if (taxiModTile.x > TaxiGame.TILE_SIZE / 2 && velocity.y > 0 || taxiModTile.x < TaxiGame.TILE_SIZE / 2 && velocity.y < 0) {
				// When going counter clockwise (around center of tile), theta needs to be
				// inverted
				theta *= -1;
			}

			double x = velocity.x, y = velocity.y;
			velocity.set(x * Math.cos(theta) - y * Math.sin(theta), x * Math.sin(theta) + y * Math.cos(theta));
		} else if (HORIZONTALLY_ALIGNED) {
			if (taxiModTile.x < TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS && taxiModTile.x + velocity.x > TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS) {
				if (TaxiGame.input.left && TaxiGame.tracks[tx][ty].up || !TaxiGame.tracks[tx][ty].down && !TaxiGame.tracks[tx][ty].right) {
					velocity.y = Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + velocity.x - (TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS), 2))
							- TaxiGame.CURVE_RADIUS;
				} else if (TaxiGame.input.right && TaxiGame.tracks[tx][ty].down || !TaxiGame.tracks[tx][ty].right) {
					velocity.y = -Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + velocity.x - (TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS), 2))
							+ TaxiGame.CURVE_RADIUS;
				}
			} else if (taxiModTile.x > TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS && taxiModTile.x + velocity.x < TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS) {
				if (TaxiGame.input.left && TaxiGame.tracks[tx][ty].down || !TaxiGame.tracks[tx][ty].up && !TaxiGame.tracks[tx][ty].left) {
					velocity.y = -Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + velocity.x - (TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS), 2))
							+ TaxiGame.CURVE_RADIUS;
				} else if (TaxiGame.input.right && TaxiGame.tracks[tx][ty].up || !TaxiGame.tracks[tx][ty].left) {
					velocity.y = Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.x + velocity.x - (TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS), 2))
							- TaxiGame.CURVE_RADIUS;
				}
			} else if (!VERTICALLY_ALIGNED) {
				velocity.y = 0;
			}
		} else if (VERTICALLY_ALIGNED) {
			if (taxiModTile.y < TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS && taxiModTile.y + velocity.y > TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS) {
				if (TaxiGame.input.left && TaxiGame.tracks[tx][ty].right || !TaxiGame.tracks[tx][ty].left && !TaxiGame.tracks[tx][ty].down) {
					velocity.x = -Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + velocity.y - (TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS), 2))
							+ TaxiGame.CURVE_RADIUS;
				} else if (TaxiGame.input.right && TaxiGame.tracks[tx][ty].left || !TaxiGame.tracks[tx][ty].down) {
					velocity.x = Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + velocity.y - (TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS), 2))
							- TaxiGame.CURVE_RADIUS;
				}
			} else if (taxiModTile.y > TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS && taxiModTile.y + velocity.y < TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS) {
				if (TaxiGame.input.left && TaxiGame.tracks[tx][ty].left || !TaxiGame.tracks[tx][ty].right && !TaxiGame.tracks[tx][ty].up) {
					velocity.x = Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + velocity.y - (TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS), 2))
							- TaxiGame.CURVE_RADIUS;
				} else if (TaxiGame.input.right && TaxiGame.tracks[tx][ty].right || !TaxiGame.tracks[tx][ty].up) {
					velocity.x = -Math.sqrt(Math.pow(TaxiGame.CURVE_RADIUS, 2) - Math.pow(taxiModTile.y + velocity.y - (TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS), 2))
							+ TaxiGame.CURVE_RADIUS;
				}
			} else {
				velocity.x = 0;
			}
		}

		// If taxi is leaving curve, it must snap back to horizontal/vertical alignment
		if (taxiModTile.x > TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS && taxiModTile.x < TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS
				&& taxiModTile.y > TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS && taxiModTile.y < TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS) {
			if (taxiModTile.x + velocity.x < TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS) {
				velocity.y = TaxiGame.TILE_SIZE / 2 - taxiModTile.y;
			} else if (taxiModTile.x + velocity.x > TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS) {
				velocity.y = TaxiGame.TILE_SIZE / 2 - taxiModTile.y;
			} else if (taxiModTile.y + velocity.y < TaxiGame.TILE_SIZE / 2 - TaxiGame.CURVE_RADIUS) {
				velocity.x = TaxiGame.TILE_SIZE / 2 - taxiModTile.x;
			} else if (taxiModTile.y + velocity.y > TaxiGame.TILE_SIZE / 2 + TaxiGame.CURVE_RADIUS) {
				velocity.x = TaxiGame.TILE_SIZE / 2 - taxiModTile.x;
			}
		}

		destination = location.plus(velocity);
		if (TaxiGame.tracks[(int) (destination.x / TaxiGame.TILE_SIZE)][(int) (destination.y / TaxiGame.TILE_SIZE)] == null) {
			noTrack();
		} else {
			location = destination;
		}
	}
	
	public void noTrack() {
		velocity.setLength(0);
	}
}