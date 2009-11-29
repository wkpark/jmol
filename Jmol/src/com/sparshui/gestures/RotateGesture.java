package com.sparshui.gestures;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import com.sparshui.common.Location;
import com.sparshui.common.messages.events.RotateEvent;
import com.sparshui.server.TouchPoint;

/**
 * 
 */
public class RotateGesture extends StandardDynamicGesture {

	/**
	 * 
	 */
	private float _rotation = 0f;

	/**
	 * 
	 */
	private Location _offset = null;

	/**
	 * 
	 */
	protected Location _offsetCentroid = null;

	//@override
	public String getName() {
		return "RotateGesture";
	}
	
	//@override
	public int getGestureType() {
		return GestureType.ROTATE_GESTURE;
	}

	/**
	 * Override to create RotateData instances instead of TouchData. This way we
	 * can use the existing _knownPoints HashMap, but add the extra data on
	 * rotation that we need to do our calculations.
	 * @param touchPoint 
	 * @return TouchData
	 */
	//@override
	protected TouchData createTouchData(TouchPoint touchPoint) {
		return new RotateData(touchPoint.getLocation());
	}

	//@override
	protected Vector processBirth(TouchData touchData) {
		if (_offset == null) {
			_offset = new Location(0, 0);
			_offsetCentroid = _newCentroid;
		} else {
			adjustOffset();
		}
		calculateRotation();
		return null;
	}

	//@override
	protected Vector processDeath(TouchData touchData) {
		if (_knownPoints.size() == 0) {
			_offset = null;
			_offsetCentroid = null;
		} else {
			adjustOffset();
		}
		calculateRotation();
		return null;
	}

	//@override
	protected Vector processMove(TouchData touchData) {
		Vector events = new Vector();
		adjustOffset();
		updateOffsetCentroid();
		_rotation = calculateRotation();
		events.add(new RotateEvent(_rotation, _newCentroid));
		
		return events;
	}

	/**
	 * 
	 */
	protected class RotateData extends TouchData {
		private float _angle;
		private float _oldAngle;

		public RotateData(Location location) {
			super(location);
			_oldAngle = _angle = 0f;
		}

		public float getAngle() {
			return _angle;
		}

		public float getOldAngle() {
			return _oldAngle;
		}

		public void setAngle(float angle) {
			_oldAngle = _angle;
			_angle = angle;
		}
	}

	private float calculateRotation() {
		// TODO Auto-generated method stub
		float retRotation = 0;
		Collection touchPoints = _knownPoints.values();
		Iterator touchDataIterator = touchPoints.iterator();
		while (touchDataIterator.hasNext()) {
			TouchData pointData = (TouchData) touchDataIterator.next();
			RotateData pointRotateData = null;
			// System.out.println("Processing Rotate");
			if (pointData instanceof RotateData) {
				// System.out.println("Cast successful");
				pointRotateData = (RotateData) pointData;
				// Find the new angle
				float angle = (float) Math.atan2(pointRotateData.getLocation()
						.getY()
						- _newCentroid.getY(), pointRotateData.getLocation()
						.getX()
						- _newCentroid.getX());
				pointRotateData.setAngle(angle);

				// Calculate the angle change
				float deltaAngle = pointRotateData.getAngle()
						- pointRotateData.getOldAngle();
				//System.out.println("Angle = " + pointRotateData.getAngle() + ", Old Angle = " + pointRotateData.getOldAngle());
				// Normalize the change in angle
				if (deltaAngle > Math.PI)
					deltaAngle -= 2 * Math.PI;
				else if (deltaAngle < -Math.PI)
					deltaAngle += 2 * Math.PI;
				// System.out.println("New Centroid: " +
				// _newCentroid.toString());
				// System.out.println("Point: + " +
				// pointZoomData.getDistance());
				retRotation += deltaAngle;
			}
		}
		retRotation = retRotation / _knownPoints.size();
		return retRotation;

	}

	/**
	 * 
	 */
	protected void adjustOffset() {
		_offset = new Location(_newCentroid.getX() - _oldCentroid.getX(),
				_newCentroid.getY() - _oldCentroid.getY());
	}

	/**
	 * 
	 */
	protected void updateOffsetCentroid() {
		float x = _newCentroid.getX() - _offset.getX();
		float y = _newCentroid.getY() - _offset.getY();
		_offsetCentroid = new Location(x, y);
	}

	/*
	 * //@override public Vector processChange(Vector
	 * touchPoints, TouchPoint changedTouchPoint) { Vector retEvents =
	 * new Vector(); // If this is the first update, initialize the
	 * known points if (_knownPoints.size() == 0) { _sumX = 0; _sumY = 0; for
	 * (TouchPoint touchPoint : touchPoints) { addTouchPoint(touchPoint); }
	 * _oldCentroid = new Location(_sumX, _sumY); _newCentroid = new
	 * Location(_sumX, _sumY); } else { // A touch point has been changed.
	 * Recalculate the centroid // and fire a drag event if a point has moved
	 * 
	 * _oldCentroid = _newCentroid; _sumX = _oldCentroid.getX() *
	 * _knownPoints.size(); _sumY = _oldCentroid.getY() * _knownPoints.size(); //
	 * Process a birth if (changedTouchPoint.getState() == TouchState.BIRTH) {
	 * processBirth(changedTouchPoint); } // Process a move else if
	 * (changedTouchPoint.getState() == TouchState.MOVE) {
	 * processMove(changedTouchPoint); _rotation =
	 * getRotationChange(touchPoints); retEvents.add(new RotateEvent(_rotation,
	 * _newCentroid)); return retEvents; } // Process a death else if
	 * (changedTouchPoint.getState() == TouchState.DEATH) {
	 * _knownPoints.remove(changedTouchPoint); _newCentroid = new
	 * Location(_sumX, _sumY); } } return retEvents; }
	 * 
	 * private float getRotationChange(Vector touchPoints) {
	 * 
	 * float rotation = 0;
	 * 
	 * for (TouchPoint touchPoint : touchPoints) { float newAngle =
	 * getAngle(touchPoint, _newCentroid); float oldAngle = getAngle(touchPoint,
	 * _oldCentroid); float deltaAngle = newAngle - oldAngle; if (deltaAngle >
	 * Math.PI) deltaAngle -= 2 * Math.PI; else if (deltaAngle < -Math.PI)
	 * deltaAngle += 2 * Math.PI; if (!Double.isNaN(deltaAngle)) { rotation +=
	 * deltaAngle; } }
	 * 
	 * return rotation; }
	 * 
	 * private float getAngle(TouchPoint touchPoint, Location center) { float
	 * retAngle;
	 * 
	 * float y = touchPoint.getLocation().getX() - center.getX(); float x =
	 * touchPoint.getLocation().getY() - center.getY();
	 * 
	 * retAngle = (float) Math.atan2(y, x); return retAngle; }
	 */
}
