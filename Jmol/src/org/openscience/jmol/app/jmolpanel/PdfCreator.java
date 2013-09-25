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
package org.openscience.jmol.app.jmolpanel;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;

import org.jmol.api.JmolPdfCreatorInterface;
import org.jmol.io.JmolOutputChannel;

public class PdfCreator implements JmolPdfCreatorInterface {
  
  public PdfCreator() {
   // for Class.forName  
  }
  
  public String createPdfDocument(JmolOutputChannel out, Object objImage) {
    Image image = (Image) objImage;
    Document document = new Document();
    try {
      int w = image.getWidth(null);
      int h = image.getHeight(null);
      PdfWriter writer = PdfWriter.getInstance(document, out);
      document.open();
      PdfContentByte cb = writer.getDirectContent();
      PdfTemplate tp = cb.createTemplate(w, h);
      Graphics2D g2 = tp.createGraphics(w, h);
      g2.setStroke(new BasicStroke(0.1f));
      tp.setWidth(w);
      tp.setHeight(h);
      g2.drawImage(image, 0, 0, w, h, 0, 0, w, h, null);
      g2.dispose();
      cb.addTemplate(tp, 72, 720 - h);
    } catch (DocumentException de) {
      return de.getMessage();
    }
    document.close();
    return null;
  }

}
