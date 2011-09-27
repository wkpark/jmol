package org.openscience.jmol.app.jmolpanel;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class KioskFrame extends JFrame {

  public KioskFrame(int width, int height, JPanel kioskPanel) {
    setTitle("KioskFrame");
    setUndecorated(true);
    setBackground(new Color(0, 0, 0, 0));
    Container contentPane = getContentPane();
    contentPane.add(kioskPanel);
    setSize(width, height);
    setBounds(0, 0, width, height);
    setVisible(true);
  }
  
}
