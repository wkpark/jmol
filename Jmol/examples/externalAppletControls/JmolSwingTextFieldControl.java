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
import javax.swing.*;
import java.awt.event.*;

public class JmolSwingTextFieldControl
  extends JApplet implements ActionListener {

  String myName;
  JmolAppletRegistry appletRegistry;
  String targetName;
  String label;
  String script;

  JTextField swingTextField;

  public void init() {
    myName = getParameterOrNull("name");
    appletRegistry = new JmolAppletRegistry(myName, false, this);
    targetName = getParameterOrNull("target");

    swingTextField = new JTextField();
    swingTextField.addActionListener(this);
    getContentPane().add(swingTextField);
  }

  private String getParameterOrNull(String paramName) {
    String value = getParameter(paramName);
    if (value != null && value.length() == 0)
      value = null;
    return value;
  }

  public void actionPerformed(ActionEvent e) {
    appletRegistry.script(targetName, swingTextField.getText());
  }
}
