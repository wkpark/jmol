/* GaussianFile.java
 *
 * Version 1.0
 *
 * A JMol import filter for Gaussian output files including NMR shielding tensors
 *
 * Copyright (C) 1997, 1998  Dr. Christoph Steinbeck
 *
 * Contact: steinbeck@ice.mpg.de
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * All I ask is that proper credit is given for my work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.openscience.jmol;


import java.io.*;
import java.net.URL;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * The intention of writing this input filter was to diplay nmr shielding tensors
 * calculated by Gaussian annotated to a 3D model of the structure. Since chemists
 * in most cases are more interested in the chemical shift (calculated
 * shielding of atom in compound minus calculated shielding of atom in a
 * standard (in organic compound and for carbon and hydrogen atoms
 * mostly Tetramethylsilan (TMS)) we have to find out on which level
 * of theory the structures geometry has been optimized and which level has been
 * used for the nmr calculation. In a multipart file we need to look for the
 * last OPT section and note the method/basisset used there.
 * In case a pre-optimized structure is used for just a nmr calculation
 * I don't know what to do yet. Maybe we can use a fixed-format remark.
 * Any suggestions?
 */
public class GaussianFile extends ChemFile 
{
    /**
     *
     **/
    Vector verts = new Vector();

    /**
     *
     **/
    Vector nmrNuclei = new Vector();

    /**
     *
     **/
    Hashtable nmrReferences = null;

    /**
     *
     **/
    BufferedReader buffRead;

    /**
     *
     **/
    static final String ATOMSYM[] = {"Lp", "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Te", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr"};

    /**
     *   Begin of References declaration
     **/
    public static String TMS = "C4H12SI1";

    /**
     * Gaussian output file (of arbitrary extension) may contain output
     * of multiple input file sections.
     * The file is scanned for a section which calculates NMR shielding tensors
     * If found, the structure as well as NMR isotropic shielding is read from the file
     * If not found, the structure is read from the last "Standard Orientation:" entry of
     * the file
     * @see ChemFrame
     * @param is input stream for the Gaussian log file
     * @param ComputeShifts whether or not to compute the Chemical Shifts
     */
    GaussianFile(InputStream is, boolean ComputeShifts) throws Exception
    {
        super();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1024);
        try
        {
            ChemFrame cf = readFrame(r, ComputeShifts);
            frames.addElement(cf);
            Vector fp = cf.getFrameProps();
            for (int i = 0; i < fp.size(); i++) {
                if (PropertyList.indexOf(fp.elementAt(i)) < 0) {
                    PropertyList.addElement(fp.elementAt(i));
                }
            }
            nframes++;
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
    }

    /**
     *
     **/
    public ChemFrame readFrame(BufferedReader buffRead, 
                               boolean ComputeShifts) throws Exception {
        StringTokenizer strTok;
        String line, atomSymbol;
        int atomicNumber, atomicType, partCounter, partCount, soCounter, soCount, tempInt;
        float xCoord, yCoord, zCoord, shielding,reference = 0;
        ChemFrame cf = new ChemFrame();
        boolean NMR = false, OPT = false, calcChemShift = ComputeShifts, refExists = true;
        GaussianVertex gvert;
        String temp;
        String optMethod = "", optBasisSet = "", nmrMethod = "", nmrBasisSet = "";
        System.out.println("Parsing Gaussian logfile for structure definition...");
	if (ComputeShifts)
       		System.out.println("I'm supposed to convert NMR isotropic shieldings to chemical shifts");
	else
		System.out.println("I'm not supposed to convert NMR isotropic shieldings to chemical shifts");

         /*
	 * The Vector nmrNuclei contains the nuclei for which nmr shielding
         * values have to be read from the file.
         * The loading is hardcoded here, but will be done by
         * the file selector box in a later version
         */
        nmrNuclei.addElement(new String("C"));
	nmrNuclei.addElement(new String("H"));
        /*
         * Find last occurence of the string "Standard orientation".
         * It is assumed that, in a multi-part job, the last occurence of
         * the structure is the most refined one
         */
        partCounter = 0;
        soCount = 0;
        do
        {
            try
            {
                line = buffRead.readLine().trim();
                //System.out.println(line);
                if(line.indexOf("Standard orientation:") >= 0)
                {
                    partCounter++;
                }
                if(line.startsWith("#"))
                {
                    soCount++;
                    buffRead.mark(512 * 1024);
                    if(line.toUpperCase().indexOf("OPT") > 0)
                    {
                        OPT = true;
                        strTok = new StringTokenizer(line," /");
                        strTok.nextToken();
                        optMethod = strTok.nextToken().toUpperCase();
                        optMethod = cleanString(optMethod,"(),");
                        optBasisSet = strTok.nextToken().toUpperCase();
                        optBasisSet = cleanString(optBasisSet,"(),");
                    }
                    if(line.toUpperCase().indexOf("NMR") > 0)
                    {
                        NMR = true;
                        // Analyse the level of theory and basis set used
                        strTok = new StringTokenizer(line," /");
                        strTok.nextToken();
                        nmrMethod = strTok.nextToken().toUpperCase();
                        nmrMethod = cleanString(nmrMethod,"(),");
                        nmrBasisSet = strTok.nextToken().toUpperCase();
                        nmrBasisSet = cleanString(nmrBasisSet,"(),");
                        break;
                    }
                    else
                        NMR = false;
                }
            }
            catch(Exception exc)
            {
                break;
            }
        }
        while(true);
        if(partCounter > 1)
        {
            System.out.println("Multipart output file consisting of at least " + soCount + " parts detected.");
        }
        if(OPT)
        {
            System.out.println("Geometry optimization detected");
            System.out.println("Method: " + optMethod + " - Basis set: " + optBasisSet);
        }
        if(NMR)
        {
            System.out.println("NMR Shielding tensor calculation detected");
            System.out.println("Method: " + nmrMethod + " - Basis set: " + nmrBasisSet);
        }
        else
            System.out.println("No NMR Shielding tensor calculation detected");
        buffRead.reset();
        partCount = partCounter;
        try
        {
            do
            {
                line = buffRead.readLine().trim();
                if(line.indexOf("Standard orientation:") >= 0)
                    break;
            }
            while(true);
            System.out.println("Structure found. Reading...");
            buffRead.readLine();
            buffRead.readLine();
            buffRead.readLine();
            buffRead.readLine();
            line = buffRead.readLine().trim();
            while(!line.startsWith("-------------------"))
            {
                strTok = new StringTokenizer(line);
                strTok.nextToken();
                // AtomCounter
                atomicNumber = new Integer(strTok.nextToken()).intValue();
                while(strTok.countTokens() > 3)
                {
                    strTok.nextToken();
                // This makes the difference between G94 and G98
                }
                xCoord = new Float(strTok.nextToken()).floatValue();
                yCoord = new Float(strTok.nextToken()).floatValue();
                zCoord = new Float(strTok.nextToken()).floatValue();
                atomSymbol = ATOMSYM[atomicNumber];
                verts.addElement(new GaussianVertex(atomSymbol,xCoord,yCoord,zCoord));
                line = buffRead.readLine().trim();
            }
            soCounter = 0;
            if(NMR)
            {
		if(calcChemShift && !OPT)
                {
                    System.out.println("*** Warning***");
                    System.out.println("Can't get reference for calculating chemical shifts.");
                    System.out.println("Information on method for geometry optimization is missing.");
                    System.out.println("Values reported are thus uncorrected isotropic shielding values.");
                	  System.out.println("***************");
                }
                do
                {
                    line = buffRead.readLine().trim();
                    if(line.indexOf("Isotropic") >= 0)
                    {
                        strTok = new StringTokenizer(line);
                        tempInt = new Integer(strTok.nextToken()).intValue();
                        atomSymbol = strTok.nextToken().trim();
                        do
                        {
                            temp = strTok.nextToken();
                        }
                        while(!temp.equals("="));
                        shielding = new Float(strTok.nextToken()).floatValue();
                        if(nmrNuclei.indexOf(atomSymbol) >= 0)
                        {
                            if(calcChemShift && OPT)
                            {
				refExists = true;
                               try{
				reference = getReference(atomSymbol,TMS,optMethod + "/" + optBasisSet + "//" + nmrMethod + "/" + nmrBasisSet);
				}
				catch(Exception exc){
					System.out.println("There is a problem retrieving a reference for ");
					System.out.println("nucleus " + atomSymbol + " with " + optMethod + "/" + optBasisSet + "//" + nmrMethod + "/" + nmrBasisSet);
					System.out.println("WARNING: Reverting to isotropic shielding for this Nucleus");
					refExists = false;		
				}
				if (refExists) shielding = reference - shielding;	

                            }
                            ((GaussianVertex)verts.elementAt(soCounter)).isotropShield = shielding;
                        }
                        soCounter++;
                    }
                }
                while(soCounter < verts.size());
            }
            for(int f = 0;f < verts.size();f++) {
                gvert = (GaussianVertex)verts.elementAt(f);
                if(NMR) {
                    NMRShielding ns = new NMRShielding(gvert.isotropShield);
                    Vector props = new Vector();
                    props.addElement(ns);
                    cf.addPropertiedVert(gvert.atomSymbol,
                                         gvert.xCoord,
                                         gvert.yCoord,
                                         gvert.zCoord,
                                         props);
                    
                } else cf.addVert(gvert.atomSymbol,
                                  gvert.xCoord,
                                  gvert.yCoord,
                                  gvert.zCoord);
            }
        }
        catch(Exception exc)
        {
            System.out.println("Error while trying to read Gaussian output");
            System.out.println(exc.toString());
            return null;
        }
        System.out.println("Parsing of structure definition done.");
        return cf;
    }

    /**
     * Constants for referenceCompounds are defined above.
     * The String levelOfTheory is to be given in the format
     * OptLevel/OptBasisSet//NMRLevel/NMRBasisSet (z.B. HF/6-31G(d)//B3LYP/6-311+G(2d,p))
     */
    protected float getReference(String element, String refMol, String levelOfTheory)
    {
        String hash;
        if(nmrReferences == null)
        {
            nmrReferences = new Hashtable();
            loadReferences();
        }
        hash = refMol + levelOfTheory.toUpperCase() + " " + element.toUpperCase();
	NMRReference nmrRef = (NMRReference)nmrReferences.get(hash);
        return nmrRef.shielding;
    }

    /**
     * This method exspects a file NMRReferences in the properties directory
     * in SHARC Mime format.
     */
    protected void loadReferences()
    {
        NMRReference nmrRef = new NMRReference();
        int count = 0;
        try
        {
            String fname = "org/openscience/jmol/Data/NMRReferences";
            URL url = ClassLoader.getSystemResource(fname);
            InputStream is = url.openStream();
            // File file = new File("org/openscience/Data/NMRReferences");
            // System.out.println(file.getAbsolutePath());
            // BufferedReader r = new BufferedReader(new FileReader(file));
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            // System.out.println(r.readLine());
            StreamTokenizer streamTok = new StreamTokenizer(r);
            streamTok.wordChars('!','~');
            do
            {
                nmrRef = readReference(streamTok);
                if(nmrRef == null)
                    break;
                nmrReferences.put(nmrRef.getRefID(),nmrRef);
            }
            while(streamTok.ttype != streamTok.TT_EOF);
        }
        catch(Exception exc)
        {
            System.out.println("Error while trying to read NMRReferences file: " + exc.toString());
        }
    }

    /**
     *    Returns the next NMRReference from Alk Dransfeld's NMR references file
     **/
    NMRReference readReference(StreamTokenizer sT)
    {
        NMRReference nmrRef = new NMRReference();
        String startTag = "";
        String temp = "";
        String element = "";
        boolean tagFound;
        Vector refTags = new Vector();
        try
        {
            do
            {
                tagFound = sT.nextToken() == sT.TT_WORD && sT.sval.indexOf("<REFERENCE_CALC_") >= 0;
                if(tagFound)
                {
                    element = sT.sval.substring(sT.sval.lastIndexOf("_") + 1,sT.sval.lastIndexOf(">"));
                    tagFound = tagFound && isValidElement(element);
                }
                if(sT.ttype == sT.TT_EOF)
                {
                    return null;
                }
            }
            while(!tagFound);
            nmrRef.element = element;
            refTags.removeAllElements();
            do
            {
                sT.nextToken();
                switch(sT.ttype)
                {
                    case sT.TT_WORD:
                        if(sT.sval.startsWith("//"))
                        {
                            nmrRef.optMethod = sT.sval.substring(2,sT.sval.length());
                            break;
                        }
                        if(sT.sval.indexOf("/") > 1)
                        {
                            nmrRef.methProg = sT.sval.substring(0,sT.sval.indexOf("/"));
                            nmrRef.nmrMethod = sT.sval.substring(sT.sval.indexOf("/") + 1,sT.sval.length());
                            break;
                        }
                        if(sT.sval.indexOf("/") == -1 && nmrRef.refMol.equals(""))
                        {
                            nmrRef.refMol = sT.sval;
                            break;
                        }
                    case sT.TT_NUMBER:
                        nmrRef.shielding = (float)sT.nval;
                        break;
                    case sT.TT_EOF:
                        return null;
                }
            }
            while(!(sT.ttype == sT.TT_WORD && sT.sval.indexOf("</REFERENCE_CALC_") >= 0));
        }
        catch(Exception exc)
        {
            exc.toString();
        }
        return nmrRef;
    }

    /**
     *    Scans the ATOMSYM array and returns the atomic number for element.
     *    If nothing is found the method return -1.
     **/
    int getElementNumber(String element)
    {
        for(int f = 0;f < ATOMSYM.length;f++)
        {
            if(ATOMSYM[f].toUpperCase().equals(element.toUpperCase()))
                return f;
        }
        return -1;
    }

    /**
     *    Scans the ATOMSYM array and returns whether element is listed.
     **/
    boolean isValidElement(String element)
    {
        for(int f = 0;f < ATOMSYM.length;f++)
        {
            if(ATOMSYM[f].toUpperCase().equals(element.toUpperCase()))
                return true;
        }
        return false;
    }

    /**
     *    Removes commas and brackets from a String
     **/
    String cleanString(String thisString, String charsToBeRemoved)
    {
        String cleaned = "";
        StringTokenizer sT = new StringTokenizer(thisString,charsToBeRemoved);
        do
        {
            cleaned += sT.nextToken();
        }
        while(sT.hasMoreTokens());
        return cleaned;
    }

    class NMRReference extends java.lang.Object 
    {
        /**
         *    The isotropic shielding for this reference entry
         **/
        float shielding = (float)0.0;

        /**
         *    The element symbol of the element for which this reference was calculated
         **/
        String element = "";

        /**
         *    The method and program used to calculate the shielding, e.g. "GIAO-G94"
         **/
        String methProg = "";

        /**
         *    The level of theory and the basis set used to optimize geometry, e.g. RHF/6-31+GD
         **/
        String optMethod = "";

        /**
         *    The level of theory and the basis set used to calculate the shieding, e.g. RHF/6-31+GD
         **/
        String nmrMethod = "";

        /**
         *    The molecular formular of the reference molecule, e.g. C4H12SI1 for TMS
         **/
        String refMol = "";

        /**
         *    returns a String representation of this NMR reference
         **/
        public String toString()
        {
            String out = "";
            out = refMol + " " + methProg + " " + optMethod + "//" + nmrMethod + " " + element + " " + shielding;
            return out;
        }

        /**
         *    returns a String ID for easy searching
         **/
        public String getRefID()
        {
            String out = "";
            out = refMol + optMethod + "//" + nmrMethod + " " + element;
            return out;
        }

        /**
         *
         **/
        public int hashCode()
        {
            return getRefID().hashCode();
        }

    }

    class GaussianVertex 
    {
        /**
         *
         **/
        String atomSymbol = "C";

        /**
         *
         **/
        float xCoord = (float)0.0;

        /**
         *
         **/
        float yCoord = (float)0.0;

        /**
         *
         **/
        float zCoord = (float)0.0;

        /**
         *
         **/
        float isotropShield = (float)0.0;

        /**
         *
         **/
        GaussianVertex(String as, float xC, float yC, float zC)
        {
            this.atomSymbol = as;
            this.xCoord = xC;
            this.yCoord = yC;
            this.zCoord = zC;
        }

    }

}
