/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.filechooser.FileSystemView;

import org.jmol.api.JmolViewer;
import org.jmol.util.TextFormat;

class JmolInstance {
  String name;
  String javaname;
  String file;
  String script;
  int width;
  int height;
  String pictFile;
  boolean pictIsScratchFile;
  JmolViewer viewer;

  JmolInstance(JmolViewer viewer, String name, String file, String script,
      int width, int height) {
    this.viewer = viewer;
    this.name = name;
    this.javaname = TextFormat.replaceAllCharacters(name,
        "[]/\\#*&^%$?.,%<>' \"", "_"); //makes things computer OK
    this.file = file;
    this.script = script;
    this.width = width;
    this.height = height;
    //need the file writing stuff...
    FileSystemView Directories = FileSystemView.getFileSystemView();
    File homedir = Directories.getHomeDirectory();
    String homedirpath = homedir.getPath();
    String scratchpath = homedirpath + "/.jmol_WPM";
    File scratchfile = new File(scratchpath);
    if (!(scratchfile.exists())) {//make the directory if necessary. we will delete when done
      boolean made_scratchdir = scratchfile.mkdir();
      if (!(made_scratchdir)) {
        LogPanel.Log("Attempt to make scratch directory failed.");
      }
    }
    String pictfile = scratchpath + "/" + javaname + ".png";
    this.pictFile = pictfile;
    viewer.createImage(pictfile, "PNG", 0, width, height);
    this.pictIsScratchFile = true;
  }

  boolean movepict(String dirpath) throws IOException {
    //need the file writing stuff...
    String imagename = dirpath + "/" + this.javaname + ".png";
    if (this.pictFile.equals(imagename))
      return false;
    String scratchname = this.pictFile;
    FileInputStream is = null;
    try {
      is = new FileInputStream(scratchname);
    } catch (IOException ise) {
      throw ise;
    }
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(imagename);
      int pngbyteint = is.read();
      while (pngbyteint != -1) {
        os.write(pngbyteint);
        pngbyteint = is.read();
      }
      os.flush();
      os.close();
      is.close();
    } catch (IOException exc) {
      throw exc;
    }
    if (this.pictIsScratchFile) { //only delete the file if not using file already saved for user.
      File scratchtoerase = new File(scratchname);
      boolean deleteOK = scratchtoerase.delete();
      if (!(deleteOK)) {
        IOException IOe = (new IOException("Failed to delete scratch file "
            + scratchname + "."));
        throw IOe;
      }
    }
    this.pictFile = imagename;
    this.pictIsScratchFile = false;
    return true;
  }
}
