package org.jmol.api;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.viewer.Viewer;
import java.util.BitSet;

public interface JmolExportInterface {

  // This method is implemented in org.jmol.export._Exporter 
  // when selecting a specific driver:

  abstract void initialize(Viewer viewer, Graphics3D g3d, StringBuffer output);

  // The following two methods are provided as a general necessity of many drivers.

  abstract void getHeader();

  abstract void getFooter();

  // These methods are used by specific shape generators, which themselves are 
  // extensions of classes in org.jmol.shape, org.jmol.shapebio, and org.jmol.shapespecial. 
  // More will be added as additional objects are added to be exportable classes.

  abstract void renderAtom(Atom atom, short colix);

  abstract void renderBond(Point3f atom1, Point3f atom2, short colix1, short colix2,
                           byte endcaps, int madBond, int bondOrder);

  // The following methods are used by a variety of shape generators and 
  // replace methods in org.jmol.g3d. More will be added as needed. 

  abstract void fillSphereCentered(int radius, Point3f pt, short colix);

  abstract void renderIsosurface(Point3f[] vertices, short colix,
                                 short[] colixes, short[] normals,
                                 int[][] indices, BitSet bsFaces,
                                 int nVertices, int nPoints);
  
  abstract void renderText(Text t);
  
  abstract void drawString(short colix, String str, Font3D font3d, int xBaseline,
                            int yBaseline, int z, int zSlab);
  
  abstract void fillCylinder(short colix, byte endcaps, int diameter, 
                             Point3i screenA, Point3i screenB);

  abstract void drawDottedLine(short colix, Point3i pointA, Point3i pointB); //axes
  
  abstract void drawPoints(short colix, int count, int[] coordinates); //dots

  abstract void drawLine(short colix, Point3i pointA, Point3i pointB); //stars
  
  abstract void fillScreenedCircleCentered(short colix, int diameter, int x,
                                                    int y, int z);  //halos 

  abstract void drawPixel(short colix, int x, int y, int z); //measures
  abstract void drawDashedLine(short colix, int run, int rise, 
                               Point3i ptA, Point3i ptB); //measures
 
  //cartoons, meshRibbons:
  abstract void drawHermite(short colix, boolean fill, boolean border, int tension,
                   Point3i s0, Point3i s1, Point3i s2, Point3i s3,
                   Point3i s4, Point3i s5, Point3i s6, Point3i s7,
                   int aspectRatio);
  
  //rockets:
  abstract void fillQuadrilateral(short colix, Point3f ptA, Point3f ptB, 
                                  Point3f ptC, Point3f ptD);
  abstract void fillCone(short colix, byte endcap, int diameter, 
                         Point3f screenBase, Point3f screenTip);
  
  //cartoons, rockets:
  abstract void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC);
  
  //cartoons, rockets, trace:
  abstract void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3);

  
  //strands:
  abstract void drawHermite(short colix, int tension,
                             Point3i s0, Point3i s1, Point3i s2, Point3i s3);

  //rockets:
  abstract void fillSphereCentered(short colix, int diameter, Point3i pt);

}