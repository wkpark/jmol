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

import org.jmol.util.BinaryDocument;
import org.jmol.util.CompoundDocument;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;
import org.jmol.util.ZipUtil;

import org.jmol.util.Logger;

import org.jmol.api.JmolFileReaderInterface;
import org.jmol.api.JmolViewer;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
//import java.io.FilterInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.text.DateFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

public class FileManager {

  protected Viewer viewer;

  FileManager(Viewer viewer) {
    this.viewer = viewer;
    clear();
  }

  void clear() {
    setLoadScript("", false);
    fullPathName = fileName = nameAsGiven = "zapped";
  }

  private String loadScript;

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
  
  private String nameAsGiven = "zapped";
  private String fullPathName;
  private String fileName;
  private String inlineData;
  //TODO: We might consider an option to not preserve inlineData in certain cases where
  //      data are huge. The reason to save the data is that we can then use load ""
  //      

  void setFileInfo(String[] fileInfo) {
    // used by ScriptEvaluator dataFrame and load methods to temporarily save the state here
    fullPathName = fileInfo[0];
    fileName = fileInfo[1];
    nameAsGiven = fileInfo[2];
    inlineData = fileInfo[3];
    loadScript = fileInfo[4];
  }

  String[] getFileInfo() {
    // used by ScriptEvaluator dataFrame method
    return new String[] { fullPathName, fileName, nameAsGiven, inlineData, loadScript };
  }

  String getFullPathName() {
    return fullPathName != null ? fullPathName : nameAsGiven;
  }

  String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  String getInlineData(int iData) {
    return (iData < 0 ? inlineData : "");  
  }

  // for applet proxy
  private URL appletDocumentBase = null;
  private URL appletCodeBase = null; //unused currently
  private String appletProxy;

  String getAppletDocumentBase() {
    return (appletDocumentBase == null ? "" : appletDocumentBase.toString());
  }

  void setAppletContext(URL documentBase, URL codeBase, String jmolAppletProxy) {
    appletDocumentBase = documentBase;
    appletCodeBase = codeBase;
    appletProxy = jmolAppletProxy;
    Logger.info("appletDocumentBase=" + appletDocumentBase
        + "\nappletCodeBase=" + appletCodeBase);
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy == null || appletProxy.length() == 0 ? null
        : appletProxy);
  }

  String getState(StringBuffer sfunc) {
    StringBuffer commands = new StringBuffer();
    if (sfunc != null) {
      sfunc.append("  _setFileState;\n");
      commands.append("function _setFileState() {\n\n");
    }
    commands.append(loadScript);
    if (viewer.getModelSetFileName().equals("zapped"))
      commands.append("  zap;\n");
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  String getFileTypeName(String fileName) {
    int pt = fileName.indexOf("::");
    if (pt >= 0)
      return fileName.substring(0, pt);
    Object br = getUnzippedBufferedReaderOrErrorMessageFromName(fileName, true,
        false, true, true);
    if (br instanceof BufferedReader)
      return viewer.getModelAdapter().getFileTypeName((BufferedReader) br);
    if (br instanceof ZipInputStream) {
      String zipDirectory = getZipDirectoryAsString(fileName);
      if (zipDirectory.indexOf("JmolManifest") >= 0)
        return "Jmol";
      return viewer.getModelAdapter().getFileTypeName(
          getBufferedReaderForString(zipDirectory));
    }
    if (br instanceof String[]) {
      return ((String[]) br)[0];
    }
    return null;
  }

  private static BufferedReader getBufferedReaderForString(String string) {
    return new BufferedReader(new StringReader(string));
  }

  private String getZipDirectoryAsString(String fileName) {
    return ZipUtil
        .getZipDirectoryAsStringAndClose((InputStream) getInputStreamOrErrorMessageFromName(
            fileName, false));
  }

  /////////////// createAtomSetCollectionFromXXX methods /////////////////
  
  // where XXX = File, Files, String, Strings, ArrayData, DOM, Reader
  
  /*
   * note -- createAtomSetCollectionFromXXX methods
   * were "openXXX" before refactoring 11/29/2008 -- BH
   * 
   * The problem was that while they did open the file, they
   * (mostly) also closed them, and this was confusing.
   * 
   * The term "clientFile" was replaced by "atomSetCollection"
   * here because that's what it is --- an AtomSetCollection,
   * not a file. The file is closed at this point. What is
   * returned is the atomSetCollection object.
   * 
   * One could say this is just semantics, but there were
   * subtle bugs here, where readers were not always being 
   * closed explicitly. In the process of identifying Out of
   * Memory Errors, I felt it was necessary to clarify all this.
   * 
   * Apologies to those who feel the original clientFile notation
   * was more generalizable or understandable. 
   * 
   */
  Object createAtomSetCollectionFromFile(String name, Hashtable htParams,
                                    String loadScript, boolean isAppend) {
    if (htParams.get("atomDataOnly") == null)
      setLoadScript(loadScript, isAppend);
    int pt = name.indexOf("::");
    nameAsGiven = (pt >= 0 ? name.substring(pt + 2) : name);
    String fileType = (pt >= 0 ? name.substring(0, pt) : null);
    Logger.info("\nFileManager.getAtomSetCollectionFromFile(" + nameAsGiven
        + ")" + (name.equals(nameAsGiven) ? "" : " //" + name));
    fullPathName = null;
    fileName = null;
    String[] names = classifyName(nameAsGiven, true);
    if (names.length == 1)
      return names[0];
    fullPathName = names[0];
    fileName = names[1];
    htParams.put("fullPathName", (fileType == null ? "" : fileType + "::")
        + fullPathName.replace('\\', '/'));
    if (viewer.getMessageStyleChime() && viewer.getDebugScript())
      viewer.scriptStatus("Requesting " + fullPathName);
    FileReader fileReader = new FileReader(fullPathName, nameAsGiven,
        fileType, null, htParams);
    fileReader.run();
    return fileReader.atomSetCollection;
  }

  Object createAtomSetCollectionFromFiles(String[] fileNames,
                                     String loadScript, boolean isAppend,
                                     Hashtable htParams) {
    setLoadScript(loadScript, isAppend);
    String[] fullPathNames = new String[fileNames.length];
    String[] namesAsGiven = new String[fileNames.length];
    String[] fileTypes = new String[fileNames.length];
    for (int i = 0; i < fileNames.length; i++) {
      int pt = fileNames[i].indexOf("::");
      nameAsGiven = (pt >= 0 ? fileNames[i].substring(pt + 2) : fileNames[i]);
      String fileType = (pt >= 0 ? fileNames[i].substring(0, pt) : null);
      String[] names = classifyName(nameAsGiven, true);
      if (names.length == 1)
        return names[0];
      fullPathNames[i] = names[0];
      fileNames[i] = names[0].replace('\\', '/');
      fileTypes[i] = fileType;
      namesAsGiven[i] = nameAsGiven;
    }

    fullPathName = fileName = nameAsGiven = "file[]";
    inlineData = "";
    FilesReader filesReader = new FilesReader(fullPathNames, namesAsGiven,
        fileTypes, null, htParams);
    filesReader.run();
    return filesReader.atomSetCollection;
  }

  Object createAtomSetCollectionFromString(String strModel, Hashtable htParams,
                                      boolean isAppend) {
    String tag = (isAppend ? "append" : "model");
    String script = "data \"" + tag + " inline\"\n" + strModel + "end \"" + tag
        + " inline\";";
    setLoadScript(script, isAppend);
    Logger.info("FileManager.getAtomSetCollectionFromString()");
    fullPathName = fileName = "string";
    inlineData = strModel;
    FileReader fileReader = new FileReader("string", "string", null,
        getBufferedReaderForString(strModel), htParams);
    fileReader.run();
    return fileReader.atomSetCollection;
  }

  Object createAtomSeCollectionFromStrings(String[] arrayModels, Hashtable htParams,
                                      boolean isAppend) {
    String oldSep = "\"" + viewer.getDataSeparator() + "\"";
    String tag = "\"" + (isAppend ? "append" : "model") + " inline\"";
    StringBuffer sb = new StringBuffer("set dataSeparator \"~~~next file~~~\";\ndata ");
    sb.append(tag);
    for (int i = 0; i < arrayModels.length; i++) {
      if (i > 0)
        sb.append("~~~next file~~~");
      sb.append(arrayModels[i]);
    }
    sb.append("end ").append(tag).append(";set dataSeparator ").append(oldSep);
    setLoadScript(sb.toString(), isAppend);
    Logger.info("FileManager.getAtomSetCollectionFromStrings(string[])");
    fullPathName = fileName = "string[]";
    inlineData = "";
    String[] fullPathNames = new String[arrayModels.length];
    StringDataReader[] readers = new StringDataReader[arrayModels.length];
    for (int i = 0; i < arrayModels.length; i++) {
      fullPathNames[i] = "string[" + i + "]";
      readers[i] = new StringDataReader(arrayModels[i]);
    }
    FilesReader filesReader = new FilesReader(fullPathNames, fullPathNames,
        null, readers, null);
    filesReader.run();
    return filesReader.atomSetCollection;
  }

  Object createAtomSeCollectionFromArrayData(Vector arrayData,
                                               Hashtable htParams,
                                               boolean isAppend) {
    // NO STATE SCRIPT -- HERE WE ARE TRYING TO CONSERVE SPACE
    Logger.info("FileManager.getAtomSetCollectionFromArrayData(Vector)");
    fullPathName = fileName = "String[]";
    inlineData = "";
    int nModels = arrayData.size();
    String[] fullPathNames = new String[nModels];
    DataReader[] readers = new DataReader[nModels];
    for (int i = 0; i < nModels; i++) {
      fullPathNames[i] = "String[" + i + "]";
      Object data = arrayData.get(i);
      if (data instanceof String)
        readers[i] = new StringDataReader((String) arrayData.get(i));
      else if (data instanceof String[])
        readers[i] = new ArrayDataReader((String[]) arrayData.get(i));
      else if (data instanceof Vector)
        readers[i] = new VectorDataReader((Vector) arrayData.get(i));
    }
    FilesReader filesReader = new FilesReader(fullPathNames, fullPathNames,
        null, readers, null);
    filesReader.run();
    return filesReader.atomSetCollection;
  }

  Object createAtomSetCollectionFromDOM(Object DOMNode, Hashtable htParams) {
    inlineData = "";
    fullPathName = fileName = "JSNode";
    DOMReader aDOMReader = new DOMReader(DOMNode, htParams);
    aDOMReader.run();
    return aDOMReader.atomSetCollection;
  }

  /**
   * not used in Jmol project -- will close reader
   * 
   * @param fullPathName
   * @param name
   * @param reader
   * @param htParams 
   * @return fileData
   */
  Object createAtomSetCollectionFromReader(String fullPathName, String name,
                                      Reader reader, Hashtable htParams) {
    this.fullPathName = fullPathName;
    fileName = name;
    FileReader fileReader = new FileReader(fullPathName, fullPathName,
        null, new BufferedReader(reader), htParams);
    fileReader.run();
    return fileReader.atomSetCollection;
  }

  /////////////// generally useful file I/O methods /////////////////
  
  // mostly internal to FileManager and its enclosed classes
  
  BufferedInputStream getBufferedInputStream(String fullPathName) {
    Object ret = getBufferedReaderOrErrorMessageFromName(fullPathName,
        new String[2], true, true);
    return (ret instanceof BufferedInputStream ? (BufferedInputStream) ret
        : null);
  }

  Object getInputStreamOrErrorMessageFromName(String name, boolean showMsg) {
    String errorMessage = null;
    int iurlPrefix;
    for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0;)
      if (name.startsWith(urlPrefixes[iurlPrefix]))
        break;
    boolean isURL = (iurlPrefix >= 0);
    boolean isApplet = (appletDocumentBase != null);
    InputStream in = null;
    //int length;
    try {
      if (isApplet || isURL) {
        if (isApplet && isURL && appletProxy != null)
          name = appletProxy + "?url=" + URLEncoder.encode(name, "utf-8");
        URL url = (isApplet ? new URL(appletDocumentBase, name) : new URL(name));
        name = url.toString();
        if (showMsg)
          Logger.info("FileManager opening " + url.toString());
        URLConnection conn = url.openConnection();
        //length = conn.getContentLength();
        in = conn.getInputStream();
      } else {
        if (showMsg)
          Logger.info("FileManager opening " + name);
        File file = new File(name);
        //length = (int) file.length();
        in = new FileInputStream(file);
      }
      return in;
    } catch (Exception e) {
      try {
        if (in != null)
          in.close();
      } catch (IOException e1) {
      }
      errorMessage = "" + e;
    }
    return errorMessage;
  }

  Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary, boolean doSpecialLoad) {
    String[] names = classifyName(name, true);
    if (names == null)
      return "cannot read file name: " + name;
    if (fullPathNameReturn != null)
      fullPathNameReturn[0] = names[0].replace('\\', '/');
    return getUnzippedBufferedReaderOrErrorMessageFromName(names[0], false,
        isBinary, false, doSpecialLoad);
  }

  Object getUnzippedBufferedReaderOrErrorMessageFromName(
                                                         String name,
                                                         boolean allowZipStream,
                                                         boolean asInputStream,
                                                         boolean isTypeCheckOnly,
                                                         boolean doSpecialLoad) {
    String[] subFileList = null;
    String[] info = (doSpecialLoad ? viewer.getModelAdapter().specialLoad(name,
        "filesNeeded?") : null);
    if (info != null) {
      if (isTypeCheckOnly)
        return info;
      if (info[2] != null) {
        String header = info[1];
        Hashtable fileData = new Hashtable();
        if (info.length == 3) {
          // we need information from the output file, info[2]
          String name0 = getObjectAsSections(info[2], header, fileData);
          fileData.put("OUTPUT", name0);
          info = viewer.getModelAdapter().specialLoad(name,
              (String) fileData.get(name0));
          if (info.length == 3) {
            // might have a second option
            name0 = getObjectAsSections(info[2], header, fileData);
            fileData.put("OUTPUT", name0);
            info = viewer.getModelAdapter().specialLoad(info[1],
                (String) fileData.get(name0));
          }
        }
        // load each file individually, but return files IN ORDER
        StringBuffer sb = new StringBuffer();
        sb.append(fileData.get(fileData.get("OUTPUT")));
        for (int i = 2; i < info.length; i++) {
          name = info[i];
          name = getObjectAsSections(name, header, fileData);
          Logger.info("reading " + name);
          String s = (String) fileData.get(name);
          sb.append(s);
        }
        return getBufferedReaderForString(sb.toString());
      }
      // continuing...
      // here, for example, for an SPT file load that is not just a type check
      // (type check is only for application file opening and drag-drop to
      // determine if
      // script or load command should be used)
    }
    if (name.indexOf("|") >= 0)
      name = (subFileList = TextFormat.split(name, "|"))[0];
    Object t = getInputStreamOrErrorMessageFromName(name, true);
    if (t instanceof String)
      return t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
      InputStream is = bis;
      if (CompoundDocument.isCompoundDocument(is)) {
        CompoundDocument doc = new CompoundDocument(bis);
        return getBufferedReaderForString(doc.getAllData("Molecule").toString());
      } else if (ZipUtil.isGzip(is)) {
        do {
          is = new BufferedInputStream(new GZIPInputStream(is));
        } while (ZipUtil.isGzip(is));
      } else if (ZipUtil.isZipFile(is)) {
        if (allowZipStream)
          return new ZipInputStream(bis);
        if (asInputStream)
          return (InputStream) ZipUtil.getZipFileContents(is, subFileList, 1,
              true);
        // danger -- converting bytes to String here.
        // we lose 128-156 or so.
        String s = (String) ZipUtil.getZipFileContents(is, subFileList, 1,
            false);
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
    return ZipUtil.getZipDirectoryAndClose(
        (InputStream) getInputStreamOrErrorMessageFromName(fileName, false),
        addManifest);
  }

  /**
   * delivers file contents and directory listing for a ZIP/JAR file into sb
   * 
   * 
   * @param name
   * @param header
   * @param fileData
   * @return name of entry
   */
  private String getObjectAsSections(String name, String header,
                                     Hashtable fileData) {
    if (name == null)
      return null;
    String[] subFileList = null;
    boolean asBinaryString = false;
    String name0 = name.replace('\\', '/');
    if (name.indexOf(":asBinaryString") >= 0) {
      asBinaryString = true;
      name = name.substring(0, name.indexOf(":asBinaryString"));
    }
    StringBuffer sb = null;
    if (fileData.containsKey(name0))
      return name0;
    if (name.indexOf("#JMOL_MODEL ") >= 0) {
      fileData.put(name0, name0 + "\n");
      return name0;
    }
    if (name.indexOf("|") >= 0) {
      name = (subFileList = TextFormat.split(name, "|"))[0];
    }
    BufferedInputStream bis = null;
    try {
      Object t = getInputStreamOrErrorMessageFromName(name, false);
      if (t instanceof String) {
        fileData.put(name0, (String) t + "\n");
        return name0;
      }
      bis = new BufferedInputStream((InputStream) t, 8192);
      if (CompoundDocument.isCompoundDocument(bis)) {
        CompoundDocument doc = new CompoundDocument(bis);
        doc.getAllData(name.replace('\\', '/'), "Molecule", fileData);
      } else if (ZipUtil.isZipFile(bis)) {
        ZipUtil.getAllData(bis, subFileList, name.replace('\\', '/'), "Molecule", fileData);
      } else if (asBinaryString) {
        // used for Spartan binary file reading
        BinaryDocument bd = new BinaryDocument();
        bd.setStream(bis, false);
        sb = new StringBuffer();
        //note -- these headers must match those in ZipUtil.getAllData and CompoundDocument.getAllData
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        try {
          while (true)
            sb.append(Integer.toHexString(((int) bd.readByte()) & 0xFF))
                .append(' ');
        } catch (Exception e1) {
          sb.append('\n');
        }
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      } else {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            ZipUtil.isGzip(bis) ? new GZIPInputStream(bis) : (InputStream) bis));
        String line;
        sb = new StringBuffer();
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        while ((line = br.readLine()) != null) {
          sb.append(line);
          sb.append('\n');
        }
        br.close();
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      }
    } catch (Exception ioe) {
      fileData.put(name0, ioe.getMessage());
    }
    if (bis != null)
      try {
        bis.close();
      } catch (Exception e) {
        //
      }
    if (!fileData.containsKey(name0))
      fileData.put(name0, "FILE NOT FOUND: " + name0 + "\n");
    return name0;
  }

  Object getFileAsBytes(String name) {
    //?? used by eval of "WRITE FILE"
    // will be full path name
    if (name == null)
      return null;
    String[] subFileList = null;
    if (name.indexOf("|") >= 0)
      name = (subFileList = TextFormat.split(name, "|"))[0];
    Object t = getInputStreamOrErrorMessageFromName(name, false);
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
      InputStream is = bis;      
      Object bytes = (ZipUtil.isZipFile(is) && subFileList != null
          && 1 < subFileList.length ? ZipUtil.getZipFileContentsAsBytes(is,
          subFileList, 1) : ZipUtil.getStreamAsBytes(bis));
      is.close();
      return bytes;
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  /**
   * 
   * @param data
   *          [0] initially path name, but returned as full path name; [1]file
   *          contents (directory listing for a ZIP/JAR file) or error string
   * @param nBytesMax
   * @param doSpecialLoad 
   * @return true if successful; false on error
   */

  boolean getFileDataOrErrorAsString(String[] data, int nBytesMax, boolean doSpecialLoad) {
    data[1] = "";
    String name = data[0];
    if (name == null)
      return false;
    Object t = getBufferedReaderOrErrorMessageFromName(name, data, false, doSpecialLoad);
    if (t instanceof String) {
      data[1] = (String) t;
      return false;
    }
    try {
      BufferedReader br = (BufferedReader) t;
      StringBuffer sb = new StringBuffer(8192);
      String line;
      if (nBytesMax == Integer.MAX_VALUE) {
        while ((line = br.readLine()) != null)
          sb.append(line).append('\n');
      } else {
        int n = 0;
        int len;
        while (n < nBytesMax && (line = br.readLine()) != null) {
          if (nBytesMax - n < (len = line.length()) + 1)
            line = line.substring(0, nBytesMax - n - 1);
          sb.append(line).append('\n');
          n += len + 1;
        }
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
    String[] names = classifyName(name, true);
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
      MediaTracker mediaTracker = new MediaTracker(viewer.getDisplay());
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

  private final static int URL_LOCAL = 3;
  private final static String[] urlPrefixes = { "http:", "https:", "ftp:",
      "file:" };

  private static int urlTypeIndex(String name) {
    for (int i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i])) {
        return i;
      }
    }
    return -1;
  }

  /**
   * 
   * @param name
   * @param isFullLoad
   * @return [0] full path name, [1] file name without path, [2] full URL
   */
  private String[] classifyName(String name, boolean isFullLoad) {
    if (name == null)
      return new String[] { null };
    if (name.startsWith("?")
        && (name = viewer.dialogAsk("load", name.substring(1))) == null) {
      return new String[] { isFullLoad ? "#CANCELED#" : null };
    }
    File file = null;
    URL url = null;
    String[] names = null;
    if (name.indexOf("=") == 0)
      name = TextFormat.formatString(viewer.getLoadFormat(), "FILE", name
          .substring(1));
    if (name.indexOf(":") < 0 && name.indexOf("/") != 0)
      name = addDirectory(viewer.getDefaultDirectory(), name);
    if (appletDocumentBase != null) {
      // This code is only for the applet
      try {
        if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
          name = "file:/" + name;
        else if (name.indexOf("/") == 0
            && viewer.getBooleanProperty("_signedApplet"))
          name = "file:" + name;
        url = new URL(appletDocumentBase, name);
      } catch (MalformedURLException e) {
        return new String[] { isFullLoad ? e.getMessage() : null };
      }
    } else {
      // This code is for the app
      if (urlTypeIndex(name) >= 0) {
        try {
          url = new URL(name);
        } catch (MalformedURLException e) {
          return new String[] { isFullLoad ? e.getMessage() : null };
        }
      } else {
        file = new File(name);
        names = new String[] { file.getAbsolutePath(), file.getName(),
            "file:/" + file.getAbsolutePath().replace('\\', '/') };
      }
    }
    if (url != null) {
      names = new String[3];
      names[0] = names[2] = url.toString();
      names[1] = names[0].substring(names[0].lastIndexOf('/') + 1);
    }
    if (isFullLoad && (file != null || urlTypeIndex(names[0]) == URL_LOCAL)) {
      String path = (file == null ? TextFormat.trim(names[0].substring(5), "/")
          : names[0]);
      int pt = path.length() - names[1].length() - 1;
      if (pt > 0) {
        path = path.substring(0, pt);
        setLocalPath(viewer, path, true);
      }
    }
    return names;
  }

  private static String addDirectory(String defaultDirectory, String name) {
    if (defaultDirectory.length() == 0)
      return name;
    char ch = (name.length() > 0 ? name.charAt(0) : ' ');
    String s = defaultDirectory.toLowerCase();
    if ((s.endsWith(".zip") || s.endsWith(".tar")) && ch != '|' && ch != '/')
      defaultDirectory += "|";
    return defaultDirectory
        + (ch == '/'
            || ch == '/'
            || (ch = defaultDirectory.charAt(defaultDirectory.length() - 1)) == '|'
            || ch == '/' ? "" : "/") + name;
  }

  String getDefaultDirectory(String name) {
    String[] names = classifyName(name, true);
    if (names == null)
      return "";
    name = fixPath(names[0]);
    return (names == null ? "" : name.substring(0, name.lastIndexOf("/")));
  }

  private static String fixPath(String path) {
    path = path.replace('\\', '/');
    path = TextFormat.simpleReplace(path, "/./", "/");
    int pt = path.lastIndexOf("//") + 1;
    if (pt < 1)
      pt = path.indexOf(":/") + 1;
    if (pt < 1)
      pt = path.indexOf("/");
    String protocol = path.substring(0, pt);
    path = path.substring(pt);

    while ((pt = path.lastIndexOf("/../")) >= 0) {
      int pt0 = path.substring(0, pt).lastIndexOf("/");
      if (pt0 < 0)
        return TextFormat.simpleReplace(protocol + path, "/../", "/");
      path = path.substring(0, pt0) + path.substring(pt + 3);
    }
    if (path.length() == 0)
      path = "/";
    return protocol + path;
  }

  String getFullPath(String name, boolean addUrlPrefix) {
    String[] names = classifyName(name, false);
    return (names == null ? "" : addUrlPrefix ? names[2] : names[0].replace(
        '\\', '/'));
  }

  private final static String[] urlPrefixPairs = { "http:", "http://", "www.",
      "http://www.", "https:", "https://", "ftp:", "ftp://", "file:",
      "file:///" };

  public static String getLocalUrl(File file) {
    // entering a url on a file input box will be accepted,
    // but cause an error later. We can fix that...
    // return null if there is no problem, the real url if there is
    if (file.getName().startsWith("="))
      return file.getName();
    String path = file.getAbsolutePath().replace('\\', '/');
    for (int i = 0; i < urlPrefixPairs.length; i++)
      if (path.indexOf(urlPrefixPairs[i]) == 0)
        return null;
    // looking for /xxx/xxxx/file://...
    for (int i = 0; i < urlPrefixPairs.length; i += 2)
      if (path.indexOf(urlPrefixPairs[i]) > 0)
        return urlPrefixPairs[i + 1]
            + TextFormat.trim(path.substring(path.indexOf(urlPrefixPairs[i])
                + urlPrefixPairs[i].length()), "/");
    return null;
  }

  public static File getLocalDirectory(JmolViewer viewer, boolean forDialog) {
    String localDir = (String) viewer
        .getParameter(forDialog ? "currentLocalPath" : "defaultDirectoryLocal");
    if (localDir.length() == 0 && forDialog)
      localDir = (String) viewer.getParameter("defaultDirectoryLocal");
    if (localDir.length() == 0)
      return (viewer.isApplet() ? null : new File(System.getProperty("user.dir")));
    if (viewer.isApplet() && localDir.indexOf("file:/") == 0)
        localDir = getLocalPathForWritingFile(viewer, localDir);
    
    File f = new File(localDir);
    return f.isDirectory() ? f : f.getParentFile();
  }

  public static void setLocalPath(JmolViewer viewer, String path,
                                  boolean forDialog) {
    while (path.endsWith("/") || path.endsWith("\\"))
      path = path.substring(0, path.length() - 1);
    viewer.setStringProperty("currentLocalPath", path);
    if (!forDialog)
      viewer.setStringProperty("defaultDirectoryLocal", path);
  }

  public static String getLocalPathForWritingFile(JmolViewer viewer, String file) {
    if (file.indexOf("file:/") == 0)
      return file.substring(6);
    if (file.indexOf("/") == 0 || file.indexOf(":") >= 0)
      return file;
    File dir = getLocalDirectory(viewer, false);
    return (dir == null ? file : fixPath(dir.toString() + "/" + file));
  }

  /**
   * Sets all local file references in a script file to point to files within
   * dataPath. If a file reference contains dataPath, then the file reference is
   * left with that RELATIVE path. Otherwise, it is changed to a relative file
   * name within that dataPath. 
   * 
   * Only file references starting with "file://" are changed.
   * 
   * @param script
   * @param dataPath
   * @param isLocal 
   * @return revised script
   */
  public static String setScriptFileReferences(String script, String dataPath, boolean isLocal) {
    if (dataPath == null)
      return script;
    if (dataPath.equals("."))
      dataPath = "";
    boolean noPath = (dataPath.length() == 0);
    Vector fileNames = new Vector();
    getFileReferences(script, fileNames);
    Vector newFileNames = new Vector();
    int nFiles = fileNames.size();
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name = (String) fileNames.get(iFile);
      int itype = urlTypeIndex(name);
      if (isLocal == (itype < 0 || itype == URL_LOCAL)) {
        int pt = (noPath ? -1 : name.indexOf("/" + dataPath + "/"));
        if (pt >= 0) {
          name = name.substring(pt + 1);
        } else {
          pt = name.lastIndexOf("/");
          if (pt < 0 && !noPath)
            name = "/" + name;
          if (pt < 0 || noPath)
            pt++;
          name = dataPath + name.substring(pt);
        }
      }
      newFileNames.add(name);
    }
    return TextFormat.replaceQuotedStrings(script, fileNames, newFileNames);
  }

  private static String[] scriptFilePrefixes = new String[] { "/*file*/", "FILE0=", "FILE1=" };
  public static void getFileReferences(String script, Vector fileList) {
    for (int ipt = 0; ipt < scriptFilePrefixes.length; ipt++) {
      String tag = scriptFilePrefixes[ipt];
      int i = -1;
      while ((i = script.indexOf(tag, i + 1)) >= 0)
        fileList.add(Parser.getNextQuotedString(script, i));
    }
  }

  String createZipSet(String fileName, String script, boolean includeRemoteFiles) {
    Vector v = new Vector();
    Vector fileNames = new Vector();
    getFileReferences(script, fileNames);
    Vector newFileNames = new Vector();
    int nFiles = fileNames.size();
    fileName = fileName.replace('\\', '/');
    String fileRoot = fileName.substring(fileName.lastIndexOf("/") + 1);
    if (fileRoot.indexOf(".") >= 0)
      fileRoot = fileRoot.substring(0, fileRoot.indexOf("."));
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name = (String) fileNames.get(iFile);
      int itype = urlTypeIndex(name);
      boolean isLocal = (itype < 0 || itype == URL_LOCAL);
      if (isLocal || includeRemoteFiles) {
        v.add(name);
        String newName = "$SCRIPT_PATH$/" + name.substring(name.lastIndexOf("/") + 1);
        if (isLocal && name.indexOf("|") < 0) {
          v.add(null);
        } else {
          Object ret = getFileAsBytes(name);
          if (!(ret instanceof byte[]))
            return (String) ret;
          v.add(ret);
        }
        name = newName;
      }
      newFileNames.add(name);
    }
    String sname = fileRoot + ".spt";
    v.add("JmolManifest.txt");
    String sinfo = "# Jmol Mmanifest Zip Format 1.0\n"
      + "# Created "
      + DateFormat.getDateInstance().format(new Date()) + "\n"
      + "# JmolVersion " + Viewer.getJmolVersion() + "\n"
      + sname;
    v.add(sinfo.getBytes());
    script = TextFormat.replaceQuotedStrings(script, fileNames, newFileNames);
    v.add(sname);
    v.add(script.getBytes());
    Object bytes = viewer.getImageAs("JPEG", -1, -1, -1, null, null, 
        JmolConstants.embedScript(script));
    if (bytes instanceof byte[]) {
      v.add(fileRoot + ".jpg");
      v.add((byte[]) bytes);
    }
    return writeZipFile(fileName, v, false, "OK JMOL");
  }

  /**
   * generic method to create a zip file based on
   * http://www.exampledepot.com/egs/java.util.zip/CreateZip.html
   * 
   * @param outFileName
   * @param fileNamesAndByteArrays
   *          Vector of [filename1, bytes|null, filename2, bytes|null, ...]
   * @param preservePath
   * @param msg 
   * @return msg bytes filename or errorMessage
   */
  private static String writeZipFile(String outFileName,
                                    Vector fileNamesAndByteArrays,
                                    boolean preservePath, String msg) {
    byte[] buf = new byte[1024];
    long nBytesOut = 0;
    long nBytes = 0;
    Logger.info("creating zip file " + outFileName + "...");
    String fullFilePath = null;
    try {
      ZipOutputStream os = new ZipOutputStream(
          new FileOutputStream(outFileName));
      for (int i = 0; i < fileNamesAndByteArrays.size(); i += 2) {
        String fname = (String) fileNamesAndByteArrays.get(i);
        if (fname.indexOf("file:/") == 0)
          fname = fname.substring(6);
        byte[] bytes = (byte[]) fileNamesAndByteArrays.get(i + 1);
        String fnameShort = fname;
        if (!preservePath || fname.indexOf("|") >= 0) {
          int pt = Math.max(fname.lastIndexOf("|"), fname.lastIndexOf("/"));
          fnameShort = fnameShort.substring(pt + 1);
        }
        Logger.info("...adding " + fname);
        os.putNextEntry(new ZipEntry(fnameShort));
        if (bytes == null) {
          // get data from disk
          FileInputStream in = new FileInputStream(fname);
          int len;
          while ((len = in.read(buf)) > 0) {
            os.write(buf, 0, len);
            nBytesOut += len;
          }
          in.close();
        } else {
          // data are already in byte form
          os.write(bytes, 0, bytes.length);
          nBytesOut += bytes.length;
        }
        os.closeEntry();
      }
      os.close();
      File f = new File(outFileName);
      fullFilePath = f.getAbsolutePath().replace('\\','/');
      nBytes = f.length();
    } catch (IOException e) {
      Logger.info(e.getMessage());
      return e.getMessage();
    }
    Logger.info(nBytesOut + " bytes prior to compression");
    return msg + " " + nBytes + " " + fullFilePath;
  }

  ////////////////// reader classes -- DOM, File, and Files /////////////

  private class DOMReader {
    private Object aDOMNode;
    Object atomSetCollection;
    Hashtable htParams;

    DOMReader(Object DOMNode, Hashtable htParams) {
      this.aDOMNode = DOMNode;
      this.htParams = htParams;
    }

    void run() {
      atomSetCollection = viewer.getModelAdapter().getAtomSetCollectionFromDOM(
          aDOMNode, htParams);
    }
  }

  private class FileReader {
    private String fullPathNameIn;
    private String nameAsGivenIn;
    private String fileTypeIn;
    Object atomSetCollection;
    private BufferedReader reader;
    private Hashtable htParams;

    FileReader(String name, String nameAsGiven, String type,
        BufferedReader reader, Hashtable htParams) {
      fullPathNameIn = name;
      nameAsGivenIn = nameAsGiven;
      fileTypeIn = type;
      this.reader = reader;
      this.htParams = htParams;
    }

    void run() {
      String errorMessage = null;
      if (reader == null) {
        String name = fullPathNameIn;
        String[] subFileList = null;
        Object t = getUnzippedBufferedReaderOrErrorMessageFromName(name, true,
            false, false, true);
        if (name.indexOf("|") >= 0)
          name = (subFileList = TextFormat.split(name, "|"))[0];
        if (t instanceof BufferedReader) {
          reader = (BufferedReader) t;
        } else if (t instanceof ZipInputStream) {
          if (subFileList != null)
            htParams.put("subFileList", subFileList);
          ZipInputStream zis = (ZipInputStream) t;
          String[] zipDirectory = getZipDirectory(name, true);
          atomSetCollection = viewer.getModelAdapter()
              .getAtomSetCollectionOrBufferedReaderFromZip(zis, name,
                  zipDirectory, htParams, false);
          try {
            zis.close();
          } catch (Exception e) {
            //
          }
        } else {
          errorMessage = (t == null ? "error opening:" + nameAsGivenIn
              : (String) t);
        }
      }
      if (reader != null)
        atomSetCollection = viewer.getModelAdapter()
            .getAtomSetCollectionFromReader(fullPathNameIn, fileTypeIn, reader,
                htParams);
      if (errorMessage != null) {
        if (!errorMessage.startsWith("NOTE:"))
          Logger.error("file ERROR: " + fullPathNameIn + "\n" + errorMessage);
        atomSetCollection = errorMessage;
      }
    }
  }

  /**
   * open a set of models residing in different files
   * 
   */
  private class FilesReader implements JmolFileReaderInterface {
    private String[] fullPathNamesIn;
    private String[] namesAsGivenIn;
    private String[] fileTypesIn;
    Object atomSetCollection;
    private DataReader[] stringReaders;
    private Hashtable[] htParamsSet;
    private Hashtable htParams;

    FilesReader(String[] name, String[] nameAsGiven, String[] types,
        DataReader[] readers, Hashtable htParams) {
      fullPathNamesIn = name;
      namesAsGivenIn = nameAsGiven;
      fileTypesIn = types;
      stringReaders = readers;
      this.htParams = htParams;
    }

    void run() {
      if (stringReaders != null) {
        atomSetCollection = viewer.getModelAdapter()
            .getAtomSetCollectionFromReaders(this, fullPathNamesIn,
                fileTypesIn, null);
        stringReaders = null;
      } else {
        htParamsSet = new Hashtable[fullPathNamesIn.length];
        for (int i = 0; i < htParamsSet.length; i++)
          htParamsSet[i] = htParams; // for now, just one common parameter set
        atomSetCollection = viewer.getModelAdapter()
            .getAtomSetCollectionFromReaders(this, fullPathNamesIn,
                fileTypesIn, htParamsSet);
      }
      if (atomSetCollection instanceof String)
        Logger.error("file ERROR: " + atomSetCollection);
    }

    /**
     * called by SmartJmolAdapter to request another buffered reader,
     * rather than opening all the readers at once.
     * 
     * @param i   the reader index
     * @return    a BufferedReader or null in the case of an error
     * 
     */
    public Object getBufferedReader(int i) {
      if (stringReaders != null)
        return stringReaders[i].getBufferedReader();
      String name = fullPathNamesIn[i];
      String[] subFileList = null;
      Hashtable htParams = htParamsSet[0]; // for now -- just reusing this
      htParams.remove("subFileList");
      if (name.indexOf("|") >= 0)
        name = (subFileList = TextFormat.split(name, "|"))[0];
      Object t = getUnzippedBufferedReaderOrErrorMessageFromName(name, true,
          false, false, true);
      if (t instanceof ZipInputStream) {
        if (subFileList != null)
          htParams.put("subFileList", subFileList);
        String[] zipDirectory = getZipDirectory(name, true);
        InputStream is = new BufferedInputStream(
            (InputStream) getInputStreamOrErrorMessageFromName(name, false),
            8192);
        t = viewer.getModelAdapter()
            .getAtomSetCollectionOrBufferedReaderFromZip(is, name,
                zipDirectory, htParams, true);
      }
      if (t instanceof BufferedReader)
        return (BufferedReader) t;
      return (t == null ? "error opening:" + namesAsGivenIn[i]
          : (String) t);
    }
  }
  
  /**
   * Just a simple abstract class to join a String reader and a String[]
   * reader under the same BufferedReader umbrella.
   * 
   * Subclassed as StringDataReader, ArrayDataReader, and VectorDataReader
   * 
   */

  abstract class DataReader extends BufferedReader {

    DataReader(Reader in) {
      super(in);
    }

    BufferedReader getBufferedReader() {
      return this;
    }  

    protected int readBuf(char[] buf) throws IOException {
      // not used by StringDataReader
      int nRead = 0;
      String line = readLine();
      if (line == null)
        return 0;
      int linept = 0;
      int linelen = (line == null ? -1 : line.length());
      for (int i = 0; i < buf.length && linelen >= 0; i++) {
          if (linept >= linelen) {
            linept = 0;
            buf[i] = '\n';
            line = readLine();
            linelen = (line == null ? -1 : line.length());
          } else {
            buf[i] = line.charAt(linept++);
          }
          nRead++;
      }
      return nRead;
    }
  }

  /**
   * 
   * ArrayDataReader subclasses BufferedReader and overrides its
   * read, readLine, mark, and reset methods so that JmolAdapter 
   * works with String[] arrays without any further adaptation. 
   * 
   */

  class ArrayDataReader extends DataReader {
    private String[] data;
    private int pt;
    private int len;
    
    ArrayDataReader(String[] data) {
      super(new StringReader(""));
      this.data = data;
      len = data.length;
    }

    public int read(char[] buf) throws IOException {
      return readBuf(buf);
    }
      
    public String readLine() {
      return (pt < len ? data[pt++] : null);
    }
    
    int ptMark;
    public void mark(long ptr) {
      //ignore ptr.
      ptMark = pt;
    }
    
    public void reset() {
      pt = ptMark;
    }
  }

  class StringDataReader extends DataReader {

    StringDataReader(String data) {
      super(new StringReader(data));
    }
  }

  /**
   * 
   * VectorDataReader subclasses BufferedReader and overrides its
   * read, readLine, mark, and reset methods so that JmolAdapter 
   * works with Vector<String> arrays without any further adaptation. 
   * 
   */

  class VectorDataReader extends DataReader {
    private Vector data;
    private int pt;
    private int len;
    
    VectorDataReader(Vector data) {
      super(new StringReader(""));
      this.data = data;
      len = data.size();
    }

    public int read(char[] buf) throws IOException {
      return readBuf(buf);
    }
    
    public String readLine() {
      return (pt < len ? (String) data.get(pt++) : null);
    }
    
    int ptMark;
    public void mark(long ptr) {
      //ignore ptr.
      ptMark = pt;
    }
    
    public void reset() {
      pt = ptMark;
    }
  }

  /*
  private class MonitorInputStream extends FilterInputStream {
    private long length;
    private int position;
    private int markPosition;

    MonitorInputStream(InputStream in, long length) {
      super(in);
      this.length = length;
    }

    public int read() throws IOException {
      int nextByte = super.read();
      if (nextByte >= 0)
        ++position;
      return nextByte;
    }

    public int read(byte[] b) throws IOException {
      int cb = super.read(b);
      if (cb > 0)
        position += cb;
      return cb;
    }

    public int read(byte[] b, int off, int len) throws IOException {
      int cb = super.read(b, off, len);
      if (cb > 0)
        position += cb;
      return cb;
    }

    public long skip(long n) throws IOException {
      long cb = super.skip(n);
      // this will only work in relatively small files ... 2Gb
      position += cb;
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

    long getLength() {
      return length;
    }

    int getPercentageRead() {
      return (int) (position * 100 / length;
    }
  }
  */
}

