/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 18:41:50 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5311 $
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
 * Wavefunction Odyssey reader -- old style
 * 
 */

public class OdysseyXMLReader extends AtomSetCollectionReader {

  String modelName = "Odyssey XML file";
  int atomCount, bondCount;
  //Hashtable moData = new Hashtable();

 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("odyssey(XML)");
    try {
      if (discardLinesUntilContains("<description") != null)
        readDescription();
      if (discardLinesUntilContains("<atoms") != null)
        readAtoms();
      if (discardLinesUntilContains("<bonds") != null)
        readBonds();
    } catch (Exception e) {
      return setError(e);
    }
    if (atomCount > 0)
      atomSetCollection.setAtomSetName(modelName);
    return atomSetCollection;
  }

  void readDescription() throws Exception {
    /*
 
  <description>
    <title>Dimethylbenzene</title>
    <phase>Liquid (Isomer Mixture)</phase>
    <common_name>Xylene</common_name>
    <formula>C8H10</formula>
    <safety_data>2-3-0</safety_data>
  </description>

     */
    String title = "";
    String phase = null;
    while (readLine() != null && line.indexOf("</description>") < 0) {
      if (line.indexOf("title")>=0)
        title = line.substring(line.indexOf(">")+1, line.lastIndexOf("<"));
      else if (line.indexOf("phase")>=0)
        phase = line.substring(line.indexOf(">")+1, line.lastIndexOf("<"));
    }
    modelName = title + (phase != null ? " - " + phase : "");
  }

  void readAtoms() throws Exception {
    atomCount = 0;
    while (readLine() != null && line.indexOf("</atoms>") < 0) {
      Hashtable xml = readXML("atom");
      Atom atom = new Atom();
      atom.atomName = (String) xml.get("id");
      atom.elementSymbol = (String) xml.get("element");
      String[] tokens = getTokens((String) xml.get("xyz"));
      atom.set(parseFloat(tokens[0]), parseFloat(tokens[1]), parseFloat(tokens[2]));
      atomSetCollection.addAtomWithMappedName(atom);
      atom.atomName = (String) xml.get("label");
      atomCount++;
    }
  }

  Hashtable readXML(String name) throws Exception {
    // simple unnested XML -- tags must begin and end on own line
    String tag = "<" + name;
    line = discardLinesUntilContains(tag);
    tag = "/" + name + ">";
    String tag2 = "/>";
    Hashtable xml = new Hashtable();
    while (line != null) {
      line = line.trim();
      int ipt = 0;
      int ipt0 = 0;
      while ((ipt = line.indexOf("=", ipt)) >= 0) {
        String key = line.substring(ipt0, ipt);
        if (key.indexOf(" ") > 0)
          key = key.substring(key.lastIndexOf(" ") + 1);
        int ptQ1 = line.indexOf("\"", ipt0);
        ipt0 = line.indexOf("\"", ptQ1 + 1);
        String val = line.substring(ptQ1 + 1, ipt0);
        xml.put(key, val);
        //System.out.println(">" + key + "<=>" + val + "<");
      }
      readLine();
      if (line.indexOf(tag) >= 0 || line.indexOf(tag2) >= 0)
        break;
    }
    return xml;
  }

  void readBonds() throws Exception {
    while (readLine() != null && line.indexOf("</bonds>") < 0) {
      Hashtable xml = readXML("bond");
      int sourceIndex = atomSetCollection.getAtomNameIndex((String) xml
          .get("a"));
      int targetIndex = atomSetCollection.getAtomNameIndex((String) xml
          .get("b"));
      String order = (String) xml.get("order");
      int bondOrder;
      if (order == "single")
        bondOrder = 1;
      else if (order == "double")
        bondOrder = 2;
      else if (order == "triple")
        bondOrder = 3;
      else// if (order == "delocalized")
      //  bondOrder = 1;
      //else
        bondOrder = 1;
      atomSetCollection.addBond(new Bond(sourceIndex, targetIndex,
          bondOrder < 4 ? bondOrder : 1)); //aromatic would be 5
      bondCount++;
    }
    if (Logger.debugging) {
      Logger.debug(bondCount + " bonds read");
    }
  }
}
