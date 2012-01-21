/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-10-02 18:29:23 -0500 (Sun, 02 Oct 2011) $
 * $Revision: 16204 $
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
package org.jmol.spectrum;

import java.util.List;

/**
 * Abstract class that is implemented in both Jmol and JSpecView
 * These two packages should extend this abstract class to process signals in their own way.
 * 
 **/

public abstract class JmolSpectralPeer {
  
  JmolSpectralPeer peer;
  public void setPeer(JmolSpectralPeer jsi) {
    peer = jsi;    
  }
  
  // The following methods must be implemented by program-specific subclass
  
  /**
   * @param list  
   */
  public abstract void highlight(List<String> list);

  /**
   * @param fileName  
   * @param qualifier 
   * @param assignmentData 
   * @param isLocal 
   * @return true if successful
   */
  public abstract boolean fileLoaded(String fileName, String qualifier,
                            String assignmentData, boolean isLocal);
  
}
