/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

/**
 * A CML2 Reader, it does not support the old CML1 architecture.
 */
class CmlReader extends ModelReader {
  
  SAXParser saxp;

  Model readModel(BufferedReader reader) throws Exception {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    SAXParser saxp = spf.newSAXParser();
    model = new Model("cml");

    XMLReader xmlr = saxp.getXMLReader();
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
    
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }

  class CmlHandler extends DefaultHandler implements ErrorHandler {
    
    ////////////////////////////////////////////////////////////////

    int moleculeCount = 0;
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

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
      throws SAXException
    {
      /*
        System.out.println("startElement(" + namespaceURI + "," + localName +
        "," + qName + "," + atts +  ")");
      */
      if ("molecule".equals(qName)) {
        ++moleculeCount;
        return;
      }
      if ("atom".equals(qName)) {
        atom = new Atom();
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("id".equals(attLocalName)) {
            atom.atomName = attValue;
          } else if ("x3".equals(attLocalName)) {
            atom.x = parseFloat(attValue);
          } else if ("y3".equals(attLocalName)) {
            atom.y = parseFloat(attValue);
          } else if ("z3".equals(attLocalName)) {
            atom.z = parseFloat(attValue);
          } else if ("elementType".equals(attLocalName)) {
            atom.elementSymbol = attValue;
          } else if ("formalCharge".equals(attLocalName)) {
            atom.formalCharge = parseInt(attValue);
          }
        }
        atom.modelNumber = moleculeCount;
        return;
      }
      if ("atomArray".equals(qName)) {
        atomCount = 0;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("atomID".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].atomName = tokens[j];
          } else if ("x3".equals(attLocalName)) {
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
          } else if ("elementType".equals(attLocalName)) {
            breakOutAtomTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              atomArray[j].elementSymbol = tokens[j];
          }
        }
        for (int j = atomCount; --j >= 0; )
          atomArray[j].modelNumber = moleculeCount;
        return;
      }
      if ("bond".equals(qName)) {
        //  <bond atomRefs2="a20 a21" id="b41" order="2"/>
        int order = -1;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("atomRefs2".equals(attLocalName)) {
            breakOutTokens(attValue);
          } else if ("order".equals(attLocalName)) {
            order = parseInt(attValue);
          }
        }
        if (tokenCount == 2 && order > 0)
          model.newBond(tokens[0], tokens[1], order);
        return;
      }
      if ("bondArray".equals(qName)) {
        bondCount = 0;
        for (int i = atts.getLength(); --i >= 0; ) {
          String attLocalName = atts.getLocalName(i);
          String attValue = atts.getValue(i);
          if ("order".equals(attLocalName)) {
            breakOutBondTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              bondArray[j].order = parseInt(tokens[j]);
          } else if ("atomRef1".equals(attLocalName)) {
            breakOutBondTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              bondArray[j].atomIndex1 = model.getAtomNameIndex(tokens[j]);
          } else if ("atomRef2".equals(attLocalName)) {
            breakOutBondTokens(attValue);
            for (int j = tokenCount; --j >= 0; )
              bondArray[j].atomIndex2 = model.getAtomNameIndex(tokens[j]);
          }
        }
        return;
      }
      if ("crystal".equals(qName)) {
        elementContext = CRYSTAL;
        notionalUnitcell = new float[6];
        return;
      }
      if ("scalar".equals(qName)) {
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
    
    public void endElement(String uri, String localName,
                           String qName) throws SAXException {
      /*
        System.out.println("endElement(" + uri + "," + localName +
        "," + qName + ")");
      */
      if ("atom".equals(qName)) {
        if (atom.elementSymbol != null &&
            ! Float.isNaN(atom.z)) {
          model.addAtomWithMappedName(atom);
        }
        atom = null;
        elementContext = UNSET;
        return;
      }
      if ("crystal".equals(qName)) {
        elementContext = UNSET;
        model.notionalUnitcell = notionalUnitcell;
        return;
      }
      if ("scalar".equals(qName)) {
        if (elementContext == CRYSTAL) {
          System.out.println("CRYSTAL atts.title: " + title);
          int i = 6;
          while (--i >= 0 && ! title.equals(Model.notionalUnitcellTags[i]))
            ;
          if (i >= 0)
            notionalUnitcell[i] = parseFloat(chars);
        } else if (elementContext == ATOM) {
          if ("jmol:charge".equals(dictRef)) {
            atom.partialCharge = parseFloat(chars);
          }
        }
        keepChars = false;
        title = null;
        dictRef = null;
        chars = null;
        return;
      }
      if ("atomArray".equals(qName)) {
        for (int i = 0; i < atomCount; ++i) {
          Atom atom = atomArray[i];
          if (atom.elementSymbol != null &&
              ! Float.isNaN(atom.z))
            model.addAtomWithMappedName(atom);
        }
        return;
      }
      if ("bondArray".equals(qName)) {
        System.out.println("adding bondArray:" + bondCount);
        for (int i = 0; i < bondCount; ++i)
          model.addBond(bondArray[i]);
        return;
      }
    }
    
    public void characters(char[] ch, int start, int length) {
      // System.out.println("End chars");
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
    
    public void error (SAXParseException exception) throws SAXException {
      System.out.println("SAX ERROR:" + exception.getMessage());
    }
    
    public void fatalError (SAXParseException exception) throws SAXException {
      System.out.println("SAX FATAL:" + exception.getMessage());
    }
    
    public void warning (SAXParseException exception) throws SAXException {
      System.out.println("SAX WARNING:" + exception.getMessage());
    }
  }
}
