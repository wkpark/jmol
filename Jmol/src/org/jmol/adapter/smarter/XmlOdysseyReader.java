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

import netscape.javascript.JSObject;

import org.jmol.api.JmolAdapter;
import org.xml.sax.*;

/**
 * An Odyssey xodydata reader
 */

class XmlOdysseyReader extends XmlReader {

  /*
   * Enter any implemented field names in the 
   * implementedAttributes array. It is for when the XML 
   * is already loaded in the DOM of an XML page.
   * 
   */

  String[] odysseyImplementedAttributes = { "id", "label", //general 
      "xyz", "element", "hybrid", //atoms
      "a", "b", "order", //bond
  };

  String modelName = null;
  String formula = null;
  String phase = null;

  XmlOdysseyReader(XmlReader parent, AtomSetCollection atomSetCollection, BufferedReader reader, XMLReader xmlReader) {
    this.parent = parent;
    this.atomSetCollection = atomSetCollection;
    new OdysseyHandler(xmlReader);
    parseReaderXML(reader, xmlReader);
  }

  XmlOdysseyReader(XmlReader parent, AtomSetCollection atomSetCollection, JSObject DOMNode) {
    this.parent = parent;
    this.atomSetCollection = atomSetCollection;
    implementedAttributes = odysseyImplementedAttributes;
    ((OdysseyHandler) (new OdysseyHandler())).walkDOMTree(DOMNode);
  }

  void processStartElement(String namespaceURI, String localName, String qName,
                           HashMap atts) {

    if ("structure".equals(localName)) {
      atomSetCollection.newAtomSet();
      return;
    }

    if ("atom".equals(localName)) {
      atom = new Atom();
      if (atts.containsKey("label"))
        atom.atomName = (String) atts.get("label");
      else
        atom.atomName = (String) atts.get("id");
      if (atts.containsKey("xyz")) {
        String xyz = (String) atts.get("xyz");
        String[] tokens = getTokens(xyz);
        atom.x = parseFloat(tokens[0]);
        atom.y = parseFloat(tokens[1]);
        atom.z = parseFloat(tokens[2]);
      } 
      if (atts.containsKey("element")) {
        atom.elementSymbol = (String) atts.get("element");
      }
      
      return;
    }
    if ("bond".equals(localName)) {
      String atom1 = (String) atts.get("a");
      String atom2 = (String) atts.get("b");
      int order = 1;
      if (atts.containsKey("order"))
        order = parseBondToken((String) atts.get("order"));
      atomSetCollection.addNewBond(atom1, atom2, order);
      return;
    }

    if ("odyssey_simulation".equals(localName)) {
      if (modelName != null && phase != null)
        modelName += " - " + phase;
      if (modelName != null)
        atomSetCollection.setAtomSetName(modelName);
      if (formula != null)
        atomSetCollection.setAtomSetAuxiliaryInfo("formula", formula);
    }
    if ("title".equals(localName) || "formula".equals(localName)
        || "phase".equals(localName))
      keepChars = true;
  }

  int parseBondToken(String str) {
    if (str.length() >= 1) {
      switch (str.charAt(0)) {
      case 's':
        return 1;
      case 'd':
        return 2;
      case 't':
        return 3;
      case 'a':
        return JmolAdapter.ORDER_AROMATIC;
      }
      return parseInt(str);
    }
    return 1;
  }

  void processEndElement(String uri, String localName, String qName) {
    if ("atom".equals(localName)) {
      if (atom.elementSymbol != null && !Float.isNaN(atom.z)) {
        atomSetCollection.addAtomWithMappedName(atom);
      }
      atom = null;
      return;
    }
    if ("title".equals(localName)) {
      modelName = chars;
    }
    if ("formula".equals(localName)) {
      formula = chars;
    }
    if ("phase".equals(localName)) {
      phase = chars;
    }
    keepChars = false;
    chars = null;
  }

  class OdysseyHandler extends JmolXmlHandler {

    OdysseyHandler() {
    }

    OdysseyHandler(XMLReader xmlReader) {
      setHandler(xmlReader, this);
    }
  }
}
