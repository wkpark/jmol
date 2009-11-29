package com.sparshui.gestures;

import java.util.Iterator;
import java.util.Vector;

import com.sparshui.common.Event;
import com.sparshui.common.TouchState;
import com.sparshui.common.messages.events.*;
import com.sparshui.server.TouchPoint;

public class SpinGesture extends StandardDynamicGesture {
	private class AXIS {
		protected final static int XAXIS = 0;
		protected final static int YAXIS = 1;
		protected final static int ZAXIS = 2;
	}

	private final double EPSSPIN = 10.0f;

	private Vector _axisPoints;
	MultiPointDragGesture _multiPointDragGesture;
	int _fixedAxis;

	public SpinGesture() {
		_axisPoints = new Vector();
		_fixedAxis = AXIS.ZAXIS; /*
									 * default spin is rotate, which is rotate
									 * about z axis
									 */
		_multiPointDragGesture = new MultiPointDragGesture();
	}

	//@override
	public String getName() {
		return "SpinGesture";
	}
	
	//@override
	public int getGestureType() {
		return GestureType.SPIN_GESTURE;
	}

	//@override
	protected Vector processBirth(TouchData touchData) {
		// TODO Auto-generated method stub
		return null;
	}

	//@override
	protected Vector processDeath(TouchData touchData) {
		// TODO Auto-generated method stub
		return null;
	}

	//@override
	protected Vector processMove(TouchData touchData) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void handleAxisPointBirth(TouchPoint touchPoint) {
		float dx, dy;
		switch (_axisPoints.size()) {
		case 1:
		  TouchPoint tp = (TouchPoint) _axisPoints.elementAt(0);
			/* calculate the fixed axis */
			dx = Math.abs(touchPoint.getLocation().getX()
					- tp.getLocation().getX());
			dy = Math.abs(touchPoint.getLocation().getY()
					- tp.getLocation().getY());
			if (Math.abs(dx) > Math.abs(dy)) {
				_fixedAxis = AXIS.YAXIS;
			} else {
				_fixedAxis = AXIS.XAXIS;
			}
			/* fall through */
		case 0:
			_axisPoints.add(touchPoint);
		default:
			/* do nothing */
		}
	}

	protected boolean isAxisPoint(TouchPoint touchPoint) {
		Iterator touchPointIterator = _axisPoints.iterator();
		while (touchPointIterator.hasNext()) {
			if (((TouchPoint) touchPointIterator.next()).getID() == touchPoint.getID()) {
				return true;
			}
		}
		return false;
	}

	public Vector processChange(Vector touchPoints, TouchPoint touchPoint) {
    /*
     * In the case of the birth of a point, see whether the axis has been set,
     * if it has, then handle it as the dragGesture's birth. In the case of a
     * move, if the axisPoint is moved, ignore it. In the case of a death, if
     * the axisPoint dies, then destroy the gesture
     */
    if (_axisPoints.size() < 2 || isAxisPoint(touchPoint)) {
      switch (touchPoint.getState()) {
      case TouchState.BIRTH:
        handleAxisPointBirth(touchPoint);
        break;
      case TouchState.MOVE:
        /* do nothing */
        break;
      case TouchState.DEATH:
        /* delete _multiPointDragGesture */
        // _multiPointDragGesture = null;
      }
      return new Vector();
    }
    /* the changed touch point corresponds to the multiPointDrag */
    Vector ev = _multiPointDragGesture.processChange(touchPoints, touchPoint);
    int i = 0;

    /* Get the drag events, if any, and convert them to spin */
    for (i = 0; i < ev.size(); ++i) {
      if (ev.elementAt(i) instanceof DragEvent) {
        ev.insertElementAt(dragToSpin((Event) ev.elementAt(i)), i);
        ev.remove(i + 1);
      }
    }
    return ev;
  }

	protected Event dragToSpin(Event event) {
    float xTheta, yTheta;
    if (event instanceof DragEvent) {

      switch (_fixedAxis) {
      case AXIS.XAXIS:
        yTheta = (float) (((DragEvent) event).getAbsY() * EPSSPIN);
        return new SpinEvent((float) 0, yTheta, (float) 0);
      case AXIS.YAXIS:
        xTheta = (float) (((DragEvent) event).getAbsX() * EPSSPIN);
        return new SpinEvent(xTheta, 0, 0);
      default:
        return new SpinEvent(0, 0, 0);
      }
    }
    return null;

  }

}
