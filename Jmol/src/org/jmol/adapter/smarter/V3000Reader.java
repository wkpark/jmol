/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-15 08:52:29 -0500 (Wed, 15 Mar 2006) $
 * $Revision: 4614 $
 *
 * Copyright (C) 2006  Miguel, Jmol Development, www.jmol.org
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

/**
 * A reader for MDL V3000 files
 *<p>
 * <a href='http://www.mdli.com/downloads/public/ctfile/ctfile.jsp'>
 * http://www.mdli.com/downloads/public/ctfile/ctfile.jsp
 * </a>
 *<p>
 */
class V3000Reader extends AtomSetCollectionReader {
    
  int headerAtomCount;
  int headerBondCount;

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
    throws Exception {
    atomSetCollection = new AtomSetCollection("v3000");
    boolean startNewAtomSet = false;
    /*
      remove code for processing more than one molecular model in
      a .sdf file as multiple models.
      we are just going to read the first model
    String line;
    while (true) {
      line = processCtab(reader, startNewAtomSet);
      if (line == null)
        break;
      if (line.equals("$$$$"))
        startNewAtomSet = true;
    }
    */
    processCtab(reader, startNewAtomSet);
    return atomSetCollection;
  }

  String processCtab(BufferedReader reader,
                     boolean startNewAtomSet) throws Exception {
    String line;
    line = reader.readLine();
    while (line != null &&
           ! line.equals("$$$$") &&
           ! line.startsWith("M  END")) {
      if (line.startsWith("M  V30 BEGIN ATOM")) {
        line = processAtomBlock(reader);
        continue;
      }
      if (line.startsWith("M  V30 BEGIN BOND")) {
        line = processBondBlock(reader);
        continue;
      }
      if (line.startsWith("M  V30 BEGIN CTAB")) {
        if (startNewAtomSet)
          atomSetCollection.newAtomSet();
      } else if (line.startsWith("M  V30 COUNTS")) {
        processCounts(line);
      }
      line = reader.readLine();
    }
    return line;
  }
  
  void processCounts(String line) {
    headerAtomCount = parseInt(line, 13);
    headerBondCount = parseInt(line, ichNextParse);
  }

  String processAtomBlock(BufferedReader reader) throws Exception {
    for (int i = headerAtomCount; --i >= 0; ) {
      String line = readLineWithContinuation(reader);
      if (line == null || (! line.startsWith("M  V30 ")))
        throw new Exception("unrecognized atom");
      Atom atom = new Atom();
      atom.atomSerial = parseInt(line, 7);
      atom.elementSymbol = parseToken(line, ichNextParse);
      atom.x = parseFloat(line, ichNextParse);
      atom.y = parseFloat(line, ichNextParse);
      atom.z = parseFloat(line, ichNextParse);
      parseInt(line, ichNextParse); // discard aamap
      while (true) {
        String option = parseToken(line, ichNextParse);
        if (option == null)
          break;
        if (option.startsWith("CHG="))
          atom.formalCharge = parseInt(option, 4);
      }
      atomSetCollection.addAtomWithMappedSerialNumber(atom);
    }
    String line = reader.readLine();
    if (line == null || ! line.startsWith("M  V30 END ATOM"))
      throw new Exception("M  V30 END ATOM not found");
    return line;
  }

  String processBondBlock(BufferedReader reader) throws Exception {
    for (int i = headerBondCount; --i >= 0; ) {
      String line = readLineWithContinuation(reader);
      if (line == null || (! line.startsWith("M  V30 ")))
        throw new Exception("unrecognized bond");
      /*int bondSerial = */parseInt(line, 7); // currently unused
      int order = parseInt(line, ichNextParse);
      int atomSerial1 = parseInt(line, ichNextParse);
      int atomSerial2 = parseInt(line, ichNextParse);
      atomSetCollection.addNewBondWithMappedSerialNumbers(atomSerial1,
                                                          atomSerial2,
                                                          order);
    }
    String line = reader.readLine();
    if (line == null || ! line.startsWith("M  V30 END BOND"))
      throw new Exception("M  V30 END BOND not found");
    return line;
  }

  String readLineWithContinuation(BufferedReader reader) throws Exception {
    String line = reader.readLine();
    if (line != null && line.length() > 7) {
      while (line.charAt(line.length() - 1) == '-') {
        String line2 = reader.readLine();
        if (line2 == null || ! line.startsWith("M  V30 "))
          throw new Exception("Invalid line continuation");
        line += line2.substring(7);
      }
    }
    return line;
  }
}
