
/*
 * Copyright 2001 The Jmol Development Team
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

import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.IOException;

/* THIS IS THE OLD COMMENT, SOME OF THE IDEAS STILL APPLY, BUT THE NEW
 * COMMENT SECTION DETAILS THE ACTUAL IMPLEMENTATION IN THE CODE.
*/

/*
 * Used to set the appearence of molecules output by PovraySaver.
 * There are two approaches avalible.
 * For properties that vary continously
 * the value should be set by the call to write the atom- Eg if you
 * are colouring atoms by mulikan
 * charge then the colour will differ for
 * every atom and should be set by overriding writeAtom()<p>
 * <i>write("sphere{ &lt x,y,z&gt,radius
 * texture{pigment{"+getColourFromCharge(myCharge)+"}}}");</i><p>
 * However, many properties will have the same value for a given type of
 * atom eg in the CPK scheme both
 * radius and colour can be determined
 * by the element. In this case you are recommended to use povray
 * declarations of the various types eg<p>
 * <i>write("#declare Cl_CPK=sphere{&lt0,0,0&gt,"+getRadius()+"
 * texture{pigment{"+getColour()+"}}}");</i><p>
 * This process has been semi-automated. If you override
 * getNameForAtom(cf,atomIndex) to return an
 * identifier unique to all atoms
 * of the same type as <i>atomIndex</i> then you may call findAtomTypes()
 * to find:<p>
 * The number of unique types- given by <i>numTypes</i><p>
 * The name of the n th type- given by <i>typeName[n]</i><p>
 * The index of an atom which is of type n- given by
 * <i>indexOfExampleAtom[n]</i><p>
 * You may then loop over these types in writeAtomsAndBondsDeclare(),
 * declaring each by name and
 * accessing the properties via indexOfExampleAtom.
 * In writeAtom() you can add an atom of the correct type by using
 * getNameForAtom().<p>
 * This system becomes more useful if you mix and match. Eg Say you wanted
 * to colour the atoms by
 * mulliken charge but you wanted to use the normal
 * VDW radius then in the writeAtomsAndBondsDeclare() you would declare
 * sizes based on element but add specific colours in writeAtoms(). To
 * break this down into a step by step process:<p>
 * 1) Override getNameForAtom() to return the element name of the atom
 * passed in.<p>
 * 2) In writeAtomsAndBondsDeclare() call findAtomTypes() to get a list of
 * all the elements present.<p>
 * 3) Still in writeAtomsAndBondsDeclare() loop over the types
 * declaring names based on the element and setting the radi:<p>
 *    w.write("#declare "+typeName[i]+"_RADIUS =
 * "+cf.getAtomType[indexOfExampleAtom[i]].getVdwRadius()+";");<p>
 * 4) This will now produce a set of declares in the pov file like
 * for chlorine '#declare Cl_RADIUS = 1.9;'<p>
 * 5) Final in the writeAtom() call for the ith atom:<p>
 *    w.write("sphere{<p>
 *       &lt"+getAtomCoords(i)[0]+","+getAtomCoords(i)[1]+","+
 *       getAtomCoords(i)[0]+"&gt, "+getAtomName(i)+"_RADIUS<p>
 *       texture{<p>
 *          pigment{<p>
 *            color "+getColourFromCharge(charge)+"}"<p>
 *          }<p>
 *       }<p>
 *    }");
 */

/* UPDATED COMMENT SECTION *********************************************/

/**
 * Writes the appearance and locations of the
 * atoms and bonds that are exported from j-mol. The default action is
 * to grab the color from j-mol based on atom type. The following are
 * passed as general parameters:
 *
 * <ul>
 *   <li>The percentage of the Van Der Walls radius to display</li>
 *   <li>The width of the bonds</li>
 * </ul>
 *
 * The <code>writeAtomsAndBondsMacros</code> method calls the
 * <code>findAtomTypes</code> method to find what types of atoms are
 * contained within the ChemFrame object. Currently the types are
 * based on the atom name. If one would like to vary the way the atoms
 * are displayed by something other than their name (ie. by mulliken
 * charge), the <code>getAtomName</code> method should be
 * overridden.<br><br>
 *
 * The <code>findAtomType</code> method will generate a list of names
 * by calling the <code>getAtomName</code> method and storing the
 * returned string in the array <code>typeName</code>. Overriding the
 * <code>getAtomName</code> method will allow you to choose how each
 * atom is identified. Overriding the <code>povrayColor</code> method
 * in conjuction with the previous override, allows one to completely
 * specify how the atoms and bonds are displayed.<br><br>
 *
 * <i>Note:</i> The <code>povrayColor</code> method must be able to
 * determine the color based on the sample atom given to it that is
 * representative of the atom type.
 *
 * @author Thomas James Grey
 * @author Matthew A. Meineke
 *
 * @version 1.1
 *
 */
public class PovrayStyleWriter {

  /**
   * The number of different atom types found via
   * <code>getAtomName</code>. Set by calling
   * <code>findAtomTypes</code>.
   */
  protected int numTypes;

  /**
   * The names of the different atom types. Set by calling
   * <code>findAtomTypes</code>.
   */
  protected String[] typeName;

  /**
   * The examples of different atom types. Set by calling
   * <code>findAtomTypes</code>. These can be used to make the
   * declarations in <code>writeAtomsAndBondsDeclaration</code>.
   */
  protected int[] indexOfExampleAtom;

  /* these are the tranformation matrixes used to rotate, translate,
   * and zoomthe camera and atoms. */
  private Matrix4d amat, tmat, zmat;

  /**
   * This method sets the rotation matrix.
   *
   * @param amat The rotation matrix.
   */
  public void setAmat(Matrix4d amat) {
    this.amat = amat;
  }

  /**
   * This method sets the translation matrix.
   *
   * @param tmat The translation matrix.
   */
  public void setTmat(Matrix4d tmat) {
    this.tmat = tmat;
  }

  /**
   * This method sets the zoom matrix.
   *
   * @param zmat The zoom matrix.
   */
  public void setZmat(Matrix4d zmat) {
    this.zmat = zmat;
  }

  /**
   * Writes out a set of povray macros that will set
   * up the bond and atom objects for each type specified by the
   * <code>findAtomTypes</code> method. In order to make these
   * scripts as portable as possible, no include files are
   * assumed. This means that all shapes should be constructed from
   * primitives and all colors explicitly declared by their rgb
   * components.
   *
   * @param w The writer to which the output will be written.
   * @param cf The <code>ChemFrame</code> object which will be written.
   * @param atomSphereFactor The percent of the Van Der Walls radius
   *                         to be displayed
   * @param bond_width The radius of the bond cylinders.
   *
   * @throws IOException
   */
  public void writeAtomsAndBondsMacros(
      BufferedWriter w, ChemFrame cf, double atomSphereFactor, float bond_width)
        throws IOException {

    findAtomTypes(cf);

    //Holy cow. Well, we should now know what types there are
    for (int j = 0; j < numTypes; j++) {
      BaseAtomType at = cf.getAtomAt(indexOfExampleAtom[j]).getType();
      String def =
        "//****************************************************\n"
          + "// DEFINE " + typeName[j] + " MACROS\n"
          + "//****************************************************\n" + "\n"
          + "#macro make_" + typeName[j] + "_bond "
          + "(end_1x, end_1y, end_1z, end_2x, end_2y, end_2z)\n"
          + "  cylinder{\n" + "    < end_1x, end_1y, end_1z >,\n"
          + "    < end_2x, end_2y, end_2z >,\n" + "    " + bond_width + "\n"
          + "    texture{\n" + "      pigment{ "
          + povrayColor(cf, indexOfExampleAtom[j]) + " }\n"
          + "      finish{\n" + "        ambient .2\n"
          + "        diffuse .6\n" + "        specular 1\n"
          + "        roughness .001\n" + "        metallic\n" + "      }\n"
          + "    }\n" + "  }\n" + "#end\n" + "#macro make_" + typeName[j]
          + "_atom " + "(center_x, center_y, center_z)\n" + "  sphere{\n"
          + "    < center_x, center_y, center_z>,\n" + "    "
          + (atomSphereFactor * at.getVdwRadius()) + "\n" + "    texture{\n"
          + "      pigment{ " + povrayColor(cf, indexOfExampleAtom[j])
          + " }\n" + "      finish{\n" + "        ambient .2\n"
          + "        diffuse .6\n" + "        specular 1\n"
          + "        roughness .001\n" + "        metallic\n" + "      }\n"
          + "    }\n" + "  }\n" + "#end\n" + "\n" + "\n";

      w.write(def);
    }
  }

  /**
   * Calls <code>getAtomName</code> for every atom in the frame
   * and identifies how many different types there are
   * (<code>numTypes</coce>), the names of the types
   * (</code>typeNames[]</code>) and the index of an example of each
   * type (<code>/indexOfExampleAtom[]</code>).
   *
   * @param cf The </code>ChemFrame</code> object to scan.
   */
  protected void findAtomTypes(ChemFrame cf) {

    int nAtoms = cf.getNumberOfAtoms();
    numTypes = 0;
    int maxTypes = 5;
    typeName = new String[maxTypes];
    indexOfExampleAtom = new int[maxTypes];
    String currentName;

    //Warning- code like this can damage your health (when someone
    //else has to maintain it)

    int j;
    int typeOfCurrentAtom;
    for (int i = 0; i < nAtoms; i++) {
      typeOfCurrentAtom = -1;
      currentName = getAtomName(cf, i);
      for (j = 0; (j < numTypes) && (typeOfCurrentAtom == -1); j++) {
        if (currentName.equals(typeName[j])) {
          typeOfCurrentAtom = j;
        }
      }

      //Right, we've looked at all the known types of atom, if
      //typeOfCurrentAtom is still -1 it is a new type-
      //comprend?
      if (typeOfCurrentAtom == -1) {
        typeName[numTypes] = currentName;
        indexOfExampleAtom[numTypes] = i;
        numTypes++;
        if (numTypes >= maxTypes) {
          int oldMaxTypes = maxTypes;
          maxTypes *= 2;
          String[] tempNames = new String[maxTypes];
          int[] tempExamples = new int[maxTypes];
          System.arraycopy(typeName, 0, tempNames, 0, oldMaxTypes);
          System.arraycopy(indexOfExampleAtom, 0, tempExamples, 0,
              oldMaxTypes);
          typeName = tempNames;
          indexOfExampleAtom = tempExamples;
        }
      }
    }
  }

  /**
   * Write this specific atom.
   *
   * @param w The writer to which the output will be written.
   * @param atomIndex The index of the atom we are looking at in the
   *                  <code>ChemFrame</code>.
   * @param cf The <code>ChemFrame</code> object from which the atom comes.
   *
   * @throws IOException
   */
  public void writeAtom(BufferedWriter w, int atomIndex, ChemFrame cf)
      throws IOException {

    BaseAtomType a = cf.getAtomAt(atomIndex).getType();
    double[] pos = cf.getAtomCoords(atomIndex);

    double c_x = (cf.getXMax() + cf.getXMin()) / 2.0;
    double c_y = (cf.getYMax() + cf.getYMin()) / 2.0;
    double c_z = (cf.getZMax() + cf.getZMin()) / 2.0;

    Matrix4d cmat = new Matrix4d();

    cmat.setIdentity();
    cmat.setTranslation(new Vector3d(-c_x, -c_y, -c_z));
    cmat.mul(amat, cmat);
    cmat.mul(tmat, cmat);

    Point3d aloc = new Point3d(pos[0], pos[1], pos[2]);
    Point3d taloc = new Point3d();

    cmat.transform(aloc, taloc);

    String st = "make_" + getAtomName(cf, atomIndex) + "_atom( "
                  + new Double(taloc.x).toString() + ", "
                  + new Double(taloc.y).toString() + ", "
                  + new Double(taloc.z).toString() + ")\n";
    w.write(st);
  }

  /**
   * Write this specific bond
   *
   * @param w The Writer to which the outupt will be written.
   * @param which_bond The index of the bond being written
   * @param cf The <code>ChemFrame</code> object from which the bond comes.
   *
   * @throws IOException
   */
  public void writeBond(
      BufferedWriter w, Atom atom1, Atom atom2, ChemFrame cf)
        throws IOException {

    float c_x = (cf.getXMax() + cf.getXMin()) / 2.0f;
    float c_y = (cf.getYMax() + cf.getYMin()) / 2.0f;
    float c_z = (cf.getZMax() + cf.getZMin()) / 2.0f;

    Matrix4d cmat = new Matrix4d();

    cmat.setIdentity();
    cmat.setTranslation(new Vector3d(-c_x, -c_y, -c_z));
    cmat.mul(amat, cmat);
    cmat.mul(tmat, cmat);

    Point3d aloc_1 = new Point3d(atom1.getPosition());
    Point3d aloc_2 = new Point3d(atom2.getPosition());

    Point3d taloc_1 = new Point3d();
    Point3d taloc_2 = new Point3d();

    cmat.transform(aloc_1, taloc_1);
    cmat.transform(aloc_2, taloc_2);

    double dx, dy, dz;

    dx = (taloc_2.x - taloc_1.x) / 2.0;
    dy = (taloc_2.y - taloc_1.y) / 2.0;
    dz = (taloc_2.z - taloc_1.z) / 2.0;

    /* The first part of the bond */

    w.write("make_" + getAtomName(atom1) + "_bond( " + taloc_1.x + ", "
        + taloc_1.y + ", " + taloc_1.z + ", " + (taloc_1.x + dx) + ", "
          + (taloc_1.y + dy) + ", " + (taloc_1.z + dz) + " )\n");

    /* The second part of the bond */

    w.write("make_" + getAtomName(atom2) + "_bond( " + taloc_2.x + ", "
        + taloc_2.y + ", " + taloc_2.z + ", " + (taloc_2.x - dx) + ", "
          + (taloc_2.y - dy) + ", " + (taloc_2.z - dz) + " )\n" + "\n");
  }

  /**
   * Takes the index of the example atom, and returns a string
   * representation of the atom type's color in povray's rgb
   * format.<br>
   * ie. &#34;rgb &lt RED_FLOAT, GREEN_FLOAT, BLUE_FLOAT
   * &gt;&#34;<br>
   * where RED_FLOAT, GREEN_FLOAT, and BLUE_FLOAT are the
   * red, green, and blue components of the color scaled from 0 to
   * 1.<p>
   *
   * Override this method to specify the color by something other
   * than the name of the atom
   *
   * @param indexOfExampleAtom The index of an example atom tpye
   *                           that you wish to color.
   *
   * @return The string representation of the color in povray rgb format.
   */
  protected String povrayColor(ChemFrame cf, int indexOfExampleAtom) {

    BaseAtomType at = cf.getAtomAt(indexOfExampleAtom).getType();
    java.awt.Color col = at.getColor();
    float tff = (float) 255.0;
    return "rgb < " + ((float) col.getRed() / tff) + ", "
        + ((float) col.getGreen() / tff) + ", "
          + ((float) col.getBlue() / tff) + ">";
  }

  /**
   * Identifys atoms types based on the name of the atom.<p>
   *
   * @param cf The <code>ChemFrame</code> object being scanned.
   * @param atomIndex The specific atom being examined.
   *
   * @return The string representation of the atom type.
   */
  protected String getAtomName(ChemFrame cf, int atomIndex) {
    return getAtomName(cf.getAtomAt(atomIndex));
  }

  /**
   * Identifys atoms types based on the name of the atom.<p>
   *
   * Override this method to identify atom types by a different
   * property.
   *
   * @param atom the atom for which the name is returned.
   * @param atomIndex The specific atom being examined.
   *
   * @return The string representation of the atom type.
   */
  protected String getAtomName(Atom atom) {
    return atom.getType().getName();
  }

}
