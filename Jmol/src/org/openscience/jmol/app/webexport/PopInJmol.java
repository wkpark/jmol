/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;

import javax.swing.*;
import org.jmol.api.JmolViewer;


public class PopInJmol extends WebPanel {

  PopInJmol(JmolViewer viewer) {
    super(viewer);
    description = "Create a web page with images that convert to live Jmol on user click...";
    infoFile = "pop_in_instructions.html";
    templateName = "pop_in_template.html";
  }

  //Need the panel maker and the action listener.
  public JPanel getPanel() {

    //Create the appletSize spinner so the user can decide how big
    //the applet should be.
    SpinnerNumberModel appletSizeModelW = new SpinnerNumberModel(300, //initial value
        50, //min
        500, //max
        25); //step size
    SpinnerNumberModel appletSizeModelH = new SpinnerNumberModel(300, //initial value
        50, //min
        500, //max
        25); //step size
    appletSizeSpinnerW = new JSpinner(appletSizeModelW);
    appletSizeSpinnerH = new JSpinner(appletSizeModelH);

    //panel to hold spinner and label
    JPanel appletSizePanel = new JPanel();
    appletSizePanel.add(new JLabel("Applet width:"));
    appletSizePanel.add(appletSizeSpinnerW);
    appletSizePanel.add(new JLabel("height:"));
    appletSizePanel.add(appletSizeSpinnerH);

    return getPanel(appletSizePanel, null);
  }

  String getAppletDefs(int i, String html, StringBuffer appletDefs, JmolInstance instance) {
    String name = instance.name;
    int JmolSizeW = instance.width;
    int JmolSizeH = instance.height;
    appletInfoDivs += "\n<div id=\""+name+"_caption\">\ninsert caption for "+name+" here\n</div>";
    appletInfoDivs += "\n<div id=\""+name+"_note\">\ninsert note for "+name+" here\n</div>";
    String floatDiv = (i % 2 == 0 ? "floatRightDiv" : "floatLeftDiv");
    appletDefs.append("\naddJmolDiv(" + i + ",'"+floatDiv+"','" + name + "',"
        + JmolSizeW + "," + JmolSizeH + ")");
    return html;
  }
}
