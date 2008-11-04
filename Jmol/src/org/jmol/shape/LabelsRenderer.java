/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;

import java.awt.FontMetrics;

public class LabelsRenderer extends ShapeRenderer {

  // offsets are from the font baseline
  byte fidPrevious;
  protected Font3D font3d;
  protected int ascent;
  protected int descent;

  private final float[] boxXY = new float[2];
  
  protected void render() {
    fidPrevious = 0;

    Labels labels = (Labels) shape;

    String[] labelStrings = labels.strings;
    short[] colixes = labels.colixes;
    short[] bgcolixes = labels.bgcolixes;
    byte[] fids = labels.fids;
    int[] offsets = labels.offsets;
    if (labelStrings == null)
      return;
    Atom[] atoms = modelSet.atoms;
    short backgroundColixContrast = viewer.getColixBackgroundContrast();
    int backgroundColor = viewer.getBackgroundArgb();
    float scalePixelsPerMicron = (viewer.getFontScaling() ? viewer.getScalePixelsPerAngstrom(true) * 10000f : 0);
    //System.out.println("labelsRenderer scalePixelsPerMicron=" + scalePixelsPerMicron);
    float imageFontScaling = viewer.getImageFontScaling();
         
    for (int i = labelStrings.length; --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || modelSet.isAtomHidden(i))
        continue;
      String label = labelStrings[i];
      if (label == null || label.length() == 0)
        continue;
      short colix = (colixes == null || i >= colixes.length) ? 0 : colixes[i];
      colix = Graphics3D.getColixInherited(colix, atom.getColix());
      if (Graphics3D.isColixTranslucent(colix))
        colix = Graphics3D.getColixTranslucent(colix, false, 0);
      short bgcolix = (bgcolixes == null || i >= bgcolixes.length) ? 0
          : bgcolixes[i];
      if (bgcolix == 0 && g3d.getColixArgb(colix) == backgroundColor)
        colix = backgroundColixContrast;
      if (!g3d.setColix(colix))
        continue;
      byte fid = ((fids == null || i >= fids.length || fids[i] == 0) ? labels.zeroFontId
          : fids[i]);
      int offsetFull = (offsets == null || i >= offsets.length ? 0 : offsets[i]);
      boolean labelsFront = ((offsetFull & Labels.FRONT_FLAG) != 0);
      boolean labelsGroup = ((offsetFull & Labels.GROUP_FLAG) != 0);
      int offset = offsetFull >> Labels.FLAG_OFFSET;
      int textAlign = Labels.getAlignment(offsetFull);
      int pointer = offsetFull & Labels.POINTER_FLAGS;
      int zSlab = atom.screenZ - atom.getScreenRadius() - 3;
      if (zSlab < 1)
        zSlab = 1;
      Group group;
      int zBox = (labelsFront ? 1 : labelsGroup
          && (group = atom.getGroup()) != null ? group.getMinZ() : zSlab);
      if (zBox < 1)
        zBox = 1;

      Text text = labels.getLabel(i);
      if (text != null) {
        if (text.font == null)
          text.setFid(fid);
        text.setXYZs(atom.screenX, atom.screenY, zBox, zSlab);
        text.setColix(colix);
        text.setBgColix(bgcolix);
      } else {
        boolean isLeft = (textAlign == Object2d.ALIGN_LEFT || textAlign == Object2d.ALIGN_NONE);
        if (fid != fidPrevious || ascent == 0) {
          g3d.setFont(fid);
          fidPrevious = fid;
          font3d = g3d.getFont3DCurrent();
          if (isLeft) {
            FontMetrics fontMetrics = font3d.fontMetrics;
            ascent = fontMetrics.getAscent();
            descent = fontMetrics.getDescent();
          }
        }
        boolean isSimple = isLeft && (imageFontScaling == 1 && scalePixelsPerMicron == 0
            && label.indexOf("|") < 0 && label.indexOf("<su") < 0);
        if (isSimple) {
          boolean doPointer = ((pointer & Object2d.POINTER_ON) != 0);
          short pointerColix = ((pointer & Object2d.POINTER_BACKGROUND) != 0
              && bgcolix != 0 ? bgcolix : colix);
          boxXY[0] = atom.screenX;
          boxXY[1] = atom.screenY;
          Text.renderSimpleLabel(g3d, font3d, label, colix, bgcolix, boxXY, 
              zBox, zSlab, Object2d.getXOffset(offset), Object2d.getYOffset(offset), 
              ascent, descent, doPointer, pointerColix);
          continue;
        }
        text = new Text(g3d, font3d, label, colix, bgcolix, atom.screenX,
            atom.screenY, zBox, zSlab, textAlign, 0);
        labels.putLabel(i, text);
      }
      text.setOffset(offset);
      if (textAlign != Object2d.ALIGN_NONE)
        text.setAlignment(textAlign);
      text.setPointer(pointer); 
      text.render(g3d, scalePixelsPerMicron, imageFontScaling);
    }
  }
}
