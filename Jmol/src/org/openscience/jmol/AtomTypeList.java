/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

import org.openscience.cdk.tools.AtomTypeFactory;

import java.util.Vector;
import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.FilterOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.EventObject;
import java.awt.Color;

/**
 *  @author Bradley A. Smith (bradley@baysmith.com)
 *  @author Egon Willighagen
 */
public class AtomTypeList {

    private final String SAU = "jmol_atomtypes.txt";
    private org.openscience.cdk.tools.LoggingTool logger;

    private static AtomTypeList instance = null;
    
    protected Vector data = null;

    private AtomTypeList(File UAF) {
        data = new Vector();
        logger = new org.openscience.cdk.tools.LoggingTool(this.getClass().getName());
        System.out.println("AtomTypeList:" + UAF);
        try {
            if (UAF.exists()) {
              System.out.println("exists");
                ReadAtypes(UAF.toString());
            } else {
              System.out.println("does not exist");
                ReadAtypes(SAU);
            }
        } catch (Exception exc) {
            System.err.println("Error while reading AtomTypes." + exc);
        }
    }

    private AtomTypeList() {
        data = new Vector();
        logger = new org.openscience.cdk.tools.LoggingTool(this.getClass().getName());
        logger.warn("Using default atom types. Make sure to instantiate this class at a proper time!");
        try {
            ReadAtypes(SAU);
        } catch (Exception exc) {
            System.err.println("Error while reading AtomTypes." + exc);
        }
    }

    public static AtomTypeList getInstance(File UAF) {
        if (instance == null) {
            instance = new AtomTypeList(UAF);
        }
        return instance;
    }
    
    public static AtomTypeList getInstance() {
        if (instance == null) {
            instance = new AtomTypeList();
        }
        return instance;
    }
    
    public void SaveAtypes(File file) {
        
        try {
            FileOutputStream fdout = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fdout, 1024);
            PrintWriter pw = new PrintWriter(bos);
            
            int numRows = getSize();
            int numCols = 7;
            
            String[] names = {"Atom Type", "Base Atom Type", "Atomic Number", "Atomic Mass",
            "Van derWaals Radius", "Covalent Radius", "Color"};            
            String headline = "#" + names[0];
            for (int j = 1; j < numCols - 1; j++) {
                headline += "\t";
                
                String str = names[j];
                BufferedReader br = new BufferedReader(new StringReader(str));
                String line;
                try {
                    line = br.readLine();
                    while (line != null) {
                        headline = headline + line + " ";
                        line = br.readLine();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            headline += "\tRed\tGreen\tBlue";
            pw.println(headline);
            
            for (int i = 0; i < numRows; i++) {
                BaseAtomType bat = getElementAt(i);
                String outline = (String) bat.getAtomTypeName();
                outline = outline + "\t" + bat.getSymbol();
                outline = outline + "\t" + bat.getAtomicNumber();
                outline = outline + "\t" + bat.getExactMass();
                outline = outline + "\t" + bat.getVanderwaalsRadius();
                outline = outline + "\t" + bat.getCovalentRadius();
                Color c = (Color) bat.getColor();
                outline = outline + "\t" + c.getRed();
                outline = outline + "\t" + c.getGreen();
                outline = outline + "\t" + c.getBlue();
                pw.println(outline);
            }
            pw.flush();
            pw.close();
            bos.close();
            fdout.close();
            
        } catch (IOException e) {
            System.err.println("Exception: " + e);
        }
        return;
    }

    private void ReadAtypes(String configFile) throws Exception {
        System.out.println("Reading config file... " + configFile);
        AtomTypeFactory atf = new AtomTypeFactory(configFile);
        
        org.openscience.cdk.AtomType[] types = atf.getAllAtomTypes();
        System.out.println("Read atom types: " + types.length);
        for (int i=0; i<types.length; i++) {
            // convert all AtomType's to BaseAtomType and add then
            // to the list
            data.addElement(BaseAtomType.get(types[i]));
        }
        System.out.println("done.");
    }

    /**
    * Returns the first occurence of an AtomType with the given name.
    */
    public BaseAtomType get(String name) {
        if (name != null) {
            Enumeration e = AtomTypeList.getInstance().elements();
            while (e.hasMoreElements()) {
                BaseAtomType at = (BaseAtomType) e.nextElement();
                if (name.equalsIgnoreCase(at.getAtomTypeName())) {
                    return at;
                }
            }
        }
        return null;
    }
    
    /**
    * Returns the first occurence of an AtomType with the given atomic number.
    */
    public BaseAtomType get(int atomicNumber) {
        
        Enumeration e = AtomTypeList.getInstance().elements();
        while (e.hasMoreElements()) {
            BaseAtomType at = (BaseAtomType) e.nextElement();
            if (atomicNumber == at.getAtomicNumber()) {
                return at;
            }
        }
        return null;
    }
    
    public BaseAtomType getElementAt(int number) {
        if (number > getSize()) {
            logger.error("Trying to get atom type where index > data.size()");
            return null;
        }
        try {
            return (BaseAtomType)data.elementAt(number);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Trying to get atom type, but got ArrayIndexOutOfBounds");
            return null;
        }
    }
    
    public void setElementAt(BaseAtomType atomType, int number) {
        if (number > getSize()) {
            logger.error("Trying to set atom type where index > data.size()");
            return;
        }
        try {
            data.setElementAt(atomType, number);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Trying to set atom type, but got ArrayIndexOutOfBounds");
            return;
        }
    }
    
    public void addElement(BaseAtomType atomType) {
        data.addElement(atomType);
    }
    
    public void removeAllElements() {
        data.removeAllElements();
    }
    
    public int getSize() {
        return data.size();
    }
    
	/**
	 *  Configures an atom. Finds the correct element type
	 *  by looking at the atoms atom type id (atom.getAtomTypeName()).
	 *
	 * @param  atom  The atom to be configured
	 * @return       The configured atom
	 */
	public Atom configure(Atom atom) {
        try {
            String atomTypeName = atom.getAtomTypeName();
            if (atomTypeName == null) {
                atomTypeName = atom.getSymbol();
            }
            BaseAtomType at = null;
            if (atomTypeName.length() > 0) {
                atom.setAtomTypeName(atomTypeName);
                // logger.debug("Looking up base with: " + atomTypeName);
                at = get(atomTypeName);
            } else {
                /* if the atom does not have an symbol and an atomTypeName
                   either, then use the atomic number */
                // logger.debug("Looking up base with: " + atom.getAtomicNumber());
                at = get(atom.getAtomicNumber());
                atom.setSymbol(at.getSymbol());
                atom.setAtomTypeName(at.getAtomTypeName());
            }
            // logger.debug("BaseAtomType: " + at.toString());
            atom.setMaxBondOrder(at.getMaxBondOrder());
            atom.setMaxBondOrderSum(at.getMaxBondOrderSum());
            atom.setVanderwaalsRadius(at.getVanderwaalsRadius());
            atom.setCovalentRadius(at.getCovalentRadius());
            atom.setProperty("org.openscience.jmol.color", at.getColor());
            if (at.getAtomicNumber() != 0) {
                atom.setAtomicNumber(at.getAtomicNumber());
            } else {
                logger.debug("Did not configure atomic number: AT.an=" + at.getAtomicNumber());
            }
            if (at.getExactMass() > 0.0) {
                atom.setExactMass(at.getExactMass());
            } else {
                logger.debug("Did not configure mass: AT.mass=" + at.getAtomicNumber());
            }
        } catch (Exception exc) {
            logger.warn("Could not configure atom with unknown AtomTypeName: " + 
                        atom.toString() + " + (id=" + atom.getAtomTypeName() + ")");
        }
        // logger.debug("Configured " + atom.toString());
		return atom;
	}

    public synchronized Enumeration elements() {
        return data.elements();
    }
}

