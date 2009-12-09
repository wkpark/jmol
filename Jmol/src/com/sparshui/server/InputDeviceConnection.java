package com.sparshui.server;

import java.io.DataInputStream;
//import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import com.sparshui.common.Location;
import com.sparshui.common.NetworkConfiguration;
import com.sparshui.common.TouchState;

/**
 * Represents a connection to the input device.
 * 
 * @author Tony Ross
 *
 */
public class InputDeviceConnection implements Runnable {
	
	/**
	 * 
	 */
	private GestureServer _gestureServer;
	
	/**
	 * 
	 */
	private Socket _socket;
	
	/**
	 * 
	 */
	private DataInputStream _in;
  //private DataOutputStream _out;
  
	
	/**
	 * 
	 */
	private HashMap _touchPoints;

	private Vector _flaggedids;

	/**
	 * Create a new input device connection with the given
	 * gesture server and socket.
	 * 
	 * @param gestureServer
	 * 		The gesture server.
	 * @param socket 
	 * @throws IOException 
	 * 		If there is a communication error.
	 */
	public InputDeviceConnection(GestureServer gestureServer, Socket socket) throws IOException {
		_gestureServer = gestureServer;
		_socket = socket;
    _in = new DataInputStream(socket.getInputStream());
    //_out = new DataOutputStream(socket.getOutputStream());
		_touchPoints = new HashMap();
		_flaggedids = new Vector();
		startListening();
	}
	
	/**
	 * 
	 */
	private void removeDeadTouchPoints() {
		for (int i = 0; i < _flaggedids.size(); i++) {
		  Integer id = (Integer)_flaggedids.get(i);
		_touchPoints.remove(id);
		}
		_flaggedids.clear();
	}
	
	/**
	 * 
	 * @param id
	 */
	private void flagTouchPointForRemoval(int id) {
		_flaggedids.add(new Integer(id));
	}
	
	/**
	 * 
	 */
	private void receiveData() {
		try {
			while(!_socket.isInputShutdown()) {
				/*boolean doConsume = */ readTouchPoints(_in.readInt());
				//_out.write((byte) (doConsume ? 1 : 0)); 
			}
		} catch (IOException e) {
			System.out.println("[InputDeviceConnection] InputDevice Disconnected");
			_gestureServer.notifyInputLost();
		}
	}
	
	/**
   * Modified for Jmol to also transmit the time as a long
   * 
   * @return doConsume
   * @throws IOException
   */
  private boolean readTouchPoint() throws IOException {
    int id = _in.readInt();
    float x = _in.readFloat();
    float y = _in.readFloat();
    Location location = new Location(x, y);
    int state = (int) _in.readByte();
    long time = _in.readLong();
    boolean doConsume = _gestureServer.processTouchPoint(_touchPoints, id,
        location, time, state);
    if (state == TouchState.DEATH)
      flagTouchPointForRemoval(id);
    return doConsume;
  }
	
	/**
	 * 
	 * @param count 
	 * @return doConsume
	 * @throws IOException
	 */
	private boolean readTouchPoints(int count) throws IOException {
		// With Count
		if(count < 0) {
			_in.close();
			return false;
		}
    boolean doConsume = false;
		//System.out.println("Reading '"+count+"' Input Events.");
		for(int i = 0; i < count; i++)
			doConsume |= readTouchPoint();
		removeDeadTouchPoints();
    return doConsume;
	}
	
	/**
	 * 
	 */
	private void startListening() {
		Thread thread = new Thread(this);
		thread.setName("SparshUI Server->InputDeviceConnection on port " + NetworkConfiguration.DEVICE_PORT);
		thread.start();
	}

	/**
	 * Begin receiving data from the input device.
	 */
	//@override
	public void run() {
		receiveData();
	}
	
}
