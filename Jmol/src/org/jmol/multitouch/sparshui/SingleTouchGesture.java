package org.jmol.multitouch.sparshui;

import java.util.Vector;

import org.jmol.util.Logger;

import com.sparshui.common.TouchState;
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

  private static final long MAXIMUM_CLICK_TIME = 200;
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
    if (Logger.debugging)
      Logger.info("\nSingle id=" + changedTouchPoint.getID() + " state=" + changedTouchPoint.getState() + " ncurrent=" + _nCurrent + " nMoves=" + nMoves);
    Vector retEvents = new Vector();
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
        if (checkClick(changedTouchPoint, retEvents, false))
          return retEvents;
        break;
      }
      break;
    case TouchState.DEATH:
      if (--_nCurrent > 0)
        return retEvents;
      _nCurrent = 0;
      if (nMoves < 2 && checkClick(changedTouchPoint, retEvents, true))
        return retEvents;
      break;
    }
    if (Logger.debugging)
      Logger.info("Single creating new touch event");
    retEvents.add(new TouchEvent(changedTouchPoint));
    return retEvents;
  }

  private boolean checkClick(TouchPoint tpNew, Vector retEvents,
                                   boolean isDeath) {
    TouchPoint tp;
    long dt = tpNew.getTime() - birth.getTime();
    boolean isSingleClick = (isDeath && dt < MAXIMUM_CLICK_TIME);
    if (dt < 500 && !isSingleClick)
      return false;
    nMoves += 2;
    // long (1/2 sec) pause and drag == double-click-drag ==> _translate
    tp = new TouchPoint(birth);
    tp.setState(TouchState.DEATH);
    retEvents.add(new TouchEvent(tp));
    tp.setState(TouchState.CLICK);
    retEvents.add(new TouchEvent(tp));
    if (isSingleClick)
      return true;
    tp.setState(TouchState.BIRTH);
    retEvents.add(new TouchEvent(tp));
    if (!isDeath)
      return true;
    tp.setState(TouchState.DEATH);
    retEvents.add(new TouchEvent(tp));
    tp.setState(TouchState.CLICK);
    retEvents.add(new TouchEvent(tp));
    return true;
  }
}
