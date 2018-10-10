
public class Customer {
	public Vector position, destination;
	public boolean pickedUp, droppedOff, goldMember;
	public int anger;
	public double visualFade;

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
			if (d < Math.pow(TaxiGame.TILE_SIZE * 3 / 4, 2)) {
				if (d < 5 * 5) {
					anger = 0;
					pickedUp = true;
				} else {
					position.set(position.lerp(TaxiGame.taxiLocation, 1));
				}
			}
		}

		// Drop off logic
		if (TaxiGame.taxiVelocity.length() < 0.5 && pickedUp && !droppedOff) {
			double d = TaxiGame.taxiLocation.distance2(destination);
			if (d < Math.pow(TaxiGame.TILE_SIZE / 1.5, 2)) {
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
				} else if(anger < 3600) {
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

	public static Customer generateCustomer() {
		Vector pos = new Vector(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length),
				dest = new Vector(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);

		while (TaxiGame.tracks[(int) pos.x / TaxiGame.TILE_SIZE][(int) pos.y / TaxiGame.TILE_SIZE] == null) {
			pos.set(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
		}
		while (TaxiGame.tracks[(int) dest.x / TaxiGame.TILE_SIZE][(int) dest.y / TaxiGame.TILE_SIZE] == null) {
			dest.set(Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks.length, Math.random() * TaxiGame.TILE_SIZE * TaxiGame.tracks[0].length);
		}

		return new Customer(pos, dest, false, false, Math.random() < 0.09);
	}
}
