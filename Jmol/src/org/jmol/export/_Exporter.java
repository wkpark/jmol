/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

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

package org.jmol.export;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.api.JmolRendererInterface;
import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.viewer.Viewer;

/*
 * Jmol Export Drivers
 * 
 * org.jmol.export._Exporter
 * org.jmol.export.[Driver]_Exporter
 * org.jmol.export.[Shape]Generator
 * 
 *  org.jmol.export is a package that contains export drivers --
 *  custom interfaces for capturing the information that would normally
 *  go to the screen. Currently org.jmol.export is not a package of the applet, 
 *  but that is not necessary.
 *  
 *  The command is:
 *  
 *    write [driverName] [filename] 
 *  
 *  For example:
 *  
 *    write VRML "myfile.wrl"
 *    
 *  Or, programmatically:
 *  
 *  String data = org.jmol.viewer.viewer.generateOutput([Driver])
 *  
 *  where in this case [Driver] is a string such as "Maya" or "Vrml".
 *  
 *  Possible implementations include a VRML driver, a Maya ascii driver, 
 *  a Maya Obj driver, an Excel driver, etc.
 *  
 *  Once a driver is registered in org.jmol.viewer.JmolConstants.EXPORT_DRIVER_LIST,
 *  all that is necessary is to add the appropriate Java class file to 
 *  the org.jmol.export directory with the name FooExporter.java. 
 *  
 *  Jmol will find it using Class.forName().
 *   
 *  This export driver is then responsible for implementing all abstractmethods
 *  of the _Exporter.java class. 
 * 
 *  Accompanying the export drivers in this package is a set of ShapeRenderers.
 *  Each of these "Generators"  provides generalized off-screen rendering to
 *  the drivers. If a generator is not present, it means that all operations
 *  are carried out by the underlying shape renderer.
 *  
 *  The two roles --- Generator and _Exporter --- are independent and, in general,
 *  can be developed (almost) independently. Thus, if a CartoonGenerator is 
 *  developed, the various export drivers may need to be updated. This is done by
 *  adding a new class here that is functionally equivalent to one of the Graphics3D
 *  methods. 
 *  
 *  If time permits, one might implement cartoon export in some or all of 
 *  the drivers. If a driver is skipped, then it needs to at least be given an instance
 *  of the added Graphics3D-like method. 
 *  
 *  Or, in some cases, it may be that no additional driver methods are needed.
 *  
 *  Basically, this system is designed to be updated easily by multiple 
 *  developers. The process should be:
 *  
 *   1) Add the Driver name to org.jmol.viewer.JmolConstants.EXPORT_DRIVER_LIST.
 *   2) Copy one of the exporters to create org.jmol.export.[Driver]_Exporter.java
 *   3) Fill out the template with proper calls. 
 *  
 *  Alternatively, Java-savvy users can create their own drivers entirely independently
 *  and place them in org.jmol.export. Setting the script variable "exportDrivers" to
 *  include this driver enables that custom driver. The default value for this variable is:
 *  
 *    exportDrivers = "Maya;Vrml"
 *   
 *  Whatever default drivers are provided with Jmol should be in EXPORT_DRIVER_LIST; setting
 *  
 *    exportDrivers = "Mydriver"
 *    
 *  Disables Maya and Vrml; setting it to   
 *  
 *    exportDrivers = "Maya;Vrml;Mydriver"
 *    
 *  Enables the default Maya and Vrml drivers as well as a user-custom driver, MydriverExporter.java
 *    
 * Bob Hanson, 7/2007
 * 
 */

public abstract class _Exporter {

  // The following fields and methods are required for instantiation or provide
  // generally useful functionality:

  protected Viewer viewer;
  protected JmolRendererInterface jmolRenderer;
  protected StringBuffer output;
  protected BufferedWriter bw;
  private FileOutputStream os;
  protected String fileName;
  protected String commandLineOptions;
  
  protected boolean isToFile;
  protected Graphics3D g3d;

  protected int screenWidth;
  protected int screenHeight;
  protected int slabZ;
  protected int depthZ;

  boolean use2dBondOrderCalculation;
  boolean canDoTriangles;
  boolean isCartesianExport;

  protected Point3f center = new Point3f();
  protected Point3f tempP1 = new Point3f();
  protected Point3f tempP2 = new Point3f();
  protected Point3f tempP3 = new Point3f();
  protected Vector3f tempV1 = new Vector3f();
  protected Vector3f tempV2 = new Vector3f();
  protected Vector3f tempV3 = new Vector3f();
  protected AxisAngle4f tempA = new AxisAngle4f();
  
  public _Exporter() {
  }

  public void setRenderer(JmolRendererInterface jmolRenderer) {
    this.jmolRenderer = jmolRenderer;
  }
  
  public boolean initializeOutput(Viewer viewer, Graphics3D g3d, Object output) {
    this.viewer = viewer;
    this.g3d = g3d;
    center.set(viewer.getRotationCenter());
    if ((screenWidth <= 0) || (screenHeight <= 0)) {
      screenWidth = viewer.getScreenWidth();
      screenHeight = viewer.getScreenHeight();
    }
    slabZ = g3d.getSlab();
    depthZ = g3d.getDepth();
    isToFile = (output instanceof String);
    if (isToFile) {
      fileName = (String) output;
      int pt = fileName.indexOf(":::"); 
      if (pt > 0) {
        commandLineOptions = fileName.substring(pt + 3);
        fileName = fileName.substring(0, pt);
      }
      viewer.writeTextFile(fileName + ".spt", viewer.getSavedState("_Export"));
      try {
        os = new FileOutputStream(fileName);
        bw = new BufferedWriter(new OutputStreamWriter(os));
      } catch (FileNotFoundException e) {
        return false;
      }
    } else {
      this.output = (StringBuffer) output;
    }
    getHeader();
    return true;
  }

  public String finalizeOutput() {
    getFooter();
    if (!isToFile)
      return output.toString();
    try {
      bw.flush();
      bw.close();
      os = null;
    } catch (IOException e) {
      //ignore
    }
    return null;
  }

  protected static String getExportDate() {
    return new SimpleDateFormat("yyyy-MM-dd', 'HH:mm").format(new Date());
  }

  final protected static float degreesPerRadian = (float) (360 / (2 * Math.PI));

  protected Vector3f getRotation(Vector3f v) {
    tempV3.set(v);
    tempV3.normalize();
    float r = (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
    float rX = (float) Math.acos(v.y / r) * degreesPerRadian;
    if (v.x < 0)
      rX += 180;
    float rY = (float) Math.atan2(v.x, v.z) * degreesPerRadian;
    tempV3.set(rX, rY, 0);
    return tempV3;
  }

  protected AxisAngle4f getAxisAngle(Vector3f v) {
    tempV3.set(0, 1, 0);
    tempV2.set(v);
    tempV2.normalize();
    tempV3.add(tempV2);
    tempA.set(tempV3.x, tempV3.y, tempV3.z, 3.14159f);
    return tempA;
  }

  protected String rgbFromColix(short colix, char sep) {
    int argb = g3d.getColixArgb(colix);
    return new StringBuffer().append((argb >> 16) & 0xFF).append(sep).append(
        (argb >> 8) & 0xFF).append(sep).append((argb) & 0xFF).toString();
  }

  protected String rgbFractionalFromColix(short colix, char sep) {
    return rgbFractionalFromArgb(g3d.getColixArgb(colix), sep);
  }

  protected String rgbFractionalFromArgb(int argb, char sep) {
    return "" + round(((argb >> 16) & 0xFF) / 255f) + sep 
        + round(((argb >> 8) & 0xFF) / 255f) + sep
        + round(((argb) & 0xFF) / 255f);
  }

  protected String translucencyFractionalFromColix(short colix) {
    int translevel = Graphics3D.getColixTranslucencyLevel(colix);
    if (Graphics3D.isColixTranslucent(colix))
      return new StringBuffer().append(translevel / 255f).toString();
    return new StringBuffer().append(0f).toString();
  }

  protected static float round(float number) { //AH
    return (float) Math.round(number*1000)/1000;  // leave just 3 decimals
  }
  
  /**
   * input an array of colixes; returns a Vector for the color list and a HashTable
   * for correlating the colix with a specific color index
   * 
   * @param i0
   * @param colixes
   * @param nVertices
   * @param bsSelected
   * @param htColixes
   * @return             Vector and HashTable
   */
  protected Vector getColorList(int i0, short[] colixes, int nVertices, BitSet bsSelected, Hashtable htColixes) {
    String color;
    int nColix = 0;
    Vector list = new Vector();
    for (int i = 0; i < nVertices; i++) 
      if (bsSelected == null || bsSelected.get(i)) {
        color = "" + colixes[i];
        if (!htColixes.containsKey(color)) {
          list.add(new Short(colixes[i]));
          htColixes.put(color, "" + (i0 + nColix++));
        }
      }
   return list;
  }


  // absbract methods that MUST be implemented
  
  abstract void getHeader();

  abstract void getFooter();


  // The following two methods are provided as a general necessity of many drivers.

  // These methods are used by specific shape generators, which themselves are 
  // extensions of classes in org.jmol.shape, org.jmol.shapebio, and org.jmol.shapespecial. 
  // More will be added as additional objects are added to be exportable classes.

  abstract void renderAtom(Atom atom, short colix);

  // The following methods are used by a variety of shape generators and 
  // replace methods in org.jmol.g3d. More will be added as needed. 

  abstract void renderIsosurface(Point3f[] vertices, short colix,
                                 short[] colixes, Vector3f[] normals,
                                 int[][] indices, BitSet bsFaces,
                                 int nVertices, int faceVertexMax, 
                                 short[] polygonColixes, int nPolygons);

  abstract void renderText(Text t);
  
  abstract void drawString(short colix, String str, Font3D font3d, int xBaseline,
                            int yBaseline, int z, int zSlab);
  
  abstract void fillCylinder(Point3f atom1, Point3f atom2, short colix1, short colix2,
                             byte endcaps, int madBond, int bondOrder);

  abstract void fillCylinder(short colix, byte endcaps, int diameter, 
                             Point3f screenA, Point3f screenB);

  abstract void drawCircleCentered(short colix, int diameter, int x,
                                           int y, int z, boolean doFill);  //draw circle 

  abstract void fillScreenedCircleCentered(short colix, int diameter, int x,
                                                    int y, int z);  //halos 

  abstract void drawPixel(short colix, int x, int y, int z); //measures
 
  abstract void drawTextPixel(int argb, int x, int y, int z);

  //rockets and dipoles
  abstract void fillCone(short colix, byte endcap, int diameter, 
                         Point3f screenBase, Point3f screenTip);
  
  //cartoons, rockets:
  abstract void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC);
  
  //rockets:
  abstract void fillSphereCentered(short colix, int diameter, Point3f pt);
  
  abstract void plotText(int x, int y, int z, short colix, String text, Font3D font3d);

  abstract void plotImage(int x, int y, int z, Image image, short bgcolix, 
                          int width, int height);

  abstract void startShapeBuffer();

  abstract void endShapeBuffer();

  // NOT IMPLEMENTED, but could be if needed:
  
  abstract void renderEllipsoid(short colix, int x, int y, int z, int diameter,
                                double[] coef, Point3i[] selectedPoints);

}
