import java.awt.Color;
import java.awt.Font;
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

	public static class BrakeSpark extends Particle {
		Vector vel;

		public BrakeSpark(Vector p, Vector v) {
			super(p);
			vel = v;
		}

		public void update() {
			pos = pos.plus(vel);

			if (age > 17) {
				remove = true;
			}
		}

		public void paint(Graphics2D g) {
			g.setColor(Color.yellow);
			g.fillOval((int) (pos.x), (int) (pos.y), 2, 2);
		}
	}

	public static class GasBlob extends Particle {
		Vector vel;

		public GasBlob(Vector p, Vector v) {
			super(p, true);
			vel = v;
		}

		public void update() {
			pos = pos.plus(vel);

			if (age == 10) {
				vel.set(-pos.x, TaxiGame.S_HEIGHT - pos.y).setLength(30);
			}

			if (pos.x < 30 || pos.y > TaxiGame.S_HEIGHT - 30) {
				remove = true;
			}
		}

		public void paint(Graphics2D g) {
			g.setColor(new Color(175, 150, 50));
			g.fillOval((int) pos.x, (int) pos.y, 6, 6);
		}
	}

	public static class Dollar extends Particle {
		Vector vel;

		public Dollar(Vector p, Vector v) {
			super(p, true);
			vel = v;
		}

		public void update() {
			pos = pos.plus(vel);
			if (age > 15) {
				vel = vel.setLength(vel.length() * 0.92).plus(new Vector(30 - pos.x, 30 - pos.y).setLength(age / 20.0));
			}

			if (pos.x < 30 && pos.y < 30) {
				remove = true;
				TaxiGame.income++;
				if (Math.random() < 0.5) {
					TaxiGame.sound.playSound("money");
				}
			}
		}

		public void paint(Graphics2D g) {
			g.setColor(new Color(0, 230, 0, 255 - age / 15));
			g.setFont(new Font("Times New Roman", Font.BOLD, 10));
			g.drawString("$", (int) (pos.x), (int) (pos.y));
		}
	}

	public static class Upgrade extends Particle {
		Vector vel;
		Color color;

		public Upgrade(Vector p, Vector v, Color c) {
			super(p);
			vel = v;
			color = c;
		}

		public void update() {
			pos = pos.plus(vel);
			vel = vel.setLength(vel.length() * 0.97);

			if (age > 60) {
				remove = true;
			}
		}

		public void paint(Graphics2D g) {
			int alpha = (int) ((60.0 - age) / 60.0 * 255);
			g.setColor(new Color((color.getRGB() & 0x00FFFFFF) | (alpha << 24), true));
			g.fillOval((int) pos.x, (int) pos.y, 3, 3);
		}
	}
}
