/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-02 11:48:43 -0500 (Wed, 02 Aug 2006) $
 * $Revision: 5364 $
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
import java.util.HashMap;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import netscape.javascript.JSObject;

import org.xml.sax.*;

/**
 * An chem3d c3xml reader
 */

class XmlChem3dReader extends XmlReader {

  /*
   * Enter any implemented field names in the 
   * implementedAttributes array. It is for when the XML 
   * is already loaded in the DOM of an XML page.
   * 
   */

  String[] chem3dImplementedAttributes = { "id", //general 
      "symbol", "cartCoords", //atoms
      "bondAtom1", "bondAtom2", "bondOrder", //bond
      "gridDatXDim", "gridDatYDim", "gridDatZDim",    
      "gridDatXSize", "gridDatYSize", "gridDatZSize",    
      "gridDatOrigin", "gridDatDat",   // grid cube data
      "calcPartialCharges", "calcAtoms" // electronicStructureCalculation 
  };

  String modelName = null;
  String formula = null;
  String phase = null;

  XmlChem3dReader(XmlReader parent, AtomSetCollection atomSetCollection, BufferedReader reader, XMLReader xmlReader) {
    this.parent = parent;
    this.atomSetCollection = atomSetCollection;
    new Chem3dHandler(xmlReader);
    parseReaderXML(reader, xmlReader);
  }

  XmlChem3dReader(XmlReader parent, AtomSetCollection atomSetCollection, JSObject DOMNode) {
    this.parent = parent;
    this.atomSetCollection = atomSetCollection;
    implementedAttributes = chem3dImplementedAttributes;
    ((Chem3dHandler) (new Chem3dHandler())).walkDOMTree(DOMNode);
  }

  void processStartElement(String namespaceURI, String localName, String qName,
                           HashMap atts) {
    String[] tokens;
    if ("model".equals(localName)) {
      atomSetCollection.newAtomSet();
      return;
    }

    if ("atom".equals(localName)) {
      atom = new Atom();
      atom.atomName = (String) atts.get("id");
      atom.elementSymbol = (String) atts.get("symbol");
      if (atts.containsKey("cartCoords")) {
        String xyz = (String) atts.get("cartCoords");
        tokens = getTokens(xyz);
        atom.x = parseFloat(tokens[0]);
        atom.y = parseFloat(tokens[1]);
        atom.z = parseFloat(tokens[2]);
      }
      return;
    }
    if ("bond".equals(localName)) {
      String atom1 = (String) atts.get("bondAtom1");
      String atom2 = (String) atts.get("bondAtom2");
      int order = 1;
      if (atts.containsKey("bondOrder"))
        order = parseInt((String) atts.get("bondOrder"));
      atomSetCollection.addNewBond(atom1, atom2, order);
      return;
    }

    if ("electronicStructureCalculation".equals(localName)) {
      tokens = getTokens((String) atts.get("calcPartialCharges"));
      String[] tokens2 = getTokens((String) atts.get("calcAtoms"));
      for (int i = parseInt(tokens[0]); --i >= 0;)
        atomSetCollection.mapPartialCharge(tokens2[i + 1],
            parseFloat(tokens[i + 1]));
    }

    if ("gridData".equals(localName)) {
      int nPointsX = parseInt((String) atts.get("gridDatXDim"));
      int nPointsY = parseInt((String) atts.get("gridDatYDim"));
      int nPointsZ = parseInt((String) atts.get("gridDatZDim"));
      float xStep = parseFloat((String) atts.get("gridDatXSize"))
          / (nPointsX - 1f);
      float yStep = parseFloat((String) atts.get("gridDatYSize"))
          / (nPointsY - 1f);
      float zStep = parseFloat((String) atts.get("gridDatZSize"))
          / (nPointsZ - 1f);
      String xyz = (String) atts.get("gridDatOrigin");
      tokens = getTokens(xyz);
      float originX = parseFloat(tokens[0]);
      float originY = parseFloat(tokens[1]);
      float originZ = parseFloat(tokens[2]);
      tokens = getTokens((String) atts.get("gridDatData"));
      int nData = parseInt(tokens[0]);
      int pt = 1;
      float[][][] voxelData = new float[nPointsX][][];
      for (int x = 0; x < nPointsX; ++x) {
        voxelData[x] = new float[nPointsY][];
        for (int y = 0; y < nPointsY; ++y) {
          voxelData[x][y] = new float[nPointsZ];
        }
      }
      // this is pure speculation for now.
      // seems to work for one test case.
      // could EASILY be backward.

      for (int z = nPointsZ; --z >= 0;)
        for (int y = 0; y < nPointsY; y++)
          for (int x = nPointsX; --x >= 0;)
            voxelData[x][y][z] = parseFloat(tokens[pt++]);
      int[] voxelCounts = new int[] { nPointsX, nPointsY, nPointsZ };
      Point3f volumetricOrigin = new Point3f(originX, originY, originZ);
      Vector3f[] volumetricVectors = new Vector3f[3];
      volumetricVectors[0] = new Vector3f(xStep, 0, 0);
      volumetricVectors[1] = new Vector3f(0, yStep, 0);
      volumetricVectors[2] = new Vector3f(0, 0, zStep);
      Hashtable surfaceInfo = new Hashtable();
      surfaceInfo.put("volumetricOrigin", volumetricOrigin);
      surfaceInfo.put("voxelCounts", voxelCounts);
      surfaceInfo.put("volumetricVectors", volumetricVectors);
      surfaceInfo.put("nCubeData", new Integer(nData));
      surfaceInfo.put("voxelData", voxelData);
      atomSetCollection.setAtomSetAuxiliaryInfo("jmolSurfaceInfo", surfaceInfo);
      return;
    }
  }

  void processEndElement(String uri, String localName, String qName) {
    if ("atom".equals(localName)) {
      if (atom.elementSymbol != null && !Float.isNaN(atom.z)) {
        atomSetCollection.addAtomWithMappedName(atom);
      }
      atom = null;
      return;
    }
    keepChars = false;
    chars = null;
  }

  class Chem3dHandler extends JmolXmlHandler {

    Chem3dHandler() {
    }

    Chem3dHandler(XMLReader xmlReader) {
      setHandler(xmlReader, this);
    }
  }
}
