/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.util.*;
import org.openscience.cdk.geometry.BondTools;
import java.util.Vector;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix3d;
import java.lang.Math;
import java.lang.reflect.Array;    //fa

/**
 * The class <code>CrystalFile</code> defines the properties of a crystal and
 * has a method to generate a set of atoms representing a part
 * of the crystal.
 *
 * <p>How to use this class? It works as a state machine.
 * Create a <code>CrystalFile</code> object and
 * set the crystal properties to the desired values:
 * <pre>
 * CrystalFile cf = new CrystalFile();
 * cf.setUnitCellBox(new UnitCellBox(...));
 *              (use the constructor appropriated to your needs)
 *              (so far, only the trivial space group "1" is implemented)
 * cf.setCrystalBox(new CrystalBox(...));
 * </pre>
 *
 * <p>You have now settled all the necessary information and you can
 * generate the crystal:
 * <pre>
 * cf.generateCrystalFrame();
 * </pre>
 *
 * <p>If you have more frames, you can set the properties to any other desired
 * value and generate a new frame.
 * <pre>
 * cf.setUnitCellBox(...);
 * cf.generateCrystalFrame();
 *    and so on if you want to add more frames.
 * </pre>
 *
 * <p>The generated frames are stored in a field of the <code>super</code>
 * class (<code>ChemFile</code>) and can be accessed with:
 * <pre>
 * cf.getFrame(whichframe);
 * </pre>
 *
 * <p>The crystal properties of a frame can be read with:
 * <pre>
 * cf.getUnitCellBox(whichframe)
 * cf.getCrystalBox(whichframe)
 * </pre>
 *
 * <p>The crystal properties state (used to generate a new frame)
 * can be read with:
 * <pre>
 * cf.getUnitCellBox()
 * cf.getCrystalBox()
 * </pre>
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 */
public class CrystalFile extends ChemFile {


  // Defines the unit cell (primitive and base vectors)
  private UnitCellBox unitCellBoxS;
  private Vector unitCellBox = new Vector(1);          //one element per frame

  // Defines the crystal box (what is visible: atomBox, bondBox, unitBox)
  private CrystalBox crystalBoxS;
  private Vector crystalBox = new Vector(1);           //one element per frame

  private Vector numberBondedAtoms = new Vector(1);    //one element per frame

  //(equivAtoms.elementAt(frameIndex))[unitCellAtomIndex] contains
  //a vector containing the equivalent atom indexes
  private Vector equivAtoms = new Vector(1);           //one element per frame

  private Vector crystalAtomRedPos = new Vector(1);    //one element per frame

  // each element is itself a Vector containing the 
  // atomic position (double[3]) of each atom


  //Compute the Unit Cell Box edges
  static Vector boxEdgesTemplate = new Vector(24);
  static {
    boxEdgesTemplate.addElement(new Point3d());           //O
    boxEdgesTemplate.addElement(new Point3d(1, 0, 0));    //E
    
    boxEdgesTemplate.addElement(new Point3d());           //O
    boxEdgesTemplate.addElement(new Point3d(0, 1, 0));    //E
    
    boxEdgesTemplate.addElement(new Point3d());           //O
    boxEdgesTemplate.addElement(new Point3d(0, 0, 1));    //E
    
    
    boxEdgesTemplate.addElement(new Point3d(1, 0, 0));    //O
    boxEdgesTemplate.addElement(new Point3d(1, 1, 0));    //E
    
    boxEdgesTemplate.addElement(new Point3d(1, 1, 0));    //O
    boxEdgesTemplate.addElement(new Point3d(0, 1, 0));    //E
    
    boxEdgesTemplate.addElement(new Point3d(0, 1, 0));    //O
    boxEdgesTemplate.addElement(new Point3d(0, 1, 1));    //E
    
    boxEdgesTemplate.addElement(new Point3d(0, 1, 1));    //O
    boxEdgesTemplate.addElement(new Point3d(0, 0, 1));    //E
    
    boxEdgesTemplate.addElement(new Point3d(0, 0, 1));    //O
    boxEdgesTemplate.addElement(new Point3d(1, 0, 1));    //E
    
    boxEdgesTemplate.addElement(new Point3d(1, 0, 1));    //O
    boxEdgesTemplate.addElement(new Point3d(1, 0, 0));    //E
    
    boxEdgesTemplate.addElement(new Point3d(1, 0, 1));    //O
    boxEdgesTemplate.addElement(new Point3d(1, 1, 1));    //E
    
    boxEdgesTemplate.addElement(new Point3d(1, 1, 1));    //O
    boxEdgesTemplate.addElement(new Point3d(1, 1, 0));    //E
    
    boxEdgesTemplate.addElement(new Point3d(0, 1, 1));    //O
    boxEdgesTemplate.addElement(new Point3d(1, 1, 1));    //E
  }
  


  /**
   * Create a CrystalFile object using default parameters.
   *
   * <p>By default, the primitive vectors
   * set to define a cube, the space group is set to 1
   * and the atom box and bond box are set to {0,0,0}->{1,1,1}.
   *
   * <p>This is the default constructor.
   */
  public CrystalFile(JmolViewer viewer) {
    super(viewer);

    unitCellBoxS = new UnitCellBox();
    crystalBoxS = new CrystalBox();


  }    //end constructor CrystalFile()


  /**
   * Construct a CrystalFile starting from a ChemFile.
   *
   * @param cf a <code>ChemFile</code> value.
   */
  public CrystalFile(JmolViewer viewer, ChemFile cf,
                     double[][] rprim, double[] acell) {
    super(viewer);

    crystalBoxS = new CrystalBox();

    //Load data from ChemFile
    int nframes = cf.getNumberOfFrames();

    for (int i = 0; i < nframes; i++) {
      ChemFrame frame = cf.getFrame(i);
      int natom = frame.getAtomCount();
      double[][] cartPos = new double[natom][3];
      int[] atomType = new int[natom];
      String info = frame.getInfo();

      for (int at = 0; at < natom; at++) {
        Atom atom = (org.openscience.jmol.Atom)frame.getAtomAt(at);
        cartPos[at][0] = atom.getPoint3D().x;
        cartPos[at][1] = atom.getPoint3D().y;
        cartPos[at][2] = atom.getPoint3D().z;
        atomType[at] = atom.getAtomicNumber();
      }
      
      UnitCellBox unitCellBoxS = new UnitCellBox(rprim, acell, true,
                                                 atomType, cartPos);

      unitCellBoxS.setInfo(info);

      this.unitCellBoxS = unitCellBoxS;
      generateCrystalFrame();
    }
  }


  /**
   * Set the unit cell box parameters  (primitive and base vectors).
   *
   * @param unitCellBox an <code>UnitCellBox</code> value
   */
  public void setUnitCellBox(UnitCellBox unitCellBox) {
    this.unitCellBoxS = unitCellBox;
  }


  /**
   * Set the crystal box (atom and bond box).
   *
   * @param crystalBox a <code>CrystalBox</code> value.
   */
  public void setCrystalBox(CrystalBox crystalBox) {
    this.crystalBoxS = crystalBox;
  }


  /**
   * Get the
   * frame <code>whichframe</code>
   *
   * @param whichframe an <code>int</code> value
   * @return a <code>UnitCellBox</code> value.
   */
  public UnitCellBox getUnitCellBox(int whichframe) {
    return (UnitCellBox) this.unitCellBox.elementAt(whichframe);
  }


  /**
   * Get the unit cell box parameters state.
   *
   * @return a <code>UnitCellBox</code> value.
   */
  public UnitCellBox getUnitCellBox() {
    return this.unitCellBoxS;
  }


  /**
   * Get the crystal box parameters of frame
   * <code>whichframe</code>.
   *
   * @param whichframe an <code>int</code> value.
   * @return a <code>CrystalBox</code> value.
   */
  public CrystalBox getCrystalBox(int whichframe) {
    return (CrystalBox) this.crystalBox.elementAt(whichframe);
  }


  /**
   * Get the crystal box parameters state.
   *
   * @return a <code>Crystal</code> value.
   */
  public CrystalBox getCrystalBox() {
    return this.crystalBoxS;
  }


  /**
   * Return the number of bonded atoms of frame
   * <code>whichframe</code>.
   *
   * @param whichframe an <code>int</code> value.
   * @return an <code>int</code> value.
   */
  public int getNumberBondedAtoms(int whichframe) {
    return ((Integer) numberBondedAtoms.elementAt(whichframe)).intValue();
  }


  /**
   * Generate the next frame.
   *
   */
  public void generateCrystalFrame() {
    int whichframe = this.unitCellBox.size();
    generateCrystalFrame(whichframe);
  }


  /**
   * Generate a frame given by its index.
   * <p>The frame must exist or be the last frame index + 1.
   *
   * @param whichframe an <code>int</code> value.
   */
  public void generateCrystalFrame(int whichframe) {

    Vector crystalFAtomRedPos = new Vector(1);
    CrystalFrame crystalFrame = new CrystalFrame(viewer);
    int natom = unitCellBoxS.getAtomCount();
    Vector frameEquivAtoms[] = (Vector[]) Array.newInstance(Vector.class,
                                 natom);
    int numberBondedAtoms;
    Vector boxEdges;

    // Generate the reduced atomic coordinate according
    // to the Atom Box and put it in crystalFAtomRedPos.
    // Generate the CrystalFrame.
    // Generate a list of equivalent atoms.
    generateAtoms(crystalFAtomRedPos, crystalFrame, frameEquivAtoms);

    //Compute the coordinates of the unit cell box frames.
    boxEdges = generateUnitBoxFrame();

    //to draw the primitive vectors and unit cell box
    crystalFrame.setRprimd(unitCellBoxS.getRprimd());
    crystalFrame.setBoxEdges(boxEdges);

    crystalFrame.setInfo(unitCellBoxS.getInfo());

    if (whichframe < super.getNumberOfFrames()) {
      this.unitCellBox.setElementAt(unitCellBoxS, whichframe);
      this.crystalBox.setElementAt(crystalBoxS, whichframe);
      this.equivAtoms.setElementAt(frameEquivAtoms, whichframe);

      //store in reduced coordinate in *this* object
      this.crystalAtomRedPos.setElementAt(crystalFAtomRedPos, whichframe);

      //store the PhysicalProperties 
      crystalFrame.setFrameProperties
	(getFrame(whichframe).getFrameProperties());

      //store in cartesian coordinate in *super* object    
      super.setFrame(crystalFrame, whichframe);

      // Bond the atoms according to the Bond Box
      numberBondedAtoms = rebond(whichframe);
      this.numberBondedAtoms
          .setElementAt((Object) (new Integer(numberBondedAtoms)),
            whichframe);
    } else if (whichframe == super.getNumberOfFrames()) {
      this.unitCellBox.addElement(unitCellBoxS);
      this.crystalBox.addElement(crystalBoxS);
      this.equivAtoms.addElement(frameEquivAtoms);

      //sorte in reduced in this object
      this.crystalAtomRedPos.addElement(crystalFAtomRedPos);

      //store in cartesian in super object
      super.addFrame(crystalFrame);

      // Bond the atoms according to the Bond Box
      numberBondedAtoms = rebond(whichframe);
      this.numberBondedAtoms
          .addElement((Object) (new Integer(numberBondedAtoms)));
    } else {
      System.out.println("Frame index too high in CrystalFile.java!");
    }



  }    //end generateCrystal


  
    private void generateAtoms(Vector crystalFAtomRedPos,
			       CrystalFrame crystalFrame, Vector frameEquivAtoms[]) {

	double[][] unitCellAtomRedPos = unitCellBoxS.getReducedPos();
	double[][] atomBox = crystalBoxS.getAtomBox();
	int natom = unitCellBoxS.getAtomCount();
	int mina, minb, minc, maxa, maxb, maxc;
	Matrix3d op = new Matrix3d();
	
	
	//Operator "op" needed to transform atomic crystal 
	//coordinate (atomCrystCoord) in cartesian atomic 
	//position (atomPos) 
	
	op.transpose(MathUtil.arrayToMatrix3d(unitCellBoxS.getRprimd()));
	
	
	int atomCrystalIndex = 0;
	
	switch(crystalBoxS.getTranslationType()) {
	case CrystalBox.CRYSTAL:
	    for (int at = 0; at < natom; at++) {
		frameEquivAtoms[at] = new Vector(1);
		//Check for atomBox
		// Determines the base vector multiplicators
		mina = MathUtil.intSup(atomBox[0][0] - unitCellAtomRedPos[at][0]);
		maxa = MathUtil.intInf(atomBox[1][0] - unitCellAtomRedPos[at][0]);
		minb = MathUtil.intSup(atomBox[0][1] - unitCellAtomRedPos[at][1]);
		maxb = MathUtil.intInf(atomBox[1][1] - unitCellAtomRedPos[at][1]);
		minc = MathUtil.intSup(atomBox[0][2] - unitCellAtomRedPos[at][2]);
		maxc = MathUtil.intInf(atomBox[1][2] - unitCellAtomRedPos[at][2]);
		for (int i = mina; i <= maxa; i++) {
		    for (int j = minb; j <= maxb; j++) {
			for (int k = minc; k <= maxc; k++) {
			    double[] newAtomRedPos = new double[3];
			    newAtomRedPos[0] = unitCellAtomRedPos[at][0] + i;
			    newAtomRedPos[1] = unitCellAtomRedPos[at][1] + j;
			    newAtomRedPos[2] = unitCellAtomRedPos[at][2] + k;
			    crystalFAtomRedPos.addElement(newAtomRedPos);
			    double[] newAtomCartPos = new double[3];
			    newAtomCartPos = MathUtil.mulVec(op, newAtomRedPos);
			    BaseAtomType bat = unitCellBoxS.getBaseAtomType(at);
			    crystalFrame.addAtom(bat.getAtomicNumber(),
						 newAtomCartPos[0], newAtomCartPos[1], newAtomCartPos[2]);
			    (frameEquivAtoms[at]).addElement(new Integer(atomCrystalIndex));
			    atomCrystalIndex++;
			}    //end k
		    }      //end j
		}        //end i
	    }          //end for at
	    break;
	case CrystalBox.ORIGINAL:
	    for (int at = 0; at < natom; at++) {
		frameEquivAtoms[at] = new Vector(1);
		//Check for atomBox
		// Determines the base vector multiplicators
		double[] newAtomRedPos = new double[3];
		newAtomRedPos[0] = unitCellAtomRedPos[at][0];
		newAtomRedPos[1] = unitCellAtomRedPos[at][1];
		newAtomRedPos[2] = unitCellAtomRedPos[at][2];
		crystalFAtomRedPos.addElement(newAtomRedPos);
		double[] newAtomCartPos = new double[3];
		newAtomCartPos = MathUtil.mulVec(op, newAtomRedPos);
		BaseAtomType bat = unitCellBoxS.getBaseAtomType(at);
		crystalFrame.addAtom(bat.getAtomicNumber(),
				     newAtomCartPos[0], newAtomCartPos[1], newAtomCartPos[2]);
		(frameEquivAtoms[at]).addElement(new Integer(atomCrystalIndex));
		atomCrystalIndex++;
	    }          //end for at
	    break;
	case CrystalBox.INBOX:
	    for (int at = 0; at < natom; at++) {
		frameEquivAtoms[at] = new Vector(1);
		//Check for atomBox
		// Determines the base vector multiplicators
		double[] newAtomRedPos = new double[3];
		newAtomRedPos[0] = unitCellAtomRedPos[at][0];
		newAtomRedPos[1] = unitCellAtomRedPos[at][1];
		newAtomRedPos[2] = unitCellAtomRedPos[at][2];
		crystalFAtomRedPos.addElement(newAtomRedPos);
		double[] newAtomCartPos = new double[3];
		newAtomCartPos = MathUtil.mulVec(op, newAtomRedPos);
		BaseAtomType bat = unitCellBoxS.getBaseAtomType(at);
		crystalFrame.addAtom(bat.getAtomicNumber(),
				     newAtomCartPos[0], newAtomCartPos[1], newAtomCartPos[2]);
		(frameEquivAtoms[at]).addElement(new Integer(atomCrystalIndex));
		atomCrystalIndex++;
	    }          //end for at
	    break;
	}  //end switch
	
    }            //end generateAtoms()
    
    
  private Vector generateUnitBoxFrame() {
	
    double[][] unitBox = crystalBoxS.getUnitBox();
    int mina, minb, minc, maxa, maxb, maxc;
    Matrix3d op = new Matrix3d();
    Vector boxEdges = new Vector(1);
    Point3d vec;
    double[][] rprimd = unitCellBoxS.getRprimd();



    //Operator "op" needed to transform atomic crystal 
    //coordinate (atomCrystCoord) in cartesian atomic 
    //position (atomPos) 

    op.transpose(MathUtil.arrayToMatrix3d(unitCellBoxS.getRprimd()));

    double[] redEdge = new double[3];
    double[] cartEdge;

    for (int i = MathUtil.intSup(unitBox[0][0]); i <= MathUtil.intInf(unitBox[1][0]) - 1; i++) {
      for (int j = MathUtil.intSup(unitBox[0][1]); j <= MathUtil.intInf(unitBox[1][1]) - 1;
          j++) {
        for (int k = MathUtil.intSup(unitBox[0][2]); k <= MathUtil.intInf(unitBox[1][2]) - 1;
            k++) {

          for (int l = 0; l < boxEdgesTemplate.size(); l++) {
            redEdge[0] = ((Point3d) boxEdgesTemplate.elementAt(l)).x + i;
            redEdge[1] = ((Point3d) boxEdgesTemplate.elementAt(l)).y + j;
            redEdge[2] = ((Point3d) boxEdgesTemplate.elementAt(l)).z + k;

            cartEdge = MathUtil.mulVec(op, redEdge);

            boxEdges.addElement(new Point3d(cartEdge[0], cartEdge[1],
                cartEdge[2]));

          }    //end l;

        }      //end k
      }        //end j
    }          //end i


    return boxEdges;
    
    
  }    //end generate UnitBoxFrame

  /*
   * Return the frame bounds corresponding to a single unit cell.
   * This method is used by CrystalFrame.calcBoundingBox() when
   * the boxEdges are 0 (no unit cell frame)
   */
  static public Vector getSingleBoxEdge(double[][] rprimd) {
    Matrix3d op = new Matrix3d();
    //Operator "op" needed to transform atomic crystal 
    //coordinate (atomCrystCoord) in cartesian atomic 
    //position (atomPos) 
    op.transpose(MathUtil.arrayToMatrix3d(rprimd));

    double[] redEdge = new double[3];
    double[] cartEdge;
    Vector boxEdgesDummy = new Vector(1);
    for (int l = 0; l < boxEdgesTemplate.size(); l++) {  
      redEdge[0] = ((Point3d) boxEdgesTemplate.elementAt(l)).x; 
      redEdge[1] = ((Point3d) boxEdgesTemplate.elementAt(l)).y; 
      redEdge[2] = ((Point3d) boxEdgesTemplate.elementAt(l)).z;
      cartEdge = MathUtil.mulVec(op, redEdge);
      boxEdgesDummy.addElement(new Point3d(cartEdge[0], cartEdge[1],
					   cartEdge[2]));
    }    //end l;
    return boxEdgesDummy;
  }



  /**
   * Walk through this frame and find all bonds in the bond box.
   *
   * @param whichframe an <code>int</code> value
   * @return an <code>int</code> value
   */
  public int rebond(int whichframe) {

    double[] redPos;
    int numberBondedAtoms = 0;
    Vector crystalRedPos =
      (Vector) (this.crystalAtomRedPos.elementAt(whichframe));
    ChemFrame crystalFrame = super.getFrame(whichframe);
    int numberAtoms = crystalFrame.getAtomCount();
    double[][] bondBox =
      ((CrystalBox) crystalBox.elementAt(whichframe)).getBondBox();

    // Clear the currently existing bonds.
    crystalFrame.clearBonds();

    // Do a n*(n-1) scan to get new bonds.

    double bondFudge = 1.12;

    for (int i = 0; i < numberAtoms; i++) {
      redPos = ((double[]) crystalRedPos.elementAt(i));

      if ((redPos[0] >= bondBox[0][0]) && (redPos[0] <= bondBox[1][0])
          && (redPos[1] >= bondBox[0][1]) && (redPos[1] <= bondBox[1][1])
            && (redPos[2] >= bondBox[0][2]) && (redPos[2] <= bondBox[1][2])) {
        numberBondedAtoms++;
        for (int j = i; j < numberAtoms; j++) {

          if (BondTools.closeEnoughToBond(
              (org.openscience.jmol.Atom)crystalFrame.getAtomAt(i),
              (org.openscience.jmol.Atom)crystalFrame.getAtomAt(j),
              bondFudge)) {

            redPos = ((double[]) crystalRedPos.elementAt(j));
            if ((redPos[0] >= bondBox[0][0]) && (redPos[0] <= bondBox[1][0])
                && (redPos[1] >= bondBox[0][1])
                  && (redPos[1] <= bondBox[1][1])
                    && (redPos[2] >= bondBox[0][2])
                      && (redPos[2] <= bondBox[1][2])) {
              crystalFrame.addBond(i, j);
            }
          }
        }
      }
    }

    //return the number of bonded atoms
    return numberBondedAtoms;

  }





}    //end class CrystalFile




