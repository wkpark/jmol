/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.*;
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
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.util.zip.GZIPInputStream;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.ReaderFactory;

public class FileManager {

  JmolViewer viewer;
  private String openErrorMessage;

  // for applet proxy
  URL appletDocumentBase = null;
  URL appletCodeBase = null;
  String appletProxy = null;

  // for expanding names into full path names
  private boolean isURL;
  private String nameAsGiven;
  private String fullPathName;
  private String fileName;
  private File file;

  private FileOpenThread fileOpenThread;


  public FileManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public void openFile(String name) {
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

  public void openStringInline(String strModel) {
    openErrorMessage = null;
    fullPathName = fileName = "string";
    fileOpenThread = new FileOpenThread(fullPathName,
                                        new StringReader(strModel));
    fileOpenThread.run();
  }

  public void openReader(String fullPathName, String name, Reader reader) {
    openErrorMessage = null;
    fullPathName = fullPathName;
    fileName = name;
    fileOpenThread = new FileOpenThread(fullPathName, reader);
    fileOpenThread.run();
  }

  public Object waitForClientFileOrErrorMessage() {
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

  public String getFullPathName() {
    return fullPathName != null ? fullPathName : nameAsGiven;
  }

  public String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  public void setAppletContext(URL documentBase, URL codeBase,
                               String jmolAppletProxy) {
    appletDocumentBase = documentBase;
    appletCodeBase = codeBase;
    appletProxy = jmolAppletProxy;
  }

  // mth jan 2003 -- there must be a better way for me to do this!?
  final String[] urlPrefixes = {"http:", "https:", "ftp:", "file:"};

  private void classifyName(String name) {
    isURL = false;
    if (name == null)
      return;
    if (appletDocumentBase != null) {
      // This code is only for the applet
      isURL = true;
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
        isURL = true;
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
    isURL = false;
    file = new File(name);
    fullPathName = file.getAbsolutePath();
    fileName = file.getName();
  }

  public Object getInputStreamOrErrorMessageFromName(String name) {
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
          name = appletProxy + "?url=" + URLEncoder.encode(name);
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
    String fullPathName;
    String nameAsGiven;
    Object clientFile;
    Reader reader;

    FileOpenThread(String fullPathName, String nameAsGiven) {
      this.fullPathName = fullPathName;
      this.nameAsGiven = nameAsGiven;
    }

    FileOpenThread(String name, Reader reader) {
      nameAsGiven = fullPathName = name;
      this.reader = reader;
    }

    public void run() {
      if (reader != null) {
        openReader(reader);
      } else {
        Object t = getInputStreamOrErrorMessageFromName(nameAsGiven);
        if (! (t instanceof InputStream)) {
          errorMessage = (t == null
                          ? "error opening:" + nameAsGiven
                          : (String)t);
        } else {
          openInputStream(fullPathName, fileName, (InputStream) t);
        }
      }
      if (errorMessage != null)
        System.out.println("error opening " + fullPathName + "\n" + errorMessage);
      terminated = true;
    }

    byte[] abMagic = new byte[4];
    private void openInputStream(String fullPathName, String fileName,
                                 InputStream istream) {
      System.out.println("FileOpenThread.openInputStream(" + fullPathName + ")");
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
      Object clientFile = viewer.getJmolModelAdapter()
        .openBufferedReader(viewer, fullPathName, new BufferedReader(reader));
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
