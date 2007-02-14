/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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


import java.io.BufferedReader;

import org.jmol.util.Logger;

/**
 * JMol DATA "coord set" script file reader. 
 * This reader does not execute the commands in the file.
 * Should it? Maybe....
 * 
 */

class JmolDataReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader)  {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("JmolData");
    try {
      String lastLine = "";
      while (readLine() != null && line.indexOf("Jmol Coordinate Data") < 0)
        lastLine = line;
      int modelAtomCount = parseInt(lastLine);
      readAtoms(modelAtomCount);
      discardLinesUntilContains("# orientation");
      while(readLine() != null && line.indexOf("#") < 0) {
        checkLineForScript("#jmolscript:" + line);
      }
      checkLineForScript("#jmolscript:set refreshing true");
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }
    
  void readAtoms(int modelAtomCount) throws Exception {
    for (int i = 0; i < modelAtomCount; ++i) {
      readLine();
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens(line);
      atom.elementSymbol = tokens[1];
      atom.x = parseFloat(tokens[3]);
      atom.y = parseFloat(tokens[4]);
      atom.z = parseFloat(tokens[5]);
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        Logger.warn("line cannot be read for JmolData: " + line);
        atom.x = 0;
        atom.y = 0;
        atom.z = 0;
      }
      setAtomCoord(atom);
    }
  }
}
