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
package org.openscience.jmol;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.Reader;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.ReaderFactory;

public class FileManager {

  DisplayControl control;

  FileManager(DisplayControl control) {
    this.control = control;
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
      if (i < urlPrefixes.length)
        url = new URL(name);
      else if (appletDocumentBase != null)
        url = new URL(appletDocumentBase, name);
      else
        url = new URL("file", null, name);
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
    InputStream istream = getInputStreamFromName(name);
    if (istream == null)
        return "error opening url/filename:" + name;
    return openInputStream(istream);
  }

  public String openFile(File file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      return openInputStream(fis);
    } catch (FileNotFoundException e) {
      return "file not found:" + file;
    }
  }

  public String openStringInline(String strModel) {
    return openReader(new StringReader(strModel));
  }

  private String openInputStream(InputStream istream) {
    return openReader(new InputStreamReader(istream));
  }

  private String openReader(Reader rdr) {
    BufferedReader bufreader = new BufferedReader(rdr);
    try {
      ChemFileReader reader = null;
      try {
        reader = ReaderFactory.createReader(bufreader);
        /*
          FIXME -- need to notify the awt component of file change
        firePropertyChange(openFileProperty, oldFile, currentFile);
        */
      } catch (IOException ex) {
        return "Error determining input format: " + ex;
      }
      if (reader == null) {
        return "unrecognized input format";
      }
      ChemFile newChemFile = reader.read();

      if (newChemFile != null) {
        if (newChemFile.getNumberOfFrames() > 0) {
          control.setChemFile(newChemFile);
        } else {
          return "file appears to be empty";
        }
      } else {
        return "unknown error reading input";
      }
    } catch (IOException ex) {
      return "Error reading input:" + ex;
    }
    return null;
  }
}
