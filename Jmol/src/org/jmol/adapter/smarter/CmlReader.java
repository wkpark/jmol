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
  int atomArrayCount;
  Atom[] atomArray;
  int stringCount;
  String[] strings;
  String chars;

  class CmlHandler extends DefaultHandler {

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
      throws SAXException
    {
      /*
      System.out.println("startElement(" + namespaceURI + "," + localName +
                         "," + qName + "," + atts +  ")");
      */
      chars = null;
      if ("molecule".equals(qName)) {
        ++moleculeCount;
        return;
      }
      if ("atom".equals(qName)) {
        inAtomContext = true;
        atom = new Atom();
        atom.atomName = atts.getValue("id");
        atom.modelNumber = moleculeCount;
        return;
      }
      if (inAtomContext) {
        if ("string".equals(qName) &&
            "elementType".equals(atts.getValue("builtin"))) {
          charactersState = ELEMENT_TYPE;
          return;
        }
        if ("coordinate3".equals(qName) &&
            "xyz3".equals(atts.getValue("builtin"))) {
          charactersState = COORDINATE3;
          return;
        }
        return;
      }
      if ("atomArray".equals(qName)) {
        inAtomArrayContext = true;
        atomArrayCount = -1;
        return;
      }
      if (inAtomArrayContext) {
        String builtin = atts.getValue("builtin");
        if ("stringArray".equals(qName)) {
          if ("id".equals(builtin)) {
            charactersState = ARRAY_ID;
            return;
          }
          if ("elementType".equals(builtin)) {
            charactersState = ELEMENT_TYPE;
            return;
          }
          return;
        }
        if ("floatArray".equals(qName)) {
          if ("x3".equals(builtin)) {
            charactersState = X3;
            return;
          }
          if ("y3".equals(builtin)) {
            charactersState = Y3;
            return;
          }
          if ("z3".equals(builtin)) {
            charactersState = Z3;
            return;
          }
          return;
        }
        return;
      }
    }

    public void endElement(String uri, String localName,
                           String qName) throws SAXException {
      /*
      System.out.println("endElement(" + uri + "," + localName +
                         "," + qName + ")");
      */
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
        if (inAtomContext) {
          if (charactersState == ELEMENT_TYPE &&
              "string".equals(qName)) {
            atom.elementSymbol = chars;
            return;
          }
          if (charactersState == COORDINATE3 &&
              "coordinate3".equals(qName) &&
              chars != null) {
            atom.x = parseFloat(chars);
            atom.y = parseFloat(chars, ichNextParse);
            atom.z = parseFloat(chars, ichNextParse);
            return;
          }
          return;
        }
        if ("atomArray".equals(qName) &&
            atomArray != null) {
          for (int i = 0; i < atomArrayCount; ++i)
            model.addAtom(atomArray[i]);
          atomArrayCount = -1;
          return;
        }
        if (inAtomArrayContext) {
          breakOutStrings();
          if (charactersState == ARRAY_ID) {
            for (int i = 0; i < stringCount; ++i)
              atomArray[i].atomName = strings[i];
            return;
          }
          if (charactersState == ELEMENT_TYPE) {
            for (int i = 0; i < stringCount; ++i)
              atomArray[i].elementSymbol = strings[i];
            return;
          }
          if (charactersState == X3) {
            for (int i = 0; i < stringCount; ++i)
              atomArray[i].x = parseFloat(strings[i]);
            return;
          }
          if (charactersState == Y3) {
            for (int i = 0; i < stringCount; ++i)
              atomArray[i].y = parseFloat(strings[i]);
            return;
          }
          if (charactersState == Z3) {
            for (int i = 0; i < stringCount; ++i)
              atomArray[i].z = parseFloat(strings[i]);
            return;
          }
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

    void breakOutStrings() {
      StringTokenizer st = new StringTokenizer(chars);
      stringCount = st.countTokens();
      if (strings == null || stringCount > strings.length)
        strings = new String[stringCount];
      if (atomArrayCount < 0) {
        atomArrayCount = stringCount;
        if (atomArray == null || atomArrayCount > atomArray.length) {
          atomArray = new Atom[atomArrayCount];
          for (int i = atomArrayCount; --i >= 0; )
            atomArray[i] = new Atom();
        }
      } else if (atomArrayCount != stringCount)
        throw new IndexOutOfBoundsException("bad cml file");

      for (int i = 0; i < stringCount; ++i) {
        try {
          strings[i] = st.nextToken();
        } catch (NoSuchElementException nsee) {
          strings[i] = null;
        }
      }
    }
  }
}

