/*
 * @(#)JMolCDO.java   0.1 99/08/15
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.miniJmol;

import org.openscience.cdopi.*;
import java.util.Vector;
import org.openscience.jmol.FortranFormat;

public final class JMolCDO implements CDOInterface {
    
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
    
    public void startAnimation() {
        System.out.println("startAnimation");
    }
  
    public void endAnimation() {}
    
    public void startFrame() {
        frameNo++;
        currentFrame = new ChemFrame();
    }

    public void endFrame() {
        allFrames.addElement(currentFrame);
    }

    public void setFrameProperty(String type, String value) {
        if (type.equals("title")) currentFrame.setInfo(value);
    }

    public void startMolecule() {}
    public void endMolecule() {}

    public void startFragment() {}
    public void endFragment() {}
 
    public void startAtom() {
        atom_type = "";
        atom_x = "";
        atom_y = "";
        atom_z = "";
    }
    
    public void endAtom() {
        double x = FortranFormat.atof(atom_x.trim());
        double y = FortranFormat.atof(atom_y.trim());
        double z = FortranFormat.atof(atom_z.trim());
        try {
            currentFrame.addAtom(atom_type.trim(), (float) x, (float) y, (float) z);
        } catch (Exception e) {
            System.out.println("JMolCDO error while adding atom: " + e);
        }
    }

    public void setAtomProperty(String type, String value) {
        if (type.equals("type")) atom_type = value;
        if (type.equals("x3")) atom_x = value;
        if (type.equals("y3")) atom_y = value;
        if (type.equals("z3")) atom_z = value;
    }

    public Vector returnChemFrames() {
        return allFrames;
    }

}
