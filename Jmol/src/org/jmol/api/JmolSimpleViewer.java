/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
package org.jmol.api;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * This is the high-level API for the JmolViewer for simple access.
 **/

public interface JmolSimpleViewer {

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip);

  public String evalFile(String strFilename);
  public String evalString(String strScript);

  public void openStringInline(String strModel);
  public void openFile(String name);
  public String getOpenFileError();
}
