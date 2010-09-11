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
package org.openscience.jmol.app.jmolpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTextPane; //import javax.swing.SwingUtilities;
import javax.swing.text.Position;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.JScrollPane;

import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolViewer;
import org.jmol.console.JmolConsole;
import org.jmol.i18n.GT;
import org.jmol.util.CommandHistory;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

public class AppConsole extends JmolConsole implements JmolAppConsoleInterface,
    JmolCallbackListener, EnterListener {

  protected static final String ALL_BUTTONS = "Editor Check Top Step Variables Clear History State Help Close UndoRedo";
  protected ConsoleTextPane console;
  private JButton varButton, haltButton, closeButton, clearButton, 
      helpButton, undoButton, redoButton, checkButton, topButton;
  protected JButton stepButton;

  /*
   * methods sendConsoleEcho, sendConsoleMessage(strStatus), notifyScriptStart(), notifyScriptTermination()
   * are public in case developers want to use appConsole separate from the Jmol application.
   * 
   */

  public AppConsole() {
    // required for Class.forName  
    // should be used only in the context:
    // appConsole = ((JmolApplicationConsoleInterface) Interface
    //       .getApplicationInterface("jmolpanel.AppConsole")).getAppConsole(viewer, display);
  }

  public JmolAppConsoleInterface getAppConsole(Viewer viewer,
                                               Component display) {
    return new AppConsole(viewer, display instanceof JFrame ? (JFrame) display
        : display instanceof DisplayPanel ? ((DisplayPanel) display).getFrame()
            : null, null, null);
  }

  public void dispose() {
    if (jcd != null)
      jcd.dispose();
  }

  public AppConsole(JmolViewer viewer, JFrame frame, 
                    Container externalPanel, String enableButtons) {
    super(viewer, frame, (externalPanel == null));
    layoutWindow(externalPanel == null ? jcd.getContentPane() : externalPanel, 
        enableButtons);
    if (jcd == null) {
      viewer.setConsole(this);
    } else {
      jcd.setSize(645, 400);
      jcd.setLocationRelativeTo(frame);
    }
  }

  JButton setButton(String s) {
    JButton b = new JButton(s);
    b.addActionListener(this);
    buttonPanel.add(b);
    return b;
  }

  JPanel buttonPanel = new JPanel();

  void layoutWindow(Container container, String enableButtons) {
    console = new ConsoleTextPane(this);
    console.setPrompt();
    console.setDragEnabled(true);
    if (enableButtons == null)
      enableButtons = ALL_BUTTONS;
    JScrollPane consolePane = new JScrollPane(console);
    String[] tokens = Parser.getTokens(enableButtons);
    for (int i = 0; i < tokens.length; i++)
      enableButton(tokens[i]);
    setEnabled(undoButton, false);
    setEnabled(redoButton, false);

    //    container.setLayout(new BorderLayout());
    //  container.add(consolePane, BorderLayout.CENTER);
    JPanel buttonPanelWrapper = new JPanel();
    buttonPanelWrapper.setLayout(new BorderLayout());
    buttonPanelWrapper.add(buttonPanel, BorderLayout.CENTER);

    JSplitPane spane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, consolePane,
        buttonPanelWrapper);
    consolePane.setMinimumSize(new Dimension(300, 300));
    consolePane.setPreferredSize(new Dimension(5000, 5000));
    buttonPanelWrapper.setMinimumSize(new Dimension(60, 60));
    buttonPanelWrapper.setMaximumSize(new Dimension(1000, 60));
    buttonPanelWrapper.setPreferredSize(new Dimension(60, 60));
    spane.setDividerSize(0);
    spane.setResizeWeight(0.95);
    container.add(spane);
    //    container.setLayout(new BorderLayout());
    //  container.add(consolePane,BorderLayout.CENTER);
    //container.add(buttonPanelWrapper,BorderLayout.SOUTH);

  }

  private void enableButton(String name) {
    switch((
        "Check     " +
    		"Clear     " +
    		"Close     " +
    		"Editor    " +
    		"Halt      " +
    		"Help      " +
    		"History   " +
    		"State     " +
    		"Step      " +
    		"Top       " +
    		"UndoRedo  " +
    		"Variables ").indexOf(name)) {
    case 0:
      checkButton = setButton(GT._("Check"));
      break;
    case 10:
      clearButton = setButton(GT._("Clear"));
      break;
    case 20:
      closeButton = setButton(GT._("Close"));
      break;
    case 30:
      editButton = setButton(GT._("Editor"));
      break;
    case 40:
        haltButton = setButton(GT._("Halt"));
     break;
    case 50:
      helpButton = setButton(GT._("Help"));
      break;
    case 60:
      historyButton = setButton(GT._("History"));
      break;
    case 70:
      stateButton = setButton(GT._("State"));
      break;
    case 80:
      stepButton = setButton(GT._("Step"));
      break;
    case 90:
      topButton = setButton(GT._("Top"));
      break;
    case 100:
      undoButton = setButton(GT._("Undo"));
      redoButton = setButton(GT._("Redo"));
      break;
    case 110:
      varButton = setButton(GT._("Variables"));
      break;
    }
  }

  public void sendConsoleEcho(String strEcho) {
    if (strEcho != null) {
      console.outputEcho(strEcho);
    }
    setError(false);
  }

  boolean isError = false;

  private void setError(boolean TF) {
    isError = TF;
  }

  public void sendConsoleMessage(String strStatus) {
    if (strStatus == null) {
      console.clearContent(null);
      console.outputStatus("");
    } else if (strStatus.indexOf("ERROR:") >= 0) {
      console.outputError(strStatus);
      setError(true);
    } else {
      console.outputStatus(strStatus);
      isError = false;
    }
  }

  public void enterPressed() {
    executeCommandAsThread(null);
  }

  class ExecuteCommandThread extends Thread {

    String strCommand;

    ExecuteCommandThread(String command) {
      strCommand = command;
      this.setName("appConsoleExecuteCommandThread");
    }

    @Override
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
        Logger.error("execution command interrupted!", ie);
      }
    }
  }

  ExecuteCommandThread execThread;

  @Override
  protected void execute(String strCommand) {
    executeCommandAsThread(strCommand);
  }

  void executeCommandAsThread(String strCommand) {
    if (strCommand == null)
      strCommand = console.getCommandString().trim();
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
    } else if (strCommand.length() == 0) {
      strCommand = "!resume";
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

  
  private static int MAXUNDO = 10;
  private String[] undoStack = new String[MAXUNDO + 1];
  private int undoPointer = 0;
  private boolean undoSaved = false;

  public void zap() {
    //undoClear();
  }

/*  
  private void undoClear() {
    if (undoButton == null)
      return;
    for (int i = 0; i <= MAXUNDO; i++)
      undoStack[i] = null;
    undoPointer = 0;
    undoButton.setEnabled(false);
    redoButton.setEnabled(false);
  }
*/
  
  void undoSetEnabled() {
    if (undoButton == null)
      return;
    undoButton
        .setEnabled(undoPointer > 0 && undoStack[undoPointer - 1] != null);
    redoButton.setEnabled(undoPointer < MAXUNDO
        && undoStack[undoPointer + 1] != null);
  }

  void undoRedo(boolean isRedo) {
    if (undoButton == null)
      return;
    // pointer is always left at the undo slot when a command is given
    // redo at CURRENT pointer position
    if (!viewer.getBooleanProperty("undo")
        || !viewer.getBooleanProperty("preserveState"))
      return;
    //dumpUndo("undoRedo1");
    int ptr = undoPointer + (isRedo ? 1 : -1);
    if (!undoSaved) {
      undoSave(false);
    }
    //dumpUndo("undoRedo2");
    if (ptr > MAXUNDO || ptr < 0)
      return;
    String state = undoStack[ptr];
    if (state != null) {
      state += CommandHistory.NOHISTORYATALL_FLAG;
      setError(false);
      viewer.evalStringQuiet(state);
      undoPointer = ptr;
    }
    undoSetEnabled();
    //dumpUndo("undoRedo DONE");
  }

  private void undoSave(boolean incrementPtr) {
    if (undoButton == null)
      return;
    if (!viewer.getBooleanProperty("undo")
        || !viewer.getBooleanProperty("preserveState"))
      return;
    //delete redo items, since they will no longer be valid
    for (int i = undoPointer + 1; i <= MAXUNDO; i++)
      undoStack[i] = null;
    Logger.startTimer();
    undoStack[undoPointer] = (String) viewer.getProperty("readable",
        "stateInfo", null);
    //shift stack if full
    if (incrementPtr && undoPointer == MAXUNDO) {
      for (int i = 1; i <= MAXUNDO; i++)
        undoStack[i - 1] = undoStack[i];
      undoStack[MAXUNDO] = null;
    } else if (incrementPtr)
      undoPointer++;
    if (Logger.checkTimer(null) > 1000) {
      //viewer.setBooleanProperty("undo", false);
      //undoClear();
      Logger.info("command processing slow; undo disabled");
    } else {
      undoSetEnabled();
    }
    undoSaved = true;
    //dumpUndo("undoSave DONE");
  }
/*
  private void dumpUndo(String string) {
    System.out.println("\n" + string);
    for (int i = 0; i < 8 && i <= MAXUNDO; i++)
      System.out.println((i == undoPointer ? ">" : " ") + i 
          + "\t" + (undoStack[i] == null ? null : "OK\t" + undoStack[i].substring(undoStack[i].indexOf(" background "),undoStack[i].indexOf(" background ") + 22 ) ));
     
    return;
  }
*/
  @SuppressWarnings("unchecked")
  void executeCommand(String strCommand) {
    boolean doWait;
    console.appendNewline();
    console.setPrompt();
    if (strCommand.length() == 0) {
      console.grabFocus();
      return;
    }
    if (strCommand.charAt(0) != '!'
        && viewer.getBooleanProperty("executionPaused"))
      strCommand = "!" + strCommand;
    if (strCommand.charAt(0) != '!' && !isError) {
      undoSave(true);
    }
    setError(false);
    undoSaved = false;

    String strErrorMessage = null;
    doWait = (strCommand.indexOf("WAITTEST ") == 0);
    if (doWait) { //for testing, mainly
      // demonstrates using the statusManager system; probably hangs application.
      Object o = viewer
          .scriptWaitStatus(strCommand.substring(5),
              "+fileLoaded,+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated");
      if (o instanceof List) {
        List<List<List<Object>>> info = (List<List<List<Object>>>) o;
        /*
         * info = [ statusRecortSet0, statusRecortSet1, statusRecortSet2, ...]
         * statusRecordSet = [ statusRecord0, statusRecord1, statusRecord2, ...]
         * statusRecord = [int msgPtr, String statusName, int intInfo, String msg]    
         */
        for (int i = 0; i < info.size(); i++) {
          List<List<Object>> statusRecordSet = info.get(i);
          for (int j = 0; j < statusRecordSet.size(); j++) {
            List<Object> statusRecord = statusRecordSet.get(j);
            Logger.info("msg#=" + statusRecord.get(0) + " "
                + statusRecord.get(1) + " intInfo=" + statusRecord.get(2)
                + " stringInfo=" + statusRecord.get(3));
          }
        }
      }
      console.appendNewline();
    } else {
      boolean isScriptExecuting = viewer.isScriptExecuting();
      strErrorMessage = "";
      String str = strCommand;
      boolean isInterrupt = (str.charAt(0) == '!');
      if (isInterrupt)
        str = str.substring(1);
      if (viewer.checkHalt(str, isInterrupt))
        strErrorMessage = (isScriptExecuting ? "script execution halted with "
            + strCommand : "no script was executing");
      //the problem is that scriptCheck is synchronized, so these might get backed up. 
      if (strErrorMessage.length() > 0) {
        console.outputError(strErrorMessage);
      } else {
        viewer.script(strCommand
            + (strCommand.indexOf("\1##") >= 0 ? ""
                : JmolConstants.SCRIPT_EDITOR_IGNORE));
      }
    }
    if (strCommand.indexOf("\1##") < 0)
      console.grabFocus();
  }

  @Override
  protected void clearContent(String text) {
    console.clearContent(text);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    console.grabFocus(); // always grab the focus (e.g., after clear)
    Object source = e.getSource();

    if (source == topButton) {
      if (scriptEditor != null)
        scriptEditor.gotoTop();
      return;
    }
    if (source == checkButton) {
      if (scriptEditor != null)
        scriptEditor.checkScript();
    }
    if (source == stepButton) {
      if (scriptEditor != null)
        scriptEditor.doStep();
      return;
    }

    if (source == closeButton) {
      setVisible(false);
      return;
    }
    if (source == haltButton) {
      viewer.haltScriptExecution();
      return;
    }
    if (source == varButton) {
      execute("!show variables");
      return;
    }
    if (source == clearButton) {
      console.clearContent(null);
      return;
    }
    if (source == undoButton) {
      undoRedo(false);
      return;
    }
    if (source == redoButton) {
      undoRedo(true);
      return;
    }
    if (source == helpButton) {
      URL url = this.getClass().getClassLoader().getResource(
          "org/openscience/jmol/Data/guide/ch04.html");
      if (url == null)
        viewer.script("help");
      else
        (new HelpDialog(null, url)).setVisible(true);
    }
    super.actionPerformed(e);
  }

  class ConsoleTextPane extends JTextPane {

    private ConsoleDocument consoleDoc;
    private EnterListener enterListener;

    boolean checking = false;

    ConsoleTextPane(AppConsole appConsole) {
      super(new ConsoleDocument());
      consoleDoc = (ConsoleDocument) getDocument();
      consoleDoc.setConsoleTextPane(this);
      this.enterListener = appConsole;
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

    public void clearContent(String text) {
      consoleDoc.clearContent();
      if (text != null)
        consoleDoc.outputEcho(text);
      setPrompt();
    }

    /*
    * (non-Javadoc)
    * 
    * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
    */

    /**
     * Custom key event processing for command 0 implementation.
     * 
     * Captures key up and key down strokes to call command history and
     * redefines the same events with control down to allow caret vertical
     * shift.
     * 
     * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
     */
    @Override
    protected void processKeyEvent(KeyEvent ke) {
      // Id Control key is down, captures events does command
      // history recall and inhibits caret vertical shift.

      int kcode = ke.getKeyCode();
      int kid = ke.getID();
      if (kid == KeyEvent.KEY_PRESSED) {
        if (kcode == KeyEvent.VK_TAB) {
          ke.consume();
          if (consoleDoc.isAtEnd()) {
            String cmd = completeCommand(consoleDoc.getCommandString());
            if (cmd != null)
              try {
                consoleDoc.replaceCommand(cmd, false);
              } catch (BadLocationException e) {
                //
              }
            nTab++;
            //checkCommand();
            return;
          }
        }
        nTab = 0;
      }
      if (kcode == KeyEvent.VK_UP && kid == KeyEvent.KEY_PRESSED
          && !ke.isControlDown()) {
        recallCommand(true);
      } else if (kcode == KeyEvent.VK_DOWN && kid == KeyEvent.KEY_PRESSED
          && !ke.isControlDown()) {
        recallCommand(false);
      } else if ((kcode == KeyEvent.VK_DOWN || kcode == KeyEvent.VK_UP)
          && kid == KeyEvent.KEY_PRESSED && ke.isControlDown()) {
        // If Control key is down, redefines the event as if it
        // where a key up or key down stroke without modifiers.
        // This allows to move the caret up and down
        // with no command history recall.
        super.processKeyEvent(new KeyEvent((Component) ke.getSource(), kid, ke
            .getWhen(), 0, // No modifiers
            kcode, ke.getKeyChar(), ke.getKeyLocation()));
      } else {
        // Standard processing for other events.
        super.processKeyEvent(ke);
        // check command for compiler-identifyable syntax issues
        // this may have to be taken out if people start complaining
        // that only some of the commands are being checked
        // that is -- that the script itself is not being fully checked

        // not perfect -- help here?
        if (kid == KeyEvent.KEY_RELEASED
            && ke.getModifiers() < 2
            && (kcode > KeyEvent.VK_DOWN && kcode < 400 || kcode == KeyEvent.VK_BACK_SPACE))
          checkCommand();
      }
    }

    /**
     * Recall command history.
     * 
     * @param up
     *          - history up or down
     */
    void recallCommand(boolean up) {
      String cmd = viewer.getSetHistory(up ? -1 : 1);
      if (cmd == null) {
        return;
      }
      boolean isError = false;
      try {
        if (cmd.endsWith(CommandHistory.ERROR_FLAG)) {
          isError = true;
          cmd = cmd.substring(0, cmd.indexOf(CommandHistory.ERROR_FLAG));
        }
        cmd = TextFormat.trim(cmd, ";");
        consoleDoc.replaceCommand(cmd, isError);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }

    synchronized void checkCommand() {
      String strCommand = consoleDoc.getCommandString();
      if (strCommand.length() == 0 || strCommand.charAt(0) == '!'
          || viewer.isScriptExecuting()
          || viewer.getBooleanProperty("executionPaused"))
        return;
      checking = true;
      consoleDoc
          .colorCommand(viewer.scriptCheck(strCommand) instanceof String ? consoleDoc.attError
              : consoleDoc.attUserInput);
      checking = false;
    }
  }

  @Override
  protected String completeCommand(String thisCmd) {
    return super.completeCommand(thisCmd);
  }

  public Object getMyMenuBar() {
    return null;
  }

  public String getText() {
    return console.getText();
  }

  class ConsoleDocument extends DefaultStyledDocument {

    private ConsoleTextPane consoleTextPane;

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

      SimpleAttributeSet attEcho;
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

    private Position positionBeforePrompt; // starts at 0, so first time isn't tracked (at least on Mac OS X)
    private Position positionAfterPrompt; // immediately after $, so this will track
    private int offsetAfterPrompt; // only still needed for the insertString override and replaceCommand

    boolean isAtEnd() {
      return consoleTextPane.getCaretPosition() == getLength();
    }

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
        super.insertString(pt, str + "\n", attribute);
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
        super.insertString(getLength(), strError + "\n", attError);
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
    @Override
    public void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException {
      int ichNewline = str.indexOf('\n');
      if (ichNewline != 0) {
        if (offs < offsetAfterPrompt) {
          offs = getLength();
        }
        super.insertString(offs, str, a == attError ? a : attUserInput);
        consoleTextPane.setCaretPosition(offs + str.length());
      }
      if (ichNewline >= 0) {
        consoleTextPane.enterPressed();
      }
    }

    String getCommandString() {
      String strCommand = "";
      try {
        int cmdStart = positionAfterPrompt.getOffset();
        strCommand = getText(cmdStart, getLength() - cmdStart);
        while (strCommand.length() > 0 && strCommand.charAt(0) == ' ')
          strCommand = strCommand.substring(1);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
      return strCommand;
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
      if (offs < offsetAfterPrompt) {
        len -= offsetAfterPrompt - offs;
        if (len <= 0)
          return;
        offs = offsetAfterPrompt;
      }
      super.remove(offs, len);
      //      consoleTextPane.setCaretPosition(offs);
    }

    @Override
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
      //      consoleTextPane.setCaretPosition(offs + str.length());
    }

    /**
    * Replaces current command on script.
    * 
    * @param newCommand new command value
    * @param isError    true to set error color  ends with #??
    * 
    * @throws BadLocationException
    */
    void replaceCommand(String newCommand, boolean isError)
        throws BadLocationException {
      if (positionAfterPrompt == positionBeforePrompt)
        return;
      replace(offsetAfterPrompt, getLength() - offsetAfterPrompt, newCommand,
          isError ? attError : attUserInput);
    }

    void colorCommand(SimpleAttributeSet att) {
      if (positionAfterPrompt == positionBeforePrompt)
        return;
      setCharacterAttributes(offsetAfterPrompt,
          getLength() - offsetAfterPrompt, att, true);
    }
  }

  ///////////// JmolCallbackListener interface

  // Allowing for just the callbacks needed to provide status feedback to the console.
  // Not used in Jmol application itself; here to provide basic callback capability
  // for applications that embed Jmol -- see the example application Integration.java.
  // 
  // See org.openscience.jmol.app.jmolpanel.StatusListener.java for a complete list

  public boolean notifyEnabled(int type) {
    switch (type) {
    case JmolConstants.CALLBACK_ECHO:
    case JmolConstants.CALLBACK_MEASURE:
    case JmolConstants.CALLBACK_MESSAGE:
    case JmolConstants.CALLBACK_PICK:
      return true;
    }
    return false;
  }

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

interface EnterListener {
  public void enterPressed();
}
