/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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

import java.awt.Graphics;
import java.awt.image.IndexColorModel;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.util.Vector;
import java.util.Hashtable;
import java.awt.Color;
import java.awt.Component;

public class AtomType {

  private BaseAtomType baseType;

  /**
   * Constructor
   *
   * @param name the name of this atom type (e.g. CA for alpha carbon)
   * @param root the root of this atom type (e.g. C for alpha carbon)
   * @param AtomicNumber the atomic number (usually the number of protons of the root)
   * @param mass the atomic mass
   * @param vdwRadius the van der Waals radius (helps determine drawing size)
   * @param covalentRadius the covalent radius (helps determine bonding)
   */
  public AtomType(String name, String root, int AtomicNumber, double mass,
      double vdwRadius, double covalentRadius) {
    baseType = BaseAtomType.get(name, root, AtomicNumber, mass, vdwRadius,
        covalentRadius);
  }

  /**
   * Constructs an AtomType from the another AtomType.
   * @param at atom type
   */
  public AtomType(AtomType at) {
    baseType = at.baseType;
  }

  /**
   * Constructs an AtomType from the BaseAtomType.
   * @param at base atom type
   */
  public AtomType(BaseAtomType at) {
    baseType = at;
  }

  public BaseAtomType getBaseAtomType() {
    return baseType;
  }

}
