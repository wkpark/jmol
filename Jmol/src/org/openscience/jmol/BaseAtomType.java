/*
 * BaseAtomType.java
 * 
 * Copyright (C) 1999  Bradley A. Smith
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

import java.awt.Color;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * The basic properties of an atom are represented by the atom
 * type. Each atom type is identified with a unique string label.  To
 * ensure that only one atom type exists for each label, atom types
 * can not be created, but only retrieved from a static pool of
 * BaseAtomTypes. If the pool does contain an BaseAtomType for a given
 * label, one will be created upon the first retreival.
 */
public class BaseAtomType {
    
    /**
     * Gets the BaseAtomType corresponding to the given name and sets
     * its values to the parameters.
     * 
     * @param name the name of this atom type (e.g. CA for alpha carbon)
     * @param root the root of this atom type (e.g. C for alpha carbon)
     * @param atomicNumber the atomic number (usually the number of
     *        protons of the root)
     * @param mass the atomic mass
     * @param vdwRadius the van der Waals radius (helps determine drawing size)
     * @param covalentRadius the covalent radius (helps determine bonding)
     * @param color the color for drawing
     * @returns the atom type corresponding to the name.
     */
    public static BaseAtomType get(String name, String root, int atomicNumber, 
                                   double mass, double vdwRadius, 
                                   double covalentRadius, Color color) {
        BaseAtomType at = get(name);
        at.set(root, atomicNumber, mass, vdwRadius, covalentRadius, color);
        return at;
    }
    
    /**
     * Returns the first occurence of an AtomType with the given
     * atomic number.
     *
     * @param atomicNumber  atomic number of the atom type to find.
     * @returns the first atom type with atomic number matching
     *          parameter, or null if not found.
     */
    public static BaseAtomType get(int atomicNumber) {
        Enumeration iter = typePool.elements();
        while (iter.hasMoreElements()) {
            BaseAtomType at = (BaseAtomType) iter.nextElement();
            if (atomicNumber == at.getAtomicNumber()) {
                return at;
            }
        }
        return null;
    }

    /**
     * Returns the BaseAtomType corresponding to the name given.
     * If a corresponding atom type does not exist, one will be created
     * with default values.
     * 
     * @param name the name of this atom type (e.g. CA for alpha carbon)
     * @returns the atom type corresponding to the name.
     */
    public static BaseAtomType get(String name) {
        if (exists(name)) {
            return (BaseAtomType)typePool.get(name);
        }
        BaseAtomType at = new BaseAtomType(name);
        typePool.put(name, at);
        return at;
    }

    /**
     * Returns true if the BaseAtomType with the name given exists.
     * 
     * @param name the name of this atom type (e.g. CA for alpha carbon).
     * @returns true if the atom type exists.
     */
    public static boolean exists(String name) {
        return typePool.containsKey(name);
    }
    
    /**
     * Creates an atom type with the given name.
     * 
     * @param name the name of this atom type (e.g. CA for alpha carbon)
     */
    private BaseAtomType(String name) {
        this.name = name;
    }

    /**
     * Sets the values of this atom type.
     * 
     * @param root the root of this atom type (e.g. C for alpha carbon)
     * @param atomicNumber the atomic number (usually the number of protons of the root)
     * @param mass the atomic mass
     * @param vdwRadius the van der Waals radius (helps determine drawing size)
     * @param covalentRadius the covalent radius (helps determine bonding)
     * @param color the color for drawing
     */
    public void set(String root, int atomicNumber, double mass, 
                    double vdwRadius, double covalentRadius, Color color) {
        this.root = root;
        this.atomicNumber = atomicNumber;
        this.mass = mass;
        this.vdwRadius = vdwRadius;
        this.covalentRadius = covalentRadius;
        this.color = color;
    }
    
    /**
     * Converts the string into the BaseAtomType.
     *
     * @param s1 the string to be converted to an BaseAtomType
     */
    public static BaseAtomType parse(String s1) {
        StringTokenizer st1 = new StringTokenizer(s1, "\t ,;");
        
        String name = st1.nextToken();
        BaseAtomType at = get(name);
        at.root = st1.nextToken();
        at.atomicNumber = Integer.parseInt(st1.nextToken());
        at.mass = Double.valueOf(st1.nextToken()).doubleValue();
        at.vdwRadius = Double.valueOf(st1.nextToken()).doubleValue();
        at.covalentRadius = Double.valueOf(st1.nextToken()).doubleValue();
        at.color = new Color(Integer.parseInt(st1.nextToken()),
                             Integer.parseInt(st1.nextToken()),
                             Integer.parseInt(st1.nextToken()));
        return at;
    }

    /**
     * Returns the name.
     */
    public String getName() {
        return name;
    } 
    
    /**
     * Sets the name.
     * 
     * @param n the Name
     */
    public void setName(String n) {
        this.name = n;
    } 
    
    /**
     * Returns the root.
     */
    public String getRoot() {
        return root;
    } 
    
    /**
     * Sets the root.
     * 
     * @param r the root
     */
    public void setRoot(String r) {
        this.root = r;
    } 
    
    /**
     * Returns the atomic number.
     */
    public int getAtomicNumber() {
        return atomicNumber;
    } 
    
    /**
     * Sets the atomic number.
     * 
     * @param an the atomicNumber
     */
    public void setAtomicNumber(int an) {
        this.atomicNumber = an;
    } 
    
    /**
     * Returns the mass.
     */
    public double getMass() {
        return mass;
    } 
    
    /**
     * Sets the mass.
     * 
     * @param m the mass
     */
    public void setMass(double m) {
        this.mass = m;
    } 
    
    /**
     * Returns the covalent radius.
     */
    public double getCovalentRadius() {
        return covalentRadius;
    } 
    
    /**
     * Sets the covalent radius.
     * 
     * @param cr the covalent radius
     */
    public void setCovalentRadius(double cr) {
        this.covalentRadius = cr;
    } 
    
    /**
     * Returns the Van derWaals radius.
     */
    public double getVdwRadius() {
        return vdwRadius;
    } 
    
    /**
     * Sets the Van derWaals radius.
     * 
     * @param vr the Van derWaals Radius
     */
    public void setVdwRadius(double vr) {
        this.vdwRadius = vr;
    } 
    
    /**
     * Returns the color.
     */
    public Color getColor() {
        return color;
    }
    
    /**
     * Sets the color.
     *
     * @param c the Color
     */
    public void setColor(Color c) {
        this.color = c;
    }
    
    /**
     * Returns true if this and the Object are equal.
     *
     * @param obj object for comparison.
     */
    public boolean equals(Object obj) {
        if (obj instanceof BaseAtomType) {
            BaseAtomType at = (BaseAtomType)obj;
            boolean nameEqual = name.equals(at.name);
            boolean rootEqual = root.equals(at.root);
            boolean atomicNumberEqual = atomicNumber == at.atomicNumber;
            boolean massEqual = mass == at.mass;
            boolean radiiEqual = vdwRadius == at.vdwRadius && covalentRadius == at.covalentRadius;
            boolean colorEqual = color.equals(at.color);
            return (nameEqual && rootEqual && atomicNumberEqual &&
                    massEqual && radiiEqual && colorEqual);
        }
        return false;
    }
    
    /**
     * Returns a String representation of this atom type.
     */
    public String toString() {
        StringBuffer sb1 = new StringBuffer();
        sb1.append(name);
        sb1.append('\t');
        sb1.append(root);
        sb1.append('\t');
        sb1.append(Integer.toString(atomicNumber));
        sb1.append('\t');
        sb1.append(Double.toString(mass));
        sb1.append('\t');
        sb1.append(Double.toString(vdwRadius));
        sb1.append('\t');
        sb1.append(Double.toString(covalentRadius));
        sb1.append('\t');
        sb1.append(Integer.toString(color.getRed()));
        sb1.append('\t');
        sb1.append(Integer.toString(color.getGreen()));
        sb1.append('\t');
        sb1.append(Integer.toString(color.getBlue()));
        return sb1.toString();
    }
    
    
    /**
     * Unique name of this atom type.
     */
    protected String name;
    /**
     * Name of the atom type from which this one is derived.
     */
    protected String root;
    /**
     * Atomic number.
     */
    protected int atomicNumber;
    /**
     * Atomic mass.
     */
    protected double mass;
    /**
     * Van der Waals radius.
     */
    protected double vdwRadius;
    /**
     * Covalent radius.
     */
    protected double covalentRadius;
    /**
     * Draw color.
     */
    protected Color color;
    
    /**
     * Static pool of atom types.
     */
    private static Hashtable typePool = new Hashtable();
}
