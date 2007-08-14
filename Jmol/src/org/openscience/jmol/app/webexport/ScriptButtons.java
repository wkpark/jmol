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

import java.awt.*;
import javax.swing.*;

import org.jmol.api.JmolViewer;
import org.jmol.util.TextFormat;

public class ScriptButtons extends WebPanel {

  ScriptButtons(JmolViewer viewer) {
    super(viewer);
    description = "Create a web page where a text and button pane scrolls next to a resizable Jmol.";
    infoFile = "script_button_instructions.html";
    templateName = "script_button_template.html";
  }

  //Need the panel maker and the action listener.
  public JPanel getPanel() {

    //Create the appletSize spinner so the user can decide what %
    // of the window width the applet should be.
    SpinnerNumberModel appletSizeModelW = new SpinnerNumberModel(60, //initial value
        20, //min
        100, //max
        5); //step size
    appletSizeSpinnerW = new JSpinner(appletSizeModelW);
    //panel to hold spinner and label
    JPanel appletSizePanel = new JPanel();
    appletSizePanel.add(new JLabel("% of window for applet width:"));
    appletSizePanel.add(appletSizeSpinnerW);
/*    SpinnerNumberModel appletSizeModelH = new SpinnerNumberModel(60, //initial value
        20, //min
        100, //max
        5); //step size
    appletSizeSpinnerH = new JSpinner(appletSizeModelH);
    appletSizePanel.add(new JLabel("height:"));
    appletSizePanel.add(appletSizeSpinnerH);
*/
    //Text info on the name choices for the instance
    JEditorPane nameChoice = new JEditorPane();
    nameChoice.setEditable(false);
    nameChoice.setPreferredSize(new Dimension(300, 1));
    nameChoice.setText("The names you choose will be used as the button labels");


    //Create the overall panel
    JPanel PopInPanel = getPanel(nameChoice, appletSizePanel);
    return PopInPanel;
  }

  String fixHtml(String html) {
      int size = ((SpinnerNumberModel) (appletSizeSpinnerW.getModel()))
          .getNumber().intValue();
      html = TextFormat.simpleReplace(html, "@WIDTHPERCENT@", "" + size);
      return html;
  }
  
  String getAppletDefs(int i, String html, StringBuffer appletDefs, JmolInstance instance) {
    String name = instance.name;
    String buttonname = instance.name;
    name = TextFormat.simpleReplace(buttonname, " ", "_");
    String label;
    if (i == 0) {
      html = TextFormat.simpleReplace(html, "@APPLETNAME0@", buttonname);
      label = "reset view";
    } else {
      label = buttonname;
    }
    String info = "info for " + name;
    appletDefs.append("\naddAppletButton("+i+",'"+ name +"',\""+label+"\",\""+info+"\");");
    return html;
  }

  String fixScript(String script) {
    script = TextFormat.simpleReplace(script, "set refreshing false;",
    "set refreshing true;");
    script = TextFormat.simpleReplace(script,
    "moveto /* time, axisAngle */ 0.0",
    "moveto /* time, axisAngle */ 5.0");
    return script;    
  }

}
