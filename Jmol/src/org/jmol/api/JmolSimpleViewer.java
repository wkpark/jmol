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
   *  or applet viewer. 
   *    
   * @param awtComponent
   * @param jmolAdapter
   * @return              a JmolViewer object
   */
  static public JmolSimpleViewer
    allocateSimpleViewer(Component awtComponent, JmolAdapter jmolAdapter) {
    return Viewer.allocateViewer(awtComponent, jmolAdapter, 
        null, null, null, null, null);
  }

  abstract public void renderScreenImage(Graphics g, Dimension size,
                                         Rectangle clip);

  abstract public String evalFile(String strFilename);
  abstract public String evalString(String strScript);

  abstract public String openStringInline(String strModel);
  abstract public String openDOM(Object DOMNode);
  abstract public String openFile(String fileName);
  //File reading is ASYNCHRONOUS
  //NOT what you think it was....  abstract public String getOpenFileError();
  //Use jmolStatusListener to trap script termination errors for openFile
  //Use (String) getProperty(null, "ErrorMessage", null) for openStringInline or openDOM
  abstract public Object getProperty(String returnType, String infoType, String paramInfo);
}
