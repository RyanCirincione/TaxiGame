
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
	 * @param x The x coordinate of the vector
	 * @param y The y coordinate of the vector
	 */
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}
}
