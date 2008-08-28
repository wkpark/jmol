/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 09:53:18 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5561 $
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
package org.jmol.adapter.readers.xml;

import org.jmol.adapter.smarter.*;


import java.io.BufferedReader;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import netscape.javascript.JSObject;

import org.jmol.api.JmolAdapter;
import org.xml.sax.XMLReader;
import org.jmol.adapter.readers.cifpdb.CifReader;
import org.jmol.util.Logger;

/**
 * A CML2 Reader - 
 * If passed a bufferedReader (from a file or inline string), we
 * generate a SAX parser and use callbacks to construct an
 * AtomSetCollection.
 * If passed a JSObject (from LiveConnect) we treat it as a JS DOM
 * tree, and walk the tree, (using the same processing as the SAX
 * parser) to construct the AtomSetCollection.
 * 
 * symmetry added by Bob Hanson:
 * 
 *  setSpaceGroupName()
 *  setUnitCellItem()
 *  setFractionalCoordinates()
 *  setAtomCoord()
 *  applySymmetry()
 *
 */

/* TODO 9/06
 * 
 *  
 *  We need to implement the capability to load a specific
 *  model as well as indicate the number of unit cells to load. 
 *  
 * Follow the equivalent in CIF files to see how this is done. 
 * 
 */

public class XmlCmlReader extends XmlReader {


  /*
   * Enter any implemented field names in the 
   * implementedAttributes array. It is for when the XML 
   * is already loaded in the DOM of an XML page.
   * 
   */

  String[] cmlImplementedAttributes = { "id", //general
      "title", //molecule
      "x3", "y3", "z3", "x2", "y2", //atom 
      "elementType", "formalCharge", //atom
      "atomId", //atomArray
      "atomRefs2", "order", //bond
      "atomRef1", "atomRef2", //bondArray
      "dictRef", //scalar
      "spaceGroup", //symmetry
  };

  ////////////////////////////////////////////////////////////////
  // Main body of class; variablaes & functiopns shared by DOM & SAX alike.

  // the same atom array gets reused
  // it will grow to the maximum length;
  // atomCount holds the current number of atoms
  int atomCount;
  Atom[] atomArray = new Atom[100];

  int bondCount;
  Bond[] bondArray = new Bond[100];

  // the same string array gets reused
  // tokenCount holds the current number of tokens
  // see breakOutTokens
  int tokenCount;
  String[] tokens = new String[16];

  int moduleCount = 0;
  boolean haveMolecule = false;
  String localSpaceGroupName;
  /**
   * state constants
   */
  final protected int START = 0, 
    CML = 1, 
    CRYSTAL = 2, 
    CRYSTAL_SCALAR = 3,
    CRYSTAL_SYMMETRY = 4, 
    CRYSTAL_SYMMETRY_TRANSFORM3 = 5, 
    MOLECULE = 6,
    MOLECULE_ATOM_ARRAY = 7, 
    MOLECULE_ATOM = 8, 
    MOLECULE_ATOM_SCALAR = 9,
    MOLECULE_BOND_ARRAY = 10, 
    MOLECULE_BOND = 11, 
    MOLECULE_FORMULA = 12,
    MOLECULE_ATOM_BUILTIN = 13, 
    MOLECULE_BOND_BUILTIN = 14,
    MODULE = 15,
    SYMMETRY = 17;

  /**
   * the current state
   */
  protected int state = START;

  /*
   * added 2/2007  Bob Hanson:
   * 
   * <crystal id="struct" dictRef="castep:ucell">
      <cellParameter latticeType="real" parameterType="length"
        units="castepunits:a">4.592100143433e0 4.592100143433e0 2.957400083542e0</cellParameter>
      <cellParameter latticeType="real" parameterType="angle"
        units="castepunits:degree">9.000000000000e1 9.000000000000e1 9.000000000000e1</cellParameter>
    </crystal>

   * 
   */

  XmlCmlReader() {
  }

  XmlCmlReader(XmlReader parent, AtomSetCollection atomSetCollection,
      BufferedReader reader, XMLReader xmlReader) {
    this.parent = parent;
    this.reader = reader;
    this.atomSetCollection = atomSetCollection;
    new CmlHandler(xmlReader);
    parseReaderXML(xmlReader);
  }

  XmlCmlReader(XmlReader parent, AtomSetCollection atomSetCollection,
      JSObject DOMNode) {
    this.parent = parent;
    this.atomSetCollection = atomSetCollection;
    implementedAttributes = cmlImplementedAttributes;
    ((CmlHandler) (new CmlHandler())).walkDOMTree(DOMNode);
  }

  String scalarDictRef;
  //String scalarDictKey;
  String scalarDictValue;
  String scalarTitle;
  String cellParameterType;

  // counter that is incremented each time a molecule element is started and 
  // decremented when finished.  Needed so that only 1 atomSet created for each
  // parent molecule that exists.
  int moleculeNesting = 0;
  boolean embeddedCrystal = false;

  public void processStartElement(String uri, String name, String qName,
                                  HashMap atts) {
    //if (!uri.equals(NAMESPACE_URI))
    //return;
/*
    try {
      System.out.println(name + "::"+atts.get("name"));
    } catch (Exception e) {
      System.out.println(name);
    }
  */  
    switch (state) {
    case START:
      if (name.equals("molecule")) {
        state = MOLECULE;
        haveMolecule = true;
        if (moleculeNesting == 0) {
          createNewAtomSet(atts);
        }
        moleculeNesting++;
      } else if (name.equals("crystal")) {
        state = CRYSTAL;
      } else if (name.equals("symmetry")) {
        state = SYMMETRY;
        if (atts.containsKey("spaceGroup")) {
          localSpaceGroupName = (String) atts.get("spaceGroup");
          applySymmetry();
        }
      } else if (name.equals("module")) {
        moduleCount++;      
      }

      break;
    case CRYSTAL:
      if (name.equals("scalar")) {
        state = CRYSTAL_SCALAR;
        setKeepChars(true);
        scalarTitle = (String) atts.get("title");
        scalarDictRef = (String) atts.get("dictRef");
        if (scalarDictRef != null) {
          int iColon = scalarDictRef.indexOf(":");
          scalarDictValue = scalarDictRef.substring(iColon + 1);
          //scalarDictKey = scalarDictRef
            //  .substring(0, (iColon >= 0 ? iColon : 0));
        }
      } else if (name.equals("symmetry")) {
        state = CRYSTAL_SYMMETRY;
        if (atts.containsKey("spaceGroup")) {
          localSpaceGroupName = (String) atts.get("spaceGroup");
          for (int i = 0; i < localSpaceGroupName.length(); i++)
            if (localSpaceGroupName.charAt(i) == '_')
              localSpaceGroupName = localSpaceGroupName.substring(0, i)
                  + localSpaceGroupName.substring((i--) + 1);
        }
      } else if (name.equals("cellParameter")) {
        if (atts.containsKey("parameterType")) {
          cellParameterType = (String) atts.get("parameterType");
          setKeepChars(true);
        }
      }
      break;
    case SYMMETRY:
      break;
    case CRYSTAL_SCALAR:
    case CRYSTAL_SYMMETRY:
      if (name.equals("transform3")) {
        state = CRYSTAL_SYMMETRY_TRANSFORM3;
      }
      break;
    case CRYSTAL_SYMMETRY_TRANSFORM3:
    case MOLECULE:
      if (name.equals("crystal")) {
        state = CRYSTAL;
        embeddedCrystal = true;
      }
      if (name.equals("molecule")) {
        state = MOLECULE;
        moleculeNesting++;
      }
      if (name.equals("bondArray")) {
        state = MOLECULE_BOND_ARRAY;
        bondCount = 0;
        if (atts.containsKey("order")) {
          breakOutBondTokens((String) atts.get("order"));
          for (int i = tokenCount; --i >= 0;)
            bondArray[i].order = parseBondToken(tokens[i]);
        }
        if (atts.containsKey("atomRef1")) {
          breakOutBondTokens((String) atts.get("atomRef1"));
          for (int i = tokenCount; --i >= 0;)
            bondArray[i].atomIndex1 = atomSetCollection
                .getAtomNameIndex(tokens[i]);
        }
        if (atts.containsKey("atomRef2")) {
          breakOutBondTokens((String) atts.get("atomRef2"));
          for (int i = tokenCount; --i >= 0;)
            bondArray[i].atomIndex2 = atomSetCollection
                .getAtomNameIndex(tokens[i]);
        }
      }
      if (name.equals("atomArray")) {
        state = MOLECULE_ATOM_ARRAY;
        atomCount = 0;
        boolean coords3D = false;
        if (atts.containsKey("atomID")) {
          breakOutAtomTokens((String) atts.get("atomID"));
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].atomName = tokens[i];
        }
        if (atts.containsKey("x3")) {
          coords3D = true;
          breakOutAtomTokens((String) atts.get("x3"));
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].x = parseFloat(tokens[i]);
        }
        if (atts.containsKey("y3")) {
          breakOutAtomTokens((String) atts.get("y3"));
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].y = parseFloat(tokens[i]);
        }
        if (atts.containsKey("z3")) {
          breakOutAtomTokens((String) atts.get("z3"));
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].z = parseFloat(tokens[i]);
        }
        if (atts.containsKey("x2")) {
          breakOutAtomTokens((String) atts.get("x2"));
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].x = parseFloat(tokens[i]);
        }
        if (atts.containsKey("y2")) {
          breakOutAtomTokens((String) atts.get("y2"));
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].y = parseFloat(tokens[i]);
        }
        if (atts.containsKey("elementType")) {
          breakOutAtomTokens((String) atts.get("elementType"));
          for (int i = tokenCount; --i >= 0;)
            atomArray[i].elementSymbol = tokens[i];
        }
        for (int i = atomCount; --i >= 0;) {
          Atom atom = atomArray[i];
          if (!coords3D)
            atom.z = 0;
          parent.setAtomCoord(atom);
        }
      }
      if (name.equals("formula")) {
        state = MOLECULE_FORMULA;
      }
      break;
    case MOLECULE_BOND_ARRAY:
      if (name.equals("bond")) {
        state = MOLECULE_BOND;
        int order = -1;
        tokenCount = 0;
        if (atts.containsKey("atomRefs2"))
          breakOutTokens((String) atts.get("atomRefs2"));
        if (atts.containsKey("order"))
          order = parseBondToken((String) atts.get("order"));
        if (tokenCount == 2 && order > 0) {
          atomSetCollection.addNewBond(tokens[0], tokens[1], order);
        }
      }
      break;
    case MOLECULE_ATOM_ARRAY:
      if (name.equals("atom")) {
        state = MOLECULE_ATOM;
        atom = new Atom();
        boolean coords3D = false;
        boolean coordsFractional = false;
        parent.setFractionalCoordinates(false);
        if (atts.containsKey("label"))
          atom.atomName = (String) atts.get("label");
        else
          atom.atomName = (String) atts.get("id");
        if (atts.containsKey("xFract")
            && (parent.iHaveUnitCell || !atts.containsKey("x3"))) {
          coords3D = coordsFractional = true;
          parent.setFractionalCoordinates(true);
          parent.setAtomCoord(atom, parseFloat((String) atts.get("xFract")),
              parseFloat((String) atts.get("yFract")), parseFloat((String) atts
                  .get("zFract")));
        }
        if (atts.containsKey("x3") && !coordsFractional) {
          coords3D = true;
          atom.x = parseFloat((String) atts.get("x3"));
          atom.y = parseFloat((String) atts.get("y3"));
          atom.z = parseFloat((String) atts.get("z3"));
        }
        if (atts.containsKey("x2") && !coords3D ) {
          atom.x = parseFloat((String) atts.get("x2"));
          atom.y = parseFloat((String) atts.get("y2"));
          atom.z = 0;
        }
        parent.setAtomCoord(atom);
        if (atts.containsKey("elementType"))
          atom.elementSymbol = (String) atts.get("elementType");
        if (atts.containsKey("formalCharge"))
          atom.formalCharge = parseInt((String) atts.get("formalCharge"));
      }

      break;
    case MOLECULE_BOND:
      if (atts.containsKey("builtin")) {
        setKeepChars(true);
        state = MOLECULE_BOND_BUILTIN;
        scalarDictValue = (String) atts.get("builtin"); 
      }
      break;
    case MOLECULE_ATOM:
      if (name.equals("scalar")) {
        state = MOLECULE_ATOM_SCALAR;
        setKeepChars(true);
        scalarTitle = (String) atts.get("title");
        scalarDictRef = (String) atts.get("dictRef");
        if (scalarDictRef != null) {
          int iColon = scalarDictRef.indexOf(":");
          scalarDictValue = scalarDictRef.substring(iColon + 1);
          //scalarDictKey = scalarDictRef
            //  .substring(0, (iColon >= 0 ? iColon : 0));
        }
      } else if (atts.containsKey("builtin")) {
        setKeepChars(true);
        state = MOLECULE_ATOM_BUILTIN;
        scalarDictValue = (String) atts.get("builtin"); 
      }
      break;
    case MOLECULE_ATOM_SCALAR:
      break;
    case MOLECULE_FORMULA:
      break;
    case MOLECULE_ATOM_BUILTIN:
      break;
    case MOLECULE_BOND_BUILTIN:
      break;
    }
  }

  public void processEndElement(String uri, String name, String qName) {
    //if (!uri.equals(NAMESPACE_URI))
      //return;
    switch (state) {
    case START:
      if (name.equals("module")) {
        moduleCount--;
        applySymmetry();
      }
      break;
    case CRYSTAL:
      if (name.equals("crystal")) {
        if (embeddedCrystal) {
          state = MOLECULE;
          embeddedCrystal = false;
        } else {
          state = START;
        }
      } else if (name.equals("cellParameter") && keepChars) {
        String[] tokens = getTokens(chars);
        setKeepChars(false);
        if (tokens.length != 3 || cellParameterType == null) {
        } else if (cellParameterType.equals("length")) {
          parent.needToApplySymmetry = true;
          for (int i = 0; i < 3; i++) 
            parent.setUnitCellItem(i, parseFloat(tokens[i]));
          break;
        } else if (cellParameterType.equals("angle")) {
          for (int i = 0; i < 3; i++) 
            parent.setUnitCellItem(i + 3, parseFloat(tokens[i]));
          applySymmetry();
          break;
        }
        //if here, then something is wrong
        Logger.error("bad cellParameter information: parameterType="
            + cellParameterType + " data=" + chars);
        parent.setFractionalCoordinates(false);
      }
      break;
    case CRYSTAL_SCALAR:
      if (name.equals("scalar")) {
        state = CRYSTAL;
        if (scalarTitle != null) {
          int i = 6;
          while (--i >= 0
              && !scalarTitle.equals(AtomSetCollection.notionalUnitcellTags[i])) {
          }
          if (i >= 0)
            parent.setUnitCellItem(i, parseFloat(chars));
        }
        if (scalarDictRef != null) {
          int i = 6;
          while (--i >= 0
              && !scalarDictValue.equals(CifReader.cellParamNames[i])) {
          }
          if (i >= 0)
            parent.setUnitCellItem(i, parseFloat(chars));
        }
      }
      setKeepChars(false);
      scalarTitle = null;
      scalarDictRef = null;
      break;
    case CRYSTAL_SYMMETRY:
      if (name.equals("symmetry")) {
        state = CRYSTAL;
      }
      break;
    case CRYSTAL_SYMMETRY_TRANSFORM3:
      if (name.equals("transform3")) {
        state = CRYSTAL_SYMMETRY;
      }
      break;
    case SYMMETRY:
      if (name.equals("symmetry")) {
        state = START;
      }
      break;
    case MOLECULE:
      if (name.equals("molecule")) {
        if (--moleculeNesting == 0) {
          // if <molecule> is within <module>, then
          // we have to wait until the end of <module> to
          // apply symmetry.
          applySymmetry();
          state = START;
        } else {
          state = MOLECULE;
        }
      }
      break;
    case MOLECULE_BOND_ARRAY:
      if (name.equals("bondArray")) {
        state = MOLECULE;
        for (int i = 0; i < bondCount; ++i) {
          atomSetCollection.addBond(bondArray[i]);
        }
      }
      break;
    case MOLECULE_ATOM_ARRAY:
      if (name.equals("atomArray")) {
        state = MOLECULE;
        for (int i = 0; i < atomCount; ++i) {
          Atom atom = atomArray[i];
          if (atom.elementSymbol != null && !Float.isNaN(atom.z))
            atomSetCollection.addAtomWithMappedName(atom);
        }
      }
      break;
    case MOLECULE_BOND:
      if (name.equals("bond")) {
        state = MOLECULE_BOND_ARRAY;
      }
      break;
    case MOLECULE_ATOM:
      if (name.equals("atom")) {
        state = MOLECULE_ATOM_ARRAY;
        if (atom.elementSymbol != null && !Float.isNaN(atom.z)) {
          atomSetCollection.addAtomWithMappedName(atom);
        }
        atom = null;
      }
      break;
    case MOLECULE_ATOM_SCALAR:
      if (name.equals("scalar")) {
        state = MOLECULE_ATOM;
        if ("jmol:charge".equals(scalarDictRef)) {
          atom.partialCharge = parseFloat(chars);
        }
      }
      setKeepChars(false);
      scalarTitle = null;
      scalarDictRef = null;
      break;
    case MOLECULE_ATOM_BUILTIN:
      state = MOLECULE_ATOM;
      if (scalarDictValue.equals("x3"))
        atom.x = parseFloat(chars);
      else if (scalarDictValue.equals("y3"))
        atom.y = parseFloat(chars);
      else if (scalarDictValue.equals("z3"))
        atom.z = parseFloat(chars);
      else if (scalarDictValue.equals("elementType"))
        atom.elementSymbol = chars;
      setKeepChars(false);
      break;
    case MOLECULE_BOND_BUILTIN:  //ACD Labs
      state = MOLECULE_BOND;
      if (scalarDictValue.equals("atomRef")) {
        if (tokenCount == 0)
          tokens = new String[2];
        if (tokenCount < 2)
          tokens[tokenCount++] = chars;
      } else if (scalarDictValue.equals("order")) {
        int order = parseBondToken(chars);
        if (order > 0 && tokenCount == 2)
          atomSetCollection.addNewBond(tokens[0], tokens[1], order);
      }
      setKeepChars(false);
      break;
    case MOLECULE_FORMULA:
      state = MOLECULE;
      break;
    }
  }

  int parseBondToken(String str) {
    float floatOrder = parseFloat(str);
    if (Float.isNaN(floatOrder) && str.length() >= 1) {
      str = str.toUpperCase();
      switch (str.charAt(0)) {
      case 'S':
        return JmolAdapter.ORDER_COVALENT_SINGLE;
      case 'D':
        return JmolAdapter.ORDER_COVALENT_DOUBLE;
      case 'T':
        return JmolAdapter.ORDER_COVALENT_TRIPLE;
      case 'A':
        return JmolAdapter.ORDER_AROMATIC;
      }
      return parseInt(str);
    }
    if (floatOrder == 1.5)
      return JmolAdapter.ORDER_AROMATIC;
    if (floatOrder == 2)
      return JmolAdapter.ORDER_COVALENT_DOUBLE;
    if (floatOrder == 3)
      return JmolAdapter.ORDER_COVALENT_TRIPLE;
    return JmolAdapter.ORDER_COVALENT_SINGLE;
  }

  //this routine breaks out all the tokens in a string
  // results are placed into the tokens array
  void breakOutTokens(String str) {
    StringTokenizer st = new StringTokenizer(str);
    tokenCount = st.countTokens();
    if (tokenCount > tokens.length)
      tokens = new String[tokenCount];
    for (int i = 0; i < tokenCount; ++i) {
      try {
        tokens[i] = st.nextToken();
      } catch (NoSuchElementException nsee) {
        tokens[i] = null;
      }
    }
  }

  void breakOutAtomTokens(String str) {
    breakOutTokens(str);
    checkAtomArrayLength(tokenCount);
  }

  void checkAtomArrayLength(int newAtomCount) {
    if (atomCount == 0) {
      if (newAtomCount > atomArray.length)
        atomArray = new Atom[newAtomCount];
      for (int i = newAtomCount; --i >= 0;)
        atomArray[i] = new Atom();
      atomCount = newAtomCount;
    } else if (newAtomCount != atomCount) {
      throw new IndexOutOfBoundsException("bad atom attribute length");
    }
  }

  void breakOutBondTokens(String str) {
    breakOutTokens(str);
    checkBondArrayLength(tokenCount);
  }

  void checkBondArrayLength(int newBondCount) {
    if (bondCount == 0) {
      if (newBondCount > bondArray.length)
        bondArray = new Bond[newBondCount];
      for (int i = newBondCount; --i >= 0;)
        bondArray[i] = new Bond();
      bondCount = newBondCount;
    } else if (newBondCount != bondCount) {
      throw new IndexOutOfBoundsException("bad bond attribute length");
    }
  }

  private void createNewAtomSet(HashMap atts) {
    atomSetCollection.newAtomSet();
    String collectionName = null;
    if (atts.containsKey("title"))
      collectionName = (String) atts.get("title");
    else if (atts.containsKey("id"))
      collectionName = (String) atts.get("id");
    if (collectionName != null) {
      atomSetCollection.setAtomSetName(collectionName);
    }
  }
  
  public void applySymmetry() {
    if (moduleCount > 0 || !haveMolecule)
      return;
    if (localSpaceGroupName == null)
      return;
    parent.setSpaceGroupName(localSpaceGroupName);
    try {
      parent.applySymmetry();
    } catch (Exception e) {
      e.printStackTrace();
      Logger.error("applySymmetry failed: " + e);
    }
  }

  class CmlHandler extends JmolXmlHandler {

    CmlHandler() {
    }

    CmlHandler(XMLReader xmlReader) {
      setHandler(xmlReader, this);
    }

  }

}
