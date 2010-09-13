/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-25 02:42:30 -0500 (Thu, 25 Jun 2009) $
 * $Revision: 11113 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, www.jmol.org
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
package org.jmol.console;


import org.jmol.api.*;
import org.jmol.i18n.GT;
import org.jmol.script.ScriptCompiler;
import org.jmol.script.Token;
import org.jmol.util.ArrayUtil;
import org.jmol.util.TextFormat;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

import java.awt.Component;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.*;

public abstract class JmolConsole implements JmolCallbackListener, ActionListener, WindowListener {

  public JmolViewer viewer;
  protected Component display;
  public JmolConsoleDialog jcd;
  protected JButton editButton, runButton, historyButton, stateButton;
  protected Map<String, String> labels;
  
  abstract protected Map<String, String> setupLabels();
  

  protected String getLabel(String key) {
    if (labels == null) {
      labels = setupLabels();
    }
    return labels.get(key);
  }

  public void setVisible(boolean isVisible) {
    if (jcd != null)
      jcd.setVisible(isVisible);
  }

  static {
    System.out.println("JmolConsole is initializing");
  }
  // common:
  
  protected ScriptEditor scriptEditor;
  
  void setScriptEditor(ScriptEditor se) {
    scriptEditor = se;
  }
  
  public JmolScriptEditorInterface getScriptEditor() {
    return (scriptEditor == null ? 
        (scriptEditor = new ScriptEditor(viewer, display instanceof JFrame ? (JFrame) display : null, this))
        : scriptEditor);
  }
  
  JmolViewer getViewer() {
    return viewer;
  }

  //public void finalize() {
  //  System.out.println("Console " + this + " finalize");
  //}

  public JmolConsole() {
  }
  
  public JmolConsole(JmolViewer viewer, JFrame frame, boolean createDialog) {
    jcd = (createDialog ? new JmolConsoleDialog(this, frame) : null);
    this.viewer = viewer;
    display = frame;
  }

  abstract protected void clearContent(String text);
  abstract protected void execute(String strCommand);
  
  public int nTab = 0;
  private String incompleteCmd;
  
  protected static String getTitleText() {
    return GT._("Jmol Script Console") + " " + Viewer.getJmolVersion();
  }
  
  protected String completeCommand(String thisCmd) {
    if (thisCmd.length() == 0)
      return null;
    String strCommand = (nTab <= 0 || incompleteCmd == null ? thisCmd
        : incompleteCmd);
    incompleteCmd = strCommand;
    String[] splitCmd = ScriptCompiler.splitCommandLine(thisCmd);
    if (splitCmd == null)
      return null;
    boolean asCommand = splitCmd[2] == null;
    String notThis = splitCmd[asCommand ? 1 : 2];
    if (notThis.length() == 0)
      return null;
    splitCmd = ScriptCompiler.splitCommandLine(strCommand);
    String cmd = null;
    if (!asCommand && (notThis.charAt(0) == '"' || notThis.charAt(0) == '\'')) {
      char q = notThis.charAt(0);
      notThis = TextFormat.trim(notThis, "\"\'");
      String stub = TextFormat.trim(splitCmd[2], "\"\'");
      cmd = nextFileName(stub, nTab);
      if (cmd != null)
        cmd = splitCmd[0] + splitCmd[1] + q + (cmd == null ? notThis : cmd) + q;
    } else {
      if (!asCommand)
        notThis = splitCmd[1];
      cmd = Token.completeCommand(null, splitCmd[1].equalsIgnoreCase("set "), asCommand, asCommand ? splitCmd[1]
          : splitCmd[2], nTab);
      cmd = splitCmd[0]
          + (cmd == null ? notThis : asCommand ? cmd : splitCmd[1] + cmd);
    }
    return (cmd == null || cmd.equals(strCommand) ? null : cmd);
  }

  private String nextFileName(String stub, int nTab) {
    String sname = FileManager.getLocalPathForWritingFile(viewer, stub);
    String root = sname.substring(0, sname.lastIndexOf("/") + 1);
    if (sname.startsWith("file:/"))
      sname = sname.substring(6);
    if (sname.indexOf("/") >= 0) {
      if (root.equals(sname)) {
        stub = "";
      } else {
        File dir = new File(sname);
        sname = dir.getParent();
        stub = dir.getName();
      }
    }
    FileChecker fileChecker = new FileChecker(stub);
    try {
      (new File(sname)).list(fileChecker);
      return root + fileChecker.getFile(nTab);
    } catch (Exception e) {
      //
    }
    return null;
  }

  protected class FileChecker implements FilenameFilter {
    private String stub;
    private List<String> v = new ArrayList<String>();
    
    protected FileChecker(String stub) {
      this.stub = stub.toLowerCase();
    }

    public boolean accept(File dir, String name) {
      name = name.toLowerCase();
      if (!name.toLowerCase().startsWith(stub))
        return false;
      v.add(name); 
      return true;
    }
    
    protected String getFile(int n) {
      return ArrayUtil.sortedItem(v, n);
    }
  }
  
  protected void setEnabled(JButton button, boolean TF) {
    if (button != null)
      button.setEnabled(TF);
  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == runButton) {
      execute(null);
    } else if (source == editButton) {
      viewer.getProperty("DATA_API","scriptEditor", null);
    } else if (source == historyButton) {
      clearContent(viewer.getSetHistory(Integer.MAX_VALUE));
    } else if (source == stateButton) {
      clearContent(viewer.getStateInfo());
      // problem here is that in some browsers, you cannot clip from
      // the editor.
      //viewer.getProperty("DATA_API","scriptEditor", new String[] { "current state" , viewer.getStateInfo() });
    }
  }

  ////////////////////////////////////////////////////////////////
  // window listener stuff to close when the window closes
  ////////////////////////////////////////////////////////////////

  /**
   * @param we 
   * 
   */
  public void windowActivated(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  public void windowClosed(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  public void windowClosing(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  public void windowDeactivated(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  public void windowDeiconified(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  public void windowIconified(WindowEvent we) {
  }

  /**
   * @param we 
   * 
   */
  public void windowOpened(WindowEvent we) {
  }

  ///////////// JmolCallbackListener interface

  // Allowing for just the callbacks needed to provide status feedback to the console.
  // For applications that embed Jmol, see the example application Integration.java.

  public boolean notifyEnabled(int type) {
    // See org.openscience.jmol.app.jmolpanel.StatusListener.java for a complete list
    switch (type) {
    case JmolConstants.CALLBACK_ECHO:
    case JmolConstants.CALLBACK_MEASURE:
    case JmolConstants.CALLBACK_MESSAGE:
    case JmolConstants.CALLBACK_PICK:
      return true;
    }
    return false;
  }

  abstract public void sendConsoleMessage(String info);
  abstract public void sendConsoleEcho(String info);
  
  public void notifyCallback(int type, Object[] data) {
    String strInfo = (data == null || data[1] == null ? null : data[1]
        .toString());
    switch (type) {
    case JmolConstants.CALLBACK_ECHO:
      sendConsoleEcho(strInfo);
      break;
    case JmolConstants.CALLBACK_MEASURE:
      String mystatus = (String) data[3];
      if (mystatus.indexOf("Picked") >= 0) // picking mode
        sendConsoleMessage(strInfo);
      else if (mystatus.indexOf("Completed") >= 0)
        sendConsoleEcho(strInfo.substring(strInfo.lastIndexOf(",") + 2, strInfo
            .length() - 1));
      break;
    case JmolConstants.CALLBACK_MESSAGE:
      sendConsoleMessage(data == null ? null : strInfo);
      break;
    case JmolConstants.CALLBACK_PICK:
      sendConsoleMessage(strInfo);
      break;
    }
  }

  public void setCallbackFunction(String callbackType, String callbackFunction) {
    // application-dependent option
  }


}
