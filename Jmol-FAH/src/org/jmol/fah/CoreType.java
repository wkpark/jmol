/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.fah;

/**
 * Typesafe enum class for core types
 */
public class CoreType {

  /**
   * Get CoreType from code
   * 
   * @param code Code of the core
   * @return CoreType
   */
  static public CoreType getFromCode(String code) {
    if (code != null) {
      if (code.equalsIgnoreCase(AMBER._code)) {
       return AMBER;
      }
      if (code.equalsIgnoreCase(DGROMACS._code)) {
        return DGROMACS;
      }
      if (code.equalsIgnoreCase(GBGROMACS._code)) {
          return GBGROMACS;
      }
      if (code.equalsIgnoreCase(GROMACS._code)) {
        return GROMACS;
      }
      if (code.equalsIgnoreCase(QMD._code)) {
        return QMD;
      }
      if (code.equalsIgnoreCase(TINKER._code)) {
        return TINKER;
      }
    }
    return UNKNOWN;
  }

  /**
   * Get CoreType from name
   * 
   * @param name Name of the core
   * @return CoreType
   */
  static public CoreType getFromName(String name) {
    if (name != null) {
      if (name.equalsIgnoreCase(AMBER._name)) {
        return AMBER;
      }
      if (name.equalsIgnoreCase(DGROMACS._name)) {
        return DGROMACS;
      }
      if (name.equalsIgnoreCase(GBGROMACS._name)) {
          return GBGROMACS;
      }
      if (name.equalsIgnoreCase(GROMACS._name)) {
        return GROMACS;
      }
      if (name.equalsIgnoreCase(QMD._name)) {
        return QMD;
      }
      if (name.equalsIgnoreCase(TINKER._name)) {
        return TINKER;
      }
    }
    return UNKNOWN;
  }

  /**
   * @return Returns the code.
   */
  public String getCode() {
    return this._code;
  }

  /**
   * @return Returns the name.
   */
  public String getName() {
    return this._name;
  }

  // Cores
  static public final CoreType UNKNOWN   = new CoreType(null, null);
  static public final CoreType AMBER     = new CoreType("Amber", "A"); //$NON-NLS-1$ //$NON-NLS-2$
  static public final CoreType DGROMACS  = new CoreType("DGromacs", "DG"); //$NON-NLS-1$ //$NON-NLS-2$
  static public final CoreType GBGROMACS = new CoreType("GBGromacs", "GB"); //$NON-NLS-1$ //$NON-NLS-2$
  static public final CoreType GROMACS   = new CoreType("Gromacs", "G");  //$NON-NLS-1$ //$NON-NLS-2$
  static public final CoreType QMD       = new CoreType("QMD", "Q"); //$NON-NLS-1$ //$NON-NLS-2$
  static public final CoreType TINKER    = new CoreType("Tinker", "T");   //$NON-NLS-1$//$NON-NLS-2$

  // Attributes
  private final String _name;
  private final String _code;

  /**
   * Constructor for CoreType
   * 
   * @param name Name of core
   * @param code Letter code of core
   */
  private CoreType(String name, String code) {
    this._name = name;
    this._code = code;
  }
}
