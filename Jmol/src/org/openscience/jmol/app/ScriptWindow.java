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
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ScriptWindow extends JDialog
    implements ActionListener {

  private JTextArea output;
  private JTextField input;
  private JButton closeButton;
  DisplayControl control;

  public ScriptWindow(DisplayControl control, JFrame frame) {

    super(frame, "Rasmol Scripts", false);
    this.control = control;
    getContentPane().setLayout(new BorderLayout());
    output = new JTextArea(20, 30);
    output.setEditable(false);
    output.append("> ");
    JScrollPane scrollPane = new JScrollPane(output);
    getContentPane().add(scrollPane, BorderLayout.NORTH);

    //scriptHandler.setOutput(output);

    input = new JTextField();
    input.addActionListener(this);
    getContentPane().add(input, BorderLayout.CENTER);
    closeButton = new JButton("Close");
    closeButton.addActionListener(this);
    getContentPane().add(closeButton, BorderLayout.SOUTH);
    setLocationRelativeTo(frame);
    pack();
  }

  public void scriptEcho(String strEcho) {
    if (strEcho != null) {
      output.append(strEcho);
      output.append("\n");
    }
  }

  public void notifyScriptTermination(String strMsg, int msWalltime) {
    if (strMsg != null) {
      output.append(strMsg);
      output.append("\n");
    }
    output.append("> ");
  }

  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == closeButton) {
      System.out.println("closeButton");
      hide();
    } else {
      String command = input.getText();
      output.append(command);
      output.append("\n");
      input.setText(null);

      String strErrorMessage = control.evalString(command);
      if (strErrorMessage != null) {
        output.append(strErrorMessage);
        output.append("\n> ");
      } else {
        output.append("!!");
      }
    }
  }

  public void hide() {
    //    eval.haltExecution();
    super.hide();
  }
}
