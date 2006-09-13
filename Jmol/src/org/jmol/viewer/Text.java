/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
package org.jmol.viewer;

import java.awt.FontMetrics;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;

class Text {

  final static int XY = 0;
  final static int LEFT = 1;
  final static int CENTER = 2;
  final static int RIGHT = 3;

  final static int TOP = 1;
  final static int BOTTOM = 2;
  final static int MIDDLE = 3;

  boolean atomBased;
  Graphics3D g3d;
  String target;
  String text;
  String[] lines;
  int align;
  int valign;
  int movableX;
  int movableY;
  int offsetX;
  int offsetY;
  int z;
  int zSlab; // z for slabbing purposes -- may be near an atom

  int windowWidth;
  int windowHeight;
  boolean adjustForWindow;
  int boxX, boxY, boxWidth, boxHeight;
  
  Font3D font3d;
  FontMetrics fm;
  byte fid;
  int ascent;
  int descent;
  int lineHeight;

  short colix;
  short bgcolix;

  int[] widths;
  int textWidth;
  int textHeight;

  // for labels and hover
  Text(Graphics3D g3d, Font3D font3d, String text, short colix,
      short bgcolix, int offsetX, int offsetY, int z, int zSlab, int textAlign) {
    windowWidth = g3d.getRenderWidth();
    windowHeight = g3d.getRenderHeight();
    atomBased = true;
    this.g3d = g3d;
    this.text = text;
    this.colix = colix;
    this.bgcolix = bgcolix;
    setXYZs(offsetX, offsetY, z, zSlab);
    align = textAlign;
    setFont(font3d);
  }

  // for echo
  Text(Graphics3D g3d, Font3D font3d, String target, short colix, int valign, int align) {
    windowWidth = g3d.getRenderWidth();
    windowHeight = g3d.getRenderHeight();
    atomBased = false;
    this.g3d = g3d;
    this.target = target;
    this.align = align;
    this.valign = valign;
    this.font3d = font3d;
    this.colix = colix;
    this.z = 2;
    this.zSlab = Integer.MIN_VALUE;
    getFontMetrics();
  }

  void getFontMetrics() {
    fm = font3d.fontMetrics;
    descent = fm.getDescent();
    ascent = fm.getAscent();
    lineHeight = ascent + descent;
  }

  void setFid(byte fid) {
    if (this.fid == fid)
      return;
    this.fid = fid;
    recalc();
  }

  void setAdjustForWindow(boolean TF) {
    adjustForWindow = TF;
  }
  
  void setColix(short colix) {
    this.colix = colix;
  }

  void setColix(Object value) {
    colix = Graphics3D.getColix(value);
  }

  void setBgColix(short colix) {
    this.bgcolix = colix;
  }

  void setBgColix(Object value) {
    bgcolix = (value == null ? (short) 0 : Graphics3D.getColix(value));
  }

  void setMovableX(int x) {
    valign = XY;
    movableX = x;
  }

  void setMovableY(int y) {
    valign = XY;
    movableY = y;
  }

  void setXY(int x, int y) {
    setMovableX(x);
    setMovableY(y);
  }

  void setZs(int z, int zSlab) {
    this.z = z;
    this.zSlab = zSlab;
  }

  void setXYZs(int x, int y, int z, int zSlab) {
    setMovableX(x);
    setMovableY(y);
    setZs(z, zSlab);
  }

  void setPositions() {
    int xLeft, xCenter, xRight;
    if (valign == XY) {
      xLeft = xRight = xCenter = movableX + offsetX;
    } else {
      xLeft = 5;
      xCenter = windowWidth / 2;
      xRight = windowWidth - 5;
    }
    
    // set box X from alignments
    
      boxX = xLeft;
      switch (align) {
      case CENTER:
        boxX = xCenter - boxWidth / 2; 
        break;
      case RIGHT:
        boxX = xRight - boxWidth;        
      }
    
    // set box Y from alignments
    
    boxY = 0;
    switch (valign) {
    case TOP:
      break;
    case MIDDLE:
      boxY = windowHeight / 2;
      break;
    case BOTTOM:
      boxY = windowHeight;
      break;
    default:
      boxY = movableY + offsetY;
    }

    // adjust positions if necessary
    
    setBoxOffsetsInWindow();

  }
  void setOffset(int offset) {
    offsetX = getXOffset(offset);
    offsetY = getYOffset(offset);
    valign = XY;
  }

  final static int getXOffset(int offset) {
    switch (offset) {
    case 0:
      return JmolConstants.LABEL_DEFAULT_X_OFFSET;
    case Short.MAX_VALUE:
      return 0;
    default:
      return (byte) (offset >> 8);
    }
  }

  final static int getYOffset(int offset) {
    switch (offset) {
    case 0:
      return -JmolConstants.LABEL_DEFAULT_Y_OFFSET;
    case Short.MAX_VALUE:
      return 0;
    default:
      return -(int)((byte) (offset & 0xFF));
    }
  }

  void setText(String text) {
    if (this.text != null && this.text.equals(text))
      return;
    this.text = text;
    recalc();
  }

  void setFont(Font3D f3d) {
    font3d = f3d;
    getFontMetrics();
    recalc();
  }

  boolean setAlignment(String align) {
    if ("left".equals(align))
      return setAlignment(LEFT);
    if ("center".equals(align))
      return setAlignment(CENTER);
    if ("right".equals(align))
      return setAlignment(RIGHT);
    return false;
  }

  boolean setAlignment(int align) {
    this.align = align;
    recalc();
    return true;
  }

  void recalc() {
    if (text == null) {
      text = null;
      lines = null;
      widths = null;
      return;
    }
    lines = split(text, '|');
    textWidth = 0;
    widths = new int[lines.length];
    for (int i = lines.length; --i >= 0;) {
      widths[i] = fm.stringWidth(lines[i]);
      textWidth = Math.max(textWidth, widths[i]);
    }
    textHeight = lines.length * lineHeight;
    boxWidth = textWidth + 8;
    boxHeight = textHeight + 8;
  }

  void render() {
    if (text == null)
      return;

    setPositions();

    // draw the box if necessary
    
    if (bgcolix != 0)
      drawBox();
    
    // now set x and y positions for text from (new?) box position

    
    int x0 = boxX + 4;
    switch (align) {
    case CENTER:
      x0 = boxX + boxWidth / 2;
      break;
    case RIGHT:
      x0 = boxX + boxWidth - 4;
    }
    
    // now write properly aligned text
    
    int x = x0;
    int y = boxY + ascent + 4;
    for (int i = 0; i < lines.length; i++) {
      switch (align) {
      case CENTER:
        x = x0 - widths[i] / 2;
        break;
      case RIGHT:
        x = x0 - widths[i];
      }
      g3d.drawString(lines[i], font3d, colix, x, y, z, zSlab);
      y += lineHeight;
    }
  }

  private void drawBox() {
    g3d.fillRect(bgcolix, boxX, boxY, z + 2, zSlab, boxWidth, boxHeight);
    g3d.drawRect(colix, boxX + 1, boxY + 1, z + 1, zSlab, boxWidth - 2,
        boxHeight - 2);
  }

  void setBoxOffsetsInWindow() {
    if (!adjustForWindow)
      boxY -= lineHeight;
    if (atomBased && align == XY) {
      boxX += JmolConstants.LABEL_DEFAULT_X_OFFSET;
      boxY -= JmolConstants.LABEL_DEFAULT_Y_OFFSET + 4;
    }
    if (!adjustForWindow)
      return;  // labels
    
    // these coordinates are (0,0) in top left
    // (user coordinates are (0,0) in bottom left)
    boxY -= textHeight;
    int x = boxX;
    if (x + boxWidth + 5 > windowWidth)
      x = windowWidth - boxWidth - 5;
    if (x < 5)
      x = 5;
    int y = boxY;
    if (y + boxHeight > windowHeight)
      y = windowHeight - boxHeight;
    int y0 = (atomBased ? 16 + lineHeight : 0);
    if (y < y0)
      y = y0;
    // (echo is not atomBased -- positioned right on)
    boxX = x;
    boxY = y;
  }

  final static void renderSimple(Graphics3D g3d, Font3D font3d, 
                                 String strLabel, short colix, short bgcolix,
                                 int x, int y, int z, int zSlab, int xOffset,
                                 int yOffset, int ascent, int descent) {

    // old static style -- quick, simple, no line breaks, odd alignment?

    int boxWidth = font3d.fontMetrics.stringWidth(strLabel) + 8;
    int boxHeight = ascent + descent + 8;
    int xBoxOffset, yBoxOffset;

    // these are based on a standard |_ grid, so y is reversed.
    if (xOffset > 0) {
      xBoxOffset = xOffset;
    } else {
      xBoxOffset = -boxWidth;
      if (xOffset == 0)
        xBoxOffset /= 2;
      else
        xBoxOffset += xOffset;
    }

    if (yOffset > 0) {
      yBoxOffset = yOffset;
    } else {
      if (yOffset == 0)
        yBoxOffset = -boxHeight / 2 - 2;
      else
        yBoxOffset = -boxHeight + yOffset;
    }

    x += xBoxOffset;
    y += yBoxOffset;

    if (bgcolix != 0) {
      g3d.fillRect(bgcolix, x, y, z, zSlab, boxWidth, boxHeight);
      g3d.drawRect(colix, x + 1, y + 1, z - 1, zSlab, boxWidth - 2,
          boxHeight - 2);
    }
    g3d.drawString(strLabel, font3d, colix, x + 4, y + 4 + ascent, 
        z - 1, zSlab);
  }
  
  String[] split(String text, char ch) {
    int n = 1;
    int i = text.indexOf(ch);
    String[] lines;
    if (i < 0) {
      lines = new String[1];
      lines[0] = text;
      return lines;
    }
    int len = text.length();
    for (; i < len; i++)
      if (text.charAt(i) == ch)
        n++;
    lines = new String[n];
    i = 0;
    len = 0;
    int pt = 0;
    for (; (len = text.indexOf(ch, i)) >= 0;) {
      lines[pt++] = text.substring(i, len);
      i = len + 1;
    }
    lines[pt] = text.substring(i, text.length());
    return lines;
  }
}
