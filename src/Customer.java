
public class Customer {
	public Vector position, destination;
	public boolean pickedUp, droppedOff;
	public double visualFade;

	public Customer(Vector pos, Vector dest, boolean p, boolean d) {
		position = pos;
		destination = dest;
		pickedUp = p;
		droppedOff = d;
		visualFade = 255;
	}

	public void update() {
		if (TaxiGame.taxiVelocity.length() < 0.5 && !pickedUp && !droppedOff) {
			double d = TaxiGame.taxiLocation.distance2(position);
			if (d < Math.pow(TaxiGame.TILE_SIZE * 3 / 4, 2)) {
				if (d < 5 * 5) {
					pickedUp = true;
				} else {
					position.set(position.lerp(TaxiGame.taxiLocation, 1));
				}
			}
		}

		if (TaxiGame.taxiVelocity.length() < 0.5 && pickedUp && !droppedOff) {
			double d = TaxiGame.taxiLocation.distance2(destination);
			if (d < Math.pow(TaxiGame.TILE_SIZE / 1.5, 2)) {
				pickedUp = false;
				droppedOff = true;
				position.set(TaxiGame.taxiLocation);
				TaxiGame.income += (int) (Math.random() * 6) + 15;
			}
		}

		if (droppedOff) {
			position = position.lerp(destination, 0.3);
			visualFade -= 2.5;
		}
	}

	public static Customer generateCustomer() {
		Vector pos = new Vector(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length),
				dest = new Vector(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);

		while (TaxiGame.tracks[(int) pos.x / TaxiGame.TILE_SIZE][(int) pos.y / TaxiGame.TILE_SIZE] == null) {
			pos.set(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
		}
		while (TaxiGame.tracks[(int) dest.x / TaxiGame.TILE_SIZE][(int) dest.y / TaxiGame.TILE_SIZE] == null) {
			dest.set(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
		}

		return new Customer(pos, dest, false, false);
	}
}
