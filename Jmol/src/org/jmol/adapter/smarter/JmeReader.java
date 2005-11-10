/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;
import java.util.StringTokenizer;

class JmeReader extends AtomSetCollectionReader {

  String line;
  StringTokenizer tokenizer;
  
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {
    atomSetCollection = new AtomSetCollection("jme");

    try {
      line = reader.readLine();
      tokenizer = new StringTokenizer(line, "\t ");
      int atomCount = parseInt(tokenizer.nextToken());
      System.out.println("atomCount=" + atomCount);
      int bondCount = parseInt(tokenizer.nextToken());
      atomSetCollection.setCollectionName("JME");
      readAtoms(atomCount);
      readBonds(bondCount);
    } catch (Exception ex) {
      atomSetCollection.errorMessage = "Could not read file:" + ex;
      logger.log(atomSetCollection.errorMessage);
    }
    return atomSetCollection;
  }
    
  void readAtoms(int atomCount) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String strAtom = tokenizer.nextToken();
      //      System.out.println("strAtom=" + strAtom);
      int indexColon = strAtom.indexOf(':');
      String elementSymbol = (indexColon > 0
                              ? strAtom.substring(0, indexColon)
                              : strAtom).intern();
      float x = parseFloat(tokenizer.nextToken());
      float y = parseFloat(tokenizer.nextToken());
      float z = 0;
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.x = x; atom.y = y; atom.z = z;
    }
  }

  void readBonds(int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      int atomIndex1 = parseInt(tokenizer.nextToken());
      int atomIndex2 = parseInt(tokenizer.nextToken());
      int order = parseInt(tokenizer.nextToken());
      //      System.out.println("bond "+atomIndex1+"->"+atomIndex2+" "+order);
      if (order < 1) {
        //        System.out.println("Stereo found:" + order);
        order = ((order == -1)
                 ? JmolAdapter.ORDER_STEREO_NEAR
                 : JmolAdapter.ORDER_STEREO_FAR);
      }
      atomSetCollection.addBond(new Bond(atomIndex1-1, atomIndex2-1, order));
    }
  }
}
