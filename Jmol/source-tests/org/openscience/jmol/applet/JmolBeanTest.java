
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol.applet;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.event.WindowEvent;
import org.openscience.jmol.ReaderFactory;
import org.openscience.jmol.ChemFileReader;

public class JmolBeanTest {

  public static void main(String[] args) {

    JFrame myFrame = new JFrame();
    JmolSimpleBean myXMLThing1 = new JmolSimpleBean();
    JmolSimpleBean myXMLThing2 = new JmolSimpleBean();
    JPanel p2 = new JPanel(new BorderLayout());
    p2.add(myXMLThing2, "Center");
    p2.add(javax.swing.Box.createVerticalStrut(400), "East");
    p2.add(javax.swing.Box.createHorizontalStrut(400), "North");
    myFrame.getContentPane().setLayout(new BorderLayout());
    myFrame.getContentPane().add(myXMLThing1, "Center");
    myFrame.getContentPane().add(javax.swing.Box.createHorizontalStrut(400),
        "North");
    myFrame.getContentPane().add(p2, "East");
    try {
      java.io.Reader r1 = new java.io.FileReader("caffeine.xyz");
      ChemFileReader cfr1 = ReaderFactory.createReader(r1);
      myXMLThing1.setAtomPropertiesFromFile("AtomTypes");
      myXMLThing1.setModel(cfr1.read());

      java.io.Reader r2 = new java.io.FileReader("methanol1.cml");
      ChemFileReader cfr2 = ReaderFactory.createReader(r2);
      myXMLThing2.setAtomPropertiesFromFile("AtomTypes");
      myXMLThing2.setModel(cfr2.read());
    } catch (java.io.FileNotFoundException e) {
      System.out.println("File not found: " + e);
    } catch (java.io.IOException e) {
      System.out.println("IOException: " + e);
    }
    myFrame.pack();
    myFrame.show();
  }


  class Windowa implements java.awt.event.WindowListener {

    public void windowClosing(java.awt.event.WindowEvent e) {
      System.exit(0);
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }
  }

  public java.awt.Dimension getMinimumSize() {
    return new java.awt.Dimension(200, 200);
  }
}

