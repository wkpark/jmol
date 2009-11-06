/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-16 14:11:08 -0500 (Sat, 16 Sep 2006) $
 * $Revision: 5569 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;

import java.util.Hashtable;
import java.util.Vector;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

abstract public class GamessReader extends MOReader {

  protected Vector atomNames;

  abstract protected void readAtomsInBohrCoordinates() throws Exception;  
 
  protected void readGaussianBasis(String initiator, String terminator) throws Exception {
    Vector gdata = new Vector();
    gaussianCount = 0;
    int nGaussians = 0;
    shellCount = 0;
    String thisShell = "0";
    String[] tokens;
    discardLinesUntilContains(initiator);
    readLine();
    int[] slater = null;
    Hashtable shellsByAtomType = new Hashtable();
    Vector slatersByAtomType = new Vector();
    String atomType = null;
    
    while (readLine() != null && line.indexOf(terminator) < 0) {
      //System.out.println(line);
      if (line.indexOf("(") >= 0)
        line = GamessReader.fixBasisLine(line);
      tokens = getTokens();
      switch (tokens.length) {
      case 1:
        if (atomType != null) {
          if (slater != null) {
            slater[2] = nGaussians;
            slatersByAtomType.addElement(slater);
            slater = null;
          }
          shellsByAtomType.put(atomType, slatersByAtomType);
        }
        slatersByAtomType = new Vector();
        atomType = tokens[0];
        break;
      case 0:
        break;
      default:
        if (!tokens[0].equals(thisShell)) {
          if (slater != null) {
            slater[2] = nGaussians;
            slatersByAtomType.addElement(slater);
          }
          thisShell = tokens[0];
          shellCount++;
          slater = new int[] {
              JmolAdapter.getQuantumShellTagID(fixShellTag(tokens[1])), gaussianCount,
              0 };
          nGaussians = 0;
        }
        ++nGaussians;
        ++gaussianCount;
        gdata.addElement(tokens);
      }
    }
    if (slater != null) {
      slater[2] = nGaussians;
      slatersByAtomType.addElement(slater);
    }
    if (atomType != null)
      shellsByAtomType.put(atomType, slatersByAtomType);
    gaussians = new float[gaussianCount][];
    for (int i = 0; i < gaussianCount; i++) {
      tokens = (String[]) gdata.get(i);
      gaussians[i] = new float[tokens.length - 3];
      for (int j = 3; j < tokens.length; j++)
        gaussians[i][j - 3] = parseFloat(tokens[j]);
    }
    int atomCount = atomNames.size();
    if (shells == null && atomCount > 0) {
      shells = new Vector();
      for (int i = 0; i < atomCount; i++) {
        atomType = (String) atomNames.elementAt(i);
        Vector slaters = (Vector) shellsByAtomType.get(atomType);
        if (slaters == null) {
          Logger.error("slater for atom " + i + " atomType " + atomType
              + " was not found in listing. Ignoring molecular orbitals");
          return;
        }
        for (int j = 0; j < slaters.size(); j++) {
          slater = (int[]) slaters.elementAt(j);
          shells.addElement(new int[] { i, slater[0], slater[1], slater[2] });
        }
      }
    }

    if (Logger.debugging) {
      Logger.debug(shellCount + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
  }

  abstract protected String fixShellTag(String tag);

  protected void readFrequencies() throws Exception {
    //not for GamessUK yet
    int atomIndex = atomSetCollection.getLastAtomSetAtomIndex();
    int atomCount = atomSetCollection.getLastAtomSetAtomCount();
    // For the case when HSSEND=.TRUE. atoms[]
    // now contains all atoms across all models (optimization steps).
    // We only want to set vetor data corresponding to new cloned
    // models and not interfere with the previous ones.
    float[] xComponents = new float[5];
    float[] yComponents = new float[5];
    float[] zComponents = new float[5];
    float[] frequencies = new float[5];
    discardLinesUntilContains("FREQUENCY:");
    while (line != null && line.indexOf("FREQUENCY:") >= 0) {
      int lineFreqCount = 0;
      String[] tokens = getTokens();
      for (int i = 0; i < tokens.length; i++) {
        float frequency = parseFloat(tokens[i]);
        if (tokens[i].equals("I"))
          frequencies[lineFreqCount - 1] = -frequencies[lineFreqCount - 1];
        if (Float.isNaN(frequency))
          continue; // may be "I" for imaginary
        frequencies[lineFreqCount] = frequency;
        lineFreqCount++;
        if (Logger.debugging) {
          Logger.debug((vibrationNumber + 1) + " frequency=" + frequency);
        }
        if (lineFreqCount == 5)
          break;
      }
      String[] red_masses = null;
      String[] intensities = null;
      readLine();
      if (line.indexOf("MASS") >= 0) {
        red_masses = getTokens();
        readLine();
      }
      if (line.indexOf("INTENS") >= 0) {
        intensities = getTokens();
      }
      boolean[] ignore = new boolean[lineFreqCount];
      for (int i = 0; i < lineFreqCount; i++) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        // The last model should be cloned because we might
        // have done an optimization with HSSEND=.TRUE.
        if (ignore[i])
          continue;
        if (vibrationNumber > 1)
          atomSetCollection.cloneLastAtomSet();
        atomSetCollection.setAtomSetName(frequencies[i] + " cm-1");
        atomSetCollection.setAtomSetProperty("Frequency", frequencies[i]
            + " cm-1");
        if (red_masses != null)
          atomSetCollection.setAtomSetProperty("Reduced Mass", red_masses[i + 2]
            + " AMU");
        if (intensities != null)
          atomSetCollection.setAtomSetProperty("IR Intensity", intensities[i + 2]
            + " D^2/AMU-Angstrom^2");

      }
      discardLinesUntilBlank();
      //This loop is over the atoms in the most recent set.
      //The number of atoms will not change, but atomIndex will have been updated
      int index0 = atomIndex - atomCount;
      for (int i = 0; i < atomCount; ++i) {
        atomIndex = index0 + i;
        readLine();
        readComponents(lineFreqCount, xComponents);
        readLine();
        readComponents(lineFreqCount, yComponents);
        readLine();
        readComponents(lineFreqCount, zComponents);
        //This loop applies the normal mode displacements 
        //to atom i across all clones (frequencies) by finding
        //its position in atoms[].
        for (int j = 0; j < lineFreqCount; ++j) {
          atomIndex += atomCount;
          if (!ignore[j])
            atomSetCollection
               .addVibrationVector(atomIndex, xComponents[j], yComponents[j], zComponents[j]);
        }
      }
      atomIndex++;
      discardLines(12);
      readLine();
    }
  }

  private void readComponents(int count, float[] components) {
    for (int i = 0, start = 20; i < count; ++i, start += 12)
      components[i] = parseFloat(line, start, start + 12);
  }

  protected static String fixBasisLine(String line) {
    int pt, pt1;
    line = line.replace(')', ' ');
    while ((pt = line.indexOf("(")) >= 0) {
      pt1 = pt;
      while (line.charAt(--pt1) == ' '){}
      while (line.charAt(--pt1) != ' '){}
      line = line.substring(0, ++pt1) + line.substring(pt + 1);
    }
    return line;
  }
  
  /*
  BASIS OPTIONS
  -------------
  GBASIS=N311         IGAUSS=       6      POLAR=DUNNING 
  NDFUNC=       3     NFFUNC=       1     DIFFSP=       T
  NPFUNC=       3      DIFFS=       T
  SPLIT3=     4.00000000     1.00000000     0.25000000


  $CONTRL OPTIONS
  ---------------
SCFTYP=UHF          RUNTYP=OPTIMIZE     EXETYP=RUN     
MPLEVL=       2     CITYP =NONE         CCTYP =NONE         VBTYP =NONE    
DFTTYP=NONE         TDDFT =NONE    
MULT  =       3     ICHARG=       0     NZVAR =       0     COORD =UNIQUE  
PP    =NONE         RELWFN=NONE         LOCAL =NONE         NUMGRD=       F
ISPHER=       1     NOSYM =       0     MAXIT =      30     UNITS =ANGS    
PLTORB=       F     MOLPLT=       F     AIMPAC=       F     FRIEND=        
NPRINT=       7     IREST =       0     GEOM  =INPUT   
NORMF =       0     NORMP =       0     ITOL  =      20     ICUT  =       9
INTTYP=BEST         GRDTYP=BEST         QMTTOL= 1.0E-06

$SYSTEM OPTIONS
*/



  private Hashtable calcOptions;
  private boolean isTypeSet;

  protected void setCalculationType() {
    if (calcOptions == null || isTypeSet)
      return;
    isTypeSet = true;
    String SCFtype = (String) calcOptions.get("contrl_options_SCFTYP");
    String Runtype = (String) calcOptions.get("contrl_options_RUNTYP");
    String igauss = (String) calcOptions.get("basis_options_IGAUSS");
    String gbasis = (String) calcOptions.get("basis_options_GBASIS");
    boolean DFunc = !"0".equals((String) calcOptions
        .get("basis_options_NDFUNC"));
    boolean PFunc = !"0".equals((String) calcOptions
        .get("basis_options_NPFUNC"));
    boolean FFunc = !"0".equals((String) calcOptions
        .get("basis_options_NFFUNC"));
    String DFTtype = (String) calcOptions.get("contrl_options_DFTTYP");
    int perturb = parseInt((String) calcOptions.get("contrl_options_MPLEVL"));
    String CItype = (String) calcOptions.get("contrl_options_CITYP");
    String CCtype = (String) calcOptions.get("contrl_options_CCTYP");

    if (igauss == null && SCFtype == null)
      return;

    if (calculationType.equals("?"))
      calculationType = "";

    if (igauss != null) {
      if ("0".equals(igauss)) { // we have a non Pople basis set.
        // most common translated to standard notation, others in GAMESS
        // internal format.
        boolean recognized = false;
        if (calculationType.length() > 0)
          calculationType += " ";
        if (gbasis.startsWith("ACC"))
          calculationType += "aug-cc-p";
        if (gbasis.startsWith("CC"))
          calculationType += "cc-p";
        if ((gbasis.startsWith("ACC") || gbasis.startsWith("CC"))
            && gbasis.endsWith("C"))
          calculationType += "C";
        if (gbasis.contains("CCD")) {
          calculationType += "VDZ";
          recognized = true;
        }
        if (gbasis.contains("CCT")) {
          calculationType += "VTZ";
          recognized = true;
        }
        if (gbasis.contains("CCQ")) {
          calculationType += "VQZ";
          recognized = true;
        }
        if (gbasis.contains("CC5")) {
          calculationType += "V5Z";
          recognized = true;
        }
        if (gbasis.contains("CC6")) {
          calculationType += "V6Z";
          recognized = true;
        }
        if (!recognized)
          calculationType += gbasis;
      } else {
        if (calculationType.length() > 0)
          calculationType += " ";
        calculationType += igauss + "-"
            + TextFormat.simpleReplace(gbasis, "N", "");
        if ("T".equals((String) calcOptions.get("basis_options_DIFFSP"))) {
          // check if we have diffuse S on H's too => "++" instead of "+"
          if ("T".equals((String) calcOptions.get("basis_options_DIFFS")))
            calculationType += "+";
          calculationType += "+";
        }
        calculationType += "G";
        // append (d,p) , (d), (f,d,p), etc. to indicate polarization.
        // not using * and ** notation as it is inconsistent.
        if (DFunc || PFunc || FFunc) {
          calculationType += "(";
          if (FFunc) {
            calculationType += "f";
            if (DFunc || PFunc)
              calculationType += ",";
          }
          if (DFunc) {
            calculationType += "d";
            if (PFunc)
              calculationType += ",";
          }
          if (PFunc)
            calculationType += "p";
          calculationType += ")";
        }
      }
      if (DFTtype!=null && !DFTtype.contains("NONE")) {
        if (calculationType.length() > 0)
          calculationType += " ";
        calculationType += DFTtype;
      }
      if (CItype !=null && !CItype.contains("NONE")) {
        if (calculationType.length() > 0)
          calculationType += " ";
        calculationType += CItype;
      }
      if (CCtype !=null && !CCtype.contains("NONE")) {
        if (calculationType.length() > 0)
          calculationType += " ";
        calculationType += CCtype;
      }
      if (perturb > 0) {
        if (calculationType.length() > 0)
          calculationType += " ";
        calculationType += "MP" + perturb;
      }
      if (SCFtype != null) {
        if (calculationType.length() > 0)
          calculationType += " ";
        calculationType += SCFtype + " " + Runtype;
      }
    }
  }

  protected void readControlInfo() throws Exception {
    readCalculationInfo("contrl_options_");
  }

  protected void readBasisInfo() throws Exception {
    readCalculationInfo("basis_options_");
  }

  private void readCalculationInfo(String type) throws Exception {
    if (calcOptions == null) {
      calcOptions = new Hashtable();
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("calculationOptions",
          calcOptions);
    }
    while (readLine() != null && (line = line.trim()).length() > 0) {
      if (line.indexOf("=") < 0)
        continue;
      String[] tokens = getTokens(TextFormat.simpleReplace(line, "="," = "));
      for (int i = 0; i < tokens.length; i++) {
        if (!tokens[i].equals("="))
          continue;
        try {
        String key = type + tokens[i - 1];
        String value = (key.equals("basis_options_SPLIT3") ? tokens[++i] + " " + tokens[++i]
            + " " + tokens[++i] : tokens[++i]);
        if (Logger.debugging)
          Logger.debug(key + " = " + value);
        calcOptions.put(key, value);
        } catch (Exception e) {
          // not interested
        }
      }
    }
  }


}
