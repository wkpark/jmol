package org.openscience.jmol.app.jmolpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class BannerFrame extends JFrame {

  public BannerFrame(int width, int height) {
    setTitle("Banner");
    setUndecorated(true);
    setBackground(Color.WHITE);
    setSize(width, height);
    setBounds(0, 0, width, height);
    bannerLabel = new JLabel("<html>type exitJmol[enter] to quit</html>", SwingConstants.CENTER);
    bannerLabel.setPreferredSize(getSize());
    bannerLabel.setFont(new Font("Helvetica", Font.BOLD, 30));
    getContentPane().add(bannerLabel, BorderLayout.CENTER);
    setVisible(true);
    setAlwaysOnTop(true);
  }

  private JLabel bannerLabel;
  
  public void setLabel(String label) {
    if (label != null)
      bannerLabel.setText(label);
    setVisible(label != null);
  }

}
