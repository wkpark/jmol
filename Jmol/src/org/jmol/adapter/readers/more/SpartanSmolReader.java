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
 * Spartan SMOL and .spartan compound document reader and .spartan06 zip files
 * 
 */

public class SpartanSmolReader extends SpartanInputReader {

  private boolean isCompoundDocument;
  private boolean isZipFile;
  private boolean isDirectory;
  private String endCheck;
  
  private Hashtable moData = new Hashtable();

  SpartanArchive spartanArchive;

  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    modelName = "Spartan file";
    this.reader = reader;
    try {
      readLine();
      isCompoundDocument = (line.indexOf("Compound Document") >= 0);
      isZipFile = (line.indexOf("Zip File") >= 0);
      isDirectory = (line.indexOf("Directory") >= 0);
      atomSetCollection = new AtomSetCollection("spartan "
          + (isCompoundDocument ? "compound document file"
              : isDirectory ? "directory" : isZipFile ? "zip file" : "smol"));
      endCheck = (isCompoundDocument ? "END Compound Document Entry"
          : isZipFile ? "END Zip File" : isDirectory ? "END Directory Entry "
              : null);
      if (isZipFile)
        isDirectory = true;
      boolean iHaveModelStatement = false;
      boolean iHaveModel = false;
      while (line != null) {
        Logger.debug(line);
        if (line.indexOf("JMOL_MODEL") >= 0) {

          // bogus type added by Jmol as a marker only

          iHaveModelStatement = true;
          int modelNo = getModelNumber();
          modelNumber = (bsModels == null && modelNo != Integer.MIN_VALUE ? modelNo : modelNumber + 1);
          bondData = "";
          if (!doGetModel(modelNumber)) {
            if (isLastModel(modelNumber) && iHaveModel)
              break;
            iHaveModel = false;
            readLine();
            continue;
          }
          iHaveModel = true;
          atomSetCollection.newAtomSet();
          moData = new Hashtable();
          String title = (String) titles.get("Title" + modelNo);
          title = "Profile " + modelNo + (title == null ? "" : ": " + title);
          Logger.info(title);
          atomSetCollection.setAtomSetAuxiliaryInfo("title", title);
          atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", Boolean.FALSE);
          atomSetCollection.setAtomSetNumber(modelNo);
          readLine();
          continue;
        }
        if (iHaveModelStatement && !iHaveModel) {
          readLine();
          continue;
        }

        if ((line.indexOf("BEGIN") == 0)) {
          String lcline = line.toLowerCase();
          if (lcline.endsWith("input")) {
            bondData = "";
            readInputRecords();
            if (atomSetCollection.errorMessage != null)
              return atomSetCollection;
          } else if (lcline.endsWith("output")) {
            readOutput();
            continue;
          } else if (lcline.endsWith("molecule")) {
            readTransform();
            continue;
          } else if (lcline.endsWith("proparc")
              || lcline.endsWith("propertyarchive")) {
            readProperties();
            continue;
          } else if (lcline.endsWith("archive")) {
            readArchive();
            continue;
          }
        }

        // TODO: 5D shell ... WHY HERE?

        if (line != null && line.indexOf("5D shell") >= 0) {
          moData.put("calculationType", line);
        }
        readLine();
      }
    } catch (Exception e) {
      return setError(e);
    }
    // info out of order -- still a chance, at least for first model
    if (atomCount > 0 && spartanArchive != null && atomSetCollection.getBondCount() == 0
        && bondData != null)
      spartanArchive.addBonds(bondData, 0);
    return atomSetCollection;
  }

  Hashtable titles;
  
  private void readOutput() throws Exception {
    titles = new Hashtable();
    String header = "";
    int pt;
    while (readLine() != null & !line.startsWith("END ")) {
      header += line + "\n";
      if ((pt = line.indexOf(")")) > 0)
        titles.put("Title"+parseInt(line.substring(0, pt))
            , (line.substring(pt + 1).trim()));
    }
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("fileHeader", header);
  }

  private void readArchive() throws Exception {
    spartanArchive = new SpartanArchive(this, atomSetCollection, moData,
        bondData, endCheck);
    if (readArchiveHeader()) {
      modelAtomCount = spartanArchive.readArchive(line, false, atomCount, false);
      atomCount += modelAtomCount;
    }
  }
  
  private void readProperties() throws Exception {
    spartanArchive.readProperties();
    if (!atomSetCollection
        .setAtomSetCollectionPartialCharges("MULCHARGES"))
      atomSetCollection.setAtomSetCollectionPartialCharges("Q1_CHARGES");
    Float n = (Float) atomSetCollection
        .getAtomSetCollectionAuxiliaryInfo("HOMO_N");
    if (moData != null && n != null)
      moData.put("HOMO", new Integer(n.intValue()));
    readLine();
  }
  
  private int getModelNumber() {
    try {
      int pt = line.indexOf("JMOL_MODEL ") + 11;
      return parseInt(line, pt);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void readTransform() throws Exception {
    String[] tokens = getTokens(readLine());
    // created with flag: binaryDoubleAsString
    //5.2494528783E-313 1.043772282734E-312 7.2911220059756244E-304 2.901580574423E-312 
    //0.0 0.0 1.0 0.0 -1.0 0.0 0.0 0.0 0.0 -1.0 0.0 0.0 0.6250277955447784 1.7865956568574344 0.3608597599807504 1.0 
    if (tokens.length > 14)
      setTransform(
        parseFloat(tokens[4]), parseFloat(tokens[5]), parseFloat(tokens[6]),
        parseFloat(tokens[8]), parseFloat(tokens[9]), parseFloat(tokens[10]),
        parseFloat(tokens[12]), parseFloat(tokens[13]), parseFloat(tokens[14]));
    else
      System.out.println("cannot read Spartan Molecule record");
  }

  private boolean readArchiveHeader()
      throws Exception {
    String modelInfo = readLine();
    Logger.debug(modelInfo);
    if (modelInfo.indexOf("Error:") == 0) // no archive here
      return false;
    atomSetCollection.setCollectionName(modelInfo);
    modelName = readLine();
    Logger.debug(modelName);
    //    5  17  11  18   0   1  17   0 RHF      3-21G(d)           NOOPT FREQ
    readLine();
    return true;
  }

}
