/* ChemFileUtils.java
 *
 * Copyright (C) 1997, 1998  Dr. Christoph Steinbeck
 *
 * Contact: steinbeck@ice.mpg.de
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.  All I ask is that
 * proper credit is given for my work, which includes - but is not
 * limited to - adding the above copyright notice to the beginning of
 * your source code files, and to any copyright notice that you may
 * distribute with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * 
*/
package org.openscience.compchem;

import java.io.*;
import java.util.*;
import java.text.*;

/** A collection of static methods to return an array of Nodes from
    various commonly used file formats, so far only MDL Mol files and
    SMILES strings */
public class ChemFileUtils {

    /**
     *  This needs to be fixed according to JICS (1992) 32/3,
     *  244-255 I have written this by trial and error.  Parses an
     *  MDL Mol file and returns an array of nodes. So far, the
     *  coordinates are ignored because I only use the method to
     *  extract the connectivity info from the file, but the code to
     *  read the coordinates into the x and y coordinates of the
     *  Node class is easily added
     **/

    public static Node[] readMDLMolFile(String args)
    {
        File file = new File(args);
        return readMDLMolFile(file);
    }
    
    
    public static Node[] readMDLMolFile(File args)
    {
        Node[] atSet = null;
        int atoms = 0, bonds = 0, atom1 = 0, atom2 = 0, value = 0;
        double x=0, y=0, z=0;
        int[][] conMat = new int[0][0];
        String help;
        Vector symbols = new Vector();
        
        try
        {
            System.out.println("Attempt to read file " + args);
            RandomAccessFile RAF = new RandomAccessFile(args, "r");
            System.out.println(RAF.readLine());
            System.out.println(RAF.readLine());
            System.out.println(RAF.readLine());
            StringBuffer strBuff = new StringBuffer(RAF.readLine());
            strBuff.insert(3, " ");
            StringTokenizer strTok =
                new StringTokenizer(strBuff.toString());
            atoms = java.lang.Integer.valueOf(
                                              strTok.nextToken()).intValue();
            bonds = java.lang.Integer.valueOf(
                                              strTok.nextToken()).intValue();
            System.out.println("Number of atoms: " + atoms);
            System.out.println("Number of bonds: " + bonds);
            conMat = new int[atoms][atoms];
            for (int f = 0; f < atoms; f++)
                {
                    strBuff = new StringBuffer(RAF.readLine());
                    //                strBuff.insert(3, " ");
                    //                strBuff.insert(7, " ");
                    strTok = new StringTokenizer(strBuff.toString().trim());
                    x = new Double(strTok.nextToken()).doubleValue();
                    y = new Double(strTok.nextToken()).doubleValue();
                    z = new Double(strTok.nextToken()).doubleValue();
                    symbols.addElement(new Node(strTok.nextToken(), x, y, z));
                }
            for (int f = 0; f < bonds; f++)
                {
                    strBuff = new StringBuffer(RAF.readLine());
                    strBuff.insert(3, " ");
                    strBuff.insert(7, " ");
                    strTok = new StringTokenizer(strBuff.toString());
                    atom1 = java.lang.Integer.valueOf(
                                                      strTok.nextToken()).intValue();
                    atom2 = java.lang.Integer.valueOf(
                                                      strTok.nextToken()).intValue();
                    value = java.lang.Integer.valueOf(
                                                      strTok.nextToken()).intValue();
                    conMat[atom1 - 1][atom2 - 1] = value;
                    conMat[atom2 - 1][atom1 - 1] = value;
                }
            RAF.close();
            System.out.println("File scan successful");
            System.out.println();
        }
        catch (Exception e)
        {
            System.err.println("Error while reading MDL Molfile.");
            System.err.println("Usage: JMDraw MDL-Molfile-name");
            System.err.println("Reason for failure: " + e.toString());
        }
        for (int f = 0; f < symbols.size(); f ++)
        {
            ((Node)symbols.elementAt(f)).number = f;
            ((Node)symbols.elementAt(f)).degree = 0;
            for (int g = 0; g < symbols.size(); g ++)
            {
                if (conMat[f][g] > 0)
                    {
                        ((Node)symbols.elementAt(f)).nodeTable[((Node)symbols.elementAt(f)).degree] = g;
                        ((Node)symbols.elementAt(f)).bondTable[((Node)symbols.elementAt(f)).degree] = conMat[f][g];
                        ((Node)symbols.elementAt(f)).degree ++;
                    }
            }
        }
        StandardElements stdE = new StandardElements();
        atSet = new Node[symbols.size()];
        for (int f = 0; f < symbols.size(); f ++)
        {
            ((Node)symbols.elementAt(f)).maxBondOrderSum =
                stdE.getMaxBondOrderSum(((Node)symbols.elementAt(f)).symbol);
            ((Node)symbols.elementAt(f)).saturate();
            atSet[f] = (Node)symbols.elementAt(f);
        }
        return atSet;
    }
    
    
    public static void writeMDLMolFile(String fQFilename, String name, Node[] setOfNodes)
    {
        File file = new File(fQFilename);
        writeMDLMolFile(file, name, setOfNodes);
        
    }
    public static void writeMDLMolFile(File file, String name, Node[] setOfNodes)
    {
        int N = setOfNodes.length;
        String line = "";
        int ct[][] = DataStructureTools.createConnectionTable(setOfNodes);
        int bonds = 0;
        for (int f = 0; f < N; f++)
            {
                for (int g = f + 1; g < N; g++)
                    {
                        if (ct[f][g] > 0)
                            {
                                bonds ++;
                                ;
                            }
                    }
            }
        try
            {
                FileWriter fwr = new FileWriter(file);
                fwr.write(name + "\n");
                fwr.write("  SENECA" + "\n");
                fwr.write("\n");
                line += formatMDLInt(setOfNodes.length, 3);
                line += formatMDLInt(bonds, 3);
                line += "  0  0  0  0  0  0  0  0  1 V2000";
                fwr.write(line + "\n");
                for (int f = 0; f < N; f++)
                    {
                        line = "";
                        line += formatMDLFloat((float) setOfNodes[f].x);
                        line += formatMDLFloat((float) setOfNodes[f].y);
                        line += formatMDLFloat((float) 0);
                        line += " ";
                        line += formatMDLString(setOfNodes[f].symbol, 3);
                        line += " 0  0  0  0  0  0  0  0  0  0  0  0";
                        fwr.write(line + "\n");
                    }
                
                for (int f = 0; f < N; f++)
                    {
                        for (int g = f + 1; g < N; g++)
                            {
                                if (ct[f][g] > 0)
                                    {
                                        line = "";
                                        line += formatMDLInt(f + 1, 3);
                                        line += formatMDLInt(g + 1, 3);
                                        line += formatMDLInt(ct[f][g], 3);
                                        line += "  0  0  0  0";
                                        fwr.write(line + "\n");
                                    }
                            }
                    }
                fwr.write("M  END\n");
                
                fwr.flush();
                fwr.close();
                
                
            }
        catch (Exception exc)
            {}
    }
    
    
    /** Parses a SMILES string and returns an array of nodes. So far only the
        SSMILES subset and the '%' tag for more than 10 rings at a time are
        supported, but this should be sufficient for most organic molecules.*/
    public static Node[] parseSmiles(String args)
    {
        int position = 0;
        int bondStatus = 0;
        String currentSymbol;
        char mychar;
        char[] chars = new char[1];
        int nodeCounter = 0;
        int counter = 0;
        int lastNode = -1;
        int thisNode = -1;
        int[] rings = new int[1024];
        int thisRing = -1;
        for (int f = 0; f < 1024; f++)
        rings[f] = -1;
        Stack stack = new Stack();
        // Scan SSMILES string for occurrence of capital letters.
        // This gives us the number of nodes to initialize.
        
        for (int f = 0; f < args.length(); f++)
        {
            if (args.charAt(f) >= 'A' && args.charAt(f) <= 'Z')
                counter ++;
        }
        Node[] atSet = new Node[counter];
        do
        {
            mychar = args.charAt(position);
            if (mychar >= 'A' && mychar <= 'Z')
                {
                    currentSymbol = getElementSymbol(args, position);
                    atSet[nodeCounter] = new Node(currentSymbol, nodeCounter);
                    if (lastNode != -1)
                        {
                            atSet[nodeCounter].nodeTable[
                                                         atSet[nodeCounter].degree] = lastNode;
                            atSet[nodeCounter].bondTable[
                                                         atSet[nodeCounter].degree] = bondStatus;
                            atSet[nodeCounter].degree ++;
                            atSet[lastNode].nodeTable[atSet[lastNode].degree] =
                                nodeCounter;
                            atSet[lastNode].bondTable[atSet[lastNode].degree] =
                                bondStatus;
                            atSet[lastNode].degree ++;
                        }
                    lastNode = nodeCounter;
                    nodeCounter ++;
                    bondStatus = 1;
                    position = position + currentSymbol.length();
                }
            else if (mychar == '=')
                {
                    bondStatus = 2;
                    position ++;
                }
            else if (mychar == '#')
                {
                    bondStatus = 3;
                    position ++;
                }
            else if (mychar == '(')
                {
                    stack.push(new Integer(lastNode));
                    position ++;
                }
            else if (mychar == ')')
                {
                    lastNode = ((Integer) stack.pop()).intValue();
                    position ++;
                }
            else if (mychar >= '0' && mychar <= '9')
                {
                    chars[0] = mychar;
                    thisRing = (new Integer(new String(chars))).intValue();
                    thisNode = rings[thisRing];
                    if (thisNode >= 0)
                        {
                            atSet[nodeCounter - 1].nodeTable[
                                                             atSet[nodeCounter - 1].degree] = thisNode;
                            atSet[nodeCounter - 1].bondTable[
                                                             atSet[nodeCounter - 1].degree] = bondStatus;
                            atSet[nodeCounter - 1].degree ++;
                            atSet[thisNode].nodeTable[atSet[thisNode].degree] =
                                nodeCounter - 1;
                            atSet[thisNode].bondTable[atSet[thisNode].degree] =
                                bondStatus;
                            atSet[thisNode].degree ++;
                            bondStatus = 1;
                            rings[thisRing] = -1;
                        }
                    else
                        rings[thisRing] = nodeCounter - 1;
                    position ++;
                }
            else if (mychar == '%')
                {
                    currentSymbol = getRingNumber(args, position);
                    //System.out.println(currentSymbol);
                    thisRing = (new Integer(currentSymbol)).intValue();
                    thisNode = rings[thisRing];
                    if (thisNode >= 0)
                        {
                            atSet[nodeCounter - 1].nodeTable[
                                                             atSet[nodeCounter - 1].degree] = thisNode;
                            atSet[nodeCounter - 1].bondTable[
                                                             atSet[nodeCounter - 1].degree] = bondStatus;
                            atSet[nodeCounter - 1].degree ++;
                            atSet[thisNode].nodeTable[atSet[thisNode].degree] =
                                nodeCounter - 1;
                            atSet[thisNode].bondTable[atSet[thisNode].degree] =
                                bondStatus;
                            atSet[thisNode].degree ++;
                            bondStatus = 1;
                        }
                    else
                        rings[thisRing] = nodeCounter - 1;
                    position += currentSymbol.length() + 1;
                }

            else
                {
                    System.out.println("Error while parsing SMILES string!");
                    return null;
                }
        }
        while (position < args.length())
        ;


        return atSet;
    }

    //This method assumes that the character at 'pos' is 'A-Z'

    private static String getElementSymbol(String s, int pos)
    {
        char mychar = ' ';
        if (pos < s.length() - 1)
            mychar = s.charAt(pos + 1);
        if (mychar >= 'a' && mychar <= 'z')
            return s.substring(pos, pos + 2);
        else
            return s.substring(pos, pos + 1);
    }

    private static String getRingNumber(String s, int pos)
    {
        char mychar = ' ';
        String retString = "";
        pos ++;
        do
            {
                retString += s.charAt(pos);
                pos ++;
            }
        while (pos < s.length() &&
               (s.charAt(pos) >= '0' && s.charAt(pos) <= '9'))
            ;
        return retString;
    }

    private static String formatMDLInt(int i, int l)
    {
        String s = "", fs = "";
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setParseIntegerOnly(true);
        nf.setMinimumIntegerDigits(1);
        nf.setMaximumIntegerDigits(l);
        nf.setGroupingUsed(false);
        s = nf.format(i);
        l = l - s.length();
        for (int f = 0; f < l; f++)
            fs += " ";
        fs += s;
        return fs;
    }

    private static String formatMDLFloat(float fl)
    {
        String s = "", fs = "";
        int l;
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumIntegerDigits(1);
        nf.setMaximumIntegerDigits(4);
        nf.setMinimumFractionDigits(4);
        nf.setMaximumFractionDigits(4);
        nf.setGroupingUsed(false);
        s = nf.format(fl);
        l = 10 - s.length();
        for (int f = 0; f < l; f++)
            fs += " ";
        fs += s;
        return fs;
    }


    private static String formatMDLString(String s, int le)
    {
        s = s.trim();
        if (s.length() > le)
            return s.substring(0, le);
        int l;
        l = le - s.length();
        for (int f = 0; f < l; f++)
            s += " ";
        return s;
    }

    public static Node[] readLUCYStructure(String args)
    {
        Node[] atSet;
        int atoms = 0, bonds = 0, atom1 = 0, atom2 = 0, value = 0,
        partner = 0;
        int[][] conMat = new int[0][0];
        String help;
        Vector symbols = new Vector();
        atSet = new Node[1];
        try
        {
            System.out.println("Attempt to read file " + args);
            RandomAccessFile RAF = new RandomAccessFile(args, "r");
            System.out.println(RAF.readLine());
            StringBuffer strBuff = new StringBuffer(RAF.readLine());
            StringTokenizer strTok =
                new StringTokenizer(strBuff.toString());
            atoms = java.lang.Integer.valueOf(
                                              strTok.nextToken()).intValue();
            bonds = java.lang.Integer.valueOf(
                                              strTok.nextToken()).intValue();
            System.out.println("Number of atoms: " + atoms);
            System.out.println("Number of bonds: " + bonds);
            conMat = new int[atoms][atoms];
            atSet = new Node[conMat.length];
            for (int f = 0; f < atoms; f++)
                {
                    atSet[f] = new Node();
                    strBuff = new StringBuffer(RAF.readLine());
                    strTok = new StringTokenizer(strBuff.toString().trim());
                    help = strTok.nextToken();
                    atSet[f].symbol = strTok.nextToken();
                    help = strTok.nextToken();
                    help = strTok.nextToken();
                    atSet[f].HCount = Integer.parseInt(strTok.nextToken());
                    atSet[f].number = f;
                    atSet[f].degree = 0;
                }

            for (int f = 0; f < bonds; f++)
                {
                    strBuff = new StringBuffer(RAF.readLine());
                    strTok = new StringTokenizer(strBuff.toString());
                    atom1 = java.lang.Integer.valueOf(
                                                      strTok.nextToken()).intValue();
                    atom2 = java.lang.Integer.valueOf(
                                                      strTok.nextToken()).intValue();
                    value = java.lang.Integer.valueOf(
                                                      strTok.nextToken()).intValue();
                    conMat[atom1 - 1][atom2 - 1] = value;
                    conMat[atom2 - 1][atom1 - 1] = value;
                }
            RAF.close();
            System.out.println("File scan successful");
            System.out.println();
        }
        catch (Exception e)
        {
            System.err.println("Error while reading LUCY result file.");
            System.err.println("Reason for failure: " + e.toString());
        }

        for (int f = 0; f < conMat.length; f ++)
        {
            for (int g = 0; g < conMat.length; g ++)
            {
                if (conMat[f][g] > 0)
                {
                    atSet[f].nodeTable[atSet[f].degree] = g;
                    atSet[f].bondTable[atSet[f].degree] = conMat[f][g];
                    atSet[f].degree ++;
                }
            }
        }
        StandardElements stdE = new StandardElements();
        for (int f = 0; f < conMat.length; f ++)
        {
            atSet[f].maxBondOrderSum =
                stdE.getMaxBondOrderSum(atSet[f].symbol);
            atSet[f].bondOrderSum = atSet[f].getBondCount();
        }
        for (int i = 1; i < 4; i++)
        {
            // handle atoms with degree 1 first and then proceed to higher order
            for (int f = 0; f < conMat.length; f ++)
            {
                if (atSet[f].degree == i)
                {
                    if (atSet[f].bondOrderSum <
                        atSet[f].maxBondOrderSum - atSet[f].HCount)
                    {
                        for (int g = 0; g < atSet[f].degree; g ++)
                        {
                            partner = atSet[f].nodeTable[g];
                            if (atSet[partner].bondOrderSum <
                                atSet[partner].maxBondOrderSum -
                                atSet[partner].HCount)
                            {
                                atSet[f].bondTable[g]++;
                                atSet[f].bondOrderSum ++;
                                for (int h = 0;
                                     h < atSet[partner].degree; h++)
                                    {
                                        if (atSet[partner].nodeTable[h] == f)
                                            {
                                                atSet[partner].bondTable[h]++;
                                                atSet[partner].bondOrderSum ++;
                                                break;
                                            }
                                    }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return atSet;
    }
}
