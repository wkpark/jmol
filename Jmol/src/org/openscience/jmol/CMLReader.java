
/*
 * Copyright 2002 The Jmol Development Team
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
package org.openscience.jmol;

import java.io.Reader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.event.EventListenerList;
import java.net.URL;
import org.openscience.cdk.io.cml.CMLHandler;
import org.openscience.cdk.io.cml.CMLErrorHandler;
import org.openscience.cdk.io.cml.JMOLANIMATIONConvention;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

/**
 * CML files contain a single ChemFrame object.
 * @see ChemFrame
 */
public class CMLReader extends DefaultChemFileReader {

  private URL url;

  /**
   * Creates a CML reader.
   *
   * @param input source of CML data
   */
  public CMLReader(Reader input) {
    super(input);
  }

  /**
   * Creates a CML reader.
   *
   * @param input source of CML data
   */
  public CMLReader(URL url) {
    super(null);
    this.url = url;
  }

  /**
   * Read the CML data with a validating parser.
   *
   * <p>Requires full gnujaxp.jar library. Not meant for
   * use with applet.
   */
  public ChemFile readValidated() throws IOException {

    try {
      XMLReader parser = new gnu.xml.aelfred2.XmlReader();
      try {
        parser.setFeature("http://xml.org/sax/features/validation", true);
        System.out.println("Activated validation");
      } catch (SAXException e) {
        System.out.println("Cannot activate validation.");
      }
      JMolCDO cdo = new JMolCDO();
      CMLHandler handler = new CMLHandler(cdo);
      handler.registerConvention("JMOL-ANIMATION",
          new JMOLANIMATIONConvention(cdo));
      parser.setContentHandler(handler);
      parser.setEntityResolver(new CMLResolver());
      parser.setErrorHandler(new CMLErrorHandler());
      if (this.input != null) {
        InputSource source = new InputSource(input);
        parser.parse(source);
      } else {
        parser.parse(url.toString());
      }

      ChemFile file = ((JMolCDO) handler.returnCDO()).returnChemFile();
      
      return file;
    } catch (SAXException ex) {
      throw new IOException("CMLReader exception: " + ex);
    }
  }

  /**
   * Read the CML data with a non-validating parser.
   *
   * <p>Written for use in applet, but can be used in
   * application too.
   */
  public ChemFile read() throws IOException {

    try {
      XMLReader parser = new gnu.xml.aelfred2.SAXDriver();
      JMolCDO cdo = new JMolCDO();
      CMLHandler handler = new CMLHandler(cdo);
      handler.registerConvention("JMOL-ANIMATION",
          new JMOLANIMATIONConvention(cdo));
      parser.setContentHandler(handler);
      parser.setEntityResolver(new CMLResolver());
      InputSource source;
      if (this.input != null) {
        source = new InputSource(input);
      } else {
        source = new InputSource(url.toString());
      }
      parser.parse(source);

      ChemFile file = ((JMolCDO) handler.returnCDO()).returnChemFile();

      return file;
    } catch (SAXException ex) {
      throw new IOException("CMLReader exception: " + ex);
    }
  }
}
