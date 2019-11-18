/*
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 *
 *  It is derived in part from Jmol
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol.app.janocchio;

import javax.swing.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.NumberFormat;

/**
 * Class for calculating NOE intensities by full matrix relaxation approach.
 * <p>
 * 
 * create an instance of the class:
 * <p>
 * 
 * NoeMatrix n = new NoeMatrix();
 * <p>
 * 
 * Create the atom list:
 * <p>
 * 
 * n.makeAtomList(x);
 * <p>
 * 
 * where x is the number of atoms (methyls count as 1). add the atoms in turn
 * with:
 * <p>
 * 
 * n.addAtom(x,y,z);
 * <p>
 * 
 * where x y and z are the atom coordinates, or:
 * <p>
 * 
 * n.addMethyl(x,y,z,x1,y1,z1,x2,y2,z2);
 * <p>
 * 
 * which does the same for a methyl. T
 * <p>
 * 
 * hen just call calcNOEs:
 * <p>
 * 
 * double[][] results = n.calcNOEs();
 * <p>
 * 
 * This will need to be in a try statement as this routine throws an exception
 * if the atoms have not been set up properly.
 * 
 * @author YE91009
 * @created 28 February 2007
 */
public class NoeMatrix {
  double[][] eigenValues;
  double[][] eigenVectors;
  double[][] relaxMatrix;
  double[][] noeMatrix;
  double[][] distanceMatrix;
  Atoms[] atoms;
  int nAtoms, atomCounter, i, j, k, m, n, p, q;
  double tau, freq, tMix, cutoff, rhoStar;
  boolean noesychanged = true, tauchanged = true, mixchanged = true,
      freqchanged = true, cutoffchanged = true, rhoStarchanged = true,
      noesy = true;

  /** Creates a new instance of NoeMatrix */
  public NoeMatrix() {
    freq = 400 * 2 * Math.PI * 1E6;
    tau = 80E-12;
    tMix = 0.5;
    cutoff = 10;
    rhoStar = 0.1;
  }

  /**
   * set the correlation time to be used in the NOE calculation
   * 
   * @param t
   *        the correlation time in seconds. Typical value would be 80E-12.
   */
  public void setCorrelationTime(double t) {
    tau = t * 1E-12;
    tauchanged = true;
  }

  /**
   * sets the mixing time for the NOE experiment
   * 
   * @param t
   *        the mixing time in seconds. Typically 0.5-1.5 seconds for small
   *        molecules
   */
  public void setMixingTime(double t) {
    tMix = t;
    mixchanged = true;
  }

  /**
   * set the NMR frequency for the NOE simulation
   * 
   * @param f
   *        the frequency in MHz
   */
  public void setNMRfreq(double f) {
    freq = f * 2 * Math.PI * 1E6;
    freqchanged = true;
  }

  /**
   * sets the cutoff distance beyond which atom interactions are not considered
   * 
   * @param c
   *        the cutoff distance in Angstroms
   */
  public void setCutoff(double c) {
    cutoff = c;
    cutoffchanged = true;
  }

  public void setRhoStar(double c) {
    rhoStar = c;
    rhoStarchanged = true;
  }

  /**
   * sets the experiemnt type to NOESY or ROESY
   * 
   * @param b
   *        true for NOESY, flase for ROESY
   */
  public void setNoesy(boolean b) {
    noesy = b;
    noesychanged = true;
  }

  /**
   * get the correlation time in seconds
   * 
   * @return the correlation time in seconds
   */
  public double getCorrelationTime() {
    return tau;
  }

  /**
   * get the mixing time
   * 
   * @return the mixing time in seconds
   */
  public double getMixingTime() {
    return tMix;
  }

  /**
   * get if NOESY or ROESY was used for simulation
   * 
   * @return true for NOESY, false for ROESY
   */
  public boolean getNoesy() {
    return noesy;
  }

  /**
   * gets the NMR frequency
   * 
   * @return the NMR frequency in MHz
   */
  public double getNMRfreq() {
    return freq / 2 / Math.PI / 1E6;
  }

  /**
   * get the cutoff distance
   * 
   * @return the cutoff in Angstroms
   */
  public double getCutoff() {
    return cutoff;
  }

  private void doItAll() {

    setNMRfreq(500);
    setCorrelationTime(80.0);
    setMixingTime(0.5);
    setCutoff(10.0);
    setRhoStar(0.1);
    setNoesy(true);
    System.out.println("starting");
    readAtomsFromFile();
    relaxMatrix = new double[nAtoms][nAtoms];
    eigenValues = new double[nAtoms][nAtoms];
    eigenVectors = new double[nAtoms][nAtoms];
    noeMatrix = new double[nAtoms][nAtoms];
    System.out.println("read atoms: " + Integer.toString(nAtoms));
    calcRelaxMatrix();
    System.out.println("built matrix");
    System.out.println("total iterations = " + Integer.toString(Diagonalise()));
    System.out.println("diagonalised matrix");
    calcNoeMatrix();
    System.out.println("calculated NOE matrix");
    System.out.println(toString());
    System.out.println("");
    System.out.println(toStringNormRow());
    try {
      calcNOEs();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
    System.out.println(getNMRfreq());
    System.out.println(getCorrelationTime());
  }

  /**
   * calculate the NOESY spectrum at a particular mixing time. Onc eyou have
   * created and built the atom list, this is the only function you need to
   * call.
   * 
   * @return the 2D array of NOE intensities
   * @exception Exception
   *            Description of the Exception
   * @throws java.lang.Exception
   *         Throws an exception when the atom list has not been created or is
   *         not properly filled with atoms
   */
  public double[][] calcNOEs() throws Exception {
    if (nAtoms == 0 || atoms == null) {
      return new double[0][0];
    }
    if (nAtoms != atomCounter) {
      throw new Exception("Not all atoms have been read in yet!");
    }
    if (tauchanged || freqchanged || cutoffchanged || noesychanged
        || rhoStarchanged) {
      calcRelaxMatrix();
      Diagonalise();
    }
    if (noesychanged || tauchanged || freqchanged || cutoffchanged
        || mixchanged || rhoStarchanged) {
      calcNoeMatrix();
    }
    mixchanged = false;
    tauchanged = false;
    freqchanged = false;
    cutoffchanged = false;
    noesychanged = false;
    rhoStarchanged = false;
    return noeMatrix;
  }

  /**
   * create an empty atom list for subsequent population with atoms
   * 
   * @param n
   *        the number of atoms to be added
   */
  public void makeAtomList(int n) {
    nAtoms = n;
    atoms = new Atoms[nAtoms];
    atomCounter = 0;
    relaxMatrix = new double[nAtoms][nAtoms];
    eigenValues = new double[nAtoms][nAtoms];
    eigenVectors = new double[nAtoms][nAtoms];
    noeMatrix = new double[nAtoms][nAtoms];
    distanceMatrix = new double[nAtoms][nAtoms];
  }

  /**
   * add a proton to the atom list
   * 
   * @param x
   *        the x position of the atom (in Angstroms)
   * @param y
   *        the x position of the atom (in Angstroms)
   * @param z
   *        the z position of the atom (in Angstroms)
   */
  public void addAtom(double x, double y, double z) {
    atoms[atomCounter] = new Atoms();
    atoms[atomCounter].x = x;
    atoms[atomCounter].y = y;
    atoms[atomCounter].z = z;
    atoms[atomCounter].methyl = false;
    System.out.println("Here " + atomCounter + " " + x + " " + y + " " + z);
    atomCounter++;

  }

  /**
   * Add a methyl group to the atom list
   * 
   * @param x
   *        the x position of atom 1 (in Angstroms)
   * @param y
   *        the y position of atom 1 (in Angstroms)
   * @param z
   *        the z position of atom 1 (in Angstroms)
   * @param x1
   *        the x position of atom 2 (in Angstroms)
   * @param y1
   *        the y position of atom 2 (in Angstroms)
   * @param z1
   *        the z position of atom 2 (in Angstroms)
   * @param x2
   *        the x position of atom 3 (in Angstroms)
   * @param y2
   *        the y position of atom 3 (in Angstroms)
   * @param z2
   *        the z position of atom 3 (in Angstroms)
   */
  public void addMethyl(double x, double y, double z, double x1, double y1,
                        double z1, double x2, double y2, double z2) {
    atoms[atomCounter] = new Atoms();
    atoms[atomCounter].x = x;
    atoms[atomCounter].y = y;
    atoms[atomCounter].z = z;
    atoms[atomCounter].x1 = x1;
    atoms[atomCounter].y1 = y1;
    atoms[atomCounter].z1 = z1;
    atoms[atomCounter].x2 = x2;
    atoms[atomCounter].y2 = y2;
    atoms[atomCounter].z2 = z2;
    atoms[atomCounter].methyl = true;
    atomCounter++;
  }

  public void addEquiv(double[] xa, double[] ya, double[] za) {
    atoms[atomCounter] = new Atoms();
    atoms[atomCounter].xa = xa;
    atoms[atomCounter].ya = ya;
    atoms[atomCounter].za = za;
    atoms[atomCounter].equiv = true;
    atomCounter++;
  }

  private void calcRelaxMatrix() {

    double alpha = 5.6965E10;
    double rho;
    double JValSigma;
    double JValRho;
    if (noesy) {
      JValSigma = 6.0 * J(2 * freq) - J(0);
      JValRho = 6.0 * J(2 * freq) + 3.0 * J(freq) + J(0);
    } else {
      JValSigma = 3.0 * J(freq) + 2.0 * J(0);
      JValRho = 3.0 * J(2 * freq) + 4.5 * J(freq) + 2.5 * J(0);
    }
    for (i = 0; i < nAtoms; i++) {
      rho = 0.0;
      for (j = 0; j < nAtoms; j++) {
        // double distSqrd = (atoms[i].x-atoms[j].x)*(atoms[i].x-atoms[j].x) + (atoms[i].y-atoms[j].y)*(atoms[i].y-atoms[j].y) + (atoms[i].z-atoms[j].z)*(atoms[i].z-atoms[j].z);
        //System.out.println(distSqrd);
        double distSqrd = distanceSqrd(atoms[i], atoms[j]);
        distanceMatrix[i][j] = Math.sqrt(distSqrd);
        double aOverR6;
        if (distSqrd < cutoff * cutoff) {
          aOverR6 = alpha / (distSqrd * distSqrd * distSqrd);
        } else {
          aOverR6 = 0;
        }
        if (i < j) {
          relaxMatrix[i][j] = aOverR6 * JValSigma;
          relaxMatrix[j][i] = relaxMatrix[i][j];
        }
        if (i != j) {
          rho = rho + aOverR6 * JValRho;
        }
      }
      relaxMatrix[i][i] = rho + rhoStar;
      //System.out.println(rho);
    }
  }

  private double J(double w) {
    double J = tau / (1 + (w * w * tau * tau));
    return J;
  }

  private void readAtomsFromFile() {
    atoms = new Atoms[200];
    nAtoms = 0;
    try {
      //BufferedReader br = new BufferedReader(new FileReader(new File("C:\\noeprom\\minlac.out")));
      BufferedReader br = new BufferedReader(new FileReader(new File(
          "/home/u6x8497/pyrrolidine_trial/pyrr_3d.dat")));
      br.readLine();
      System.out.println("found file");
      while (true) {
        //br.readLine();
        String[] linetokens = br.readLine().split("\\s+");
        //System.out.println("checking " + Integer.toString(linetokens.length) + linetokens[22]);

        //if (linetokens[22].matches(".*H.*")) {
        // DAE changed to match particular H atom types in mm files with no atom names
        if (linetokens[1].matches("41") || linetokens[1].matches("44")) {
          //System.out.println("found "  + linetokens[14]);
          atoms[nAtoms] = new Atoms();
          atoms[nAtoms].x = Double.valueOf(linetokens[14]).doubleValue();
          atoms[nAtoms].y = Double.valueOf(linetokens[15]).doubleValue();
          atoms[nAtoms].z = Double.valueOf(linetokens[16]).doubleValue();
          atoms[nAtoms].methyl = false;
          nAtoms++;
        }
      }
    } catch (Exception e) {
      System.out.println(e.toString());
    }

  }

  private int sign(double x) {
    if (x < 0) {
      return -1;
    }
    return 1;
  }

  private void calcNoeMatrix() {
    double[] tempEVs = new double[nAtoms];
    for (i = 0; i < nAtoms; i++) {
      tempEVs[i] = Math.exp(-eigenValues[i][i] * tMix);
    }
    for (i = 0; i < nAtoms; i++) {
      for (j = 0; j <= i; j++) {
        double sum = 0;
        for (k = 0; k < nAtoms; k++) {
          sum += eigenVectors[i][k] * eigenVectors[j][k] * tempEVs[k];
        }
        noeMatrix[i][j] = sum;
        noeMatrix[j][i] = sum;
        //System.err.println("Here " + noeMatrix[i][j]);
      }
    }
  }

  private int Diagonalise() {

    int iter = 0;

    for (int i = 0; i < nAtoms; i++) {
      for (int z = 0; z < nAtoms; z++) {
        eigenVectors[i][z] = 0.0;
        eigenValues[i][z] = relaxMatrix[i][z];
      }
    }

    for (int i = 0; i < nAtoms; i++) {
      eigenVectors[i][i] = 1.0;
    }

    String state = "ITERATING";
    //double tolerance = maxOffDiag()*0.0001;
    //System.out.println("tolerance = " + Double.toString(tolerance));
    int maxIter = 100000;

    while (state == "ITERATING") {
      double max = maxOffDiag();
      if (max > 0.0) {
        rotate();
        //System.out.println("iterating");
        iter++;
        if (iter >= maxIter) {
          state = "STOP";
          System.out.println("maximum iteration reached");
        }
      } else {
        state = "SUCCESS";

      }
    }
    return iter;
  }

  private double maxOffDiag() {
    double max = 0.0;
    for (int i = 0; i < nAtoms - 1; i++) {
      for (int j = i + 1; j < nAtoms; j++) {
        double aij = Math.abs(eigenValues[i][j]);
        if (aij > max) {
          max = aij;
          p = i;
          q = j;
        }
      }
    }
    return max;
  }

  //Rotates matrix matA through theta in pq-plane to set matA.re[p][q] = 0
  //Rotation stored in matrix matR whose columns are eigenvectors of matA
  private void rotate() {
    // d = cot 2*theta, t = tan theta, c = cos theta, s = sin theta
    double d = (eigenValues[p][p] - eigenValues[q][q])
        / (2.0 * eigenValues[p][q]);
    double t = sign(d) / (Math.abs(d) + Math.sqrt(d * d + 1));
    double c = 1.0 / Math.sqrt(t * t + 1);
    double s = t * c;
    eigenValues[p][p] += t * eigenValues[p][q];
    eigenValues[q][q] -= t * eigenValues[p][q];
    eigenValues[p][q] = eigenValues[q][p] = 0.0;
    for (int k = 0; k < nAtoms; k++) {// Transform eigenvalues
      if (k != p && k != q) {
        double akp = c * eigenValues[k][p] + s * eigenValues[k][q];
        double akq = -s * eigenValues[k][p] + c * eigenValues[k][q];
        eigenValues[k][p] = eigenValues[p][k] = akp;
        eigenValues[k][q] = eigenValues[q][k] = akq;
      }
    }
    for (int k = 0; k < nAtoms; k++) {// Store eigenvectors
      double rkp = c * eigenVectors[k][p] + s * eigenVectors[k][q];
      double rkq = -s * eigenVectors[k][p] + c * eigenVectors[k][q];
      eigenVectors[k][p] = rkp;
      eigenVectors[k][q] = rkq;
    }
  }

  public String toString() {
    StringBuffer sb;
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumFractionDigits(4);
    nf.setMaximumFractionDigits(4);
    sb = new StringBuffer();
    for (i = 0; i < nAtoms; i++) {
      for (j = 0; j < nAtoms; j++) {
        sb.append(nf.format(noeMatrix[i][j]) + "\t");

      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public String toStringNormRow() {
    StringBuffer sb;
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumFractionDigits(4);
    nf.setMaximumFractionDigits(4);
    sb = new StringBuffer();
    for (i = 0; i < nAtoms; i++) {
      for (j = 0; j < nAtoms; j++) {
        double val = noeMatrix[i][j] / noeMatrix[i][i];
        sb.append(nf.format(val) + "\t");

      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private double distanceSqrd(Atoms a, Atoms b) {

    Atoms atom1, atom2;
    double d, d1, d2, d3;
    double prod11, prod22, prod12, prod13, prod23, prod33;
    double d15, d25, d35;

    if (b.methyl && !a.methyl) {//exchange i and j atoms
      atom1 = b;
      atom2 = a;
    } else if (b.equiv && !a.equiv) {
      atom1 = b;
      atom2 = a;
    } else {
      atom1 = a;
      atom2 = b;
    }

    if (atom1.methyl) {// methyl group detected in atom i : use Tropp model
      double a2x, a2y, a2z;
      if (atom2.methyl) {// case of two methyl groups average coords of methyl j
        a2x = (atom2.x + atom2.x1 + atom2.x2) / 3.0;
        a2y = (atom2.y + atom2.y1 + atom2.y2) / 3.0;
        a2z = (atom2.z + atom2.z1 + atom2.z2) / 3.0;
      } else if (atom2.equiv) {//average coords of equivs
        a2x = 0.0;
        a2y = 0.0;
        a2z = 0.0;
        for (int j = 0; j < atom2.xa.length; j++) {
          a2x += atom2.xa[j] / atom2.xa.length;
          a2y += atom2.ya[j] / atom2.xa.length;
          a2z += atom2.za[j] / atom2.xa.length;
        }
      } else {//use normal coords
        a2x = atom2.x;
        a2y = atom2.y;
        a2z = atom2.z;
      }
      double x1 = atom1.x - a2x;
      double y1 = atom1.y - a2y;
      double z1 = atom1.z - a2z;
      double x2 = atom1.x1 - a2x;
      double y2 = atom1.y1 - a2y;
      double z2 = atom1.z1 - a2z;
      double x3 = atom1.x2 - a2x;
      double y3 = atom1.y2 - a2y;
      double z3 = atom1.z2 - a2z;

      d1 = (x1 * x1) + (y1 * y1) + (z1 * z1);
      d2 = (x2 * x2) + (y2 * y2) + (z2 * z2);
      d3 = (x3 * x3) + (y3 * y3) + (z3 * z3);

      d15 = d1 * d1 * Math.sqrt(d1);
      d25 = d2 * d2 * Math.sqrt(d2);
      d35 = d3 * d3 * Math.sqrt(d3);

      prod11 = x1 * x1 + y1 * y1 + z1 * z1;
      prod12 = x1 * x2 + y1 * y2 + z1 * z2;
      prod13 = x1 * x3 + y1 * y3 + z1 * z3;
      prod22 = x2 * x2 + y2 * y2 + z2 * z2;
      prod23 = x2 * x3 + y2 * y3 + z2 * z3;
      prod33 = x3 * x3 + y3 * y3 + z3 * z3;

      d = ((3 * (prod11 * prod11)) - (d1 * d1)) / (d15 * d15);
      d += ((3 * (prod12 * prod12)) - (d1 * d2)) / (d15 * d25);
      d += ((3 * (prod13 * prod13)) - (d1 * d3)) / (d15 * d35);
      d += ((3 * (prod12 * prod12)) - (d2 * d1)) / (d25 * d15);
      d += ((3 * (prod22 * prod22)) - (d2 * d2)) / (d25 * d25);
      d += ((3 * (prod23 * prod23)) - (d2 * d3)) / (d25 * d35);
      d += ((3 * (prod13 * prod13)) - (d3 * d1)) / (d35 * d15);
      d += ((3 * (prod23 * prod23)) - (d3 * d2)) / (d35 * d25);
      d += ((3 * (prod33 * prod33)) - (d3 * d3)) / (d35 * d35);
      //System.err.println("Hello "  + Math.pow(d/18,(-1.0/6.0)));
      return (Math.pow(d / 18.0, -1.0 / 3.0));
    } else if (atom1.equiv) {// equivalent atom - do r6 averaging
      if (atom2.equiv) {
        double dd = 0.0;
        for (int i = 0; i < atom1.xa.length; i++) {
          for (int j = 0; j < atom2.xa.length; j++) {
            double x1 = atom1.xa[i] - atom2.xa[j];
            double y1 = atom1.ya[i] - atom2.ya[j];
            double z1 = atom1.za[i] - atom2.za[j];
            dd += Math.pow((x1 * x1) + (y1 * y1) + (z1 * z1), -3.0);
          }
        }
        return Math.pow(dd / (atom1.xa.length * atom2.xa.length), -1.0 / 3.0);
      } else {
        double dd = 0.0;
        for (int i = 0; i < atom1.xa.length; i++) {
          double x1 = atom1.xa[i] - atom2.x;
          double y1 = atom1.ya[i] - atom2.y;
          double z1 = atom1.za[i] - atom2.z;
          dd += Math.pow((x1 * x1) + (y1 * y1) + (z1 * z1), -3.0);
        }
        return Math.pow(dd / atom1.xa.length, -1.0 / 3.0);
      }

    } else {// normal distance for non equivalent C-H and C-H2 groups
      double x1 = atom1.x - atom2.x;
      double y1 = atom1.y - atom2.y;
      double z1 = atom1.z - atom2.z;
      return (x1 * x1) + (y1 * y1) + (z1 * z1);
    }
  }

  public double getDistance(int i, int j) {
    // Method to return NOE distance, i.e. including Tropp averaging for methyl groups
    return distanceMatrix[i][j];
  }

  public static void main(String args[]) {
    NoeMatrix nm = new NoeMatrix();
    nm.doItAll();
  }
}

class Atoms {

  double x;
  double y;
  double z;
  boolean methyl;
  double x1, x2, y1, y2, z1, z2;
  boolean equiv;
  double[] xa, ya, za;

}
