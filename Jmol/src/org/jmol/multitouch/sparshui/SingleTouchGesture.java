package org.jmol.multitouch.sparshui;

import java.util.Vector;

import com.sparshui.common.TouchState;
import com.sparshui.gestures.TouchGesture;
import com.sparshui.server.TouchPoint;

/**
 * SINGLE_TOUCH_GESTURE
 * 
 * only passes single-touch gestures.
 * 
 * 
 */
public class SingleTouchGesture extends TouchGesture {

  private int _nCurrent = 0;

  // @override
  public Vector processChange(Vector touchPoints, TouchPoint changedTouchPoint) {
    System.out.println("SingleTouchGesture processChange1 " + _nCurrent + "  state=" + changedTouchPoint.getState());
    switch (changedTouchPoint.getState()) {
    case TouchState.BIRTH:
      if (++_nCurrent > 1)
        return new Vector();
      break;
    case TouchState.MOVE:
      if (_nCurrent > 1)
        return new Vector();
      break;
    case TouchState.DEATH:
      if (--_nCurrent > 0)
        return new Vector();
      _nCurrent = 0;
      break;
    }
    System.out.println("SingleTouchGesture processChange2 " + _nCurrent);
    return super.processChange(touchPoints, changedTouchPoint);
  }
}
