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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.io.BufferedReader;

import javax.vecmath.Point3f;

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.util.Logger;

abstract class ElectronDensityFileReader extends VolumeFileReader {

  ElectronDensityFileReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    isAngstroms = true;
  }

    /* see http://ami.scripps.edu/software/mrctools/mrc_specification.php
     * 
     * many thanks to Eric Martz for providing the test files that
     * made this understandable.
     * 
    1 NX       number of columns (fastest changing in map)
    2 NY       number of rows   
    3 NZ       number of sections (slowest changing in map)
    4 MODE     data type :
         0        image : signed 8-bit bytes range -128 to 127
         1        image : 16-bit halfwords
         2        image : 32-bit reals
         3        transform : complex 16-bit integers
         4        transform : complex 32-bit reals
         6        image : unsigned 16-bit range 0 to 65535
    5 NXSTART number of first column in map (Default = 0)
    6 NYSTART number of first row in map
    7 NZSTART number of first section in map
    8 MX       number of intervals along X
    9 MY       number of intervals along Y
    10  MZ       number of intervals along Z
    11-13 CELLA    cell dimensions in angstroms
    14-16 CELLB    cell angles in degrees
    17  MAPC     axis corresp to cols (1,2,3 for X,Y,Z)
    18  MAPR     axis corresp to rows (1,2,3 for X,Y,Z)
    19  MAPS     axis corresp to sections (1,2,3 for X,Y,Z)
    20  DMIN     minimum density value
    21  DMAX     maximum density value
    22  DMEAN    mean density value
    23  ISPG     space group number 0 or 1 (default=0)
    24  NSYMBT   number of bytes used for symmetry data (0 or 80)
    25-49 EXTRA    extra space used for anything   - 0 by default
    50-52 ORIGIN   origin in X,Y,Z used for transforms
    53  MAP      character string 'MAP ' to identify file type
    54  MACHST   machine stamp
    55  RMS      rms deviation of map from mean density
    56  NLABL    number of labels being used
    57-256  LABEL(20,10) 10 80-character text labels
    */

    // inputs:
    protected int mapc, mapr, maps;
    protected int nx, ny, nz, mode;
    protected int nxStart, nyStart, nzStart;
    protected int mx, my, mz;
    protected float a, b, c, alpha, beta, gamma;
    protected float originX, originY, originZ;
    
    // outputs:
    protected Point3f[] vectors = new Point3f[3];
    protected Point3f origin = new Point3f();

    protected void getVectorsAndOrigin() {
      
      Logger.info("grid parameters: nx,ny,nz: " + nx + "," + ny + "," + nz);
      Logger.info("grid parameters: nxStart,nyStart,nzStart: " + nxStart + "," + nyStart + "," + nzStart);

      Logger.info("grid parameters: mx,my,mz: " + mx + "," + my + "," + mz);
      Logger.info("grid parameters: a,b,c,alpha,beta,gamma: " + a + "," + b + "," + c + "," + alpha + "," + beta + "," + gamma);
      Logger.info("grid parameters: mapc,mapr,maps: " + mapc + "," + mapr + "," + maps);
      Logger.info("grid parameters: originX,Y,Z: " + originX + "," + originY + "," + originZ);
      

      SymmetryInterface unitCell;
      
      Logger.info("grid header: a,b,c,alpha,beta,gamma: " + a + "," + b + ","
          + c + "," + alpha + "," + beta + "," + gamma);

      unitCell = (SymmetryInterface) Interface
          .getOptionInterface("symmetry.Symmetry");
      unitCell.setUnitCell(new float[] { a, b, c, alpha, beta, gamma });

        /*
         
         Many thanks to Eric Martz for helping get this right. 
         Basically we have:
        
         Three principal crystallographic axes: a, b, and c...
         ...also referred to as x, y, and z
         ...also referred to as directions 1, 2, and 3
         ...a mapping of "sheets" "rows" and "columns" of data
            set in the file as 
             
                             s1r1c1...s1r1c9.....
                             s1r2c1...s1r2c9.....
                            
                             s2r1c1...s2r1c9.....
                             s2r2c1...s2r2c9.....
         etc.
         
         Now, nx (but NOT nxStart) refers to "column" data
              ny (but NOT nyStart) refers to "row" data
              nz (but NOT nzStart) refers to "sheet" data
        
         (These, in my opinion, should have been called "nc, nr, ns"!)
         
         In Jmol, we always have x (our [0]) running slowest, so we 
         ultimately must make the following assignment:
         
           MRC "maps" maps to .x or [0]
           MRC "mapr" maps to .y or [1]
           MRC "mapc" maps to .z or [2]
        
         We really don't care if this is actually physical "x" "y" or "z".
         In fact, for a hexagonal cell these will be combinations of xyz.
         
         Now, we also have:
        
           mx and nxStart, which refer to (a) unit cell direction
           my and nyStart, which refer to (b) unit cell direction
           mz and nzStart, which refer to (c) unit cell direction
        
         mx=2 I THINK says "map fasted moving data to the second axis (b)"
         
         So it goes something like this:
         
         scale the (a) vector by 1/mx and call that vector[0]
         scale the (b) vector by 1/my and call that vector[1]
         scale the (c) vector by 1/mz and call that vector[2]
         
         Now map these vectors to Jmol volumetricVectors using
        
         our x: volVector[0] = vector[maps - 1]  (slow)
         our y: volVector[1] = vector[mapr - 1] 
         our z: volVector[2] = vector[mapc - 1]  (fast)
        
         This is because our x is the slowest running variable.
        */               
        
      vectors[0] = new Point3f(1f / mx, 0, 0);
      vectors[1] = new Point3f(0, 1f / my, 0);
      vectors[2] = new Point3f(0, 0, 1f / mz);
      unitCell.toCartesian(vectors[0]);
      unitCell.toCartesian(vectors[1]);
      unitCell.toCartesian(vectors[2]);

      Logger.info("Jmol unit cell vectors:");
      Logger.info("    a: " + vectors[0]);
      Logger.info("    b: " + vectors[1]);
      Logger.info("    c: " + vectors[2]);


      voxelCounts[0] = nz; // slowest
      voxelCounts[1] = ny;
      voxelCounts[2] = nx; // fastest
      volumetricVectors[0].set(vectors[maps - 1]);
      volumetricVectors[1].set(vectors[mapr - 1]);
      volumetricVectors[2].set(vectors[mapc - 1]);

      /*
        
        For the offset of the orgin, now, we must...
         
        ...scale the "unit vector" vector[0] by nxStart
        ...scale the "unit vector" vector[1] by nyStart
        ...scale the "unit vector" vector[2] by nzStart
        ...add those up to give origin.xyz
        
        This is only a temporary assignment, in the
        coordinate system of the unit cell.
        
        */
        
      origin.scaleAdd(nxStart, vectors[0], origin);
      origin.scaleAdd(nyStart, vectors[1], origin);
      origin.scaleAdd(nzStart, vectors[2], origin);
      
      Logger.info("Jmol origin in unit cell coordinates: " + origin);

        /*
        
        The origin point is in reference to the Cartesian 
        transform of the unit cell [a b c], but still needs
        to be set in the coordinate system of MRC fast (x) to
        slow (z). This origin remains in this system throughout 
        the calculation. We do not have to convert to "real" 
        coordinates until the end (in VolumeData.voxelPtToXYZ).
        
        Now, you might think we need:
        
         origin.x = OriginX + origin[maps-1]
         origin.y = OriginY + origin[mapr-1]
         origin.z = OriginZ + origin[mapc-1]
        
        but actually we need those reversed:
        
         origin.x = OriginZ + origin[mapc-1]
         origin.y = OriginY + origin[mapr-1]
         origin.z = OriginX + origin[maps-1]
        
        I think what is happening is that this .x, .y, .z
        refer to our Jmol "x slowest/z fastest",
        while the MRC definitions are for the reverse.
        
        At least this is what is needed to match the
        PDB file with the CCP4 MAP file for 3hyd.

        
        -Bob Hanson, 1/16/2010
        
        */    
        
      float[] o = new float[3];
      o[0] = origin.x; // x --> c = 2 --> y 
      o[1] = origin.y; // y --> r = 1 --> x
      o[2] = origin.z; // z --> s = 3 --> z
        
      if (originX > 0) {
        // emd_1003.map" from http://www.ebi.ac.uk/pdbe/emdb/
        // assume they mean the "center the data at this point"
        // not "put the grid origin at this coordinate"
        Logger.info("Jmol assuming positive center means the origin of data is at {-x -y -z}");
        originX = -originX;
        originY = -originY;
        originZ = -originZ;
      } else if (originX < 0) {
        // assume the standard "put the grid origin at this coordinate"
        // not that they mean the "center the data at this point"
        Logger.info("Jmol negative center found -- uncertain here.");          
      } else if (nxStart == 0 && nyStart == 0 && nzStart == 0) {
        // emd_1004.map
        Logger.info("Jmol origin and xyz map starts are all zeros.");
      }
      Logger.info("Jmol use   isosurface OFFSET {x y z}   if you want to shift it.");
      origin.x = originZ + o[mapc - 1];
      origin.y = originY + o[mapr - 1];
      origin.z = originX + o[maps - 1];
 
      volumetricOrigin.set(origin);
      
      Logger.info("Jmol origin in slow-to-fast system: " + origin);
        
      /* example:
          
grid parameters: nx,ny,nz: 38,90,55
grid parameters: nxStart,nyStart,nzStart: -11,-6,-11
grid parameters: mx,my,mz: 144,16,56
grid parameters: a,b,c,alpha,beta,gamma: 49.475,4.8375,19.4375,90.0,96.65,90.0
grid parameters: mapc,mapr,maps: 2,1,3
grid parameters: originX,Y,Z: 0.0,0.0,0.0

MRC unit cell vectors:
    a: (0.34357637, 0.0, 0.0)
    b: (0.0, 0.30234376, 0.0)
    c: (-0.040195342, 0.0, 0.34476298)
MRC origin in unit cell coordinates: (-3.3371913, -1.8140624, -3.7923927)
MRC origin in slow-to-fast (Jmol) sytem: (-1.8140624, -3.3371913, -3.7923927)

voxel grid origin:(-1.8140624, -3.3371913, -3.7923927)
voxel grid count/vector:55 -0.040195342 -1.692914E-8 0.34476298
voxel grid count/vector:90 0.34357637 0.0 0.0
voxel grid count/vector:38 -1.3215866E-8 0.30234376 0.0
JVXL read: 55 x 90 x 38 data points
         
         */

        // setting the cutoff to mean + 2 x RMS seems to work
        // reasonably well as a default.
        
    Logger.info("\n");
    

    }    
  
}
