
public class Hotdog extends TrackObject {
	int radius = 10;
	boolean collision = false;
	Vector prevVelocity = new Vector(); // this is for velocity stuff

	@Override
	public void tick() {
		super.tick();
		if (velocity.length() > 0) prevVelocity = velocity;
		if (location.distance2(TaxiGame.taxi.location) <= radius * radius && !collision) {
			collision = true;
			if (prevVelocity.length() != 0) {
				Vector curDest = new Vector(location.x + prevVelocity.x, location.y + prevVelocity.y);
				Vector oppDest = new Vector(location.x - prevVelocity.x, location.y - prevVelocity.y);
				if (curDest.distance2(TaxiGame.taxi.location) > oppDest.distance2(TaxiGame.taxi.location)) {
					velocity = prevVelocity;
				} else {
					velocity = prevVelocity.scale(-1);
				}
			} else {
				velocity = TaxiGame.taxi.velocity;
			}
			velocity.setLength(TaxiGame.taxi.velocity.length());
			TaxiGame.taxi.velocity = TaxiGame.taxi.velocity.scale(0.5);
		}
		if (!(location.distance2(TaxiGame.taxi.location) <= radius * radius)) {
			collision = false;
		}
	}

	@Override
	public void noTrack() {
		velocity = velocity.scale(-1);
	}
}
