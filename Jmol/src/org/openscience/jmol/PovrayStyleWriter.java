
/*
 * Copyright (C) 2001  The Jmol Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol;

import java.io.*;

/**
 *  Used to set the appearence of molecules output by PovraySaver. There are two
 *  approaches avalible. For properties that vary continously the value should be
 *  set by the call to write the atom- Eg if you are colouring atoms by mulikan
 *  charge then the colour will differ for every atom and should be set by
 *  overriding writeAtom()
 *  
 *  <p><i>write("sphere{ &ltx,y,z&gt,radius
 *  texture{pigment{"+getColourFromCharge(myCharge)+"}}}");</i>
 *  
 *  <p>However, many properties will have the same value for a given type of atom eg in
 *  the CPK scheme both radius and colour can be determined by the element. In this
 *  case you are recommended to use povray declarations of the various types eg
 *  
 *  <p><i>write("#declare Cl_CPK=sphere{&lt0,0,0&gt,"+getRadius()+"
 *  texture{pigment{"+getColour()+"}}}");</i>
 *  
 *  <p>This process has been semi-automated. If you override
 *  getNameForAtom(cf,atomIndex) to return an identifier unique to all atoms of the
 *  same type as <i>atomIndex</i> then you may call findAtomTypes() to find:
 *  
 *  <p>The number of unique types- given by <i>numTypes</i>
 *  <p>The name of the n th type- given by <i>typeName[n]</i>
 *  <p>The index of an atom which is of type n- given by <i>indexOfExampleAtom[n]</i>
 *  
 *  <p>You may then loop over these types in writeAtomsAndBondsDeclare(), declaring
 *  each by name and accessing the properties via indexOfExampleAtom. In writeAtom()
 *  you can add an atom of the correct type by using getNameForAtom().
 *  
 *  <p>This system becomes more useful if you mix and match. Eg Say you wanted to colour the atoms by mulliken charge but you wanted to use the normal
 *  VDW radius then in the writeAtomsAndBondsDeclare() you would declare sizes based on element but add specific colours in writeAtoms(). To
 *  break this down into a step by step process:
 *  
 *  <ol>
 *  <li> Override getNameForAtom() to return the element name of the atom passed in.
 *  
 *  <li> In writeAtomsAndBondsDeclare() call findAtomTypes() to get a list of all
 *  the elements present.
 *  
 *  <li> Still in writeAtomsAndBondsDeclare() loop over the types declaring names
 *  based on the element and setting the radi: w.write("#declare
 *  "+typeName[i]+"_RADIUS =
 *  "+cf.getAtomType[indexOfExampleAtom[i]].getVdwRadius()+";");
 *  
 *  <li> This will now produce a set of declares in the pov file like for chlorine
 *  '#declare Cl_RADIUS = 1.9;'
 *  
 *  <li> Final in the writeAtom() call for the ith atom:
 *  <pre>
 *     w.write("sphere{
 *        &lt"+getVertCoords(i)[0]+","+getVertCoords(i)[1]+","+getVertCoords(i)[0]+"&gt, "+getAtomName(i)+"_RADIUS
 *        texture{
 *           pigment{
 *             color "+getColourFromCharge(charge)+"}"
 *           }
 *        }
 *     }");
 *  </pre>
 *  </ol>
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 *  @author Thomas James Grey
 */
public class PovrayStyleWriter {

    /** The number of different atom types found via getTypeNameForAtom(). Set by calling findAtomTypes()**/
    int numTypes;
    /** The names of different atom types. Set by calling findAtomTypes().**/
    protected String[] typeName;
    /** The examples of different atom types. Set by calling findAtomTypes(). These can be used to make the declarations in wrietAtomsAndBondsDeclaration().**/
    protected int[] indexOfExampleAtom;
    
    
    /**Write out objects used to represent the atoms and bonds. Since multiple styles maybe used, definitions should use the name of the class eg:<p>
     * #declare Cl_myClassName=sphere{.... <p>
     * You may assume that Colors.inc and textures.inc have been included.
     **/
    public void writeAtomsAndBondsDeclare(BufferedWriter w, ChemFrame cf) throws IOException{
        findAtomTypes(cf);
        //Holy cow. Well, we should now know what types there are
        for (int j=0;j<numTypes;j++){
            BaseAtomType at = cf.getAtomAt(indexOfExampleAtom[j]).getBaseAtomType();
            java.awt.Color c = at.getColor();
            String def = "#declare "+at.getName()+"_PovrayStyleWriter = sphere{<0,0,0>,"+at.getVdwRadius()+" texture{pigment{"+povrayColor(c)+"}finish{Shiny}}}\n";
            w.write(def);
        }
    }

    /**This calls getAtomName() for every atom in the frame and identifies how many different types there are (numTypes),
     * the names of the types (typeNames[]) and the index of an example of each type (indexOfExampleAtom[]).
     **/
    protected void findAtomTypes(ChemFrame cf){
        int nAtoms = cf.getNvert();
        numTypes = 0;
        int maxTypes = 5; //Never set this less than 1 (ok thats paranoid of me)
        typeName = new String[maxTypes];
        indexOfExampleAtom = new int[maxTypes];
        String currentName;
        //Warning- code like this can damage your health (when someone else has to maintain it)
        int j;
        int typeOfCurrentAtom;
        for (int i=0; i<nAtoms;i++){
            typeOfCurrentAtom = -1;
            currentName = getAtomName(cf,i);
            for(j=0; j<numTypes && typeOfCurrentAtom==-1;j++){
                if(currentName.equals(typeName[j])){typeOfCurrentAtom = j;}
            }
            //Right, we've looked at all the known types of atom, if typeOfCurrentAtom is still -1 it is a new type- comprend?             
            if (typeOfCurrentAtom == -1){
                typeName[numTypes] = currentName;
                indexOfExampleAtom[numTypes] = i;
                numTypes++;
                if (numTypes >= maxTypes){
                    int oldMaxTypes = maxTypes;
                    maxTypes *= 2;
                    String[] tempNames = new String[maxTypes];
                    int[] tempExamples = new int[maxTypes];
                    System.arraycopy(typeName,0,tempNames,0,oldMaxTypes);
                    System.arraycopy(indexOfExampleAtom,0,tempExamples,0,oldMaxTypes);
                    typeName = tempNames;
                    indexOfExampleAtom = tempExamples;
                }
            }
        }
    }

    /**
     * Write this specific atom.
     * @param w The output stream to write to
     * @param atomIndex The index of the atom we are looking at in the chemfile
     * @param cf The file we are writing
     */
    public void writeAtom(BufferedWriter w, int atomIndex, ChemFrame cf) throws IOException{
        BaseAtomType a = cf.getAtomAt(atomIndex).getBaseAtomType();
        double[] pos = cf.getVertCoords(atomIndex);
        String st = ("object{ "+getAtomName(cf,atomIndex)+"_PovrayStyleWriter translate<"
                     +new Double(pos[0]).toString()+","
                     +new Double(pos[1]).toString()+","
                     +new Double(pos[2]).toString()+">}\n");
        w.write(st);
    }
    /**
     * Write this specific bond
     * @param 
     * @param w the Writer to write it to
     */
    //     public void writeBond(BufferedWriter w, int bondIndex, ChemFile cf){
    //    }

    /** Takes a java colour and returns a String representing the colour in povray eg 'rgb<1.0,0.0,0.0>'**/
    protected String povrayColor(java.awt.Color col){
        float tff = (float)255.0;
        return "rgb<"+((float)col.getRed()/tff)+","+((float)col.getGreen()/tff)+","+((float)col.getBlue()/tff)+">";
    }
    
    /**Override this method to idetify atom types by different properties. By default they are identifed by name.
       @param cf The ChemFrame we are writing
       @param atomIndex The specific atom we are examining.
    **/
    protected String getAtomName(ChemFrame cf, int atomIndex){
        return cf.getAtomAt(atomIndex).getBaseAtomType().getName();
    }
    
}

/*
 * @(#)PovrayStyleWriter.java    1.0 99/06/09
 *
 * Copyright (c) 1999 THOMAS JAMES GREY All Rights Reserved.
 *
 * Thomas James Grey grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  THOMAS JAMES GREY AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL THOMAS JAMES GREY OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF THOMAS JAMES GREY HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

