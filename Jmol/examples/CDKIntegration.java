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
import org.jmol.api.*;
import org.jmol.adapter.cdk.CdkJmolAdapter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A example of integrating the Jmol viewer into a CDK application.
 *
 * <p>I compiled/ran this code directly in the examples directory by doing:
 * <pre>
 * javac -classpath ../Jmol.jar CDKIntegration.java
 * java -cp .:../Jmol.jar:../../../CDK/cdk/dist/jar/cdk-all.jar CDKIntegration
 * </pre>
 *
 * @author Miguel <mth@mth.com>
 * @author Egon <egonw@jmol.org>
 */
public class CDKIntegration {
    
    public static void main(String[] argv) {
        
        if (argv.length != 1) {
            System.out.println("Syntax: CDKIntegration <filename>");
            System.exit(-1);
        }
        
        String filename = argv[0];
        
        JFrame frame = new JFrame("CDK Integration Example");
        frame.addWindowListener(new ApplicationCloser());
        Container contentPane = frame.getContentPane();
        JmolPanel jmolPanel = new JmolPanel();
        contentPane.add(jmolPanel);
        frame.setSize(300, 300);
        frame.setVisible(true);
        
        JmolViewer viewer = jmolPanel.getViewer();
        viewer.openFile(filename);
        String strError = viewer.getOpenFileError();
        if (strError != null)
            System.out.println(strError);

        viewer.evalString(strScript);
    }
    
    final static String strScript = "select *; spacefill on;";
}

class ApplicationCloser extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }
}

class JmolPanel extends JPanel {
    JmolViewer viewer;
    JmolAdapter adapter;
    JmolPanel() {
        // use CDK IO
        adapter = new CdkJmolAdapter(null);
        viewer = JmolViewer.allocateViewer(this, adapter);
    }
    
    public JmolViewer getViewer() {
        return viewer;
    }
    
    final Dimension currentSize = new Dimension();
    final Rectangle rectClip = new Rectangle();
    
    public void paint(Graphics g) {
        viewer.setScreenDimension(getSize(currentSize));
        g.getClipBounds(rectClip);
        viewer.renderScreenImage(g, currentSize, rectClip);
    }
}
