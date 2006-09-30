/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolSimpleViewer;
import org.jmol.util.Logger;

/**
 * A example of integrating the Jmol viewer into a java application.
 *
 * <p>I compiled/ran this code directly in the examples directory by doing:
 * <pre>
 * javac -classpath ../Jmol.jar Integration.java
 * java -cp .:../Jmol.jar Integration
 * </pre>
 *
 * @author Miguel <miguel@jmol.org>
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

    JmolSimpleViewer viewer = jmolPanel.getViewer();
    //    viewer.openFile("../samples/caffeine.xyz");
    //    viewer.openFile("http://database.server/models/1pdb.pdb.gz");
    viewer.openStringInline(strXyzHOH);
    viewer.evalString(strScript);
    String strError = viewer.getOpenFileError();
    if (strError != null)
      Logger.error(strError);
  }

  final static String strXyzHOH = 
    "3\n" +
    "water\n" +
    "O  0.0 0.0 0.0\n" +
    "H  0.76923955 -0.59357141 0.0\n" +
    "H -0.76923955 -0.59357141 0.0\n";

  final static String strScript = "delay; move 360 0 0 0 0 0 0 0 4;";

  static class ApplicationCloser extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      System.exit(0);
    }
  }

  static class JmolPanel extends JPanel {
    JmolSimpleViewer viewer;
    JmolAdapter adapter;
    JmolPanel() {
      adapter = new SmarterJmolAdapter(null);
      viewer = JmolSimpleViewer.allocateSimpleViewer(this, adapter);
    }

    public JmolSimpleViewer getViewer() {
      return viewer;
    }

    final Dimension currentSize = new Dimension();
    final Rectangle rectClip = new Rectangle();

    public void paint(Graphics g) {
      getSize(currentSize);
      g.getClipBounds(rectClip);
      viewer.renderScreenImage(g, currentSize, rectClip);
    }
  }
}
