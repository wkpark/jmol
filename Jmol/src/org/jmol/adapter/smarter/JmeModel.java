/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.jmol.adapter.smarter;

import org.jmol.api.ModelAdapter;

import java.io.BufferedReader;
import java.util.StringTokenizer;

class JmeModel extends Model {
  String line;
  StringTokenizer tokenizer;
  
  JmeModel(BufferedReader reader) {
    try {
      line = reader.readLine();
      tokenizer = new StringTokenizer(line, "\t ");
      int atomCount = Integer.parseInt(tokenizer.nextToken());
      System.out.println("atomCount=" + atomCount);
      int bondCount = Integer.parseInt(tokenizer.nextToken());
      setModelName("JME");
      readAtoms(atomCount);
      readBonds(bondCount);
    } catch (Exception ex) {
      errorMessage = "Could not read file:" + ex;
      System.out.println(errorMessage);
    }
  }
    
  void readAtoms(int atomCount) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String atom = tokenizer.nextToken();
      //      System.out.println("atom=" + atom);
      int indexColon = atom.indexOf(':');
      String elementSymbol = (indexColon > 0
                             ? atom.substring(0, indexColon)
                             : atom);
      float x = Float.valueOf(tokenizer.nextToken()).floatValue();
      float y = Float.valueOf(tokenizer.nextToken()).floatValue();
      float z = 0;
      addAtom(new Atom(elementSymbol, 0, x, y, z));
    }
  }

  void readBonds(int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      int atomIndex1 = Integer.parseInt(tokenizer.nextToken());
      int atomIndex2 = Integer.parseInt(tokenizer.nextToken());
      int order = Integer.parseInt(tokenizer.nextToken());
      //      System.out.println("bond "+atomIndex1+"->"+atomIndex2+" "+order);
      if (order < 1) {
        //        System.out.println("Stereo found:" + order);
        order = ((order == -1)
                 ? ModelAdapter.ORDER_STEREO_NEAR
                 : ModelAdapter.ORDER_STEREO_FAR);
      }
      addBond(new Bond(atomIndex1-1, atomIndex2-1, order));
    }
  }
}
