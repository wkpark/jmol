/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-11 23:56:13 -0500 (Mon, 11 Sep 2006) $
 * $Revision: 5499 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;


import java.io.BufferedReader;
import java.util.Hashtable;

import org.jmol.util.Logger;

/*
 * Spartan SMOL and .spartan compound document reader
 * 
 */

public class SpartanSmolReader extends AtomSetCollectionReader {

  boolean isCompoundDocument;

  String modelName = "Spartan file";
  int atomCount;

  Hashtable moData = new Hashtable();

 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    String bondData = "";
    SpartanArchive spartanArchive = null;
    try {
      readLine();
      isCompoundDocument = (line.indexOf("Compound Document") >= 0);
      atomSetCollection = new AtomSetCollection("spartan "
          + (isCompoundDocument ? "compound document file" : "smol"));
      while (line != null) {
        //if (atomCount == 0)
          //Logger.debug(line);
        if (line.equals("HESSIAN") && bondData != null) {
          //cache for later if necessary -- this is from the INPUT section
          while (readLine() != null
              && line.indexOf("ENDHESS") < 0)
            bondData += line + " ";
          //Logger.debug("bonddata:" + bondData);
        }
        if (line.equals("BEGINARCHIVE")
            || line.equals("BEGIN Compound Document Entry: Archive")) {
          spartanArchive = new SpartanArchive(this, atomSetCollection,
              moData, bondData);
          bondData = null;
          readArchiveHeader();
          atomCount = spartanArchive.readArchive(line, false);
          if (atomCount > 0) {
            atomSetCollection.setAtomSetName(modelName);
          }
        } else if (atomCount > 0 && line.indexOf("BEGINPROPARC") == 0
            || line.equals("BEGIN Compound Document Entry: PropertyArchive")) {
          spartanArchive.readProperties();
          if (!atomSetCollection
              .setAtomSetCollectionPartialCharges("MULCHARGES"))
            atomSetCollection.setAtomSetCollectionPartialCharges("Q1_CHARGES");
          Float n = (Float) atomSetCollection.getAtomSetCollectionAuxiliaryInfo("HOMO_N");
          if (moData != null && n != null)
            moData.put("HOMO", new Integer(n.intValue()));
        } else if (line.indexOf("5D shell") >= 0) {
          moData.put("calculationType", line);
        }
        readLine();
      }
    } catch (Exception e) {
      return setError(e);
    }
    // info out of order -- still a chance, at least for first model
    if (atomCount > 0 && spartanArchive != null && bondData != null)
      spartanArchive.addBonds(bondData);
    return atomSetCollection;
  }

  void readArchiveHeader()
      throws Exception {
    String modelInfo = readLine();
    Logger.debug(modelInfo);
    atomSetCollection.setCollectionName(modelInfo);
    modelName = readLine();
    Logger.debug(modelName);
    //    5  17  11  18   0   1  17   0 RHF      3-21G(d)           NOOPT FREQ
    readLine();
  }

}
