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
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

public class JmolAwtButtonControl extends Applet implements ActionListener {

  String myName;
  JmolAppletRegistry appletRegistry;
  String targetName;
  String label;
  String script;

  Button awtButton;

  public void init() {
    myName = getParameterOrNull("name");
    appletRegistry = new JmolAppletRegistry(myName, false, this);
    targetName = getParameterOrNull("target");

    label = getParameterOrNull("label");
    script = getParameterOrNull("script");
    if (label == null && script != null)
      label = script.substring(0, Math.min(20, script.length()));

    setLayout(new GridLayout(1, 1));
    awtButton = new Button(label);
    awtButton.addActionListener(this);
    add(awtButton);
  }

  private String getParameterOrNull(String paramName) {
    String value = getParameter(paramName);
    if (value != null && value.length() == 0)
      value = null;
    return value;
  }

  public void actionPerformed(ActionEvent e) {
    appletRegistry.script(targetName, script);
  }
}
