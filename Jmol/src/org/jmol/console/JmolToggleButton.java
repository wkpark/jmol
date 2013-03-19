package org.jmol.console;

import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import org.jmol.api.JmolAbstractButton;

public class JmolToggleButton extends JToggleButton implements JmolAbstractButton {

  public JmolToggleButton(ImageIcon ii) {
    super(ii);
  }

  public void addConsoleListener(Object console) {
    addActionListener((ActionListener) console);
  }

  public String getKey() {
    return null;
  }
  

}
