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

/**
 * A reader for MDLI mol and sdf files.
 *<p>
 * <a href='http://www.mdli.com/downloads/public/ctfile/ctfile.jsp'>
 * http://www.mdli.com/downloads/public/ctfile/ctfile.jsp
 * </a>
 *<p>
 */
class MolReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader)
    throws Exception {
    atomSetCollection = new AtomSetCollection("mol");
    String firstLine = reader.readLine();
    if (firstLine.startsWith("$MDL")) {
      processRgHeader(reader, firstLine);
      //String line;
      while (!reader.readLine().startsWith("$CTAB"))
        { }
      processCtab(reader);
    } else {
      processMolSdHeader(reader, firstLine);
      processCtab(reader);
    }
    return atomSetCollection;
  }

  void processMolSdHeader(BufferedReader reader, String firstLine)
                          throws Exception {
    /* 
     * obviously we aren't being this strict, but for the record:
     *  
     * from ctfile.pdf (October 2003):
     * 
     * Line 1: Molecule name. This line is unformatted, but like all 
     * other lines in a molfile may not extend beyond column 80. 
     * If no name is available, a blank line must be present.
     * Caution: This line must not contain any of the reserved 
     * tags that identify any of the other CTAB file types 
     * such as $MDL (RGfile), $$$$ (SDfile record separator), 
     * $RXN (rxnfile), or $RDFILE (RDfile headers). 
     * 
     * Line 2: This line has the format:
     * IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR
     * (FORTRAN: A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6-> )
     * User's first and last initials (l), program name (P), 
     * date/time (M/D/Y,H:m), dimensional codes (d), scaling factors (S, s), 
     * energy (E) if modeling program input, internal 
     * registry number (R) if input through MDL form. A blank line can be 
     * substituted for line 2. If the internal registry number is more than 
     * 6 digits long, it is stored in an M REG line (described in Chapter 3). 
     * 
     * Line 3: A line for comments. If no comment is entered, a blank line 
     * must be present.
     */
    
    String header = firstLine+"\n";
    atomSetCollection.setCollectionName(firstLine);
    header += reader.readLine() + "\n";
    //line 3:
    String comment = reader.readLine();
    header += comment + "\n";
    checkLineForScript(comment);
    atomSetCollection.setAtomSetCollectionProperty("fileHeader", header);
  }

  void processRgHeader(BufferedReader reader, String firstLine)
    throws Exception {
    /*
     * from ctfile.pdf:
     * 
     * $MDL REV 1 date/time
     * $MOL
     * $HDR
     * [Molfile Header Block (see Chapter 4) = name, pgm info, comment]
     * $END HDR
     * $CTAB
     * [Ctab Block (see Chapter 2) = count + atoms + bonds + lists + props]
     * $END CTAB
     * $RGP
     * rrr [where rrr = Rgroup number]
     * $CTAB
     * [Ctab Block]
     * $END CTAB
     * $END RGP
     * $END MOL
     */
    
    String line;
    while ((line = reader.readLine()) != null &&
           !line.startsWith("$HDR"))
      { }
    if (line == null) {
      System.out.println("$HDR not found in MDL RG file");
      return;
    }
    processMolSdHeader(reader, reader.readLine());
  }

  void processCtab(BufferedReader reader) throws Exception {
    String countLine = reader.readLine();
    int atomCount = parseInt(countLine, 0, 3);
    int bondCount = parseInt(countLine, 3, 6);
    readAtoms(reader, atomCount);
    readBonds(reader, bondCount);
  }
  
  void readAtoms(BufferedReader reader, int atomCount) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String line = reader.readLine();
      String elementSymbol = "";
      if (line.length() > 34) {
          elementSymbol = line.substring(31,34).trim().intern();
      } else {
           // deal with older Mol format where nothing after the symbol is used
          elementSymbol = line.substring(31).trim().intern();
      }
      float x = parseFloat(line,  0, 10);
      float y = parseFloat(line, 10, 20);
      float z = parseFloat(line, 20, 30);
      int charge = 0;
      if (line.length() >= 39) {
        int chargeCode = parseInt(line, 36, 39);
        if (chargeCode >= 1 && chargeCode <= 7)
          charge = 4 - chargeCode;
      }
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.formalCharge = charge;
      atom.x = x; atom.y = y; atom.z = z;
    }
  }

  void readBonds(BufferedReader reader, int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      String line = reader.readLine();
      int atomIndex1 = parseInt(line, 0, 3);
      int atomIndex2 = parseInt(line, 3, 6);
      int order = parseInt(line, 6, 9);
      if (order == 4)
        order = JmolAdapter.ORDER_AROMATIC;
      atomSetCollection.addBond(new Bond(atomIndex1-1, atomIndex2-1, order));
    }
  }
}
