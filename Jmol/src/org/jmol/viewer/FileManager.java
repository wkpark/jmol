/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

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
/****************************************************************
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
  URL appletCodeBase = null;
  String appletProxy = null;

  // for expanding names into full path names
  //private boolean isURL;
  private String nameAsGiven;
  private String fullPathName;
  String fileName;
  private File file;

  private FileOpenThread fileOpenThread;


  FileManager(Viewer viewer, JmolAdapter modelAdapter) {
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
  }

  void openFile(String name) {
    System.out.println("FileManager.openFile(" + name + ")");
    nameAsGiven = name;
    openErrorMessage = fullPathName = fileName = null;
    classifyName(name);
    if (openErrorMessage != null) {
      System.out.println("openErrorMessage=" + openErrorMessage);
      return;
      }
    fileOpenThread = new FileOpenThread(fullPathName, name);
    fileOpenThread.run();
  }

  void openStringInline(String strModel) {
    openErrorMessage = null;
    fullPathName = fileName = "string";
    fileOpenThread = new FileOpenThread(fullPathName,
                                        new StringReader(strModel));
    fileOpenThread.run();
  }

  void openReader(String fullPathName, String name, Reader reader) {
    openErrorMessage = null;
    this.fullPathName = fullPathName;
    fileName = name;
    fileOpenThread = new FileOpenThread(fullPathName, reader);
    fileOpenThread.run();
  }

  String getFileAsString(String name) {
    System.out.println("FileManager.getFileAsString(" + name + ")");
    Object t = getInputStreamOrErrorMessageFromName(name);
    byte[] abMagic = new byte[4];
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream)t, 8192);
      InputStream is = bis;
      bis.mark(5);
      int countRead = 0;
      countRead = bis.read(abMagic, 0, 4);
      bis.reset();
      if (countRead == 4 &&
          abMagic[0] == (byte)0x1F && abMagic[1] == (byte)0x8B)
        is = new GZIPInputStream(bis);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      StringBuffer sb = new StringBuffer(8192);
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
      return "" + sb;
      } catch (IOException ioe) {
        return ioe.getMessage();
      }
  }

  Object waitForClientFileOrErrorMessage() {
    Object clientFile = null;
    if (fileOpenThread != null) {
      clientFile = fileOpenThread.clientFile;
      if (fileOpenThread.errorMessage != null)
        openErrorMessage = fileOpenThread.errorMessage;
      else if (clientFile == null)
        openErrorMessage = "Client file is null loading:" + nameAsGiven;
      fileOpenThread = null;
    }
    if (openErrorMessage != null)
      return openErrorMessage;
    return clientFile;
  }

  String getFullPathName() {
    return fullPathName != null ? fullPathName : nameAsGiven;
  }

  String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  void setAppletContext(URL documentBase, URL codeBase,
                               String jmolAppletProxy) {
    appletDocumentBase = documentBase;
    System.out.println("appletDocumentBase=" + documentBase);
    //    dumpDocumentBase("" + documentBase);
    appletCodeBase = codeBase;
    appletProxy = jmolAppletProxy;
  }

  void dumpDocumentBase(String documentBase) {
    System.out.println("dumpDocumentBase:" + documentBase);
    Object inputStreamOrError =
      getInputStreamOrErrorMessageFromName(documentBase);
    if (inputStreamOrError == null) {
      System.out.println("?Que? ?null?");
    } else if (inputStreamOrError instanceof String) {
      System.out.println("Error:" + inputStreamOrError);
    } else {
      BufferedReader br =
        new BufferedReader(new
                           InputStreamReader((InputStream)inputStreamOrError));
      String line;
      try {
        while ((line = br.readLine()) != null)
          System.out.println(line);
        br.close();
      } catch (Exception ex) {
        System.out.println("exception caught:" + ex);
      }
    }
  }

  // mth jan 2003 -- there must be a better way for me to do this!?
  final String[] urlPrefixes = {"http:", "https:", "ftp:", "file:"};

  private void classifyName(String name) {
    //isURL = false;
    if (name == null)
      return;
    if (appletDocumentBase != null) {
      // This code is only for the applet
      //isURL = true;
      try {
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
        //isURL = true;
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
    //isURL = false;
    file = new File(name);
    fullPathName = file.getAbsolutePath();
    fileName = file.getName();
  }
  
  Object getInputStreamOrErrorMessageFromName(String name) {
    String errorMessage = null;
    int iurlPrefix;
    for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0; )
      if (name.startsWith(urlPrefixes[iurlPrefix]))
        break;
    try {
      InputStream in;
      int length;
      if (appletDocumentBase == null) {
        if (iurlPrefix >= 0) {
          URL url = new URL(name);
          URLConnection conn = url.openConnection();
          length = conn.getContentLength();
          in = conn.getInputStream();
        }
        else {
          File file = new File(name);
          length = (int)file.length();
          in = new FileInputStream(file);
        }
      } else {
        if (iurlPrefix >= 0 && appletProxy != null)
          name = appletProxy + "?url=" + URLEncoder.encode(name, "utf-8");
        URL url = new URL(appletDocumentBase, name);
        URLConnection conn = url.openConnection();
        length = conn.getContentLength();
        in = conn.getInputStream();
      }
      return new MonitorInputStream(in, length);
    } catch (Exception e) {
      errorMessage = "" + e;
    }
    return errorMessage;
  }

  class FileOpenThread implements Runnable {
    boolean terminated;
    String errorMessage;
    String fullPathNameInThread;
    String nameAsGivenInThread;
    Object clientFile;
    Reader reader;

    FileOpenThread(String fullPathName, String nameAsGiven) {
      this.fullPathNameInThread = fullPathName;
      this.nameAsGivenInThread = nameAsGiven;
    }

    FileOpenThread(String name, Reader reader) {
      nameAsGivenInThread = fullPathNameInThread = name;
      this.reader = reader;
    }

    public void run() {
      if (reader != null) {
        openReader(reader);
      } else {
        Object t = getInputStreamOrErrorMessageFromName(nameAsGivenInThread);
        if (! (t instanceof InputStream)) {
          errorMessage = (t == null
                          ? "error opening:" + nameAsGivenInThread
                          : (String)t);
        } else {
          openInputStream(fullPathNameInThread, fileName, (InputStream) t);
        }
      }
      if (errorMessage != null)
        System.out.println("error opening " + fullPathNameInThread + "\n" + errorMessage);
      terminated = true;
    }

    byte[] abMagic = new byte[4];
    private void openInputStream(String fullPathName, String fileName,
                                 InputStream istream) {
      BufferedInputStream bistream = new BufferedInputStream(istream, 8192);
      InputStream istreamToRead = bistream;
      bistream.mark(5);
      int countRead = 0;
      try {
        countRead = bistream.read(abMagic, 0, 4);
        bistream.reset();
        if (countRead == 4) {
          if (abMagic[0] == (byte)0x1F && abMagic[1] == (byte)0x8B) {
            istreamToRead = new GZIPInputStream(bistream);
          }
        }
        openReader(new InputStreamReader(istreamToRead));
      } catch (IOException ioe) {
        errorMessage = ioe.getMessage();
      }
    }

    private void openReader(Reader reader) {
      Object clientFile =
        modelAdapter.openBufferedReader(fullPathNameInThread,
                                        new BufferedReader(reader));
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
    /*
      System.out.println("" + getPercentageRead() + "% " +
      getPosition() + " of " + getLength() + " in " +
      getReadingTimeMillis());
    */
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
