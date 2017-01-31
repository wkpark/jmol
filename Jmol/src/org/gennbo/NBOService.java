/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.gennbo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Queue;

import javajs.util.AU;
import javajs.util.PT;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * A service for interacting with NBOServe (experimental)
 * 
 * TODO: figure out how to manage time-consuming asynchronous requests
 * 
 * 
 */
public class NBOService {

  /// BH: observations: 
  // 
  // 1) NBOServe creates jmol_infile.txt, jmol_molfile.txt, and jmol_outfile.txt. 
  // 
  // 2) Of these three, jmol_outfile is locked for writing during NBOServe sessions 
  //    and stays locked until the program finishes. 
  //    
  //    
  // 3) When View starts up, a new jmol_outfile is created, and it can be deleted.
  // 
  // 4) The process of View...Browse... leads to the following:
  // 
  //    07/02/2015  09:04 AM                20 jmol_infile.txt
  //    07/02/2015  09:04 AM               429 jmol_outfile.txt 
  //    07/02/2015  09:05 AM                95 v_test1435845900375.txt
  //    07/02/2015  09:05 AM                11 fort.106
  //    07/02/2015  09:05 AM                 0 v_test1435845900375.txtDONE
  // 
  // v_test... is 
  //
  //  GLOBAL C_PATH C:\temp
  //  GLOBAL I_KEYWORD 6
  //  GLOBAL C_JOBSTEM ch3nh2
  //  GLOBAL I_BAS_1 6
  //  CMD LABEL
  //
  // jmol_infile.txt is:
  //
  //          1    1    2
  //  
  // jmol_outfile.txt is:

  //    DATA " "
  //    1    1    2
  //  NBOServe: NATURAL BOND ORBITAL PROGRAM SUITE
  //  _______________________________________________
  //  (1) NBOModel:
  //    Create & edit molecular model and input files
  //  (2) NBORun:
  //    Launch NBO analysis for chosen archive (.47) file
  //  (3) NBOView:
  //    Display NBO orbitals in 1D/2D/3D imagery
  //  (4) NBOSearch:
  //    Search NBO output interactively
  //  Your choice (1-4), (H)elp, e(X)it, or (D)ir reset?
  //  END ""
  //
  // fort.106 is 
  //
  //    END ""
  //
  // note that the actual label information came back only over sysout.
  //
  // requesting a 3D view (raytrace) produces the following files:
  //
  //  07/02/2015  09:13 AM                97 v_test1435846433040.txt
  //  07/02/2015  09:13 AM                 0 v_test1435846433040.txtDONE
  //  07/02/2015  09:14 AM                59 v_test1435846446147.txt
  //  07/02/2015  09:14 AM           480,029 ch3nh2.bmp (in c:\temp)
  //  07/02/2015  09:14 AM                 0 v_test1435846446147.txtDONE
  //  07/02/2015  09:14 AM                 2 jmol_outfile.txt
  //  07/02/2015  09:14 AM               121 jmol_molfile.txt
  //  07/02/2015  09:14 AM             5,552 raytrace.rt

  // the first v_test is: 
  //
  //  GLOBAL C_PATH C:\temp
  //  GLOBAL C_JOBSTEM ch3nh2
  //  GLOBAL I_BAS_1 6
  //  GLOBAL SIGN +1 
  //  CMD PROFILE 10  
  // 
  // and the second is:
  //
  //  GLOBAL C_PATH C:\temp
  //  GLOBAL C_JOBSTEM ch3nh2
  //  CMD VIEW 1 

  // modes of operation

  // NOTE: There was a problem in that View and Raw were both 4 here

  static final int MODE_ERROR = -1;
  static final int MODE_RAW = 0;// leave this 0; it is referred to that in StatusListener

  // these are for panel state
  static final int MODE_MODEL          = 1; // don't change this number -- it is used for LOAD NBO 
  static final int MODE_RUN            = 20;

  // these are for rawCmdNew only
  static final int MODE_VALUE          = 45;
  static final int MODE_LIST           = 46;
  static final int MODE_LIST_MO        = 47;
  static final int MODE_IMAGE          = 88;
  static final int MODE_SEARCH_SELECT  = 90;
  static final int MODE_LABEL          = 50;
  //static final int MODE_VALUE_M = 60;
  //static final int MODE_GETRS = 61;
  static final int MODE_LABEL_BONDS    = 62;
  static final int MODE_MODEL_EDIT     = 63;
  static final int MODE_MODEL_SYMMETRY = 64;
  static final int MODE_MODEL_SAVE     = 65;
  static final int MODE_MODEL_TO_NBO   = 66;

  protected Viewer vwr;
  protected Process nboServer;
  protected Thread nboListener;
  protected NBODialog nboDialog;
  protected NBORequest currentRequest;
  protected Object lock;
  protected Queue<NBORequest> requestQueue;  
  
  private PrintWriter stdinWriter;
  protected BufferedInputStream stdout;

  private boolean cantStartServer;  
  private String serverPath;

  private String exeName = "NBOServe.exe";
  private boolean doConnect;
  
  protected boolean isReady;

  /**
   * A class to manage communication between Jmol and NBOServe.
   * 
   * @param nboDialog 
   * 
   * @param vwr
   *        The interacting display we are reproducing (source of view angle
   *        info etc)
   * @param doConnect 
   */
  public NBOService(NBODialog nboDialog, Viewer vwr, boolean doConnect) {
    this.nboDialog = nboDialog;
    this.vwr = vwr;
    this.doConnect = doConnect;
    setServerPath(nboDialog.nboPlugin.getNBOProperty("serverPath", System.getProperty("user.home")
        + "/NBOServe"));
    requestQueue = new ArrayDeque<NBORequest>();
    lock = new Object();
  }

  /**
   * Check to see if we have tried and are not able to make contact with NBOServe at the designated location.
   * 
   * @return true if we hvae found that we cannot start the server.
   * 
   */
  boolean isOffLine() {
    return cantStartServer;
  }

  /**
   * Return path to NBOServe directory.
   * 
   * @param fileName
   *        or null for path itself, without slash
   * 
   * @return path
   */
  String getServerPath(String fileName) {
    return (fileName == null ? serverPath : serverPath + "/" + fileName);
  }

  /**
   * Set path to NBOServe directory
   * 
   * @param path
   */
  protected void setServerPath(String path) {
    serverPath = path.replace('\\',  '/');
    nboDialog.nboPlugin.setNBOProperty("serverPath", path);
  }

  /**
   * Check to see that the service has been initialized.
   * 
   * @return true if the path to the server has been set.
   */
  protected boolean isEnabled() {
    return serverPath != null;
  }

  /**
   * Set the current request by writing its metacommands to disk and sending a
   * command to NBOServe directing it to that file.
   * 
   * @param request
   */
  protected void startRequest(NBORequest request) {
    if (request == null)
      return;
    currentRequest = request;
    String cmdFileName = null, data = null;
    for (int j = 0, n = request.fileData.length; j < n; j += 2) {
      // we must do file 0 last, as it is the command
      int i = (j + 2) % n;
      cmdFileName = request.fileData[i];
      data = request.fileData[i + 1];
      if (cmdFileName != null) {
        System.out.println("saving file " + cmdFileName + "\n" + data);
        nboDialog.inputFileHandler.writeToFile(getServerPath(cmdFileName),
            data);
      }
    }
    nboDialog.setStatus(request.statusInfo);
    String cmd = "<" + cmdFileName + ">";

    System.out.println("sending " + cmd);

    if (stdinWriter == null)
      restart();
    stdinWriter.println(cmd);
    stdinWriter.flush();
  }

  /**
   * Start the ProcessBuilder for NBOServe and listen to its stdout (Fortran LFN
   * 6, a Java BufferedInputStream). We simply look for available bytes and listen
   * for a 10-ms gap, which should be sufficient, since all these are done via a 
   * single flush;
   * 
   * 
   * Expected packets are of one of the following three forms:
   * 
   * *start*
   * 
   * [one or more lines]
   * 
   * *end*
   * 
   * or
   * 
   * ***errmess***
   * 
   * [one error message line]
   * 
   * or
   * 
   * [some sort of identifiable Fortran or system error message most likely
   * indicating NBO has died]
   * 
   * 
   * @return a caught exception message, or null if we are not connected or we
   *         are successful
   */
  String startProcess() {
    System.out.println("startProcess");
    try {
      cantStartServer = true;
      if (!doConnect)
        return null;
      nboListener = null;
      System.out.println("starting NBO process");
      String path = getServerPath(exeName);
      ProcessBuilder builder = new ProcessBuilder(path);
      builder.directory(new File(new File(path).getParent())); // root folder for executable 
      builder.redirectErrorStream(true);
      nboServer = builder.start();
      stdout = (BufferedInputStream) nboServer.getInputStream();
      nboListener = new Thread(new Runnable() {

        @Override
        public void run() {
          //boolean haveStart = false;
          byte[] buffer = new byte[1024];
          while (!Thread.currentThread().isInterrupted()) {
            try {
              int n = stdout.available();
              if (n <= 0)
                continue;
              isReady = true;
              int m = 0;
              do {
                n = m;
                Thread.sleep(10);
              } while ((m = stdout.available()) > n);
              while (n > buffer.length) {
                buffer = AU.doubleLengthByte(buffer);
              }
              n = stdout.read(buffer, 0, n);
              String s = new String(buffer, 0, n);
              if (!processServerReturn(s))
                continue;
              nboDialog.setStatus("");
            } catch (Throwable e1) {
              clearQueue();
              nboDialog.setStatus(e1.getMessage());
              continue;
              // includes thread death
            }
            startRequest(currentRequest = requestQueue.peek());
          }
        }
      });
      nboListener.setName("NBOServiceThread" + System.currentTimeMillis());
      nboListener.start();
      stdinWriter = new PrintWriter(nboServer.getOutputStream());
    } catch (IOException e) {
      nboDialog.logInfo(e.getMessage(), Logger.LEVEL_ERROR);
      return e.getMessage();
    }
    cantStartServer = false;
    return null;
  }

  /**
   * Log a message to NBODialog; probably an error.
   * 
   * @param line
   * @param level
   */
  protected void logServerLine(String line, int level) {
    nboDialog.logInfo(line, level);
  }

  /**
   * Check for known errors; PG is the Portland Group Compiler 
   * @param line
   * @return true if a recognized error has been found.
   */
  protected boolean isFortranError(String line) {
    return line.indexOf("Permission denied") >= 0
        || line.indexOf("PGFIO-F") >= 0 
        || line.indexOf("Invalid command") >= 0;
  }

  /**
   * Close the process and all channels associated with it. 
   */
  public void closeProcess() {
    isReady = false;
    stdout = null;
    try {
      stdinWriter.close();
    } catch (Exception e) {
    }
    stdinWriter = null;
    try {
      nboListener.interrupt();
    } catch (Exception e) {
      System.out.println("can't interrupt");
    }
    nboListener = null;
    try {
      nboServer.destroy();
    } catch (Exception e) {
    }
    nboServer = null;
  }

  /**
   * Restart the process from scratch.
   * 
   * @return null or an error message
   * 
   */
  String restart() {
    closeProcess();
    return startProcess();
  }

  /**
   * Restart the processor only if necessary
   * 
   * @return true if successful
   */
  public boolean restartIfNecessary() {
    if (nboServer == null)
      startProcess();
    return (nboServer != null);
  }

  /**
   * The interface for ALL communication with NBOServe from NBODialog.
   * 
   * @param request
   * 
   */
  protected void postToNBO(NBORequest request) {
    synchronized (lock) {
      postToQueue(request);
    }
  }
  
  /**
   * Post a request to 
   * @param j
   */
  private void postToQueue(NBORequest j) {
    if (isReady && requestQueue.isEmpty() && currentRequest == null) {
      currentRequest = j;
      requestQueue.add(currentRequest);
      startRequest(currentRequest);
    } else {
      requestQueue.add(j);
    }
  }

  /**
   * Take a quick look to see that gennbo.bat is present and notify the user if it is not.
   * 
   * @return true if gennbo.bat exists.
   * 
   */
  public boolean connect() {
    if (!doConnect)
      return true;
    File f = new File(getServerPath("gennbo.bat"));
    if (!f.exists()) {
      vwr.alert(f + " not found, make sure gennbo.bat is in same directory as "
          + exeName);
      return false;
    }
    return true;
  }

  /**
   * Clear the request queue
   * 
   */
  public void clearQueue() {
    requestQueue.clear();
  }

  /**
   * Check to see if there is a current request running
   * 
   * @return true if there is a current request
   * 
   */
  public boolean isWorking() {
    return (currentRequest != null);
  }  
  

  //// new ///////

  /**
   * Process the return from NBOServe.
   * 
   * @param s
   * @return  true if we are done
   */
  protected boolean processServerReturn(String s) {
    if (s.indexOf("FORTRAN STOP") >= 0) {
      nboDialog.alertError("NBOServe has stopped working - restarting");
      clearQueue();
      restart();
      return true;
    }

    if (isFortranError(s) || s.indexOf("missing or invalid") >= 0) {
      if (!s.contains("end of file")) {
        nboDialog.alertError(s);
      }
      clearQueue();
      restart();
      return true;
    }

    s = PT.rep(s, "\r", ""); // to standard Java format without carriage return.
    
    System.out.println("NBO reply:\n" + s);

    int pt;
    
    boolean removeRequest = true;

    try { // with finally clause to remove request from queue

      if (currentRequest == null) {
        //    *start*
        //    NBOServe v. 27-Jan-2017
        //    *end*
        //   NBOServe v6: development version (A_000000)
        // this was unsolicited.
        if (s.indexOf("NBOServe v") >= 0) {
          nboDialog.setLicense(s.substring(s.lastIndexOf("NBOServe v")));
        }
        return true;
      }

      if ((pt = s.indexOf("***errmess***")) >= 0) {
        try {
          s = PT.split(s, "\n")[2]; 
          logServerLine(s.substring(s.indexOf("\n") + 1), Logger.LEVEL_ERROR);
        } catch (Exception e) {
          // ignore
        }
        logServerLine("NBOPro can't do that.", Logger.LEVEL_WARN);
        return true;
      }

      if ((pt = s.indexOf("*start*")) < 0) {
        
        // Note that RUN can dump all kinds of things to SYSOUT prior to completion.
        
        logServerLine(s, (currentRequest.isRun ? Logger.LEVEL_DEBUG : Logger.LEVEL_ERROR));
        return (removeRequest = !currentRequest.isRun);
      }
      s = s.substring(pt + 8); // includes \n
      pt = s.indexOf("*end*");
      if (pt < 0) {
        System.out.println("bad start/end packet from NBOServe: " + s);
        return true;
      }

      // standard expectation

      currentRequest.sendReply(s.substring(0, pt));
      return true;
    } finally {
      if (currentRequest != null && removeRequest) {
        requestQueue.remove();
        currentRequest = null;
      }
    }
  }

}



