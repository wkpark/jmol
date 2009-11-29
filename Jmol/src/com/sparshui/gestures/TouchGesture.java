package com.sparshui.gestures;

import java.util.Vector;

import com.sparshui.common.messages.events.TouchEvent;
import com.sparshui.server.TouchPoint;

public class TouchGesture implements Gesture {

	//@override
	public Vector processChange(Vector touchPoints, TouchPoint changedTouchPoint) {
		Vector retEvents = new Vector();
		retEvents.add(new TouchEvent(changedTouchPoint));
		return retEvents;
	}

	//@override
	public String getName() {
		return "TouchGesture";
	}
	
	//@override
	public int getGestureType() {
		return GestureType.TOUCH_GESTURE;
	}

}
