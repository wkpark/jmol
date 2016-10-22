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
package org.openscience.jmol.app.nbo;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.swing.DefaultComboBoxModel;
import javajs.util.PT;
import javajs.util.SB;

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
  static final int MODE_VALUE_M = 60;
  static final int MODE_GETRS = 61;
  static final int MODE_LABEL_BONDS = 62;
  protected int count;

  protected Viewer vwr;

  NBODialog nboDialog;

  protected Process nboServer;
  protected Thread nboListener;
  private InputStream stdout;
  protected BufferedReader nboReader;
  private PrintWriter stdinWriter;

  private SB sbRet;

  private boolean inData;
  protected boolean isWorking;
  String serverPath;
  String serverDir;


  private String nboModel;
  
  NBOJob currJob;
  Queue<NBOJob> jobQueue;
  protected Object lock;

  /**
   * Manage communication between Jmol and NBOServer
   * 
   * @param vwr
   *        The interacting display we are reproducing (source of view angle
   *        info etc)
   */
  public NBOService(Viewer vwr) {    
    this.vwr = vwr;
    sbRet = new SB();
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    setServerPath(props.getProperty("nboServerPath",
        System.getProperty("user.home") + "/NBOServe"));
    jobQueue = new ArrayDeque<NBOJob>();
    lock = new Object();
  }

  /**
   * Set path to NBOServe.exe
   * 
   * @param path
   */
  protected void setServerPath(String path) {
    serverPath = path;
    serverDir = new File(serverPath).getParent() + "/";
  }

  /**
   * @param job
   */
  protected void sendToNBO(NBOJob job) {
    String s = "<" + job.cmd + ">";
    currJob = job;
    vwr.writeTextFile(serverDir + "/" + job.cmd, job.sb.toString());

    nboDialog.statusLab.setText(job.statusInfo);
    sendCmd(s);
  }

  private void sendCmd(String s) {
    count = 1;
    System.out.println("sending " + s);
    if(stdinWriter == null)
      restart();
    stdinWriter.println(s);
    stdinWriter.flush();
  }

  protected void nboReport(String line) {
    if (Logger.debugging)
      Logger.debug(inData + " " + sbRet.length() + " "
          + "receiving: " + line);
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
      return;
    }
    if (!inData)
      nboDialog.addLine(NBODialog.DIALOG_MODEL, line);
    if (inData && sbRet != null) {
      sbRet.append(line + "\n");
      if (line.indexOf("END") >= 0) {
        inData = false;
        String m = "\"" + nboModel + "\"";
        nboModel = "\0";
        if (line.indexOf(m) >= 0) {
          String s = sbRet.toString();
          sbRet.setLength(0);
          if(s.contains("\\")) 
            s = s.replaceAll("\\\\", "");
          nboDialog.runScriptQueued(s);// + ";rotate best;select remove {*}; select on;");
          
        }
        return;
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
  
  
  protected static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();

    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }
  
  
  String startProcess(boolean sync, @SuppressWarnings("unused") final int mode) {
    //this.dialogMode = mode;
    try {
      System.out.println("starting NBO process sync=" + sync);
      File pathToExecutable = new File(serverPath);
      ProcessBuilder builder = new ProcessBuilder(serverPath);
      builder.directory(new File(pathToExecutable.getParent())); // this is where you set the root folder for the executable to run with
      builder.redirectErrorStream(true);
      nboServer = builder.start();
      stdout = nboServer.getInputStream();
      nboReader = new BufferedReader(new InputStreamReader(stdout));
      nboListener = null;
      nboListener = new Thread(new Runnable() {

        @Override
        public void run() {
          //boolean haveStart = false;
          boolean inOpener = false;
          boolean inRequest = false;
          System.out.println("nboListener " + this + " running");
          while (!Thread.currentThread().isInterrupted()) {
            String line = null;
            try {
              while ((line = nboReader.readLine()) != null) {

                Logger.info(line);
                // ignore the opener business
                if (line.indexOf("DATA \" \"") >= 0) {
                  Logger.info(" [NBO opener ignored]");
                  inOpener = true;
                  continue;
                }
                if (line.indexOf("END \"\"") >= 0) {
                  inOpener = false;
                  continue;
                }
                if (inOpener)
                  continue;
                if (line.indexOf("*start*") >= 0) {
                  if(currJob.dialogMode != MODE_LABEL)
                    inRequest = isWorking = true;
                  continue;
                }
                if (line.indexOf("Permission denied") >= 0||line.indexOf("PGFIO-F")>=0 || line.indexOf("Invalid command")>=0) {
                  if(!line.contains("end of file")){
                    nboDialog.vwr.alert(line);
                  isWorking = inRequest = false;
                  }
                  continue;
                }
                if (line.indexOf("missing or invalid") >= 0){
                  vwr.alert(line);
                  inRequest = isWorking = false;
                }
                if (line.indexOf("FORTRAN STOP") >= 0){
                  vwr.alert("NBOServe has stopped working");
                  restart();
                }
                if (line.indexOf("NBOServe") >= 0){
                  nboDialog.licenseInfo.setText(
                      "<html><div style='text-align: center'>" + line + "</html>");
                  isWorking = false;
                  continue;
                }
                if (line.indexOf("*end*") >= 0) {
                  synchronized(lock){                   
                    if (!isWorking) continue;
                    isWorking = false;
                    nboDialog.notifyList(currJob.list);
                    nboDialog.statusLab.setText("");
                    inRequest = false;
                    jobQueue.remove();
                    if(currJob.dialogMode == MODE_IMAGE){
                      String fname = 
                          nboDialog.fileHndlr.inputFile.getParent() + 
                          "\\" + nboDialog.fileHndlr.jobStem + ".bmp";
                      File f = new File(fname);
                      final SB title = new SB();
                      
                      String id = "id " + PT.esc(title.toString().trim());
                      String script = "image " + id + " close;image id \"\" "
                          + PT.esc(f.toString().replace('\\', '/'));
                      nboDialog.runScriptQueued(script);
                    }else if(currJob.dialogMode == MODE_RUN){
                      nboDialog.fileHndlr.setInputFile(nboDialog.fileHndlr.inputFile);
                    }

                    if(!jobQueue.isEmpty() && nboDialog.dialogMode != NBODialog.DIALOG_RUN)
                      sendToNBO(currJob = jobQueue.peek());
                    else currJob = null;
                    continue;
                  }
                }
                switch (currJob.dialogMode) {
                case MODE_VALUE:
                  if (isWorking && inRequest){
                    if(nboDialog.isOpenShell){
                      String spin;
                      if(nboDialog.alphaSpin.isSelected())
                        spin = "&uarr;";
                      else
                        spin = "&darr;";
                      int ind = line.indexOf(')')+1;
                      line = line.substring(0,ind) 
                           + spin  + line.substring(ind);
                    }
                    nboDialog.appendOutputWithCaret(" " + line, 'b');
                    if(line.contains("*"))
                      nboDialog.showMax(line);
                    inRequest = false;
                  }
                  break;
                case MODE_LIST:
                  if (isWorking && inRequest) {
                    currJob.list.addElement(line.trim());
                    
                  }
                  break;
                case MODE_LIST_MO:
                  if (isWorking && inRequest) {
                    String tmp = line.replace("MO ","");
                    tmp = tmp.replace(" ",".  ");
                    currJob.list.addElement("  " + tmp);
                  }
                  break;
                case MODE_SEARCH_SELECT:
                  if (line.startsWith(" Select")) {
                    isWorking = inRequest = false;
                  }
                  break;
                case MODE_IMAGE: 
                  if(line.contains("Missing valid")){
                    isWorking = inRequest = false;
                    vwr.alert(line);
                  }
                  break;
                case MODE_LABEL:
                  if(isWorking && inRequest){
                    double val = Double.parseDouble(line);
                    val = round(val,4);
                    nboDialog.runScriptQueued("select{*}[" + (count) + "];label " + val);
                    count++;
                  }
                  if(line.indexOf("END \"model") >= 0)
                    isWorking = inRequest = true;
                  break;
                case MODE_LABEL_BONDS:
                  if(line.indexOf("DATA")>=0)
                    inRequest = false;
                  if(line.indexOf("END")>=0){
                    inRequest = true;
                    continue;
                  }if(isWorking && inRequest){
                    String[] toks = line.split(" ");
                    float order = Float.parseFloat(toks[2]);
                    if(order > 0.01){
                      int at1 = Integer.parseInt(toks[0]) - 1;
                      int at2 = Integer.parseInt(toks[1]) - 1;
                      float x = (vwr.ms.at[at1].x - vwr.ms.at[at2].x)/2;
                      float y = (vwr.ms.at[at1].y - vwr.ms.at[at2].y)/2;
                      float z = (vwr.ms.at[at1].z - vwr.ms.at[at2].z)/2;
                      
                      nboDialog.runScriptQueued("select (atomno = " + at1 + ");" +
                      		"label \"" + toks[2] + "\";set labeloffset {" +
                          x + "," + y + "," + (z) +"}");
                    }
                  }
                  break;
                case MODE_MODEL:
                  if(line.indexOf("can't do that")>=0){
                    nboDialog.addLine(NBODialog.DIALOG_MODEL,line);
                    isWorking = inRequest = false;
                    break;
                  }
                  nboReport(line);
                  break;
                case MODE_VALUE_M:
                  if(line.indexOf("DATA")>=0)
                    isWorking = inRequest = false;
                  if(inRequest && isWorking)
                    nboDialog.addLine(NBODialog.DIALOG_MODEL,line);
                  break;

                case MODE_GETRS:
                  if(inRequest && isWorking){
                    int cnt = Integer.parseInt(line.trim());
                    for(int i = 1; i <= cnt; i++)
                      currJob.list.addElement("R.S. " + i);
                  }
                  break;
                case MODE_RAW:
                case MODE_RUN:
                  break;
                default:
                  nboReport(line);
                  break;
                }
              }
            
            } catch (Throwable e1) {
              continue;
              // includes thread death
            }
          }
        }
      });
      nboListener.setName("NBOServiceThread" + System.currentTimeMillis());
      nboListener.start();
      stdinWriter = new PrintWriter(nboServer.getOutputStream());
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return e.getMessage();
    }
    return null;
  }

  public void closeProcess() {
    isWorking = false;
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
   * @param cmd
   * @param data
   * @param dialogMode 
   * @param list 
   * @param status 
   */
  protected void rawCmdNew(String cmd, SB data, int dialogMode,
                           DefaultComboBoxModel<String> list, String status) {
      if(dialogMode == MODE_LABEL){
        nboDialog.runScriptNow("mo delete; nbo delete");
      }
      String fname = null;
      synchronized(lock){
        if (data == null) {
          Logger.info("issuing\n" + cmd);
        } else {
          fname =  cmd + "_cmd.txt";
          Logger.info("issuing " + fname + "\n" + data);
          cmd = "<" + fname + ">";
          isWorking = true;
        }
        if(jobQueue.isEmpty()){
          currJob = new NBOJob(fname,data, status, list);
          jobQueue.add(currJob);
          currJob.dialogMode = dialogMode;
          sendToNBO(currJob);
        }
        else{ 
          NBOJob j = new NBOJob(fname,data, status, list);
          j.dialogMode = dialogMode;
          jobQueue.add(j);
        }
        }
  }

}
class NBOJob {
  String cmd;
  String statusInfo;
  DefaultComboBoxModel<String> list;
  SB sb;
  protected int dialogMode;
  
  NBOJob(String cmd, SB sb, String statusInfo, DefaultComboBoxModel<String> list) {
    this.cmd = cmd;
    this.sb = sb;
    this.list = list;
    this.statusInfo = statusInfo;
  }
}
