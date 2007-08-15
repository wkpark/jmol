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
import org.jmol.util.TextFormat;

public class ScriptButtons extends WebPanel {

  ScriptButtons(JmolViewer viewer, JFileChooser fc, WebPanel[] webPanels, int panelIndex) {
    super(viewer, fc, webPanels, panelIndex);
    description = "Create a web page containing a text and button pane scrolls next to a resizable Jmol applet";
    listLabel = "These names will be used for button labels";
    infoFile = "script_button_instructions.html";
    templateName = "script_button_template.html";
    appletTemplateName = "script_button_template2.html";
  }

  String fixHtml(String html) {
      int size = ((SpinnerNumberModel) (appletSizeSpinnerP.getModel()))
          .getNumber().intValue();
      html = TextFormat.simpleReplace(html, "@WIDTHPERCENT@", "" + size);
      return html;
  }
  
  String getAppletDefs(int i, String html, StringBuffer appletDefs, JmolInstance instance) {
    String name = instance.name;
    String buttonname = instance.name;
    String label = buttonname;
    if (i == 0)
      html = TextFormat.simpleReplace(html, "@APPLETNAME0@", buttonname);
    if (useAppletJS) {
      String info = "info for " + name;
      appletDefs.append("\naddAppletButton("+i+",'"+ name +"',\""+label+"\",\""+info+"\");");
    } else {
      String s = htmlAppletTemplate;    
      s = TextFormat.simpleReplace(s, "@NAME@", name);
      s = TextFormat.simpleReplace(s, "@LABEL@", label);
      appletDefs.append(s);
    }
    return html;
  }
}
