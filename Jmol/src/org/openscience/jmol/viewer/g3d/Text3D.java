/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.openscience.jmol.viewer.g3d;

import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.image.PixelGrabber;
import java.util.Hashtable;

public class Text3D {
  /*
    we have a few problems here
    a message is probably going to vary in size with z depth
    a message is probably going to be repeated by more than one atom
    fonts?
      just one?
      a restricted number?
      any font?
      if we want to support more than one then a fontindex is probably
      called for in order to prevent a hashtable lookup
    color - can be applied by the painter
    rep
      array of booleans - uncompressed
      array of bits - uncompressed - i like this
      some type of run-length, using bytes
  */
  Component component;
  int height; // this height is just ascent + descent ... no reason for leading
  int ascent;
  int width;
  int size;
  int[] bitmap;

  public Text3D(String text, Font font, Platform3D platform) {
    calcMetrics(text, font, platform);
    platform.checkOffscreenSize(width, height);
    renderOffscreen(text, font, platform);
    rasterize(platform);
  }

  /*
  static int widthBuffer;
  static int heightBuffer;
  static Image img;
  static Graphics g;

  void checkImageBufferSize(Component component, int width, int height) {
    boolean realloc = false;
    int widthT = widthBuffer;
    int heightT = heightBuffer;
    if (width > widthT) {
      widthT = (width + 63) & ~63;
      realloc = true;
    }
    if (height > heightT) {
      heightT = (height + 7) & ~7;
      realloc = true;
    }
    if (realloc) {
      if (g != null)
        g.dispose();
      img = component.createImage(widthT, heightT);
      widthBuffer = widthT;
      heightBuffer = heightT;
      g = img.getGraphics();
    }
  }

  */

  void calcMetrics(String text, Font font, Platform3D platform) {
    FontMetrics fontMetrics = platform.getFontMetrics(font);
    ascent = fontMetrics.getAscent();
    height = ascent + fontMetrics.getDescent();
    width = fontMetrics.stringWidth(text);
    size = width*height;
  }

  void renderOffscreen(String text, Font font, Platform3D platform) {
    Graphics g = platform.gOffscreen;
    g.setColor(Color.black);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.white);
    g.setFont(font);
    g.drawString(text, 0, ascent);
  }

  void rasterize(Platform3D platform) {
    PixelGrabber pixelGrabber = new PixelGrabber(platform.imageOffscreen,
                                                 0, 0, width, height, true);
    try {
      pixelGrabber.grabPixels();
    } catch (InterruptedException e) {
      System.out.println("Que? 7748");
    }
    int pixels[] = (int[])pixelGrabber.getPixels();

    int bitmapSize = (size + 31) >> 5;
    bitmap = new int[bitmapSize];

    int offset, shifter;
    for (offset = shifter = 0; offset < size; ++offset, shifter <<= 1) {
      if ((pixels[offset] & 0x00FFFFFF) != 0)
        shifter |= 1;
      if ((offset & 31) == 31)
        bitmap[offset >> 5] = shifter;
    }
    if ((offset & 31) != 0) {
      shifter <<= 31 - (offset & 31);
      bitmap[offset >> 5] = shifter;
    }

    if (false) {
      // error checking
      // shifter error checking
      boolean[] bits = new boolean[size];
      for (int i = 0; i < size; ++i)
        bits[i] = (pixels[i] & 0x00FFFFFF) != 0;
      //
      for (offset = 0; offset < size; ++offset, shifter <<= 1) {
        if ((offset & 31) == 0)
          shifter = bitmap[offset >> 5];
        if (shifter < 0) {
          if (!bits[offset]) {
            System.out.println("false positive @" + offset);
            System.out.println("size = " + size);
          }
        } else {
          if (bits[offset]) {
            System.out.println("false negative @" + offset);
            System.out.println("size = " + size);
          }
        }
      }
      // error checking
    }
  }

  static Hashtable htText = new Hashtable();
  
  // FIXME mth
  // we have a synchronization issue/race condition  here with multiple
  // so only one Text3D can be generated at a time

  synchronized static Text3D getText3D(String text, Font font,
                                         Platform3D platform) {
    int size = font.getSize();
    Text3D[] at25d = (Text3D[])htText.get(text);
    if (at25d != null) {
      if (size <= at25d.length) {
        Text3D t25d = at25d[size - 1];
        if (t25d != null)
          return t25d;
      } else {
        Text3D[] at25dNew = new Text3D[size + 8];
        System.arraycopy(at25d, 0, at25dNew, 0, at25d.length);
        at25d = at25dNew;
        htText.put(text, at25d);
      }
    } else {
      at25d = new Text3D[size + 8];
      htText.put(text, at25d);
    }
    Text3D text3d = new Text3D(text, font, platform);
    at25d[size - 1] = text3d;
    return text3d;
  }

  public static void plot(int x, int y, int z, int argb,
                          String text, Font font, Graphics3D g3d) {
    Text3D text25d = getText3D(text, font, g3d.platform);
    int[] bitmap = text25d.bitmap;
    int textWidth = text25d.width;
    int textHeight = text25d.height;
    if (x + textWidth < 0 || x > g3d.width ||
        y + textHeight < 0 || y > g3d.height)
      return;
    if (x < 0 || x + textWidth > g3d.width ||
        y < 0 || y + textHeight > g3d.height)
      plotClipped(x, y, z, argb, g3d, textWidth, textHeight, bitmap);
    else
      plotUnclipped(x, y, z, argb, g3d, textWidth, textHeight, bitmap);
  }

  static void plotUnclipped(int x, int y, int z, int argb, Graphics3D g3d,
                            int textWidth, int textHeight, int[] bitmap) {
    int offset = 0;
    int shiftregister = 0;
    int i = 0, j = 0;
    short[] zbuf = g3d.zbuf;
    int[] pbuf = g3d.pbuf;
    int screenWidth = g3d.width;
    int pbufOffset = y * screenWidth + x;
    while (i < textHeight) {
      while (j < textWidth) {
        if ((offset & 31) == 0)
          shiftregister = bitmap[offset >> 5];
        if (shiftregister == 0) {
          int skip = 32 - (offset & 31);
          j += skip;
          offset += skip;
          pbufOffset += skip;
        } else {
          if (shiftregister < 0) {
            if (z < zbuf[pbufOffset]) {
              zbuf[pbufOffset] = (short)z;
              pbuf[pbufOffset] = argb;
            }
          }
          shiftregister <<= 1;
          ++offset;
          ++j;
          ++pbufOffset;
        }
      }
      while (j >= textWidth) {
        ++i;
        j -= textWidth;
        pbufOffset += (screenWidth - textWidth);
      }
    }
  }
  
  static void plotClipped(int x, int y, int z, int argb, Graphics3D g3d,
                          int textWidth, int textHeight, int[] bitmap) {
    int offset = 0;
    int shiftregister = 0;
    int i = 0, j = 0;
    while (i < textHeight) {
      while (j < textWidth) {
        if ((offset & 31) == 0)
          shiftregister = bitmap[offset >> 5];
        if (shiftregister == 0) {
          int skip = 32 - (offset & 31);
          j += skip;
          offset += skip;
        } else {
          if (shiftregister < 0)
            g3d.plotPixelClipped(argb, x + j, y + i, z);
          shiftregister <<= 1;
          ++offset;
          ++j;
        }
      }
      while (j >= textWidth) {
        ++i;
        j -= textWidth;
      }
    }
  }
}
