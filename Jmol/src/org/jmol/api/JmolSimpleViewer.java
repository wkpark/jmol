/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.api;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;

import org.jmol.viewer.Viewer;

/**
 * This is the high-level API for the JmolViewer for simple access.
 **/

abstract public class JmolSimpleViewer {

  /**
   *  This is the main access point for creating an application
   *  or applet viewer. After allocation it is MANDATORY that one of 
   *  the next commands is either 
   *  
   *    viewer.evalString("ZAP");
   *    
   *    This command is necessary to establish the 
   *    first modelset, which might be required by one or more
   *    later evaluated commands or file loadings.
   *    
   * @param awtComponent
   * @param jmolAdapter
   * @return              a JmolViewer object
   */
  static public JmolSimpleViewer
    allocateSimpleViewer(Component awtComponent, JmolAdapter jmolAdapter) {
    JmolViewer viewer = Viewer.allocateViewer(awtComponent, jmolAdapter);
    viewer.setAppletContext("", null, null, "");
    return viewer;
  }

  abstract public void renderScreenImage(Graphics g, Dimension size,
                                         Rectangle clip);

  abstract public String evalFile(String strFilename);
  abstract public String evalString(String strScript);

  abstract public void openStringInline(String strModel);
  abstract public void openDOM(Object DOMNode);
  abstract public void openFile(String name);
  abstract public String getOpenFileError();
}
