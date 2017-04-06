package org.jmol.symmetry;

import java.util.Arrays;

import javajs.util.BS;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.viewer.Viewer;

public class CIPChirality {

  Viewer vwr;
  BS bsFound;

  public CIPChirality() {
    // for reflection 
  }

  public CIPChirality setViewer(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  public String getChirality(Node atom) {
    if (atom.getCovalentBondCount() != 4)
      return "";
    bsFound = new BS();
    CIPAtom a = new CIPAtom(atom, null, false);

    String rs = (a.set() ? getRorS(a) : "");
    if (Logger.debugging)
      Logger.info(atom + " " + rs);

    return rs;
  }

  public String getRorS(CIPAtom a) {
    try {
      if (!a.sortAtoms())
        return "";
      SmilesMatcherInterface sm = vwr.getSmilesMatcher();
      switch (sm.getChirality(a.atoms[3].atom, a.atoms[2].atom,
          a.atoms[1].atom, a.atoms[0].atom)) {
      case 1:
        return "R";
      case 2:
        return "S";
      default:
        return "";
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return "";
    }
  }

  private class CIPAtom implements Comparable<CIPAtom> {
    String path = ";";
    Node atom;
    boolean isDummy = true;
    boolean isTerminal;
    boolean isSet;
    int isAbove;
    CIPAtom parent;
    CIPAtom[] atoms;
    private int nAtoms;

    @Override
    public String toString() {
      return path + " " + atom + (isDummy ? " *" : "");
    }

    public CIPAtom(Node atom, CIPAtom parent, boolean isDummy) {
      if (atom == null)
        return;
      this.atom = atom;
      this.isTerminal = atom.getCovalentBondCount() == 1;
      int iatom = atom.getIndex();
      if (bsFound.get(iatom)) {
        isDummy = true;
      } else {
        bsFound.set(iatom);
        this.isDummy = isDummy;
      }
      this.parent = parent;
      if (parent != null)
        path = parent.path;
      path += getPathString();
    }

    private String getPathString() {
      String elemno = "000" + atom.getElementNumber();
      String mass = "00" + (isDummy ? 0 : atom.getNominalMass());
      return elemno.substring(elemno.length() - 3) + "_"
          + mass.substring(mass.length() - 3) + ";";
    }

    boolean set() {
      if (isTerminal)
        return true;
      if (isSet)
        return true;
      atoms = new CIPAtom[parent == null ? 4 : 3];
      int nBonds = atom.getBondCount();
      Edge[] bonds = atom.getEdges();
      int pt = 0;
      for (int i = 0; i < nBonds; i++) {
        Edge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        Node other = bond.getOtherAtomNode(atom);
        if (parent != null && parent.atom == other)
          continue;
        int order = bond.getCovalentOrder();
        switch (order) {
        case 3:
          if (!addAtom(pt++, other, true)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 2:
          if (!addAtom(pt++, other, true)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 1:
          if (!addAtom(pt++, other, false)) {
            isTerminal = true;
            return false;
          }
          break;
        default:
          isTerminal = true;
          return false;
        }
      }
      isTerminal = (pt == 0);
      nAtoms = pt;
      for (; pt < atoms.length; pt++)
        atoms[pt] = new CIPAtom(null, null, true);
      return !isTerminal;
    }

    private boolean addAtom(int i, Node other, boolean isDummy) {
      if (i >= atoms.length)
        return false;
      atoms[i] = new CIPAtom(other, this, isDummy);
      return true;
    }

    boolean sortAtoms() {

      for (int i = 0; i < nAtoms; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < nAtoms; j++) {
          CIPAtom b = atoms[j];
          int score = (int) Math.signum(a.compareTo(b));
          if (Logger.debugging)
            Logger.info("comparing " + a + " and " + b + " = " + score);
          switch (score) {
          case 1:
            a.isAbove++;
            break;
          case -1:
            b.isAbove++;
            break;
          case 0:
            switch (breakTie(a, b)) {
            case 1:
              a.isAbove++;
              break;
            case -1:
              b.isAbove++;
              break;
            case 0:
              return false;
            }
          }
        }
      }
      for (int i = 0; i < nAtoms; i++)
        atoms[i].path += ";" + atoms[i].isAbove;
      Arrays.sort(atoms);
      if (Logger.debugging)
        for (int i = 0; i < nAtoms; i++)
          Logger.info("" + atoms[i]);
      return true;
    }

    private int breakTie(CIPAtom a, CIPAtom b) {
      if (a.isDummy || !a.set() || !b.set() || a.isTerminal || a.atom == b.atom)
        return 0;
      if (Logger.debugging)
        Logger.info("tie for " + a + " and " + b);
      Arrays.sort(a.atoms);
      Arrays.sort(b.atoms);
      // check to see if any of the three connections to a and b are different.
      for (int i = 0; i < a.nAtoms; i++) {
        int score = (int) Math.signum(a.atoms[i].compareTo(b.atoms[i]));
        switch (score) {
        case -1:
        case 1:
          return score;
        }
      }
      // all are the same -- check to break tie next level
      for (int i = 0; i < a.nAtoms; i++) {
        int score = breakTie(a.atoms[i], b.atoms[i]);
        switch (score) {
        case -1:
        case 1:
          return score;
        }
      }
      // all are the same and no tie breakers
      return 0;
    }

    @Override
    public int compareTo(CIPAtom a) {
      // check to see that atoms are non-null and are different, and path is different 
      return a.atom == null && atom == null ? 0 : a.atom == null ? -1
          : atom == null ? 1 : a.atom == atom ? 0 : -a.path.compareTo(path);
    }

  }

}
