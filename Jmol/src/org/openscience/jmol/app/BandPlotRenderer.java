/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.app;

import org.openscience.jmol.util.*;
import org.openscience.jmol.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

abstract class BandPlotRenderer {

  BandPlot ebp;

  JFrame dummyFrame = new JFrame(); //dummy Frame to compute string width. 
  
  //coordinates: (0,0) is loxer left
  //  x increase right
  //  y increase up
  
  double x;        
  double y;  

  double xorigin; //origin of the plot (the band diagram) axis.
  double yorigin; //minimum energy position. Labels are below
  
  double plotLength;
  double plotHeight;

  double scalex;
  double scaley;
  double ratio;

  public BandPlotRenderer(BandPlot ebp, 
			  double scalex, double ratio) {
    this.ebp = ebp;
    this.scalex = scalex;
    this.ratio = ratio;
    // Set xorigin. Depends on width of the units label
    if ((Rounder.rounds(ebp.maxE, ebp.nRound, ebp.roundScheme)).length() 
	> (Rounder.rounds(ebp.minE, ebp.nRound, ebp.roundScheme)).length()) {
      xorigin = 1.4*getStringWidth
	((Rounder.rounds(ebp.maxE, ebp.nRound, ebp.roundScheme)),
	 "Times-Roman", (int)ebp.fontsize2)
	+ getMaxFontHeight("Times-Roman",(int)ebp.fontsize3);
    }
    else {
      xorigin = 1.4*getStringWidth
	((Rounder.rounds(ebp.minE, ebp.nRound, ebp.roundScheme)),
	 "Times-Roman" , (int)ebp.fontsize2)
	+ getMaxFontHeight("Times-Roman",(int)ebp.fontsize3);
    }

    yorigin=getMaxFontHeight("Times-Roman",(int)ebp.fontsize1);

    plotLength = getPlotLength();
    scaley =  ratio / (ebp.maxE-ebp.minE) * scalex;
    plotHeight = getPlotHeight();

  }

  public void render() {
    
    double xold;
    int sepFlag=0;
    BandPlot.BandPlotSection plotSection;
    boolean ticsLabel=true;
    x=0;
    
            
    // Be careful if modifying variable x (side effects bc relative behavior)
    // Variable y can be modified (absolute behavior)
    for (int i=0; i< ebp.bandPlotSections.size() ;i++) { 
      ticsLabel = true;
      // Draw a thick vertical line if
      //  *it is the first section
      //  *a big separation has been done
      if (i==0 || sepFlag ==1) {
	sepFlag = 0;
	drawVerticalSeparation(2);
	drawVerticalTics(ebp.nvtics,false,1);
	if (i==0) {
	  drawVerticalTicsLabels(ebp.nvtics, (int)ebp.fontsize2);
	}
      }
      
      plotSection = (BandPlot.BandPlotSection)ebp.bandPlotSections.elementAt(i);
      EnergyBand.KLine kLine = ebp.energyBand.getKLine(plotSection.lineIndex);
      xold = x;
      for (int eindex=0; eindex < kLine.getNumberOfBands(); eindex++) {
	x = xold;   
	drawSectionLine(plotSection, kLine, eindex, ticsLabel); //x is modified
	ticsLabel = false;
      } //end for eindex

      y = ebp.minE;
      drawHorizontalSeparation(2, xold, x);
      drawHorizontalTics(ebp.nhtics, xold, x, 1);
      y = ebp.maxE;
      drawHorizontalSeparation(2, xold, x);
      y = ebp.fermiE;
      drawHorizontalSeparation(1, xold, x);      

      switch (plotSection.endDelimiter) {
      case BandPlot.NONE:
	break;
      case BandPlot.VLINE: 
	drawVerticalSeparation(1);
	break;
      case BandPlot.BIGSEP:
	sepFlag=1;
	drawVerticalSeparation(2);
	drawVerticalTics(ebp.nvtics,true,1);
	x = x + ebp.bigSepSize;
	break;
      }
    } //end for bandSection
    
    drawVerticalSeparation(2);
    drawVerticalTics(ebp.nvtics,true,1);
    drawVerticalAxisLabel(ebp.yLabel,(int)ebp.fontsize3);

  } //end render
  
  /**
   * draw the energy curve of the line kLine with index eindex
   * according to plotSection specifications.
   */
  public void drawSectionLine(BandPlot.BandPlotSection plotSection,
			      EnergyBand.KLine kLine, int eindex,
			      boolean ticsLabel) {
    double energie;
    int kindexOld = plotSection.origIndex;
    double xtmp=0;
    double ytmp=0;
    
    if (plotSection.origIndex < plotSection.endIndex) {
      for (int kindex = plotSection.origIndex; 
	   kindex <= plotSection.endIndex; kindex++) {
	
	energie = kLine.getEnergies(kindex)[eindex] * ebp.conversionFactor;
	x = x+ kLine.getDistance(kindex, kindexOld);
	y = energie;
	if (kindex != kindexOld) {
	  drawLineR(xtmp, ytmp, x, y, 0);
	}
	
	if (ticsLabel){ // draw only once
	  if (kindex==plotSection.origIndex) {
	    drawHorizontalTicsLabel(x, plotSection.origName,
				    (int)ebp.fontsize1);
	  } else if (kindex==plotSection.endIndex) {
	    drawHorizontalTicsLabel(x, plotSection.endName,
				    (int)ebp.fontsize1);
	  }
	}
	
	kindexOld = kindex;
	xtmp = x;
	ytmp = y;
      } //end kindex
    } else {
      for (int kindex = plotSection.origIndex; 
	   kindex >= plotSection.endIndex; kindex--) {
	
	energie = kLine.getEnergies(kindex)[eindex] * ebp.conversionFactor;
	x = x+ kLine.getDistance(kindex, kindexOld);
	y = energie;
	if (kindex != kindexOld) {
	  drawLineR(xtmp, ytmp, x, y, 0);
	}
	
	if (ticsLabel){ // draw only once
	  if (kindex==plotSection.origIndex) {
	    drawHorizontalTicsLabel(x, plotSection.origName,
				    (int)ebp.fontsize1);
	  } else if (kindex==plotSection.endIndex) {
	    drawHorizontalTicsLabel(x, plotSection.endName,
				    (int)ebp.fontsize1);
	  }
	}
	
	kindexOld = kindex;
	xtmp = x;
	ytmp = y;
      } //end kindex
    }
  }


  public double getPlotLength() {
    BandPlot.BandPlotSection plotSection;
    double plotLength=0;
    
    for (int i=0; i< ebp.bandPlotSections.size() ;i++) {
      plotSection = (BandPlot.BandPlotSection)ebp.bandPlotSections.elementAt(i);
      EnergyBand.KLine kLine = ebp.energyBand.getKLine(plotSection.lineIndex); 
      plotLength = plotLength + kLine.getDistance(plotSection.origIndex,
						  plotSection.endIndex);
    }
    
    plotLength = plotLength*scalex + xorigin;
    plotLength = plotLength + (ebp.bigSepNumber*ebp.bigSepSize)*scalex;
    plotLength = plotLength + (double)getStringWidth("M","Times-Roman"
						     , (int)ebp.fontsize1) /2 ;

    return plotLength;
  }
  
  public double getPlotHeight() {
    return (yorigin + (ebp.maxE - ebp.minE)*scaley 
	    + (double)getMaxFontHeight("Times-Roaman",(int)ebp.fontsize2) /2);
  }

  abstract double getX();
  abstract double getX(double x);
  abstract double getY();
  abstract double getY(double y);
  
  abstract void drawLineR(double xorig, double yorig, double xend, double yend,
			 int thickness);
  abstract void drawLineA(double xorig, double yorig, double xend, double yend,
			 int thickness);
  
  // Draw a vertical separation at the current x
  abstract void drawVerticalSeparation(int linewidth);
  
  //Draw a horizontal separation at the current y
  abstract void drawHorizontalSeparation(int linewidth, double orig, double end);
  
  //Draw vertical tics at current x to the left(right) direction
  //if direction is true(false)
  abstract void drawVerticalTics(int ntics, boolean direction, int linewidth);
    
  abstract void drawVerticalTicsLabels(int ntics, int fontsize);  
  abstract void drawHorizontalTics(int ntics, double orig, double end,
				   int linewidth);  
  abstract void drawHorizontalTicsLabel(double pos, String label,
					int fontsize);  
  abstract void drawVerticalAxisLabel(String label, int fontsize);

  abstract void setFont(String name, int size);
  abstract void setLineWidth(int width);

  //Alignment markers
  public final static int ALIGN_LEFT=0;
  public final static int ALIGN_RIGHT=1;
  public final static int ALIGN_CENTER=2;
  abstract void drawText(String text,int fontsize, double x, double y,
			 int align, double rotation);
  
  public double getStringWidth(String string, String name, int size){
    Font f = new Font(name, Font.PLAIN, size);
    FontMetrics fm = dummyFrame.getFontMetrics(f);
    return (double)fm.stringWidth(string);
  }

  public double getMaxFontHeight(String name, int size){
    Font f = new Font(name, Font.PLAIN, size);
    FontMetrics fm = dummyFrame.getFontMetrics(f);
    return ((double)fm.getHeight())*1.1;
  }


  
  
} //end class BandPlotRenderer
