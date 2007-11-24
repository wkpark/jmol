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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Hashtable;
import java.util.Vector;

/* ***************************************************************
 * will not work with applet
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.ReaderFactory;
*/

class FileManager {

  private Viewer viewer;
  private String openErrorMessage;
 
  JmolAdapter modelAdapter;

  // for applet proxy
  private URL appletDocumentBase = null;
  private URL appletCodeBase = null; //unused currently
  private String appletProxy;

  // for expanding names into full path names
  //private boolean isURL;
  private String nameAsGiven = "zapped";
  private String fullPathName;
  private String fileName;
  private String fileType;

  private String inlineData;
  private String[] inlineDataArray;
  
  String getInlineData(int iData) {
    return (iData < 0 ? inlineData : iData < inlineDataArray.length ? inlineDataArray[iData] : "");  
  }
     
  String[] getInlineDataArray() {
    return inlineDataArray;  
  }

  private String loadScript;  
  private File file;

  FileOpenThread fileOpenThread;
  FilesOpenThread filesOpenThread;
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
    Object br = getUnzippedBufferedReaderOrErrorMessageFromName(fileName, true);
    if (br instanceof BufferedReader)
      return modelAdapter.getFileTypeName((BufferedReader) br);
    if (br instanceof ZipInputStream) {
      String zipDirectory = getZipDirectoryAsString(fileName);
      return modelAdapter.getFileTypeName(new BufferedReader(new StringReader(zipDirectory)));
    }
    return null;
  }
  
  void clear() {
    setLoadScript("", false);
  }
  
  String getLoadScript() {
    return loadScript;
  }
  
  private void setLoadScript(String script, boolean isMerge) {
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
    //inlineDataArray = arrayModels;
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

  private void openBufferedReader(String fullPathName, String name, BufferedReader reader) {
    openErrorMessage = null;
    this.fullPathName = fullPathName;
    fileName = name;
    fileType = null;
    fileOpenThread = new FileOpenThread(fullPathName, fullPathName, fileType, reader, null);
    fileOpenThread.run();
  }

  static boolean isGzip(InputStream is) throws Exception {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
  }

  static boolean isZipDirectory(InputStream is) throws Exception {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x50 && abMagic[1] == (byte) 0x4B
        && abMagic[2] == (byte) 0x03 && abMagic[3] == (byte) 0x04);
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
    Object t = getInputStreamOrErrorMessageFromName(name, false);
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
      } else if (isZipDirectory(is)) {
        return "Error: Cannot read ZIP format as string";
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
    // not compatible with inlineDataArray, by the way....
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
    appletCodeBase = codeBase;
    Logger.info("appletDocumentBase=" + appletDocumentBase + "\nappletCodeBase=" + appletCodeBase);
    //    dumpDocumentBase("" + documentBase);
    appletProxy = jmolAppletProxy;
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy ==  null || appletProxy.length() == 0 ? null : appletProxy);
  }
/*  
  private void dumpDocumentBase(String documentBase) {
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
*/
  // mth jan 2003 -- there must be a better way for me to do this!?
  private final static String[] urlPrefixes = {"http:", "https:", "ftp:", "file:"};

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
  
  Object getInputStreamOrErrorMessageFromName(String name, boolean showMsg) {
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
        name = url.toString();
        if (showMsg)
          Logger.info("FileManager opening " + url.toString());
        URLConnection conn = url.openConnection();
        length = conn.getContentLength();
        in = conn.getInputStream();
      } else {
        if (!isURL && name.indexOf(":") < 0 && defaultDirectory.length() != 0)
          name = defaultDirectory + "/" + name;
        if (showMsg)
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

  Object getUnzippedBufferedReaderOrErrorMessageFromName(String name, boolean allowZipStream) {
    Object t = getInputStreamOrErrorMessageFromName(name, true);
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
      } else if (isZipDirectory(is)) {
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
            is = dumpZipDirectory(is, name);
          ZipInputStream zis = new ZipInputStream(is);
          ZipEntry ze = zis.getNextEntry();
          if (ze == null)
            return "Error: No Zip Entry found";
          if (allowZipStream)
            return new ZipInputStream(is);
          //otherwise just get first file
          while (ze != null && ze.isDirectory())
            ze = zis.getNextEntry();
          String s = getZipEntryAsString(zis);
          zis.closeEntry();
          zis.close();
          return new BufferedReader(new StringReader(s));
      }
      return new BufferedReader(new InputStreamReader(is));
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  private InputStream dumpZipDirectory(InputStream is, String fileName) throws IOException {
    ZipInputStream zis = new  ZipInputStream(is);
    ZipEntry ze;
    while ((ze = zis.getNextEntry()) != null) {
      Logger.debug(ze.getCompressedSize() + " " + ze.getName());
    }
    is.close();
    return new BufferedInputStream((InputStream)getInputStreamOrErrorMessageFromName(fileName, false), 8192);
  }
  
  static String[] getZipDirectoryAndClose(InputStream is) throws IOException {
    Vector v = new Vector();
    ZipInputStream zis = new ZipInputStream(is);
    ZipEntry ze;
    while ((ze = zis.getNextEntry()) != null)
      v.addElement(ze.getName());
    is.close();
    int len = v.size();
    String[] dirList = new String[len];
    for (int i = 0; i < len; i++)
      dirList[i] = (String) v.elementAt(i);
    return dirList;
  }
  
  String getZipDirectoryAsString(String fileName) {
    StringBuffer sb = new StringBuffer();
    InputStream is = new BufferedInputStream((InputStream)getInputStreamOrErrorMessageFromName(fileName, false), 8192);
    String[] s = new String[0];
    try {
      s = getZipDirectoryAndClose(is);
    } catch (Exception e) {      
    }
    for (int i = 0; i < s.length; i++)
      sb.append(s[i]).append('\n');
    return sb.toString();
  }
  
  static String getZipEntryAsString(ZipInputStream zis) throws IOException {
    StringBuffer sb = new StringBuffer();
    byte[] buf = new byte[1024];
    int len;
    while (zis.available() == 1 && (len = zis.read(buf)) > 0)
      sb.append(new String(buf, 0, len));
    return sb.toString();
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
        Object t = getUnzippedBufferedReaderOrErrorMessageFromName(nameAsGivenInThread, true);
        if (t instanceof BufferedReader) {
          openBufferedReader((BufferedReader) t);
        } else if (t instanceof ZipInputStream) {
          openZipStream();
        } else {
          errorMessage = (t == null
                          ? "error opening:" + nameAsGivenInThread
                          : (String)t);
        }
      }
      if (errorMessage != null)
        Logger.error("file ERROR: " + fullPathNameInThread + "\n" + errorMessage);
      //terminated = true;
    }

    private void openZipStream() {
      String fileName = nameAsGivenInThread;
      String zipDirectory = getZipDirectoryAsString(fileName);
      String fileType = modelAdapter.getFileTypeName(new BufferedReader(new StringReader(zipDirectory)));
      boolean isSpartan = "SpartanSmol".equals(fileType);
      InputStream is = new BufferedInputStream(
          (InputStream) getInputStreamOrErrorMessageFromName(fileName, false), 8192);
      String[] files = new String[0];
      try {
        files = getZipDirectoryAndClose(is);
      } catch (Exception e) {
      }
      int nFiles;
      StringBuffer data = new StringBuffer();
      if (isSpartan) {
        nFiles = 1;
        data = new StringBuffer();
        data.append("Zip File Directory: ").append("\n").append(zipDirectory).append("\n");
      } else {
        nFiles = 0;
      for (int i = 0; i < files.length; i++)
        if (files[i].lastIndexOf("/") != files[i].length() - 1)
          nFiles++;
      }
      String[] fullPathNames = new String[nFiles];
      StringReader[] readers = new StringReader[nFiles];
      
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
          (InputStream) getInputStreamOrErrorMessageFromName(fileName, false), 8192));
      ZipEntry ze;
      int i = 0;
      try {
        while ((ze = zis.getNextEntry()) != null) {
          if (ze.isDirectory())
            continue;
          String s = getZipEntryAsString(zis);
          if (isSpartan) {
            String thisEntry = ze.getName();
            data.append("\nBEGIN Zip File Entry: ").append(thisEntry).append("\n");            
            data.append(s);
            data.append("\nEND Zip File Entry: ").append(thisEntry).append("\n");            
            data.append(ze.getName()).append("/n");
          } else {
            fullPathNames[i] = "zip://" + ze.getName();
            readers[i] = new StringReader(s);
            i++;
          }
        }

      } catch (Exception e) {

      }

      if (isSpartan) {
        fullPathNames[0] = fileName;
        readers[0] = new StringReader(data.toString());
      }
      fileOpenThread = null;
      filesOpenThread = new FilesOpenThread(fullPathNames, fullPathNames, null,
          readers);
      filesOpenThread.run();
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
    private String[] fullPathNamesInThread;
    private String[] namesAsGivenInThread;
    private String[] fileTypesInThread;
    Object clientFile;
    private Reader[] reader;

    FilesOpenThread(String[] name, String[] nameAsGiven, String[] types, Reader[] reader) {
      fullPathNamesInThread = name;
      namesAsGivenInThread = nameAsGiven;
      fileTypesInThread = types;
      this.reader = reader;
    }

    public void run() {
      if (reader != null) {
        openReaders();
        reader = null;
      } else {
        InputStream[] istream = new InputStream[namesAsGivenInThread.length];
        for (int i = 0; i < namesAsGivenInThread.length; i++) {
          Object t = getInputStreamOrErrorMessageFromName(namesAsGivenInThread[i], true);
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
      reader = new Reader[istream.length];
      for (int i = 0; i < istream.length; i++) {
        BufferedInputStream bis = new BufferedInputStream(istream[i], 8192);
        InputStream is = bis;
        try {
          if (isCompoundDocument(is)) {
            CompoundDocument doc = new CompoundDocument(bis);
            reader[i] = new StringReader("" + doc.getAllData());
          } else if (isGzip(is)) {
            reader[i] = new InputStreamReader(new GZIPInputStream(bis));
          } else {
            reader[i] = new InputStreamReader(is);            
          }
        } catch (Exception ioe) {
          errorMessage = ioe.getMessage();
          return;
        }
      }
      openReaders();
    }

    private void openReaders() {
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
