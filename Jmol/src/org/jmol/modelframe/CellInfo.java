/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

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

package org.jmol.modelframe;

import org.jmol.util.Logger;
import org.jmol.symmetry.UnitCell;

import javax.vecmath.Point3f;
import java.util.Hashtable;

public class CellInfo {

  boolean coordinatesAreFractional;
  String spaceGroup;
  String[] symmetryOperations;
  String symmetryInfoString;

  private int modelIndex;
  private UnitCell unitCell;
  private Point3f periodicOriginXyz;
  private int[] cellRange;

  public boolean isPeriodic() {
    return periodicOriginXyz != null;
  }

  CellInfo(int modelIndex, boolean doPdbScale, Hashtable info) {
    float[] notionalUnitcell = (float[]) info.get("notionalUnitcell");
    cellRange = (int[]) info.get("unitCellRange");
    this.modelIndex = modelIndex;
    periodicOriginXyz = (Point3f) info.get("periodicOriginXyz");
    spaceGroup = (String) info.get("spaceGroup");
    if (spaceGroup == null || spaceGroup == "")
      spaceGroup = "spacegroup unspecified";
    int symmetryCount = info.containsKey("symmetryCount") ? 
        ((Integer) info.get("symmetryCount")).intValue() 
        : 0;
    symmetryOperations = (String[]) info.get("symmetryOperations");
    symmetryInfoString = "Spacegroup: " + spaceGroup;
    if (symmetryOperations == null) {
      symmetryInfoString += "\nNumber of symmetry operations: ?"
          + "\nSymmetry Operations: unspecified\n";
    } else {
      symmetryInfoString += "\nNumber of symmetry operations: "
          + (symmetryCount == 0 ? 1 : symmetryCount) + "\nSymmetry Operations:";
      for (int i = 0; i < symmetryCount; i++)
        symmetryInfoString += "\n" + symmetryOperations[i];
    }
    symmetryInfoString += "\n";
    coordinatesAreFractional = info.containsKey("coordinatesAreFractional") ? 
        ((Boolean) info.get("coordinatesAreFractional")).booleanValue() 
        : false;
    if (notionalUnitcell == null || notionalUnitcell[0] == 0)
      return;
    unitCell = new UnitCell(notionalUnitcell);
    showInfo();
  }

  public UnitCell getUnitCell() {
    return unitCell;
  }

  int[] getCellRange() {
    return cellRange;
  }

  float[] getNotionalUnitCell() {
    return (unitCell == null ? null : unitCell.getNotionalUnitCell());
  }

  void toCartesian(Point3f pt) {
    if (unitCell == null)
      return;
    unitCell.toCartesian(pt);
  }

  void toFractional(Point3f pt) {
    if (unitCell == null)
      return;
    unitCell.toFractional(pt);
  }

  void toUnitCell(Point3f pt, Point3f offset) {
    if (!coordinatesAreFractional || symmetryOperations == null)
      return;
    unitCell.toUnitCell(pt, offset);
  }

  void showInfo() {
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger
          .debug("cellInfos[" + modelIndex + "]:\n" + unitCell.dumpInfo(true));
  }

  String getUnitCellInfo() {
    return (unitCell == null ? "no unit cell information" : unitCell
        .dumpInfo(false));
  }

  public String getSpaceGroup() {
    return spaceGroup;
  }
}
