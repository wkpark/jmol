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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.adapter.smarter;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import org.jmol.api.JmolAdapter;

/**
 * A CML2 Reader, it does not support the old CML1 architecture.
 */
class CmlReader extends AtomSetCollectionReader {

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
    throws Exception {
    atomSetCollection = new AtomSetCollection("cml");

    XMLReader xmlr = null;
    // JAXP is preferred (comes with Sun JVM 1.4.0 and higher)
    if (xmlr == null &&
        System.getProperty("java.version").compareTo("1.4") >= 0)
      xmlr = allocateXmlReader14();
    // Aelfred is the first alternative.
    if (xmlr == null)
      xmlr = allocateXmlReaderAelfred2();
    if (xmlr == null) {
      System.out.println("No XML reader found");
      atomSetCollection.errorMessage = "No XML reader found";
      return atomSetCollection;
    }
    //    System.out.println("opening InputSource");
    InputSource is = new InputSource(reader);
    is.setSystemId("foo");
    //    System.out.println("creating CmlHandler");
    CmlHandler cmlh = new CmlHandler();
    
    //    System.out.println("setting features");
    xmlr.setFeature("http://xml.org/sax/features/validation", false);
    xmlr.setFeature("http://xml.org/sax/features/namespaces", true);
    xmlr.setEntityResolver(cmlh);
    xmlr.setContentHandler(cmlh);
    xmlr.setErrorHandler(cmlh);
    
    xmlr.parse(is);
    
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }

  XMLReader allocateXmlReader14() {
    XMLReader xmlr = null;
    try {
      javax.xml.parsers.SAXParserFactory spf =
        javax.xml.parsers.SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      javax.xml.parsers.SAXParser saxParser = spf.newSAXParser();
      xmlr = saxParser.getXMLReader();
      System.out.println("Using JAXP/SAX XML parser.");
    } catch (Exception e) {
      System.out.println("Could not instantiate JAXP/SAX XML reader: " +
                         e.getMessage());
    }
    return xmlr;
  }
  
  XMLReader allocateXmlReaderAelfred2() {
    XMLReader xmlr = null;
    try {
      xmlr = (XMLReader)this.getClass().getClassLoader().
        loadClass("gnu.xml.aelfred2.XmlReader").newInstance();
      System.out.println("Using Aelfred2 XML parser.");
    } catch (Exception e) {
      System.out.println("Could not instantiate Aelfred2 XML reader!");
    }
    return xmlr;
  }

  class CmlHandler extends DefaultHandler implements ErrorHandler {
    
    ////////////////////////////////////////////////////////////////

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
      System.out.println("reading bond: " + str);
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
      float floatOrder = parseFloat(str);
      if (Float.isNaN(floatOrder)) {
          if (str.equals("partial01")) {
              return JmolAdapter.ORDER_PARTIAL01;
          } else if (str.equals("partial12")) {
              return JmolAdapter.ORDER_PARTIAL12;
          } else if (str.equals("hbond")) {
              return JmolAdapter.ORDER_HBOND;
          }
      }
      if (floatOrder == 1.5)
        return JmolAdapter.ORDER_PARTIAL12;
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


    public void startDocument() {
      //      System.out.println("model: " + model);
    }

    int moleculeNesting = 0;

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts) {
      /*
        System.out.println("startElement(" + namespaceURI + "," + localName +
        "," + qName + "," + atts +  ")");
      /* */
      if ("molecule".equals(localName)) {
        if (++moleculeNesting > 1)
          return;
        atomSetCollection.newAtomSet();
        String collectionName = null;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("title".equals(attLocalName)) {
            collectionName = attValue;
          } else if ("id".equals(attLocalName)) {
            if (collectionName == null) {
              collectionName = attValue;
            } // else: don't overwrite title!
          }
        }
        if (collectionName != null) {
          atomSetCollection.setAtomSetName(collectionName);
        }
        return;
      }
      if ("atom".equals(localName)) {
        elementContext = ATOM;
        atom = new Atom();
        boolean coords3D = false;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("id".equals(attLocalName)) {
            atom.atomName = attValue;
          } else if ("x3".equals(attLocalName)) {
            coords3D = true;
            atom.x = parseFloat(attValue);
          } else if ("y3".equals(attLocalName)) {
            atom.y = parseFloat(attValue);
          } else if ("z3".equals(attLocalName)) {
            atom.z = parseFloat(attValue);
          } else if ("x2".equals(attLocalName)) {
            if (Float.isNaN(atom.x))
              atom.x = parseFloat(attValue);
          } else if ("y2".equals(attLocalName)) {
            if (Float.isNaN(atom.y))
              atom.y = parseFloat(attValue);
          } else if ("elementType".equals(attLocalName)) {
            atom.elementSymbol = attValue;
          } else if ("formalCharge".equals(attLocalName)) {
            atom.formalCharge = parseInt(attValue);
          }
        }
        if (! coords3D)
          atom.z = 0;
        return;
      }
      if ("atomArray".equals(localName)) {
        atomCount = 0;
        boolean coords3D = false;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("atomID".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].atomName = tokens[j];
          } else if ("x3".equals(attLocalName)) {
            coords3D = true;
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].x = parseFloat(tokens[j]);
          } else if ("y3".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].y = parseFloat(tokens[j]);
          } else if ("z3".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].z = parseFloat(tokens[j]);
          } else if ("x2".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].z = parseFloat(tokens[j]);
          } else if ("y2".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].z = parseFloat(tokens[j]);
          } else if ("elementType".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].elementSymbol = tokens[j];
          }
        }
        for (int j = atomCount; --j >= 0; ) {
          Atom atom = atomArray[j];
          if (! coords3D)
            atom.z = 0;
        }
        return;
      }
      if ("bond".equals(localName)) {
        //  <bond atomRefs2="a20 a21" id="b41" order="2"/>
        int order = -1;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("atomRefs2".equals(attLocalName)) {
            breakOutTokens(attValue);
          } else if ("order".equals(attLocalName)) {
            order = parseBondToken(attValue);
            System.out.println("Bond order read: " + order);
          }
        }
        /*
        System.out.println("trying to add a new bond tokenCount:" +
                           tokenCount + " order:" + order);
        */
        if (tokenCount == 2 && order > 0)
          atomSetCollection.addNewBond(tokens[0], tokens[1], order);
        return;
      }
      if ("bondArray".equals(localName)) {
        bondCount = 0;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("order".equals(attLocalName)) {
            breakOutBondTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              bondArray[j].order = parseBondToken(tokens[j]);
          } else if ("atomRef1".equals(attLocalName)) {
            breakOutBondTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              bondArray[j].atomIndex1 = atomSetCollection.getAtomNameIndex(tokens[j]);
          } else if ("atomRef2".equals(attLocalName)) {
            breakOutBondTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              bondArray[j].atomIndex2 = atomSetCollection.getAtomNameIndex(tokens[j]);
          }
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
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("title".equals(attLocalName)) {
            title = attValue;
          } else if ("dictRef".equals(attLocalName)) {
            dictRef = attValue;
          }
        }
        keepChars = true;
        return;
      }
    }
    
    public void endElement(String uri, String localName, String qName)  {
      /*
        System.out.println("endElement(" + uri + "," + localName +
        "," + qName + ")");
      /* */
      processEndElement(uri, localName, qName);
      keepChars = false;
      title = null;
      dictRef = null;
      chars = null;
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
          /*
          System.out.println(" I just added an atom of type "
                             + atom.elementSymbol +
                             " @ " + atom.x + "," + atom.y + "," + atom.z);
          */
        }
        atom = null;
        elementContext = UNSET;
        return;
      }
      if ("crystal".equals(localName)) {
        elementContext = UNSET;
        for (int i = 6; --i >= 0; )
          if (Float.isNaN(notionalUnitcell[i])) {
            System.out.println("incomplete/unrecognized unitcell");
            return;
          }
        atomSetCollection.notionalUnitcell = notionalUnitcell;
        return;
      }
      if ("scalar".equals(localName)) {
        if (elementContext == CRYSTAL) {
          //          System.out.println("CRYSTAL atts.title: " + title);
          if (title != null) {
              int i = 6;
              while (--i >= 0 && !
                     title.equals(AtomSetCollection.notionalUnitcellTags[i]))
                { }
              if (i >= 0)
                notionalUnitcell[i] = parseFloat(chars);
          }
          //          System.out.println("CRYSTAL atts.dictRef: " + dictRef);
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
            //System.out.println("jmol.partialCharge=" + atom.partialCharge);
          }
        }
        return;
      }
      if ("atomArray".equals(localName)) {
        //        System.out.println("adding atomArray:" + atomCount);
        for (int i = 0; i < atomCount; ++i) {
          Atom atom = atomArray[i];
          if (atom.elementSymbol != null &&
              ! Float.isNaN(atom.z))
            atomSetCollection.addAtomWithMappedName(atom);
        }
        return;
      }
      if ("bondArray".equals(localName)) {
        //        System.out.println("adding bondArray:" + bondCount);
        for (int i = 0; i < bondCount; ++i)
          atomSetCollection.addBond(bondArray[i]);
        return;
      }
    }
    
    public void characters(char[] ch, int start, int length) {
      //System.out.println("End chars: " + new String(ch, start, length));
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
      System.out.println("Not resolving this:");
      System.out.println("      name: " + name);
      System.out.println("  systemID: " + systemId);
      System.out.println("  publicID: " + publicId);
      System.out.println("   baseURI: " + baseURI);
      return null;
    }
    
    public InputSource resolveEntity (String publicId, String systemId) {
      System.out.println("Not resolving this:");
      System.out.println("  publicID: " + publicId);
      System.out.println("  systemID: " + systemId);
      return null;
    }
    
    public void error (SAXParseException exception)  {
      System.out.println("SAX ERROR:" + exception.getMessage());
    }
    
    public void fatalError (SAXParseException exception)  {
      System.out.println("SAX FATAL:" + exception.getMessage());
    }
    
    public void warning (SAXParseException exception)  {
      System.out.println("SAX WARNING:" + exception.getMessage());
    }
  }
}
