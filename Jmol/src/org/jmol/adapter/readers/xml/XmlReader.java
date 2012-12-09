/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-02 11:48:43 -0500 (Wed, 02 Aug 2006) $
 * $Revision: 5364 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.adapter.readers.xml;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Resolver;
import org.jmol.api.Interface;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.Logger;

/**
 * A generic XML reader template -- by itself, does nothing.
 * 
 * The actual readers are XmlCmlReader, XmlMolproReader (which is an
 * extension of XmlCmlReader), XmlChem3dReader, and XmlOdysseyReader.
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
 * XmlHandler extends DefaultHandler is the generic interface to both reader and DOM element parsing.
 * 
 * XmlCmlReader extends XmlReader
 * 
 * XmlMolproReader extends XmlCmlReader. If you feel like expanding on that, feel free.
 * 
 * XmlChem3dReader extends XmlReader. That one is simple; no need to expand on it at this time.
 * 
 * XmlOdysseyReader extends XmlReader. That one is simple; no need to expand on it at this time.
 * 
 * Note that the tag processing routines are shared between SAX 
 * and DOM processors. This means that attributes must be
 * transformed from either Attributes (SAX) or JSObjects (DOM)
 * to Hashtable name:value pairs. This is taken care of in JmolXmlHandler
 * for all readers. 
 * 
 * TODO 27/8/06:
 * 
 * Several aspects of CifReader are NOT YET implemented here. 
 * These include loading a specific model when there are several,
 * applying the symmetry, and loading fractional coordinates. [DONE for CML reader 2/2007 RMH]
 * 
 *  
 *  Test files:
 *  
 *  molpro:  vib.xml
 *  odyssey: water.xodydata
 *  cml: a wide variety of files in data-files. 
 * 
 * -Bob Hanson
 * 
 */

 public class XmlReader extends AtomSetCollectionReader {

  protected Atom atom;
  protected String[] domAttributes = { "id" };
  protected XmlReader parent;    // XmlReader itself; to be assigned by the subReader
  public Map<String, String> atts = new Hashtable<String, String>();

  /////////////// file reader option //////////////

  @Override
  public void initializeReader() throws Exception {
    Object saxReader = null;
    /**
     * @j2sNative
     * 
     * 
     * 
     */
    {
      try {
        javax.xml.parsers.SAXParserFactory spf = javax.xml.parsers.SAXParserFactory
            .newInstance();
        spf.setNamespaceAware(true);
        javax.xml.parsers.SAXParser saxParser = spf.newSAXParser();
        saxReader = saxParser.getXMLReader();
        Logger.debug("Using JAXP/SAX XML parser.");
      } catch (Exception e) {
        Logger.debug("Could not instantiate JAXP/SAX XML reader: "
            + e.getMessage());
      }
      if (saxReader == null) {
        atomSetCollection = new AtomSetCollection("xml", this, null, null);
        atomSetCollection.errorMessage = "No XML reader found";
        return;
      }
    }
    atomSetCollection = new AtomSetCollection(readerName, this, null, null);
    String className = null;
    try {
      int pt = readerName.indexOf("(");
      String name = (pt < 0 ? readerName : readerName.substring(0, pt));
      className = Resolver.getReaderClassBase(name);
      Class<?> atomSetCollectionReaderClass = Class.forName(className);
      XmlReader thisReader = (XmlReader) atomSetCollectionReaderClass.newInstance();
      Object domNode = null;
      /**
       * 
       * @j2sNative
       * domNode = this.viewer.applet._createDomNode("xmlReader",this.reader.lock.lock);
       *
       */
      {
      }
      thisReader.processXml(this, atomSetCollection, reader, domNode, saxReader);
    } catch (Exception e) {
      atomSetCollection = new AtomSetCollection("xml", this, null, null);
      atomSetCollection.errorMessage = "File reader was not found: "
          + className;
    }
    continuing = false;
    /**
     * @j2sNative
     * 
     * this.viewer.applet._createDomNode("xmlReader",null);
     * 
     */
    {
    }
  }

  /**
   * 
   * @param parent
   * @param atomSetCollection
   * @param reader
   * @param domNode
   * @param saxReader
   * @throws Exception 
   */
  protected void processXml(XmlReader parent,
                            AtomSetCollection atomSetCollection,
                            BufferedReader reader, Object domNode,
                            Object saxReader) throws Exception {
    this.parent = parent;
    this.atomSetCollection = atomSetCollection;
    this.reader = reader;
    if (saxReader == null) {
      domAttributes = getDOMAttributes();
      walkDOMTree(domNode);
    } else {
        JmolXmlHandler saxHandler = (JmolXmlHandler) Interface.getOptionInterface("adapter.readers.xml.XmlHandler");
        saxHandler.set(this, saxReader);
        saxHandler.processXml(saxReader, reader);
    }
  }

  /////////////// DOM option //////////////
  
  @Override
  protected void processDOM(Object DOMNode) {
    atomSetCollection = new AtomSetCollection(readerName, this, null, null);
    String className = null;
    Class<?> atomSetCollectionReaderClass;
    XmlReader thisReader = null;
    String name = readerName.substring(0, readerName.indexOf("("));
    try {
      className = Resolver.getReaderClassBase(name);
      atomSetCollectionReaderClass = Class.forName(className);//,true, Thread.currentThread().getContextClassLoader());
      thisReader = (XmlReader) atomSetCollectionReaderClass
          .newInstance();
      thisReader.processXml(this, atomSetCollection, reader, DOMNode, null);
    } catch (Exception e) {
      atomSetCollection.errorMessage = "File reader was not found:" + className;
    }
  }

  protected String[] getDOMAttributes() {
    // different subclasses will implement this differently
    // it is only for DOM nodes
    return domAttributes;
  }
  
  @Override
  public void applySymmetryAndSetTrajectory() {
    try {
      if (parent == null) 
        super.applySymmetryAndSetTrajectory();
      else
        parent.applySymmetryAndSetTrajectory();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Logger.error("applySymmetry failed: " + e);
    }
  }

  ////////////////////////////////////////////////////////////////

  /**
   * 
   * @param localName
   */
  protected void processStartElement(String localName) {
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

  /**
   * 
   * @param localName
   */
  void processEndElement(String localName) {
    /* 
     * specific to each xml reader
     */
  }

  //////////////////// DOM or JavaScript parsing /////////////////

  // walk DOM tree given by JSObject. For every element, call
  // startElement with the appropriate strings etc., and then
  // endElement when the element is closed.

  private void walkDOMTree(Object DOMNodeObj) {
    String localName;
    /**
     * @j2sNative
     * 
     * 
     *            localName = this.jsObjectGetMember(DOMNodeObj,
     *            "nodeName").toLowerCase();
     */
    {
      localName = ((String) jsObjectGetMember(DOMNodeObj, "localName"))
          .toLowerCase();
      //namespaceURI = (String) jsObjectGetMember(DOMNodeObj, "namespaceURI");
      //qName = (String) jsObjectGetMember(DOMNodeObj, "nodeName");
      if (localName == null)
        return;
    }
    if (localName.equals("text")) {
      if (keepChars)
        chars = (String) jsObjectGetMember(DOMNodeObj, "innerHTML");
      return;
    }
    getDOMAttributes(jsObjectGetMember(DOMNodeObj, "attributes"));
    processStartElement(localName);
    boolean haveChildren;
    /**
     * @j2sNative
     * 
     *            haveChilden = this.jsObjectCall(DOMNodeObj, "hasChildNodes",
     *            null);
     * 
     */
    {
      haveChildren = ((Boolean) jsObjectCall(DOMNodeObj, "hasChildNodes",
          null)).booleanValue();
    }
    if (haveChildren)
      for (Object nextNode = jsObjectGetMember(DOMNodeObj, "firstChild"); nextNode != null; nextNode = jsObjectGetMember(
          nextNode, "nextSibling"))
        walkDOMTree(nextNode);
    processEndElement(localName);
  }

  private void getDOMAttributes(Object attributes) {
    atts.clear();
    if (attributes != null) {
      return;
    }

    // load up only the implemented attributes

    /**
     * @j2sNative
     * 
     *            if (!this.jsObjectGetMember(attributes, "length")) return;
     * 
     */
    {
      Number n = (Number) jsObjectGetMember(attributes, "length");
      if (n == null || Integer.parseInt(n.toString()) == 0)
        return;
    }
    for (int i = domAttributes.length; --i >= 0;) {
      Object[] attArgs = { domAttributes[i] };
      Object attNode = jsObjectCall(attributes, "getNamedItem", attArgs);
      if (attNode != null) {
        String attLocalName = (String) jsObjectGetMember(attNode, "name");
        String attValue = (String) jsObjectGetMember(attNode, "value");
        if (attLocalName != null && attValue != null)
          atts.put(attLocalName, attValue);
      }
    }
  }

  private Object jsObjectCall(Object jsObject, String method,
                              Object[] args) {
    return parent.viewer.getJsObjectInfo(jsObject, method, args);
  }

  private Object jsObjectGetMember(Object jsObject, String name) {
    return parent.viewer.getJsObjectInfo(jsObject, name, null);
  }

}
