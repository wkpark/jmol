/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.quantum;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolNMRInterface;
import org.jmol.io.JmolBinary;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;


/*
 * 
 * Bob Hanson hansonr@stolaf.edu 7/4/2013
 * 
 */

/*
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 * 
 */

public class NMRCalculation implements JmolNMRInterface {
  
  public NMRCalculation() {
    getData();
  }

  /**
   * isotopeData keyed by nnnSym, for example: 1H, 19F, etc.;
   * and also by element name itself:  H, F, etc., for default
   */
  private Map<String, float[]> isotopeData;
  
  /**
   * NOTE! Do not change this txt file! 
   * Instead, edit trunk/Jmol/_documents/nmr_data.xls.
   * 
   */
  private final static String resource = "org/jmol/quantum/nmr_data.txt";

  /**
   * Get magnetogyricRatio (gamma/10^7 rad s^-1 T^-1) and quadrupoleMoment (Q/10^-2 fm^2)
   * for a given isotope or for the default isotope of an element.
   * 
   * @param isoSym may be an element symbol (H, F) or an isotope_symbol (1H, 19F)
   * @return  [g, Q]
   */
  public float[] getIsotopeData(String isoSym) {
     return isotopeData.get(isoSym);
  }
  
  private void getData() {
    BufferedReader br = null;
    boolean debugging = Logger.debugging;
    try {
      InputStream is;
      URL url = null;
      if ((url = this.getClass().getResource("nmr_data.txt")) == null) {
        Logger.error("Couldn't find file: " + resource);
        return;
      }
      is = (InputStream) url.getContent();
      br = JmolBinary.getBufferedReader(new BufferedInputStream(is), null);
      String line;
      // "#extracted by Simone Sturniolo from ROBIN K. HARRIS, EDWIN D. BECKER, SONIA M. CABRAL DE MENEZES, ROBIN GOODFELLOW, AND PIERRE GRANGER, Pure Appl. Chem., Vol. 73, No. 11, pp. 1795–1818, 2001. NMR NOMENCLATURE. NUCLEAR SPIN PROPERTIES AND CONVENTIONS FOR CHEMICAL SHIFTS (IUPAC Recommendations 2001)"
      // #element atomNo  isotopeDef  isotope1  G1  Q1  isotope2  G2  Q2  isotope3  G3  Q3
      // H 1 1 1 26.7522128  0 2 4.10662791  0.00286 3 28.5349779  0
      isotopeData = new Hashtable<String, float[]>();
      while ((line = br.readLine()) != null) {
        if (debugging)
          Logger.info(line);
        if (line.indexOf("#") >= 0)
          continue;
        String[] tokens = Parser.getTokens(line);
        String name = tokens[0];
        String defaultIso = tokens[2] + name;
        if (debugging)
          Logger.info(name + " default isotope " + defaultIso);
        for (int i = 3; i < tokens.length; i += 3) {
          String isoname = tokens[i] + name;
          float[] dataGQ = new float[] { Float.parseFloat(tokens[i + 1]),
              Float.parseFloat(tokens[i + 2]) };
          if (debugging)
            Logger.info(isoname + "  " + Escape.eAF(dataGQ));
          isotopeData.put(isoname, dataGQ);
        }
        float[] defdata = isotopeData.get(defaultIso);
        if (defdata == null) {
          Logger.error("Cannot find default NMR data in nmr_data.txt for " + defaultIso);
          throw new NullPointerException();
        }
        isotopeData.put(name, defdata);          
      }
      br.close();
    } catch (Exception e) {
      Logger.error("Exception " + e.toString() + " reading " + resource);
      try {
        br.close();
      } catch (Exception ee) {
        // ignore        
      }
    }
  }
}
