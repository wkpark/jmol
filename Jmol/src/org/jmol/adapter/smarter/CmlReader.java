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
    System.out.println("opening InputSource");
    InputSource is = new InputSource(reader);
    is.setSystemId("foo");
    System.out.println("creating CmlHandler");
    CmlHandler cmlh = new CmlHandler();

    System.out.println("setting features");
    xmlr.setFeature("http://xml.org/sax/features/validation", false);
    xmlr.setFeature("http://xml.org/sax/features/namespaces", false);
    xmlr.setEntityResolver(cmlh);
    xmlr.setContentHandler(cmlh);
    xmlr.setErrorHandler(cmlh);

    xmlr.parse(is);
    
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }

  boolean inAtomContext = false;
  int charactersState;

  final static int DISCARD = 0;
  final static int ELEMENT_TYPE = 1;
  final static int COORDINATE3 = 2;
  final static int ARRAY_ID = 3;
  final static int X3 = 4;
  final static int Y3 = 5;
  final static int Z3 = 6;

  boolean inAtomArrayContext = false;

  int moleculeCount = 0;
  Atom atom;
  Atom[] atomArray;

  String chars;

  class CmlHandler extends DefaultHandler implements ErrorHandler {

    public void startDocument() {
        System.out.println("model: " + model);
    }

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
      throws SAXException
    {
      System.out.println("startElement(" + namespaceURI + "," + localName +
                         "," + qName + "," + atts +  ")");
      chars = null;
      if ("molecule".equals(qName)) {
        ++moleculeCount;
        return;
      }
      if ("atom".equals(qName)) {
        inAtomContext = true;
        atom = new Atom();
        for (int i=0; i<atts.getLength(); i++) {
            if ("id".equals(atts.getLocalName(i))) {
                atom.atomName = atts.getValue(i);
            } else if ("x3".equals(atts.getLocalName(i))) {
                atom.x = parseFloat(atts.getValue(i));
            } else if ("y3".equals(atts.getLocalName(i))) {
                atom.y = parseFloat(atts.getValue(i));
            } else if ("z3".equals(atts.getLocalName(i))) {
                atom.z = parseFloat(atts.getValue(i));
            } else if ("elementType".equals(atts.getLocalName(i))) {
                atom.elementSymbol = atts.getValue(i);
            }
        }
        atom.modelNumber = moleculeCount;
        return;
      }
      if ("atomArray".equals(qName)) {
        inAtomContext = true;
        atomArray = new Atom[0];
        for (int i=0; i<atts.getLength(); i++) {
            if ("atomID".equals(atts.getLocalName(i))) {
                String[] strings = breakOutStrings(atts.getValue(i));
                if (strings.length > atomArray.length)
                    initLargerAromArray(atomArray.length);
                for (int j = 0; j < strings.length; ++j)
                    atomArray[j].atomName = strings[j];
            } else if ("x3".equals(atts.getLocalName(i))) {
                String[] strings = breakOutStrings(atts.getValue(i));
                if (strings.length > atomArray.length)
                    initLargerAromArray(atomArray.length);
                for (int j = 0; j < strings.length; ++j)
                    atomArray[j].x = parseFloat(strings[j]);
            } else if ("y3".equals(atts.getLocalName(i))) {
                String[] strings = breakOutStrings(atts.getValue(i));
                if (strings.length > atomArray.length)
                    initLargerAromArray(atomArray.length);
                for (int j = 0; j < strings.length; ++j)
                    atomArray[j].y = parseFloat(strings[j]);
            } else if ("z3".equals(atts.getLocalName(i))) {
                String[] strings = breakOutStrings(atts.getValue(i));
                if (strings.length > atomArray.length)
                    initLargerAromArray(atomArray.length);
                for (int j = 0; j < strings.length; ++j)
                    atomArray[j].z = parseFloat(strings[j]);
            } else if ("elementType".equals(atts.getLocalName(i))) {
                String[] strings = breakOutStrings(atts.getValue(i));
                if (strings.length > atomArray.length)
                    initLargerAromArray(atomArray.length);
                for (int j = 0; j < strings.length; ++j)
                    atomArray[j].elementSymbol = strings[j];
            }
        }
        atom.modelNumber = moleculeCount;
        return;
      }
    }

    public void endElement(String uri, String localName,
                           String qName) throws SAXException {
      System.out.println("endElement(" + uri + "," + localName +
                         "," + qName + ")");
      try {
        if ("atom".equals(qName)) {
          inAtomContext = false;
          if (atom.elementSymbol != null &&
              ! Float.isNaN(atom.z)) {
            model.addAtom(atom);
          }
          atom = null;
          return;
        }
        if ("atomArray".equals(qName) &&
          atomArray != null) {
          for (int i = 0; i < atomArray.length; ++i)
            model.addAtom(atomArray[i]);
          return;
        }
      } finally {
        charactersState = DISCARD;
        chars = null;
      }
    }

    public void characters(char[] ch, int start, int length) {
      if (charactersState == DISCARD)
        return;
      String str = new String(ch, start, length);
      if (chars == null)
        chars = str;
      else
        chars += str;
    }
    
    // Methods for entity resolving, e.g. getting an DTD resolved
    
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

    String[] breakOutStrings(String chars) {
      StringTokenizer st = new StringTokenizer(chars);
      int stringCount = st.countTokens();
      String[] strings = new String[stringCount];
      for (int i = 0; i < stringCount; ++i) {
        try {
          strings[i] = st.nextToken();
        } catch (NoSuchElementException nsee) {
          strings[i] = null;
        }
      }
      return strings;
    }
    
    void initLargerAromArray(int atomArrayLength) {
      int currentLength = atomArray.length;
      Atom[] newatoms = new Atom[atomArrayLength];
      System.arraycopy(atomArray, 0, newatoms, 0, currentLength);
      atomArray = newatoms;
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

