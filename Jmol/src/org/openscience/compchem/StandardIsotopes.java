/* StandardIsotopes.java
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

import java.util.*;
import java.io.*;

/** StandardsIsotopes.java
 * Used to store data of a particular isotope
 */
 
public class StandardIsotopes{
    Isotope[] isotopes = new Isotope[300];
    int counter = 0;
 
    public StandardIsotopes(String FileName){
        int token;
        try{
            StreamTokenizer st = new StreamTokenizer(new BufferedReader(new FileReader(FileName)));
            do{
                isotopes[counter] = new Isotope();
                if (st.nextToken() == st.TT_NUMBER){
                    isotopes[counter].atomicMass = (int)st.nval;
                }
                else if (st.ttype == st.TT_EOF) break;
                else{
                    throw new java.lang.Exception("Error: wrong isotope file format!"); 
                }
    
                if (st.nextToken() == st.TT_WORD){
                    isotopes[counter].symbol = st.sval;
                }
                else{
                    throw new java.lang.Exception("Error: wrong isotope file format!"); 
                }
                if (st.nextToken() == st.TT_NUMBER){
                    isotopes[counter].exactMass = (double)st.nval;
                }
                else{
                    throw new java.lang.Exception("Error: wrong isotope file format!"); 
                }
                if (st.nextToken() == st.TT_NUMBER){
                    isotopes[counter].naturalAbundance = (float)st.nval;
                }
                else{
                    throw new java.lang.Exception("Error: wrong isotope file format!"); 
                }
                if (counter < 300) counter ++;   
                else{
                    enlargeIsotopeArray();
                    counter ++;
                }

            }while(true);
        }
        catch(java.lang.Exception fne){
            System.err.println(fne.toString());      
        }
    }
  
    public int getSize(){
        return counter; 
    }
  
    public Isotope elementAt(int pos){
        if (pos >= 0 && pos < counter) return (Isotope)isotopes[pos].clone();
   
        return null;
    }
  
    /** returns the major, i.e. the most abundant isotope whose symbol
        euquals element */
    public Isotope getMajorIsotope(String element){
        Isotope isotope = null;
        for (int f = 0; f < counter; f++){
            if (isotopes[f].symbol.equals(element)){
                if ((int)isotopes[f].naturalAbundance == 100){
                    isotope = (Isotope)isotopes[f].clone();  
                }
            }
            if (isotope != null) return isotope;
        }
        return null;
    }
  
    private void enlargeIsotopeArray(){
        int newSize = (int)(isotopes.length * 1.1);
        Isotope[] newIsotopes = new Isotope[newSize];
        System.arraycopy(isotopes, 0, newIsotopes, 0, isotopes.length); 
        isotopes = newIsotopes;
    }
}
