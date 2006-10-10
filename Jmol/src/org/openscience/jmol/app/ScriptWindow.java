/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package org.openscience.jmol.app;

import org.jmol.api.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Component;
import java.awt.event.*;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.Position;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.JScrollPane;
import java.util.Vector;

import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.util.CommandHistory;

public final class ScriptWindow extends JDialog
    implements ActionListener, EnterListener{
  
  private ConsoleTextPane console;
  private JButton closeButton;
  private JButton runButton;
  private JButton haltButton;
  private JButton clearButton;
  private JButton historyButton;
  private JButton helpButton;
  JmolViewer viewer;
  
  public ScriptWindow(JmolViewer viewer, JFrame frame) {
    super(frame, GT._("Jmol Script Console"), false);
    this.viewer = viewer;
    layoutWindow(getContentPane());
    setSize(500, 400);
    setLocationRelativeTo(frame);
  }

  void layoutWindow(Container container) {
    container.setLayout(new BorderLayout());

    console = new ConsoleTextPane(this);
    
    
    console.setPrompt();
    container.add(new JScrollPane(console), BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    container.add(buttonPanel, BorderLayout.SOUTH);

    runButton = new JButton(GT._("Run"));
    runButton.addActionListener(this);
    buttonPanel.add(runButton);

    haltButton = new JButton(GT._("Halt"));
    haltButton.addActionListener(this);
    buttonPanel.add(haltButton);
    haltButton.setEnabled(false);

    clearButton = new JButton(GT._("Clear"));
    clearButton.addActionListener(this);
    buttonPanel.add(clearButton);

    historyButton = new JButton(GT._("History"));
    historyButton.addActionListener(this);
    buttonPanel.add(historyButton);

    helpButton = new JButton(GT._("Help"));
    helpButton.addActionListener(this);
    buttonPanel.add(helpButton);

    closeButton = new JButton(GT._("Close"));
    closeButton.addActionListener(this);
    buttonPanel.add(closeButton);

  }

  public void sendConsoleEcho(String strEcho) {
    if (strEcho != null && !isError) {
      console.outputEcho(strEcho);
    }
    setError(false);
  }

  boolean isError = false;
  void setError(boolean TF) {
    isError = TF;
    //if (isError)
      //console.recallCommand(true);
  }
  
  public void sendConsoleMessage(String strStatus) {
    if (strStatus == null) {
      console.clearContent();
      console.outputStatus("");
    } else if (strStatus.indexOf("ERROR:") >= 0) {
      console.outputError(strStatus);
      isError = true;
    } else if (!isError) {
      console.outputStatus(strStatus);
    }
  }

  public void notifyScriptTermination(String strMsg, int msWalltime) {
    if (strMsg != null && strMsg.indexOf("ERROR") >= 0) {
      console.outputError(strMsg);
    }
    runButton.setEnabled(true);
    haltButton.setEnabled(false);
  }

  public void enterPressed() {
    runButton.doClick(100);
    //    executeCommand();
  }

  
  class ExecuteCommandThread extends Thread {

    String strCommand;
    ExecuteCommandThread (String command) {
      strCommand = command;
    }
    
    public void run() {
      try {
        executeCommand(strCommand);
      } catch (Exception ie) {
        Logger.debug("execution command interrupted!"+ie);
      }
    }
  }
   
  ExecuteCommandThread execThread;
  void executeCommandAsThread(){ 
    String strCommand = console.getCommandString().trim();
    if (strCommand.length() > 0) {
      execThread = new ExecuteCommandThread(strCommand);
      execThread.start();
    }
  }
  
  void executeCommand(String strCommand) {
    boolean doWait;
    setError(false);
    console.appendNewline();
    console.setPrompt();
    if (strCommand.length() > 0) {
      String strErrorMessage = null;
      doWait = (strCommand.indexOf("WAIT ") == 0);
      if (doWait) { //for testing, mainly
        // demonstrates using the statusManager system.
        runButton.setEnabled(false);
        haltButton.setEnabled(true);

        Vector info = (Vector) viewer
            .scriptWaitStatus(strCommand.substring(5),
                "+fileLoaded,+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated");
        runButton.setEnabled(true);
        haltButton.setEnabled(false);
        /*
         * info = [ statusRecortSet0, statusRecortSet1, statusRecortSet2, ...]
         * statusRecordSet = [ statusRecord0, statusRecord1, statusRecord2, ...]
         * statusRecord = [int msgPtr, String statusName, int intInfo, String msg]    
         */
        for (int i = 0; i < info.size(); i++) {
          Vector statusRecordSet = (Vector) info.get(i);
          for (int j = 0; j < statusRecordSet.size(); j++) {
            Vector statusRecord = (Vector) statusRecordSet.get(j);
            Logger.info("msg#=" + statusRecord.get(0) + " "
                + statusRecord.get(1) + " intInfo=" + statusRecord.get(2)
                + " stringInfo=" + statusRecord.get(3));
          }
        }
        console.appendNewline();
      } else {
        boolean isScriptExecuting = viewer.isScriptExecuting();
        if (viewer.checkHalt(strCommand))
          strErrorMessage = (isScriptExecuting ? "string execution halted with " + strCommand : "no script was executing");
        else
          strErrorMessage = "";//viewer.scriptCheck(strCommand);
        //the problem is that scriptCheck is synchronized, so these might get backed up. 
        if (strErrorMessage != null && strErrorMessage.length() > 0) {
          console.outputError(strErrorMessage);
        } else {
          //runButton.setEnabled(false);
          haltButton.setEnabled(true);
          viewer.script(strCommand);
        }
      }
    }
    console.grabFocus();
  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == closeButton) {
      hide();
    } else if (source == runButton) {
      executeCommandAsThread();
    } else if (source == clearButton) {
      console.clearContent();
    } else if (source == historyButton) {
      console.clearContent(viewer.getSetHistory(Integer.MAX_VALUE));
    } else if (source == haltButton) {
      viewer.haltScriptExecution();
    } else if (source == helpButton) {
        URL url = this.getClass().getClassLoader()
            .getResource("org/openscience/jmol/Data/guide/ch04.html");
        HelpDialog hd = new HelpDialog(null, url);
        hd.show();
    }
    console.grabFocus(); // always grab the focus (e.g., after clear)
  }
}

class ConsoleTextPane extends JTextPane {

  ConsoleDocument consoleDoc;
  EnterListener enterListener;
  JmolViewer viewer;
  
  ConsoleTextPane(ScriptWindow scriptWindow) {
    super(new ConsoleDocument());
    consoleDoc = (ConsoleDocument)getDocument();
    consoleDoc.setConsoleTextPane(this);
    this.enterListener = (EnterListener) scriptWindow;
    this.viewer = scriptWindow.viewer;
  }

  public String getCommandString() {
    String cmd = consoleDoc.getCommandString();
    return cmd;
  }

  public void setPrompt() {
    consoleDoc.setPrompt();
  }

  public void appendNewline() {
    consoleDoc.appendNewline();
  }

  public void outputError(String strError) {
    consoleDoc.outputError(strError);
  }

  public void outputErrorForeground(String strError) {
    consoleDoc.outputErrorForeground(strError);
  }

  public void outputEcho(String strEcho) {
    consoleDoc.outputEcho(strEcho);
  }

  public void outputStatus(String strStatus) {
    consoleDoc.outputStatus(strStatus);
  }

  public void enterPressed() {
    if (enterListener != null)
      enterListener.enterPressed();
  }
  
  public void clearContent() {
    clearContent(null);
  }
  public void clearContent(String text) {
    consoleDoc.clearContent();
    if (text != null)
      consoleDoc.outputEcho(text);  
    setPrompt();
  }
  
   /* (non-Javadoc)
    * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
    */
    
   /**
    * Custom key event processing for command history implementation.
    * 
    * Captures key up and key down strokes to call command history
    * and redefines the same events with control down to allow
    * caret vertical shift.
    * 
    * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
    */
   protected void processKeyEvent(KeyEvent ke)
   {
      // Id Control key is down, captures events does command
      // history recall and inhibits caret vertical shift.
      if (ke.getKeyCode() == KeyEvent.VK_UP
         && ke.getID() == KeyEvent.KEY_PRESSED
         && !ke.isControlDown())
      {
         recallCommand(true);
      }
      else if (
         ke.getKeyCode() == KeyEvent.VK_DOWN
            && ke.getID() == KeyEvent.KEY_PRESSED
            && !ke.isControlDown())
      {
         recallCommand(false);
      }
      // If Control key is down, redefines the event as if it 
      // where a key up or key down stroke without modifiers.  
      // This allows to move the caret up and down
      // with no command history recall.
      else if (
         (ke.getKeyCode() == KeyEvent.VK_DOWN
            || ke.getKeyCode() == KeyEvent.VK_UP)
            && ke.getID() == KeyEvent.KEY_PRESSED
            && ke.isControlDown())
      {
         super
            .processKeyEvent(new KeyEvent(
               (Component) ke.getSource(),
               ke.getID(),
               ke.getWhen(),
               0,         // No modifiers
               ke.getKeyCode(), 
               ke.getKeyChar(), 
               ke.getKeyLocation()));
      }
      // Standard processing for other events.
      else
      {
         super.processKeyEvent(ke);
      }
   }

   /**
   * Recall command history.
   * 
   * @param up - history up or down
   */
   void recallCommand(boolean up) {
     String cmd = viewer.getSetHistory(up ? -1 : 1);
    if (cmd == null) {
      String str = getText();
      if (str.lastIndexOf("$") != str.length() - 2) {
        appendNewline();
        setPrompt();
      }
      return;
    }
    try {
      if (cmd.endsWith(CommandHistory.ERROR_FLAG)) {
        cmd = cmd.substring(0, cmd.indexOf(CommandHistory.ERROR_FLAG));
        consoleDoc.replaceCommand(cmd, true);
      } else {
        consoleDoc.replaceCommand(cmd, false);
      }
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }  
}

class ConsoleDocument extends DefaultStyledDocument {

  ConsoleTextPane consoleTextPane;

  SimpleAttributeSet attError;
  SimpleAttributeSet attEcho;
  SimpleAttributeSet attPrompt;
  SimpleAttributeSet attUserInput;
  SimpleAttributeSet attStatus;

  ConsoleDocument() {
    super();

    attError = new SimpleAttributeSet();
    StyleConstants.setForeground(attError, Color.red);

    attPrompt = new SimpleAttributeSet();
    StyleConstants.setForeground(attPrompt, Color.magenta);

    attUserInput = new SimpleAttributeSet();
    StyleConstants.setForeground(attUserInput, Color.black);

    attEcho = new SimpleAttributeSet();
    StyleConstants.setForeground(attEcho, Color.blue);
    StyleConstants.setBold(attEcho, true);

    attStatus = new SimpleAttributeSet();
    StyleConstants.setForeground(attStatus, Color.black);
    StyleConstants.setItalic(attStatus, true);
  }

  void setConsoleTextPane(ConsoleTextPane consoleTextPane) {
    this.consoleTextPane = consoleTextPane;
  }

  Position positionBeforePrompt; // starts at 0, so first time isn't tracked (at least on Mac OS X)
  Position positionAfterPrompt;  // immediately after $, so this will track
  int offsetAfterPrompt;         // only still needed for the insertString override and replaceCommand

  /** 
   * Removes all content of the script window, and add a new prompt.
   */
  void clearContent() {
      try {
          super.remove(0, getLength());
      } catch (BadLocationException exception) {
          System.out.println("Could not clear script window content: " + exception.getMessage());
      }
  }
  
  void setPrompt() {
    try {
      super.insertString(getLength(), "$ ", attPrompt);
      offsetAfterPrompt = getLength();
      positionBeforePrompt = createPosition(offsetAfterPrompt - 2);
       // after prompt should be immediately after $ otherwise tracks the end
       // of the line (and no command will be found) at least on Mac OS X it did.
      positionAfterPrompt = createPosition(offsetAfterPrompt-1);
      consoleTextPane.setCaretPosition(offsetAfterPrompt);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  void setNoPrompt() {
    try {
      offsetAfterPrompt = getLength();
      positionAfterPrompt = positionBeforePrompt = createPosition(offsetAfterPrompt);
      consoleTextPane.setCaretPosition(offsetAfterPrompt);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  // it looks like the positionBeforePrompt does not track when it started out as 0
  // and a insertString at location 0 occurs. It may be better to track the
  // position after the prompt in stead
  void outputBeforePrompt(String str, SimpleAttributeSet attribute) {
    try {
      Position caretPosition = createPosition(consoleTextPane.getCaretPosition());
      super.insertString(positionBeforePrompt.getOffset(), str+"\n", attribute);
      // keep the offsetAfterPrompt in sync
      offsetAfterPrompt = positionBeforePrompt.getOffset() + 2;
      consoleTextPane.setCaretPosition(caretPosition.getOffset());
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  void outputError(String strError) {
    outputBeforePrompt(strError, attError);
  }

  void outputErrorForeground(String strError) {
    try {
      super.insertString(getLength(), strError+"\n", attError);
      consoleTextPane.setCaretPosition(getLength());
    } catch (BadLocationException e) {
      e.printStackTrace();

    }
  }

  void outputEcho(String strEcho) {
    outputBeforePrompt(strEcho, attEcho);
  }

  void outputStatus(String strStatus) {
    outputBeforePrompt(strStatus, attStatus);
  }

  void appendNewline() {
    try {
      super.insertString(getLength(), "\n", attUserInput);
      consoleTextPane.setCaretPosition(getLength());
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  // override the insertString to make sure everything typed ends up at the end
  // or in the 'command line' using the proper font, and the newline is processed.
  public void insertString(int offs, String str, AttributeSet a)
    throws BadLocationException {
    int ichNewline = str.indexOf('\n');
    if (ichNewline > 0)
      str = str.substring(0, ichNewline);
    if (ichNewline != 0) {
      if (offs < offsetAfterPrompt) {
        offs = getLength();
      }
      super.insertString(offs, str, a == attError ? a : attUserInput);
      consoleTextPane.setCaretPosition(offs+str.length());
    }
    if (ichNewline >= 0) {
      consoleTextPane.enterPressed();
    }
  }

  String getCommandString() {
    String strCommand = "";
    try {
      int cmdStart = positionAfterPrompt.getOffset();
      // skip unnecessary leading spaces in the command.
      strCommand =  getText(cmdStart, getLength() - cmdStart).trim();
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
    return strCommand;
  }

  public void remove(int offs, int len)
    throws BadLocationException {
    if (offs < offsetAfterPrompt) {
      len -= offsetAfterPrompt - offs;
      if (len <= 0)
        return;
      offs = offsetAfterPrompt;
    }
    super.remove(offs, len);
//    consoleTextPane.setCaretPosition(offs);
  }

  public void replace(int offs, int length, String str, AttributeSet attrs)
    throws BadLocationException {
    if (offs < offsetAfterPrompt) {
      if (offs + length < offsetAfterPrompt) {
        offs = getLength();
        length = 0;
      } else {
        length -= offsetAfterPrompt - offs;
        offs = offsetAfterPrompt;
      }
    }
    super.replace(offs, length, str, attrs);
//    consoleTextPane.setCaretPosition(offs + str.length());
  }

   /**
   * Replaces current command on script.
   * 
   * @param newCommand new command value
   * @param isError    true to set error color  ends with #??
   * 
   * @throws BadLocationException
   */
  void replaceCommand(String newCommand, boolean isError) throws BadLocationException {
    if (positionAfterPrompt == positionBeforePrompt)
      return;
    replace(offsetAfterPrompt, getLength() - offsetAfterPrompt, newCommand,
        isError ? attError : attUserInput);
  }
}

interface EnterListener {
  public void enterPressed();
}

