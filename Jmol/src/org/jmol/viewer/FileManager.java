/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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
package org.jmol.viewer;

import org.jmol.util.CompoundDocument;

import org.jmol.util.Logger;

import org.jmol.api.JmolAdapter;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.Hashtable;

/* ***************************************************************
 * will not work with applet
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.ReaderFactory;
*/

class FileManager {

  Viewer viewer;
  JmolAdapter modelAdapter;
  private String openErrorMessage;

  // for applet proxy
  URL appletDocumentBase = null;
  URL appletCodeBase = null; //unused currently
  String appletProxy;

  // for expanding names into full path names
  //private boolean isURL;
  private String nameAsGiven = "zapped";
  private String fullPathName;
  String fileName;
  String fileType;
  String inlineData;
  String[] inlineDataArray;
  //boolean isInline;
  //boolean isDOM;
  
  private String loadScript;  
  private File file;

  private FileOpenThread fileOpenThread;
  private FilesOpenThread filesOpenThread;
  private DOMOpenThread aDOMOpenThread;


  FileManager(Viewer viewer, JmolAdapter modelAdapter) {
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
    clear();
  }

  String getState(StringBuffer sfunc) {
    StringBuffer commands = new StringBuffer();
    if (sfunc != null) {
      sfunc.append("  _setFileState;\n");
      commands.append("function _setFileState();\n\n");
    }
    commands.append(loadScript);
    if (viewer.getModelSetFileName().equals("zapped"))
      commands.append("  zap;\n");
    if (sfunc != null)
      commands.append("\nend function;\n\n");
    return commands.toString();
  }

  String getFileTypeName(String fileName) {
    int pt = fileName.indexOf("::");
    if (pt >= 0)
      return fileName.substring(0, pt);
    Object br = getUnzippedBufferedReaderOrErrorMessageFromName(fileName);
    if (! (br instanceof BufferedReader))
      return null;
    return modelAdapter.getFileTypeName((BufferedReader) br);
  }
  
  void clear() {
    setLoadScript("", false);
  }
  
  String getLoadScript() {
    return loadScript;
  }
  
  void setLoadScript(String script, boolean isMerge) {
    if (loadScript == null || !isMerge)
      loadScript = "";
    loadScript += viewer.getLoadState() + "  " + script + "\n";
  }

  void openFile(String name, Hashtable htParams, String loadScript, boolean isMerge) {
    setLoadScript(loadScript, isMerge);
    int pt = name.indexOf("::");
    nameAsGiven = (pt >= 0 ? name.substring(pt + 2) : name);
    fileType = (pt >= 0 ? name.substring(0, pt) : null);
    Logger.info("\nFileManager.openFile(" + nameAsGiven + ") //" + name);
    openErrorMessage = fullPathName = fileName = null;
    classifyName(nameAsGiven);
    if (openErrorMessage != null) {
      Logger.error("file ERROR: " + openErrorMessage);
      return;
    }
    fileOpenThread = new FileOpenThread(fullPathName, nameAsGiven, fileType, null, htParams);
    fileOpenThread.run();
  }

  void openFiles(String modelName, String[] names, String loadScript, boolean isMerge) {
    setLoadScript(loadScript, isMerge);
    String[] fullPathNames = new String[names.length];
    String[] namesAsGiven = new String[names.length];
    String[] fileTypes = new String[names.length];
    for (int i = 0; i < names.length; i++) {
      int pt = names[i].indexOf("::");
      nameAsGiven = (pt >= 0 ? names[i].substring(pt + 2) : names[i]);
      fileType = (pt >= 0 ? names[i].substring(0, pt) : null);
      openErrorMessage = fullPathName = fileName = null;
      classifyName(nameAsGiven);
      if (openErrorMessage != null) {
        Logger.error("file ERROR: " + openErrorMessage);
        return;
      }
      fullPathNames[i] = fullPathName;
      fileTypes[i] = fileType;
      namesAsGiven[i] = nameAsGiven;
    }
    
    fullPathName = fileName = nameAsGiven = modelName;
    inlineData = "";
    //isInline = false;
    //isDOM = false;
    filesOpenThread = new FilesOpenThread(fullPathNames, namesAsGiven, fileTypes, null);
    filesOpenThread.run();
  }

  void openStringInline(String strModel, Hashtable htParams, boolean isMerge) {
    String tag = (isMerge ? "append" : "model");
    String script = "data \""+tag+" inline\"" + strModel + "end \""+tag+" inline\";";
    setLoadScript(script, isMerge);
    String sp = "";
//    if (htParams != null)
  //    for (int i = 0; i < params.length; i++)
    //    sp += "," + params[i];
    Logger.info("FileManager.openStringInline(" + sp + ")");
    openErrorMessage = null;
    fullPathName = fileName = "string";
    if (!isMerge)
      inlineData = strModel;
    //isInline = true;
    //isDOM = false;
    fileOpenThread = new FileOpenThread(fullPathName, fullPathName, null, new BufferedReader(new StringReader(
        strModel)), htParams);
    fileOpenThread.run();
  }

  void openStringsInline(String[] arrayModels, Hashtable htParams, boolean isMerge) {
    loadScript = "dataSeparator = \"~~~next file~~~\";\ndata \"model inline\"";
    for (int i = 0; i < arrayModels.length; i++) {
      if (i > 0)
        loadScript += "~~~next file~~~";
      loadScript += arrayModels[i];
    }
    loadScript += "end \"model inline\";";
    setLoadScript(loadScript, isMerge);

    String sp = "";
    //if (params != null)
      //for (int i = 0; i < params.length; i++)
        //sp += "," + params[i];
    Logger.info("FileManager.openStringInline(string[]" + sp + ")");
    openErrorMessage = null;
    fullPathName = fileName = "string[]";
    inlineDataArray = arrayModels;
    //isInline = true;
    //isDOM = false;
    String[] fullPathNames = new String[arrayModels.length];
    StringReader[] readers = new StringReader[arrayModels.length];
    for (int i = 0; i < arrayModels.length; i++) {
      fullPathNames[i] = "string["+i+"]";
      readers[i] = new StringReader(arrayModels[i]);
    }
    filesOpenThread = new FilesOpenThread(fullPathNames, fullPathNames, null, readers);
    filesOpenThread.run();
  }

  void openDOM(Object DOMNode) {
    openErrorMessage = null;
    fullPathName = fileName = "JSNode";
    inlineData = "";
    //isInline = false;
    //isDOM = true;
    aDOMOpenThread = new DOMOpenThread(DOMNode);
    aDOMOpenThread.run();
  }

  /**
   * not used in Jmol project
   * 
   * @param fullPathName
   * @param name
   * @param reader
   */
  void openReader(String fullPathName, String name, Reader reader) {
    openBufferedReader(fullPathName, name, new BufferedReader(reader));
  }

  void openBufferedReader(String fullPathName, String name, BufferedReader reader) {
    openErrorMessage = null;
    this.fullPathName = fullPathName;
    fileName = name;
    fileType = null;
    fileOpenThread = new FileOpenThread(fullPathName, fullPathName, fileType, reader, null);
    fileOpenThread.run();
  }

  boolean isGzip(InputStream is) throws Exception {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
  }

  boolean isCompoundDocument(InputStream is) throws Exception {
    byte[] abMagic = new byte[8];
    is.mark(9);
    int countRead = is.read(abMagic, 0, 8);
    is.reset();
    return (countRead == 8 && abMagic[0] == (byte) 0xD0
        && abMagic[1] == (byte) 0xCF && abMagic[2] == (byte) 0x11
        && abMagic[3] == (byte) 0xE0 && abMagic[4] == (byte) 0xA1
        && abMagic[5] == (byte) 0xB1 && abMagic[6] == (byte) 0x1A 
        && abMagic[7] == (byte) 0xE1);
  }
  
  String getFileAsString(String name) {
    //System.out.println("FileManager.getFileAsString(" + name + ")");
    Object t = getInputStreamOrErrorMessageFromName(name);
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
      InputStream is = bis;
      if (isCompoundDocument(is)) {
        CompoundDocument doc = new CompoundDocument(bis);
        return "" + doc.getAllData();
      } else if (isGzip(is)) {
        is = new GZIPInputStream(bis);
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      StringBuffer sb = new StringBuffer(8192);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
      return sb.toString();
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  /**
   * the real entry point 
   * 
   * @return string error or an AtomSetCollection
   */
  Object waitForClientFileOrErrorMessage() {
    Object clientFile = null;
    if (fileOpenThread != null) {
      clientFile = fileOpenThread.clientFile;
      if (fileOpenThread.errorMessage != null)
        openErrorMessage = fileOpenThread.errorMessage;
      else if (clientFile == null)
        openErrorMessage = "Client file is null loading:" + nameAsGiven;
      fileOpenThread = null;
    } else if (filesOpenThread != null) {
      clientFile = filesOpenThread.clientFile;
      if (filesOpenThread.errorMessage != null)
        openErrorMessage = filesOpenThread.errorMessage;
      else if (clientFile == null)
        openErrorMessage = "Client file is null loading:" + nameAsGiven;
    } else if (aDOMOpenThread != null) {
      clientFile = aDOMOpenThread.clientFile;
      if (aDOMOpenThread.errorMessage != null)
        openErrorMessage = aDOMOpenThread.errorMessage;
      else if (clientFile == null)
        openErrorMessage = "Client file is null loading:" + nameAsGiven;
      aDOMOpenThread = null;
    }
    if (openErrorMessage != null)
      return openErrorMessage;
    return clientFile;
  }

  String getFullPathName() {
    return fullPathName != null ? fullPathName : nameAsGiven;
  }
  
  void setFileInfo(String[] fileInfo) {
    try {
    fullPathName = fileInfo[0];
    fileName = fileInfo[1];
    inlineData = fileInfo[2];
    loadScript = fileInfo[3];
    } catch (Exception e) {
      Logger.error("Exception saving file info: " + e.getMessage());
    }
  }

  String[] getFileInfo() {
    return new String[]{fullPathName, fileName, inlineData, loadScript};
  }
  
  String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  String getAppletDocumentBase() {
    if (appletDocumentBase == null)
      return "";
    return appletDocumentBase.toString();    
  }
  
  void setAppletContext(URL documentBase, URL codeBase,
                               String jmolAppletProxy) {
    appletDocumentBase = documentBase;
    Logger.info("appletDocumentBase=" + documentBase);
    //    dumpDocumentBase("" + documentBase);
    appletCodeBase = codeBase;
    appletProxy = jmolAppletProxy;
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy ==  null || appletProxy.length() == 0 ? null : appletProxy);
  }
  
  void dumpDocumentBase(String documentBase) {
    Logger.info("dumpDocumentBase:" + documentBase);
    Object inputStreamOrError =
      getInputStreamOrErrorMessageFromName(documentBase);
    if (inputStreamOrError instanceof String) {
      Logger.error("file ERROR:" + inputStreamOrError);
    } else {
      BufferedReader br =
        new BufferedReader(new
                           InputStreamReader((InputStream)inputStreamOrError));
      String line;
      try {
        while ((line = br.readLine()) != null)
          Logger.info(line);
        br.close();
      } catch (Exception ex) {
        Logger.error("exception caught:" + ex);
      }
    }
  }

  // mth jan 2003 -- there must be a better way for me to do this!?
  final String[] urlPrefixes = {"http:", "https:", "ftp:", "file:"};

  private void classifyName(String name) {
    if (name == null)
      return;
    String defaultDirectory = viewer.getDefaultDirectory();
    if (appletDocumentBase != null) {
      // This code is only for the applet
      try {
        if (defaultDirectory.length() != 0 && name.indexOf(":") < 0)
          name = defaultDirectory + "/" + name;
        URL url = new URL(appletDocumentBase, name);
        fullPathName = url.toString();
        // we add one to lastIndexOf(), so don't worry about -1 return value
        fileName = fullPathName.substring(fullPathName.lastIndexOf('/') + 1,
                fullPathName.length());
      } catch (MalformedURLException e) {
        openErrorMessage = e.getMessage();
      }
      return;
    }
    // This code is for the app
    for (int i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i])) {
        try {
          URL url = new URL(name);
          fullPathName = url.toString();
          fileName = fullPathName.substring(fullPathName.lastIndexOf('/') + 1,
              fullPathName.length());
        } catch (MalformedURLException e) {
          openErrorMessage = e.getMessage();
        }
        return;
      }
    }
    if (name.indexOf(":") < 0 && defaultDirectory.length() > 0)
      name = defaultDirectory + "/" + name;
    file = new File(name);
    fullPathName = file.getAbsolutePath();
    fileName = file.getName();
  }
  
  Object getInputStreamOrErrorMessageFromName(String name) {
    String errorMessage = null;
    int iurlPrefix;
    for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0;)
      if (name.startsWith(urlPrefixes[iurlPrefix]))
        break;
    boolean isURL = (iurlPrefix >= 0);
    boolean isApplet = (appletDocumentBase != null);
    InputStream in;
    int length;
    String defaultDirectory = viewer.getDefaultDirectory();
    try {
      if (isApplet || isURL) {
        if (isApplet && isURL && appletProxy != null)
          name = appletProxy + "?url=" + URLEncoder.encode(name, "utf-8");
        else if (!isURL && defaultDirectory.length() != 0)
          name = defaultDirectory + "/" + name;
        URL url = (isApplet ? new URL(appletDocumentBase, name) : new URL(name));
        Logger.info("FileManager opening " + url.toString());
        URLConnection conn = url.openConnection();
        length = conn.getContentLength();
        in = conn.getInputStream();
      } else {
        if (!isURL && name.indexOf(":") < 0 && defaultDirectory.length() != 0)
          name = defaultDirectory + "/" + name;
        Logger.info("FileManager opening " + name);
        File file = new File(name);
        length = (int) file.length();
        in = new FileInputStream(file);
      }
      return new MonitorInputStream(in, length);
    } catch (Exception e) {
      errorMessage = "" + e;
    }
    return errorMessage;
  }

  Object getBufferedReaderForString(String string) {
    return new BufferedReader(new StringReader(string));
  }

  Object getUnzippedBufferedReaderOrErrorMessageFromName(String name) {
    Object t = getInputStreamOrErrorMessageFromName(name);
    if (t instanceof String)
      return t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream)t, 8192);
      InputStream is = bis;
      if (isCompoundDocument(is)) {
        CompoundDocument doc = new CompoundDocument(bis);
        return getBufferedReaderForString("" + doc.getAllData());
      } else if (isGzip(is)) {
        is = new GZIPInputStream(bis);
      }
      return new BufferedReader(new InputStreamReader(is));
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  class DOMOpenThread implements Runnable {
    //boolean terminated;
    String errorMessage;
    Object aDOMNode;
    Object clientFile;
	        
    DOMOpenThread(Object DOMNode) {
      this.aDOMNode = DOMNode;
    }

    public void run() {
      clientFile = modelAdapter.openDOMReader(aDOMNode);
      errorMessage = null;
      //terminated = true;
    }
  }

  class FileOpenThread implements Runnable {
    //boolean terminated;
    String errorMessage;
    String fullPathNameInThread;
    String nameAsGivenInThread;
    String fileTypeInThread;
    Object clientFile;
    BufferedReader reader;
    Hashtable htParams;
    

    FileOpenThread(String name, String nameAsGiven, String type, BufferedReader reader, Hashtable htParams) {
      fullPathNameInThread = name;
      nameAsGivenInThread = nameAsGiven;
      fileTypeInThread = type;
      this.reader = reader;
      this.htParams = htParams;
    }

    public void run() {
      if (reader != null) {
        openBufferedReader(reader);
      } else {
        Object t = getUnzippedBufferedReaderOrErrorMessageFromName(nameAsGivenInThread);
        if (t instanceof BufferedReader)
          openBufferedReader((BufferedReader) t);
        else
          errorMessage = (t == null
                          ? "error opening:" + nameAsGivenInThread
                          : (String)t);
      }
      if (errorMessage != null)
        Logger.error("file ERROR: " + fullPathNameInThread + "\n" + errorMessage);
      //terminated = true;
    }

    private void openBufferedReader(BufferedReader reader) {
      Object clientFile = modelAdapter.openBufferedReader(fullPathNameInThread, fileTypeInThread,
          reader, htParams);
      if (clientFile instanceof String)
        errorMessage = (String) clientFile;
      else
        this.clientFile = clientFile;
    }
  }
  
  class FilesOpenThread implements Runnable {
    //boolean terminated;
    String errorMessage;
    String[] fullPathNamesInThread;
    String[] namesAsGivenInThread;
    String[] fileTypesInThread;
    Object clientFile;
    Reader[] reader;
    

    FilesOpenThread(String[] name, String[] nameAsGiven, String[] types, Reader[] reader) {
      fullPathNamesInThread = name;
      namesAsGivenInThread = nameAsGiven;
      fileTypesInThread = types;
      this.reader = reader;
    }

    public void run() {
      if (reader != null) {
        openReader(reader);
      } else {
        InputStream[] istream = new InputStream[namesAsGivenInThread.length];
        for (int i = 0; i < namesAsGivenInThread.length; i++) {
          Object t = getInputStreamOrErrorMessageFromName(namesAsGivenInThread[i]);
          if (! (t instanceof InputStream)) {
            errorMessage = (t == null
                            ? "error opening:" + namesAsGivenInThread[i]
                            : (String)t);
            //terminated = true;
            return;
          }
          istream[i] = (InputStream) t;
        }
        openInputStream(istream);
      }
      if (errorMessage != null)
        Logger.error("file ERROR: " + errorMessage);
      //terminated = true;
    }

    private void openInputStream(InputStream[] istream) {
      Reader[] zistream = new Reader[istream.length];
      for (int i = 0; i < istream.length; i++) {
        BufferedInputStream bis = new BufferedInputStream(istream[i], 8192);
        InputStream is = bis;
        try {
          if (isCompoundDocument(is)) {
            CompoundDocument doc = new CompoundDocument(bis);
            zistream[i] = new StringReader("" + doc.getAllData());
          } else if (isGzip(is)) {
            zistream[i] = new InputStreamReader(new GZIPInputStream(bis));
          } else {
            zistream[i] = new InputStreamReader(is);            
          }
        } catch (Exception ioe) {
          errorMessage = ioe.getMessage();
          return;
        }
      }
      openReader(zistream);
    }

    private void openReader(Reader[] reader) {
      BufferedReader[] buffered = new BufferedReader[reader.length];
      for (int i = 0; i < reader.length; i++) {
        buffered[i] = new BufferedReader(reader[i]);
      }
      Object clientFile =
        modelAdapter.openBufferedReaders(fullPathNamesInThread, fileTypesInThread,
                                         buffered);
      if (clientFile instanceof String)
        errorMessage = (String)clientFile;
      else
        this.clientFile = clientFile;
    }
  }
}

class MonitorInputStream extends FilterInputStream {
  int length;
  int position;
  int markPosition;
  int readEventCount;
  long timeBegin;

  MonitorInputStream(InputStream in, int length) {
    super(in);
    this.length = length;
    this.position = 0;
    timeBegin = System.currentTimeMillis();
  }

  public int read() throws IOException{
    ++readEventCount;
    int nextByte = super.read();
    if (nextByte >= 0)
      ++position;
    return nextByte;
  }

  public int read(byte[] b) throws IOException {
    ++readEventCount;
    int cb = super.read(b);
    if (cb > 0)
      position += cb;
    return cb;
  }

  public int read(byte[] b, int off, int len) throws IOException {
    ++readEventCount;
    int cb = super.read(b, off, len);
    if (cb > 0)
      position += cb;
    return cb;
  }

  public long skip(long n) throws IOException {
    long cb = super.skip(n);
    // this will only work in relatively small files ... 2Gb
    position = (int)(position + cb);
    return cb;
  }

  public void mark(int readlimit) {
    super.mark(readlimit);
    markPosition = position;
  }

  public void reset() throws IOException {
    position = markPosition;
    super.reset();
  }

  int getPosition() {
    return position;
  }

  int getLength() {
    return length;
  }

  int getPercentageRead() {
    return position * 100 / length;
  }

  int getReadingTimeMillis() {
    return (int)(System.currentTimeMillis() - timeBegin);
  }

}
