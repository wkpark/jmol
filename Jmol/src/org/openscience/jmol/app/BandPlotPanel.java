package org.openscience.jmol.app;
import javax.swing.*;
import java.awt.*;

public class BandPlotPanel extends JPanel {
    
    BandPlotG2DRenderer bpg2r;
    
    public BandPlotPanel(BandPlotG2DRenderer bpg2r) {
      this.bpg2r = bpg2r;
    }

    public void paint(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      bpg2r.setGraphics2D(g2);
      super.paintComponent(g2); //paint background
      setBackground(Color.white);
      bpg2r.render();
    }
  } 
