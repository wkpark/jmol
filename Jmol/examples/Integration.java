/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;
import org.openscience.jmol.viewer.JmolStatusListener;

import org.openscience.jmol.adapters.SimpleModelAdapter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A example of integrating the Jmol viewer into a java application <p>
 *
 * I compiled/ran this code directly in the examples directory by doing: <br>
 *
 * javac -classpath ../jmol.jar Integration.java                         <br>
 * java -cp .:../jmol.jar Integration                                    <br>
 *
 * @author Miguel <mth@mth.com>
 */

public class Integration {

  public static void main(String[] argv) {
    JFrame frame = new JFrame("Hello");
    frame.addWindowListener(new ApplicationCloser());
    Container contentPane = frame.getContentPane();
    JmolPanel jmolPanel = new JmolPanel();
    contentPane.add(jmolPanel);
    frame.setSize(300, 300);
    frame.setVisible(true);

    JmolViewer viewer = jmolPanel.getViewer();
    //    viewer.openFile("../samples/caffeine.xyz");
    //    viewer.openFile("http://database.server/models/1pdb.pdb.gz");
    viewer.openStringInline(strHOH);
    viewer.evalString(strScript);
    String strError = viewer.getOpenFileError();
    if (strError != null)
      System.out.println(strError);
  }

  final static String strHOH =
    "3\n" +
    "water\n" +
    "O  0.0 0.0 0.0\n" +
    "H  0.76923955 -0.59357141 0.0\n" +
    "H -0.76923955 -0.59357141 0.0\n";

  final static String strScript = "delay; move 360 0 0 0 0 0 0 0 4;"
}

class ApplicationCloser extends WindowAdapter {
  public void windowClosing(WindowEvent e) {
    System.exit(0);
  }
}

class JmolPanel extends JPanel {
  JmolViewer viewer;
  JmolModelAdapter adapter;
  JmolPanel() {
    adapter = new SimpleModelAdapter();
    viewer = new JmolViewer(this, adapter);
  }

  public JmolViewer getViewer() {
    return viewer;
  }

  public void paint(Graphics g) {
    viewer.setScreenDimension(getSize());
    g.drawImage(viewer.renderScreenImage(g.getClipBounds()), 0, 0, null);
  }
}
