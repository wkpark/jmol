/*
 * Copyright 2002 The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.script.Eval;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.Position;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ScriptWindow extends JDialog
    implements ActionListener, EnterListener {

  private ConsoleTextPane console;
  private JTextField input;
  private JButton closeButton;
  DisplayControl control;

  public ScriptWindow(DisplayControl control, JFrame frame) {

    super(frame, "Rasmol Scripts", false);
    this.control = control;
    getContentPane().setLayout(new BorderLayout());

    console = new ConsoleTextPane(this);
    console.setPrompt();
    getContentPane().add(new JScrollPane(console), BorderLayout.CENTER);

    closeButton = new JButton("Close");
    closeButton.addActionListener(this);
    getContentPane().add(closeButton, BorderLayout.SOUTH);

    setSize(300, 400);
    setLocationRelativeTo(frame);
  }

  public void scriptEcho(String strEcho) {
    if (strEcho != null) {
      console.outputEcho(strEcho);
    }
  }

  public void notifyScriptTermination(String strMsg, int msWalltime) {
    if (strMsg != null) {
      console.outputError(strMsg);
    }
  }

  public void enterPressed() {
    executeCommand();
  }

  void executeCommand() {
    String strCommand = console.getCommandString();    
    console.appendNewline();
    console.setPrompt();
    String strErrorMessage = control.evalString(strCommand);
    if (strErrorMessage != null)
      console.outputError(strErrorMessage);

    System.out.println("enterPressed:" + strCommand);
  }

  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == closeButton) {
      System.out.println("closeButton");
      hide();
    } else {
      String command = input.getText();
      console.appendCommand(command);
      input.setText(null);

      String strErrorMessage = control.evalString(command);
      if (strErrorMessage != null) {
        console.outputError(strErrorMessage);
        console.setPrompt();
      }
    }
  }

  public void hide() {
    //    eval.haltExecution();
    super.hide();
  }
}

class ConsoleTextPane extends JTextPane {

  ConsoleDocument consoleDoc;
  EnterListener enterListener;

  ConsoleTextPane(EnterListener enterListener) {
    super(new ConsoleDocument());
    consoleDoc = (ConsoleDocument)getDocument();
    consoleDoc.setConsoleTextPane(this);
    this.enterListener = enterListener;
 }

  public String getCommandString() {
    return consoleDoc.getCommandString();
  }

  public void setPrompt() {
    consoleDoc.setPrompt();
  }

  public void appendCommand(String strCommand) {
    consoleDoc.appendCommand(strCommand);
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

  public void enterPressed() {
    if (enterListener != null)
      enterListener.enterPressed();
  }
}

class ConsoleDocument extends DefaultStyledDocument {

  ConsoleTextPane consoleTextPane;

  SimpleAttributeSet attError;
  SimpleAttributeSet attEcho;
  SimpleAttributeSet attPrompt;
  SimpleAttributeSet attUserInput;

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
  }

  void setConsoleTextPane(ConsoleTextPane consoleTextPane) {
    this.consoleTextPane = consoleTextPane;
  }

  Position positionBeforePrompt;
  int offsetAfterPrompt;

  void setPrompt() {
    try {
      super.insertString(getLength(), "# ", attPrompt);
      offsetAfterPrompt = getLength();
      positionBeforePrompt = createPosition(offsetAfterPrompt - 2);
      consoleTextPane.setCaretPosition(offsetAfterPrompt);
    } catch (BadLocationException e) {
    }
  }

  void outputError(String strError) {
    try {
      super.insertString(positionBeforePrompt.getOffset(), strError, attError);
      super.insertString(positionBeforePrompt.getOffset(), "\n", attError);
      offsetAfterPrompt = positionBeforePrompt.getOffset() + 2;
    } catch (BadLocationException e) {
    }
  }

  void outputErrorForeground(String strError) {
    try {
      super.insertString(getLength(), strError, attError);
      super.insertString(getLength(), "\n", attError);
    } catch (BadLocationException e) {
    }
  }

  void outputEcho(String strEcho) {
    try {
      super.insertString(positionBeforePrompt.getOffset(), strEcho, attEcho);
      super.insertString(positionBeforePrompt.getOffset(), "\n", attEcho);
      offsetAfterPrompt = positionBeforePrompt.getOffset() + 2;
    } catch (BadLocationException e) {
    }
  }

  void appendCommand(String strCommand) {
    try {
      super.insertString(getLength(), strCommand, attUserInput);
      super.insertString(getLength(), "\n", attUserInput);
      consoleTextPane.setCaretPosition(getLength());
    } catch (BadLocationException e) {
    }
  }

  void appendNewline() {
    try {
      super.insertString(getLength(), "\n", attUserInput);
      consoleTextPane.setCaretPosition(getLength());
    } catch (BadLocationException e) {
    }
  }

  public void insertString(int offs, String str, AttributeSet a)
    throws BadLocationException {
    int ichNewline = str.indexOf('\n');
    if (ichNewline > 0)
      str = str.substring(0, ichNewline);
    if (ichNewline != 0) {
      System.out.println("insertString(" + offs + "," + str + ")");
      if (offs < offsetAfterPrompt) {
        System.out.println("offs < minimumOffset");
        offs = getLength();
      }
      super.insertString(offs, str, attUserInput);
      consoleTextPane.setCaretPosition(getLength());
    }
    if (ichNewline >= 0) {
      consoleTextPane.enterPressed();
    }
  }

  String getCommandString() {
    String strCommand = "";
    try {
      strCommand =  getText(offsetAfterPrompt,
                            getLength() - offsetAfterPrompt);
    } catch (BadLocationException e) {
    }
    return strCommand;
  }

  public void remove(int offs, int len)
    throws BadLocationException {
    System.out.println("remove(" + offs + "," + len + ")" );
    if (offs < offsetAfterPrompt) {
      System.out.println("offs < minimumOffset");
      len -= offsetAfterPrompt - offs;
      if (len <= 0)
        return;
      offs = offsetAfterPrompt;
    }
    super.remove(offs, len);
    consoleTextPane.setCaretPosition(offs);
  }

  public void replace(int offs, int length, String str, AttributeSet attrs)
    throws BadLocationException {
    System.out.println("replace(" + offs + "," + length + "," + str + ")" );
    if (offs < offsetAfterPrompt) {
      System.out.println("offs < minimumOffset");
      if (offs + length < offsetAfterPrompt) {
        System.out.println("offs + length < minimumOffs");
        offs = getLength();
        length = 0;
      } else {
        System.out.println("offs + length >= minimumOffs");
        length -= offsetAfterPrompt - offs;
        offs = offsetAfterPrompt;
      }
    }
    super.replace(offs, length, str, attUserInput);
    consoleTextPane.setCaretPosition(offs + str.length());
  }
}

interface EnterListener {
  public void enterPressed();
}

