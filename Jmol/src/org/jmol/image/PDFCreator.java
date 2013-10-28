/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-30 18:58:33 -0500 (Tue, 30 Jun 2009) $
 * $Revision: 11158 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.image;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.awt.Font;
import javajs.util.List;
import javajs.util.SB;

//import javajs.api.GenericColor;
//import javajs.util.ColorUtil;

/**
 * A rudimentary 1-page single-graphic PDF creator.
 * Source: PDF Reference Manual 13.
 * 
 * @author hansonr Bob Hanson, hansonr@stolaf.edu
 */
public class PDFCreator {
  
  public OutputStream os;
  public List<PDFObject> indirectObjects;
  public PDFObject root;
  public PDFObject graphics; 
  
//  public Font currentFont;

  private int pt;
  private int xrefPt;
  private int count;

  private int height;
  private int width;
  
  private Map<Object, PDFObject> images;  
  private Map<String, PDFObject>fonts;

  /**
   * Basic process is very simple:
   * 
   * openDocument(...)
   * [issue drawing commands]
   * closeDocument()
   * 
   * That's it!
   * 
   */

  public PDFCreator() {
  }

  public void openDocument(OutputStream os, int paperWidth,
                                int paperHeight, boolean isLandscape)
      throws Exception {
    this.os = os;
    width = (isLandscape ? paperHeight : paperWidth);
    height = (isLandscape ? paperWidth : paperHeight);
    System.out.println("Creating PDF with width=" + width + " and height=" + height);
    fonts = new Hashtable<String, PDFObject>();
    indirectObjects = new List<PDFObject>();
    //graphicsResources = newObject(null);
    //pageResources = newObject(null); // will set this to compressed stream later
    root = newObject("Catalog");
    PDFObject pages = newObject("Pages");
    PDFObject page = newObject("Page");
    PDFObject pageContents = newObject(null);
    graphics = newObject("XObject");
    
    root.addDef("Pages", pages.getRef());
    pages.addDef("Count", "1");
    pages.addDef("Kids", "[ " + page.getRef() +" ]");
    page.addDef("Parent", pages.getRef());
    page.addDef("MediaBox", "[ 0 0 " + paperWidth + " " + paperHeight + " ]");
    if (isLandscape)
      page.addDef("Rotate", "90");

    pageContents.addDef("Length", "?");
    pageContents.append((isLandscape ? "q 0 1 1 0 0 0 " : "q 1 0 0 -1 0 "+(paperHeight))+" cm /" + graphics.getID() + " Do Q");
    page.addDef("Contents", pageContents.getRef());   
    addProcSet(page);
    addProcSet(graphics);
    // will add fonts as well as they are needed
    graphics.addDef("Subtype", "/Form");
    graphics.addDef("FormType", "1");
    graphics.addDef("BBox", "[0 0 " + width + " " + height + "]");
    graphics.addDef("Matrix", "[1 0 0 1 0 0]");
    graphics.addDef("Length", "?");
    page.addResource("XObject", graphics.getID(), graphics.getRef());   
    g("q 1 w 1 J 1 j 10 M []0 d q "); // line width 1, line cap circle, line join circle, miter limit 10, solid
    clip(0, 0, width, height);
  }   

  public void closeDocument() throws IOException {
    g("Q Q");
    outputHeader();
    writeObjects();
    writeXRefTable();
    writeTrailer();
    os.flush();
    os.close();
  }

  private void addProcSet(PDFObject o) {
    o.addResource(null, "ProcSet", "[/PDF /Text /ImageB /ImageC /ImageI]");
  }

  private PDFObject newObject(String type) {
    PDFObject o = new PDFObject(++count);
    if (type != null)
      o.addDef("Type", "/" + type);
    indirectObjects.addLast(o);
    return o;
  }

  private void output(String s) throws IOException {
   byte[] b = s.getBytes();
   os.write(b, 0, b.length);
   pt += b.length;
  }

  private void outputHeader() throws IOException {
    output("%PDF-1.3\n%");
    byte[] b = new byte[] {-1, -1, -1, -1};
    os.write(b, 0, b.length);
    pt += 4;
    output("\n");
  }

  private void writeTrailer() throws IOException {
    PDFObject trailer = new PDFObject(-2);
    output("trailer");
    trailer.addDef("Size", "" + indirectObjects.size());
    trailer.addDef("Root", root.getRef());
    trailer.output(os);
    output("startxref\n");
    output("" + xrefPt + "\n");
    output("%%EOF\n");
  }

  private void writeObjects() throws IOException {
    int nObj = indirectObjects.size();
    for (int i = 0; i < nObj; i++) {
      PDFObject o = indirectObjects.get(i);
      if (!o.isFont())
        continue;
      o.pt = pt;
      pt += o.output(os);
    }
    for (int i = 0; i < nObj; i++) {
      PDFObject o = indirectObjects.get(i);
      if (o.isFont())
        continue;
      o.pt = pt;
      pt += o.output(os);
    }
  }

  private void writeXRefTable() throws IOException {
    xrefPt = pt;
    int nObj = indirectObjects.size();
    SB sb = new SB();
    // note trailing space, needed because \n is just one character
    sb.append("xref\n0 " + (nObj + 1) 
        + "\n0000000000 65535 f\r\n");
    for (int i = 0; i < nObj; i++) {
      PDFObject o = indirectObjects.get(i);
      String s = "0000000000" + o.pt;
      sb.append(s.substring(s.length() - 10));
      sb.append(" 00000 n\r\n");
    }
    output(sb.toString());
  }

  /////////////// adding resources ////////////////
  
  public void addInfo(Map<String, String> data) {
    Hashtable<String, Object> info = new Hashtable<String, Object>();
    for (Entry<String, String> e: data.entrySet()) {
      String value = "(" + e.getValue().replace(')','_').replace('(','_')+ ")";
      info.put(e.getKey(), value);      
    }
    root.addDef("Info", info);
  }

  public PDFObject addFontResource(String fname, Font font) {
    PDFObject f = newObject("Font");
    fonts.put(fname, f);
    f.addDef("BaseFont", fname);
    f.addDef("Encoding", "/WinAnsiEncoding");
    f.addDef("Subtype", "/Type1");
    f.addDef("!privateFont", font);
    graphics.addResource("Font", f.getID(), f.getRef());
    return f;
  }

  public void addImageResource(Object image, int width, int height, int[] buffer, boolean isColored) {
    if (images == null)
      images = new Hashtable<Object, PDFObject>();
    PDFObject pdfImage = newObject("XObject");
    pdfImage.addDef("Subtype", "/Image");
    pdfImage.addDef("Length", "?");
    pdfImage.addDef("ColorSpace", isColored ? "/DeviceRGB" : "/DeviceGray");
    pdfImage.addDef("BitsPerComponent", "8");
    pdfImage.addDef("Width", "" + width);
    pdfImage.addDef("Height", "" + height);
    pdfImage.addDef("!privateImage", image);
    graphics.addResource("XObject", pdfImage.getID(), pdfImage.getRef());
    int n = buffer.length;
    byte[] stream = new byte[n * (isColored ? 3 : 1)];
    if (isColored) {
      for (int i = 0, pt = 0; i < n; i++) {
        stream[pt++] = (byte) ((buffer[i] >> 16) & 0xFF);
        stream[pt++] = (byte) ((buffer[i] >> 8) & 0xFF);
        stream[pt++] = (byte) (buffer[i] & 0xFF);
      }
    } else {
      for (int i = 0; i < n; i++)
        stream[i] = (byte) buffer[i];
    }
    pdfImage.setStream(stream);
    graphics.addResource("XObject", pdfImage.getID(), pdfImage.getRef());
    images.put(image, pdfImage);   
  }


  /////////////// graphics primitives ////////////////
  
  public void g(String cmd) {
    graphics.append(cmd).appendC('\n');
  }
  
  public void clip(int x1, int y1, int x2, int y2) {
    moveto(x1, y1);
    lineto(x2, y1);
    lineto(x2, y2);
    lineto(x1, y2);
    g("h W n");
  }

  public void moveto(int x, int y) {
    g(x + " " + y  + " m");
  }

  public void lineto(int x, int y) {
    g(x + " " + y  + " l");
  }

  public void drawImage(Object image, int destX0, int destY0,
      int destX1, int destY1, int srcX0, int srcY0, int srcX1, int srcY1) {
    PDFObject imageObj = images.get(image);
    if (imageObj == null)
      return;
    g("q");
    clip(destX0, destY0, destX1, destY1);
    float iw = Float.parseFloat((String) imageObj.getDef("Width"));
    float ih = Float.parseFloat((String) imageObj.getDef("Height"));    
    float dw = (destX1 - destX0 + 1);
    float dh  = (destY1 - destY0 + 1);
    float sw = (srcX1 - srcX0 + 1);
    float sh = (srcY1 - srcY0 + 1);
    float scaleX = dw / sw;
    float scaleY = dh / sh;
    float transX = destX0 - srcX0 * scaleX;
    float transY = destY0 + (ih - srcY0) * scaleY;
    g(scaleX*iw + " 0 0 " + -scaleY*ih + " " + transX + " " + transY + " cm");
    g("/" + imageObj.getID() + " Do");
    g("Q");
  }


/*
 *  Rest of these just commented out because Jmol doesn't use them.
 *  I created them for JSpecView.
 *  
 */
  
//  public boolean inPath;
//  
//  public void doStroke(boolean isBegin) {
//     inPath = isBegin;
//     if (!isBegin)
//       g("S");    
//  }
//
//  public void drawCircle(int x, int y, int diameter) {
//    bezierCircle(x, y, diameter/2.0, false);    
//  }
//
//  public void bezierCircle(int x, int y, double r, boolean doFill) {
//    double d = r*4*(Math.sqrt(2)-1)/3;
//    double dx = x;
//    double dy = y;
//    g((dx + r) + " " + dy + " m");
//    g((dx + r) + " " + (dy + d) + " " + (dx + d) + " " + (dy + r) + " " + (dx) + " " + (dy + r) + " "  + " c");
//    g((dx - d) + " " + (dy + r) + " " + (dx - r) + " " + (dy + d) + " " + (dx - r) + " " + (dy) + " c");
//    g((dx - r) + " " + (dy - d) + " " + (dx - d) + " " + (dy - r) + " " + (dx) + " " + (dy - r) + " c");
//    g((dx + d) + " " + (dy - r) + " " + (dx + r) + " " + (dy - d) + " " + (dx + r) + " " + (dy) + " c");
//    g(doFill ? "f" : "h S");
//  }
//
//  public void drawLine(int x0, int y0, int x1, int y1) {
//    moveto(x0, y0);
//    lineto(x1, y1);
//    if (!inPath)
//      g("S");   
//  }
//
//  public void drawPolygon(int[] axPoints, int[] ayPoints, int nPoints) {
//    moveto(axPoints[0], ayPoints[0]);
//    for (int i = 1; i < nPoints; i++)
//      lineto(axPoints[i], ayPoints[i]);
//    g("s");
//  }
//
//  public void drawRect(int x, int y, int width, int height) {
//    g(x + " " + y + " " + width + " " + height + " re s");
//  }
//
//  public void drawString(String s, int x, int y) {
//    drawStringRotated(s, x, y, 0);
//  }
//
//  public void drawStringRotated(String s, int x, int y, double angle) {
//    angle = angle / 180.0 * Math.PI;
//    double cos = Math.cos(angle);
//    double sin = Math.sin(angle);
//    if (Math.abs(cos) < 0.0001)
//      cos = 0;
//    if (Math.abs(sin) < 0.0001)
//      sin = 0;
//    g("q " + cos + " " + sin + " " + sin + " " + -cos + " " + x + " " + y + " cm BT(" + s + ")Tj ET Q");
//  }
//  
//  public void fillCircle(int x, int y, int diameter) {
//    bezierCircle(x, y, diameter/2.0, true);       
//  }
//
//  public void fillPolygon(int[] ayPoints, int[] axPoints, int nPoints) {
//    moveto(axPoints[0], ayPoints[0]);
//    for (int i = 1; i < nPoints; i++)
//      lineto(axPoints[i], ayPoints[i]);
//    g("f");
//  }
//
//  public void fillRect(int x, int y, int width, int height) {
//    g(x + " " + y + " " + width + " " + height + " re f");
//  }
//
//
//  private static float[] rgb = new float[3];
//
//  public void setFillColor(GenericColor c) {
//    ColorUtil.toRGBf(c.getRGB(), rgb);
//    g(rgb[0] + " " + rgb[1] + " " + rgb[2] + " rg");
//  }
//
//  public void setStrokeColor(GenericColor c) {
//    ColorUtil.toRGBf(c.getRGB(), rgb);
//    g(rgb[0] + " " + rgb[1] + " " + rgb[2] + " RG");
//  }
//
//  public void setGraphicsFont(Font font) {
//    currentFont = font;
//    String fname = "/Helvetica";// + font.fontFace;
//    switch (font.idFontStyle) {
//    case Font.FONT_STYLE_BOLD:
//      fname += "-Bold";
//      break;
//    case Font.FONT_STYLE_BOLDITALIC:
//      fname += "-BoldOblique";
//      break;
//    case Font.FONT_STYLE_ITALIC:
//      fname += "-Oblique";
//      break;
//    }
//    PDFObject f = fonts.get(fname);
//    if (f == null)
//      f = addFontResource(fname, font);
//    g("/" + f.getID() + " " + font.fontSizeNominal + " Tf");
//    
//  }
//
//  public void setStrokeBold(boolean tf) {
//    g((tf ? 2 : 1) + " w");   
//  }
//
//  public void translateScale(double x, double y, double scale) {
//    g(scale + " 0 0 " + scale + " " + x + " " + y + " cm");
//  }
//  
//  public int getFontHeight() {
//    return currentFont.getAscent();
//  }
//
//  public int getStringWidth(String s) {
//    return currentFont.stringWidth(s);
//  }

}
