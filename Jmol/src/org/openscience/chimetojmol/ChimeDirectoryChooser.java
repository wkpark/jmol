package org.openscience.chimetojmol;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;



public class ChimeDirectoryChooser extends JPanel
   implements ActionListener {
   JButton go;
   
   String choosertitle;
   
  public ChimeDirectoryChooser() {
    go = new JButton("Do it");
    go.addActionListener(this);
    add(go);
   }

  public void actionPerformed(ActionEvent e) {
  }   
  @Override
  public Dimension getPreferredSize(){
    return new Dimension(200, 200);
    }

}