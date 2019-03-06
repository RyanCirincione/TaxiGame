import java.awt.Graphics2D;

public abstract class Particle {
	public Vector pos;
	public int age;
	public boolean UI, remove;

	public Particle() {
		this(new Vector(), false);
	}

	public Particle(Vector p) {
		this(p, false);
	}

	public Particle(Vector p, boolean ui) {
		pos = p;
		age = 0;
		UI = ui;
		remove = false;
	}
	
	public abstract void update();
	public abstract void paint(Graphics2D g);
}
