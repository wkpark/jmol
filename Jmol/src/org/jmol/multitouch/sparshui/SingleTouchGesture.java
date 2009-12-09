package org.jmol.multitouch.sparshui;

import java.util.Vector;

import com.sparshui.common.TouchState;
import com.sparshui.common.messages.events.DblClkEvent;
import com.sparshui.common.messages.events.TouchEvent;
import com.sparshui.gestures.Gesture;
import com.sparshui.gestures.GestureType;
import com.sparshui.server.TouchPoint;

/**
 * SINGLE_TOUCH_GESTURE
 * 
 * only passes single-touch gestures.
 * 
 * 
 */
public class SingleTouchGesture implements Gesture {

  private int _nCurrent = 0;
  private TouchPoint birth;
  private int nMoves;
  
  //@override
  public String getName() {
    return "SingleTouchGesture";
  }
  
  //@override
  public int getGestureType() {
    return GestureType.TOUCH_GESTURE;
  }

  /**
   * 
   * incorporates double-click gesture
   * 
   * @param touchPoints
   * @param changedTouchPoint
   * @return Vector of Events
   * 
   */
  // @override
  public Vector processChange(Vector touchPoints, TouchPoint changedTouchPoint) {
    System.out.println("SingleTouchGesture processChange1 " + _nCurrent
        + "  state=" + changedTouchPoint.getState());
    Vector retEvents = new Vector();
    boolean isDoubleClick = false;
    switch (changedTouchPoint.getState()) {
    case TouchState.BIRTH:
      if (++_nCurrent > 1)
        return new Vector();
      birth = new TouchPoint(changedTouchPoint);
      nMoves = 0;
      break;
    case TouchState.MOVE:
      if (_nCurrent > 1)
        return retEvents;
      switch (++nMoves) {
      case 2:
        if (changedTouchPoint.getTime() - birth.getTime() > 500) {
          // long (1/2 sec) pause and drag == double-click-drag ==> _translate
          TouchPoint tp = new TouchPoint(birth);
          tp.setState(TouchState.DEATH);
          retEvents.add(new TouchEvent(tp));
          tp.setState(TouchState.BIRTH);
          retEvents.add(new TouchEvent(tp));
        }
        break;
      }
      break;
    case TouchState.DEATH:
      if (--_nCurrent > 0)
        return retEvents;
      _nCurrent = 0;
      // single fingers only here
      // look for a long click --> dbl-click (not right-click)
      isDoubleClick = (nMoves <= 2 && birth.isNear(changedTouchPoint) && changedTouchPoint
          .getTime()
          - birth.getTime() > 500 /* ms */);
      break;
    }
    retEvents.add(new TouchEvent(changedTouchPoint));
    if (isDoubleClick)
      retEvents.add(new DblClkEvent(changedTouchPoint));
    return retEvents;
  }
}
