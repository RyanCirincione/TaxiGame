
public class Taxi extends TrackObject {
	public double gas;
	public static final double START_MAX_SPEED = 2.0, START_ACCELERATION = 0.03, START_MAX_GAS = 20.0, START_FRICTION = .9985;
	public static final int START_MAX_CAPACITY = 2;
	public double maxSpeed = START_MAX_SPEED, acceleration = START_ACCELERATION, maxGas = START_MAX_GAS;
	public int maxCustomers = START_MAX_CAPACITY;
	public static final double MAX_MAX_SPEED = 10, MAX_ACCELERATION = .2, MAX_MAX_GAS = 40.0, MAX_FRICTION = 1;

	public void start() {
		maxSpeed = START_MAX_SPEED;
		acceleration = START_ACCELERATION;
		maxGas = START_MAX_GAS;
		friction = START_FRICTION;
		gas = maxGas;
	}

	@Override
	public void tick() {
		super.tick();
		if (TaxiGame.tracks[(int) (destination.x / TaxiGame.TILE_SIZE)][(int) (destination.y / TaxiGame.TILE_SIZE)] == null) {
			noTrack();
		}
		
		double l = velocity.length();
		if (TaxiGame.input.up && gas > 0) {
			if (l < maxSpeed - acceleration) {
				gas -= 0.02;
				velocity.setLength(l + acceleration);
			} else {
				velocity.setLength(maxSpeed);
			}

			// If velocity is zero, we need a direction to increase length toward
			if (velocity.length() < 0.01) {
				velocity.set(Math.cos(TaxiGame.cameraAngle + Math.PI / 2), Math.sin(TaxiGame.cameraAngle - Math.PI / 2));
				velocity.setLength(acceleration);
			}
		}
		if (TaxiGame.input.down) {
			if (l > acceleration) {
				velocity.setLength(l - acceleration);
			} else {
				velocity.setLength(0);
			}
		}
	}
	
	@Override
	public void noTrack() {
		if (TaxiGame.trackStock > 0) {
			TaxiGame.addTrack((int) (destination.x / TaxiGame.TILE_SIZE), (int) (destination.y / TaxiGame.TILE_SIZE));
			TaxiGame.trackStock--;
		} else {
			velocity.setLength(0);
			destination = location;
		}
	}
}
