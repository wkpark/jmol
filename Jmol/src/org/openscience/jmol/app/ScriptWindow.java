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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
//import javax.swing.SwingUtilities;
import javax.swing.text.Position;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.JScrollPane;
import java.util.Vector;

import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.util.CommandHistory;
//import org.jmol.viewer.Token;

public final class ScriptWindow extends JDialog
    implements ActionListener, EnterListener{
  
  ConsoleTextPane console;
  private JButton closeButton;
  private JButton runButton;
  private JButton haltButton;
  private JButton clearButton;
  private JButton historyButton;
  private JButton stateButton;
  private JButton helpButton;
  private JButton undoButton;
  private JButton redoButton;
  JmolViewer viewer;
  
  public ScriptWindow(JmolViewer viewer, JFrame frame) {
    super(frame, GT._("Jmol Script Console"), false);
    this.viewer = viewer;
    layoutWindow(getContentPane());
    setSize(645, 400);
    setLocationRelativeTo(frame);
  }

  void layoutWindow(Container container) {
    console = new ConsoleTextPane(this);    
    console.setPrompt();
    JScrollPane consolePane = new JScrollPane(console);
        
    JPanel buttonPanel = new JPanel();

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

    stateButton = new JButton(GT._("State"));
    stateButton.addActionListener(this);
    buttonPanel.add(stateButton);

    helpButton = new JButton(GT._("Help"));
    helpButton.addActionListener(this);
    buttonPanel.add(helpButton);

    closeButton = new JButton(GT._("Close"));
    closeButton.addActionListener(this);
    buttonPanel.add(closeButton);

    undoButton = new JButton(GT._("Undo"));
    undoButton.addActionListener(this);
    undoButton.setEnabled(false);
    buttonPanel.add(undoButton);

    redoButton = new JButton(GT._("Redo"));
    redoButton.addActionListener(this);
    redoButton.setEnabled(false);
    buttonPanel.add(redoButton);

    
//    container.setLayout(new BorderLayout());
  //  container.add(consolePane, BorderLayout.CENTER);
    JPanel buttonPanelWrapper = new JPanel();
    buttonPanelWrapper.setLayout(new BorderLayout());
    buttonPanelWrapper.add(buttonPanel, BorderLayout.CENTER);

    JSplitPane spane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        consolePane, buttonPanelWrapper);
    consolePane.setMinimumSize(new Dimension(300,300));
    consolePane.setPreferredSize(new Dimension(5000,5000));
    buttonPanelWrapper.setMinimumSize(new Dimension(60,60));
    buttonPanelWrapper.setMaximumSize(new Dimension(1000,60));
    buttonPanelWrapper.setPreferredSize(new Dimension(60,60));
    spane.setDividerSize(0);
    spane.setResizeWeight(0.95);
    container.add(spane);
//    container.setLayout(new BorderLayout());
  //  container.add(consolePane,BorderLayout.CENTER);
    //container.add(buttonPanelWrapper,BorderLayout.SOUTH);

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
  }
  
  void sendConsoleMessage(String strStatus) {
    if (strStatus == null) {
      console.clearContent();
      console.outputStatus("");
    } else if (strStatus.indexOf("ERROR:") >= 0) {
      console.outputError(strStatus);
      setError(true);
      runButton.setEnabled(true);
      haltButton.setEnabled(false);
    } else if (!isError) {
      console.outputStatus(strStatus);
    }
  }

  void notifyScriptStart() {
    runButton.setEnabled(true);
    haltButton.setEnabled(true);
  }

  void notifyScriptTermination() {
    runButton.setEnabled(true);
    haltButton.setEnabled(false);
  }

  public void enterPressed() {
    runButton.doClick(100);
  }
  
  class ExecuteCommandThread extends Thread {

    String strCommand;
    ExecuteCommandThread (String command) {
      strCommand = command;
      this.setName("ScriptWindowExecuteCommandThread");
    }
    
    public void run() {
      
      try {
        
        while (console.checking) {
            try {
              Thread.sleep(100); //wait for command checker
            } catch (Exception e) {
              break; //-- interrupt? 
            }
        }

        executeCommand(strCommand);
      } catch (Exception ie) {
        Logger.error("execution command interrupted!",ie);
      }
    }
  }
   
  ExecuteCommandThread execThread;
  void executeCommandAsThread(){ 
    String strCommand = console.getCommandString().trim();
    if (strCommand.equalsIgnoreCase("undo")) {
      undoRedo(false);
      console.appendNewline();
      console.setPrompt();
      return;
    } else if (strCommand.equalsIgnoreCase("redo")) {
      undoRedo(true);
      console.appendNewline();
      console.setPrompt();
      return;
    } else if (strCommand.equalsIgnoreCase("exitJmol")) {
      System.exit(0);
    }
      
    if (strCommand.length() > 0) {
      execThread = new ExecuteCommandThread(strCommand);
      execThread.start();
      //can't do this: 
      //SwingUtilities.invokeLater(execThread);
      //because then the thread runs from the event queue, and that 
      //causes PAUSE to hang the application on refresh()
    }
  }

  static int MAXUNDO = 50;
  String[] undoStack = new String[MAXUNDO];
  int undoPointer = 0;
  boolean undoSaved = false;
 
  void undoClear() {
    for (int i = 0; i < MAXUNDO; i++)
      undoStack[i] = null;
    undoPointer = 0;
    undoButton.setEnabled(false);
    redoButton.setEnabled(false);
  }
  
  void undoSetEnabled() {
    undoButton.setEnabled(undoPointer > 0 && undoStack[undoPointer - 1] != null);
    redoButton.setEnabled(undoPointer + 1 < MAXUNDO && undoStack[undoPointer + 1] != null);
  }
  
  void undoRedo(boolean isRedo) {
    // pointer is always left at the undo slot when a command is given
    // redo at CURRENT pointer position
    if (!viewer.getBooleanProperty("undo"))
      return;
    if (!undoSaved) 
      undoSave();
    String state = undoStack[undoPointer];
    int ptr = undoPointer + (isRedo ? 1 : -1);
    if (ptr == MAXUNDO)
      ptr--;
    if (ptr < 0)
      ptr = 0;
    //console.outputError("undoredo " + isRedo + " " + ptr);
    state = undoStack[ptr];
    if (state != null) {
      state += CommandHistory.NOHISTORYATALL_FLAG;
      setError(false);
      viewer.evalStringQuiet(state);
      undoPointer = ptr;
    }
    undoSetEnabled();
  }
  
  void undoSave() {
    if (!viewer.getBooleanProperty("undo"))
      return;
    //shift stack if full
    undoPointer++;
    if (undoPointer == MAXUNDO) {
      for (int i = 1; i < MAXUNDO; i++)
        undoStack[i - 1] = undoStack[i];
      undoPointer--;
    }
    //delete redo items, since they will no longer be valid
    for (int i = undoPointer; i < MAXUNDO; i++)
      undoStack[i] = null;
    Logger.startTimer();
    undoStack[undoPointer] = (String) viewer.getProperty("readable", "stateInfo",
        null);
    if (Logger.checkTimer(null) > 1000) {
      viewer.setBooleanProperty("undo", false);
      Logger.info("command processing slow; undo disabled");
      undoClear();
    } else {
      undoSetEnabled();
    }
    undoSaved = true;
  }
  
  void executeCommand(String strCommand) {
    boolean doWait;
    console.appendNewline();
    console.setPrompt();
    if (strCommand.length() == 0) {
      console.grabFocus();
      return;
    }
    
    if (strCommand.charAt(0) != '!' && !isError) {
      undoSave();
    }
    setError(false);
    undoSaved = false;

    String strErrorMessage = null;
    doWait = (strCommand.indexOf("WAITTEST ") == 0);
    if (doWait) { //for testing, mainly
      // demonstrates using the statusManager system; probably hangs application.
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
          Logger.info("msg#=" + statusRecord.get(0) + " " + statusRecord.get(1)
              + " intInfo=" + statusRecord.get(2) + " stringInfo="
              + statusRecord.get(3));
        }
      }
      console.appendNewline();
    } else {
      boolean isScriptExecuting = viewer.isScriptExecuting();
      strErrorMessage = "";
      if (viewer.checkHalt(strCommand))
        strErrorMessage = (isScriptExecuting ? "script execution halted with "
            + strCommand : "no script was executing");
      //the problem is that scriptCheck is synchronized, so these might get backed up. 
      if (strErrorMessage.length() > 0) {
        console.outputError(strErrorMessage);
      } else {
        runButton.setEnabled(true);
        haltButton.setEnabled(true);
        viewer.script(strCommand);
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
    } else if (source == stateButton) {
      console.clearContent(viewer.getStateInfo());
    } else if (source == haltButton) {
      viewer.haltScriptExecution();
    } else if (source == undoButton) {
      undoRedo(false);
    } else if (source == redoButton) {
      undoRedo(true);
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
  
  boolean checking = false;
  
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
    * Custom key event processing for command 0 implementation.
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

     int kcode = ke.getKeyCode();
     int kid = ke.getID();
     if (kcode == KeyEvent.VK_UP
         && kid == KeyEvent.KEY_PRESSED
         && !ke.isControlDown()) {
         recallCommand(true);
      } else if (
         kcode == KeyEvent.VK_DOWN
            && kid == KeyEvent.KEY_PRESSED
            && !ke.isControlDown()) {
         recallCommand(false);
      } else if (
         (kcode == KeyEvent.VK_DOWN
            || kcode == KeyEvent.VK_UP)
            && kid == KeyEvent.KEY_PRESSED
            && ke.isControlDown()) {
        // If Control key is down, redefines the event as if it 
        // where a key up or key down stroke without modifiers.  
        // This allows to move the caret up and down
        // with no command history recall.
         super
            .processKeyEvent(new KeyEvent(
               (Component) ke.getSource(),
               kid,
               ke.getWhen(),
               0,         // No modifiers
               kcode, 
               ke.getKeyChar(), 
               ke.getKeyLocation()));
      } else {
        // Standard processing for other events.
         super.processKeyEvent(ke);
         //check command for compiler-identifyable syntax issues
         //this may have to be taken out if people start complaining
         //that only some of the commands are being checked
         //that is -- that the script itself is not being fully checked
         
         //not perfect -- help here?
         if (kid == KeyEvent.KEY_RELEASED && ke.getModifiers() < 2
             && (kcode > KeyEvent.VK_DOWN  && kcode < 400 || kcode == KeyEvent.VK_BACK_SPACE))
           checkCommand();
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

   
  synchronized void checkCommand() {
    String strCommand = consoleDoc.getCommandString();
    if (strCommand.length() == 0 || strCommand.charAt(0) == '!'
        || viewer.isScriptExecuting())
      return;
    checking = true;
    consoleDoc
        .colorCommand(viewer.scriptCheck(strCommand) == null ? consoleDoc.attUserInput
            : consoleDoc.attError);
    checking = false;
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
      Logger.error("Could not clear script window content", exception);
    }
  }
  
  void setPrompt() {
    try {
      super.insertString(getLength(), "$ ", attPrompt);
      setOffsetPositions();
      consoleTextPane.setCaretPosition(offsetAfterPrompt);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  void setOffsetPositions() {
    try {
      offsetAfterPrompt = getLength();
      positionBeforePrompt = createPosition(offsetAfterPrompt - 2);
      // after prompt should be immediately after $ otherwise tracks the end
      // of the line (and no command will be found) at least on Mac OS X it did.
      positionAfterPrompt = createPosition(offsetAfterPrompt - 1);
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
      int pt = consoleTextPane.getCaretPosition();
      Position caretPosition = createPosition(pt);
      pt = positionBeforePrompt.getOffset();
      super.insertString(pt, str+"\n", attribute);
      //setOffsetPositions();
      offsetAfterPrompt += str.length() + 1;
      positionBeforePrompt = createPosition(offsetAfterPrompt - 2);
      positionAfterPrompt = createPosition(offsetAfterPrompt - 1);
      
      pt = caretPosition.getOffset();
      consoleTextPane.setCaretPosition(pt);
    } catch (Exception e) {
      e.printStackTrace();
      consoleTextPane.setCaretPosition(getLength());
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
      strCommand =  getText(cmdStart, getLength() - cmdStart);
      while (strCommand.length() > 0 && strCommand.charAt(0) == ' ')
        strCommand = strCommand.substring(1);
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

  void colorCommand(SimpleAttributeSet att) {
    if (positionAfterPrompt == positionBeforePrompt)
      return;
    setCharacterAttributes(offsetAfterPrompt, getLength() - offsetAfterPrompt, att, true);
  }
}

interface EnterListener {
  public void enterPressed();
}

