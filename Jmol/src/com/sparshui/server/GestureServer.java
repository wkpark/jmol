package com.sparshui.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import org.jmol.api.JmolGestureServerInterface;
import org.jmol.util.Logger;

import com.sparshui.common.ConnectionType;
import com.sparshui.common.Location;
import com.sparshui.common.NetworkConfiguration;

/**
 * The main gesture server class.
 * In the Jmol version, this server is created by 
 * org.jmol.multitouch.sparshui.JmolSparshClientAdapter
 * so there is no main method.
 * 
 * adapted by Bob Hanson for Jmol 11/29/2009
 *
 * @author Tony Ross
 * 
 */

public class GestureServer implements Runnable, JmolGestureServerInterface {
  private ServerSocket _serverSocket;
  private Vector _clients = new Vector();
  private Thread gsThread;
  public void startGestureServer() {
    gsThread = new Thread(new GestureServer());
    gsThread.setName("Jmol SparshUI GestureServer on port " + NetworkConfiguration.PORT);
    gsThread.start();
  }

  public void dispose() {
    try {
      _serverSocket.close();
    } catch (Exception e) {
      // ignore
    }
    try {
      gsThread.interrupt();
    } catch (Exception e) {
      // ignore      
    }
    _serverSocket = null;
    gsThread = null;
  }

  /**
   * Start accepting connections.
   */
  public void run() {
    try {
      openSocket();
      acceptConnections();
    } catch (Exception e) {
      Logger.info("[GestureServer] connection unavailable");
    }
  }

  /**
	 * 
	 */
  private void openSocket() {
    try {
      _serverSocket = new ServerSocket(NetworkConfiguration.PORT);
      Logger.info("[GestureServer] Socket Open");
    } catch (IOException e) {
      Logger.error("[GestureServer] Failed to open a server socket.");
      e.printStackTrace();
    }
  }

  /**
	 * 
	 */
  private void acceptConnections() {
    Logger.info("[GestureServer] Accepting Connections");
    while (!_serverSocket.isClosed()) {
      try {
        acceptConnection(_serverSocket.accept());
      } catch (IOException e) {
        Logger.error("[GestureServer] Failed to establish client connection");
        e.printStackTrace();
      }
    }
    Logger.info("[GestureServer] Socket Closed");
  }

  /**
   * 
   * @param socket
   * @throws IOException
   */
  private void acceptConnection(Socket socket) throws IOException {
    // no remote access!
    byte[] add = socket.getInetAddress().getAddress();
    if (add[0] != 127 || add[1] != 0 || add[2] != 0 || add[3] != 1)
      return;
    int type = socket.getInputStream().read();
    if (type == ConnectionType.CLIENT) {
      acceptClientConnection(socket);
    } else if (type == ConnectionType.INPUT_DEVICE) {
      acceptInputDeviceConnection(socket);
    }
  }

  /**
   * 
   * @param socket
   * @throws IOException
   */
  private void acceptClientConnection(Socket socket) throws IOException {
    Logger.info("[GestureServer] ClientConnection Accepted");
    ClientConnection cc = new ClientConnection(socket);
    _clients.add(cc);
  }

  /**
   * 
   * @param socket
   * @throws IOException
   */
  private void acceptInputDeviceConnection(Socket socket) throws IOException {
    Logger.info("[GestureServer] InputDeviceConnection Accepted");
    new InputDeviceConnection(this, socket);
  }

  /**
   * This method was tucked into InputDeviceConnection but really has to do more
   * with server-to-client interaction, so I moved it here. BH
   * 
   * @param inputDeviceTouchPoints
   *          container for this input device's touchPoints
   * @param id
   * @param location
   * @param state
   * @return doConsume;
   */
  boolean processTouchPoint(HashMap inputDeviceTouchPoints, int id,
                            Location location, int state) {
    Integer iid = new Integer(id);
    if (inputDeviceTouchPoints.containsKey(iid)) {
      TouchPoint touchPoint = (TouchPoint) inputDeviceTouchPoints.get(iid);
      if (touchPoint.isValid()) {
        synchronized (touchPoint) {
          touchPoint.update(location, state);
        }
        return true;
      }
      return false;
    }
    TouchPoint touchPoint = new TouchPoint(location);
    inputDeviceTouchPoints.put(iid, touchPoint);
    return processBirth(touchPoint);
  }

  /**
   * Process a touch point birth by getting the groupID and gestures for the
   * touch point.
   * 
   * @param touchPoint
   *          The new touch point.
   * @return doConsume
   * 
   */
  private boolean processBirth(TouchPoint touchPoint) {
    Vector clients_to_remove = null;
    boolean doConsume = false;
    for (int i = 0; i < _clients.size(); i++) {
      ClientConnection client = (ClientConnection) _clients.get(i);
      // Return if the client claims the touch point
      try {
        doConsume = client.processBirth(touchPoint);
        if (doConsume)
          break;
      } catch (IOException e) {
        // This occurs if there is a communication error
        // with the client. In this case, we will want
        // to remove the client.
        if (clients_to_remove == null)
          clients_to_remove = new Vector();
        clients_to_remove.add(client);
      }
    }
    if (clients_to_remove != null)
      for (int i = 0; i < clients_to_remove.size(); i++) {
        _clients.remove(clients_to_remove.elementAt(i));
        Logger.info("[GestureServer] Client Disconnected");
      }
    return doConsume;
  }


}
