
/**
 * This is a utility class for representing a pair of doubles. Functionality
 * will be added to it as needed.
 * 
 * @author Ryan
 *
 */
public class Vector {
	public double x, y;

	/**
	 * Creates a new [0, 0] vector
	 */
	public Vector() {
		this(0, 0);
	}

	/**
	 * Creates a new [x, y] vector
	 * 
	 * @param x
	 *            The x coordinate of the vector
	 * @param y
	 *            The y coordinate of the vector
	 */
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Sets this vector's x and y
	 * 
	 * @param x
	 *            New x
	 * @param y
	 *            New y
	 * @return This vector (for chaining)
	 */
	public Vector set(double x, double y) {
		this.x = x;
		this.y = y;

		return this;
	}

	/**
	 * Sets this vector's x and y based on the given vector
	 * 
	 * @param v
	 *            Vector with new x and y
	 * @return This vector (for chaining)
	 */
	public Vector set(Vector v) {
		this.set(v.x, v.y);

		return this;
	}

	/**
	 * Returns a new vector that is this vector lerped toward v by distance
	 * 
	 * @param v
	 *            The vector to be lerped to
	 * @param distance
	 *            The distance to be lerped by
	 * @return
	 */
	public Vector lerp(Vector v, double distance) {
		return this.plus(v.minus(this).setLength(distance));
	}

	/**
	 * Creates a new vector that is the sum of this and v
	 * 
	 * @param v
	 *            The vector to be added to this
	 * @return The new vector sum
	 */
	public Vector plus(Vector v) {
		return new Vector(x + v.x, y + v.y);
	}

	/**
	 * Creates a new vector that is the difference of this and v
	 * 
	 * @param v
	 *            The vector to be subtracted from this
	 * @return The new vector difference
	 */
	public Vector minus(Vector v) {
		return new Vector(x - v.x, y - v.y);
	}

	/**
	 * Effectively calculates this vector's distance from 0
	 * 
	 * @return This vector's length
	 */
	public double length() {
		return this.distance(new Vector());
	}

	/**
	 * Changes this vector's components to make its length equal to l
	 * 
	 * @param l
	 *            The target length
	 * @return This vector (for chaining)
	 */
	public Vector setLength(double l) {
		double d = this.distance(new Vector());
		if (d == 0) {
			return this;
		}

		x *= l / d;
		y *= l / d;

		return this;
	}

	/**
	 * Returns the distance between this vector and v
	 * 
	 * @param v
	 *            The vector to be compared for distance
	 * @return The distance between this vector and v
	 */
	public double distance(Vector v) {
		return Math.sqrt(this.distance2(v));
	}

	/**
	 * Returns the square of the distance between this vector and v. Square
	 * roots are computationally slow, so when possible, using distance2
	 * improves framerate
	 * 
	 * @param v
	 *            The vector for distance calculation
	 * @return The squared distance
	 */
	public double distance2(Vector v) {
		return Math.pow(x - v.x, 2) + Math.pow(y - v.y, 2);
	}

	/**
	 * Returns a new vector that is a scaled version of this vector
	 * 
	 * @param s
	 *            The scalar value
	 * @return The new scaled vector
	 */
	public Vector scale(double s) {
		return new Vector(x * s, y * s);
	}

	/**
	 * Creates a copy of this vector
	 * 
	 * @return A new vector with the same x and y as this one
	 */
	public Vector clone() {
		return new Vector(x, y);
	}

	/**
	 * Converts this vector to a string of the form <x, y>
	 * 
	 * @return A string of the format "<" + x + ", " + y + ">"
	 */
	public String toString() {
		return "<" + x + ", " + y + ">";
	}
}
