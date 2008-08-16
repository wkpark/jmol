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
import org.jmol.util.TextFormat;
import org.jmol.util.ZipUtil;

import org.jmol.util.Logger;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolFileReaderInterface;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
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
import java.util.zip.ZipInputStream;
import java.util.Hashtable;

/* ***************************************************************
 * will not work with applet
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.ReaderFactory;
*/

public class FileManager {

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
  
  String getInlineData(int iData) {
    return (iData < 0 ? inlineData : "");//iData < inlineDataArray.length ? inlineDataArray[iData] : "");  
  }

  private String loadScript; 

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
    Object br = getUnzippedBufferedReaderOrErrorMessageFromName(fileName, true, false, true);
    if (br instanceof BufferedReader)
      return modelAdapter.getFileTypeName((BufferedReader) br);
    if (br instanceof ZipInputStream) {
      String zipDirectory = getZipDirectoryAsString(fileName);
      return modelAdapter.getFileTypeName(getBufferedReaderForString(zipDirectory));      
    }
    if (br instanceof String[]) {
      return ((String[])br)[0];
    }
    return null;
  }
  
  void clear() {
    setLoadScript("", false);
  }
  
  String getLoadScript() {
    return loadScript;
  }
  
  private void setLoadScript(String script, boolean isAppend) {
    if (loadScript == null || !isAppend)
      loadScript = "";
    loadScript += viewer.getLoadState();
    addLoadScript(script);
  }

  void addLoadScript(String script) {
    if (script == null)
      return;
    if (script.equals("-")) {
      loadScript = "";
      return;
    }
    loadScript += "  " + script + ";\n";  
  }
  
  void openFile(String name, Hashtable htParams, String loadScript, boolean isAppend) {
    setLoadScript(loadScript, isAppend);
    int pt = name.indexOf("::");
    nameAsGiven = (pt >= 0 ? name.substring(pt + 2) : name);
    fileType = (pt >= 0 ? name.substring(0, pt) : null);
    Logger.info("\nFileManager.openFile(" + nameAsGiven + ") //" + name);
    openErrorMessage = fullPathName = fileName = null;
    setNames(classifyName(nameAsGiven));
    htParams.put("fullPathName", (fileType == null ? "" : fileType + "::")
        + fullPathName.replace('\\', '/'));
    if (openErrorMessage != null) {
      Logger.error("file ERROR: " + openErrorMessage);
      return;
    }
    if (viewer.getMessageStyleChime() && viewer.getDebugScript())
      viewer.scriptStatus("Requesting " + fullPathName);
    fileOpenThread = new FileOpenThread(fullPathName, nameAsGiven, fileType, null, htParams);
    fileOpenThread.run();
  }

  void openFiles(String modelName, String[] names, String loadScript, boolean isAppend) {
    setLoadScript(loadScript, isAppend);
    String[] fullPathNames = new String[names.length];
    String[] namesAsGiven = new String[names.length];
    String[] fileTypes = new String[names.length];
    for (int i = 0; i < names.length; i++) {
      int pt = names[i].indexOf("::");
      nameAsGiven = (pt >= 0 ? names[i].substring(pt + 2) : names[i]);
      fileType = (pt >= 0 ? names[i].substring(0, pt) : null);
      openErrorMessage = fullPathName = fileName = null;
      setNames(classifyName(nameAsGiven));
      if (openErrorMessage != null) {
        Logger.error("file ERROR: " + openErrorMessage);
        return;
      }
      fullPathNames[i] = fullPathName;
      names[i] = fullPathName.replace('\\','/');
      fileTypes[i] = fileType;
      namesAsGiven[i] = nameAsGiven;
    }
    
    fullPathName = fileName = nameAsGiven = modelName;
    inlineData = "";
    //inlineDataArray = null;
    filesOpenThread = new FilesOpenThread(fullPathNames, namesAsGiven, fileTypes, null);
    filesOpenThread.run();
  }

  void openStringInline(String strModel, Hashtable htParams, boolean isAppend) {
    String tag = (isAppend ? "append" : "model");
    String script = "data \""+tag+" inline\"\n" + strModel + "end \""+tag+" inline\";";
    setLoadScript(script, isAppend);
    Logger.info("FileManager.openStringInline()");
    openErrorMessage = null;
    fullPathName = fileName = "string";
    inlineData = strModel;
    //inlineDataArray = null;
    fileOpenThread = new FileOpenThread("string", "string", null, 
        getBufferedReaderForString(strModel), htParams);
    fileOpenThread.run();
  }

  void openStringsInline(String[] arrayModels, Hashtable htParams, boolean isAppend) {
    String oldSep = "\"" + viewer.getDataSeparator() + "\"";
    String tag = "\"" + (isAppend ? "append" : "model") + " inline\"";
    String script = "set dataSeparator \"~~~next file~~~\";\ndata " + tag;
    for (int i = 0; i < arrayModels.length; i++) {
      if (i > 0)
        script += "~~~next file~~~";
      script += arrayModels[i];
    }
    script += "end " + tag + ";set dataSeparator " + oldSep;
    setLoadScript(script, isAppend);
    Logger.info("FileManager.openStringsInline(string[])");
    openErrorMessage = null;
    fullPathName = fileName = "string[]";
    inlineData = "";
    //just too complicated and space-absorbing inlineDataArray = arrayModels;
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
    //inlineDataArray = null;
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

  public Object getFileAsBytes(String name) {
    //?? used by eval of "WRITE FILE"
    // will be full path name
    if (name == null)
      return null;
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) 
      name = (subFileList = TextFormat.split(name, "|"))[0];
    //System.out.println("FileManager.getFileAsString(" + name + ")");
    Object t = getInputStreamOrErrorMessageFromName(name, false);
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
      InputStream is = bis;
      Object bytes = (ZipUtil.isZipFile(is) && subFileList != null && 1 < subFileList.length
          ? ZipUtil.getZipFileContentsAsBytes(is, subFileList, 1)
              : ZipUtil.getStreamAsBytes(bis));
      is.close();
      return bytes;
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  /**
   * 
   * @param data [0] initially path name, but returned as full path name; [1]file contents (directory listing for a ZIP/JAR file) or error string
   * @return true if successful; false on error 
   */
  boolean getFileDataOrErrorAsString(String[] data) {
    data[1] = "";
    String name = data[0];
    if (name == null)
      return false;
    Object t = getBufferedReaderOrErrorMessageFromName(name, data, false);
    if (t instanceof String) {
      data[1] = (String) t;
      return false;
    }
    try {
      BufferedReader br = (BufferedReader) t;
      StringBuffer sb = new StringBuffer(8192);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
      br.close();
      data[1] = sb.toString();
      return true;
    } catch (Exception ioe) {
      data[1] = ioe.getMessage();
      return false;
    }
  }

  Object getFileAsImage(String name, Hashtable htParams) {
    if (name == null)
      return "";
    String[] names = classifyName(name);
    if (names == null)
      return "cannot read file name: " + name;
    Image image = null;
    //try {
    fullPathName = names[0].replace('\\', '/');
    if (urlTypeIndex(fullPathName) >= 0)
      try {
        image = Toolkit.getDefaultToolkit().createImage(new URL(fullPathName));
      } catch (Exception e) {
        return "bad URL: " + fullPathName;
      }
    else
      image = Toolkit.getDefaultToolkit().createImage(fullPathName);
    try {
      MediaTracker mediaTracker = new MediaTracker(viewer.getAwtComponent());
      mediaTracker.addImage(image, 0);
      mediaTracker.waitForID(0); 
      /* SUN but here for malformed URL - can't trap
Uncaught error fetching image:
java.lang.NullPointerException
  at sun.net.www.ParseUtil.toURI(Unknown Source)
  at sun.net.www.protocol.http.HttpURLConnection.plainConnect(Unknown Source)
  at sun.net.www.protocol.http.HttpURLConnection.connect(Unknown Source)
  at sun.net.www.protocol.http.HttpURLConnection.getInputStream(Unknown Source)
  at sun.awt.image.URLImageSource.getDecoder(Unknown Source)
  at sun.awt.image.InputStreamImageSource.doFetch(Unknown Source)
  at sun.awt.image.ImageFetcher.fetchloop(Unknown Source)
  at sun.awt.image.ImageFetcher.run(Unknown Source)
       */
    } catch (Exception e) {
      return e.getMessage() + " opening " + fullPathName;
    }
    if (image.getWidth(null) < 1)
      return "invalid or missing image " + fullPathName;
    htParams.put("fullPathName", fullPathName);
    return image;
  }
  
  
  /**
   * 
   * @param name
   * @return file contents; directory listing for a ZIP/JAR file
   */
  private String getFullFilePathAsString(String name) {
    if (name == null)
      return "";
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) 
      name = (subFileList = TextFormat.split(name, "|"))[0];
    //System.out.println("FileManager.getFileAsString(" + name + ")");
    Object t = getInputStreamOrErrorMessageFromName(name, false);
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
      InputStream is = bis;
      if (CompoundDocument.isCompoundDocument(is)) {
        CompoundDocument doc = new CompoundDocument(bis);
        return "" + doc.getAllData();
      } else if (isGzip(is)) {
        is = new GZIPInputStream(bis);
      } else if (ZipUtil.isZipFile(is)) {
        return (String) ZipUtil.getZipFileContents(is, subFileList, 1, false);
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      StringBuffer sb = new StringBuffer(8192);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
      br.close();
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
      filesOpenThread = null;
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
    appletCodeBase = codeBase;
    Logger.info("appletDocumentBase=" + appletDocumentBase + "\nappletCodeBase=" + appletCodeBase);
    //    dumpDocumentBase("" + documentBase);
    appletProxy = jmolAppletProxy;
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy ==  null || appletProxy.length() == 0 ? null : appletProxy);
  }

  private final static String[] urlPrefixes = {"http:", "https:", "ftp:", "file:"};

  private void setNames(String[] names) {
    fullPathName = names[0];
    fileName = names[1];
  }
  
  private String[] classifyName(String name) {
    if (name == null)
      return null;
    String[] names = new String[2];
    if (name.indexOf("=") == 0)
      name = TextFormat.formatString(viewer.getLoadFormat(), "FILE", name.substring(1));
    String defaultDirectory = viewer.getDefaultDirectory();
    if (name.indexOf(":") < 0)
      name = addDirectory(defaultDirectory, name);
    if (appletDocumentBase != null) {
      // This code is only for the applet
      try {
        if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
          name = "file:///" + name;
        //System.out.println("filemanager name " + name);
        //System.out.println("filemanager adb " + appletDocumentBase);
        URL url = new URL(appletDocumentBase, name);
        names[0] = url.toString();
        // we add one to lastIndexOf(), so don't worry about -1 return value
        names[1] = names[0].substring(names[0].lastIndexOf('/') + 1,
                names[0].length());
        //System.out.println("filemanager 0 " + names[0]);
        //System.out.println("filemanager 1 " + names[1]);
      } catch (MalformedURLException e) {
        openErrorMessage = e.getMessage();
      }
      return names;
    }
    // This code is for the app
    int i = urlTypeIndex(name);
    if (i >= 0) {
      try {
        URL url = new URL(name);
        names[0] = url.toString();
        names[1] = names[0].substring(names[0].lastIndexOf('/') + 1,
            names[0].length());
      } catch (MalformedURLException e) {
        openErrorMessage = e.getMessage();
      }
      return names;
    }
    File file = new File(name);
    names[0] = file.getAbsolutePath();
    names[1] = file.getName();
    return names;
  }

  private static int urlTypeIndex(String name) {
    for (int i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i])) {
        return i;
      }
    }
    return -1;
  }
  
  private String addDirectory(String defaultDirectory, String name) {
    if (defaultDirectory.length() == 0)
      return name;
    char ch = (name.length() > 0 ? name.charAt(0) : ' ');
    String s = defaultDirectory.toLowerCase();
    if ((s.endsWith(".zip") || s.endsWith(".tar")) && ch != '|' && ch != '/')
      defaultDirectory += "|";
    return defaultDirectory 
        + (ch == '/' || ch == '/'
        || (ch = defaultDirectory.charAt(defaultDirectory.length() - 1)) == '|' || ch == '/' ? "" : "/") 
        + name;
  }
  
  Object getInputStreamOrErrorMessageFromName(String name, boolean showMsg) {
    return getInputStream(name, showMsg, appletDocumentBase, appletProxy);    
  }
  
  public static Object getInputStream(String name, boolean showMsg, URL appletDocumentBase, String appletProxy) {
    //System.out.println("inputstream for " + name);
    String errorMessage = null;
    int iurlPrefix;
    for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0;)
      if (name.startsWith(urlPrefixes[iurlPrefix]))
        break;
    boolean isURL = (iurlPrefix >= 0);
    boolean isApplet = (appletDocumentBase != null);
    InputStream in;
    int length;
    try {
      if (isApplet || isURL) {
        if (isApplet && isURL && appletProxy != null)
          name = appletProxy + "?url=" + URLEncoder.encode(name, "utf-8");
        URL url = (isApplet ? new URL(appletDocumentBase, name) : new URL(name));
        name = url.toString();
        if (showMsg)
          Logger.info("FileManager opening " + url.toString());
        URLConnection conn = url.openConnection();
        length = conn.getContentLength();
        in = conn.getInputStream();
      } else {
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

  BufferedReader getBufferedReaderForString(String string) {
    return new BufferedReader(new StringReader(string));
  }

  Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary) {
    String[] names = classifyName(name);
    if (names == null)
      return "cannot read file name: " + name;
    if (fullPathNameReturn != null)
      fullPathNameReturn[0] = names[0].replace('\\', '/');
    return getUnzippedBufferedReaderOrErrorMessageFromName(names[0], false,
        isBinary, false);
  }

  Object getUnzippedBufferedReaderOrErrorMessageFromName(String name,
                                                         boolean allowZipStream,
                                                         boolean asInputStream,
                                                         boolean isTypeCheckOnly) {
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) 
      name = (subFileList = TextFormat.split(name, "|"))[0];
    String[] fileSet = modelAdapter.specialLoad(name, null);
    if (fileSet != null) {
      if (isTypeCheckOnly)
        return fileSet;
      if (fileSet[2] != null) {
        StringBuffer sb = new StringBuffer();
        String header = fileSet[1];
        for (int i = 2; i < fileSet.length; i++) {
          name = fileSet[i];
          if (header != null)
            sb.append("BEGIN " + header + " " + name + "\n");
          sb.append(getFullFilePathAsString(name));
          if (header != null)
            sb.append("\nEND " + header + " " + name + "\n");
        }
        return getBufferedReaderForString(sb.toString());
      }
      //continuing...
      //here, for example, for an SPT file load that is not just a type check
      //(type check is only for application file opening and drag-drop to determine if 
      //script or load command should be used)
    }
    Object t = getInputStreamOrErrorMessageFromName(name, true);
    if (t instanceof String)
      return t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream)t, 8192);
      InputStream is = bis;
      if (CompoundDocument.isCompoundDocument(is)) {
        CompoundDocument doc = new CompoundDocument(bis);
        return getBufferedReaderForString("" + doc.getAllData());
      } else if (isGzip(is)) {
        is = new GZIPInputStream(bis);
      } else if (ZipUtil.isZipFile(is)) {
        if (allowZipStream)
          return new ZipInputStream(bis);
        if (asInputStream)
          return (InputStream) ZipUtil.getZipFileContents(is, subFileList, 1, true);
        //danger -- converting bytes to String here. 
        //we lose 128-156 or so.
        String s = (String) ZipUtil.getZipFileContents(is, subFileList, 1, false);
        is.close();
        return getBufferedReaderForString(s);
      }
      if (asInputStream)
        return is;
      return new BufferedReader(new InputStreamReader(is));
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  String[] getZipDirectory(String fileName, boolean addManifest) {
    return ZipUtil.getZipDirectoryAndClose((InputStream)getInputStreamOrErrorMessageFromName(fileName, false), addManifest);
  }
  
  String getZipDirectoryAsString(String fileName) {
    return ZipUtil.getZipDirectoryAsStringAndClose((InputStream)getInputStreamOrErrorMessageFromName(fileName, false));
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
        openBufferedReader();
      } else {
        String name = fullPathNameInThread;
        String[] subFileList = null;
        if (name.indexOf("|") >= 0) 
          name = (subFileList = TextFormat.split(name, "|"))[0];
        Object t = getUnzippedBufferedReaderOrErrorMessageFromName(name, true, false, false);
        if (t instanceof BufferedReader) {
          reader = (BufferedReader) t;
          openBufferedReader();
        } else if (t instanceof ZipInputStream) {
          if (subFileList != null)
            htParams.put("subFileList", subFileList);
          openZipStream(name, (ZipInputStream) t);
        } else {
          errorMessage = (t == null
                          ? "error opening:" + nameAsGivenInThread
                          : (String)t);
        }
      }
      if (errorMessage != null) {
        Logger.error("file ERROR: " + fullPathNameInThread + "\n" + errorMessage);
      }
      //terminated = true;
    }

    private void openZipStream(String fileName, ZipInputStream zis) {
      String[] zipDirectory = getZipDirectory(fileName, true);
      Object clientFile = modelAdapter.openZipFiles(zis, fileName, zipDirectory,
          htParams, false);
      if (clientFile instanceof String)
        errorMessage = (String) clientFile;
      else
        this.clientFile = clientFile;
      try {
        zis.close();
      } catch (Exception e) {
        //
      }
    }
    
    private void openBufferedReader() {
      Object clientFile = modelAdapter.openBufferedReader(fullPathNameInThread, fileTypeInThread,
          reader, htParams);
      if (clientFile instanceof String)
        errorMessage = (String) clientFile;
      else
        this.clientFile = clientFile;
    }
  }
  
  class FilesOpenThread implements Runnable, JmolFileReaderInterface {
    //boolean terminated;
    String errorMessage;
    private String[] fullPathNamesInThread;
    private String[] namesAsGivenInThread;
    private String[] fileTypesInThread;
    Object clientFile;
    private Reader[] stringReaders;
    private Hashtable[] htParamsSet;

    FilesOpenThread(String[] name, String[] nameAsGiven, String[] types,
        Reader[] readers) {
      fullPathNamesInThread = name;
      namesAsGivenInThread = nameAsGiven;
      fileTypesInThread = types;
      this.stringReaders = readers;
    }

    public void run() {
      if (stringReaders != null) {
        openStringReaders();
        stringReaders = null;
      } else {
        htParamsSet = new Hashtable[fullPathNamesInThread.length];
        htParamsSet[0] = new Hashtable();
        Object clientFile = modelAdapter.openBufferedReaders(this,
            fullPathNamesInThread, fileTypesInThread, htParamsSet);
        if (clientFile instanceof String)
          errorMessage = (String) clientFile;
        else
          this.clientFile = clientFile;
        if (errorMessage != null)
          Logger.error("file ERROR: " + errorMessage);
      }
    }
    
    private void openStringReaders() {
      Object clientFile = modelAdapter.openBufferedReaders(this,
          fullPathNamesInThread, fileTypesInThread, null);
      if (clientFile == null)
        return; // errorMessage has been set in getBufferedReader(int i);
      if (clientFile instanceof String)
        errorMessage = (String) clientFile;
      else
        this.clientFile = clientFile;
    }

    /**
     * called by SmartJmolAdapter to request another buffered reader,
     * rather than opening all the readers at once.
     * 
     * @param i   the reader index
     * @return    a BufferedReader or null in the case of an error
     * 
     */
    public BufferedReader getBufferedReader(int i) {
      if (stringReaders != null)
        return new BufferedReader(stringReaders[i]);
      String name = fullPathNamesInThread[i];
      String[] subFileList = null;
      Hashtable htParams = htParamsSet[0]; // for now -- just reusing this
      htParams.remove("subFileList");
      if (name.indexOf("|") >= 0)
        name = (subFileList = TextFormat.split(name, "|"))[0];
      Object t = getUnzippedBufferedReaderOrErrorMessageFromName(name, true,
          false, false);
      if (t instanceof ZipInputStream) {
        if (subFileList != null)
          htParams.put("subFileList", subFileList);
        String[] zipDirectory = getZipDirectory(name, true);
        InputStream is = new BufferedInputStream(
            (InputStream) getInputStreamOrErrorMessageFromName(name, false),
            8192);
        t = modelAdapter.openZipFiles(is, name, zipDirectory, htParams, true);
      }
      if (t instanceof BufferedReader)
        return (BufferedReader) t;
      errorMessage = (t == null ? "error opening:" + namesAsGivenInThread[i]
          : (String) t);
      return null;
    }
  }
}

class MonitorInputStream extends FilterInputStream {
  int length;
  int position;
  int markPosition;
  int readEventCount;

  MonitorInputStream(InputStream in, int length) {
    super(in);
    this.length = length;
    this.position = 0;
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

}
