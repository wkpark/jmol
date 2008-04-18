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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.api.JmolExportInterface;
import org.jmol.api.JmolRendererInterface;
import org.jmol.g3d.*;
import org.jmol.modelset.ModelSet;
//import java.awt.Rectangle;
import org.jmol.shape.Shape;
import org.jmol.shape.ShapeRenderer;
import org.jmol.viewer.Viewer;
import org.jmol.util.Logger;

public class FrameRenderer {

  boolean logTime;
    
  Viewer viewer;

  ShapeRenderer[] renderers = new ShapeRenderer[JmolConstants.SHAPE_MAX];

  FrameRenderer(Viewer viewer) {
    this.viewer = viewer;
  }

  void render(Graphics3D g3d, ModelSet modelSet) {  //, Rectangle rectClip

    if (modelSet == null || !viewer.mustRenderFlag())
      return;
    //System.out.println("Frame: rendering viewer "+ viewer + " thread " + Thread.currentThread());    
    logTime = viewer.getTestFlag1();

    viewer.finalizeTransformParameters();

    if (logTime)
      Logger.startTimer();

    for (int i = 0; i < JmolConstants.SHAPE_MAX && g3d.currentlyRendering(); ++i) {
      Shape shape = modelSet.getShape(i);

      if (shape == null)
        continue;
      //System.out.println("FrameRenderer: " + JmolConstants.getShapeClassName(i));
        getRenderer(i, g3d).render(g3d, modelSet, shape); //, rectClip
    }
    if (logTime)
      Logger.checkTimer("render time");
  }

  public void clear() {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i)
      renderers[i] = null;
  }

  ShapeRenderer getRenderer(int shapeID, Graphics3D g3d) {
    if (renderers[shapeID] == null)
      renderers[shapeID] = allocateRenderer(shapeID, g3d);
    return renderers[shapeID];
  }
  
  ShapeRenderer allocateRenderer(int shapeID, Graphics3D g3d) {
    String className = JmolConstants.getShapeClassName(shapeID) + "Renderer";
    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer)shapeClass.newInstance();
      renderer.setViewerG3dShapeID(viewer, g3d, shapeID);
      return renderer;
    } catch (Exception e) {
      Logger.error("Could not instantiate renderer:" + className, e);
    }
    return null;
  }

  String generateOutput(String type, Graphics3D g3d, ModelSet modelSet, String fileName) {
    
    viewer.finalizeTransformParameters();

    JmolExportInterface exporter = null;
    JmolRendererInterface g3dExport = null;
    Object output = null;
    try {
      if (fileName == null)
        output = new StringBuffer();
      else
        output = fileName;
      Class exporterClass = Class.forName("org.jmol.export._"+type+"Exporter");
      exporter = (JmolExportInterface) exporterClass.newInstance();
      exporterClass = Class.forName("org.jmol.export.Export3D");
      g3dExport = (JmolRendererInterface) exporterClass.newInstance();
    } catch (Exception e) {
      Logger.error("Cannot export " + type);
      return "";
    }
    if (!exporter.initializeOutput(viewer, g3d, output))
      return null;
    exporter.getHeader();

    g3dExport.setg3dExporter(g3d, exporter);
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = modelSet.getShape(i);
      if (shape == null)
        continue;
      ShapeRenderer generator = getGenerator(i, g3d);
      if (generator == null)
        continue;
      generator.setGenerator(true);
      g3dExport.setRenderer(generator);
      generator.render(g3dExport, modelSet, shape);
    }
    exporter.getFooter();
       
    return exporter.finalizeOutput();
  }

  ShapeRenderer getGenerator(int shapeID, Graphics3D g3d) {
    String className = "org.jmol.export."
        + JmolConstants.getShapeClassName(~shapeID) + "Generator";
    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer) shapeClass.newInstance();
      renderer.setViewerG3dShapeID(viewer, g3d, shapeID);
      return renderer;
    } catch (Exception e) {
      //that's ok -- just not implemented;
    }
    return null;
  }

}
