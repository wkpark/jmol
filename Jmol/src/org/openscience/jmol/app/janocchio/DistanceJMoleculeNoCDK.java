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

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;

import javajs.util.BS;
import javajs.util.V3;


/**
 * 
 * @author u6x8497
 */
public class DistanceJMoleculeNoCDK {
//
//  private Vector distances = new Vector();
//  private Vector couples = new Vector();
//  private Vector couplesWhole = new Vector();
////  private AtomContainer mol;
//  private NoeMatrix noeMatrix = new NoeMatrix();
//  private int nAtoms;
//  private double[][] noeM;
//  private int[] indexAtomInNoeMatrix;
//  private String[] labelArray;
//  private String CHequation = "was";
//  private NMR_Viewer viewer;
//
//  public DistanceJMoleculeNoCDK(NMR_Viewer viewer) {
//    this.viewer = viewer;
//  }
//
//  public DistanceJMoleculeNoCDK(NMR_Viewer viewer, String[] labelArray) {
//    this.viewer = viewer;
//    this.labelArray = labelArray;
//  }
//
//  public Vector getDistances() {
//    return distances;
//  }
//
//  public Vector getCouples() {
//    return couples;
//  }
//
//  public double[][] getNoeM() {
//    return noeM;
//  }
//
//  public void calcNOEs() {
//
//    this.indexAtomInNoeMatrix = addAtomstoMatrix();
//
//    try {
//      this.noeM = noeMatrix.calcNOEs();
//    } catch (Exception e) {
//      System.out.println(e.toString());
//    }
//  }
//
//  public void setCorrelationTime(double t) {
//    noeMatrix.setCorrelationTime(t);
//  }
//
//  /**
//   * sets the mixing time for the NOE experiment
//   * 
//   * @param t
//   *        the mixing time in seconds. Typically 0.5-1.5 seconds for small
//   *        molecules
//   */
//  public void setMixingTime(double t) {
//    noeMatrix.setMixingTime(t);
//  }
//
//  /**
//   * set the NMR frequency for the NOE simulation
//   * 
//   * @param f
//   *        the frequency in MHz
//   */
//  public void setNMRfreq(double f) {
//    noeMatrix.setNMRfreq(f);
//  }
//
//  /**
//   * sets the cutoff distance beyond which atom interactions are not considered
//   * 
//   * @param c
//   *        the cutoff distance in Angstroms
//   */
//  public void setCutoff(double c) {
//    noeMatrix.setCutoff(c);
//  }
//
//  public void setRhoStar(double c) {
//    noeMatrix.setRhoStar(c);
//  }
//
//  public void setNoesy(boolean b) {
//    noeMatrix.setNoesy(b);
//  }
//
//  public void setCHequation(String eq) {
//    this.CHequation = eq;
//  }
//
//  public double getNoe(int a, int b) {
//    int indexa = indexAtomInNoeMatrix[a];
//    int indexb = indexAtomInNoeMatrix[b];
//    double noe = noeM[indexa][indexb];
//    return noe;
//  }
//
//  public int[] getIndexAtomInNoeMatrix() {
//    return indexAtomInNoeMatrix;
//  }
//
//  public void addDistance(Integer[] numa, Integer[] numb) {
//
//    // Get distance from NOE matrix. This will do Tropp averaging for methyl groups
//    int indexa = indexAtomInNoeMatrix[numa[0].intValue()];
//    int indexb = indexAtomInNoeMatrix[numb[0].intValue()];
//    double d = noeMatrix.getDistance(indexa, indexb);
//    distances.add(new Double(d));
//
//  }
//
//  public void addDistance(int a, int b) {
//
//    // Get distance from NOE matrix. This will do Tropp averaging for methyl groups
//    int indexa = indexAtomInNoeMatrix[a];
//    int indexb = indexAtomInNoeMatrix[b];
//    double d = noeMatrix.getDistance(indexa, indexb);
//    distances.add(new Double(d));
//
//  }
//
//  public double calcDistance(Integer[] numa, Integer[] numb) {
//
//    // Get distance from NOE matrix. This will do Tropp averaging for methyl groups
//    int indexa = indexAtomInNoeMatrix[numa[0].intValue()];
//    int indexb = indexAtomInNoeMatrix[numb[0].intValue()];
//    double d = noeMatrix.getDistance(indexa, indexb);
//    return d;
//  }
//
//  public double calcDistance(int a, int b) {
//
//    // Get distance from NOE matrix. This will do Tropp averaging for methyl groups
//    int indexa = indexAtomInNoeMatrix[a];
//    int indexb = indexAtomInNoeMatrix[b];
//    double d = noeMatrix.getDistance(indexa, indexb);
//    return d;
//  }
//
//  // Adds couple to the list whihc will be output. USed in command line tool cdkReade
//  public void addCouple(Integer[] numAtom1, Integer[] numAtom2,
//                        Integer[] numAtom3, Integer[] numAtom4) {
//    DihedralCouple dihe = new DihedralCouple(numAtom1, numAtom2, numAtom3,
//        numAtom4);
//    double jvalue = dihe.getJvalue();
//
//    couples.add(new Double(jvalue));
//    couplesWhole.add(dihe);
//  }
//
//  public void addCouple(int a1, int a2, int a3, int a4) {
//    Integer[] numAtom1 = new Integer[1];
//    Integer[] numAtom2 = new Integer[1];
//    Integer[] numAtom3 = new Integer[1];
//    Integer[] numAtom4 = new Integer[1];
//    numAtom1[0] = new Integer(a1);
//    numAtom2[0] = new Integer(a2);
//    numAtom3[0] = new Integer(a3);
//    numAtom4[0] = new Integer(a4);
//
//    DihedralCouple dihe = new DihedralCouple(numAtom1, numAtom2, numAtom3,
//        numAtom4);
//    double jvalue = dihe.getJvalue();
//
//    couples.add(new Double(jvalue));
//    couplesWhole.add(dihe);
//  }
//
//  // Returns couple does not add to output list. Used in NMR viewer applet 
//  public Double[] calcCouple(Integer[] numAtom1, Integer[] numAtom2,
//                             Integer[] numAtom3, Integer[] numAtom4) {
//    DihedralCouple dihe = new DihedralCouple(numAtom1, numAtom2, numAtom3,
//        numAtom4);
//    //double jvalue = dihe.getJvalue();
//
//    Double[] dihecouple = new Double[2];
//    dihecouple[0] = new Double(dihe.getTheta());
//    dihecouple[1] = new Double(dihe.getJvalue());
//    return dihecouple;
//  }
//
//  // Returns couple does not add to output list. Used in NMR viewer applet 
//  public Double[] calcCouple(int a1, int a2, int a3, int a4) {
//    Integer[] numAtom1 = new Integer[1];
//    Integer[] numAtom2 = new Integer[1];
//    Integer[] numAtom3 = new Integer[1];
//    Integer[] numAtom4 = new Integer[1];
//    numAtom1[0] = new Integer(a1);
//    numAtom2[0] = new Integer(a2);
//    numAtom3[0] = new Integer(a3);
//    numAtom4[0] = new Integer(a4);
//
//    DihedralCouple dihe = new DihedralCouple(numAtom1, numAtom2, numAtom3,
//        numAtom4);
//
//    //double jvalue = dihe.getJvalue();
//
//    Double[] dihecouple = new Double[2];
//    dihecouple[0] = new Double(dihe.getTheta());
//    dihecouple[1] = new Double(dihe.getJvalue());
//    return dihecouple;
//  }
//
//  private int[] addAtomstoMatrix() {
//
//    // Converts the CDK molecule to input for Gary Sharman's implementation of the NoeMatrix class
//    
//    List<Object> hAtoms = new ArrayList<Object>();
//    Hashtable labels = new Hashtable();
//
//    // Make vector of all hAtoms in molecule
//    Hashtable indexAtominMol = new Hashtable();
//    int modelIndex = Math.max(viewer.am.cmi, 0);
//
//    // todo?
////    for (int i = 0; i < mol.getAtomCount(); i++) {
////      Atom a = mol.getAtomAt(i);
//
////      if (labelArray != null) {
////        if (labelArray[i] != null) {
////          labels.put(a, labelArray[i]);
////        } else {
////          labels.put(a, "");
////        }
////      }
////      indexAtominMol.put(a, new Integer(i));
////
////      if (a.getSymbol().equals("H")) {
////        hAtoms.add(a);
////      }
////    }
//    // Check every H atom to see if it is part of a methyl group
//    // If is, the other two H atoms are removed from the hAtoms vector,
//    // and the methyl group is added
//    // If not, just the H atom is added
//    //System.out.println("Here1 " + hAtoms.size());
//    //System.out.println("Here2 " + mol.getBondCount());
//    
//    Atom[] atoms = viewer.ms.at;
//    
//    BS bsMethyls = (BS) viewer.evaluateExpression("{modelIndex=" + modelIndex + "&search('[CH3]')");
//    for (int i = bsMethyls.nextSetBit(0); i >= 0; i = bsMethyls.nextSetBit(i + 1)) {
//      Atom c = atoms[i];
//      Atom[] meHs = new Atom[3];
//      Bond[] bonds = c.bonds;
//      int n = 0;
//      for (int j = bonds.length; --j >= 0;) {
//        Bond b = bonds[j];
//        Atom ba = b.getOtherAtom(c);
//        if (ba.getElementNumber() != 1)
//          continue;
//        meHs[n++] = ba;
//      }
//      if (n == 3)
//    }
//    
//    int nHAtoms = hAtoms.size();
//    int i = nHAtoms - 1;
//    while (i >= 0) {
//
//      Atom atom = (Atom) hAtoms.get(i);
//
//      Atom c[] = mol.getConnectedAtoms(atom);
//
//      Vector others = mol.getConnectedAtomsVector(c[0]);
//      boolean methyl = false;
//      if (others.size() == 4) {
//
//        for (int j = others.size() - 1; j >= 0; j--) {
//          Atom atomj = (Atom) others.get(j);
//          if ((!atomj.getSymbol().equals("H")) || atomj.compare(atom)) {
//            others.remove(atomj);
//          }
//        }
//        if (others.size() == 2) {
//          // it is a methyl group
//          methyl = true;
//          //System.out.println(atom.toString());
//          Atom atom1 = (Atom) others.get(0);
//          Atom atom2 = (Atom) others.get(1);
//          hAtoms.remove(atom1);
//          hAtoms.remove(atom2);
//
//          Atom[] tmpa = new Atom[3];
//          tmpa[0] = atom;
//          tmpa[1] = atom1;
//          tmpa[2] = atom2;
//          i = i - 2;
//          hAtoms.set(i, tmpa);
//        }
//      }
//      if (methyl) {
//        i--;
//        continue;
//      }
//      // Check for equivalent labels
//      String label = (String) labels.get(atom);
//      Vector equiv = new Vector();
//      equiv.add(atom);
//      if (!(label == null || label.trim().length() == 0)) {
//
//        int j = i - 1;
//        while (j >= 0) {
//          Atom jatom = (Atom) hAtoms.get(j);
//          String jlabel = (String) labels.get(jatom);
//          if (jlabel.equals(label) && (!jatom.compare(atom))) {
//            equiv.add(jatom);
//            hAtoms.remove(jatom);
//            i--;
//            //j--;
//          }
//          j--;
//        }
//        if (equiv.size() > 1) {
//          hAtoms.set(i, equiv);
//        }
//      }
//      i--;
//    }
//
//    nAtoms = hAtoms.size();
//    //System.out.println("Here " + nAtoms);
//    noeMatrix.makeAtomList(nAtoms);
//
//    int[] indexAtomInNoeMatrix = new int[mol.getAtomCount()];
//
//    for (i = 0; i < nAtoms; i++) {
//      Object aobj = hAtoms.get(i);
//      if (aobj.getClass() == (new Atom[3]).getClass()) {
//        Atom[] a = (Atom[]) aobj;
//
//        indexAtomInNoeMatrix[((Integer) indexAtominMol.get(a[0])).intValue()] = i;
//        indexAtomInNoeMatrix[((Integer) indexAtominMol.get(a[1])).intValue()] = i;
//        indexAtomInNoeMatrix[((Integer) indexAtominMol.get(a[2])).intValue()] = i;
//
//        noeMatrix.addMethyl(a[0].getX3d(), a[0].getY3d(), a[0].getZ3d(),
//            a[1].getX3d(), a[1].getY3d(), a[1].getZ3d(), a[2].getX3d(),
//            a[2].getY3d(), a[2].getZ3d());
//      } else if (aobj.getClass() == (new Atom()).getClass()) {
//        Atom a = (Atom) hAtoms.get(i);
//        indexAtomInNoeMatrix[((Integer) indexAtominMol.get(a)).intValue()] = i;
//        noeMatrix.addAtom(a.getX3d(), a.getY3d(), a.getZ3d());
//      }
//      //else if (aobj.getClass() == (new Vector()).getClass()) {
//      else {
//        Vector a = (Vector) aobj;
//        for (int j = 0; j < a.size(); j++) {
//          indexAtomInNoeMatrix[((Integer) indexAtominMol.get(a.get(j)))
//              .intValue()] = i;
//        }
//        double[] xa = new double[a.size()];
//        double[] ya = new double[a.size()];
//        double[] za = new double[a.size()];
//        for (int j = 0; j < a.size(); j++) {
//          xa[j] = ((Atom) a.get(j)).getX3d();
//          ya[j] = ((Atom) a.get(j)).getY3d();
//          za[j] = ((Atom) a.get(j)).getZ3d();
//        }
//        noeMatrix.addEquiv(xa, ya, za);
//        //System.out.println("hello");
//      }
//      /*
//      try  {
//          
//          Atom[] a = (Atom[])hAtoms.get(i);
//          
//          indexAtomInNoeMatrix[((Integer)indexAtominMol.get(a[0])).intValue()] = i;
//          indexAtomInNoeMatrix[((Integer)indexAtominMol.get(a[1])).intValue()] = i;
//          indexAtomInNoeMatrix[((Integer)indexAtominMol.get(a[2])).intValue()] = i;
//          
//          noeMatrix.addMethyl(a[0].getX3d(), a[0].getY3d(), a[0].getZ3d(),
//                         a[1].getX3d(), a[1].getY3d(), a[1].getZ3d(),
//                         a[2].getX3d(), a[2].getY3d(), a[2].getZ3d());
//      }
//      catch (Exception e) {
//          Atom a = (Atom)hAtoms.get(i);
//          indexAtomInNoeMatrix[((Integer)indexAtominMol.get(a)).intValue()] = i;
//          noeMatrix.addAtom(a.getX3d(), a.getY3d(), a.getZ3d());
//      }*/
//
//    }
//
//    return indexAtomInNoeMatrix;
//
//  }
//
//  private boolean checkMethyl(Integer[] num) {
//    if (num.length == 3) {
//      return true;
//    } else {
//      return false;
//    }
//  }
//
//  private class DihedralCouple {
//
//    private double theta;
//    private double jvalue;
//
//    private Atom atom1;
//    private Atom atom2;
//    private Atom atom3;
//    private Atom atom4;
//
//    // These vectors are named for a standard HCCH dihedral, as they
//    // are applied to the Altona equation. But they are also used
//    // for all other dihedrals
//    private Vector3d cB_cA;
//    private Vector3d hA_cA;
//    private Vector3d hB_cB;
//
//    /** Creates a new instance of DihedralCouple */
//    public DihedralCouple(Integer[] numAtom1, Integer[] numAtom2,
//        Integer[] numAtom3, Integer[] numAtom4) {
//
//      this.atom1 = mol.getAtomAt(numAtom1[0].intValue());
//      this.atom2 = mol.getAtomAt(numAtom2[0].intValue());
//      this.atom3 = mol.getAtomAt(numAtom3[0].intValue());
//      this.atom4 = mol.getAtomAt(numAtom4[0].intValue());
//
//      this.cB_cA = getVector3d(atom3, atom2);
//      this.hA_cA = getVector3d(atom1, atom2);
//      this.hB_cB = getVector3d(atom4, atom3);
//
//      this.theta = calcTheta();
//      //double dum = theta * 180.0/Math.PI;
//      //System.out.println(dum);
//      this.jvalue = calcJvalue();
//
//    }
//
//    public double getJvalue() {
//      return this.jvalue;
//    }
//
//    public double getTheta() {
//      return this.theta;
//    }
//
//    private double calcTheta() {
//      Vector3d n1 = new Vector3d();
//      Vector3d n2 = new Vector3d();
//      n1.cross(hA_cA, cB_cA);
//      n2.cross(hB_cB, cB_cA);
//      double angle = n1.angle(n2);
//      return n1.dot(hB_cB) > 0 ? -angle : angle;
//    }
//
//    private double calcJvalue() {
//      String element1 = atom1.getSymbol();
//      String element2 = atom2.getSymbol();
//      String element3 = atom3.getSymbol();
//      String element4 = atom4.getSymbol();
//
//      double jvalue = 0;
//      if (element1.equals("H") && element4.equals("H")) {
//        if (element2.equals("C") && element3.equals("C")) {
//          jvalue = getJAltona();
//
//        } else {
//          jvalue = calcJKarplus();
//        }
//      } else if ((element1.equals("C") && element4.equals("H"))
//          || (element1.equals("H") && element4.equals("C"))) {
//        jvalue = calcJCH();
//      }
//      return jvalue;
//    }
//
//    private double calcJKarplus() {
//      // Simple Karplus equation for 3JHH, ignoring differences in C-substituents
//      final double j0 = 8.5;
//      final double j180 = 9.5;
//      final double jconst = 0.28;
//
//      double jab = 0;
//      if (theta >= -90.0 && theta < 90.0) {
//        jab = j0 * Math.pow((Math.cos(theta)), 2) - jconst;
//      } else {
//        jab = j180 * Math.pow((Math.cos(theta)), 2) - jconst;
//      }
//
//      return jab;
//    }
//
//    private double calcJCH() {
//
//      if (CHequation.equals("was")) {
//        // Simple equation for 3JCH, from Wasylishen and Schaefer
//        // Can J Chem (1973) 51 961
//        // Used in Kozerski et al. J Chem Soc Perkin 2, (1997) 1811
//
//        final double A = 3.56;
//        final double C = 4.26;
//
//        double j = A * Math.cos(2 * theta) - Math.cos(theta) + C;
//        return j;
//
//      } else if (CHequation.equals("tva")) {
//        // Tvaroska and Taravel
//        // Adv. Carbohydrate Chem. Biochem. (1995) 51, 15-61
//        double j = 4.5 - 0.87 * Math.cos(theta) + Math.cos(2.0 * theta);
//        return j;
//      } else if (CHequation.equals("ayd")) {
//        // Aydin and Guether
//        // Mag. Res. Chem. (1990) 28, 448-457
//        double j = 5.8 * Math.pow(Math.cos(theta), 2) - 1.6 * Math.cos(theta)
//            + 0.28 * Math.sin(2.0 * theta) - 0.02 * Math.sin(theta) + 0.52;
//        return j;
//      } else {
//        return 0.0;
//      }
//    }
//
//    private double getJAltona() {
//      // Use Altona equation (Tetrahedron 36, 2783-2792)
//      // If there are less than three substituents on each central carbon
//      // goes to general Karplus 
//
//      // Values taken from www.spectroscopynow.com/Spy/tools/proton-proton.html website. Need to find original source
//      // 2.20 is eneg of H
//      double enegH = 2.20;
//      Hashtable deltaElectro = new Hashtable();
//      deltaElectro.put("C", new Double(2.60 - enegH));
//      deltaElectro.put("O", new Double(3.50 - enegH));
//      deltaElectro.put("N", new Double(3.05 - enegH));
//      deltaElectro.put("F", new Double(3.90 - enegH));
//      deltaElectro.put("Cl", new Double(3.15 - enegH));
//      deltaElectro.put("Br", new Double(2.95 - enegH));
//      deltaElectro.put("I", new Double(2.65 - enegH));
//      deltaElectro.put("S", new Double(2.58 - enegH));// Pauling
//      deltaElectro.put("Si", new Double(1.90 - enegH));// Pauling
//
//      // Substituents on atoms A and B
//      Vector subVA = mol.getConnectedAtomsVector(atom2);
//      Vector subVB = mol.getConnectedAtomsVector(atom3);
//
//      subVA.remove(atom3); // don't include atom A/B in B/A's sub list
//      subVB.remove(atom2);
//
//      int nNonH = 0;
//
//      // Count substituents of carbons A and B  
//      for (int i = 0; i < subVA.size(); i++) {
//        String element = ((Atom) subVA.get(i)).getSymbol();
//        if (element.compareTo("H") != 0) {
//          nNonH++;
//        }
//      }
//      for (int i = 0; i < subVB.size(); i++) {
//        String element = ((Atom) subVB.get(i)).getSymbol();
//        if (element.compareTo("H") != 0) {
//          nNonH++;
//        }
//      }
//
//      // Check number of substituents
//      if ((subVA.size() != 3) || (subVB.size() != 3)) {
//        // Call general Karplus
//        return calcJKarplus();
//      }
//
//      double[] p = new double[8];
//      switch (nNonH) {
//      case 0:
//      case 1:
//      case 2:
//        p[1] = 13.89;
//        p[2] = -0.98;
//        p[3] = 0;
//        p[4] = 1.02;
//        p[5] = -3.40;
//        p[6] = 14.9;
//        p[7] = 0.24;
//        break;
//      case 3:
//        p[1] = 13.22;
//        p[2] = -0.99;
//        p[3] = 0;
//        p[4] = 0.87;
//        p[5] = -2.46;
//        p[6] = 19.9;
//        p[7] = 0;
//        break;
//      case 4:
//        p[1] = 13.24;
//        p[2] = -0.91;
//        p[3] = 0;
//        p[4] = 0.53;
//        p[5] = -2.41;
//        p[6] = 15.5;
//        p[7] = 0.19;
//        break;
//      }
//
//      p[6] = p[6] * Math.PI / 180.0;
//
//      Atom carbonA = atom2;
//      Atom carbonB = atom3;
//      //Atom hydrogenA = atom1;
//      //Atom hydrogenB = atom4;
//
//      double jvalue = p[1] * Math.cos(theta) * Math.cos(theta) + p[2]
//          * Math.cos(theta);
//
//      // Now add all terms for each substituent
//      // Carbon A
//      for (int i = 0; i < 3; i++) {
//        Atom sub = (Atom) subVA.get(i);
//        String element = sub.getSymbol();
//        if (element.compareTo("H") != 0) {
//          Vector3d s_cA = getVector3d(sub, carbonA);
//          double e = 0.0;
//          if (deltaElectro.containsKey(element)) {
//            e = ((Double) deltaElectro.get(element)).doubleValue();
//          }
//
//          int sign = getSubSign(s_cA, hA_cA, cB_cA);
//          jvalue += e
//              * (p[4] + p[5] * Math.cos(sign * theta + p[6] * Math.abs(e))
//                  * Math.cos(sign * theta + p[6] * Math.abs(e)));
//        }
//      }
//
//      for (int i = 0; i < 3; i++) {
//        Atom sub = (Atom) subVB.get(i);
//        String element = sub.getSymbol();
//        if (element.compareTo("H") != 0) {
//          Vector3d s_cB = getVector3d(sub, carbonB);
//          double e = 0.0;
//          if (deltaElectro.containsKey(element)) {
//            e = ((Double) deltaElectro.get(element)).doubleValue();
//          }
//          //double e = ((Double)deltaElectro.get(element)).doubleValue();
//          int sign = -getSubSign(s_cB, hB_cB, cB_cA); //   cB_cA vector should be reversed   
//          jvalue += e
//              * (p[4] + p[5] * Math.cos(sign * theta + p[6] * Math.abs(e))
//                  * Math.cos(sign * theta + p[6] * Math.abs(e)));
//        }
//      }
//
//      return jvalue;
//    }
//
//    private int getSubSign(Vector3d sA_cA, Vector3d hA_cA, Vector3d cB_cA) {
//
//      // Look for sign of (cB_cA x hA_cA).(sA_cA)
//      Vector3d cross = new Vector3d();
//      cross.cross(cB_cA, hA_cA);
//      int sign;
//      if (cross.dot(sA_cA) > 0) {
//        sign = 1;
//      } else {
//        sign = -1;
//      }
//      return sign;
//    }
//
//    private V3 getVector3d(Atom atom2, Atom atom1) {
//      return V3.newVsub(atom2, atom1);
//    }
//  }
}
