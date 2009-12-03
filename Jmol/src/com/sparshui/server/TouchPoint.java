package com.sparshui.server;

import com.sparshui.common.Location;
import com.sparshui.common.TouchState;

/**
 * Represents a touch point.
 * 
 * @author Tony Ross
 *
 */
public class TouchPoint {
	/**
	 * Used to assign a globally unique id to a new touch point.
	 */
	private static int nextID = 0;
	
	/**
	 * 
	 */
	private static Object idLock = new Object();
	
	/**
	 * 
	 */
	private int _id;
	
	/**
	 * 
	 */
	private Location _location;
	
	/**
	 * 
	 */
	private int _state;
	
	/**
	 * 
	 */
	private boolean _changed;
	
	private long _time;
	
	/**
	 * 
	 */
	private Group _group;

	/**
	 * The GestureServer needs to know whether an incoming
	 * touchPoint is bound to a client or not so that it
	 * can tell the input device whether or not to
	 * consume the event.
	 * 
	 * @return whether a client is bound to this touchPoint
	 * 
	 */
	public boolean isValid() {
	  return (_group != null);
	}
	/**
	 * 
	 * @param location
	 */
	public TouchPoint(Location location) {
		synchronized(idLock) {
			_id = nextID++;
		}
		_location = location;
		_state = TouchState.BIRTH;
		_time = System.currentTimeMillis();
	}
	
	/**
	 * Copy constructor
	 * @param tp
	 */
	public TouchPoint(TouchPoint tp) {
		_id = tp._id;
		_location = tp._location;
		_state = tp._state;
		_time = tp._time;
	}

	public long getTime() {
		return _time;
	}
	
	/**
	 * Get the touch point ID.
	 * @return
	 * 		The touch point ID.
	 */
	public int getID() {
		return _id;
	}
	
	/**
	 * Get the touch point location.
	 * @return
	 * 		The location of this touch point.
	 */
	public Location getLocation() {
		return _location;
	}
	
	/**
	 * Get the touch point state.
	 * @return
	 * 		The state of this touch point.
	 */
	public int getState() {
		return _state;
	}
	
	/**
	 * Set the group for this touch point.
	 * 
	 * @param group
	 * 		The group the touch point should belong to.
	 */
	public void setGroup(Group group) {
		_group = group;
		//System.out.println("Group set, group = " + _group.toString());
		_group.update(this);
	}
	
	/**
	 * Update this touch point with a new location and state.
	 * 
	 * @param location
	 * 		The new location.
	 * @param state
	 * 		The new state.
	 */
	public void update(Location location, int state) {
		_location = location;
		_state = state;
		_changed = true;
		if(_group != null) _group.update(this);
	}
	
	/**
	 * Reset the changed flag.
	 */
	public void resetChanged() {
		_changed = false;
	}
	
	/**
	 * Get the value of the changed flag.
	 * @return
	 * 		True if this touchpoint has changed since the
	 * 		last time resetChanged() was called.
	 */
	public boolean isChanged() {
		return _changed;
	}
	
	//@override
	public Object clone() {
		return new TouchPoint(this);
	}

}
