package org.jmol.multitouch.sparshui;

import java.util.Vector;

import javax.vecmath.Vector3f;

import org.jmol.util.Logger;
import org.jmol.viewer.ActionManagerMT;

import com.sparshui.common.Event;
import com.sparshui.common.Location;
import com.sparshui.common.TouchState;
import com.sparshui.common.messages.events.DragEvent;
import com.sparshui.common.messages.events.RotateEvent;
import com.sparshui.common.messages.events.ZoomEvent;
import com.sparshui.gestures.Gesture;
import com.sparshui.server.TouchPoint;

/**
 * TWO_POINT_GESTURE
 * 
 * This gesture requires two points of contact, but its type is not initially
 * defined. Instead, its type is determined on-the-fly to be one of ZOOM,
 * ROTATE, or 2-point DRAG based on the direction of motion and relative
 * positions of the starting points. Two traces are obtained, assuming nothing
 * about the ID of the incoming points from the input device but instead
 * operating from position on the screen.
 * 
 * ZOOM IN:    <---- x ----->  (at any angle)
 * ZOOM OUT:   ----> x <-----  (at any angle)  
 * 
 * ROTATE CW:    ^        |
 *               |   x    |    (at any angle)
 *               |        V
 *    
 * ROTATE CCW:   |        ^
 *               |   x    |    (at any angle)
 *               V        |
 *    
 * 2-point drag:
 * 
 *      --------->  
 *     x             (Jmol will recognize horizontal as "next frame")
 *      --------->    
 *    
 * It should work with any simple two-point touchpad.
 * 
 * Bob Hanson 11/29/2009
 * 
 * 
 */
public class TwoPointGesture implements Gesture /*extends StandardDynamicGesture*/ {

  /**
	 * 
	 */

  private int _myType = ActionManagerMT.INVALID_GESTURE;

  protected Location _offset = null;

  protected Location _offsetCentroid = null;
  private Vector _traces1 = new Vector();
  private Vector _traces2 = new Vector();
  private int _id1 = -1;
  private int _id2 = -1;
  private int _nTraces = 0;
  private float _scale;
  private float _rotation;
  private float _distance0;
  private Vector3f _v00;


  // @override
  public String getName() {
    return "TwoPointGesture";
  }

  // @override
  public int getGestureType() {
    return _myType;
  }

  //@override
  public Vector processChange(Vector touchPoints,
      TouchPoint changedPoint) {
    return processChangeSync(touchPoints, changedPoint);
  }
  private synchronized Vector processChangeSync(Vector touchPoints,
                                        TouchPoint changedPoint) {
    Vector events = null;
    switch(changedPoint.getState()) {
      case TouchState.BIRTH:
        events = processBirth(changedPoint);
        break;
      case TouchState.MOVE:
        events = processMove(changedPoint);
        break;
      case TouchState.DEATH:
        events = processDeath(changedPoint);
        break;
    }
    return (events != null) ? events : new Vector();
  }
  
  // @override
  protected Vector processBirth(TouchPoint touchPoint) {
    Location location = touchPoint.getLocation();
    int id = touchPoint.getID();
    System.out.println("TwoPointGesture birth ntraces:" + _nTraces + " ids:" + _id1+","+_id2+ " id:" + id);
    switch (_nTraces) {
    case 0:
      _traces1.clear();
      _traces1.add(Location.pixelLocation(location));
      _id1 = id;
      _nTraces = 1;
      break;
    case 1:
      _traces2.clear();
      _traces2.add(Location.pixelLocation(location));
      _id2 = id;
      Object o = _traces1.lastElement();
      _traces1.clear();
      _traces1.add(o);
      _nTraces = 2;
      break;
    }
    return null;
  }

  // @override
  protected Vector processMove(TouchPoint touchPoint) {
    int id = touchPoint.getID();
    if (id != _id1 && id != _id2) {
      
    }
    System.out.println("TwoPointGesture move ntraces:" + _nTraces + " ids:" + _id1+","+_id2+ " id:" + touchPoint.getID());
    Vector events = new Vector();
    if (!updateLocations(touchPoint))
      return events;
    if (_myType == ActionManagerMT.INVALID_GESTURE)
      checkType();
    if (_myType == ActionManagerMT.INVALID_GESTURE 
        || !updateCentroid())
      return events;
    Location location = Location.screenLocation(_offsetCentroid);
    Event event = null;
    switch (_myType) {
    case ActionManagerMT.ZOOM_GESTURE:
      event = new ZoomEvent(_scale, location);
      break;
    case ActionManagerMT.ROTATE_GESTURE:
      event = new RotateEvent(_rotation, location);
      break;
    case ActionManagerMT.MULTI_POINT_DRAG_GESTURE:
      event = new DragEvent(location, (byte) 2);
      break;
    }
    if (event != null)
      events.add(event);
    return events;
  }

  // @override
  protected Vector processDeath(TouchPoint touchPoint) {
   System.out.println("TwoPointGesture death ntraces:" + _nTraces + " ids:"
        + _id1 + "," + _id2 + " id:" + touchPoint.getID());
    if (--_nTraces == 1) {
      int id = touchPoint.getID();
      if (id == _id1) {
        _id1 = _id2;
        Vector v = _traces1;
        _traces1 = _traces2;
        _traces2 = v;
        _traces2.clear();
        _id2 = -1;
      } else if (id == _id2) {
        _traces2.clear();
        _id2 = -1;
      } else {
        _nTraces = 0;
      }
    } 
    if (_nTraces == 0) {
      _traces1.clear();
      _traces2.clear();
      _id1 = _id2 = -1;
    }
    _v00 = null;
    _myType = ActionManagerMT.INVALID_GESTURE;
    return null;
  }

  private boolean updateLocations(TouchPoint touchPoint) {
    Location location = Location.pixelLocation(touchPoint.getLocation());
    int id = touchPoint.getID();
    if (id == _id1) {
     //System.out.println("TwoPointGesture updateLocation 1: " + id + " "
          //+ location);
      _traces1.add(location);
    } else if (id == _id2) {
     //System.out.println("TwoPointGesture updateLocation 2: " + id + " "
         // + location);
      _traces2.add(location);
    } else {
     //System.out.println("TwoPointGesture updateLocation NOT: " + id);
      return false;
    }
    if (_nTraces < 2)
      return false;
    // weight centroid to the branch that is not moving
    // this works for zoom or rotation
    if (_v00 != null)
      return true;
    Location l1 = (Location) _traces1.firstElement();
    Location l2 = (Location) _traces2.firstElement();
    _v00 = new Vector3f(l2.getX() - l1.getX(), l2.getY() - l1.getY(), 0);
    _distance0 = _v00.length();
    _v00.normalize();
   //System.out.println("TwoPointGesture updateLocation _v00 and _distance0: "
   //    + _v00 + " " + _distance0);
    return true;
  }

  private void checkType() {
   //System.out.println("TwoPointGesture type=" + _myType + " _v00=" + _v00
     //   + "\n traces sizes: " + _traces1.size() + "  " + _traces2.size());
    if (_traces1.size() < 5 || _traces2.size() < 5)
      return;
    Location loc10 = (Location) _traces1.firstElement();
    Location loc20 = (Location) _traces2.firstElement();
    Location loc11 = (Location) _traces1.lastElement();
    Location loc21 = (Location) _traces2.lastElement();
    Vector3f v1 = new Vector3f(loc11.getX() - loc10.getX(), loc11.getY()
        - loc10.getY(), 0);
    Vector3f v2 = new Vector3f(loc21.getX() - loc20.getX(), loc21.getY()
        - loc20.getY(), 0);
    float d1 = v1.length();
    float d2 = v2.length();
    if (d1 <= 2 || d2 <= 2)
      return;
    v1.normalize();
    v2.normalize();
    float cos01 = Math.abs(_v00.dot(v1));
    float cos02 = Math.abs(_v00.dot(v2));
    float cos12 = v1.dot(v2);
    System.out.println("2pg cos12=" + cos12);
    if (cos12 > 0.9) {
      // two co-aligned motions
      _myType = ActionManagerMT.MULTI_POINT_DRAG_GESTURE;
    } else if (cos12 < -0.8) {
      // to classic zoom motions
      _myType = ActionManagerMT.ZOOM_GESTURE;
    }
    //if (Logger.debugging)
      Logger.info("TwoPointGesture type=" + _myType + " _v00=" + _v00
          + "\n cos01=" + cos01 + " cos02=" + cos02 + " cos12=" + cos12
          + "\n v1=" + v1 + " v2=" + v2 + " d1=" + d1 + " d2=" + d2    
          + "\n loc10=" + loc10 + " loc11=" + loc11
          + "\n loc20=" + loc20 + " loc21=" + loc21
          
      );
  }

  private boolean updateCentroid() {
    Location loc10 = (Location) _traces1.firstElement();
    Location loc20 = (Location) _traces2.firstElement();
    Location loc11 = (Location) _traces1.lastElement();
    Location loc21 = (Location) _traces2.lastElement();
    float d1 = loc11.distance(loc10);
    float d2 = loc21.distance(loc20);
    switch (_myType) {
    case ActionManagerMT.ROTATE_GESTURE:
      _offsetCentroid = new Location((loc10.getX() + loc20.getX()) / 2, 
          (loc10.getY() + loc20.getY()) / 2);
      Vector3f v1;
      Vector3f v2;
      if (d2 < 2) {
        loc10 = (Location) _traces1.get(_traces1.size() - 2);
        v1 = loc20.directionTo(loc10);
        v2 = loc20.directionTo(loc11);
      } else {
        loc20 = (Location) _traces2.get(_traces2.size() - 2);
        v1 = loc10.directionTo(loc20);
        v2 = loc10.directionTo(loc21);
      }
      v1.cross(v1, v2);
      _rotation = (v1.z < 0 ? 1 : -1);
      return true;
    case ActionManagerMT.ZOOM_GESTURE:
      d1 = loc21.distance(loc11);
      if (Math.abs(d1 - _distance0) < 2)
        return false;
      _scale = (d1 < _distance0 ? -1 : 1);
      _distance0 = d1;
      float w1 = d2 / (d1 + d2);
      float w2 = 1 - w1;
      _offsetCentroid = new Location(loc10.getX() * w1 + loc20.getX() * w2,
          loc10.getY() * w1 + loc20.getY() * w2);
      return true;
    case ActionManagerMT.MULTI_POINT_DRAG_GESTURE:
      _offsetCentroid = new Location((loc11.getX() + loc21.getX()) / 2, (loc11
          .getY() + loc21.getY()) / 2);
      return true;
    }
    return false;
  }
  
}
