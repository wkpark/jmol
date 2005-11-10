/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 * Copyright (C) 2005  Peter Knowles
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

import org.xml.sax.Attributes;

/**
 * A Molpro 2005 reader
 */
class MolproReader extends CmlReader {

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
    throws Exception {
      return readAtomSetCollectionSax(reader, (new MolproHandler()),"molpro");
  }

  class MolproHandler extends CmlHandler {

      // tag names in http://www.molpro.net/schema/molpro2005
    private String normalCoordinateTag = "normalCoordinate";
    private String vibrationsTag = "vibrations";
    
    public void startElement(String namespaceURI, String localName,
			     String qName, Attributes atts) {
      //logger.log("startElement(" + namespaceURI + "," + localName +
      //"," + qName + "," + atts +  ")");
      // the CML stuff

      HashMap hashAtts = new HashMap(atts.getLength());
      for (int i = atts.getLength(); --i >= 0; )
        hashAtts.put(atts.getLocalName(i), atts.getValue(i));

      processStartElement(namespaceURI, localName, qName, hashAtts);
      // the extra Molpro stuff
      molproStartElement(namespaceURI, localName, qName, atts);
    }

    int frequencyCount;

    public void molproStartElement(String namespaceURI, String localName,
				   String qName, Attributes atts) {
      //logger.log("molproStartElement(" + namespaceURI + "," + localName +
      //"," + qName + "," + atts +  ")");
      if (normalCoordinateTag.equals(localName)) {
        //int atomCount = atomSetCollection.getLastAtomSetAtomCount();
        String wavenumber = "";
        //String units = "";
        //String tokens[];
        atomSetCollection.cloneLastAtomSet();
        frequencyCount++;
        for (int i = atts.getLength(); --i >= 0; ) {
	        String attLocalName = atts.getLocalName(i);
	        String attValue = atts.getValue(i);
	        if ("wavenumber".equals(attLocalName)) {
	          wavenumber = attValue;
          } else if ("units".equals(attLocalName)) {
            //units = attValue;
          }
        }
        atomSetCollection.setAtomSetProperty("Frequency", wavenumber+" cm**-1");
        //logger.log("new normal mode " + wavenumber + " " + units);
        keepChars = true;
         return;
      }

      if (vibrationsTag.equals(localName)) {
        frequencyCount = 0;
        return;
      }
    }

    public void endElement(String uri, String localName, String qName)  {
      /*
        System.out.println("endElement(" + uri + "," + localName +
        "," + qName + ")");
      /* */
      processEndElement(uri, localName, qName);
      molproProcessEndElement(uri, localName, qName);
      keepChars = false;
      title = null;
      dictRef = null;
      chars = null;
    }

    public void molproProcessEndElement(String uri, String localName, String qName)  {
      if (normalCoordinateTag.equals(localName)) {
	int atomCount = atomSetCollection.getLastAtomSetAtomCount();
	tokens = getTokens(chars);
	for (int offset=tokens.length-atomCount*3, i=0; i<atomCount; i++) {
          Atom atom = atomSetCollection.atoms[i+atomSetCollection.currentAtomSetIndex*atomCount];
	  atom.vectorX = parseFloat(tokens[offset++]);
	  atom.vectorY = parseFloat(tokens[offset++]);
	  atom.vectorZ = parseFloat(tokens[offset++]);
	}
      }
    }
  }
}
