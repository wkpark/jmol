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

import java.util.Random;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.util.Logger;
import org.jmol.util.Measure;

class IsoShapeReader extends VolumeDataReader {

  private int psi_n = 2;
  private int psi_l = 1;
  private int psi_m = 1;
  private float psi_Znuc = 1; // hydrogen
  private float sphere_radiusAngstroms;
  private int monteCarloCount;
  private Random random;

  IsoShapeReader(SurfaceGenerator sg, float radius) {
    super(sg);
    sphere_radiusAngstroms = radius;
  }

  IsoShapeReader(SurfaceGenerator sg, int n, int l, int m, float z_eff,
      int monteCarloCount) {
    super(sg);
    psi_n = n;
    psi_l = l;
    psi_m = m;
    psi_Znuc = z_eff;
    sphere_radiusAngstroms = 0;
    this.monteCarloCount = monteCarloCount;
  }

  private boolean allowNegative = true;

  private double[] rfactor = new double[10];
  private double[] pfactor = new double[10];

  private final static double A0 = 0.52918; //x10^-10 meters
  private final static double ROOT2 = 1.414214;

  private float radius;
  private final Point3f ptPsi = new Point3f();

  @Override
  protected void setup(boolean isMapData) {
    volumeData.sr = this; // we will provide point data for mapping
    precalculateVoxelData = false;
    if (center.x == Float.MAX_VALUE)
      center.set(0, 0, 0);
    String type = "sphere";
    switch (dataType) {
    case Parameters.SURFACE_ATOMICORBITAL:
      calcFactors(psi_n, psi_l, psi_m);
      autoScaleOrbital();
      ptsPerAngstrom = 5f;
      maxGrid = 40;
      type = "hydrogen-like orbital";
      if (monteCarloCount > 0) {
        vertexDataOnly = true;
        //params.colorDensity = true;
        random = new Random(params.randomSeed);
      }
      break;
    case Parameters.SURFACE_LONEPAIR:
    case Parameters.SURFACE_RADICAL:
      type = "lp";
      vertexDataOnly = true;
      radius = 0;
      ptsPerAngstrom = 1;
      maxGrid = 1;
      break;
    case Parameters.SURFACE_LOBE:
      allowNegative = false;
      calcFactors(psi_n, psi_l, psi_m);
      psi_normalization = 1;
      radius = 1.1f * eccentricityRatio * eccentricityScale;
      if (eccentricityScale > 0 && eccentricityScale < 1)
        radius /= eccentricityScale;
      ptsPerAngstrom = 10f;
      maxGrid = 21;
      type = "lobe";
      break;
    case Parameters.SURFACE_ELLIPSOID3:
      type = "ellipsoid(thermal)";
      radius = 3.0f * sphere_radiusAngstroms;
      ptsPerAngstrom = 10f;
      maxGrid = 22;
      break;
    case Parameters.SURFACE_ELLIPSOID2:
      type = "ellipsoid";
      // fall through
    case Parameters.SURFACE_SPHERE:
    default:
      radius = 1.2f * sphere_radiusAngstroms * eccentricityScale;
      ptsPerAngstrom = 10f;
      maxGrid = 22;
      break;
    }
    if (monteCarloCount == 0)
      setVolumeData();
    setHeader(type + "\n");
  }

  @Override
  protected void setVolumeData() {
    setVoxelRange(0, -radius, radius, ptsPerAngstrom, maxGrid);
    setVoxelRange(1, -radius, radius, ptsPerAngstrom, maxGrid);
    if (allowNegative)
      setVoxelRange(2, -radius, radius, ptsPerAngstrom, maxGrid);
    else
      setVoxelRange(2, 0, radius / eccentricityRatio, ptsPerAngstrom, maxGrid);
  }

  @Override
  public float getValue(int x, int y, int z, int ptyz) {
    volumeData.voxelPtToXYZ(x, y, z, ptPsi);    
    return getValueAtPoint(ptPsi);
  }

  @Override
  public float getValueAtPoint(Point3f pt) {
    ptTemp.set(pt);
    ptTemp.sub(center);
    if (isEccentric)
      eccentricityMatrixInverse.transform(ptTemp);
    if (isAnisotropic) {
      ptTemp.x /= anisotropy[0];
      ptTemp.y /= anisotropy[1];
      ptTemp.z /= anisotropy[2];
    }
    if (sphere_radiusAngstroms > 0) {
      if (params.anisoB != null) {

        return sphere_radiusAngstroms
            -

            (float) Math.sqrt(ptTemp.x * ptTemp.x + ptTemp.y * ptTemp.y
                + ptTemp.z * ptTemp.z)
            / (float) (Math.sqrt(params.anisoB[0] * ptTemp.x * ptTemp.x
                + params.anisoB[1] * ptTemp.y * ptTemp.y + params.anisoB[2]
                * ptTemp.z * ptTemp.z + params.anisoB[3] * ptTemp.x * ptTemp.y
                + params.anisoB[4] * ptTemp.x * ptTemp.z + params.anisoB[5]
                * ptTemp.y * ptTemp.z));
      }
      return sphere_radiusAngstroms
          - (float) Math.sqrt(ptTemp.x * ptTemp.x + ptTemp.y * ptTemp.y
              + ptTemp.z * ptTemp.z);
    }
    float value = (float) hydrogenAtomPsi(ptTemp);
    return (allowNegative || value >= 0 ? value : 0);
  }

  private void setHeader(String line1) {
    jvxlFileHeaderBuffer = new StringBuffer(line1);
    if (sphere_radiusAngstroms > 0) {
      jvxlFileHeaderBuffer.append(" rad=").append(sphere_radiusAngstroms);
    } else {
      jvxlFileHeaderBuffer.append(" n=").append(psi_n).append(", l=").append(
          psi_l).append(", m=").append(psi_m).append(" Znuc=").append(psi_Znuc)
          .append(" res=").append(ptsPerAngstrom).append(" rad=")
          .append(radius);
    }
    jvxlFileHeaderBuffer.append(isAnisotropic ? " anisotropy=(" + anisotropy[0]
        + "," + anisotropy[1] + "," + anisotropy[2] + ")\n" : "\n");
    JvxlCoder.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData,
        jvxlFileHeaderBuffer);
  }

  private final static float[] fact = new float[20];
  static {
    fact[0] = 1;
    for (int i = 1; i < 20; i++)
      fact[i] = fact[i - 1] * i;
  }

  private double psi_normalization = 4 * Math.PI; // not applied!

  private void calcFactors(int n, int el, int m) {
    int abm = Math.abs(m);
    double NnlLnl = Math.pow(2 * psi_Znuc / n / A0, 1.5)
        * Math.sqrt(fact[n - el - 1] * fact[n + el] / 2 / n);
    //double Lnl = fact[n + el] * fact[n + el];
    double Plm = Math.pow(2, -el) * fact[el] * fact[el + abm]
        * Math.sqrt((2 * el + 1) * fact[el - abm] / 2 / fact[el + abm]);

    for (int p = 0; p <= n - el - 1; p++)
      rfactor[p] = NnlLnl / fact[p] / fact[n - el - p - 1]
          / fact[2 * el + p + 1];
    for (int p = abm; p <= el; p++)
      pfactor[p] = Math.pow(-1, el - p) * Plm / fact[p] / fact[el + abm - p]
          / fact[el - p] / fact[p - abm];
  }

  private double aoMax;
  private double aoMax2;
  private double angMax2;
  
  private void autoScaleOrbital() {
    float r0 = 0;
    aoMax = 0;
    float rmax = 0;
    aoMax2 = 0;
    float rmax2 = 0;
    double d;
    for (radius = 100f; radius >= 0.1f; radius -= 0.1f) {
      d = Math.abs(radialPart(radius));
      System.out.println(radius + " " + d + " " + r0);
      if (d >= aoMax) {
        rmax = radius;
        aoMax = d;
      }
      d *= d * radius * radius;
      if (d >= aoMax2) {
        rmax2 = radius;
        aoMax2 = d;
      }
    }
    
    angMax2 = 0;
    for (float ang = 0; ang < 180; ang += 1) {
      double th = ang / (2 * Math.PI);
      d = Math.abs(angularPart(th, 0, 0));
      System.out.println(ang + "\t" + d);
      if (d > angMax2) {
        angMax2 = d;
      }
    }
    angMax2 *= angMax2; 
    if (psi_m != 0)
      angMax2 *= 2; // apparently...
    
    Logger.info("Atomic Orbital radial max = " + aoMax + " at " + rmax);
    Logger.info("Atomic Orbital r2R2 max = " + aoMax2 + " at " + rmax2);
    Logger.info("Atomic Orbital angular max = " + angMax2 * 4 * Math.PI);
    // (we don't apply the final 4pi here because it is just a constant)
    //aoMax *= Math.sqrt(psi_l - psi_m + 0.5);      
    //Logger.info("Atomic Orbital overall max = " + aoMax);

    double min;
    if (params.cutoff == 0) {
      min = (monteCarloCount > 0 ? aoMax * 0.01f : 0.01f);
    } else if (monteCarloCount > 0) {
      aoMax = Math.abs(params.cutoff);
      min = aoMax * 0.01f;
    } else {
      min = Math.abs(params.cutoff / 2);
      // ISSQUARED means cutoff is in terms of psi*2, not psi
      if (params.isSquared)
        min = Math.sqrt(min / 2);
    }
    for (radius = 100; radius >= 0.1f; radius -= 0.1f) {
      d = Math.abs(radialPart(radius));
      if (d >= min) {
        r0 = radius;
        break;
      }
    }
    radius = r0 + 1;
    if (isAnisotropic) {
      float aMax = 0;
      for (int i = 3; --i >= 0;)
        if (anisotropy[i] > aMax)
          aMax = anisotropy[i];
      radius *= aMax;
    }
    Logger.info("Atomic Orbital radial extent set to " + radius
        + " for cutoff " + params.cutoff);
  }

  private double radialPart(double r) {
    double rho = 2d * psi_Znuc * r / psi_n / A0;
    double sum = 0;
    for (int p = 0; p <= psi_n - psi_l - 1; p++)
      sum += Math.pow(-rho, p) * rfactor[p];
    return Math.exp(-rho / 2) * Math.pow(rho, psi_l) * sum;
  }

  private double rnl;

  private double hydrogenAtomPsi(Point3f pt) {
    // ref: http://www.stolaf.edu/people/hansonr/imt/concept/schroed.pdf
    double x2y2 = pt.x * pt.x + pt.y * pt.y;
    rnl = radialPart(Math.sqrt(x2y2 + pt.z * pt.z));
    double ph = Math.atan2(pt.y, pt.x);
    double th = Math.atan2(Math.sqrt(x2y2), pt.z);
    double theta_lm_phi_m = angularPart(th, ph, psi_m);
    return rnl * theta_lm_phi_m;
  }

  private double angularPart(double th, double ph, int m) {
    double cth = Math.cos(th);
    double sth = Math.sin(th);
    boolean isS = (m == 0 && psi_l == 0);
    int abm = Math.abs(m);
    double sum = 0;
    if (isS)
      sum = pfactor[0];
    else
      for (int p = abm; p <= psi_l; p++)
        sum += (p == abm ? 1 : Math.pow(1 + cth, p - abm))
            * (p == psi_l ? 1 : Math.pow(1 - cth, psi_l - p)) * pfactor[p];
    double theta_lm = (abm == 0 ? sum : Math.abs(Math.pow(sth, abm)) * sum);
    double phi_m;
    if (m == 0)
      return theta_lm;
    if (m > 0)
      phi_m = Math.cos(m * ph) * ROOT2;
    else
      phi_m = Math.sin(m * ph) * ROOT2;
    return (Math.abs(phi_m) < 0.0000000001 ? 0 : theta_lm * phi_m);
  }

  private boolean monteCarloDone;
  private int nTries;

  private void createMonteCarloOrbital() {
    if (monteCarloDone)
      return;
    boolean isS = (psi_m == 0 && psi_l == 0);
    monteCarloDone = true;
    float value;
    if (params.thePlane != null)
      vTemp = new Vector3f();
    float f = 0;
    float d = radius * 2;
    float rave = 0;
    if (params.thePlane == null) {
      f = (float) aoMax;
    } else {
      // we need an approximation  of the max value here
      for (int i = 0; i < 10000; i++) {
        setRandomPoint(d);
        value = (float) Math.abs(hydrogenAtomPsi(ptPsi));
        if (value > f)
          f = value;
      }
      if (f < 0.01f) // must be a node
        return;
    }
    f *= f;
    nTries = 0;
    for (int i = 0; i < monteCarloCount; nTries++) {
      // we do Pshemak's idea here -- force P(r2R2), then pick a random
      // point on the sphere for that radius
      if (params.thePlane == null) {
      double r = random.nextDouble() * radius;
      double rp = r * radialPart(r);
      if (rp * rp <= aoMax2 * random.nextDouble())
        continue;
      double u = random.nextDouble();        
      double v = random.nextDouble();        
      double theta = 2 * Math.PI * u;
      double cosPhi = 2 * v - 1;
      if (!isS) {
        double phi = Math.acos(cosPhi);
        double ap = angularPart(phi, theta, psi_m);
        if (ap * ap <= angMax2 * random.nextDouble())
          continue;
      }
      //http://mathworld.wolfram.com/SpherePointPicking.html
      double sinPhi = Math.sin(Math.acos(cosPhi));
      double x = r * Math.cos(theta) * sinPhi;
      double y = r * Math.sin(theta) * sinPhi;
      double z = r * cosPhi;
      //x = r; y = r2R2/aoMax2 * 10; z = 0;
      ptPsi.set((float)x, (float) y, (float) z);
      } else {
        setRandomPoint(d);
        ptPsi.add(center);
        value = getValueAtPoint(ptPsi);
        if (value * value <= f * random.nextFloat())
          continue;
      }
      ptPsi.add(center);
      value = getValueAtPoint(ptPsi);
      rave += ptPsi.distance(center);
      addVertexCopy(ptPsi, value, 0);
      i++;
    }
    Logger.info("Atomic Orbital mean radius = " + rave / monteCarloCount
        + " for " + monteCarloCount + " points (" + nTries
        + " tries using psi^2_max=" + f + " and r_max = " + d / 2 + ")");

    //        + " points within a " + d + "x" + d + "x" + d + " cube");
  }

  private Vector3f vTemp;
  private Point3f pt0 = new Point3f();

  private void setRandomPoint(float x) {
    do {
      nTries++;
      ptPsi.x = (random.nextFloat() - 0.5f) * x;
      ptPsi.y = (random.nextFloat() - 0.5f) * x;
      ptPsi.z = (random.nextFloat() - 0.5f) * x;
    } while (pt0.distance(ptPsi) * 2 > x);
    if (params.thePlane != null)
      Measure.getPlaneProjection(ptPsi, params.thePlane, ptPsi, vTemp);
  }

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    switch (params.dataType) {
    case Parameters.SURFACE_ATOMICORBITAL:
      if (monteCarloCount <= 0)
        break;
      createMonteCarloOrbital();
      return;
    case Parameters.SURFACE_LONEPAIR:
    case Parameters.SURFACE_RADICAL:
      ptPsi.set(0, 0, eccentricityScale / 2);
      eccentricityMatrixInverse.transform(ptPsi);
      ptPsi.add(center);
      addVertexCopy(center, 0, 0);
      addVertexCopy(ptPsi, 0, 0);
      addTriangleCheck(0, 0, 0, 0, 0, false, 0);
      return;
    }
    super.readSurfaceData(isMapData);
  }

}
