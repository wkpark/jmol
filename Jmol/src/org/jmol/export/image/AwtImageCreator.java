/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-06-02 12:14:13 -0500 (Sat, 02 Jun 2007) $
 * $Revision: 7831 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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

package org.jmol.export.image;

import java.awt.Image;
import java.io.IOException;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolPdfCreatorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.io.JmolOutputChannel;
import org.jmol.viewer.Viewer;

public class AwtImageCreator extends GenericImageCreator {

  public AwtImageCreator() {
    // by reflection
  }

  @Override
  public String clipImageOrPasteText(JmolViewer viewer, String text) {
    this.viewer = (Viewer) viewer;
    String msg;
    try {
      if (text == null) {
        Image image = (Image) viewer.getScreenImageBuffer(null, true);
        ImageSelection.setClipboard(image);
        msg = "OK image to clipboard: "
            + (image.getWidth(null) * image.getHeight(null));
      } else {
        ImageSelection.setClipboard(text);
        msg = "OK text to clipboard: " + text.length();
      }
    } catch (Error er) {
      msg = viewer.getErrorMessage();
    } finally {
      if (text == null)
        viewer.releaseScreenImage();
    }
    return msg;
  }

  @Override
  public String getClipboardText() {
    return ImageSelection.getClipboardText();
  }

  public static String getClipboardTextStatic() {
    
    return ImageSelection.getClipboardText();
  }

  /**
   * @param objImage
   * @param type
   * @param out
   * @param errRet
   * @return byte array if needed
   * @throws IOException
   */
  @Override
  protected boolean getOtherBytes(Object objImage, String type,
                                 JmolOutputChannel out,
                                 Map<String, Object> params, String[] errRet)
      throws IOException {
    java.awt.Image image = (java.awt.Image) objImage;
    type = type.substring(0, 1) + type.substring(1).toLowerCase();
    if (!type.equals("Pdf"))
      return ImageEncoder.write(type, image, out, params);
    // applet will not have this interface
    // PDF is application-only because it is such a HUGE package
    JmolPdfCreatorInterface pci = (JmolPdfCreatorInterface) Interface
        .getApplicationInterface("jmolpanel.PdfCreator");
    errRet[0] = pci.createPdfDocument(out, image);
    return errRet[0] == null;
  }

}
