/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
 *
 * This demonstration code is released to the public domain and may be
 * used for any purpose
 *
 */

import org.jmol.applet.JmolAppletRegistry;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class JmolSwingTextAreaControl
  extends JApplet implements ActionListener {

  String myName;
  JmolAppletRegistry appletRegistry;
  String targetName;
  String label;
  String script;

  JTextArea swingTextArea;
  JButton swingButton;

  public void init() {
    myName = getParameterOrNull("name");
    appletRegistry = new JmolAppletRegistry(myName, false, this);
    targetName = getParameterOrNull("target");

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    swingTextArea = new JTextArea();
    System.out.println("here we go with swingTextArea:" + swingTextArea);
    panel.add(swingTextArea, BorderLayout.CENTER);
    swingButton = new JButton("Go!");
    swingButton.addActionListener(this);
    panel.add(swingButton, BorderLayout.SOUTH);
    getContentPane().add(panel);
  }

  private String getParameterOrNull(String paramName) {
    String value = getParameter(paramName);
    if (value != null && value.length() == 0)
      value = null;
    return value;
  }

  public void actionPerformed(ActionEvent e) {
    appletRegistry.script(targetName, swingTextArea.getText());
  }
}
