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

package org.jmol.viewer;

import org.jmol.util.Logger;
import org.jmol.g3d.Graphics3D;
import java.util.BitSet;
import java.util.Hashtable;

class Dots extends AtomShape {

  EnvelopeCalculation ec;
  
  final static float SURFACE_DISTANCE_FOR_CALCULATION = 10f;

  BitSet bsOn = new BitSet();
  BitSet bsSelected;
  
  static int MAX_LEVEL = EnvelopeCalculation.MAX_LEVEL;
  
  int thisAtom;
  float thisRadius;
  int thisArgb;

  //short mad = 0;
  short lastMad = 0;
  float lastSolventRadius = 0;
  
  long timeBeginExecution;
  long timeEndExecution;
  int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  void initShape() {

    //these next two are for the geodesic fragment at a distance

    translucentAllowed = false; //except for geosurface
    super.initShape();
    ec = new EnvelopeCalculation(frame, atoms, mads);
    
  }

  boolean isSurface = false;
  
  void setProperty(String propertyName, Object value, BitSet bs) {

    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("Dots.setProperty: " + propertyName + " " + value);
    }

    if ("init" == propertyName) {
      initialize();
      return;
    }

    if ("translucency" == propertyName) {
      return; // no translucent dots
    }

    if ("ignore" == propertyName) {
      ec.setIgnore((BitSet) value);
      return;
    }

    if ("select" == propertyName) {
      bsSelected = (BitSet) value;
      return;
    }

    // next four are for serialization
    if ("radius" == propertyName) {
      thisRadius = ((Float) value).floatValue();
      return;
    }
    if ("colorRGB" == propertyName) {
      thisArgb = ((Integer) value).intValue();
      return;
    }
    if ("atom" == propertyName) {
      thisAtom = ((Integer) value).intValue();
      atoms[thisAtom].setShapeVisibility(myVisibilityFlag, true);
      ec.dotsConvexMax = Math.max(thisAtom + 1, ec.dotsConvexMax);
      return;
    }
    if ("dots" == propertyName) {
      isActive = true;
      ec.setFromBits(thisAtom, (BitSet) value);
      atoms[thisAtom].setShapeVisibility(myVisibilityFlag, true);
      if (mads == null) {
        mads = new short[atomCount];
        for (int i = 0; i < atomCount; i++)
          if (atoms[i].isShapeVisible(myVisibilityFlag))
            mads[i] = (short) (ec.getAppropriateRadius(atoms[i]) * 1000);
        ec.setMads(mads);
      }
      mads[thisAtom] = (short) (thisRadius * 1000f);
      if (colixes == null) {
        colixes = new short[atomCount];
        paletteIDs = new byte[atomCount];
      }
      colixes[thisAtom] = Graphics3D.getColix(thisArgb);
      //all done!
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  void setSuperProperty(String propertyName, Object value, BitSet bs) {
    super.setProperty(propertyName, value, bs);
  }
  
  void initialize() {
    bsSelected = null;
    isActive = false;
    if (ec == null)
      ec = new EnvelopeCalculation(frame, atoms, mads);
    ec.initialize();
  }
  
  void setSize(int size, BitSet bsSelected) {
    if (this.bsSelected != null)
      bsSelected = this.bsSelected;

    // if mad == 0 then turn it off
    //    1           van der Waals (dots) or +1.2, calconly)
    //   -1           ionic/covalent
    // 2 - 1001       (mad-1)/100 * van der Waals
    // 1002 - 11002    (mad - 1002)/1000 set radius 0.0 to 10.0 angstroms
    // 11003- 13002    (mad - 11002)/1000 set radius to vdw + additional radius 

    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("Dots.setSize " + size);
    }
    boolean isVisible = true;
    float addRadius = Float.MAX_VALUE;
    float setRadius = Float.MAX_VALUE;
    boolean useVanderwaalsRadius = true;
    float scale = 1;

    isActive = true;
    short mad = (short) size;
    if (mad < 0) { // ionic
      useVanderwaalsRadius = false;
    } else if (mad == 0) {
      isVisible = false;
    } else if (mad == 1) {
    } else if (mad <= 1001) {
      scale = (mad - 1) / 100f;
    } else if (mad <= 11002) {
      useVanderwaalsRadius = false;
      setRadius = (mad - 1002) / 1000f;
    } else if (mad <= 13002) {
      addRadius = (mad - 11002) / 1000f;
      scale = 1;
    }
    float maxRadius = !useVanderwaalsRadius ? setRadius : frame
        .getMaxVanderwaalsRadius();
    float solventRadius = viewer.getCurrentSolventProbeRadius();
    if (addRadius == Float.MAX_VALUE)
      addRadius = (solventRadius != 0 ? solventRadius : 0);

    timeBeginExecution = System.currentTimeMillis();

    // combine current and selected set
    boolean newSet = (lastSolventRadius != addRadius || mad != 0
        && mad != lastMad || ec.dotsConvexMax == 0);

    // for an solvent-accessible surface there is no torus/cavity issue. 
    // we just increment the atom radius and set the probe radius = 0;

    if (isVisible) {
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i) && !bsOn.get(i)) {
          bsOn.set(i);
          newSet = true;
        }
    } else {
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          bsOn.set(i, false);
    }

    for (int i = atomCount; --i >= 0;) {
      atoms[i].setShapeVisibility(myVisibilityFlag, bsOn.get(i));
    }
    if (newSet) {
      mads = null;
      ec.newSet();
      lastSolventRadius = addRadius;
    }
    // always delete old surfaces for selected atoms

    if (isVisible && ec.dotsConvexMaps != null) {
      for (int i = atomCount; --i >= 0;)
        if (bsOn.get(i)) {
          ec.dotsConvexMaps[i] = null;
        }
    }
    // now, calculate surface for selected atoms
    if (isVisible) {
      lastMad = mad;
      if (ec.dotsConvexMaps == null) {
        colixes = new short[atomCount];
        paletteIDs = new byte[atomCount];
      }
      boolean disregardNeighbors = (viewer.getDotSurfaceFlag() == false);
      boolean onlySelectedDots = (viewer.getDotsSelectedOnlyFlag() == true);
      ec.setSelected(bsOn);
      ec
          .calculate(addRadius, setRadius, scale, maxRadius,
              useVanderwaalsRadius, disregardNeighbors, onlySelectedDots,
              isSurface);
    }
    timeEndExecution = System.currentTimeMillis();
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("dots generation time = " + getExecutionWalltime());
    }
  }

  void setModelClickability() {
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(i))
        continue;
      atom.clickabilityFlags |= myVisibilityFlag;
    }
  }

  String getShapeState() {
    if (ec.dotsConvexMaps == null || ec.dotsConvexMax == 0)
      return "";
    StringBuffer s = new StringBuffer();
    Hashtable temp = new Hashtable();
    int atomCount = viewer.getAtomCount();
    String type = (isSurface ? "geoSurface " : "dots ");
    for (int i = 0; i < atomCount; i++) {
      if (ec.dotsConvexMaps[i] == null
          || !atoms[i].isShapeVisible(myVisibilityFlag))
        continue;
      if (!isSurface && bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp, i, getColorCommand(type, paletteIDs[i], colixes[i]));
      BitSet bs = new BitSet();
      int[] map = ec.dotsConvexMaps[i];
      int iDot = map.length << 5;
      int n = 0;
      while (--iDot >= 0)
        if (EnvelopeCalculation.getBit(map, iDot)) {
          n++;
          bs.set(iDot);
        }
      if (n > 0) {
        appendCmd(s, type + i + " radius " + ec.getAppropriateRadius(atoms[i])
            + " " + StateManager.escape(bs));
      }
    }
    s.append(getShapeCommands(temp, null, atomCount));
    return s.toString();
  }

}
