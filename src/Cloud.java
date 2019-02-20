
public class Cloud {
	public static double MAX_Y = 50*TaxiGame.TILE_SIZE-10*TaxiGame.TILE_SIZE, MIN_Y = -MAX_Y;
	public double x, y, speed, size, myZoom;
	public boolean actuallyBird = false;
	
	public Cloud() {
		if (Math.random() < 0.5) {
			actuallyBird = true;
		}
		if (!actuallyBird) {
			x = Math.random()*50*TaxiGame.TILE_SIZE-10*TaxiGame.TILE_SIZE;
			y = Math.random()*50*TaxiGame.TILE_SIZE-10*TaxiGame.TILE_SIZE;
			speed = Math.random();
			size = Math.random()*10*TaxiGame.TILE_SIZE+10*TaxiGame.TILE_SIZE;
			myZoom = Math.random()*0.25+0.5;
		}
		else {
			x = Math.random()*50*TaxiGame.TILE_SIZE-10*TaxiGame.TILE_SIZE;
			y = Math.random()*50*TaxiGame.TILE_SIZE-10*TaxiGame.TILE_SIZE;
			speed = Math.random()*4+3;
			size = Math.random()*TaxiGame.TILE_SIZE/2;
			myZoom = Math.random()*0.25+0.75;
		}
		speed = 0;
	}
	
	public void Update() {
		y -= speed;
		if (y < MIN_Y) {
			y = MAX_Y;
		}
	}
}
