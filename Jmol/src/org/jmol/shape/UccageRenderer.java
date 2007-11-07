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
package org.jmol.shape;

import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import java.text.NumberFormat;

import org.jmol.modelset.CellInfo;
import org.jmol.symmetry.UnitCell;
import org.jmol.util.TextFormat;
import org.jmol.viewer.StateManager;

public class UccageRenderer extends FontLineShapeRenderer {

  NumberFormat nf;
  byte fid;
  boolean doLocalize;
  
  final Point3i[] screens = new Point3i[8];
  final Point3f[] verticesT = new Point3f[8];  
  {
    for (int i = 8; --i >= 0; ) {
      screens[i] = new Point3i();
      verticesT[i] = new Point3f();
    }
  }

  protected void render() {
    short mad = viewer.getObjectMad(StateManager.OBJ_UNITCELL);
    colix = viewer.getObjectColix(StateManager.OBJ_UNITCELL);
    if (mad == 0 || !g3d.setColix(colix) || viewer.isJmolDataFrame())
      return;
    doLocalize = viewer.getUseNumberLocalization();
    render1(mad);
  }

  void render1(short mad) {
    CellInfo[] cellInfos = modelSet.getCellInfos();
    if (cellInfos == null)
      return;
    UnitCell unitCell = viewer.getCurrentUnitCell();
    if (unitCell == null)
      return;
    CellInfo cellInfo = cellInfos[viewer.getDisplayModelIndex()];
    Point3f[] vertices = unitCell.getVertices();
    Point3f offset = unitCell.getCartesianOffset();
    for (int i = 8; --i >= 0;)
      verticesT[i].add(vertices[i], offset);
    Point3f[] axisPoints = viewer.getAxisPoints();
    boolean drawAllLines = (viewer.getObjectMad(StateManager.OBJ_AXIS1) == 0 
        || viewer.getAxesScale() < 2);
    render(mad, verticesT, screens, axisPoints, drawAllLines ? 0 : 3);
    if (viewer.getDisplayCellParameters() && !cellInfo.isPeriodic())
      renderInfo(cellInfo, unitCell);
  }
  
  private String nfformat(float x) {
    return (doLocalize && nf != null ? nf.format(x) : TextFormat.formatDecimal(x, 3));
  }

  private void renderInfo(CellInfo cellInfo, UnitCell unitCell) {
    if (isGenerator)
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
    int lineheight = 15;
    int x = 5;
    if (antialias) {
      lineheight <<= 1;
      x <<= 1;
    }
    int y = lineheight;
    
    String spaceGroup = cellInfo.getSpaceGroup(); 
    if (spaceGroup != null) {
      y += lineheight;
      g3d.drawStringNoSlab(spaceGroup, null, x, y, 0);
    }
    y += lineheight;
    g3d.drawStringNoSlab("a=" + nfformat(unitCell.getInfo(UnitCell.INFO_A))
        + "\u00C5", null, x, y, 0);
    y += lineheight;
    g3d.drawStringNoSlab("b=" + nfformat(unitCell.getInfo(UnitCell.INFO_B))
        + "\u00C5", null, x, y, 0);
    y += lineheight;
    g3d.drawStringNoSlab("c=" + nfformat(unitCell.getInfo(UnitCell.INFO_C))
        + "\u00C5", null, x, y, 0);
    if (nf != null)
      nf.setMaximumFractionDigits(1);
    y += lineheight;
    g3d.drawStringNoSlab("\u03B1="
        + nfformat(unitCell.getInfo(UnitCell.INFO_ALPHA)) + "\u00B0", null,
        x, y, 0);
    y += lineheight;
    g3d.drawStringNoSlab("\u03B2="
        + nfformat(unitCell.getInfo(UnitCell.INFO_BETA)) + "\u00B0", null,
        x, y, 0);
    y += lineheight;
    g3d.drawStringNoSlab("\u03B3="
        + nfformat(unitCell.getInfo(UnitCell.INFO_GAMMA)) + "\u00B0", null,
        x, y, 0);
  }
}

