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
  public void setAppletDocumentBase(URL base) {
    appletDocumentBase = base;
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
        if (i < urlPrefixes.length)
          name = "JmolAppletProxy.pl?url=" + URLEncoder.encode(name);
        System.out.println("an applet will try to open the URL:" + name);
        url = new URL(appletDocumentBase, name);
      } else {
        url = (i < urlPrefixes.length
               ? new URL(name)
               : new URL("file", null, name));
      }
    } catch (MalformedURLException e) {
    }
    return url;
  }

  public InputStream getInputStreamFromName(String name) {
    URL url = getURLFromName(name);
    if (url != null) {
      try {
        return url.openStream();
      } catch (IOException e) {
      }
    }
    return null;
  }

  public String openFile(String name) {
    System.out.println("openFile(" + name + ")");
    InputStream istream = getInputStreamFromName(name);
    if (istream == null)
        return "error opening url/filename:" + name;
    return openInputStream(name, istream);
  }

  public String openFile(File file) {
    System.out.println("openFile(File:" + file.getName() + ")");
    try {
      FileInputStream fis = new FileInputStream(file);
      return openInputStream(file.getName(), fis);
    } catch (FileNotFoundException e) {
      return "file not found:" + file;
    }
  }

  public String openStringInline(String strModel) {
    return openReader("StringInline", new StringReader(strModel));
  }

  byte[] abMagic = new byte[4];
  private String openInputStream(String name, InputStream istream) {
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
      return openReader(name, new InputStreamReader(istreamToRead));
    } catch (IOException ioe) {
      return "Error reading stream header: " + ioe;
    }
  }

  private String openReader(String name, Reader reader) {
    System.out.println("openReader(" + name + ")");
    Object clientFile = viewer.getJmolModelAdapter()
      .openBufferedReader(viewer, name, new BufferedReader(reader));
    if (clientFile instanceof String)
      return (String)clientFile;
    viewer.setClientFile(name, clientFile);
    return null;
  }
}
