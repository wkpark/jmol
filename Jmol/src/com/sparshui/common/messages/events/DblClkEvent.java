package com.sparshui.common.messages.events;

import com.sparshui.common.Event;
import com.sparshui.common.utils.Converter;

/*
 * 
 * adapted by Bob Hanson for Jmol 11/29/2009 to include time and state
 * 
 */
public class DblClkEvent implements Event {
	private static final long serialVersionUID = -1643892133742179717L;
	
  //@override
  public int getEventType() {
    return EventType.DBLCLK_EVENT;
  }

	private int _id;
	private float _x;
	private float _y;
  private long _time;
  private int _state;

	//default constructor
	public DblClkEvent() {
		_id = 0;
		_x = 0;
		_y = 0;
	}
	
	//specific constructor
	/**
	 * 
	 * @param id
	 * @param x
	 * @param y
	 * @param state
	 */
  public DblClkEvent(int id, float x, float y, int state) {
    _id = id;
    _x = x;
    _y = y;
    _state = state;
    _time = System.currentTimeMillis();
  }
  	
	public int getTouchID() {
		return _id;
	}
	
	public int getState() {
	  return _state;
	}
	
  public long getTime() {
    return _time;
  }
  
	public float getX() {
		return _x;
	}
	
	public float getY() {
		return _y;
	}
	
	public void setX(float x) {
		_x = x;
	}
	
	public void setY(float y) {
		_y = y;
	}
	
  /**
   *  Constructs a new DlbClkEvent from a serialized version
   *    - 4 bytes : id
   *    - 4 bytes : x
   *    - 4 bytes : y
   *      - 4 bytes   : state
   *      - 8 bytes   : time
   *    - 24 bytes total
   *
   * @param data the serialized version of touchEvent
   */
  public DblClkEvent(byte[] data) {
    if (data.length < 24) {
      System.err.println("An error occurred while deserializing a DblClkEvent.");
    } else {
      _id = Converter.byteArrayToInt(data, 0);
      _x = Converter.byteArrayToFloat(data, 4);
      _y = Converter.byteArrayToFloat(data, 8);
      _state = Converter.byteArrayToInt(data, 12);
      _time = Converter.byteArrayToLong(data, 16);
    }
  }
  
  /**
   * Constructs the data packet with this event data. Message format for this
   * event:
   *    - 4 bytes : event type
   *    - 4 bytes : id
   *    - 4 bytes : x
   *    - 4 bytes : y
   *      - 4 bytes   : state
   *    - 8 bytes : time
   *    - 28 bytes total
   * @return serialized data
   */
  public byte[] serialize() {
    byte[] data = new byte[28];
    Converter.intToByteArray(data, 0, getEventType());
    Converter.intToByteArray(data, 4, _id);
    Converter.floatToByteArray(data, 8, _x);
    Converter.floatToByteArray(data, 12, _y);
    Converter.intToByteArray(data, 16, _state);
    Converter.longToByteArray(data, 20, _time);
    return data;
  } 
}
