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
package org.jmol.shape;

import java.awt.FontMetrics;
import javax.vecmath.Point3f;

import org.jmol.api.JmolRendererInterface;
import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.util.Escape;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;
import org.jmol.util.TextFormat;

public class Text {

  public final static int POINTER_NONE = 0;
  public final static int POINTER_ON = 1;
  public final static int POINTER_BACKGROUND = 2;

  private final static String[] hAlignNames = { "", "left", "center", "right",
      "" };

  final static int ALIGN_NONE = 0;
  final static int ALIGN_LEFT = 1;
  final static int ALIGN_CENTER = 2;
  final static int ALIGN_RIGHT = 3;

  final static String[] vAlignNames = { "xy", "top", "bottom", "middle" };

  final static int VALIGN_XY = 0;
  final static int VALIGN_TOP = 1;
  final static int VALIGN_BOTTOM = 2;
  final static int VALIGN_MIDDLE = 3;
  final static int VALIGN_XYZ = 4;

  private boolean isLabelOrHover;
  private Viewer viewer;
  private JmolRendererInterface g3d;
  Point3f xyz;
  String target;
  private String text, textUnformatted;
  private String script;

  private float scalePixelsPerMicron;

  float getScalePixelsPerMicron() {
    return scalePixelsPerMicron;
  }

  void setScalePixelsPerMicron(float scalePixelsPerMicron) {
    
    fontScale = 0;//fontScale * this.scalePixelsPerMicron / scalePixelsPerMicron;
    this.scalePixelsPerMicron = scalePixelsPerMicron;
  }
  private float fontScale;

  private boolean doFormatText;

  private String[] lines;
  private int align;
  int valign;
  private int pointer;
  private int movableX;
  private int movableY;
  private int movableXPercent = Integer.MAX_VALUE;
  private int movableYPercent = Integer.MAX_VALUE;
  private int offsetX;
  private int offsetY;
  private int z;
  private int zSlab; // z for slabbing purposes -- may be near an atom

  private int windowWidth;
  private int windowHeight;
  private boolean adjustForWindow;
  private float boxWidth, boxHeight, boxX, boxY;

  int modelIndex = -1;
  boolean visible = true;

  Font3D font;
  private FontMetrics fm;
  private byte fid;
  float fontSize;
  private int ascent;
  private int descent;
  private int lineHeight;

  private short colix;
  private short bgcolix;

  private int[] widths;
  private int textWidth;
  private int textHeight;

  // for labels and hover
  Text(JmolRendererInterface g3d, Font3D font, String text, short colix,
      short bgcolix, int x, int y, int z, int zSlab, int textAlign,
      float scalePixelsPerMicron) {
    this.scalePixelsPerMicron = scalePixelsPerMicron;
    //System.out.println("Text scalePixelsPerMicron=" + scalePixelsPerMicron);
    this.viewer = null;
    this.g3d = g3d;
    isLabelOrHover = true;
    setText(text);
    this.colix = colix;
    this.bgcolix = bgcolix;
    setXYZs(x, y, z, zSlab);
    align = textAlign;
    setFont(font);
  }

  // for echo
  Text(Viewer viewer, Graphics3D g3d, Font3D font, String target, short colix,
      int valign, int align, float scalePixelsPerMicron) {
    this.viewer = viewer;
    this.g3d = g3d;
    isLabelOrHover = false;
    this.target = target;
    if (target.equals("error"))
      valign = VALIGN_TOP;
    this.align = align;
    this.valign = valign;
    this.font = font;
    this.colix = colix;
    this.scalePixelsPerMicron = scalePixelsPerMicron;
    this.z = 2;
    this.zSlab = Integer.MIN_VALUE;
    fontSize = font.fontSizeNominal;
    getFontMetrics();
  }

  private void getFontMetrics() {
    fm = font.fontMetrics;
    descent = fm.getDescent();
    ascent = fm.getAscent();
    lineHeight = ascent + descent;
  }

  void setFid(byte fid) { //labels only
    if (this.fid == fid)
      return;
    fontScale = 0;
    setFont(Font3D.getFont3D(fid));
  }

  void setModel(int modelIndex) {
    this.modelIndex = modelIndex;
  }

  void setVisibility(boolean TF) {
    visible = TF;
  }

  void setXYZ(Point3f xyz) {
    valign = VALIGN_XYZ;
    this.xyz = xyz;
    setAdjustForWindow(false);
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

  void setTranslucent(float level, boolean isBackground) {
    if (isBackground) {
      if (bgcolix != 0)
        bgcolix = Graphics3D.getColixTranslucent(bgcolix, !Float.isNaN(level),
            level);
    } else {
      colix = Graphics3D.getColixTranslucent(colix, !Float.isNaN(level), level);
    }
  }

  void setBgColix(short colix) {
    this.bgcolix = colix;
  }

  void setBgColix(Object value) {
    bgcolix = (value == null ? (short) 0 : Graphics3D.getColix(value));
  }

  void setMovableX(int x) {
    valign = (valign == VALIGN_XYZ ? VALIGN_XYZ : VALIGN_XY);
    movableX = x;
    movableXPercent = Integer.MAX_VALUE;
  }

  void setMovableY(int y) {
    valign = (valign == VALIGN_XYZ ? VALIGN_XYZ : VALIGN_XY);
    movableY = y;
    movableYPercent = Integer.MAX_VALUE;
  }

  void setMovableXPercent(int x) {
    valign = (valign == VALIGN_XYZ ? VALIGN_XYZ : VALIGN_XY);
    movableX = Integer.MAX_VALUE;
    movableXPercent = x;
  }

  void setMovableYPercent(int y) {
    valign = (valign == VALIGN_XYZ ? VALIGN_XYZ : VALIGN_XY);
    movableY = Integer.MAX_VALUE;
    movableYPercent = y;
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

  void setScript(String script) {
    this.script = (script == null || script.length() == 0 ? null : script);
  }

  String getScript() {
    return script;
  }

  void setOffset(int offset) {
    //Labels only
    offsetX = getXOffset(offset);
    offsetY = getYOffset(offset);
    valign = VALIGN_XY;
  }

  static int getXOffset(int offset) {
    switch (offset) {
    case 0:
      return JmolConstants.LABEL_DEFAULT_X_OFFSET;
    case Short.MAX_VALUE:
      return 0;
    default:
      return (byte) ((offset >> 8) & 0xFF);
    }
  }

  static int getYOffset(int offset) {
    switch (offset) {
    case 0:
      return -JmolConstants.LABEL_DEFAULT_Y_OFFSET;
    case Short.MAX_VALUE:
      return 0;
    default:
      return -(int) (byte) (offset & 0xFF);
    }
  }

  void setText(String text) {
    text = fixText(text);
    if (this.text != null && this.text.equals(text))
      return;
    this.text = text;
    textUnformatted = text;
    doFormatText = (viewer != null && text != null && (text.indexOf("%{") >= 0 || text
        .indexOf("@{") >= 0));
    if (!doFormatText)
      recalc();
  }

  void setFont(Font3D f3d) {
    font = f3d;
    if (font == null)
      return;
    fid = font.fid;
    fontSize = font.fontSizeNominal;
    getFontMetrics();
    recalc();
  }

  private void setFontScale(float scale) {
    if (fontScale == scale)
      return;
    //System.out.println(fontSize + " " + scale + " " + (fontSize * scale));
    fontScale = scale;
    setFont(g3d.getFont3DScaled(font, scale));
  }

  boolean setAlignment(String align) {
    if ("left".equals(align))
      return setAlignment(ALIGN_LEFT);
    if ("center".equals(align))
      return setAlignment(ALIGN_CENTER);
    if ("right".equals(align))
      return setAlignment(ALIGN_RIGHT);
    return false;
  }

  static String getAlignment(int align) {
    return hAlignNames[align & 3];
  }

  boolean setAlignment(int align) {
    if (this.align != align) {
      this.align = align;
      recalc();
    }
    return true;
  }

  void setPointer(int pointer) {
    this.pointer = pointer;
  }

  static String getPointer(int pointer) {
    return ((pointer & POINTER_ON) == 0 ? ""
        : (pointer & POINTER_BACKGROUND) > 0 ? "background" : "on");
  }

  String fixText(String text) {
    if (text == null || text.length() == 0)
      return null;
    int pt;
    while ((pt = text.indexOf("\n")) >= 0)
      text = text.substring(0, pt) + "|" + text.substring(pt + 1);
    return text;
  }

  void recalc() {
    if (text == null) {
      text = null;
      lines = null;
      widths = null;
      return;
    }
    if (fm == null)
      return;
    lines = TextFormat.split(text, '|');
    textWidth = 0;
    widths = new int[lines.length];
    for (int i = lines.length; --i >= 0;)
      textWidth = Math.max(textWidth, widths[i] = stringWidth(lines[i]));
    textHeight = lines.length * lineHeight;
    boxWidth = textWidth + (fontScale >= 2 ? 16 : 8);
    boxHeight = textHeight + (fontScale >= 2 ? 16 : 8);
    //System.out.println("Text recalc fontScale,font.fontScale,font.fontScaleNominal=" 
      //  +fontScale + " "+ font.fontSize + " "+ font.fontSizeNominal);
  }

  private void formatText() {
    text = (viewer == null ? textUnformatted : viewer
        .formatText(textUnformatted));
    recalc();
  }

  final float[] boxXY = new float[2];
  
  void render(JmolRendererInterface g3d, float scalePixelsPerMicron,
              float imageFontScaling) {
    if (text == null)
      return;
    //if (imageFontScaling == 4)
    //System.out.println("render scalePixelsPerMicron=" + scalePixelsPerMicron + " imageFontScaling=" + imageFontScaling);
    windowWidth = g3d.getRenderWidth();
    windowHeight = g3d.getRenderHeight();
    if (this.scalePixelsPerMicron < 0 && scalePixelsPerMicron != 0)
      this.scalePixelsPerMicron = scalePixelsPerMicron;
    if (scalePixelsPerMicron != 0 && this.scalePixelsPerMicron != 0)
      setFontScale(scalePixelsPerMicron / this.scalePixelsPerMicron);
    else if (fontScale != imageFontScaling)
      setFontScale(imageFontScaling);
    
    if (doFormatText)
      formatText();

    if (isLabelOrHover) {
      boxXY[0] = movableX;
      boxXY[1] = movableY;      
      setLabelPosition(boxWidth, boxHeight, 
          offsetX * imageFontScaling, offsetY * imageFontScaling, boxXY);
    } else {
      setPosition();
    }
    boxX = boxXY[0];
    boxY = boxXY[1];

    // adjust positions if necessary

    if (adjustForWindow)
      setBoxOffsetsInWindow();

    // draw the box if necessary

    if (bgcolix != 0 && g3d.setColix(bgcolix))
      showBox(g3d, colix, bgcolix, (int) boxX, (int) boxY, z + 2, zSlab, 
          (int) boxWidth, (int) boxHeight, fontScale, isLabelOrHover);
    if (g3d.setColix(colix)) {

      // now set x and y positions for text from (new?) box position

      float offset = boxWidth;
      float adj = (fontScale >= 2 ? 8 : 4);
      int x0 = (int) boxX;
      switch (align) {
      case ALIGN_CENTER:
        x0 += offset / 2;
        break;
      case ALIGN_RIGHT:
        x0 += offset - adj;
        break;
      default:
        x0 += adj;
      }

      // now write properly aligned text

      float x = x0;
      float y = boxY + ascent + adj;
      //System.out.println("scale=" + fontScale + " boxwidth/height=" + boxWidth
        //  + "/" + boxHeight + " ascent=" + ascent + " lineheight=" + lineHeight
          //+ " boxY=" + boxY + " adj=" + adj);
      offset = lineHeight;
      for (int i = 0; i < lines.length; i++) {
        switch (align) {
        case ALIGN_CENTER:
          x = x0 - widths[i] / 2;
          break;
        case ALIGN_RIGHT:
          x = x0 - widths[i];
        }
        //System.out.println(lines[i] + " text render font " + font.fid + " " + font.fontSize);
        g3d.drawString(lines[i], font, (int) x, (int) y, z, zSlab);
        y += offset;
      }
    }

    // now draw the pointer, if requested

    if ((pointer & POINTER_ON) != 0) {
      g3d.setColix((pointer & POINTER_BACKGROUND) != 0 && bgcolix != 0 ? bgcolix
              : colix);
      if (boxX > movableX)
        g3d.drawLine(movableX, movableY, zSlab, (int) boxX, (int) (boxY + boxHeight / 2),
            zSlab);
      else if (boxX + boxWidth < movableX)
        g3d.drawLine(movableX, movableY, zSlab, (int) (boxX + boxWidth), 
            (int) (boxY + boxHeight / 2), zSlab);
    }
  }

  private void setPosition() {
    float xLeft, xCenter, xRight;
    boolean is3dEcho = (xyz != null);
    if (valign == VALIGN_XY || valign == VALIGN_XYZ) {
      float x = (movableXPercent != Integer.MAX_VALUE ? movableXPercent
          * windowWidth / 100 : is3dEcho ? movableX : movableX * fontScale);
      float offsetX = this.offsetX * fontScale;
      xLeft = xRight = xCenter = x + offsetX;
      //System.out.print("movableX = " + movableX + " offsetX = " + offsetX);
    } else {
      xLeft = 5 * fontScale;
      xCenter = windowWidth / 2;
      xRight = windowWidth - xLeft;
    }

    // set box X from alignments

    boxXY[0] = xLeft;
    switch (align) {
    case ALIGN_CENTER:
      boxXY[0] = xCenter - boxWidth / 2;
      break;
    case ALIGN_RIGHT:
      boxXY[0] = xRight - boxWidth;
    }

    // set box Y from alignments

    boxXY[1] = 0;
    switch (valign) {
    case VALIGN_TOP:
      break;
    case VALIGN_MIDDLE:
      boxXY[1] = windowHeight / 2;
      break;
    case VALIGN_BOTTOM:
      boxXY[1] = windowHeight;
      break;
    default:
      float y = (movableXPercent != Integer.MAX_VALUE ? movableYPercent
          * windowHeight / 100 : is3dEcho? movableY : movableY * fontScale);
      float offsetY = this.offsetY * fontScale;
      boxXY[1] = (is3dEcho ? y : (windowHeight - y)) + offsetY;
      //System.out.println(" movableY = " + movableY + " offsetY = " + offsetY + " boxXY[1]=" + boxXY[1]);
    }
    
    if (valign == VALIGN_XYZ)
      boxXY[1] += ascent / 2;

  }

  static void setLabelPosition(float boxWidth, float boxHeight,
                               float xOffset, float yOffset, float[] boxXY) {
    float xBoxOffset, yBoxOffset;

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

    boxXY[0] += xBoxOffset;
    boxXY[1] += yBoxOffset;
  
  }
  
  void setBoxOffsetsInWindow() {
    // not labels

    // these coordinates are (0,0) in top left
    // (user coordinates are (0,0) in bottom left)
    float margin = 5 * fontScale;
    float bw = boxWidth + margin;
    float x = boxXY[0];
    if (x + bw > windowWidth)
      x = windowWidth - bw;
    if (x < margin)
      x = margin;
    boxXY[0] = x;

    margin = (isLabelOrHover ? 16 * fontScale + lineHeight : 0);
    float bh = boxHeight;
    float y = boxXY[1] - textHeight;
    if (y + bh > windowHeight)
      y = windowHeight - bh;
    if (y < margin)
      y = margin;
    boxXY[1] = y;
  }

  private static void showBox(JmolRendererInterface g3d, short colix,
                              short bgcolix, int x, int y, int z, int zSlab,
                              int boxWidth, int boxHeight,
                              float imageFontScaling, boolean atomBased) {
    g3d.fillRect(x, y, z, zSlab, boxWidth, boxHeight);
    g3d.setColix(colix);
    if (!atomBased)
      return;
    if (imageFontScaling >= 2) {
      g3d.drawRect(x + 3, y + 3, z - 1, zSlab, boxWidth - 6, boxHeight - 6);
      g3d.drawRect(x + 4, y + 4, z - 1, zSlab, boxWidth - 8, boxHeight - 8);
    } else {
      g3d.drawRect(x + 1, y + 1, z - 1, zSlab, boxWidth - 2, boxHeight - 2);
    }
  }

  final static void renderSimpleLabel(JmolRendererInterface g3d, Font3D font,
                                 String strLabel, short colix, short bgcolix,
                                 float[] boxXY, int z, int zSlab,
                                 int xOffset, int yOffset, float ascent,
                                 int descent, boolean doPointer,
                                 short pointerColix) {

    // old static style -- quick, simple, no line breaks, odd alignment?
    // LabelsRenderer only

    float boxWidth = font.fontMetrics.stringWidth(strLabel) + 8;
    float boxHeight = ascent + descent + 8;
    
    int x0 = (int) boxXY[0];
    int y0 = (int) boxXY[1];
    
    setLabelPosition(boxWidth, boxHeight, xOffset, yOffset, boxXY);

    float x = boxXY[0];
    float y = boxXY[1];
    if (bgcolix != 0 && g3d.setColix(bgcolix))
      showBox(g3d, colix, bgcolix, (int) x, (int) y, z, zSlab, (int) boxWidth,
          (int) boxHeight, 1, true);
    else
      g3d.setColix(colix);
    g3d.drawString(strLabel, font, (int) (x + 4),
        (int) (y + 4 + ascent), z - 1, zSlab);

    if (doPointer) {
      g3d.setColix(pointerColix);
      if (xOffset > 0)
        g3d.drawLine(x0, y0, zSlab, (int) x, (int) (y + boxHeight / 2), zSlab);
      else if (xOffset < 0)
        g3d.drawLine(x0, y0, zSlab, (int) (x + boxWidth),
            (int) (y + boxHeight / 2), zSlab);
    }
  }

  public String getState(boolean isDefine) {
    StringBuffer s = new StringBuffer();
    if (text == null || isLabelOrHover || target.equals("error"))
      return "";
    //set echo top left
    //set echo myecho x y
    //echo .....

    if (isDefine) {
      String strOff = null;
      switch (valign) {
      case VALIGN_XY:
        strOff = (movableXPercent == Integer.MAX_VALUE ? movableX + " "
            : movableXPercent + "% ");
        strOff += (movableYPercent == Integer.MAX_VALUE ? movableY + ""
            : movableYPercent + "%");
      //fall through
      case VALIGN_XYZ:
        if (strOff == null)
          strOff = Escape.escape(xyz);
        s.append("  set echo ").append(target).append(" ").append(strOff);
        if (align != ALIGN_LEFT)
          s.append("  set echo ").append(target).append(" ").append(
              hAlignNames[align]);
        break;
      default:
        s.append("  set echo ").append(vAlignNames[valign]).append(" ").append(
            hAlignNames[align]);
      }
      s.append("; echo ").append(Escape.escape(textUnformatted)).append(";\n");
      if (script != null)
        s.append("  set echo ").append(target).append(" script ").append(
            Escape.escape(script)).append(";\n");
      if (modelIndex >= 0)
        s.append("  set echo ").append(target).append(" model ").append(
            viewer.getModelNumberDotted(modelIndex)).append(";\n");
    }
    //isDefine and target==top: do all
    //isDefine and target!=top: just start
    //!isDefine and target==top: do nothing
    //!isDefine and target!=top: do just this
    //fluke because top is defined with default font
    //in initShape(), so we MUST include its font def here
    if (isDefine != target.equals("top"))
      return s.toString();
    // these may not change much:
    s.append("  " + Shape.getFontCommand("echo", font));
    if (scalePixelsPerMicron > 0)
      s.append (" " + (10000f / scalePixelsPerMicron)); // Angstroms per pixel
    s.append(";\n");
    s.append("  color echo");
    if (Graphics3D.isColixTranslucent(colix))
      s.append(" translucent " + Graphics3D.getColixTranslucencyLevel(colix));
    s.append(" [x").append(g3d.getHexColorFromIndex(colix)).append("]");
    if (bgcolix != 0) {
      s.append("; color echo background");
      if (Graphics3D.isColixTranslucent(bgcolix))
        s.append(" translucent "
            + Graphics3D.getColixTranslucencyLevel(bgcolix));
      s.append(" [x").append(g3d.getHexColorFromIndex(bgcolix)).append("]");
    }
    s.append(";\n");
    return s.toString();
  }

  public boolean checkObjectClicked(int x, int y) {
    return (script != null 
        && x >= boxX && x <= boxX + boxWidth 
        && y >= boxY && y <= boxY + boxHeight);
  }

  private int stringWidth(String str) {
    int w = 0;
    int f = 1;
    int subscale = 1; //could be something less than that
    if (str == null)
      return 0;
    if (str.indexOf("<su") < 0)
      return fm.stringWidth(str);
    int len = str.length();
    String s;
    for (int i = 0; i < len; i++) {
      if (str.charAt(i) == '<') {
        if (i + 4 < len
            && ((s = str.substring(i, i + 5)).equals("<sub>") || s
                .equals("<sup>"))) {
          i += 4;
          f = subscale;
          continue;
        }
        if (i + 5 < len
            && ((s = str.substring(i, i + 6)).equals("</sub>") || s
                .equals("</sup>"))) {
          i += 5;
          f = 1;
          continue;
        }
      }
      w += fm.stringWidth(str.substring(i, i + 1)) * f;
    }
    return w;
  }
}
