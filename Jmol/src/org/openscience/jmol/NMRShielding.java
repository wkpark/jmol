/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

/**
 * A class to store the NMRShielding property for an atom
 */
public class NMRShielding extends PhysicalProperty {

  /**
   * Constructor for NMR Shielding
   * @param s The isotropic shielding of the atom
   */
  public NMRShielding(double s) {
    super("Isotropic Shielding", new Double(s));
  }

  /**
   * Constructor for NMR Shielding
   * @param s The isotropic shielding of the atom
   */
  public NMRShielding(String descriptor, double s) {
    super(descriptor, new Double(s));
  }

}
