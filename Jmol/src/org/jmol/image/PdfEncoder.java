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

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Map;

import org.jmol.api.ApiPlatform;

/**
 * A relatively primitive PDF generator that just makes a document with an image
 * in it.
 * 
 */
public class PdfEncoder extends ImageEncoder {

  private Image image;

  public PdfEncoder() {
    // for Class.forName  
  }

  @Override
  protected void setParams(Map<String, Object> params) {
    // n/a
    //
    // note that even the signed applet will not have this interface 
    // because the com.lowagie package (itext.jar) is 1.8 MB and is 
    // not included in the applet installation. 
  }

  @Override
  protected void encodeImage(ApiPlatform apiPlatform, Object objImage)
      throws Exception {
    // obviously not going  work in JavaScript. We just let it throw the error in generate().
    this.image = (Image) objImage;
  }

  @Override
  protected void generate() throws Exception {
    Document document = new Document();
    PdfWriter writer = PdfWriter.getInstance(document, out);
    document.open();
    PdfContentByte cb = writer.getDirectContent();
    PdfTemplate tp = cb.createTemplate(width, height);
    Graphics2D g2 = tp.createGraphics(width, height);
    g2.setStroke(new BasicStroke(0.1f));
    tp.setWidth(width);
    tp.setHeight(height);
    g2.drawImage(image, 0, 0, width, height, 0, 0, width, height, null);
    g2.dispose();
    cb.addTemplate(tp, 72, 720 - height);
    document.close();
  }

}
