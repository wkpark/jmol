
package org.openscience.jmol;

import java.util.Vector;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import java.lang.Math;
import java.lang.reflect.Array;    //fa

/**
 * The class <code>CrystalFile</code> defines the properties of a crystal and
 * has a method to generate a set of atoms representing a part
 * of the crystal.<br>
 *
 * How to use this class? It works as a state machine.
 * Create a <code>CrystalFile</code> object and
 * set the crystal properties to the desired values:<br><br>
 *
 * <code>CrystalFile cf = new CrystalFile();</code><br>
 * <code>cf.setUnitCellBox(new UnitCellBox(...));</code><br>
 *              (use the constructor appropriated to your needs)<br>
 *              (so far, only the trivial space group "1" is implemented)<br>
 * <code>cf.setCrystalBox(new CrystalBox(...));</code><br>

 * You have now settled all the necessary information and you can
 * generate the crystal:<br><br>
 * <code>cf.generateCrystalFrame();</code><br><br>
 *
 * If you have more frames, you can set the properties to any other desired
 * value and generate a new frame.<br>
 * <code>cf.setUnitCellBox(...);</code><br>
 * <code>cf.generateCrystalFrame();</code><br>
 *    and so on if you want to add more frames.<br><br>
 *
 * The generated frames are stored in a field of the <code>super</code>
 * class (<code>ChemFile</code>) and can be accessed with:
 * <code>cf.getFrame(whichframe);</code><br><br>
 *
 * The crystal properties of a frame can be read with:<br>
 * <code>cf.getUnitCellBox(whichframe)</code>,
 * <code>cf.getCrystalBox(whichframe)</code><br>
 *
 * The crystal properties state (used to generate a new frame)
 * can be read with:<br>
 * <code>cf.getUnitCellBox()</code>,
 * <code>cf.getCrystalBox()</code><br>
 *
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.2
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
  // atomic position (float[3]) of each atom


  /**
   * Default constructor:<br>
   * Create a CrystalFile object using default parameters.<br>
   * By default, the primitive vectors
   * set to define a cube, the space group is set to 1
   * and the atom box and bond box are set to {0,0,0}->{1,1,1}.
   */
  public CrystalFile() {

    unitCellBoxS = new UnitCellBox();
    crystalBoxS = new CrystalBox();


  }    //end constructor CrystalFile()


  /**
   * Construct a CrystalFile starting from a ChemFile.
   *
   * @param cf a <code>ChemFile</code> value.
   */
  public CrystalFile(ChemFile cf, float[][] rprim, float[] acell) {

    crystalBoxS = new CrystalBox();

    //Load data from ChemFile
    int nframes = cf.getNumberOfFrames();

    for (int i = 0; i < nframes; i++) {
      ChemFrame frame = cf.getFrame(i);
      int natom = frame.getNumberOfAtoms();
      float[][] cartPos = new float[natom][3];
      int[] atomType = new int[natom];
      String info = frame.getInfo();

      for (int at = 0; at < natom; at++) {
        cartPos[at][0] = frame.getAtomAt(at).getPosition().x;
        cartPos[at][1] = frame.getAtomAt(at).getPosition().y;
        cartPos[at][2] = frame.getAtomAt(at).getPosition().z;
        atomType[at] = frame.getAtomAt(at).getType().getAtomicNumber();
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
   * Generate a frame given by its index.<br>
   * The frame must exist or be the last frame index + 1.
   *
   * @param whichframe an <code>int</code> value.
   */
  public void generateCrystalFrame(int whichframe) {

    Vector crystalFAtomRedPos = new Vector(1);
    CrystalFrame crystalFrame = new CrystalFrame();
    int natom = unitCellBoxS.getNumberOfAtoms();
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

    float[][] unitCellAtomRedPos = unitCellBoxS.getReducedPos();
    float[][] atomBox = crystalBoxS.getAtomBox();
    int natom = unitCellBoxS.getNumberOfAtoms();
    int mina, minb, minc, maxa, maxb, maxc;
    Matrix3f op = new Matrix3f();


    //Operator "op" needed to transform atomic crystal 
    //coordinate (atomCrystCoord) in cartesian atomic 
    //position (atomPos) 

    op.transpose(arrayToMatrix3f(unitCellBoxS.getRprimd()));


    int atomCrystalIndex = 0;
    for (int at = 0; at < natom; at++) {

      frameEquivAtoms[at] = new Vector(1);

      //Check for atomBox
      // Determines the base vector multiplicators
      mina = intSup(atomBox[0][0] - unitCellAtomRedPos[at][0]);
      maxa = intInf(atomBox[1][0] - unitCellAtomRedPos[at][0]);
      minb = intSup(atomBox[0][1] - unitCellAtomRedPos[at][1]);
      maxb = intInf(atomBox[1][1] - unitCellAtomRedPos[at][1]);
      minc = intSup(atomBox[0][2] - unitCellAtomRedPos[at][2]);
      maxc = intInf(atomBox[1][2] - unitCellAtomRedPos[at][2]);

      for (int i = mina; i <= maxa; i++) {
        for (int j = minb; j <= maxb; j++) {
          for (int k = minc; k <= maxc; k++) {

            float[] newAtomRedPos = new float[3];
            newAtomRedPos[0] = unitCellAtomRedPos[at][0] + (float) i;
            newAtomRedPos[1] = unitCellAtomRedPos[at][1] + (float) j;
            newAtomRedPos[2] = unitCellAtomRedPos[at][2] + (float) k;

            crystalFAtomRedPos.addElement(newAtomRedPos);

            float[] newAtomCartPos = new float[3];
            newAtomCartPos = mulVec(op, newAtomRedPos);

            crystalFrame.addAtom(unitCellBoxS.getAtomType(at),
                newAtomCartPos[0], newAtomCartPos[1], newAtomCartPos[2]);


            (frameEquivAtoms[at]).addElement(new Integer(atomCrystalIndex));

            atomCrystalIndex++;

          }    //end k
        }      //end j
      }        //end i
    }          //end for at

  }            //end generateAtoms()


  private Vector generateUnitBoxFrame() {

    float[][] unitBox = crystalBoxS.getUnitBox();
    int mina, minb, minc, maxa, maxb, maxc;
    Matrix3f op = new Matrix3f();
    Vector boxEdgesTemplate = new Vector(24);
    Vector boxEdges = new Vector(1);
    Point3f vec;
    float[][] rprimd = unitCellBoxS.getRprimd();

    //Compute the Unit Cell Box edges
    boxEdgesTemplate.add(new Point3f());           //O
    boxEdgesTemplate.add(new Point3f(1, 0, 0));    //E

    boxEdgesTemplate.add(new Point3f());           //O
    boxEdgesTemplate.add(new Point3f(0, 1, 0));    //E

    boxEdgesTemplate.add(new Point3f());           //O
    boxEdgesTemplate.add(new Point3f(0, 0, 1));    //E


    boxEdgesTemplate.add(new Point3f(1, 0, 0));    //O
    boxEdgesTemplate.add(new Point3f(1, 1, 0));    //E

    boxEdgesTemplate.add(new Point3f(1, 1, 0));    //O
    boxEdgesTemplate.add(new Point3f(0, 1, 0));    //E

    boxEdgesTemplate.add(new Point3f(0, 1, 0));    //O
    boxEdgesTemplate.add(new Point3f(0, 1, 1));    //E

    boxEdgesTemplate.add(new Point3f(0, 1, 1));    //O
    boxEdgesTemplate.add(new Point3f(0, 0, 1));    //E

    boxEdgesTemplate.add(new Point3f(0, 0, 1));    //O
    boxEdgesTemplate.add(new Point3f(1, 0, 1));    //E

    boxEdgesTemplate.add(new Point3f(1, 0, 1));    //O
    boxEdgesTemplate.add(new Point3f(1, 0, 0));    //E

    boxEdgesTemplate.add(new Point3f(1, 0, 1));    //O
    boxEdgesTemplate.add(new Point3f(1, 1, 1));    //E

    boxEdgesTemplate.add(new Point3f(1, 1, 1));    //O
    boxEdgesTemplate.add(new Point3f(1, 1, 0));    //E

    boxEdgesTemplate.add(new Point3f(0, 1, 1));    //O
    boxEdgesTemplate.add(new Point3f(1, 1, 1));    //E

    //Operator "op" needed to transform atomic crystal 
    //coordinate (atomCrystCoord) in cartesian atomic 
    //position (atomPos) 

    op.transpose(arrayToMatrix3f(unitCellBoxS.getRprimd()));

    float[] redEdge = new float[3];
    float[] cartEdge;

    for (int i = intSup(unitBox[0][0]); i <= intInf(unitBox[1][0]) - 1; i++) {
      for (int j = intSup(unitBox[0][1]); j <= intInf(unitBox[1][1]) - 1;
          j++) {
        for (int k = intSup(unitBox[0][2]); k <= intInf(unitBox[1][2]) - 1;
            k++) {

          System.out.println("ijk: " + i + " " + " " + j + " " + k);
          for (int l = 0; l < boxEdgesTemplate.size(); l++) {
            redEdge[0] = ((Point3f) boxEdgesTemplate.elementAt(l)).x + i;
            redEdge[1] = ((Point3f) boxEdgesTemplate.elementAt(l)).y + j;
            redEdge[2] = ((Point3f) boxEdgesTemplate.elementAt(l)).z + k;

            cartEdge = mulVec(op, redEdge);

            boxEdges.addElement(new Point3f(cartEdge[0], cartEdge[1],
                cartEdge[2]));

          }    //end l;

        }      //end k
      }        //end j
    }          //end i

    return boxEdges;

  }    //end generate UnitBoxFrame



  /**
   * Walk through this frame and find all bonds in the bond box.
   *
   * @param whichframe an <code>int</code> value
   * @return an <code>int</code> value
   */
  public int rebond(int whichframe) {

    float[] redPos;
    int numberBondedAtoms = 0;
    Vector crystalRedPos =
      (Vector) (this.crystalAtomRedPos.elementAt(whichframe));
    ChemFrame crystalFrame = super.getFrame(whichframe);
    int numberAtoms = crystalFrame.getNumberOfAtoms();
    float[][] bondBox =
      ((CrystalBox) crystalBox.elementAt(whichframe)).getBondBox();

    // Clear the currently existing bonds.
    crystalFrame.clearBonds();

    // Do a n*(n-1) scan to get new bonds.

    for (int i = 0; i < numberAtoms; i++) {
      redPos = ((float[]) crystalRedPos.elementAt(i));

      if ((redPos[0] >= bondBox[0][0]) && (redPos[0] <= bondBox[1][0])
          && (redPos[1] >= bondBox[0][1]) && (redPos[1] <= bondBox[1][1])
            && (redPos[2] >= bondBox[0][2]) && (redPos[2] <= bondBox[1][2])) {
        numberBondedAtoms++;
        for (int j = i; j < numberAtoms; j++) {

          if (Atom.closeEnoughToBond(crystalFrame.getAtomAt(i),
              crystalFrame.getAtomAt(j), crystalFrame.getBondFudge())) {

            redPos = ((float[]) crystalRedPos.elementAt(j));
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


  //Pure mathematical method

  /**
   * Multiply the matrix "mat" by the vector "vec".
   * The result is vector.
   */
  private float[] mulVec(Matrix3f mat, float[] vec) {

    float[] result = new float[3];
    result[0] = mat.m00 * vec[0] + mat.m01 * vec[1] + mat.m02 * vec[2];
    result[1] = mat.m10 * vec[0] + mat.m11 * vec[1] + mat.m12 * vec[2];
    result[2] = mat.m20 * vec[0] + mat.m21 * vec[1] + mat.m22 * vec[2];
    return result;
  }


  /**
   * Given a <code>float</code> f, return the closest superior integer
   *
   */
  private int intSup(float f) {

    if (f <= 0) {
      return (int) f;
    } else {
      return (int) f + 1;
    }

  }

  int intInf(float f) {

    if (f < 0) {
      return (int) f - 1;
    } else {
      return (int) f;
    }
  }

  /**
   * Convert a <code>Matrix3f</code> to a <code>float[3][3]</code>.
   *
   */
  private float[][] matrix3fToArray(Matrix3f matrix3f) {

    float[][] array = new float[3][3];

    array[0][0] = matrix3f.m00;
    array[0][1] = matrix3f.m01;
    array[0][2] = matrix3f.m02;

    array[1][0] = matrix3f.m10;
    array[1][1] = matrix3f.m11;
    array[1][2] = matrix3f.m12;

    array[2][0] = matrix3f.m20;
    array[2][1] = matrix3f.m21;
    array[2][2] = matrix3f.m22;

    return array;
  }

  /**
   * Convert a <code>float[3][3]</code> to a <code>Matrix3f</code>.
   *
   */
  private Matrix3f arrayToMatrix3f(float[][] array) {

    Matrix3f matrix3f = new Matrix3f();

    matrix3f.m00 = array[0][0];
    matrix3f.m01 = array[0][1];
    matrix3f.m02 = array[0][2];

    matrix3f.m10 = array[1][0];
    matrix3f.m11 = array[1][1];
    matrix3f.m12 = array[1][2];

    matrix3f.m20 = array[2][0];
    matrix3f.m21 = array[2][1];
    matrix3f.m22 = array[2][2];

    return matrix3f;
  }


}    //end class CrystalFile




