package com.sparshui.common;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.Serializable;

import javax.vecmath.Vector3f;

/**
 * Represents a 2D location with float values.
 * 
 * @author Jay Roltgen
 */
public class Location implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3472243250219991476L;
	private float _x;
	private float _y;

	/**
	 * Cosntruct a default location.  Values are initialized
	 * as the coordinates (0, 0).
	 */
	public Location() {
		_x = 0;
		_y = 0;
	}
	
	/**
	 * Construct a specific location.
	 * @param x
	 * 		The x coordinate value of the location.
	 * @param y
	 * 		The y coordinate value of the location.
	 */
	public Location(float x, float y) {
		_x = x;
		_y = y;
	}
	
	/**
	 * 
	 * @return
	 * 		The x coordinate value.
	 */
	public float getX() {
		return _x;
	}
	
	/**
	 * 
	 * @return
	 * 		The y coordinate value.
	 */
	public float getY() {
		return _y;
	}
	
	public String toString() {
		return "x = " + _x + ", y = " + _y 
		        + (_x < 1 && _x > 0 ? "(" 
		        + pixelLocation(this).getX() + " " + pixelLocation(this).getY() + ")"
		    : "");
	}
	

  public float distance(Location location) {
    return (float) Math.sqrt(distance2(location));
  }

  public float distance2(Location location) {
    float dx, dy;
    return (dx = _x - location._x) * dx + (dy = _y - location._y) * dy;
  }

  public Vector3f directionTo(Location location) {
    return new Vector3f(location._x - _x, location._y - _y, 0);  
  }
  
  static final Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();

  public static Location pixelLocation(Location location) {
    return new Location(location.getX() * screenDim.width, location.getY()
        * screenDim.height);
  }

  public static Location screenLocation(Location location) {
    return new Location(location.getX() / screenDim.width, location.getY()
        / screenDim.height);
  }

}
