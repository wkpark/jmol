/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-28 23:13:00 -0500 (Thu, 28 Sep 2006) $
 * $Revision: 5772 $
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

package org.jmol.adapter.readers.simple;

import org.jmol.adapter.smarter.*;

import org.jmol.api.JmolAdapter;

import java.util.StringTokenizer;

public class JmeReader extends AtomSetCollectionReader {
/*
 *  see http://www.molinspiration.com/jme/doc/jme_functions.html
 *
 * not fully supported; recognized simply as a file with a single
 * line and a digit as the first character.
 * 
 * the format of the JME String is as follows
 * natoms nbonds (atomic_symbol x_coord y_coord) for all atoms 
 * (atom1 atom2 bond_order) for all bonds
 * (for stereo bonds the bond order is -1 for up and -2 for down 
 * from the first to the second atom)
 * Molecules in multipart system are separated by 
 * the | character. Components of the reaction are 
 * separated by the > character. The JME string for 
 * the reaction is thus "reactant1 | reactant 2 ... > 
 * modulator(s) > product(s)"
 * 
 * Which, unfortunately, is not much to go on. 
 * JME also outputs MDL MOL files; this should be the preferred
 * option
 * 
 * 
 */

  private StringTokenizer tokenizer;
  
  public void initializeReader() throws Exception {
    atomSetCollection.setCollectionName("JME");
    atomSetCollection.newAtomSet();
    if (filter == null || filter.toUpperCase().indexOf("NOMIN") < 0)
      addJmolScript("minimize addHydrogens");
    readLine();
    tokenizer = new StringTokenizer(line, "\t ");
    int atomCount = parseInt(tokenizer.nextToken());
    int bondCount = parseInt(tokenizer.nextToken());
    readAtoms(atomCount);
    readBonds(bondCount);
    continuing = false;
  }
    
  private void readAtoms(int atomCount) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String strAtom = tokenizer.nextToken();
      //Logger.debug("strAtom=" + strAtom);
      int indexColon = strAtom.indexOf(':');
      String elementSymbol = (indexColon > 0
                              ? strAtom.substring(0, indexColon)
                              : strAtom).intern();
      float x = parseFloat(tokenizer.nextToken());
      float y = parseFloat(tokenizer.nextToken());
      float z = (float) (Math.random()* 0.2 - 0.1);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.set(x, y, z);
    }
  }

  private void readBonds(int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      int atomIndex1 = parseInt(tokenizer.nextToken());
      int atomIndex2 = parseInt(tokenizer.nextToken());
      int order = parseInt(tokenizer.nextToken());
      switch (order) {
      case 0:
        continue;
      case -1:
        order = JmolAdapter.ORDER_STEREO_NEAR;
        break;
      case -2:
        order = atomIndex1;
        atomIndex1 = atomIndex2;
        atomIndex2 = order;
        order = JmolAdapter.ORDER_STEREO_NEAR;
        break;
      }
      atomSetCollection
          .addBond(new Bond(atomIndex1 - 1, atomIndex2 - 1, order));
    }
  }
}
