/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import org.jmol.modelset.ModelLoader;
import org.jmol.modelset.ModelSet;

class ModelManager {

  private final Viewer viewer;
  private ModelLoader modelLoader;

  private String fullPathName;
  private String fileName;

  ModelManager(Viewer viewer) {
    this.viewer = viewer;
  }

  ModelSet zap() {
    fullPathName = fileName = null;
    modelLoader = new ModelLoader(viewer, "empty");
    return (ModelSet) modelLoader;
  }
  
  String getModelSetFileName() {
    return fileName == null ? "zapped" : fileName;
  }

  String getModelSetPathName() {
    return fullPathName;
  }

  ModelSet createModelSet(String fullPathName, String fileName,
                          Object atomSetCollection, boolean isAppend) {
    // 11.9.10 11/22/2009 bh adjusted to never allow a null return
    if (isAppend) {
      if (atomSetCollection != null)
        modelLoader = new ModelLoader(viewer, atomSetCollection, modelLoader,
            "merge");
    } else if (atomSetCollection == null) {
      return zap();
    } else {
      this.fullPathName = fullPathName;
      this.fileName = fileName;
      String modelSetName = viewer.getModelAdapter().getAtomSetCollectionName(
          atomSetCollection);
      if (modelSetName != null) {
        modelSetName = modelSetName.trim();
        if (modelSetName.length() == 0)
          modelSetName = null;
      }
      if (modelSetName == null)
        modelSetName = reduceFilename(fileName);
      modelLoader = new ModelLoader(viewer, atomSetCollection, null,
          modelSetName);
    }
    if (modelLoader.getAtomCount() == 0)
      zap();
    return (ModelSet) modelLoader;
  }

  private static String reduceFilename(String fileName) {
    if (fileName == null)
      return null;
    int ichDot = fileName.indexOf('.');
    if (ichDot > 0)
      fileName = fileName.substring(0, ichDot);
    if (fileName.length() > 24)
      fileName = fileName.substring(0, 20) + " ...";
    return fileName;
  }

}
