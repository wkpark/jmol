package org.openscience.jmol.app;

import org.openscience.jmol.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.lang.Integer;
import java.util.*;
import java.lang.reflect.Array;

public class BandPlotG2DRenderer extends BandPlotRenderer  {

  Graphics2D g2;
  JPanel bandPlotPanel;

  public BandPlotG2DRenderer(BandPlot bandPlot,
				      double scalex, double ratio) {
    super(bandPlot, scalex, ratio);
    bandPlotPanel = new BandPlotPanel(this);
  }

  public JPanel getJPanel() {
    return bandPlotPanel;
  }
  
  void setGraphics2D(Graphics2D g2) {
    this.g2 = g2;
  }

  
  double getX() {
    return xorigin + x*scalex;
  }
  
  double getX(double x) {
    return xorigin + x*scalex;
  }
  
  double getY() {
    return (plotHeight - (yorigin + (y-ebp.minE)*scaley));
  }

  double getY(double y) {
    return (plotHeight - (yorigin + (y-ebp.minE)*scaley));
  }
  
  void setFont(String name, int size) {
    g2.setFont(new Font(name, Font.PLAIN, size));
  }
  
  void setLineWidth(int width) {

  }


  //Draw line in relative coordinate.  
  void drawLineR(double xorig, double yorig, double xend, double yend,
		int thickness) {
    g2.drawLine((int)getX(xorig), (int)getY(yorig),
	       (int)getX(xend), (int)getY(yend));
  }

  //Draw line in absolute coordinate.
  void drawLineA(double xorig, double yorig, double xend, double yend,
		int thickness) {
    g2.drawLine((int)xorig, (int)yorig,
		(int)xend, (int)yend);
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
	     end,  y,
	     linewidth);
  }
  
  //Draw vertical tics at current x to the left(right) direction
  //if direction is true(false)
  void drawVerticalTics(int ntics, boolean direction, int linewidth) {
    
    double step = (ebp.maxE - ebp.minE)/ntics;
    for (int i=0; i<= ntics ;i++) {
      if (direction) {
	drawLineA(getX(x), 
		 getY(ebp.minE + i*step),
		 getX(x) - ebp.ticSize,
		 getY(ebp.minE + i*step),
		 linewidth);
      } else {
	drawLineA(getX(x),
		 getY(ebp.minE + i*step),
		 getX(x) + ebp.ticSize,
		 getY(ebp.minE + i*step),
		 linewidth);
      }
    }
  }
  
  void drawVerticalTicsLabels(int ntics, int fontsize) {
    String value;
    double width;    

    g2.setFont(new Font("Times-Roman", Font.PLAIN, fontsize));

    double step = (ebp.maxE - ebp.minE)/ntics;
    for (int i=0; i<= ntics ;i++) {
      value = Rounder.rounds((ebp.minE + i*step ), 
			     ebp.nRound, ebp.roundScheme);
      width
	= getStringWidth(value,
			 g2.getFont().getFontName(),
			 g2.getFont().getSize());    
      g2.drawString(value, 
		    (int)(getX() - width),
		    (int)(getY(ebp.minE + i*step) + ebp.fontsize2 /2));
    }
  }
  
  void drawHorizontalTics(int ntics, double orig, double end, int linewidth) {
    double step = (end-orig)/ntics;
    
    for (int i=0; i < ntics; i++){
      drawLineA(getX(orig + i*step), getY(y),
	       getX(orig + i*step), getY(y) - ebp.ticSize,
	       linewidth);
    }
  }
  
  void drawHorizontalTicsLabel(double pos, String label, int fontsize) {
    drawText(label, fontsize,
	     (int)getX(pos),
	     (int)(plotHeight),
	     1,  //Align center);
	     0); 
  }
  
  void drawVerticalAxisLabel(String label, int fontsize) {
    //The label is centered around the middle of the energy axis
    drawText(label, fontsize, 
	     getMaxFontHeight("Times-Roman", fontsize), 
	     (int)(plotHeight
		   - (yorigin + (ebp.maxE-ebp.minE)/2 *scaley))
	     ,1    //Align center
	     ,-90.0);

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
    Font f = new Font("Times-Roman", Font.PLAIN, fontsize);

    double dx=0;    
    double dy=0;
    switch(align) {
    case ALIGN_LEFT: dx=0; break;
    case ALIGN_CENTER: dx=-width/2.0; break;
    case ALIGN_RIGHT: dx=-width; break;
    }

    AffineTransform saveXform = g2.getTransform();   
    AffineTransform at = new AffineTransform();
    at.rotate(Math.toRadians(rotation), x, y);
    g2.transform(at);

    
    
    for (Enumeration e = textDef.elements() ; e.hasMoreElements() ;) {
      token = e.nextElement();
      if(token instanceof Integer) {
	switch (((Integer)token).intValue()) {
	case FormatedText.FONT_NORMAL:
	  f = new Font("Times-Roman", Font.PLAIN, fontsize);
	  g2.setFont(f);	
	  break;
	case FormatedText.FONT_SYMBOL:
	  f = new Font("Greek Poly Plain", Font.PLAIN, fontsize);
	  g2.setFont(f);
	  break;
	case FormatedText.POS_NORMAL:
	  f = new Font(f.getName(), Font.PLAIN, fontsize);
	  dy=0;
	  g2.setFont(f);
	  break;
	case FormatedText.POS_EXP:
	  dy=-(double)fontsize *2/3;
	  f = new Font(f.getName(), Font.PLAIN, 
		       (int)((double)fontsize *1/2));
	  g2.setFont(f);
	  break;
	}
      } else if (token instanceof String) {
	FontMetrics fm = g2.getFontMetrics(f);
	g2.drawString((String)token,(int)(x + dx), (int)(y + dy));
	dx = dx + fm.stringWidth((String)token);
      }
    } //for on textDef elements
    
    g2.setTransform(saveXform);

    // DUBUG: Print font list
    //GraphicsEnvironment ge 
    //  = GraphicsEnvironment.getLocalGraphicsEnvironment();
    //Font[] allFonts = ge.getAllFonts();
    //for (int i=0; i < Array.getLength(allFonts); i++) {
    //System.out.println("Font "+ i +" : "+ allFonts[i].getFontName());
    //}
  }
  
  
}
