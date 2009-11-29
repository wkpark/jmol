package com.sparshui.gestures;

import java.util.HashMap;
import java.util.Vector;

import com.sparshui.common.Location;
import com.sparshui.common.TouchState;
import com.sparshui.server.TouchPoint;

/**
 * This is a class to ease implementation of standard dynamic
 * gestures.  Standard dynamic gestures can inherit from this class
 * and this class takes care of managing the list of touchpoints
 * that belong to this gesture.  Subclasses need only implement
 * the processBirth, processMove, and processDeath methods to
 * perform gesture processing.
 * 
 * @author Jay Roltgen
 */
public abstract class StandardDynamicGesture implements Gesture {
	
	protected HashMap _knownPoints;
	protected HashMap _traceData;
	protected Location _oldCentroid;
	protected Location _newCentroid;
	protected float _sumX, _sumY;
	
	/**
	 * 
	 */
	protected StandardDynamicGesture() {
		_knownPoints = new HashMap();
		_traceData = new HashMap();
		_oldCentroid = new Location(0, 0);
		_newCentroid = new Location(0, 0);
	}

	/**
	 * Get the name of this gesture.
	 * 
	 * @return
	 * 		The name of this gesture.
	 */
	//@override
	public abstract String getName();

	//@override
	public Vector processChange(Vector touchPoints,
			TouchPoint changedPoint) {
		Vector events = null;
		
		switch(changedPoint.getState()) {
			case TouchState.BIRTH:
				events = handleBirth(changedPoint);
				break;
			case TouchState.MOVE:
				events = handleMove(changedPoint);
				break;
			case TouchState.DEATH:
				//System.out.println("calling handledeath");
				events = handleDeath(changedPoint);
				//System.out.println(events);
				break;
		}
		
		return (events != null) ? events : new Vector();
	}
	
	/**
	 * 
	 * @param touchPoint
	 * @return TouchData
	 */
	protected TouchData createTouchData(TouchPoint touchPoint) {
		return new TouchData(touchPoint.getLocation());
	}
	
	/**
	 * 
	 * @param touchData
	 * @return Vector
	 */
	protected abstract Vector processBirth(TouchData touchData);
	
	/**
	 * 
	 * @param touchData
	 * @return Vector
	 */
	protected abstract Vector processMove(TouchData touchData);
	
	/**
	 * 
	 * @param touchData
	 * @return Vector
	 */
	protected abstract Vector processDeath(TouchData touchData);
	
	/**
	 * 
	 * @param touchPoint
	 * @return Vector
	 */
	private Vector handleBirth(TouchPoint touchPoint) {
		TouchData touchData = createTouchData(touchPoint);
		_knownPoints.put(new Integer(touchPoint.getID()), touchData);
		moveCentroid(touchData.getLocation().getX(), touchData.getLocation().getY());
		return processBirth(touchData);
	}
	
	/**
	 * 
	 * @param touchPoint
	 * @return Vector
	 */
	private Vector handleMove(TouchPoint touchPoint) {
		TouchData touchData = (TouchData) _knownPoints.get(new Integer(touchPoint.getID()));
		touchData.setLocation(touchPoint.getLocation());
		moveCentroid(
				touchData.getLocation().getX() - touchData.getOldLocation().getX(),
				touchData.getLocation().getY() - touchData.getOldLocation().getY()
		);
		return processMove(touchData);
	}
	
	/**
	 * 
	 * @param touchPoint
	 * @return Vector
	 */
	private Vector handleDeath(TouchPoint touchPoint) {
	  Integer iid = new Integer(touchPoint.getID());
		TouchData touchData = (TouchData) _knownPoints.get(iid);
		touchData.setLocation(touchPoint.getLocation());
		_knownPoints.remove(iid);
		moveCentroid(-touchData.getOldLocation().getX(), -touchData.getOldLocation().getY());
		return processDeath(touchData);
	}
	
	/**
	 * 
	 * @param dx
	 * @param dy
	 */
	private void moveCentroid(float dx, float dy) {
		_sumX += dx;
		_sumY += dy;
		_oldCentroid = _newCentroid;
		_newCentroid = new Location(_sumX / _knownPoints.size(), _sumY / _knownPoints.size());
	}

	/**
	 * 
	 */
	public class TouchData {
		private Location _location;
		private Location _oldLocation;
		public TouchData(Location location) {
			_oldLocation = _location = location;
		}
		public Location getLocation() {
			return _location;
		}
		public Location getOldLocation() {
			return _oldLocation;
		}
		public void setLocation(Location location) {
			_oldLocation = _location;
			_location = location;
		}
	}
}
