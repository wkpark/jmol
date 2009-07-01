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
 *  Lesser General License for more details.
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
import org.jmol.shape.Shape;
import org.jmol.shape.ShapeRenderer;
import org.jmol.util.Logger;

import java.awt.Rectangle;

class RepaintManager {

  private Viewer viewer;

  RepaintManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void clear() {
    clear(-1);
  }

  private int holdRepaint = 0;
  boolean repaintPending;

  void pushHoldRepaint() {
    ++holdRepaint;
    // System.out.println("Repaintmanager pushHold  " + holdRepaint + " " +
    // repaintPending + " " + Thread.currentThread());
  }

  void popHoldRepaint() {
    // System.out.println("Repaintmanager popHold  " + holdRepaint + " " +
    // repaintPending + " " + Thread.currentThread());
    --holdRepaint;
    if (holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      viewer.repaint();
    }
  }

  boolean refresh() {
    // System.out.println("Repaintmanager refresh  " + holdRepaint + " " +
    // repaintPending + " " + Thread.currentThread());System.out.flush();
    if (repaintPending)
      return false;
    repaintPending = true;
    if (holdRepaint == 0) {
      // System.out.println("Repaintmanager refresh  " + holdRepaint + " " +
      // repaintPending + " " + Thread.currentThread());System.out.flush();
      viewer.repaint();
    }
    return true;
  }

  synchronized void requestRepaintAndWait() {
    // System.out.println("Repaintmanager requestRepaintAndWait  " + holdRepaint
    // + " " + repaintPending + " " +
    // Thread.currentThread());System.out.flush();
    viewer.repaint();
    try {
      wait();
    } catch (InterruptedException e) {
    }
  }

  synchronized void repaintDone() {
    // System.out.println("Repaintmanager repaintDone  " + holdRepaint + " " +
    // repaintPending + " " + Thread.currentThread());
    repaintPending = false;
    notify(); // to cancel any wait in requestRepaintAndWait()
  }

  /*
   * final Rectangle rectOversample = new Rectangle(); boolean tOversample;
   * 
   * void setOversample(boolean tOversample) { //not implemented --
   * this.tOversample = tOversample; }
   */

  void releaseRenderer(int shapeID) {
    clear(shapeID);
  }

  void render(Graphics3D g3d, ModelSet modelSet) {// , Rectangle rectClip
    if (!viewer.getRefreshing())
      return;
    // System.out.println("repaint manager render " + modelSet);
    render1(g3d, modelSet); // , rectClip
    Rectangle band = viewer.getRubberBandSelection();
    if (band != null && g3d.setColix(viewer.getColixRubberband()))
      g3d.drawRect(band.x, band.y, 0, 0, band.width, band.height);
  }

  private boolean logTime;

  private ShapeRenderer[] renderers = new ShapeRenderer[JmolConstants.SHAPE_MAX];

  private void render1(Graphics3D g3d, ModelSet modelSet) { // , Rectangle rectClip

    if (modelSet == null || !viewer.mustRenderFlag())
      return;
    // System.out.println("Frame: rendering viewer "+ viewer + " thread " +
    // Thread.currentThread());
    logTime = viewer.getTestFlag1();

    viewer.finalizeTransformParameters();

    if (logTime)
      Logger.startTimer();

    // System.out.println(" render 1");

    try {
      g3d.renderBackground(null);
      for (int i = 0; i < JmolConstants.SHAPE_MAX && g3d.currentlyRendering(); ++i) {
        Shape shape = modelSet.getShape(i);

        if (shape == null)
          continue;

        // System.out.println("FrameRenderer: " + i + " " +
        // JmolConstants.getShapeClassName(i));
        getRenderer(i, g3d).render(g3d, modelSet, shape); // , rectClip
      }

    } catch (Exception e) {
      Logger
          .error("rendering error -- perhaps use \"set refreshing FALSE/TRUE\" ? ");
    }

    // System.out.println((new Date()).getTime() + " render 2");

    if (logTime)
      Logger.checkTimer("render time");
  }

  private void clear(int iShape) {
    if (iShape >= 0)
      renderers[iShape] = null;
    else
      for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i)
        renderers[i] = null;
  }

  private ShapeRenderer getRenderer(int shapeID, Graphics3D g3d) {
    if (renderers[shapeID] == null)
      renderers[shapeID] = allocateRenderer(shapeID, g3d);
    return renderers[shapeID];
  }

  private ShapeRenderer allocateRenderer(int shapeID, Graphics3D g3d) {
    String className = JmolConstants.getShapeClassName(shapeID) + "Renderer";
    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer) shapeClass.newInstance();
      renderer.setViewerG3dShapeID(viewer, g3d, shapeID);
      return renderer;
    } catch (Exception e) {
      Logger.error("Could not instantiate renderer:" + className, e);
    }
    return null;
  }

  String generateOutput(String type, Graphics3D g3d, ModelSet modelSet,
                        String fileName) {

    viewer.finalizeTransformParameters();

    JmolExportInterface exporter = null;
    JmolRendererInterface g3dExport = null;
    Object output = null;
    try {
      if (fileName == null) {
        output = new StringBuffer();
      } else {
        if (fileName.charAt(0) == '?')
          fileName = viewer.dialogAsk("save", fileName.substring(1));
        if (fileName == null)
          return null;
        output = fileName;
      }
      Class exporterClass = Class.forName("org.jmol.export._" + type
          + "Exporter");
      exporter = (JmolExportInterface) exporterClass.newInstance();
      exporterClass = Class.forName("org.jmol.export.Export3D");
      g3dExport = (JmolRendererInterface) exporterClass.newInstance();
    } catch (Exception e) {
      Logger.error("Cannot export " + type);
      return null;
    }
    if (!exporter.initializeOutput(viewer, g3d, output))
      return null;
    exporter.getHeader();

    g3dExport.setg3dExporter(g3d, exporter);
    exporter.renderBackground();
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

  private ShapeRenderer getGenerator(int shapeID, Graphics3D g3d) {
    String className = "org.jmol.export."
        + JmolConstants.getShapeClassName(~shapeID) + "Generator";
    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer) shapeClass.newInstance();
      renderer.setViewerG3dShapeID(viewer, g3d, shapeID);
      return renderer;
    } catch (Exception e) {
      // that's ok -- just not implemented;
    }
    return null;
  }
}
