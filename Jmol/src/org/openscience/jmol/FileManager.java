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
  final String[] urlPrefixes = {"http:", "https:", "ftp:"};

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
        return "error opening url/filename " + name;
    try {
      openInputStream(istream);
    } catch (Exception e) {
      return "" + e;
    }
    return null;
  }

  private void openInputStream(InputStream istream) throws JmolException {
    InputStreamReader isr = new InputStreamReader(istream);
    BufferedReader bufreader = new BufferedReader(isr);
    try {
      ChemFileReader reader = null;
      try {
        reader = ReaderFactory.createReader(bufreader);
      } catch (IOException ex) {
        throw new JmolException("readMolecule",
            "Error determining input format: " + ex);
      }
      if (reader == null) {
        throw new JmolException("readMolecule", "Unknown input format");
      }
      ChemFile newChemFile = reader.read();

      if (newChemFile != null) {
        if (newChemFile.getNumberOfFrames() > 0) {
          control.setChemFile(newChemFile);
        } else {
          throw new JmolException("readMolecule",
              "the input appears to be empty");
        }
      } else {
        throw new JmolException("readMolecule",
            "unknown error reading input");
      }
    } catch (IOException ex) {
      throw new JmolException("readMolecule", "Error reading input: " + ex);
    }
  }
}
