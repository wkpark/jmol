package org.openscience.jmol;

import java.io.IOException;
import org.openscience.jmol.util.*;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;


public class BandPlotEPSRenderer extends BandPlotRenderer {
  
  FileOutputStream file;
  BufferedWriter w;

  public BandPlotEPSRenderer(BandPlot bandPlot,
				   double scalex, double ratio,
				   File file)
    throws FileNotFoundException { 

    super(bandPlot, scalex, ratio);

    //ebp = energyBandPlot;
    this.file = new FileOutputStream(file);
    w = new BufferedWriter(new OutputStreamWriter(this.file), 1024);
  }
  
  public void generateEPS() throws IOException {

    //Generate Encapsulated PostScript Header
    writeLine("%!PS-Adobe-2.0 EPSF-2.0");
    writeLine("%%Title: Energy Band plot");
    writeLine("%%Creator: Jmol");
    writeLine("%%CreationDate: Mon Nov 11 11:26:46 2002"); //FIX
    writeLine("%%For: fabian@sandy (Fabian Dortu,,,)"); //FIX
    writeLine("%%BoundingBox: 0 0 "    //Needed for EPS!!!!
	      + plotLength 
	      + " " 
	      + plotHeight);
    
    //Generate core
    render();

    // Finalize
    w.flush();
    w.close();
    file.flush();
    file.close();
  } //end render
  
  
  private void writeLine(String line) {
    try {
      w.write(line, 0, line.length());
      w.newLine();  
    } catch (IOException ioe) {
    }
  }


  double getX() {
    return xorigin + x*scalex;
  }

  double getX(double x) {
    return xorigin + x*scalex;
  }
  
  double getY() {
    return yorigin + (y-ebp.minE)*scaley;
  }

  double getY(double y) {
    return yorigin + (y-ebp.minE)*scaley;
  }
   
  void setFont(String name, int size) {

  }
  
  void setLineWidth(int width) {

  }

  void drawLineR(double xorig, double yorig, double xend, double yend,
		int thickness) {
    String psLine=" gsave newpath " + thickness + " setlinewidth ";
    psLine = psLine + getX(xorig) + " " + getY(yorig) + " moveto ";
    psLine = psLine + getX(xend)  + " " + getY(yend)  + " lineto stroke";
    psLine = psLine + " grestore ";
    writeLine(psLine);
  }

  void drawLineA(double xorig, double yorig, double xend, double yend,
		int thickness) {
    String psLine=" gsave newpath " + thickness + " setlinewidth ";
    psLine = psLine + xorig + " " + yorig + " moveto ";
    psLine = psLine + xend  + " " + yend  + " lineto stroke";
    psLine = psLine + " grestore ";
    writeLine(psLine);
  }

  // Draw a vertical separation at the current x
  void drawVerticalSeparation(int linewidth) {
    drawLineR(x, ebp.minE,
	     x, ebp.maxE,
	     linewidth);
  }
  
  //Draw a horizontal separation at the current y
  void drawHorizontalSeparation(int linewidth, double orig, double end) {
    drawLineR(orig, y,
	     end, y, 
	     linewidth);
}
  
  //Draw vertical tics at current x to the left(right) direction
  //if direction is true(false)
  void drawVerticalTics(int ntics, boolean direction, int linewidth) {
    
    double step = (ebp.maxE - ebp.minE)/ntics;
    for (int i=0; i<= ntics ;i++) {
      if (direction) {
	drawLineA(getX(x), getY(ebp.minE + i*step),
		 getX(x) - ebp.ticSize,
		 getY(ebp.minE + i*step),
		 linewidth);
      } else {
	drawLineA(getX(x), getY(ebp.minE + i*step),
		 getX(x) + ebp.ticSize, getY(ebp.minE + i*step),
		 linewidth);
      }
    }
  }
  
  void drawVerticalTicsLabels(int ntics, int fontsize) {
    String value;
    
    double step = (ebp.maxE - ebp.minE)/ntics;
    for (int i=0; i<= ntics ;i++) {
      value = Rounder.rounds((ebp.minE + i*step ), 
			     ebp.nRound, ebp.roundScheme);
      
      drawText(value, fontsize,
	       getX() ,
	       getY(ebp.minE + i*step) - fontsize /3, 
	       2,  //Align right
	       0.0);   //No rotation
    } 
  }
  
  void drawHorizontalTics(int ntics, double orig, double end, int linewidth) {
    double step = (end-orig)/ntics;
    
    for (int i=0; i < ntics; i++){
      drawLineA(getX(orig + i*step), getY(y),
	       getX(orig + i*step), getY(y) + ebp.ticSize,
	       linewidth);
    }
  }
  
  void drawHorizontalTicsLabel(double pos, String line, int fontsize) {
    drawText(line, fontsize,
	     getX(pos),
	     fontsize * .15f,
	     1,
	     0.0f);
	     
  }
  
  void drawVerticalAxisLabel(String label, int fontsize) {
    drawText(label, fontsize, 
	     getMaxFontHeight("Times-Roman", fontsize), 
	     yorigin + (ebp.maxE-ebp.minE)/2 *scaley,
	     1,
	     90.0f);

  }
  
  




  void drawText(String text,int fontsize, double x, double y,
		int align, double rotation) {


    final int ALIGN_LEFT = 0;
    final int ALIGN_CENTER = 1;
    final int ALIGN_RIGHT = 2;
    
    FormatedText fText = new FormatedText(text, fontsize);
    double width = (double)fText.getWidth();
    Vector textDef = fText.getTextDef();
    
    Object token;
    String tokenPS;
    Font f = new Font("Times-Roman", Font.PLAIN, fontsize);
    String fontPS = "Times-Roman";
    int sizePS = fontsize;
    
    double dx=0;    
    double dy=0;
    switch(align) {
    case ALIGN_LEFT: dx=0; break;
    case ALIGN_CENTER: dx=-width/2.0; break;
    case ALIGN_RIGHT: dx=-width; break;
    }
    
    String psLine=" gsave ";
    psLine = psLine + x + " " + y + " translate " 
      + rotation + " rotate 0 0 moveto ";     
    

    for (Enumeration e = textDef.elements() ; e.hasMoreElements() ;) {
      token = e.nextElement();
      if(token instanceof Integer) {
	switch (((Integer)token).intValue()) {
	case FormatedText.FONT_NORMAL:
	  f = new Font("Times-Roman", Font.PLAIN, fontsize);
	  fontPS = "Times-Roman";
	  sizePS = fontsize;
	  break;
	case FormatedText.FONT_SYMBOL:
	  f = new Font("Greek Poly Plain", Font.PLAIN, fontsize);
	  fontPS = "Symbol";
	  sizePS = fontsize; 
	  break;
	case FormatedText.POS_NORMAL:
	  f = new Font(f.getName(), Font.PLAIN, fontsize);
	  sizePS = fontsize;
	  dy=-dy;
	  break;
	case FormatedText.POS_EXP:
	  dy = (double)fontsize *2/3;
	  f = new Font(f.getName(), Font.PLAIN, 
		       (int)((double)fontsize *1/2));
	  sizePS = (int)((double)fontsize *1/2);
	  break;
	}
      } else if (token instanceof String) {
	FontMetrics fm = dummyFrame.getFontMetrics(f);

	// "(" and ")" must be "escaped" with "\(" and "\)" 
	// because have PostScrip signification. 
	// The regexp seems complicated
	// because:
	// *the escape character must escaped in  both
	// java and in the regexp itself.
	// *the paranthesis must be escaped in regexp.
	tokenPS=(String)token;

        /*
          replaceAll is only available in JVM 1.4 and greater
          see substitute routine below
          I have not tested this, so please confirm that it works properly
	tokenPS=tokenPS.replaceAll("\\(","\\\\(");
	tokenPS=tokenPS.replaceAll("\\)","\\\\)");
        */
        tokenPS=replaceCharString(tokenPS, '(', "\\(");
        tokenPS=replaceCharString(tokenPS, ')', "\\)");
	  
	psLine=psLine +
	  "/" + fontPS + " findfont " +
	  sizePS + " scalefont setfont " +
	  dx + " " + dy + " rmoveto " +
	  "(" + tokenPS + ")" + " show ";

	dx=0; // PostScript automaticaly increments its position
      }
    } //for on textDef elements
    
    psLine = psLine + " grestore ";
    writeLine(psLine);
  }

  String replaceCharString(String strOld, char charOld, String substrNew) {
    // FIXME - mth 2003 01 07 - confirm that this works
    // I wrote this as a replacement for the replaceAll code because
    // replaceAll is only available on JVM >= 1.4
    // I have not tested this code to convirm that it works
    String strNew = "";
    int ichStart = 0;
    int ichMatch;
    while ((ichMatch = strOld.indexOf(charOld, ichStart)) != -1) {
      strNew += strOld.substring(ichStart, ichMatch) + substrNew;
      ++ichStart;
    }
    strNew += strOld.substring(ichStart);
    return strNew;
  }
}
