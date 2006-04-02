/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 16:52:44 +0100 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2006  Jmol Development, www.jmol.org
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
package org.jmol.adapter.smarter;

class Strand {

  String chainID; // aka asym_id in PDB/mmCIF
  String authorID;
  Boolean isBlank;

  Strand() {
    chainID = null;
    authorID = null;
    isBlank = Boolean.TRUE;
  }

  Strand(String chainID, String authorID, Boolean isBlank) {
    this.chainID = chainID;
    this.authorID = authorID;
    this.isBlank = isBlank;
  }
  
  public String toString() {
    return "Strand " + chainID + 
           ", authorID=" + authorID +
           ", isBlank=" + isBlank.toString();
  }
}
