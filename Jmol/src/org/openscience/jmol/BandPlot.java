/*
 * Copyright 2002 The Jmol Development Team
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
import org.openscience.jmol.util.*;

import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;
import java.io.File;

public class BandPlot {

  EnergyBand energyBand;
  Vector bandPlotSections = new Vector(0); // a BandPlotSection vector
  String plotDef;
  int energyUnits;
  double conversionFactor;
  
  double fontsize1;   // x  (in pixel)
  double fontsize2;   // y  (in pixel)
  double fontsize3;   // y label (in pixel)
  double ticSize; //   (in pixel)
  
  // Y coordinate. We use an *absolute* behavior
  double minE;
  double maxE;
  double fermiE;
  
  
  int nvtics;
  int nhtics;
  String yLabel;

  //if true, rounds energies according to the number
  // of decimal figures. If false rounds energies according to
  // the number of significative figures
  int roundScheme=0; 
  int nRound=2;
  
  
  
  int bigSepNumber=0; //Number of big separations in the plot.
  double bigSepSize;
  
  //  String line; // To generate PostScript

  public BandPlot(EnergyBand energyBand, String plotDef,
			int energyUnits,
			double minE, double maxE, double fermiE,
			int nRound, int roundScheme,
			int nvtics, int nhtics, int ticSize,
			double fontsize1, double fontsize2, double fontsize3,
			double bigSepSize, String yLabel)
    throws ParseErrorException {
    
    this.energyBand = energyBand;
    this.plotDef = plotDef;
    this.energyUnits = energyUnits;
    conversionFactor = (double)Units.getConversionFactor
      (energyBand.getEnergyUnits(), energyUnits);
    this.nRound = nRound;
    this.roundScheme = roundScheme;
    this.nvtics = nvtics;
    this.nhtics = nhtics;
    this.ticSize = ticSize;
    this.fontsize1 = fontsize1;
    this.fontsize2 = fontsize2;
    this.fontsize3 = fontsize3;


    this.minE = minE;
    this.maxE = maxE;
    this.fermiE= fermiE;
    this.bigSepSize = bigSepSize;
    this.yLabel = yLabel;

    
    parse();


  }
  
  public class ParseErrorException extends Exception {

    public ParseErrorException(int index) {
      //super("Parse Error. Position: " + index);

    }
  }

  private void parse() throws ParseErrorException {
    
    StringTokenizer st = new StringTokenizer(plotDef, "[],;-{}", true);
    String s="";
    boolean isInt;
    boolean reverse=false;
    int value = 0;
    EnergyBand.KLine kLine=null;
    BandPlotSection section=null;
    
    final int WAIT_LINEINDEX = 0;
    final int WAIT_FIRSTINDEX = 1;
    final int WAIT_SECONDINDEX = 2;
    final int WAIT_LABEL = 3;
    final int WAIT_SEP = 4;
    int flag = WAIT_LINEINDEX;
    
    String sepString = "";
    boolean isFirstIndex = true;
    boolean sepFound;
    int lineIndex;
    int parseIndex=0; //use to generate a ParseError

    try {
      while (st.hasMoreTokens()) {
	s = st.nextToken();
	parseIndex = parseIndex+ s.length();
	
	isInt = true;
	try { value = Integer.parseInt(s);
	} catch (NumberFormatException e){
	  isInt = false;
	}
	
      	switch (flag) {
	case WAIT_SEP:
	  sepFound= false;
	  for (int i=0; i<sepString.length(); i++) {
	    if (s.charAt(0) == (sepString.charAt(i))) {
	      switch (s.charAt(0)) {
	      case ',': 
		flag = WAIT_LINEINDEX;
		section.endDelimiter = BandPlotSection.VLINE;
		reverse=false;
		break;
	      case ';':
		flag = WAIT_LINEINDEX;	
		section.endDelimiter = BandPlotSection.BIGSEP;
		bigSepNumber++;
		reverse=false;
		break;
	      case '[': flag = WAIT_FIRSTINDEX; break;
	      case '-': flag = WAIT_SECONDINDEX; break;
	      case '{': flag = WAIT_LABEL; break;
	      case ']': flag = WAIT_SEP; sepString=",;"; break;
	      case '}': 
		flag = WAIT_SEP; 
		if (isFirstIndex) {
		  sepString="-";
		} else {
		  sepString="]";
		}
		break;
	      }
	      sepFound= true;
	    }
	  }
	  if (sepFound == false) {
	    throw new ParseErrorException(parseIndex);
	  }
	  break;
	case WAIT_LINEINDEX:
	  if (s.equals("-")) {
	    reverse = true;
	  } else if (isInt) {
	    lineIndex = value;
	    section = new BandPlotSection(lineIndex, reverse);
	    kLine = energyBand.getKLine(lineIndex);
	    bandPlotSections.addElement(section);
	    flag = WAIT_SEP; sepString=",;["; 
	  } else {
	    throw new ParseErrorException(parseIndex);
	  }
	break;
	case WAIT_FIRSTINDEX:
	  section.origName="";
	  section.endName="";
	  if (isInt) {
	    if (reverse) {
	      section.endIndex = value;
	      if (value == 0) {
		section.endName = kLine.getOriginName();
	      }
	      else if (value == kLine.getNumberOfkPoints()-1){
		section.endName = kLine.getEndName();
	      } 
	    } else {
	      section.origIndex = value;
	      if (value == 0) {
		section.origName = kLine.getOriginName();
	      }
	      else if (value == kLine.getNumberOfkPoints()-1){
		section.origName = kLine.getEndName();
	      } 
	    }
	    flag = WAIT_SEP; sepString="{-"; isFirstIndex = true;
	  } else {
	    throw new ParseErrorException(parseIndex);
	  }
	  break;
	case WAIT_SECONDINDEX:
	  if (isInt) {
	    if (reverse) {
	      section.origIndex = value;
	      if (value == 0) {
		section.origName = kLine.getOriginName();
	      }
	      else if (value == kLine.getNumberOfkPoints()-1){
		section.origName = kLine.getEndName();
	      }
	    } else {
	      section.endIndex = value;
	      if (value == 0) {
		section.endName = kLine.getOriginName();
	      }
	      else if (value == kLine.getNumberOfkPoints()-1){
		section.endName = kLine.getEndName();
	      }
	    }
	    flag = WAIT_SEP; sepString="{]"; isFirstIndex = false;
	  } else {
	    throw new ParseErrorException(parseIndex);
	  }
	  break;
	case WAIT_LABEL:
	  if (isFirstIndex) {
	    if (reverse) {
	      section.endName = s;
	    } else {
	      section.origName = s;
	    }
	  } else {
	    if (reverse) {
	      section.origName = s;
	    } else {
	      section.endName = s;
	    }
	  }
	  flag = WAIT_SEP; sepString="}";
	  break;
	} //end swith
      } //end while
    } catch (ParseErrorException parseError) {
      JOptionPane.showMessageDialog(null, plotDef + 
				    "  !!Error at position "+ (parseIndex-1),
				    "Parse Error",
				    JOptionPane.ERROR_MESSAGE);
      throw new ParseErrorException(parseIndex);      
    } 

  } //end parse
  
  
  class BandPlotSection {
    int lineIndex;
    int origIndex;
    String origName;
    int endIndex;
    String endName;
    int endDelimiter;
    final static int NONE = 0;
    final static int VLINE = 1;
    final static int BIGSEP = 2;
    
    BandPlotSection(int lineIndex, boolean reverse) {
      this.lineIndex = lineIndex;
      //default value
      if (reverse) {
	origIndex = energyBand.getKLine(lineIndex).getNumberOfkPoints()-1;
	origName = energyBand.getKLine(lineIndex).getEndName();
	endIndex =  0; 
	endName = energyBand.getKLine(lineIndex).getOriginName();
	
      } else {
	origIndex = 0; 
	origName = energyBand.getKLine(lineIndex).getOriginName();
	endIndex = energyBand.getKLine(lineIndex).getNumberOfkPoints()-1;
	endName = energyBand.getKLine(lineIndex).getEndName();
      }
    }
    
  } //end class BandPlotSection


 
  
} //end class BandPlot
