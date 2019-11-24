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
 *
 * distanceJMolecule.java
 *
 * Created on 03 April 2006, 17:05
 *
 */

package org.openscience.jmol.app.janocchio;

import java.util.Vector;

/**
 * 
 * @author u6x8497
 */
abstract public class DistanceJMolecule {

  protected NMR_Viewer viewer;

  protected Vector<Double> distances = new Vector<Double>();
  protected Vector<Double> couples = new Vector<Double>();
  protected Vector<DihedralCouple> couplesWhole = new Vector<DihedralCouple>();
  protected NoeMatrix noeMatrix;
  protected double[][] noeM;
  protected int[] indexAtomInNoeMatrix;
  protected String CHequation = "was";

  abstract protected DihedralCouple getDihedralCouple(int numAtom1,
                                                      int numAtom2,
                                                      int numAtom3, int numAtom4);

  public Vector<Double> getDistances() {
    return distances;
  }

  public Vector<Double> getCouples() {
    return couples;
  }

  public double[][] getNoeM() {
    return noeM;
  }

  public void calcNOEs() {

    try {
      noeM = noeMatrix.calcNOEs();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  // Adds couple to the list whihc will be output. USed in command line tool cdkReade
  public void addCouple(Integer[] numAtom1, Integer[] numAtom2,
                        Integer[] numAtom3, Integer[] numAtom4) {
    DihedralCouple dihe = getDihedralCouple(numAtom1[0].intValue(),
        numAtom2[0].intValue(), numAtom3[0].intValue(), numAtom4[0].intValue());
    double jvalue = dihe.getJvalue();
    if (Double.isNaN(jvalue))
      return;
    couples.add(new Double(jvalue));
    couplesWhole.add(dihe);
  }

  public void addCouple(int a1, int a2, int a3, int a4) {
    DihedralCouple dihe = getDihedralCouple(a1, a2, a3, a4);
    double jvalue = dihe.getJvalue();
    if (Double.isNaN(jvalue))
      return;
    couples.add(new Double(jvalue));
    couplesWhole.add(dihe);
  }

  // Returns couple does not add to output list. Used in NMR viewer applet 
  public Double[] calcCouple(Integer[] numAtom1, Integer[] numAtom2,
                             Integer[] numAtom3, Integer[] numAtom4) {
    DihedralCouple dihe = getDihedralCouple(numAtom1[0].intValue(),
        numAtom2[0].intValue(), numAtom3[0].intValue(), numAtom4[0].intValue());
    //double jvalue = dihe.getJvalue();

    double jvalue = dihe.getJvalue();
    if (Double.isNaN(jvalue))
      return null;
    Double[] dihecouple = new Double[2];
    dihecouple[0] = new Double(dihe.getTheta());
    dihecouple[1] = new Double(jvalue);
    return dihecouple;
  }

  // Returns couple does not add to output list. Used in NMR viewer applet 
  public Double[] calcCouple(int a1, int a2, int a3, int a4) {
    DihedralCouple dihe = getDihedralCouple(a1, a2, a3, a4);

    //double jvalue = dihe.getJvalue();

    Double[] dihecouple = new Double[2];
    double jvalue = dihe.getJvalue();
    if (Double.isNaN(jvalue))
      return null;
    dihecouple[0] = new Double(dihe.getTheta());
    dihecouple[1] = new Double(jvalue);
    return dihecouple;
  }

  public void setCorrelationTime(double t) {
    noeMatrix.setCorrelationTime(t);
  }

  /**
   * sets the mixing time for the NOE experiment
   * 
   * @param t
   *        the mixing time in seconds. Typically 0.5-1.5 seconds for small
   *        molecules
   */
  public void setMixingTime(double t) {
    noeMatrix.setMixingTime(t);
  }

  /**
   * set the NMR frequency for the NOE simulation
   * 
   * @param f
   *        the frequency in MHz
   */
  public void setNMRfreq(double f) {
    noeMatrix.setNMRfreq(f);
  }

  /**
   * sets the cutoff distance beyond which atom interactions are not considered
   * 
   * @param c
   *        the cutoff distance in Angstroms
   */
  public void setCutoff(double c) {
    noeMatrix.setCutoff(c);
  }

  public void setRhoStar(double c) {
    noeMatrix.setRhoStar(c);
  }

  public void setNoesy(boolean b) {
    noeMatrix.setNoesy(b);
  }

  public void setCHequation(String eq) {
    this.CHequation = eq;
  }

  public double getNoe(int a, int b) {
    int indexa = indexAtomInNoeMatrix[a];
    int indexb = indexAtomInNoeMatrix[b];
    double noe = noeM[indexa][indexb];
    return noe;
  }

  public int[] getIndexAtomInNoeMatrix() {
    return indexAtomInNoeMatrix;
  }

  public void addDistance(int a, int b) {

    // Get distance from NOE matrix. This will do Tropp averaging for methyl groups
    int indexa = indexAtomInNoeMatrix[a];
    int indexb = indexAtomInNoeMatrix[b];
    double d = noeMatrix.getDistance(indexa, indexb);
    distances.add(new Double(d));

  }

  public double calcDistance(Integer[] numa, Integer[] numb) {

    // Get distance from NOE matrix. This will do Tropp averaging for methyl groups
    int indexa = indexAtomInNoeMatrix[numa[0].intValue()];
    int indexb = indexAtomInNoeMatrix[numb[0].intValue()];
    double d = noeMatrix.getDistance(indexa, indexb);
    return d;
  }

  public double calcDistance(int a, int b) {

    // Get distance from NOE matrix. This will do Tropp averaging for methyl groups
    int indexa = indexAtomInNoeMatrix[a];
    int indexb = indexAtomInNoeMatrix[b];
    double d = noeMatrix.getDistance(indexa, indexb);
    return d;
  }

  abstract protected class DihedralCouple {

    protected double theta;
    protected double jvalue = Double.NaN;

    public DihedralCouple() {
    }

    public double getJvalue() {
      return this.jvalue;
    }

    public double getTheta() {
      return this.theta;
    }

  }

}
