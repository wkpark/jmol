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

import org.openscience.jmol.viewer.JmolViewer;
import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.BufferedInputStream;
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

  public FileManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  URL appletDocumentBase = null;
  URL appletCodeBase = null;
  String appletProxy = null;

  public void setAppletContext(URL documentBase, URL codeBase,
                               String jmolAppletProxy) {
    appletDocumentBase = documentBase;
    appletCodeBase = codeBase;
    appletProxy = jmolAppletProxy;
  }

  // mth jan 2003 -- there must be a better way for me to do this!?
  final String[] urlPrefixes = {"http:", "https:", "ftp:", "file:"};

  public URL getURLFromName(String name) {
    URL url = null;
    int i;
    for (i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i]))
        break;
    }
    try {
      if (appletDocumentBase != null) {
        // we are running as an applet
        System.out.println("an applet will try to open the URL:" + name);
        if (i < urlPrefixes.length)
          if (appletProxy != null)
            name = appletProxy + "?url=" + URLEncoder.encode(name);
        url = new URL(appletDocumentBase, name);
      } else {
        url = (i < urlPrefixes.length
               ? new URL(name)
               : new URL("file", null, name));
      }
    } catch (MalformedURLException e) {
      System.out.println("MalformedURLException:" + e);
    }
    System.out.println("returning url=" + url);
    return url;
  }

  public InputStream getInputStreamFromName(String name) {
    classifyName(name);
    if (errorMessage != null)
      return null;
    if (isURL) {
      URL url = getURLFromName(name);
      if (url != null) {
        try {
          System.out.println("getting ready to open url=" + url);
          InputStream is = url.openStream();
          return is;
        } catch (IOException e) {
          System.out.println("error doing a url.openStream:" + e.getMessage());
          errorMessage = e.getMessage();
        }
      }
    } else {
      try {
        return new FileInputStream(file);
      } catch (FileNotFoundException fnf) {
        errorMessage = "File Not Found:" + fnf.getMessage();
      }
    }
    return null;
  }

  private boolean isURL;
  private String fullPathName;
  private String fileName;
  private File file;
  private String errorMessage;

  private void classifyName(String name) {
    errorMessage = fullPathName = fileName = null;
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
        errorMessage = e.getMessage();
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
          errorMessage = e.getMessage();
        }
        return;
      }
    }
    isURL = false;
    file = new File(name);
    fullPathName = file.getAbsolutePath();
    fileName = file.getName();
  }

  public String openFile(String name) {
    System.out.println("openFile(" + name + ")");
    InputStream istream = getInputStreamFromName(name);
    if (errorMessage != null)
      return errorMessage;
    if (istream == null)
      return "error opening url/filename:" + name;
    //    System.out.println(" fullPathName=" + fullPathName +
    //                       " fileName=" + fileName);
    return openInputStream(fullPathName, fileName, istream);
  }

  public String openStringInline(String strModel) {
    return openReader(null, "StringInline", new StringReader(strModel));
  }

  byte[] abMagic = new byte[4];
  private String openInputStream(String fullPathName, String fileName,
                                 InputStream istream) {
    System.out.println("entering openInputStream");
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
      return openReader(fullPathName, fileName,
                        new InputStreamReader(istreamToRead));
    } catch (IOException ioe) {
      return ioe.getMessage();
    }
  }

  private String openReader(String fullPathName, String fileName,
                            Reader reader) {
    Object clientFile = viewer.getJmolModelAdapter()
      .openBufferedReader(viewer, fullPathName, new BufferedReader(reader));
    if (clientFile instanceof String)
      return (String)clientFile;
    viewer.setClientFile(fullPathName, fileName, clientFile);
    return null;
  }
}
