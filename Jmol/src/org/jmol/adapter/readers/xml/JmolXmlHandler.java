package org.jmol.adapter.readers.xml;

import java.io.BufferedReader;

interface JmolXmlHandler {  

  void processXml(Object saxReader, BufferedReader reader) throws Exception;

  void set(XmlReader xmlReader, Object saxReader) throws Exception;

}
