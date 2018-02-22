/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.more;

import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import javajs.util.BS;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import javajs.util.SB;
import javajs.util.P3;



/**
 * DCD binary trajectory file reader.
 * see http://www.ks.uiuc.edu/Research/vmd/plugins/molfile/dcdplugin.html
 * and http://www.ks.uiuc.edu/Research/namd/mailing_list/namd-l/5651.html
 * Bob Hanson 2/18/2011
 * 
 * requires PDB file
 * 
 *  load trajectory "c:/temp/t.pdb" coord "c:/temp/t.dcd"
 * 
 */

public class BinaryDcdReader extends AtomSetCollectionReader {

  @Override
  protected void setup(String fullPath, Map<String, Object> htParams, Object reader) {
    isBinary = true;
    requiresBSFilter = true;
    setupASCR(fullPath, htParams, reader);
  }

  @Override
  protected void initializeReader() {
    initializeTrajectoryFile();
    asc.setInfo("ignoreUnitCell", Boolean.TRUE);
  }


//dcd trajectory format
//
//From: Jianhui Tian (jianhuitian_at_gmail.com)
//Date: Mon Apr 30 2007 - 11:26:31 CDT
//
//*Hi all,*
//**
//*I am writting my own fortran code to read the dcd file written by NAMD 2.6.
//I am confused about the binary format now. I find one description about the
//format(what are written there) at:*
//http://www-bio.unizh.ch/docu/acc_docs/doc/charmm_principles/Ch04_mol_dyn.FM5.html
//
//The format of the binary trajectory (DCD) file is illustrated in the
//following example:
//
//   - For the first coordinate set:
//
//HDR,ICNTRL ! character*4 HDR, integer
//ICNTRL(20)NTITL,(TITLE(J),J=1,NTITL) ! integer NTITL, CHARACTER*80
//TITLE(*)
//NATOM ! integer NATOM
//
//   - If fixed atoms exist (for example, NFREAT.NE.NATOM), an extra record
//   exists with the list of free (movable) atoms:
//
//        FREEAT(I),I=1,NFREAT) ! INTEGER FREEAT(*)
//
//   - If this is crystal-type calculation (for example, a constant
//   pressure job), an extra record exists with unit cell parameters in
//   lower-triangle form:
//
//XTLABC ! REAL*8 XTLABC(6)
//(X(I),I=1,NATOM) ! real X(NATOM)
//(Y(I),I=1,NATOM) ! real Y(NATOM)
//(Z(I),I=1,NATOM) ! real Z(NATOM)
//
// For all subsequent coordinate sets, only movable atoms are written.
//
//   - If this is a crystal-type calculation (such as a constant pressure
//   job), an extra record exists with unit cell parameters in lower-triangle
//   form:
//
//XTLABC
//(X(FREEAT(I)),I=1,NFREAT)
//(Y(FREEAT(I)),I=1,NFREAT)
//(Z(FREEAT(I)),I=1,NFREAT)
//
// Where HDR = `CORD' or `VELD' for coordinates and velocities, respectively:
//
//ICNTRL(1)=NFILE ! number of frames in trajectory file
//ICNTRL(2)=NPRIV ! number of steps in previous run
//ICNTRL(3)=NSAVC ! frequency of saving
//ICNTRL(4)=NSTEP ! total number of steps
//NFILE=NSTEP/NSAVC
//ICNTRL(8)=NDEGF ! number of degrees of freedom
//ICNTRL(9)=NATOM-NFREAT ! number of fixed atoms
//ICNTRL(10)=DELTA4 ! coded time step
//ICNTRL(11)=stoi(XTLTYP,ALPHABET) ! coded crystallographic
//! group (or zero)
//ICNTRL(20)=VERNUM ! version number
//
// All other entries (12-19) are zero.
//*You can see, the format here writes the whole file "in many lines".*
//*Instead, my dcd file here writes everything in "one single line", which
//means you can only use "read(unit)" once.* *Could anyone please clarify for
//me and tell me what are writting sequentially in the binary file? Thanks a
//lot.*
//
//*Jianhui*
//
//This archive was generated by hypermail 2.1.6 : Wed Feb 29 2012 - 05:20:11 CST

//  There's a description of the X-PLOR variation of the DCD format here:
//
//  The DCD format is structured as follows (FORTRAN UNFORMATTED, with Fortran data type descriptions):
//
//  HDR     NSET    ISTRT   NSAVC   5-ZEROS NATOM-NFREAT    DELTA   9-ZEROS
//  `CORD'  #files  step 1  step    zeroes  (zero)          timestep  (zeroes)
//                          interval
//  C*4     INT     INT     INT     5INT    INT             DOUBLE  9INT
//  ==========================================================================
//  NTITLE          TITLE
//  INT (=2)        C*MAXTITL
//                  (=32)
//  ==========================================================================
//  NATOM
//  #atoms
//  INT
//  ==========================================================================
//  X(I), I=1,NATOM         (DOUBLE)
//  Y(I), I=1,NATOM         
//  Z(I), I=1,NATOM         
//  ==========================================================================
// T\0 \0\0 
//5400 0000 4-byte HEADER
//C O  R D  
//434F 5244 
//
// Where HDR = `CORD' or `VELD' for coordinates and velocities, respectively:
//
//ICNTRL(1)=NFILE ! number of frames in trajectory file
//ICNTRL(2)=NPRIV ! number of steps in previous run
//ICNTRL(3)=NSAVC ! frequency of saving
//ICNTRL(4)=NSTEP ! total number of steps
//NFILE=NSTEP/NSAVC
//ICNTRL(8)=NDEGF ! number of degrees of freedom
//ICNTRL(9)=NATOM-NFREAT ! number of fixed atoms
//ICNTRL(10)=DELTA4 ! coded time step
//ICNTRL(11)=stoi(XTLTYP,ALPHABET) ! coded crystallographic
//! group (or zero)
//ICNTRL(20)=VERNUM ! version number 

  private int nModels;
  private int nAtoms;
  private int nFree;
  private BS bsFree;
  private float[] xAll, yAll, zAll;
  private int crystGroup;
  

  @Override
  protected void processBinaryDocument() throws Exception {
    byte[] bytes = new byte[40];
    
    // read DCD header
    
    binaryDoc.setStream(null, binaryDoc.readInt() == 0x54);
    binaryDoc.readInt(); // "CORD"
    nModels = binaryDoc.readInt();
    int nPriv = binaryDoc.readInt();
    int nSaveC = binaryDoc.readInt();
    int nStep = binaryDoc.readInt();
    binaryDoc.readInt();
    binaryDoc.readInt();
    binaryDoc.readInt();
    int ndegf = binaryDoc.readInt();
    nFree = ndegf / 3;
    int nFixed = binaryDoc.readInt();
    int delta4 = binaryDoc.readInt();
    crystGroup = binaryDoc.readInt();
    binaryDoc.readByteArray(bytes, 0, 32);
    /* int nTitle = */ binaryDoc.readInt();
    binaryDoc.readInt();  // TRAILER
    
    // read titles
    
    binaryDoc.readInt();  // HEADER
    SB sb = new SB();
    for (int i = 0, n = binaryDoc.readInt(); i < n; i++)
      sb.append(trimString(binaryDoc.readString(80))).appendC('\n');
    binaryDoc.readInt(); // TRAILER
    Logger.info("BinaryDcdReadaer:\n" + sb);

    // read number of atoms and free-atom list
    
    binaryDoc.readInt(); // HEADER
    nAtoms = binaryDoc.readInt();
    binaryDoc.readInt(); // TRAILER
    nFree = nAtoms - nFixed;
    if (nFixed != 0) {
      // read list of free atoms
      binaryDoc.readInt(); // HEADER
      bsFree = BS.newN(nFree);
      for (int i = 0; i < nFree; i++)
        bsFree.set(binaryDoc.readInt() - 1);
      binaryDoc.readInt(); // TRAILER
      Logger.info("free: " + bsFree.cardinality() + " " + Escape.eBS(bsFree));
    }
    readCoordinates();
    
    Logger.info("Total number of trajectory steps=" + trajectorySteps.size());
  }

  private String trimString(String s) {
    int pt = s.indexOf('\0');
    if (pt >= 0)
      s = s.substring(0, pt);
    return s.trim();
  }

  private float[] readFloatArray() throws Exception {
    int n = binaryDoc.readInt() / 4; // HEADER
    float[] data = new float[n];
    for (int i = 0; i < n; i++)
      data[i] = binaryDoc.readFloat();
    binaryDoc.readInt();// TRAILER
    return data;
  }

  private double[] readDoubleArray() throws Exception {
    int n = binaryDoc.readInt() / 8; // HEADER
    double[] data = new double[n];
    for (int i = 0; i < n; i++)
      data[i] = binaryDoc.readDouble();
    binaryDoc.readInt(); // TRAILER
    return data;
  }

  private void readCoordinates() throws Exception {
    int ac = (bsFilter == null ? templateAtomCount : ((Integer) htParams
        .get("filteredAtomCount")).intValue());
    for (int i = 0; i < nModels; i++)
      if (doGetModel(++modelNumber, null)) {
        P3[] trajectoryStep = new P3[ac];
        if (!getTrajectoryStep(trajectoryStep))
          return;
        trajectorySteps.addLast(trajectoryStep);
        if (isLastModel(modelNumber))
          return;
      } else {
        if (crystGroup > 0)
            readDoubleArray();        
        readFloatArray();
        readFloatArray();
        readFloatArray();
      }
  }

  private boolean getTrajectoryStep(P3[] trajectoryStep)
      throws Exception {
    try {
    int ac = trajectoryStep.length;
    int n = -1;
    if (crystGroup > 0)
      calcUnitCell( readDoubleArray());
    float[] x = readFloatArray();
    float[] y = readFloatArray();
    float[] z = readFloatArray();
    BS bs = (xAll == null ? null : bsFree);
    if (bs == null) {
      xAll = x;
      yAll = y;
      zAll = z;
    }
    for (int i = 0, vpt = 0; i < nAtoms; i++) {
      P3 pt = new P3();
      if (bs == null || bs.get(i)) {
        pt.set(x[vpt], y[vpt], z[vpt]);
        vpt++;
      } else {
        pt.set(xAll[i], yAll[i], zAll[i]);
      }
      if (bsFilter == null || bsFilter.get(i)) {
        if (++n == ac)
          return true;
        trajectoryStep[n] = pt;
      }
    }
    return true;
    } catch (Exception e) {
      return false;
    }
  }

  private float[] calcUnitCell(double[] abc) {
    
// from openmm/wrappers/python/simtk/openmm/app/dcdfile.py
// https://github.com/pandegroup/openmm.git
//
//    (a_length, b_length, c_length, alpha, beta, gamma) = computeLengthsAndAngles(boxVectors)
//    a_length = a_length * 10.  # computeLengthsAndAngles returns unitless nanometers, but need angstroms here.
//    b_length = b_length * 10.  # computeLengthsAndAngles returns unitless nanometers, but need angstroms here.
//    c_length = c_length * 10.  # computeLengthsAndAngles returns unitless nanometers, but need angstroms here.
//    angle1 = math.sin(math.pi/2-gamma)
//    angle2 = math.sin(math.pi/2-beta)
//    angle3 = math.sin(math.pi/2-alpha)
//    file.write(struct.pack('<i6di', 48, a_length, angle1, b_length, angle2, angle3, c_length, 48))

    double a = abc[0];
    double angle1   = abc[1];
    double b = abc[2];
    double angle2   = abc[3];
    double angle3   = abc[4];
    double c = abc[5];
    double alpha = (Math.PI/2-Math.asin(angle3)) * 180/Math.PI;
    double beta = (Math.PI/2-Math.asin(angle2)) * 180/Math.PI;
    double gamma = (Math.PI/2-Math.asin(angle1)) * 180/Math.PI;

    System.out.println("unitcell:[" + a + " " + b + " " + c + " " + alpha + " " + beta + " " + gamma + "]");
    
    return new float[] {(float) a, (float) b, (float) c, (float) alpha, (float) beta, (float) gamma};
    
  }

}
