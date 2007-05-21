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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapebio;

import java.util.BitSet;
import java.util.Hashtable;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Mmset;
import org.jmol.modelset.Model;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.shape.Closest;
import org.jmol.shape.Shape;
import org.jmol.viewer.JmolConstants;
/****************************************************************
 * Mps stands for Model-Polymer-Shape
 * 
 * When a Cartoon is instantiated with a call to setSize(),
 * it creates an MpsShape for each BioPolymer in the model set.
 * 
 * It is these shapes that are the real "shapes". Unlike other
 * shapes, which are indexed by atom and throughout the entire
 * model set, these shapes are indexed by residue and are 
 * restricted to a given BioPolymer within a given Model.
 * 
 * Model 
 * 
 ****************************************************************/
public abstract class BioShapeCollection extends Shape {

  Mmset mmset;
  Atom[] atoms;
  
  short madOn = -2;
  short madHelixSheet = 3000;
  short madTurnRandom = 800;
  short madDnaRna = 5000;
  boolean isActive = false;

  BioShape[] bioShapes;
  
  public final void initModelSet() {
    mmset = modelSet.getMmset();
    atoms = modelSet.atoms;
    initialize();
  }

  public void setSize(int size, BitSet bsSelected) {
    short mad = (short) size;
    initialize();
    for (int i = bioShapes.length; --i >= 0;) {
      BioShape bioShape = bioShapes[i];
      if (bioShape.monomerCount > 0)
        bioShape.setMad(mad, bsSelected);
    }
  }

  public void setProperty(String propertyName, Object value, BitSet bsSelected) {
    initialize();
    if ("color" == propertyName) {
      byte pid = JmolConstants.pidOf(value);
      short colix = Graphics3D.getColix(value);
      for (int i = bioShapes.length; --i >= 0;) {
        BioShape bioShape = bioShapes[i];
        if (bioShape.monomerCount > 0)
          bioShape.setColix(colix, pid, bsSelected);
      }
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = ("translucent".equals(value));
      for (int i = bioShapes.length; --i >= 0;) {
        BioShape bioShape = bioShapes[i];
        if (bioShape.monomerCount > 0)
          bioShape.setTranslucent(isTranslucent, bsSelected, translucentLevel);
      }
      return;
    }
    super.setProperty(propertyName, value, bsSelected);
  }

  public String getShapeState() {
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();    
    for (int i = bioShapes.length; --i >= 0; ) {
      BioShape bioShape = bioShapes[i];
      if (bioShape.monomerCount > 0)
        bioShape.setShapeState(temp, temp2);
    }
    return getShapeCommands(temp, temp2, modelSet.getAtomCount());
  }

  void initialize() {
    int modelCount = mmset == null ? 0 : mmset.getModelCount();
    Model[] models = mmset.getModels();
    int nPolymers = mmset.getBioPolymerCount();
    BioShape[] m = new BioShape[nPolymers];
    int n = nPolymers;
    for (int i = modelCount; --i >= 0;)
      for (int j = models[i].getBioPolymerCount(); --j >= 0;) {
        n--;
        if (bioShapes == null || bioShapes.length <= n || bioShapes[n] == null) {
          m[n] = new BioShape(this, i, (BioPolymer) models[i].getBioPolymer(j));
        } else {
          m[n] = bioShapes[n];
        }
      }
    bioShapes = m;
  }

  int getMpsmodelCount() {
    return (bioShapes == null ? 0 : mmset.getModelCount());
  }

  public void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
    for (int i = bioShapes.length; --i >= 0; ){
      BioShape b = bioShapes[i];
      b.bioPolymer.findNearestAtomIndex(xMouse, yMouse, closest, bioShapes[i].mads, myVisibilityFlag);      
    }
  }

  public void setVisibilityFlags(BitSet bs) {
    if (bioShapes == null)
      return;
    int modelIndex = viewer.getCurrentModelIndex();
    for (int i = bioShapes.length; --i >= 0; ) {
      BioShape b = bioShapes[i];
      b.modelVisibilityFlags = (modelIndex >= 0
          && modelIndex != b.modelIndex ? 0 : myVisibilityFlag);
    }
  }

  public void setModelClickability() {
    if (bioShapes == null)
      return;
    for (int i = bioShapes.length; --i >= 0; )
      bioShapes[i].setModelClickability();
  }

  int getMpsShapeCount() {
    return bioShapes.length;
  }

  BioShape getBioShape(int i) {
    return bioShapes[i];
  }  
}
