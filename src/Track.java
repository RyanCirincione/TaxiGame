
public class Track {
	public boolean right, up, left, down;
	
	public Track() {
		this(false, false, false, false);
	}
	
	public Track(boolean r, boolean u, boolean l, boolean d) {
		right = r;
		up = u;
		left = l;
		down = d;
	}
}
