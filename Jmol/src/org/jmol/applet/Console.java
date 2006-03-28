/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package org.jmol.applet;

import org.jmol.api.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

class Console implements ActionListener, WindowListener {
  final JTextArea input = new ShiftEnterTextArea();
  final JTextPane output = new JTextPane();
  final Document outputDocument = output.getDocument();
  final JFrame jf = new JFrame("Jmol Console");

  final JButton runButton = new JButton("Execute");

  final SimpleAttributeSet attributesCommand = new SimpleAttributeSet();

  final JmolViewer viewer;
  final Jvm12 jvm12;

  Console(Component componentParent, JmolViewer viewer, Jvm12 jvm12) {
    this.viewer = viewer;
    this.jvm12 = jvm12;

    System.out.println("Console constructor");

    setupInput();
    setupOutput();

    JScrollPane jscrollInput = new JScrollPane(input);
    jscrollInput.setMinimumSize(new Dimension(2, 25));

    JScrollPane jscrollOutput = new JScrollPane(output);
    jscrollOutput.setMinimumSize(new Dimension(2, 25));
    Container c = jf.getContentPane();

    JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                    jscrollOutput, jscrollInput);
    jsp.setResizeWeight(.9);

    c.setLayout(new BorderLayout());
    c.add(jsp, BorderLayout.CENTER);
    c.add(runButton, BorderLayout.SOUTH);

    runButton.addActionListener(this);

    jf.setSize(400, 400);
    jf.addWindowListener(this);
  }

  void setupInput() {
    input.setLineWrap(true);
    input.setWrapStyleWord(true);

    Keymap map = input.getKeymap();
    //    KeyStroke shiftCR = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
    //                                               InputEvent.SHIFT_MASK);
    KeyStroke shiftA = KeyStroke.getKeyStroke(KeyEvent.VK_A,
                                              InputEvent.SHIFT_MASK);
    map.removeKeyStrokeBinding(shiftA);
  }

  void setupOutput() {
    output.setEditable(false);
    //    output.setLineWrap(true);
    //    output.setWrapStyleWord(true);
    StyleConstants.setBold(attributesCommand, true);
}

  void setVisible(boolean visible) {
    System.out.println("Console.setVisible(" + visible + ")");
    jf.setVisible(visible);
    input.requestFocus();
  }

  void output(String message) {
    output(message, null);
  }

  void output(String message, AttributeSet att) {
    if (message.charAt(message.length() - 1) != '\n')
      message += "\n";
    try {
      outputDocument.insertString(outputDocument.getLength(), message, att);
    } catch (BadLocationException ble) {
    }
    output.setCaretPosition(outputDocument.getLength());
  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == runButton) {
      execute();
    }
  }

  void execute() {
    String strCommand = input.getText();
    input.setText(null);
    output(strCommand, attributesCommand);
    String strErrorMessage = viewer.evalString(strCommand);
    if (strErrorMessage != null)
      output(strErrorMessage);
    input.requestFocus();
  }

  class ShiftEnterTextArea extends JTextArea {
    public void processComponentKeyEvent(KeyEvent ke) {
      switch (ke.getID()) {
      case KeyEvent.KEY_PRESSED:
        if (ke.getKeyCode() == KeyEvent.VK_ENTER && ke.isShiftDown()) {
          execute();
          return;
        }
        break;
      case KeyEvent.KEY_RELEASED:
        if (ke.getKeyCode() == KeyEvent.VK_ENTER && ke.isShiftDown())
          return;
        break;
      }
      super.processComponentKeyEvent(ke);
    }
  }

  ////////////////////////////////////////////////////////////////
  // window listener stuff to close when the window closes
  ////////////////////////////////////////////////////////////////
  
  public void windowActivated(WindowEvent we) {
  }

  public void windowClosed(WindowEvent we) {
    jvm12.console = null;
  }

  public void windowClosing(WindowEvent we) {
    jvm12.console = null;
  }

  public void windowDeactivated(WindowEvent we) {
  }

  public void windowDeiconified(WindowEvent we) {
  }

  public void windowIconified(WindowEvent we) {
  }

  public void windowOpened(WindowEvent we) {
  }

}
