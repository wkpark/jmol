/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:23:28 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5305 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

package org.jmol.shapesurface;

import java.util.BitSet;

import org.jmol.atomdata.RadiusData;
import org.jmol.script.Token;

public class Contact extends Isosurface {

  // these are globals, stored here and only passed on when the they are needed. 

  @Override
  public void initShape() {
    super.initShape();
    myType = "contact";
  }

  @Override
  public void setProperty(String propertyName, Object value, BitSet bs) {


    if ("init" == propertyName) {
      super.setProperty("init", value, null);
      return;
    }

    if ("set" == propertyName) {
      setContacts((Object[]) value);
      return;
    }
    
    super.setProperty(propertyName, value, bs);
  }
  
  private void setContacts(Object[] value) {
    BitSet bsA = (BitSet) value[0];
    BitSet bsB = (BitSet) value[1];
    BitSet bsIgnore = (BitSet) value[2];
    int type = ((Integer) value[3]).intValue();
    RadiusData rd = (RadiusData) value[4];
    float[] params = (float[]) value[5];
    Object func = value[6];
    Object slabObject = value[7];
    boolean isColorDensity = ((Boolean)value[8]).booleanValue();
    String command = (String) value[9];
    float ptSize = (isColorDensity  && params != null && params[0] < 0 ? Math.abs(params[0]) : 0.15f);
    
    
    if (type == Token.full)
      type = Token.plane; // for now
    
    setProperty("newObject", null, null);
    
    if (isColorDensity)
      sg.setParameter("colorDensity", null);

    switch (type) {
    case Token.full:
      // TODO
      break;
    case Token.plane:
    case Token.connect:
      if (type == Token.connect)
        setProperty("parameters", params, null);
      setProperty("func", func, null);
      setProperty("intersection", new BitSet[] { bsA, bsB }, null);
      setProperty("radius", rd, null);
      if (isColorDensity)
        setProperty("cutoffRange", new float[] { -0.3f, 0.3f }, null);
      setProperty("bsSolvent", null, null);
      setProperty("sasurface", Float.valueOf(0), null);
      // mapping
      setProperty("map", Boolean.TRUE, null);
      setProperty("select", bsA, null);
      setProperty("radius", rd, null);
      setProperty("sasurface", Float.valueOf(0), null);
      setProperty("finalize", command, null);
      // slabbing:
      if (slabObject != null) {
        setProperty("clear", null, null);
        setProperty("init", command, null);
        setProperty("title", new String[] { command }, null);
        setProperty("slab", slabObject, null);
      }
      break;      
    case Token.nci:
      bsA.or(bsB); // for now -- TODO -- need to distinguish ligand
      if (params[0] < 0)
        params[0] = 0; // reset to default for density
      setProperty("select", bsA, null);
      setProperty("ignore", bsIgnore, null);
      setProperty("parameters", params, null);
      setProperty("nci", Boolean.TRUE, null);
      break;
    }  
    if (isColorDensity) {
      setProperty("pointSize", Float.valueOf(ptSize), null);
    }
    if (thisMesh != null)
      thisMesh.slabOptions = null;
  }

}
