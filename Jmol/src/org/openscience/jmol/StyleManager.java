/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol;

import org.openscience.jmol.render.AtomShape;

import java.awt.Font;
import java.util.BitSet;

public class StyleManager {

  DisplayControl control;

  public StyleManager(DisplayControl control) {
    this.control = control;
  }

  public void initializeAtomShapes() {
    JmolAtomIterator iter = control.getChemFileIterator();
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      atom.setAtomShape(new AtomShape(atom,
                                      styleAtom, -percentVdwAtom,
                                      styleBond, percentAngstromBond * 10,
                                      control.getColorAtom(atom),
                                      control.getLabelAtom(atom)));
    }
  }

  public byte styleAtom = DisplayControl.QUICKDRAW;
  public void setStyleAtom(byte styleAtom, boolean setDefault,
                           JmolAtomIterator iter) {
    if (setDefault)
      this.styleAtom = styleAtom;
    while (iter.hasNext())
      iter.nextAtom().atomShape.setStyleAtom(styleAtom);
  }

  public int percentVdwAtom = 20;
  public void setPercentVdwAtom(int percentVdwAtom, boolean setDefault,
                           JmolAtomIterator iter) {
    if (setDefault)
      this.percentVdwAtom = percentVdwAtom;
    while (iter.hasNext())
      iter.nextAtom().atomShape.setMadAtom(-percentVdwAtom);
  }

  public byte styleBond = DisplayControl.QUICKDRAW;
  public void setStyleBond(byte styleBond, boolean setDefault,
                           JmolAtomIterator iter) {
    if (setDefault)
      this.styleBond = styleBond;
    while (iter.hasNext()) {
      if (iter.allBonds())
        iter.nextAtom().atomShape.setStyleAllBonds(styleBond);
      else
        iter.nextAtom().atomShape.setStyleBond(styleBond, iter.indexBond());
    }
  }

  public int percentAngstromBond = 10;
  public void setPercentAngstromBond(int percentAngstromBond,
                                     boolean setDefault,
                                     JmolAtomIterator iter) {
    if (setDefault)
      this.percentAngstromBond = percentAngstromBond;
    int mad = percentAngstromBond * 10;
    while (iter.hasNext()) {
      if (iter.allBonds())
        iter.nextAtom().atomShape.setMadAllBonds(mad);
      else
        iter.nextAtom().atomShape.setMadBond(mad, iter.indexBond());
    }
  }

  public boolean showAtoms = true;
  public void setShowAtoms(boolean showAtoms) {
    this.showAtoms = showAtoms;
  }

  public boolean showBonds = true;
  public void setShowBonds(boolean showBonds) {
    this.showBonds = showBonds;
  }

  public boolean showHydrogens = true;
  public void setShowHydrogens(boolean showHydrogens) {
    this.showHydrogens = showHydrogens;
  }

  public boolean showVectors = true;
  public void setShowVectors(boolean showVectors) {
    this.showVectors = showVectors;
  }

  public boolean showMeasurements = true;
  public void setShowMeasurements(boolean showMeasurements) {
    this.showMeasurements = showMeasurements;
  }

  public Font getMeasureFont(int size) {
    return new Font("Helvetica", Font.PLAIN, size);
  }

  public String propertyStyleString = "";
  public void setPropertyStyleString(String s) {
    propertyStyleString = s;
  }

  public boolean wireframeRotation = false;
  public void setWireframeRotation(boolean wireframeRotation) {
    this.wireframeRotation = wireframeRotation;
  }

  // FIXME NEEDSWORK -- arrow vector stuff
  public double arrowHeadSize = 10.0f;
  public double arrowHeadRadius = 1.0f;
  public double arrowLengthScale = 1.0f;

  public void setArrowHeadSize(double ls) {
    arrowHeadSize = 10.0f * ls;
  }

  public double getArrowHeadSize() {
    return arrowHeadSize / 10.0f;
  }

  // mth dec 2003
  // for some reason, internal to ArrowLine the raw arrowHeadSize was
  // used, but externally it is multiplied/divided by 10
  // will figure it out and fix it later
  public double getArrowHeadSize10() {
    return arrowHeadSize;
  }

  public void setArrowLengthScale(double ls) {
    arrowLengthScale = ls;
  }

  public double getArrowLengthScale() {
    return arrowLengthScale;
  }

  public void setArrowHeadRadius(double rs) {
    arrowHeadRadius = rs;
  }

  public double getArrowHeadRadius() {
    return arrowHeadRadius;
  }
}
