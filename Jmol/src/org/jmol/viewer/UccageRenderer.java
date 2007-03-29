/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.viewer;

import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import java.text.NumberFormat;

import org.jmol.symmetry.UnitCell;
import org.jmol.util.TextFormat;

class UccageRenderer extends ShapeRenderer {

  NumberFormat nf;
  byte fid;
  boolean doLocalize;
  void initRenderer() {
  }

  final Point3i[] screens = new Point3i[8];
  final Point3f[] verticesT = new Point3f[8];  
  {
    for (int i = 8; --i >= 0; ) {
      screens[i] = new Point3i();
      verticesT[i] = new Point3f();
    }
  }

  void render() {
    short mad = viewer.getObjectMad(StateManager.OBJ_UNITCELL);
    if (mad == 0 || !g3d.setColix(viewer.getObjectColix(StateManager.OBJ_UNITCELL)))
        return;
    doLocalize = viewer.getUseNumberLocalization();
    render1(mad);
  }

  void render1(short mad) {
    if (frame.cellInfos == null)
      return;
    UnitCell unitCell = viewer.getCurrentUnitCell();
    if (unitCell == null)
      return;
    Frame.CellInfo cellInfo = frame.cellInfos[viewer.getDisplayModelIndex()];
    Point3f[] vertices = unitCell.getVertices();
    Point3f offset = unitCell.getCartesianOffset();
    for (int i = 8; --i >= 0;)
      verticesT[i].add(vertices[i], offset);
    int firstLine = (viewer.getObjectMad(StateManager.OBJ_AXIS1) == 0  || viewer.getAxesMode() != JmolConstants.AXES_MODE_UNITCELL ? 0 : 3);
    BbcageRenderer.render(viewer, g3d, mad, verticesT, screens, firstLine);
    if (!viewer.getDisplayCellParameters() || cellInfo.periodicOriginXyz != null)
      return;
    if (nf == null) {
      nf = NumberFormat.getInstance();
      fid = g3d.getFontFid("Monospaced", 14);
    }
    if (nf != null) {
      nf.setMaximumFractionDigits(3);
      nf.setMinimumFractionDigits(3);
    }
    g3d.setFont(fid);
    int line = 15;
    int lineheight = 15;
    if (cellInfo.spaceGroup != null) {
      line += lineheight;
      g3d.drawStringNoSlab(cellInfo.spaceGroup, null, (short) 0, 5, line, 0);
    }
    line += lineheight;
    g3d.drawStringNoSlab("a=" + nfformat(unitCell.getInfo(UnitCell.INFO_A))
        + "\u00C5", null, (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("b=" + nfformat(unitCell.getInfo(UnitCell.INFO_B))
        + "\u00C5", null, (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("c=" + nfformat(unitCell.getInfo(UnitCell.INFO_C))
        + "\u00C5", null, (short) 0, 5, line, 0);
    if (nf != null)
      nf.setMaximumFractionDigits(1);
    line += lineheight;
    g3d.drawStringNoSlab("\u03B1="
        + nfformat(unitCell.getInfo(UnitCell.INFO_ALPHA)) + "\u00B0", null,
        (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("\u03B2="
        + nfformat(unitCell.getInfo(UnitCell.INFO_BETA)) + "\u00B0", null,
        (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("\u03B3="
        + nfformat(unitCell.getInfo(UnitCell.INFO_GAMMA)) + "\u00B0", null,
        (short) 0, 5, line, 0);
  }
  
  String nfformat(float x) {
    return (doLocalize && nf != null ? nf.format(x) : TextFormat.formatDecimal(x, 3));
  }

}

