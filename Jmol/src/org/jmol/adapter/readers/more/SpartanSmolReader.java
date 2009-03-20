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

 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    modelName = "Spartan file";
    this.reader = reader;
    String bondData = "";
    SpartanArchive spartanArchive = null;
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

      while (line != null) {
        System.out.println(line);
        if (line.equals("HESSIAN") && bondData != null) {
          //cache for later if necessary -- this is from the INPUT section
          while (readLine() != null && line.indexOf("ENDHESS") < 0)
            bondData += line + " ";
          //Logger.debug("bonddata:" + bondData);
        }
        if (line.indexOf("BEGIN") == 0 && line.indexOf("/input") > 0) {
          readInputRecords();
          if (atomSetCollection.errorMessage != null)
            return atomSetCollection;
        } else if (line.indexOf("BEGIN") == 0 && line.indexOf("/Molecule") > 0) {
            readTransform();
        } else if (line.equals("BEGINARCHIVE") || isCompoundDocument
            && line.equals("BEGIN Compound Document Entry: Archive")
            || isDirectory && line.indexOf("BEGIN") == 0
            && line.indexOf("/archive") > 0) {
          spartanArchive = new SpartanArchive(this, atomSetCollection, moData,
              bondData, endCheck);
          if (readArchiveHeader()) {
            if (atomCount > 0)
              atomSetCollection.discardPreviousAtoms(); 
            if (atomSetCollection.getBondCount() > 0)
              bondData = null;
            atomCount = spartanArchive.readArchive(line, false);
            if (atomCount > 0) {
              atomSetCollection.setAtomSetName(modelName);
            }
          }
        } else if (atomCount > 0 && line.indexOf("BEGINPROPARC") == 0
            || isCompoundDocument
            && line.equals("BEGIN Compound Document Entry: PropertyArchive")
            || isDirectory && line.indexOf("BEGIN") == 0
            && line.indexOf("/proparc") > 0) {
          spartanArchive.readProperties();
          if (!atomSetCollection
              .setAtomSetCollectionPartialCharges("MULCHARGES"))
            atomSetCollection.setAtomSetCollectionPartialCharges("Q1_CHARGES");
          Float n = (Float) atomSetCollection
              .getAtomSetCollectionAuxiliaryInfo("HOMO_N");
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

  private void readTransform() throws Exception {
    String[] tokens = getTokens(readLine());
    //5.2494528783E-313 1.043772282734E-312 7.2911220059756244E-304 2.901580574423E-312 
    //0.0 0.0 1.0 0.0 -1.0 0.0 0.0 0.0 0.0 -1.0 0.0 0.0 0.6250277955447784 1.7865956568574344 0.3608597599807504 1.0 

    String view = "resetQuaternion = quaternion({"+tokens[4] + " " + tokens[5] + " " + tokens[6]+"},{"
    +tokens[8] + " " + tokens[9] + " " + tokens[10]+"});rotate quaternion @resetQuaternion;";
    Logger.info(view);
    addJmolScript(view);
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
