/*
 * Copyright 2001 The Jmol Development Team
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
import org.openscience.cdk.io.cml.cdopi.*;
import org.openscience.cdk.io.cml.*;
import org.xml.sax.helpers.*;
import org.xml.sax.*;

/**
 * CML files contain a single ChemFrame object.
 * @see ChemFrame
 */
public class CMLReader implements ChemFileReader {

  private URL url;

  /**
   * Creates a CML reader.
   *
   * @param input source of CML data
   */
  public CMLReader(Reader input) {
    this.input = input;
  }

  /**
   * Creates a CML reader.
   *
   * @param input source of CML data
   */
  public CMLReader(URL url) {
    this.url = url;
  }

  /**
   * Whether bonds are enabled in the files and frames read.
   */
  private boolean bondsEnabled = true;

  /**
   * Sets whether bonds are enabled in the files and frames which are read.
   *
   * @param bondsEnabled if true, enables bonds.
   */
  public void setBondsEnabled(boolean bondsEnabled) {
    this.bondsEnabled = bondsEnabled;
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
      ((CMLHandler) handler).registerConvention("JMOL-ANIMATION",
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
      ChemFile file = new ChemFile(bondsEnabled);
      Enumeration framesIter =
        ((JMolCDO) handler.returnCDO()).returnChemFrames().elements();
      while (framesIter.hasMoreElements()) {
        file.addFrame((ChemFrame) framesIter.nextElement());
        fireFrameRead();
      }
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
      ((CMLHandler) handler).registerConvention("JMOL-ANIMATION",
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
      ChemFile file = new ChemFile(bondsEnabled);
      Enumeration framesIter =
        ((JMolCDO) handler.returnCDO()).returnChemFrames().elements();
      while (framesIter.hasMoreElements()) {
        file.addFrame((ChemFrame) framesIter.nextElement());
        fireFrameRead();
      }
      return file;
    } catch (SAXException ex) {
      throw new IOException("CMLReader exception: " + ex);
    }
  }

  /**
   * Holder of reader event listeners.
   */
  private Vector listenerList = new Vector();

  /**
   * An event to be sent to listeners. Lazily initialized.
   */
  private ReaderEvent readerEvent = null;

  /**
   * Adds a reader listener.
   *
   * @param l the reader listener to add.
   */
  public void addReaderListener(ReaderListener l) {
    listenerList.addElement(l);
  }
  
  /**
   * Removes a reader listener.
   *
   * @param l the reader listener to remove.
   */
  public void removeReaderListener(ReaderListener l) {
    listenerList.removeElement(l);
  }
  
  /**
   * Sends a frame read event to the reader listeners.
   */
  private void fireFrameRead() {
    for (int i = 0; i < listenerList.size(); ++i) {
      ReaderListener listener = (ReaderListener) listenerList.elementAt(i);
      // Lazily create the event:
      if (readerEvent == null) {
        readerEvent = new ReaderEvent(this);
      }
      listener.frameRead(readerEvent);
    }
  }

  /**
   * The source for CML data.
   */
  private Reader input;
}
