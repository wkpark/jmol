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

import org.jmol.util.GT;

public final class ScriptWindow extends JDialog
    implements ActionListener, EnterListener{

  private ConsoleTextPane console;
  private JButton closeButton;
  private JButton runButton;
  private JButton haltButton;
  private JButton clearButton;
  private JButton helpButton;
  JmolViewer viewer;
  
  public ScriptWindow(JmolViewer viewer, JFrame frame) {
    super(frame, GT._("Rasmol Scripts"), false);
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

    closeButton = new JButton(GT._("Close"));
    closeButton.addActionListener(this);
    buttonPanel.add(closeButton);

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

    helpButton = new JButton(GT._("Help"));
    helpButton.addActionListener(this);
    buttonPanel.add(helpButton);
  }

  public void scriptEcho(String strEcho) {
    if (strEcho != null) {
      console.outputEcho(strEcho);
    }
  }

  public void scriptStatus(String strStatus) {
    if (strStatus != null) {
      console.outputStatus(strStatus);
    }
  }

  public void notifyScriptTermination(String strMsg, int msWalltime) {
    if (strMsg != null) {
      console.outputError(strMsg);
    }
    runButton.setEnabled(true);
    haltButton.setEnabled(false);
  }

  public void enterPressed() {
    runButton.doClick(100);
    //    executeCommand();
  }

  void executeCommand() {
    String strCommand = console.getCommandString().trim();
    console.appendNewline();
    console.setPrompt();
    if (strCommand.length() > 0) {
      String strErrorMessage = viewer.evalString(strCommand);
      if (strErrorMessage != null)
        console.outputError(strErrorMessage);
      else {
        runButton.setEnabled(false);
        haltButton.setEnabled(true);
      }
    }
    console.grabFocus();
  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == closeButton) {
      hide();
    } else if (source == runButton) {
      executeCommand();
    } else if (source == clearButton) {
      System.out.println("clearing content of script window.");
      console.clearContent();
    } else if (source == haltButton) {
      System.out.println("calling viewer.haltScriptExecution();");
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

  private CommandHistory commandHistory = new CommandHistory(20);

  ConsoleDocument consoleDoc;
  EnterListener enterListener;

  ConsoleTextPane(EnterListener enterListener) {
    super(new ConsoleDocument());
    consoleDoc = (ConsoleDocument)getDocument();
    consoleDoc.setConsoleTextPane(this);
    this.enterListener = enterListener;
  }

  public String getCommandString() {
    String cmd = consoleDoc.getCommandString();
    commandHistory.addCommand(cmd);
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
    consoleDoc.clearContent();
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
    * Recall command histoy.
    * 
    * @param up - history up or down
    */
   private final void recallCommand(boolean up)
   {
      String cmd = up?commandHistory.getCommandUp():commandHistory.getCommandDown();
      
      try
       {
           consoleDoc.replaceCommand(cmd);
            
       }
       catch (BadLocationException e)
       {
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
      } catch (javax.swing.text.BadLocationException exception) {
          System.out.println("Could not clear script window content: " + exception.getMessage());
      }
      setPrompt();
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
    }
  }

  // override the insertString to make sure everything typed ends up at the end
  // or in the 'command line' using the proper font, and the newline is processed.
  public void insertString(int offs, String str, AttributeSet a)
    throws BadLocationException {
//    System.out.println("insertString("+offs+","+str+",attr)");
    int ichNewline = str.indexOf('\n');
    if (ichNewline > 0)
      str = str.substring(0, ichNewline);
    if (ichNewline != 0) {
      if (offs < offsetAfterPrompt) {
        offs = getLength();
      }
      super.insertString(offs, str, attUserInput);
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
    }
    return strCommand;
  }

  public void remove(int offs, int len)
    throws BadLocationException {
//    System.out.println("remove("+offs+","+len+")");
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
//    System.out.println("replace("+offs+","+length+","+str+",attr)");
    if (offs < offsetAfterPrompt) {
      if (offs + length < offsetAfterPrompt) {
        offs = getLength();
        length = 0;
      } else {
        length -= offsetAfterPrompt - offs;
        offs = offsetAfterPrompt;
      }
    }
    super.replace(offs, length, str, attUserInput);
//    consoleTextPane.setCaretPosition(offs + str.length());
  }

   /**
    * Replaces current command on script.
    * 
    * @param newCommand new command value
    * 
    * @throws BadLocationException
    */
   void replaceCommand(String newCommand) throws BadLocationException
   {
      replace(
         offsetAfterPrompt,
         getLength() - offsetAfterPrompt,
         newCommand,
         attUserInput);
   }
}

interface EnterListener {
  public void enterPressed();
}

