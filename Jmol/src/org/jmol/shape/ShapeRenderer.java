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

package org.jmol.shape;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelframe.ModelSet;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;
//import java.awt.Rectangle;

public abstract class ShapeRenderer {

  protected Viewer viewer;
  protected int myVisibilityFlag;
  protected int shapeID;

  public final void setViewerG3dShapeID(Viewer viewer, Graphics3D g3d, int shapeID) {
    this.viewer = viewer;
    this.g3d = g3d;
    this.shapeID = shapeID;
    myVisibilityFlag = JmolConstants.getShapeVisibilityFlag(shapeID);
    initRenderer();
  }

  protected void initRenderer() {
  }

  protected Graphics3D g3d;
  //Rectangle rectClip; //not implemented
  protected ModelSet modelSet;
  protected Shape shape;

  public void render(Graphics3D g3d, ModelSet modelSet, Shape shape) { //, Rectangle rectClip
    this.g3d = g3d;
    //this.rectClip = rectClip; //not implemented -- could be a place for optimization
    this.modelSet = modelSet;
    this.shape = shape;
    render();
  }

  abstract protected void render();
}

