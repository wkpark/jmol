/*
 * @(#)JMolCDO.java   0.1 99/08/15
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.jmol;

import org.openscience.cdopi.*;
import java.util.Vector;

public final class JMolCDO extends ANIMATIONCDO {
    
    private Vector allFrames;
    private ChemFrame currentFrame;
    private int frameNo;
    
    private String atom_type;
    private String atom_x;
    private String atom_y;
    private String atom_z;
    
    public JMolCDO() {
        allFrames = new Vector();
        currentFrame = new ChemFrame();
        frameNo = 0;
    }

    public void startDocument() {}
    public void endDocument() {}
    
  public void startObject(String type) {
    if (type.equals("Atom")) {
      this.startAtom();
    } else if (type.equals("Molecule")) {
      this.startMolecule();
    } else if (type.equals("Fragment")) {
      this.startFragment();
    } else if (type.equals("Bond")) {
      this.startBond();
    } else if (type.equals("Animation")) {
      this.startAnimation();
    } else if (type.equals("Frame")) {
      this.startFrame();
    } else {
      System.err.println("DEBUG: unknown CDO Object Type at StartObject -> " + type);
    }
  }
 
  public void endObject(String type) {
    if (type.equals("Atom")) {
      this.endAtom();
    } else if (type.equals("Molecule")) {
      this.endMolecule();
    } else if (type.equals("Fragment")) {
      this.endFragment();
    } else if (type.equals("Bond")) {
      this.endBond();
    } else if (type.equals("Animation")) {
      this.endAnimation();
    } else if (type.equals("Frame")) {
      this.endFrame();
    } else {
      System.err.println("DEBUG: unknown CDO Object Type at EndObject -> " + type);
    }
  }
 
  public void setObjectProperty(String type, String proptype, String propvalue) {
    if (type.equals("Atom")) {
      this.setAtomProperty(proptype, propvalue);
    } else if (type.equals("Molecule")) {
      this.setMoleculeProperty(proptype, propvalue);
    } else if (type.equals("Fragment")) {
      this.setFragmentProperty(proptype, propvalue);
    } else if (type.equals("Bond")) {
      this.setBondProperty(proptype, propvalue);
    } else if (type.equals("Animation")) {
      this.setAnimationProperty(proptype, propvalue);
    } else if (type.equals("Frame")) {
      this.setFrameProperty(proptype, propvalue);
    } else {
      System.err.println("DEBUG: unknown CDO Object Type at SetObjectProperty -> " + type);
    }
  }                                                                                                                  
    public void startAnimation() {
        System.out.println("startAnimation");
    }
  
    public void endAnimation() {}
    
    public void startFrame() {
        System.out.println("startFrame");
        frameNo++;
        currentFrame = new ChemFrame();
    }

    public void endFrame() {
        System.out.println("endFrame");
        allFrames.addElement(currentFrame);
    }

    public void setFrameProperty(String type, String value) {
        System.out.println("setFrameProperty: " + type + "=" + value);
        if (type.equals("title")) {
          currentFrame.setInfo(value);
        } else if (type.equals("energy")) {
          double energy = (new Double(value)).doubleValue();
          Energy prop = new Energy(energy);
          currentFrame.addFrameProperty((PhysicalProperty)prop);
        }
    }

    public void startMolecule() {}
    public void endMolecule() {}

    public void startFragment() {}
    public void endFragment() {}
 
    public void startAtom() {
        System.out.println("startAtom");
        atom_type = "";
        atom_x = "";
        atom_y = "";
        atom_z = "";
    }
    
    public void endAtom() {
        System.out.println("endAtom: " + atom_type + " " + atom_x + " " +
                           atom_y + " " + atom_z);
        double x = FortranFormat.atof(atom_x.trim());
        double y = FortranFormat.atof(atom_y.trim());
        double z = FortranFormat.atof(atom_z.trim());
        try {
            currentFrame.addVert(atom_type.trim(), (float) x, (float) y, (float) z);
        } catch (Exception e) {
            System.out.println("JMolCDO error while adding atom: " + e);
        }
    }

    public void setAtomProperty(String type, String value) {
        System.out.println("setAtomProp: " + type + "=" + value);
        if (type.equals("type")) atom_type = value;
        if (type.equals("x3")) atom_x = value;
        if (type.equals("y3")) atom_y = value;
        if (type.equals("z3")) atom_z = value;
    }

    public Vector returnChemFrames() {
        return allFrames;
    }

    public void setDocumentProperty(String type, String value) {
    }
    public void setMoleculeProperty(String type, String value) {
    }
    public void setFragmentProperty(String type, String value) {
    }
    public void setAnimationProperty(String type, String value) {
    }
    public void setBondProperty(String type, String value) {
    }
    public void startBond() {
    }
    public void endBond() {
    }
}
