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

    System.out.println("here we go!");
    xmlr.parse(is);
    
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }

  class CmlHandler extends DefaultHandler {

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
      throws SAXException
    {
      System.out.println("startElement(" + namespaceURI + "," + localName +
                         "," + qName + "," + atts +  ")");
    }

    public void endElement(String uri, String localName,
                           String qName) throws SAXException {
      System.out.println("endElement(" + uri + "," + localName +
                         "," + qName + ")");
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
  }
}

