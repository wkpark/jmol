
/*
 * Copyright 2002 The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import java.util.Vector;
import java.util.Enumeration;
import org.openscience.cdk.io.cml.cdopi.ANIMATIONCDO;
import org.openscience.cdk.io.cml.cdopi.CDOAcceptedObjects;

public final class JMolCDO extends ANIMATIONCDO {

  private ChemFile file;
  private ChemFrame currentFrame;

  private String atomType;
  private String atomX;
  private String atomY;
  private String atomZ;
  private String partialCharge;

  //Crystal
  private float[][] rprimd; //the dimensional primitive vectors

  public JMolCDO() {
    currentFrame = new ChemFrame();
    file = new ChemFile();
  }

  public void startDocument() {
  }

  public void endDocument() {
    if (file.getNumberOfFrames() == 0) {
      this.endFrame();
    }
  }
    

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
    } else if (type.equals("Crystal")) {
      this.startCrystal();
      if (!(file instanceof CrystalFile)) {
	file = new CrystalFile();
      }
      // assume frame has been started       
    } else {
      System.err.println("DEBUG: unknown CDO Object Type at StartObject -> "
          + type);
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
    } else if (type.equals("Crystal")) {
      this.endCrystal();
    } else {
      System.err.println("DEBUG: unknown CDO Object Type at EndObject -> "
          + type);
    }
  }

  public void setObjectProperty(String type, String proptype,
      String propvalue) {

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
    } else if (type.equals("Crystal")) {
      //this.setCrystalProperty(proptype, propvalue);
    } else if (type.equals("a-axis")) {
      this.setCrystalProperty(type, proptype, propvalue);
    } else if (type.equals("b-axis")) {
      this.setCrystalProperty(type, proptype, propvalue);
    } else if (type.equals("c-axis")) {
      this.setCrystalProperty(type, proptype, propvalue);
    } else {
      System.err
          .println("DEBUG: unknown CDO Object Type at SetObjectProperty -> "
            + type);
    }
  }

  public void startAnimation() {
  }

  public void endAnimation() {
  }

  public void startFrame() {
    currentFrame = new ChemFrame();
  }
  
  public void endFrame() {
    if (file instanceof CrystalFile) {
      float[] acell = {1.0f ,1.0f ,1.0f};
      UnitCellBox unitCellBox = new UnitCellBox(rprimd, acell, currentFrame);
      ((CrystalFile)file).setUnitCellBox(unitCellBox);
      ((CrystalFile)file).generateCrystalFrame();
      //Set title
      file.getFrame(file.getNumberOfFrames() - 1) 
	.setInfo(currentFrame.getInfo());
      //Set PhysicalProperties
      Vector fp = currentFrame.getFrameProperties();
      Enumeration ef = fp.elements();
      while (ef.hasMoreElements()) {
	PhysicalProperty pf = (PhysicalProperty) ef.nextElement();
	file.getFrame(file.getNumberOfFrames() - 1)
	  .addProperty(pf);	
      }
    } else {
      file.addFrame(currentFrame);
    }
  }

  public void setFrameProperty(String type, String value) {
    
    if (type.equals("title")) {
      currentFrame.setInfo(value);
    } else if (type.equals("energy")) {
      double energy = (new Double(value)).doubleValue();
      Energy prop = new Energy(energy);
      currentFrame.addProperty(prop);
    }
  }
  

  public void startMolecule() {
  }

  public void endMolecule() {
  }

  public void startFragment() {
  }

  public void endFragment() {
  }

  public void startAtom() {

    atomType = "";
    atomX = "";
    atomY = "";
    atomZ = "";
    partialCharge = "";
  }

  public void endAtom() {
    double x = FortranFormat.atof(atomX.trim());
    double y = FortranFormat.atof(atomY.trim());
    double z = FortranFormat.atof(atomZ.trim());
    
    try {
      int index = currentFrame.addAtom(atomType.trim(), (float) x, (float) y,
				       (float) z);
      if (partialCharge.length() > 0) {
	System.out.println("Adding charge for atom " + index);
	double c = FortranFormat.atof(partialCharge);
	currentFrame.getAtomAt(index).addProperty(new Charge(c));
      } else {
	System.out.println("Not adding charge for atom " + index);
      }
    } catch (Exception e) {
      System.out.println("JMolCDO error while adding atom: " + e);
    
    }
  }
  
  public void setAtomProperty(String type, String value) {

    if (type.equals("type")) {
      atomType = value;
    }
    if (type.equals("x3")) {
      atomX = value;
    }
    if (type.equals("y3")) {
      atomY = value;
    }
    if (type.equals("z3")) {
      atomZ = value;
    }
    if (type.equals("partialCharge")) {
      partialCharge = value;
    }
  }

  public void startCrystal() {
    rprimd = new float[3][3];
  }
  
  public void endCrystal() {
  }
  
  public void setCrystalProperty(String type, String proptype, String value) {
    
    if (type.equals("a-axis")) {
      if (proptype.equals("x")) {
	rprimd[0][0] = (float) FortranFormat.atof(value);
      } else if (proptype.equals("y")) {
	rprimd[0][1] = (float) FortranFormat.atof(value);
      } else if (proptype.equals("z")) {
	rprimd[0][2] = (float) FortranFormat.atof(value);
      }
    }
    if (type.equals("b-axis")) {
      if (proptype.equals("x")) {
	rprimd[1][0] = (float) FortranFormat.atof(value);
      } else if (proptype.equals("y")) {
	rprimd[1][1] = (float) FortranFormat.atof(value);
      } else if (proptype.equals("z")) {
	rprimd[1][2] = (float) FortranFormat.atof(value);
      }
    }
    if (type.equals("c-axis")) {
      if (proptype.equals("x")) {
	rprimd[2][0] = (float) FortranFormat.atof(value);
      } else if (proptype.equals("y")) {
	rprimd[2][1] = (float) FortranFormat.atof(value);
      } else if (proptype.equals("z")) {
	rprimd[2][2] = (float) FortranFormat.atof(value);
      }
    }
  }
  
  public ChemFile returnChemFile() {
    return file;
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

  public CDOAcceptedObjects acceptObjects() {

    CDOAcceptedObjects objects = super.acceptObjects();
    objects.add("Crystal");
    objects.add("a-axis");
    objects.add("b-axis");
    objects.add("c-axis");
    return objects;
  }

}
