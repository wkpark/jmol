package org.openscience.jmol.app.jmolpanel;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class KioskFrame extends JFrame {

  public KioskFrame(int x, int y, int width, int height, JPanel kioskPanel) {
    setTitle("KioskFrame");
    setUndecorated(true);
    setBackground(new Color(0, 0, 0, 0));
    setPanel(kioskPanel);
    setSize(width, height);
    setBounds(x, y, width, height);
    setVisible(true);
  }

  void setPanel(JPanel kioskPanel) {
    if (kioskPanel == null)
      return;
    getContentPane().add(kioskPanel);
  }
  
}
