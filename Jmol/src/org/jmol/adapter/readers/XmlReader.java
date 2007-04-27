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
package org.jmol.adapter.readers;

import org.jmol.adapter.smarter.*;


import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.HashMap;

import netscape.javascript.JSObject;
import org.jmol.util.Logger;

/**
 * A generic XML reader template -- by itself, does nothing.
 * 
 * The actual readers are XmlCmlReader, XmlMolproReader (which is an
 * extension of XmlCmlReader, XmlChem3dReader, and XmlOdysseyReader, which is wholely different.
 * 
 * 
 * XmlReader takes all XML streams, whether from a file reader or from DOM.
 *  
 * This class functions as a resolver, since it:
 *  (1) identifying the specific strain of XML to be handled, and 
 *  (2) passing the responsibility on to the correct format-specific XML readers. 
 * There are parallel entry points and handler methods for reader and DOM. Each 
 * format-specific XML reader then assigns its own handler to manage the 
 * parsing of elements.
 * 
 * In addition, this class handles generic XML tag parsing.
 * 
 * XmlReader.JmolXmlHandler extends DefaultHandler is the generic interface to both reader and DOM element parsing.
 * 
 * XmlCmlReader extends XmlReader and is where I'd like Andrew to take charge.
 * XmlCmlReader.CmlHandler extends XmlReader.JmolXmlHandler is generic
 * 
 * XmlMolproReader extends XmlCmlReader. If you feel like expanding on that, feel free.
 * XmlMolproReader.MolprolHandler extends XmlCmlReader.CmlHandler adds Molpro-specific XML tag processing
 * 
 * XmlChem3dReader extends XmlReader. That one is simple; no need to expand on it at this time.
 * XmlChem3dReader.Chem3dHandler extends XmlReader.JmolXmlHandler is generic
 * 
 * XmlOdysseyReader extends XmlReader. That one is simple; no need to expand on it at this time.
 * XmlOdysseyReader.OdysseyHandler extends XmlReader.JmolXmlHandler is generic
 * 
 * Note that the tag processing routines are shared between SAX 
 * and DOM processors. This means that attributes must be
 * transformed from either Attributes (SAX) or JSObjects (DOM)
 * to HashMap name:value pairs. This is taken care of in JmolXmlHandler
 * for all readers. 
 * 
 * TODO 27/8/06:
 * 
 * Several aspects of CifReader are NOT YET implemented here. 
 * These include loading a specific model when there are several,
 * applying the symmetry, and loading fractional coordinates. [DONE for CML reader 2/2007 RMH]
 * 
 * The DOM reader is NOT CHECKED OVER, and I do not think that it supports
 * reading characters between start/end tags:
 * 
 *  <tag>characters</tag>
 *  
 *  If you work on this, please read formats other than CML into DOM so that
 *  we can see that that works as well.
 *  
 *  Test files:
 *  
 *  molpro:  vib.xml
 *  odyssey: water.xodydata
 *  cml: a wide variety of files in data-files. Feel free to prune if some are
 *  not of use.
 * 
 * -Bob Hanson
 * 
 */

public class XmlReader extends AtomSetCollectionReader {

  //XmlReader subReader; // the actual reader; to be determined
  protected XmlReader parent;    // XmlReader itself; to be assigned by the subReader

  protected Atom atom;

  String[] implementedAttributes = { "id" };

  static final String CML_NAMESPACE_URI = "http://www.xml-cml.org/schema";

  /////////////// file reader option //////////////

 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    XMLReader xmlReader = getXmlReader();
    if (xmlReader == null) {
      atomSetCollection = new AtomSetCollection("xml");
      atomSetCollection.errorMessage = "No XML reader found";
      return atomSetCollection;
    }
    try {
      processXml(xmlReader);
    } catch (Exception e) {
      e.printStackTrace();
      atomSetCollection.errorMessage = "XML reader error: " + e.getMessage();
    }
    return atomSetCollection;
  }

  private XMLReader getXmlReader() {
    XMLReader xmlr = null;
    // JAXP is preferred (comes with Sun JVM 1.4.0 and higher)
    if (xmlr == null
        && System.getProperty("java.version").compareTo("1.4") >= 0)
      xmlr = allocateXmlReader14();
    // Aelfred is the first alternative.
    if (xmlr == null)
      xmlr = allocateXmlReaderAelfred2();
    return xmlr;
  }

  private XMLReader allocateXmlReader14() {
    XMLReader xmlr = null;
    try {
      javax.xml.parsers.SAXParserFactory spf = javax.xml.parsers.SAXParserFactory
          .newInstance();
      spf.setNamespaceAware(true);
      javax.xml.parsers.SAXParser saxParser = spf.newSAXParser();
      xmlr = saxParser.getXMLReader();
      Logger.debug("Using JAXP/SAX XML parser.");
    } catch (Exception e) {
      Logger.debug("Could not instantiate JAXP/SAX XML reader: "
          + e.getMessage());
    }
    return xmlr;
  }

  private XMLReader allocateXmlReaderAelfred2() {
    XMLReader xmlr = null;
    try {
      xmlr = (XMLReader) this.getClass().getClassLoader().loadClass(
          "gnu.xml.aelfred2.XmlReader").newInstance();
      Logger.debug("Using Aelfred2 XML parser.");
    } catch (Exception e) {
      Logger.debug("Could not instantiate Aelfred2 XML reader!");
    }
    return xmlr;
  }

  private void processXml(XMLReader xmlReader) throws Exception {
    String xmlType = getXmlType(reader);
    atomSetCollection = new AtomSetCollection(xmlType);
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("XmlReader thinks " + xmlType);
    }
    if (xmlType.indexOf("cml(xml)") >= 0) {
      new XmlCmlReader(this, atomSetCollection, reader, xmlReader);
      return;
    }
    if (xmlType == "molpro(xml)") {
      new XmlMolproReader(this, atomSetCollection, reader, xmlReader);
      return;
    }
    if (xmlType == "chem3d(xml)") {
      new XmlChem3dReader(this, atomSetCollection, reader, xmlReader);
      return;
    }
    if (xmlType == "odyssey(xml)") {
      new XmlOdysseyReader(this, atomSetCollection, reader, xmlReader);
      return;
    }
    if (xmlType == "arguslab(xml)") {
      new XmlArgusReader(this, atomSetCollection, reader, xmlReader);
      return;
    }
    new JmolXmlHandler(xmlReader);
    parseReaderXML(xmlReader);
    return;
  }

  private String getReaderHeader(int nBytes) throws Exception {
    reader.mark(nBytes);
    char[] buf = new char[nBytes];
    int cchBuf = reader.read(buf);
    reader.reset();
    StringBuffer str = new StringBuffer();
    return str.append(buf, 0, cchBuf).toString();
  }

  private String getXmlType(BufferedReader reader) throws Exception  {
    String header = getReaderHeader(5000);
    if (header.indexOf("http://www.molpro.net/") >= 0) {
      return "molpro(xml)";
    }
    if (header.indexOf("odyssey") >= 0) {
      return "odyssey(xml)";
    }
    if (header.indexOf("C3XML") >= 0) {
      return "chem3d(xml)";
    }
    if (header.indexOf("arguslab") >= 0) {
      return "arguslab(xml)";
    }
    if (header.indexOf(CML_NAMESPACE_URI) >= 0
        || header.indexOf("cml:") >= 0) {
      return "cml(xml)";
    }
    return "unidentified cml(xml)";
  }

  protected void parseReaderXML(XMLReader xmlReader) {
    InputSource is = new InputSource(reader);
    is.setSystemId("foo");
    try {
      xmlReader.parse(is);
    } catch (Exception e) {
      e.printStackTrace();
      atomSetCollection.errorMessage = "XML parsing error: " + e.getMessage();
    }
  }

  /////////////// DOM option //////////////

 public AtomSetCollection readAtomSetCollectionFromDOM(Object Node) {
    JSObject DOMNode = (JSObject) Node;
    processXml(DOMNode);
    return atomSetCollection;
  }

  private void processXml(JSObject DOMNode) {
    if (DOMNode == null)
      throw new RuntimeException("Not a node");
    String xmlType = getXmlType(DOMNode);
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("XmlReader thinks " + xmlType);
    }
    atomSetCollection = new AtomSetCollection(xmlType);
    if (xmlType.indexOf("cml(DOM)") >= 0) {
      new XmlCmlReader(this, atomSetCollection, DOMNode);
      return;
    }
    if (xmlType == "molpro(DOM)") {
      new XmlMolproReader(this, atomSetCollection, DOMNode);
      return;
    }
    if (xmlType == "chem3d(DOM)") {
      new XmlChem3dReader(this, atomSetCollection, DOMNode);
      return;
    }
    if (xmlType == "odyssey(DOM)") {
      new XmlOdysseyReader(this, atomSetCollection, DOMNode);
      return;
    }
    if (xmlType == "arguslab(DOM)") {
      new XmlArgusReader(this, atomSetCollection, DOMNode);
      return;
    }
    //arguslab and odyssey don't have namespaces
    Logger.error("XmlReader.java could not resolve DOM XML type");
    ((JmolXmlHandler) (new JmolXmlHandler())).walkDOMTree(DOMNode);
    return;
  }

  private String getXmlType(JSObject DOMNode) {
    String namespaceURI = (String) DOMNode.getMember("namespaceURI");
    String localName = (String) DOMNode.getMember("localName");
    if (namespaceURI.startsWith("http://www.molpro.net/"))
      return "molpro(DOM)";
    if (((String) DOMNode.getMember("localName")).equals("odyssey_simulation"))
      return "odyssey(DOM)";
    if (((String) DOMNode.getMember("localName")).equals("arguslab"))
      return "arguslab(DOM)";
    if (namespaceURI.startsWith(CML_NAMESPACE_URI)
        || "cml" == localName)
      return "cml(DOM)";
    return "unidentified cml(DOM)";
  }

  protected void processStartElement(String namespaceURI, String localName, String qName,
                           HashMap atts) {
    /* 
     * specific to each xml reader
     */
  }

  /*
   *  keepChars is used to signal 
   *  that characters between end tags should be kept
   *  
   */

  protected boolean keepChars;
  protected String chars;
  protected void setKeepChars(boolean TF) {
    keepChars = TF;
    chars = null;
  }

  protected void processEndElement(String uri, String localName, String qName) {
    /* 
     * specific to each xml reader
     */
  }

  public class JmolXmlHandler extends DefaultHandler implements ErrorHandler {

    public JmolXmlHandler() {
    }

    public JmolXmlHandler(XMLReader xmlReader) {
      setHandler(xmlReader, this);
    }

    public void setHandler(XMLReader xmlReader, JmolXmlHandler handler) {
      try {
        xmlReader.setFeature("http://xml.org/sax/features/validation", false);
        xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
        xmlReader.setEntityResolver(handler);
        xmlReader.setContentHandler(handler);
        xmlReader.setErrorHandler(handler);
      } catch (Exception e) {
        Logger.error("ERROR IN XmlReader.JmolXmlHandler.setHandler", e);
      }

    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    /*
     * see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     * startElement and endElement should be extended in each reader
     */

    public HashMap atts;

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attributes) {
      getAttributes(attributes);
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("start " + localName);
      }
      startElement(namespaceURI, localName, qName);
    }

    private void startElement(String namespaceURI, String localName, String qName) {
      processStartElement(namespaceURI, localName, qName, atts);
    }

    public void endElement(String uri, String localName, String qName) {
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("end " + localName);
      }
      processEndElement(uri, localName, qName);
    }

    // Won't work for DOM? -- equivalent of "innerHTML"

    public void characters(char[] ch, int start, int length) {
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
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug(
            "Not resolving this:" +
            "\n      name: " + name +
            "\n  systemID: " + systemId +
            "\n  publicID: " + publicId +
            "\n   baseURI: " + baseURI
          );
      }
      return null;
    }

    public InputSource resolveEntity(String publicId, String systemId) {
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug(
            "Not resolving this:" +
            "\n  publicID: " + publicId +
            "\n  systemID: " + systemId
          );
      }
      return null;
    }

    public void error(SAXParseException exception) {
      Logger.error("SAX ERROR:" + exception.getMessage());
    }

    public void fatalError(SAXParseException exception) {
      Logger.error("SAX FATAL:" + exception.getMessage());
    }

    public void warning(SAXParseException exception) {
      Logger.warn("SAX WARNING:" + exception.getMessage());
    }

    ////////////////////////////////////////////////////////////////

    // walk DOM tree given by JSObject. For every element, call
    // startElement with the appropriate strings etc., and then
    // endElement when the element is closed.

    protected void walkDOMTree(JSObject DOMNode) {
      String namespaceURI = (String) DOMNode.getMember("namespaceURI");
      String localName = (String) DOMNode.getMember("localName");
      String qName = (String) DOMNode.getMember("nodeName");
      JSObject attributes = (JSObject) DOMNode.getMember("attributes");
      getAttributes(attributes);
      startElement(namespaceURI, localName, qName);
      if (((Boolean) DOMNode.call("hasChildNodes", (Object[]) null))
          .booleanValue()) {
        for (JSObject nextNode = (JSObject) DOMNode.getMember("firstChild"); nextNode != (JSObject) null; nextNode = (JSObject) nextNode
            .getMember("nextSibling"))
          walkDOMTree(nextNode);
      }
      endElement(namespaceURI, localName, qName);
    }

    ////////////////////

    private void getAttributes(Attributes attributes) {
      int nAtts = attributes.getLength();
      atts = new HashMap(nAtts);
      for (int i = nAtts; --i >= 0;)
        atts.put(attributes.getLocalName(i), attributes.getValue(i));
    }

    private void getAttributes(JSObject attributes) {
      if (attributes == null) {
        atts = new HashMap(0);
        return;
      }

      // load up only the implemented attributes

      int nAtts = ((Number) attributes.getMember("length")).intValue();
      atts = new HashMap(nAtts);
      for (int i = implementedAttributes.length; --i >= 0;) {
        Object[] attArgs = { implementedAttributes[i] };
        JSObject attNode = (JSObject) attributes.call("getNamedItem", attArgs);
        if (attNode != null) {
          String attLocalName = (String) attNode.getMember("name");
          String attValue = (String) attNode.getMember("value");
          atts.put(attLocalName, attValue);
        }
      }
    }
  }
}
