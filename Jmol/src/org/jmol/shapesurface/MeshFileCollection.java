/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-09 09:51:59 -0600 (Fri, 09 Feb 2007) $
 * $Revision: 6742 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.shapesurface;

import org.jmol.shape.MeshCollection;
import org.jmol.util.Parser;

public abstract class MeshFileCollection extends MeshCollection {

  
  protected String line;
  protected int[] next = new int[1];
  

  protected String[] getTokens() {
    return Parser.getTokens(line);
  }

  protected float parseFloat() {
    return Parser.parseFloat(line, next);
  }

  protected float parseFloat(String s) {
    next[0] = 0;
    return Parser.parseFloat(s, next);
  }

  protected float parseFloatNext(String s) {
    return Parser.parseFloat(s, next);
  }

  protected int parseInt() {
    return Parser.parseInt(line, next);
  }
  
  protected int parseInt(String s) {
    next[0] = 0;
    return Parser.parseInt(s, next);
  }
  
  protected int parseIntNext(String s) {
    return Parser.parseInt(s, next);
  }
  
  protected int parseInt(String s, int iStart) {
    next[0] = iStart;
    return Parser.parseInt(s, next);
  }  
}

 