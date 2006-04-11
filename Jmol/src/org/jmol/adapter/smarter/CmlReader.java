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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.HashMap;

import netscape.javascript.JSObject;

import org.jmol.api.JmolAdapter;

/**
 * A CML2 Reader - 
 * If passed a bufferedReader (from a file or inline string), we
 * generate a SAX parser and use callbacks to construct an
 * AtomSetCollection.
 * If passed a JSObject (from LiveConnect) we treat it as a JS DOM
 * tree, and walk the tree, (using the same processing as the SAX
 * parser) to construct the AtomSetCollection.
 */

/**
 * NB Note that the processing routines are shared between SAX 
 * and DOM processors. This means that attributes must be
 * transformed from either Attributes (SAX) or JSObjects (DOM)
 * to HashMap name:value pairs. 
 * This is done in startElement (SAX) or walkDOMTree (DOM)
 */

class CmlReader extends AtomSetCollectionReader {

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
    throws Exception {
    logger.log("readAtomSetCollection");
    return readAtomSetCollectionSax(reader, (new CmlHandler()),"cml");
  }

  AtomSetCollection readAtomSetCollectionSax(BufferedReader reader, Object handler, String name)
    throws Exception {
    atomSetCollection = new AtomSetCollection(name);

    logger.log("readAtomSetCollectionSax");
    
    XMLReader xmlr = getXmlReader();
    if (xmlr == null) {
      logger.log("No XML reader found");
      atomSetCollection.errorMessage = "No XML reader found";
      return atomSetCollection;
    }
    // logger.log("opening InputSource");
    InputSource is = new InputSource(reader);
    is.setSystemId("foo");
    // logger.log("setting features");
    xmlr.setFeature("http://xml.org/sax/features/validation", false);
    xmlr.setFeature("http://xml.org/sax/features/namespaces", true);
    xmlr.setEntityResolver((CmlHandler)handler);
    xmlr.setContentHandler((CmlHandler)handler);
    xmlr.setErrorHandler((CmlHandler)handler);

    xmlr.parse(is);
    
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }


  AtomSetCollection readAtomSetCollectionFromDOM(Object Node)
    throws Exception {
    JSObject DOMNode = (JSObject) Node;
    atomSetCollection = new AtomSetCollection("cml");
    logger.log("CmlReader.readAtomSetCollectionfromDOM\n");

    checkFirstNode(DOMNode);

    walkDOMTree(DOMNode);

    // logger.log("atom count:");
    // logger.log(atomSetCollection.atomCount);

    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }

  XMLReader getXmlReader() {
    XMLReader xmlr = null;
    // JAXP is preferred (comes with Sun JVM 1.4.0 and higher)
    if (xmlr == null &&
      System.getProperty("java.version").compareTo("1.4") >= 0)
      xmlr = allocateXmlReader14();
    // Aelfred is the first alternative.
    if (xmlr == null)
      xmlr = allocateXmlReaderAelfred2();
    return xmlr;
  }

  XMLReader allocateXmlReader14() {
    XMLReader xmlr = null;
    try {
      javax.xml.parsers.SAXParserFactory spf =
        javax.xml.parsers.SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      javax.xml.parsers.SAXParser saxParser = spf.newSAXParser();
      xmlr = saxParser.getXMLReader();
      logger.log("Using JAXP/SAX XML parser.");
    } catch (Exception e) {
      logger.log("Could not instantiate JAXP/SAX XML reader: " +
                         e.getMessage());
    }
    return xmlr;
  }
  
  XMLReader allocateXmlReaderAelfred2() {
    XMLReader xmlr = null;
    try {
      xmlr = (XMLReader)this.getClass().getClassLoader().
        loadClass("gnu.xml.aelfred2.XmlReader").newInstance();
      logger.log("Using Aelfred2 XML parser.");
    } catch (Exception e) {
      logger.log("Could not instantiate Aelfred2 XML reader!");
    }
    return xmlr;
  }

  ////////////////////////////////////////////////////////////////
  // Main body of class; variablaes & functiopns shared by DOM & SAX alike.

  Atom atom;
  float[] notionalUnitcell;

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

  // this param is used to keep track of the parent element type
  int elementContext;
  final static int UNSET = 0;
  final static int CRYSTAL = 1;
  final static int ATOM = 2;

  // this param is used to signal that chars should be kept
  boolean keepChars;
  String chars;
    
  // do some bookkeeping of attrib value across the element
  String dictRef;
  String title;
    
  // this routine breaks out all the tokens in a string
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
    
  int parseBondToken(String str) {
    if (str.length() == 1) {
      switch (str.charAt(0)) {
      case 'S':
        return 1;
      case 'D':
        return 2;
      case 'T':
        return 3;
      case 'A':
        return JmolAdapter.ORDER_AROMATIC;
      }
      return parseInt(str);
    }
    if (str.equals("partial01"))
      return JmolAdapter.ORDER_PARTIAL01;
    if (str.equals("partial12"))
      return JmolAdapter.ORDER_PARTIAL12;
    float floatOrder = parseFloat(str);
    if (floatOrder == 1.5)
      return JmolAdapter.ORDER_AROMATIC;
    if (floatOrder == 2)
      return 2;
    if (floatOrder == 3)
      return 3;
    return 1;
  }

  void breakOutAtomTokens(String str) {
    breakOutTokens(str);
    checkAtomArrayLength(tokenCount);
  }
    
  void checkAtomArrayLength(int newAtomCount) {
    if (atomCount == 0) {
      if (newAtomCount > atomArray.length)
        atomArray = new Atom[newAtomCount];
      for (int i = newAtomCount; --i >= 0; )
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
      for (int i = newBondCount; --i >= 0; )
        bondArray[i] = new Bond();
      bondCount = newBondCount;
    } else if (newBondCount != bondCount) {
      throw new IndexOutOfBoundsException("bad bond attribute length");
    }
  }

  ////////////////////////////////////////////////////////////////

  void checkFirstNode(JSObject DOMNode) {
    // System.out.println("checkFirstNode");
    if (DOMNode == null)
      throw new RuntimeException("Not a node");
    String namespaceURI = (String) DOMNode.getMember("namespaceURI");
    String localName = (String) DOMNode.getMember("localName");

    if (("http://www.xml-cml.org/schema/cml2/core"!=namespaceURI)||
        ("cml"!=localName))
        new RuntimeException("Not a cml:cml node");
  }
    

  // walk dom tree given by JSObject. For every element, call
  // startElement with the appropriate strings etc., and then
  // endElement when the element is closed.

  void walkDOMTree(JSObject DOMNode) {

    String namespaceURI = (String) DOMNode.getMember("namespaceURI");
    String localName = (String) DOMNode.getMember("localName");
    String qName = (String) DOMNode.getMember("nodeName");
    JSObject attributes = (JSObject) DOMNode.getMember("attributes");

    HashMap atts;
    if (attributes!=null) // in case this is a text or other weird sort of node.
      atts = attributesToHashMap(attributes);
    else
      atts = new HashMap(0);

    // put the attributes all into a name:value HashMap for processing.
    // This should be as easy as the code snippet below.
    // Unfortunately, Opera 8.5 doesn't work properly with that, so
    // we have to use attributesToHashMap.
    //HashMap atts = new HashMap(numAtts);
    //for (int i = 0; i < numAtts; i++) {
    //  String attLocalName = (String) ((JSObject) attributes.getSlot(i)).getMember("localName");
    //  String attValue = (String) ((JSObject) attributes.getSlot(i)).getMember("value");
    //  atts.put(attLocalName, attValue);
    //}
     

    //if ("http://www.xml-cml.org/schema/cml2/core"!=namespaceURI)
    //    return;
      
    processStartElement(namespaceURI, localName, qName, atts);
    if (((Boolean) DOMNode.call("hasChildNodes", (Object[]) null)).booleanValue()) {
      for (JSObject nextNode =  (JSObject) DOMNode.getMember("firstChild"); 
      nextNode != (JSObject) null; 
      nextNode =  (JSObject) nextNode.getMember("nextSibling"))
      walkDOMTree(nextNode);
    }
    processEndElement(namespaceURI, localName, qName);
  }

  HashMap attributesToHashMap(JSObject attributes) {
    // list of all attributes we might be interested in:
    Object[] interestingAtts = 
      { "title", "id", "x3", "y3", "z3", "x2", "y2",
        "elementType", "formalCharge", "atomId",
        "atomRefs2", "order", "atomRef1", "atomRef2",
        "dictRef" };

    int numAtts =  ((Number) attributes.getMember("length")).intValue();
    HashMap atts = new HashMap(numAtts);
    for (int i = interestingAtts.length; --i >= 0; ) {
      Object[] attArgs = { interestingAtts[i] };
      JSObject attNode = (JSObject) attributes.call("getNamedItem", attArgs);
      if (attNode != null) {
        String attLocalName = (String) attNode.getMember("name");
        String attValue = (String) attNode.getMember("value");
        atts.put(attLocalName, attValue);
      }
    }
    return atts;
  }

  int moleculeNesting = 0;

  void processStartElement(String namespaceURI, String localName,
                           String qName, HashMap atts) {
      
    if ("molecule".equals(localName)) {
      //  logger.log("found molecule");
      if (++moleculeNesting > 1)
        return;
      atomSetCollection.newAtomSet();
      String collectionName = null;
      if (atts.containsKey("title"))
        collectionName = (String) atts.get("title");
      else if (atts.containsKey("id")) 
        collectionName = (String) atts.get("id");
      if (collectionName != null) {
        atomSetCollection.setAtomSetName(collectionName);
      }
      return;
    }
    if ("atom".equals(localName)) {
      //logger.log("found atom");
      elementContext = ATOM;
      atom = new Atom();
      boolean coords3D = false;
      atom.atomName = (String) atts.get("id");
      if (atts.containsKey("x3")) {
        coords3D = true;
        atom.x = parseFloat((String) atts.get("x3"));
        atom.y = parseFloat((String) atts.get("y3"));
        atom.z = parseFloat((String) atts.get("z3"));
      }
      if (atts.containsKey("x2")) {
        if (Float.isNaN(atom.x))
          atom.x = parseFloat((String) atts.get("x2"));
      }
      if (atts.containsKey("y2")) {
        if (Float.isNaN(atom.y))
          atom.y = parseFloat((String) atts.get("y2"));
      }
      if (atts.containsKey("elementType"))
        atom.elementSymbol = (String) atts.get("elementType");
      if (atts.containsKey("formalCharge"))
        atom.formalCharge = parseInt((String) atts.get("formalCharge"));
      if (! coords3D) {
        atom.z = 0;
      }
      return;
    }
    if ("atomArray".equals(localName)) {
      //  logger.log("found atomArray");
      atomCount = 0;
      boolean coords3D = false;
      if (atts.containsKey("atomID")) {
        breakOutAtomTokens((String) atts.get("atomID"));
        for (int i = tokenCount; --i >= 0; )
          atomArray[i].atomName = tokens[i];
      }
      if (atts.containsKey("x3")) {
        coords3D = true;
        breakOutAtomTokens((String) atts.get("x3"));
        for (int i = tokenCount; --i >= 0; )
          atomArray[i].x = parseFloat(tokens[i]);
      }
      if (atts.containsKey("y3")) {
        breakOutAtomTokens((String) atts.get("y3"));
        for (int i = tokenCount; --i >= 0; )
          atomArray[i].y = parseFloat(tokens[i]);
      }
      if (atts.containsKey("z3")) {
        breakOutAtomTokens((String) atts.get("z3"));
        for (int i = tokenCount; --i >= 0; )
          atomArray[i].z = parseFloat(tokens[i]);
      }
      if (atts.containsKey("x2")) {
        breakOutAtomTokens((String) atts.get("x2"));
        for (int i = tokenCount; --i >= 0; )
          atomArray[i].x = parseFloat(tokens[i]);
      }
      if (atts.containsKey("y2")) {
        breakOutAtomTokens((String) atts.get("y2"));
        for (int i = tokenCount; --i >= 0; )
          atomArray[i].y = parseFloat(tokens[i]);
      }
      if (atts.containsKey("elementType")) {
        breakOutAtomTokens((String) atts.get("elementType"));
        for (int i = tokenCount; --i >= 0; )
          atomArray[i].elementSymbol = tokens[i];
      }
      for (int i = atomCount; --i >= 0; ) {
        Atom atom = atomArray[i];
        if (! coords3D)
          atom.z = 0;
      }
      return;
    }
    if ("bond".equals(localName)) {
      //  <bond atomRefs2="a20 a21" id="b41" order="2"/>
      int order = -1;
      if (atts.containsKey("atomRefs2"))
        breakOutTokens((String) atts.get("atomRefs2"));
      if (atts.containsKey("order"))
        order = parseBondToken((String) atts.get("order"));
      if (tokenCount == 2 && order > 0) {
        atomSetCollection.addNewBond(tokens[0], tokens[1], order);
      }
      return;
    }
    if ("bondArray".equals(localName)) {
      bondCount = 0;
      if (atts.containsKey("order")) {
        breakOutBondTokens((String) atts.get("order"));
        for (int i = tokenCount; --i >= 0; )
          bondArray[i].order = parseBondToken(tokens[i]);
      }
      if (atts.containsKey("atomRef1")) {
        breakOutBondTokens((String) atts.get("atomRef1"));
        for (int i = tokenCount; --i >= 0; )
          bondArray[i].atomIndex1 = atomSetCollection.getAtomNameIndex(tokens[i]);
      }
      if (atts.containsKey("atomRef2")) {
        breakOutBondTokens((String) atts.get("atomRef2"));
        for (int i = tokenCount; --i >= 0; )
          bondArray[i].atomIndex2 = atomSetCollection.getAtomNameIndex(tokens[i]);
      }
      return;
    }
    if ("crystal".equals(localName)) {
      elementContext = CRYSTAL;
      notionalUnitcell = new float[6];
      for (int i = 6; --i >= 0; )
        notionalUnitcell[i] = Float.NaN;
      return;
    }
    if ("scalar".equals(localName)) {
      title = (String) atts.get("title");
      dictRef = (String) atts.get("dictRef");
      keepChars = true;
      return;
    }
  }
    
  void processEndElement(String uri, String localName, String qName)  {
    if ("molecule".equals(localName)) {
      --moleculeNesting;
      return;
    }
    if ("atom".equals(localName)) {
      if (atom.elementSymbol != null &&
          ! Float.isNaN(atom.z)) {
        atomSetCollection.addAtomWithMappedName(atom);
          
        /*  logger.log(" I just added an atom of type "
                             + atom.elementSymbol +
                             " @ " + atom.x + "," + atom.y + "," + atom.z); */
         
      }
      atom = null;
      elementContext = UNSET;
      return;
    }
    if ("crystal".equals(localName)) {
      elementContext = UNSET;
      for (int i = 6; --i >= 0; )
        if (Float.isNaN(notionalUnitcell[i])) {
          logger.log("incomplete/unrecognized unitcell");
          return;
        }
      atomSetCollection.notionalUnitcell = notionalUnitcell;
      return;
    }
    if ("scalar".equals(localName)) {
      if (elementContext == CRYSTAL) {
        //          logger.log("CRYSTAL atts.title: " + title);
        if (title != null) {
          int i = 6;
          while (--i >= 0 && !
                 title.equals(AtomSetCollection.notionalUnitcellTags[i]))
          { }
          if (i >= 0)
            notionalUnitcell[i] = parseFloat(chars);
        }
        //          logger.log("CRYSTAL atts.dictRef: " + dictRef);
        if (dictRef != null) {
          int i = 6;
          while (--i >= 0 &&
                 ! dictRef.equals("cif:" + CifReader.cellParamNames[i]))
          { }
          if (i >= 0)
            notionalUnitcell[i] = parseFloat(chars);
        }
        return;
      }
      if (elementContext == ATOM) {
        if ("jmol:charge".equals(dictRef)) {
          atom.partialCharge = parseFloat(chars);
          //logger.log("jmol.partialCharge=" + atom.partialCharge);
        }
      }
      return;
    }
    if ("atomArray".equals(localName)) {
      //    logger.log("adding atomArray:" + atomCount);
      for (int i = 0; i < atomCount; ++i) {
        Atom atom = atomArray[i];
        if (atom.elementSymbol != null &&
            ! Float.isNaN(atom.z))
          atomSetCollection.addAtomWithMappedName(atom);
      }
      return;
    }
    if ("bondArray".equals(localName)) {
      //        logger.log("adding bondArray:" + bondCount);
      for (int i = 0; i < bondCount; ++i)
        atomSetCollection.addBond(bondArray[i]);
      return;
    }

    keepChars = false;
    title = null;
    dictRef = null;
    chars = null;
  }

  class CmlHandler extends DefaultHandler implements ErrorHandler {
     
    public void startDocument() {
    }

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attributes) {
        
      /* logger.log("startElement(" + namespaceURI + "," + localName +
                    "," + qName + ")"); */
      HashMap atts = new HashMap(attributes.getLength());
      for (int i = attributes.getLength(); --i >= 0; )
        atts.put(attributes.getLocalName(i), attributes.getValue(i));

      processStartElement(namespaceURI, localName, qName, atts);
    }

    public void endElement(String uri, String localName, String qName)  {
      
      /* logger.log("endElement(" + uri + "," + localName +
                    "," + qName + ")"); */
      processEndElement(uri, localName, qName);
      keepChars = false;
      title = null;
      dictRef = null;
      chars = null;
    }

    public void characters(char[] ch, int start, int length) {
      //logger.log("End chars: " + new String(ch, start, length));
      if (keepChars) {
        if (chars == null) {
          chars = new String(ch, start, length);
        } else {
          chars += new String(ch, start, length);
        }
      }
    }  

    // Methods for entity resolving, e.g. getting a DTD resolved
    
    public InputSource resolveEntity(String name, String publicId,
                                     String baseURI, String systemId) {
      logger.log("Not resolving this:");
      logger.log("      name: " + name);
      logger.log("  systemID: " + systemId);
      logger.log("  publicID: " + publicId);
      logger.log("   baseURI: " + baseURI);
      return null;
    }
    
    public InputSource resolveEntity (String publicId, String systemId) {
      logger.log("Not resolving this:");
      logger.log("  publicID: " + publicId);
      logger.log("  systemID: " + systemId);
      return null;
    }
    
    public void error (SAXParseException exception)  {
      logger.log("SAX ERROR:" + exception.getMessage());
    }
    
    public void fatalError (SAXParseException exception)  {
      logger.log("SAX FATAL:" + exception.getMessage());
    }
    
    public void warning (SAXParseException exception)  {
      logger.log("SAX WARNING:" + exception.getMessage());
    }
  }
}
