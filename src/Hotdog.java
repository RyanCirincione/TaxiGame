
public class Hotdog extends TrackObject {
	int radius = 10;
	boolean collision = false;
	
	@Override
	public void tick() {
		super.tick();
		if (location.distance2(TaxiGame.taxi.location) <= radius*radius && !collision) {
			collision = true;
			velocity = TaxiGame.taxi.velocity;
			TaxiGame.taxi.velocity = TaxiGame.taxi.velocity.scale(0.5);
		}
		if (!(location.distance2(TaxiGame.taxi.location) <= radius*radius)) {
			collision = false;
		}
	}
	
	@Override
	public void noTrack() {
		velocity = velocity.scale(-1);
	}
}
