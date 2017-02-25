/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.adapter.readers.cif;

import org.jmol.adapter.smarter.AtomSetCollectionReader;


/**
 * 
 * Not used -- originally in a plan to create a specialized parser for magnetic structure CIF data
 * 
 * magCIF files are recognized after class creation, so this is a subreader
 * but it turned out not to be necessary.
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 * 
 */
public class MagCifRdr implements MagCifRdrInterface {

  
  private AtomSetCollectionReader r;

  public MagCifRdr() {
    // for reflection
  }

  @Override
  public void initialize(AtomSetCollectionReader r) throws Exception {
     this.r = r;
  }

}
