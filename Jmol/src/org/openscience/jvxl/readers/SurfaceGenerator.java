/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
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


/*
 
 * The JVXL file format
 * --------------------
 * 
 * as of 3/29/07 this code is COMPLETELY untested. It was hacked out of the
 * Jmol code, so there is probably more here than is needed.
 * 
 * 
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 *
 * The JVXL (Jmol VoXeL) format is a file format specifically designed
 * to encode an isosurface or planar slice through a set of 3D scalar values
 * in lieu of a that set. A JVXL file can contain coordinates, and in fact
 * it must contain at least one coordinate, but additional coordinates are
 * optional. The file can contain any finite number of encoded surfaces. 
 * However, the compression of 300-500:1 is based on the reduction of the 
 * data to a SINGLE surface. 
 * 
 * 
 * The original Marching Cubes code was written by Miguel Howard in 2005.
 * The classes Parser, ArrayUtil, and TextFormat are condensed versions
 * of the classes found in org.jmol.util
 * 
 * All code relating to JVXL format is copyrighted 2006/2007 and invented by 
 * Robert M. Hanson, 
 * Professor of Chemistry, 
 * St. Olaf College, 
 * 1520 St. Olaf Ave.
 * Northfield, MN. 55057.
 * 
 * Implementations of the JVXL format should reference 
 * "Robert M. Hanson, St. Olaf College" and the opensource Jmol project.
 * 
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 * 
 * lines through coordinates are identical to CUBE files
 * after that, we have a line that starts with a negative number to indicate this
 * is a JVXL file:
 * 
 * line1:  (int)-nSurfaces  (int)edgeFractionBase (int)edgeFractionRange  
 * (nSurface lines): (float)cutoff (int)nBytesData (int)nBytesFractions
 * 
 * definition1
 * edgedata1
 * fractions1
 * colordata1
 * ....
 * definition2
 * edgedata2
 * fractions2
 * colordata2
 * ....
 * 
 * definitions: a line with detail about what sort of compression follows
 * 
 * edgedata: a list of the count of vertices ouside and inside the cutoff, whatever
 * that may be, ordered by nested for loops for(x){for(y){for(z)}}}.
 * 
 * nOutside nInside nOutside nInside...
 * 
 * fractions: an ascii list of characters represting the fraction of distance each
 * encountered surface point is along each voxel cube edge found to straddle the 
 * surface. The order written is dictated by the reader algorithm and is not trivial
 * to describe. Each ascii character is constructed by taking a base character and 
 * adding onto it the fraction times a range. This gives a character that can be
 * quoted EXCEPT for backslash, which MAY be substituted for by '!'. Jmol uses the 
 * range # - | (35 - 124), reserving ! and } for special meanings.
 * 
 * colordata: same deal here, but with possibility of "double precision" using two bytes.
 * 
 * 
 * 
 * THIS READER
 * -----------
 * 
 * This is a first attempt at a generic JVXL file reader and writer class.
 * It is an extraction of Jmol org.jmol.viewer.Isosurface.Java and related pieces.
 * 
 * The goal of the reader is to be able to read CUBE-like data and 
 * convert that data to JVXL file data.
 * 
 * 
 */

package org.openscience.jvxl.readers;

import java.io.BufferedReader;
import javax.vecmath.Point4f;

import org.jmol.util.Logger;
import org.openscience.jvxl.util.*;

public class SurfaceGenerator { 

  ColorEncoder colorEncoder;
  Parameters params;
  MeshData meshData = new MeshData();
  JvxlData jvxlData = new JvxlData();
  VolumeData volumeData;

  public SurfaceGenerator(ColorEncoder colorEncoder) {
    params = new Parameters();
    initializeIsosurface();
    params.colorPos = colorEncoder.getColorPositive();
    params.colorNeg = colorEncoder.getColorNegative();
    this.colorEncoder = colorEncoder;
  }
  
  private int state;
  
  private final static int STATE_INITIALIZED = 1;
  private final static int STATE_DATA_READ = 2;
  private final static int STATE_DATA_COLORED = 3;

  class Parameters {

    float mappedDataMin;
    float mappedDataMax;
    String[] title;
    boolean blockCubeData;
    int fileIndex; //one-based    
    boolean insideOut;
    float cutoff = Float.MAX_VALUE;
    boolean rangeDefined;
    float valueMappedToRed, valueMappedToBlue;

    boolean isColorReversed;

    Point4f thePlane;
    boolean isContoured;
    boolean isBicolorMap;
    boolean isCutoffAbsolute;
    boolean isPositiveOnly;

    boolean colorBySign;
    boolean colorByPhase;
    boolean colorBySets;
    int colorNeg;
    int colorPos;
    int minColor;
    int maxColor;
    int colorPhase;
    float resolution;
    
    void initialize() {
      mappedDataMin = Float.MAX_VALUE;
      blockCubeData = false; // Gaussian standard, but we allow for multiple surfaces one per data block
      isColorReversed = false;
      colorBySign = colorByPhase = colorBySets = false;
      resolution = Float.MAX_VALUE;
      //anisotropy[0] = anisotropy[1] = anisotropy[2] = 1f;
      cutoff = Float.MAX_VALUE;
      thePlane = null;
      //surface_data = null;
      isContoured = false;
      rangeDefined = false;
      isBicolorMap = isCutoffAbsolute = isPositiveOnly = false;
    }

   void setMapRanges() {
      if (colorByPhase || colorBySign || isBicolorMap && !isContoured) {
        mappedDataMin = -1;
        mappedDataMax = 1;
      }
      if (mappedDataMin == Float.MAX_VALUE || mappedDataMin == mappedDataMax) {
        mappedDataMin = voxelReader.getMinMappedValue();
        mappedDataMax = voxelReader.getMaxMappedValue();
      }
      if (mappedDataMin == 0 && mappedDataMax == 0) {
        //just set default -1/1 if there is no obvious data
        mappedDataMin = -1;
        mappedDataMax = 1;
      }

      if (!rangeDefined) {
        valueMappedToRed = mappedDataMin;
        valueMappedToBlue = mappedDataMax;
      }

      minColor = (isColorReversed ? colorPos : colorNeg);
      maxColor = (isColorReversed ? colorNeg : colorPos);

    }
  }
  //////////////////////////////////////////////////////////////
   
  int colorPtr;
  VoxelReader voxelReader;
  
  public void setProperty(String propertyName, Object value) {

    
    if ("init" == propertyName) {
      initializeIsosurface();
      return;
    }
    
    if ("fileIndex" == propertyName) {
      params.fileIndex = ((Integer) value).intValue();
      if (params.fileIndex < 1)
        params.fileIndex = 1;
      return;
    }

    if ("blockData" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      params.blockCubeData = TF;
      return;
    }

    if ("title" == propertyName) {
      if (value == null) {
        params.title = null;
        return;
      } else if (value instanceof String[]) {
        params.title = (String[]) value;
      }
      return;
    }

    if ("cutoff" == propertyName) {
      params.cutoff = ((Float) value).floatValue();
      params.isPositiveOnly = false;
      return;
    }

    if ("cutoffPositive" == propertyName) {
      params.cutoff = ((Float) value).floatValue();
      params.isPositiveOnly = true;
      return;
    }

    /// color options 

    if ("insideOut" == propertyName) {
      params.insideOut = true;
      return;
    }

    if ("sign" == propertyName) {
      params.isCutoffAbsolute = true;
      params.colorBySign = true;
      colorPtr = 0;
      return;
    }

    if ("red" == propertyName) {
      params.valueMappedToRed = ((Float) value).floatValue();
      return;
    }

    if ("blue" == propertyName) {
      params.valueMappedToBlue = ((Float) value).floatValue();
      params.rangeDefined = true;
      return;
    }

    if ("reverseColor" == propertyName) {
      params.isColorReversed = true;
      return;
    }

    if ("setColorScheme" == propertyName) {
      String colorScheme = (String) value;
      colorEncoder.setColorScheme(colorScheme);
      return;
    }

    if ("plane" == propertyName) {
      params.thePlane = (Point4f) value;
      params.isContoured = true;
      ++state;
      return;
    }

    if ("readData" == propertyName) {
      if (++state != STATE_DATA_READ)
        return;
      if ((voxelReader = setData(value)) == null) {
        Logger.error("Could not set the data");
        return;
      }
      if (params.colorBySign)
        params.isBicolorMap = true;
      if (!voxelReader.createIsosurface()) {
        Logger.error("Could not create isosurface");
        return;
      }
      jvxlData.jvxlFileMessage = (jvxlData.jvxlDataIsColorMapped ? "mapped" : "");
      if (params.isContoured && params.thePlane == null)
        voxelReader.setPlanarVectors();

      if (params.colorBySign) {
        state = STATE_DATA_COLORED;
        voxelReader.applyColorScale();
      }
      voxelReader.jvxlUpdateInfo();
      voxelReader.discardTempData(false);
      params.mappedDataMin = Float.MAX_VALUE;
      return;
    }

    if ("phase" == propertyName) {
      String color = (String) value;
      params.isCutoffAbsolute = true;
      params.colorBySign = true;
      params.colorByPhase = true;
      params.colorPhase = VoxelReader.getColorPhaseIndex(color);
      if (params.colorPhase <= 0) {
        Logger.warn(" invalid color phase: " + color);
        params.colorPhase = 1;
      }
      return;
    }

    if ("mapColor" == propertyName) {
      if (++state != STATE_DATA_COLORED)
        return;
      if (value instanceof String && ((String)value).equalsIgnoreCase("sets")) {
        meshData.getSurfaceSet(0);
        params.colorBySets = true;
      } else if ((voxelReader = setData(value)) == null) {
        Logger.error("Could not set the mapping data");
        return;
      }
      if (params.thePlane != null) {
        voxelReader.createIsosurface(); //for the plane
        voxelReader.readVolumetricData(true); //for the data
        voxelReader.colorIsosurface();
      } else {
        if (!params.colorBySets)
          voxelReader.readData(true);
        if (jvxlData.jvxlDataIsColorMapped) {
          voxelReader.readColorData();
        } else {
          voxelReader.colorIsosurface();
        }
      }
      voxelReader.jvxlUpdateInfo();
      voxelReader.discardTempData(true);
      return;
    }
  }

  public Object getProperty(String property, int index) {
    //StringBuffer bs = new StringBuffer();
    //JvxlReader.jvxlCreateHeader("line1", "line2", volumeData, bs);
    //System.out.println(bs);

    if (property == "plane")
      return (jvxlData.jvxlPlane);
    if (property == "jvxlFileData")
      return JvxlReader.jvxlGetFile(params.title, jvxlData, "", true, index);
    if (property == "jvxlFileInfo")
      return jvxlData.jvxlInfoLine;
    if (property == "jvxlSurfaceData")
      return JvxlReader.jvxlGetFile(params.title, jvxlData, "", false, 1);
    if (property == "volumeData")
      return volumeData;
    if (property == "meshData")
      return meshData;
    return null;
  }

  VoxelReader setData(Object value) {
    if (value instanceof VolumeData) {
      volumeData = (VolumeData) value;
      return new VolumeDataReader(this);
    }
    volumeData = new VolumeData();
    BufferedReader br = null; 
    if (value instanceof String) {
      Object t = FileReader.getBufferedReaderOrErrorMessageFromName((String) value);
      if (t instanceof String) {
        Logger.error((String) t);
        return null;
      }
      br = (BufferedReader) t;
    } else if (value instanceof BufferedReader) {
      br = (BufferedReader) value;
    }
    if (br == null)
      return null;
    String fileType = VolumeFileReader.determineFileType(br);
    Logger.info("data file type was determined to be " + fileType);
    if (fileType.equals("Jvxl+"))
      return new JvxlReader(this, br);
    if (fileType.equals("Jvxl"))
      return new JvxlReader(this, br);
    if (fileType.equals("Apbs"))
      return new ApbsReader(this, br);
    if (fileType.equals("Cube"))
      return new CubeReader(this, br);
    return null;
  }
  
  void initializeIsosurface() {
    params.initialize();
    colorPtr = 0;
    initState();
  }

  void initState() {
    state = STATE_INITIALIZED;
  }

}

