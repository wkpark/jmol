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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Queue;

import javajs.util.AU;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import javax.swing.AbstractListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

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
  static final int MODE_MODEL = 1; // don't change this number -- it is used for LOAD NBO 
  static final int MODE_RUN = 20;

  // these are for rawCmdNew only
  static final int MODE_VALUE = 45;
  static final int MODE_LIST = 46;
  static final int MODE_LIST_MO = 47;
  static final int MODE_IMAGE = 88;
  static final int MODE_SEARCH_SELECT = 90;
  static final int MODE_LABEL = 50;
  //static final int MODE_VALUE_M = 60;
  static final int MODE_GETRS = 61;
  static final int MODE_LABEL_BONDS = 62;

  protected int count;
  protected boolean isWorking;

  protected Viewer vwr;
  protected Process nboServer;
  protected Thread nboListener;
  protected BufferedReader nboReader;
  protected NBODialog nboDialog;
  protected NBOJob currJob;
  protected Object lock;
  protected Queue<NBOJob> jobQueue;  
  
  private PrintWriter stdinWriter;
  protected BufferedInputStream stdout;

  private SB sbRet;
  private boolean inData;
  private boolean cantStartServer;  
  private String serverPath;
  private String nboModel;

  private String exeName = "NBOServe.exe";
  private boolean doConnect;
  
  protected boolean isReady;

  /**
   * Manage communication between Jmol and NBOServer
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
    sbRet = new SB();
    setServerPath(getNBOProperty("serverPath", System.getProperty("user.home")
        + "/NBOServe"));
    jobQueue = new ArrayDeque<NBOJob>();
    lock = new Object();
  }

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
   * Set path to NBOServe diretory
   * 
   * @param path
   */
  protected void setServerPath(String path) {
    serverPath = NBOFileHandler.fixPath(path);
    setNBOProperty("serverPath", path);
  }

  protected boolean isEnabled() {
    return serverPath != null;
  }

  protected String getNBOProperty(String name, String defaultValue) {
    return JmolPanel.getPluginOption("NBO", name, defaultValue);
  }

  protected void setNBOProperty(String name, String option) {
    option = PT.rep(option, "\\", "/");
    JmolPanel.setPluginOption("NBO", name, option);
  }

  /**
   * @param job
   */
  protected void startJob(NBOJob job) {
    if (job == null)
      return;
    System.out.println("NBOServer ready?" + isReady);
    String s = "<" + job.cmd + ">";
    currJob = job;
    nboDialog.inputFileHandler.writeToFile(getServerPath(job.cmd),
        job.sb.toString());
    nboDialog.setStatus(job.statusInfo);// + " (sending job.cmd)");
    sendCmd(s, job.sb.toString());
  }
 
  private void sendCmd(String s, String script) {
    count = 1;
    System.out.println("sending " + s + "\n" + script);
    if (stdinWriter == null)
      restart();
    stdinWriter.println(s);
    stdinWriter.flush();
  }

  protected void nboAddModelLine(String line) {
    if (Logger.debugging)
      Logger.debug(inData + " " + sbRet.length() + " " + "receiving: " + line);
    if (line.startsWith("DATA \" \"")) {
      isWorking = false;
    } else if (line.startsWith("DATA ")) {
      if (line.startsWith("DATA \"model")) {
        sbRet.setLength(0);
        line = fixNBOModel(line);
      }
      inData = (line.indexOf("exit") < 0);
      if (inData)
        sbRet.append(line + "\n");
    } else if (!inData) {
      nboDialog.processModelLine(line);
    } else if (sbRet != null) {
      sbRet.append(line + "\n");
      if (line.indexOf("END") >= 0) {
        inData = false;
        if (line.indexOf("\"" + nboModel + "\"") >= 0)
          nboDialog.processModelEnd(sbRet.toString(), currJob.statusInfo);
        sbRet.setLength(0);
        nboModel = "\0";
      }
    }
  }

  /**
   * fixes DATA line to include a title
   * 
   * @param line
   * @return line or full data block
   */
  private String fixNBOModel(String line) {
    nboModel = PT.getQuotedStringAt(line, 0);
    String s = " NBO " + nboModel;
    int pt = line.indexOf("\n");
    return (pt < 0 ? line + s : line.substring(0, pt) + s + line.substring(pt));
  }

  String startProcess(boolean sync, @SuppressWarnings("unused") final int mode) {
    System.out.println("startProcess");
    try {
      cantStartServer = true;
      if (!doConnect)
        return null;
      nboListener = null;
      System.out.println("starting NBO process sync=" + sync);
      String path = getServerPath(exeName);
      ProcessBuilder builder = new ProcessBuilder(path);
      builder.directory(new File(new File(path).getParent())); // root folder for executable 
      builder.redirectErrorStream(true);
      nboServer = builder.start();
      stdout = (BufferedInputStream) nboServer.getInputStream();
      nboReader = new BufferedReader(new InputStreamReader(stdout));
      nboListener = new Thread(new Runnable() {

        @Override
        public void run() {
          //boolean haveStart = false;
          boolean inData = false;
          boolean inError = false;
          byte[] buffer = new byte[1024];

          System.out.println("nboListener " + this + " running");
          while (!Thread.currentThread().isInterrupted()) {
            String line = null;
            try {
              int n = stdout.available();
              if (n <= 0)
                continue;
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
              System.out.println(s);
              isReady = true;
              BufferedReader rdr = Rdr.getBR(s);
              while ((line = rdr.readLine()) != null) {
                // ignore the opener business
                if (line.indexOf("DATA \" \"") >= 0) {
                  //nboDialog.logInfo(" DATA...", Logger.LEVEL_INFO);
                  inData = true;
                  continue;
                }
                if (line.indexOf("END \"\"") >= 0) {
                  inData = false;
                  continue;
                }
                if (inData)
                  continue;

                
                if (line.indexOf("***errmess***") >= 0) {
                  //                  logServerLine(line, Logger.LEVEL_ERROR);
                  inError = true;
                  continue;
                }
                logServerLine(line, inError ? Logger.LEVEL_ERROR : Logger.LEVEL_DEBUG);
                if (inError) {
                  // second line of <space>***errmess*** is the message.
                  logServerLine("NBOPro can't do that.", Logger.LEVEL_WARN);
                  //isWorking = inRequest = false;
                  inError = false;
                  continue;
                }
                if (line.indexOf("*start*") >= 0) {
                  if (currJob.dialogMode != MODE_LABEL)
                    isWorking = true;
                  continue;
                }
                if (line.indexOf("*end*") >= 0) {
                  synchronized (lock) {
                    isWorking = false;
                    processJobAndGetNext();
                    break;
                  }
                }
                if (isFortranError(line)) {
                  if (!line.contains("end of file")) {
                    nboDialog.alertError(line);
                  }
                  isWorking = false;
                  clearQueue();
                  break;
                }
                if (line.indexOf("missing or invalid") >= 0) {
                  nboDialog.alertError(line);
                  isWorking = false;
                  clearQueue();
                  break;
                }
                if (line.indexOf("FORTRAN STOP") >= 0) {
                  nboDialog.alertError("NBOServe has stopped working - restarting");
                  clearQueue();
                  restart();
                  continue;
                }
                //
                if (line.indexOf("NBOServe v") >= 0) {
                  nboDialog.setLicense(line);
                  continue;
                }
                switch (currJob.dialogMode) {
                case MODE_VALUE:
                  if (isWorking) {
                    nboDialog.processValue(line);
                    isWorking = false;
                  }
                  break;
                case MODE_LIST:
                  if (isWorking) {
                    currJob.addListElement(line.trim());
                  }
                  break;
                case MODE_LIST_MO:
                  if (isWorking) {
                    String tmp = line.replace("MO ", "");
                    tmp = tmp.replace(" ", ".  ");
                    currJob.addListElement("  " + tmp);
                  }
                  break;
                case MODE_SEARCH_SELECT:
                  if (line.startsWith(" Select")) {
                    isWorking = false;
                  }
                  break;
                case MODE_IMAGE:
                  if (line.contains("Missing valid")) {
                    isWorking = false;
                    nboDialog.alertError(line);
                  }
                  break;
                case MODE_LABEL:
                  if (isWorking)
                    nboDialog.processLabel(line, count++);
                  if (line.indexOf("END \"model") >= 0)
                    isWorking = true;
                  break;
                case MODE_LABEL_BONDS:
                  if (line.indexOf("DATA") >= 0)
                    isWorking = false;
                  if (line.indexOf("END") >= 0) {
                    isWorking = true; // ???
                    continue;
                  }
                  if (isWorking)
                    nboDialog.processLabelBonds(line);
                  break;
                case MODE_MODEL:
                  nboAddModelLine(line);
                  break;
                //                case MODE_VALUE_M:
                //                  if (line.indexOf("DATA") >= 0)
                //                    isWorking = inRequest = false;
                //                  if (inRequest && isWorking)
                //                    nboDialog.processModelLine(line);
                //                  break;
                //
                case MODE_GETRS:
                  if (isWorking) {
                    int cnt = Integer.parseInt(line.trim());
                    for (int i = 1; i <= cnt; i++)
                      currJob.addListElement("R.S. " + i);
                  }
                  break;
                case MODE_RAW:
                case MODE_RUN:
                  break;
                default:
                  nboAddModelLine(line);
                  break;
                }
              }

            } catch (Throwable e1) {
              continue;
              // includes thread death
            }
            startJob(currJob = jobQueue.peek());
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

  private int lineNo = 0;

  protected void logServerLine(String line, int level) {
    if (isFortranError(line))
      level = Logger.LEVEL_ERROR;
    nboDialog.logInfo((nboDialog.debugVerbose ? (++lineNo) + "< " : "") + line,
      level);
  }

  protected boolean isFortranError(String line) {
    return line.indexOf("Permission denied") >= 0
        || line.indexOf("PGFIO-F") >= 0 
        || line.indexOf("Invalid command") >= 0;
  }

  public void closeProcess() {
    isWorking = false;
    isReady = false;
    stdout = null;
    try {
      stdinWriter.close();
    } catch (Exception e) {
    }
    stdinWriter = null;
    try {
      nboReader.close();
    } catch (Exception e) {
    }
    nboReader = null;
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

  String restart() {
    closeProcess();
    return startProcess(false, MODE_RAW);
  }

  public boolean restartIfNecessary() {
    if (nboServer == null)
      startProcess(false, MODE_RAW);
    return (nboServer != null);
  }

  /**
   * The interface for ALL communication with NBOServe from NBODialog.
   * 
   * @param cmdFileRoot
   * @param script
   * @param dialogMode
   * @param list
   * @param statusMessage
   */
  protected void postToNBO(String cmdFileRoot, SB script, int dialogMode,
                           AbstractListModel<String> list, String statusMessage) {
    String fname = null;
    
    synchronized (lock) {
      if (script == null) {
        nboDialog.logInfo("> " + cmdFileRoot, Logger.LEVEL_DEBUG);
      } else {
        fname = cmdFileRoot + "_cmd.txt";
        nboDialog.logInfo("> " + fname + "\n" + script, Logger.LEVEL_DEBUG);
        cmdFileRoot = "<" + fname + ">";
        isWorking = true;
      }
      postToQueue(new NBOJob(fname, script, statusMessage, dialogMode, list));
    }
  }

  private void postToQueue(NBOJob j) {
    if (isReady && jobQueue.isEmpty() && currJob == null) {
      currJob = j;
      jobQueue.add(currJob);
      startJob(currJob);
    } else {
      jobQueue.add(j);
    }
  }

  protected void processJobAndGetNext() {
    jobQueue.remove();
    nboDialog.processEnd(currJob.dialogMode, currJob.list);
    if (jobQueue.isEmpty()) {// || nboDialog.dialogMode == NBODialogConfig.DIALOG_RUN) {
      currJob = null;
    } else {
      System.out.println(">>jobQueue.size() = " + jobQueue.size());
    }
  }

  class NBOJob {
    String cmd;
    String statusInfo;
    AbstractListModel<String> list;
    SB sb;
    NBODialog dialog;
    protected int dialogMode;
    private boolean isComboBoxList;

    NBOJob(String cmd, SB sb, String statusInfo,
        int dialogMode, AbstractListModel<String> list) {
      this.cmd = cmd;
      this.sb = sb;
      this.list = list;
      this.isComboBoxList = (list instanceof DefaultComboBoxModel);
      this.statusInfo = statusInfo;
      this.dialogMode = dialogMode;
    }

    public void addListElement(String s) {
      if (isComboBoxList) {
        ((DefaultComboBoxModel<String>) list).addElement(s);
      } else {
        ((DefaultListModel<String>) list).addElement(s);
      }
    }
  }

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

  public void clearQueue() {
    jobQueue.clear();
  }

}
