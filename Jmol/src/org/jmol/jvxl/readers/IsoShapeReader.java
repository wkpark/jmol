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

import javax.vecmath.Point3f;

class IsoShapeReader extends VolumeDataReader {

  IsoShapeReader(SurfaceGenerator sg, float radius) {
    super(sg);
    sphere_radiusAngstroms = radius;    
  }
  
  IsoShapeReader(SurfaceGenerator sg) {
    super(sg);
  }

  IsoShapeReader(SurfaceGenerator sg, int n, int l, int m, float z_eff) {
    super(sg);
    psi_n = n;
    psi_l = l;
    psi_m = m;
    psi_Znuc = z_eff;    
    sphere_radiusAngstroms = 0;
  }

  boolean allowNegative = true;
  
  float lobe_sizeAngstroms = 1.0f;
  int lobe_gridMax = 21;
  int lobe_ptsPerAngstrom = 10;


  int psi_gridMax = 40;
  float psi_ptsPerAngstrom = 5f;
  float psi_radiusAngstroms;

  double[] rfactor = new double[10];
  double[] pfactor = new double[10];

  final static double A0 = 0.52918f; //x10^-10 meters

  final static double ROOT2 = 1.414214;
  int psi_n = 2;
  int psi_l = 1;
  int psi_m = 1;
  float psi_Znuc = 1; // hydrogen

  int sphere_gridMax = 20;
  float sphere_ptsPerAngstrom = 10f;
  float sphere_radiusAngstroms;

  
  void setup() {
    float radius;
    int maxGrid;
    float ppa;
    if (center.x == Float.MAX_VALUE)
      center.set(0, 0, 0);
    switch (dataType) {
    case Parameters.SURFACE_ATOMICORBITAL:
      setHeader("hydrogen-like orbital\n");
      calcFactors(psi_n, psi_l, psi_m);
      radius = psi_radiusAngstroms = autoScaleOrbital();
      maxGrid = psi_gridMax;
      ppa = psi_ptsPerAngstrom;
      break;
    case Parameters.SURFACE_LOBE:
      allowNegative = false;
      radius = lobe_sizeAngstroms * 1.1f * eccentricityRatio
          * eccentricityScale;
      if (eccentricityScale > 0 && eccentricityScale < 1)
        radius /= eccentricityScale;
      ppa = lobe_ptsPerAngstrom;
      maxGrid = lobe_gridMax;
      setHeader("lobe\n");
      calcFactors(psi_n, psi_l, psi_m);
      break;
    //case Parameters.SURFACE_SPHERE:
    //case Parameters.SURFACE_ELLIPSOID:
    default:
      radius = sphere_radiusAngstroms * 1.1f * eccentricityScale;
      ppa = sphere_ptsPerAngstrom;
      maxGrid = sphere_gridMax;
      setHeader("sphere\n");
      break;
    }
    setVoxelRange(0, -radius, radius, ppa, maxGrid);
    setVoxelRange(1, -radius, radius, ppa, maxGrid);
    if (allowNegative)
      setVoxelRange(2, -radius, radius, ppa, maxGrid);
    else
      setVoxelRange(2, 0, radius / eccentricityRatio, ppa, maxGrid);
  }

  float getValue(int x, int y, int z) {
    volumeData.voxelPtToXYZ(x, y, z, ptPsi);
    getCalcPoint(ptPsi);
    float value = (float) hydrogenAtomPsiAt(ptPsi, psi_n, psi_l, psi_m);
    value = (sphere_radiusAngstroms > 0 ? sphere_radiusAngstroms - value : value >= 0 || allowNegative ? value : 0);
    return value;
  }

  void setHeader(String line1) {
    jvxlFileHeaderBuffer = new StringBuffer(line1);
    if(sphere_radiusAngstroms > 0) {
    jvxlFileHeaderBuffer.append(" rad=").append(sphere_radiusAngstroms);
    }else{
      jvxlFileHeaderBuffer
      .append(" n=").append(psi_n)
      .append(", l=").append(psi_l)
      .append(", m=").append(psi_m)
      .append(" Znuc=").append(psi_Znuc)
      .append(" res=").append(psi_ptsPerAngstrom)
      .append(" rad=").append(psi_radiusAngstroms);
    }
    jvxlFileHeaderBuffer.append(
            isAnisotropic ? " anisotropy=(" + anisotropy[0] + ","
                + anisotropy[1] + "," + anisotropy[2] + ")\n" : "\n");
    JvxlReader.jvxlCreateHeader(null, null, volumeData, Integer.MAX_VALUE,
        jvxlFileHeaderBuffer);
    atomCount = 0;
    negativeAtomCount = false;
  }
  
  float autoScaleOrbital() {
    float w = (psi_n * (psi_n + 3) - 5f) / psi_Znuc;
    if (w < 1)
      w = 1;
    if (psi_n < 3)
      w += 1;
    float aMax = 0;
    if (!isAnisotropic)
      return w;
    for (int i = 3; --i >= 0;)
      if (anisotropy[i] > aMax)
        aMax = anisotropy[i];
    return w * aMax;
  }


  static float[] fact = new float[20];
  static {
    fact[0] = 1;
    for (int i = 1; i < 20; i++)
      fact[i] = fact[i - 1] * i;
  }

  void calcFactors(int n, int el, int m) {
    int abm = Math.abs(m);
    double Nnl = Math.pow(2 * psi_Znuc / n / A0, 1.5)
        * Math.sqrt(fact[n - el - 1] / 2 / n / Math.pow(fact[n + el], 3));
    double Lnl = fact[n + el] * fact[n + el];
    double Plm = Math.pow(2, -el) * fact[el] * fact[el + abm]
        * Math.sqrt((2 * el + 1) * fact[el - abm] / 2 / fact[el + abm]);

    for (int p = 0; p <= n - el - 1; p++)
      rfactor[p] = Nnl * Lnl / fact[p] / fact[n - el - p - 1]
          / fact[2 * el + p + 1];
    for (int p = abm; p <= el; p++)
      pfactor[p] = Math.pow(-1, el - p) * Plm / fact[p] / fact[el + abm - p]
          / fact[el - p] / fact[p - abm];
  }

  final Point3f ptPsi = new Point3f();

  double hydrogenAtomPsiAt(Point3f pt, int n, int el, int m) {
    // ref: http://www.stolaf.edu/people/hansonr/imt/concept/schroed.pdf
    int abm = Math.abs(m);
    double x2y2 = pt.x * pt.x + pt.y * pt.y;
    double r2 = x2y2 + pt.z * pt.z;
    double r = Math.sqrt(r2);
    double rho = 2d * psi_Znuc * r / n / A0;
    double ph, th, cth, sth;
    double theta_lm = 0;
    double phi_m = 0;
    double sum = 0;
    for (int p = 0; p <= n - el - 1; p++)
      sum += Math.pow(-rho, p) * rfactor[p];
    double rnl = Math.exp(-rho / 2) * Math.pow(rho, el) * sum;
    ph = Math.atan2(pt.y, pt.x);
    th = Math.atan2(Math.sqrt(x2y2), pt.z);
    cth = Math.cos(th);
    sth = Math.sin(th);
    sum = 0;
    for (int p = abm; p <= el; p++)
      sum += Math.pow(1 + cth, p - abm) * Math.pow(1 - cth, el - p)
          * pfactor[p];
    theta_lm = Math.abs(Math.pow(sth, abm)) * sum;
    if (m == 0)
      phi_m = 1;
    else if (m > 0)
      phi_m = Math.cos(m * ph) * ROOT2;
    else
      phi_m = Math.sin(-m * ph) * ROOT2;
    if (Math.abs(phi_m) < 0.0000000001)
      phi_m = 0;
    return rnl * theta_lm * phi_m;
  }
}
