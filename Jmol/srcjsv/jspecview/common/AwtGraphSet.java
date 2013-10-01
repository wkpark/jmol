/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;

/**
 * AwtGraphSet class represents a set of overlaid spectra within some
 * subset of the main JSVPanel. See also GraphSet.java
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class AwtGraphSet extends GraphSet {

  private AwtPanel jsvp;
  private BufferedImage image2D;
  private Color[] plotColors;

  @Override
  protected void disposeImage() {
    image2D = null;
    jsvp = null;
    pd = null;
    highlights = null;
    plotColors = null;
  }


  AwtGraphSet(AwtPanel jsvp) {
    this.jsvp = jsvp;
    this.pd = jsvp.pd;
  }

  @Override
  protected void initGraphSet(int startIndex, int endIndex) {
    setPlotColors(AwtParameters.defaultPlotColors);
    super.initGraphSet(startIndex, endIndex);
  }

  @Override
	void setPlotColors(Object oColors) {
    Color[] colors = (Color[]) oColors;
    if (colors.length > nSpectra) {
      Color[] tmpPlotColors = new Color[nSpectra];
      System.arraycopy(colors, 0, tmpPlotColors, 0, nSpectra);
      colors = tmpPlotColors;
    } else if (nSpectra > colors.length) {
      Color[] tmpPlotColors = new Color[nSpectra];
      int numAdditionColors = nSpectra - colors.length;
      System.arraycopy(colors, 0, tmpPlotColors, 0, colors.length);
      for (int i = 0, j = colors.length; i < numAdditionColors; i++, j++)
        tmpPlotColors[j] = generateRandomColor();
      colors = tmpPlotColors;
    }
    plotColors = colors;
  }

  private static Color generateRandomColor() {
    while (true) {
      int red = (int) (Math.random() * 255);
      int green = (int) (Math.random() * 255);
      int blue = (int) (Math.random() * 255);
      Color randomColor = new Color(red, green, blue);
      if (!randomColor.equals(Color.blue))
        return randomColor;
    }
  }

  @Override
	void setPlotColor0(Object oColor) {
    plotColors[0] = (Color) oColor;
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  Color getPlotColor(int index) {
    if (index >= plotColors.length)
      return null;
    return plotColors[index];
  }

  @Override
  protected void setColor(Object g, ScriptToken whatColor) {
    if (whatColor != null)
      ((Graphics) g)
          .setColor(whatColor == ScriptToken.PLOTCOLOR ? plotColors[0] : jsvp
              .getColor(whatColor));
  }

  private static Color veryLightGrey = new Color(200, 200, 200);

  @Override
  protected void setPlotColor(Object g, int i) {
  	Color c;
  	switch (i) {
  	case -3:
  		c = veryLightGrey;
  		break;
  	case -2:
  		c = Color.GRAY;
  		break;
  	case -1:
  		c = jsvp.getColor(ScriptToken.INTEGRALPLOTCOLOR);
  		break;
    default:
    	c = plotColors[i];
  		break;
  	}
    ((Graphics) g).setColor(c);
  }

  /////////////// 2D image /////////////////

  @Override
	protected void draw2DImage(Object g) {
    if (imageView != null) {
      ((Graphics) g).drawImage(image2D, imageView.xPixel0, imageView.yPixel0, // destination 
          imageView.xPixel0 + imageView.xPixels - 1, // destination 
          imageView.yPixel0 + imageView.yPixels - 1, // destination 
          imageView.xView1, imageView.yView1, imageView.xView2, imageView.yView2, null); // source
    }
  }

  @Override
  protected boolean get2DImage() {
    imageView = new ImageView();
    imageView.set(viewList.get(0).getScale());
    if (!update2dImage(true))
      return false;
    imageView.resetZoom();
    sticky2Dcursor = true;// I don't know why
    return true;
  }

	@Override
	protected boolean update2dImage(boolean isCreation) {
		imageView.set(viewData.getScale());
		JDXSpectrum spec = getSpectrumAt(0);
		int[] buffer = imageView.get2dBuffer(spec, !isCreation);
		if (buffer == null) {
			image2D = null;
			imageView = null;
			return false;
		}
		if (isCreation) {
			buffer = imageView.adjustView(spec, viewData);
			imageView.resetView();
		}
		image2D = new BufferedImage(imageView.imageWidth, imageView.imageHeight,
				BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image2D.getRaster();
		raster.setSamples(0, 0, imageView.imageWidth, imageView.imageHeight, 0,
				buffer);
		setImageWindow();
		return true;
	}


	@Override
  Annotation getAnnotation(double x, double y, String text, boolean isPixels,
                           boolean is2d, int offsetX, int offsetY) {
    return new AwtColoredAnnotation(getSpectrum(), x, y, text, Color.BLACK, isPixels, is2d,
        offsetX, offsetY);
  }

  @Override
  Annotation getAnnotation(List<String> args, Annotation lastAnnotation) {
    return AwtColoredAnnotation.getAnnotation(getSpectrum(), args,
        (AwtColoredAnnotation) lastAnnotation);
  }

  @Override
  protected void fillBox(Object g, int x0, int y0, int x1, int y1,
                         ScriptToken whatColor) {
    setColor(g, whatColor);
    ((Graphics) g).fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x0
        - x1), Math.abs(y0 - y1));
  }

  @Override
  protected void drawTitle(Object g, int height, int width, String title) {
    jsvp.drawTitle(g, height, width, title);
  }

  @Override
  protected void drawHandle(Object g, int x, int y, boolean outlineOnly) {
    if (outlineOnly)
      ((Graphics) g).drawRect(x - 2, y - 2, 4, 4);
    else
      ((Graphics) g).fillRect(x - 2, y - 2, 5, 5);
  }

  @Override
  protected void drawLine(Object g, int x0, int y0, int x1, int y1) {
  	((Graphics) g).drawLine(x0, y0, x1, y1);
  }

  @Override
  protected void drawRect(Object g, int x0, int y0, int nx, int ny) {
    ((Graphics) g).drawRect(x0, y0, nx, ny);
  }

  @Override
  protected void setCurrentBoxColor(Object g) {
    ((Graphics) g).setColor(Color.MAGENTA);
  }

  @Override
  protected void drawString(Object g, String s, int x, int y) {
    ((Graphics) g).drawString(s, x, y);
  }

  @Override
  protected int getFontHeight(Object g) {
    return ((Graphics) g).getFontMetrics().getHeight();
  }

  @Override
  protected int getStringWidth(Object g, String s) {
  	if (s == null)
  		return 0;
    return ((Graphics) g).getFontMetrics().stringWidth(s);
  }

  @Override
	protected void rotatePlot(Object g, int angle, int x, int y) {
  	((Graphics2D) g).rotate(Math.PI * angle / 180.0, x, y);
  }

  @Override
	protected void setAnnotationColor(Object g, Annotation note,
                                    ScriptToken whatColor) {
    if (whatColor != null) {
      setColor(g, whatColor);
      return;
    }
    Color color = null;
    if (note instanceof AwtColoredAnnotation)
      color = ((AwtColoredAnnotation) note).getColor();
    if (color == null)
      color = Color.BLACK;
    ((Graphics) g).setColor(color);
  }


  @Override
  protected void setColor(Object g, int red, int green, int blue) {
    ((Graphics) g).setColor(new Color(red, green, blue));
  }

  BasicStroke strokeBasic = new BasicStroke();
  BasicStroke strokeBold = new BasicStroke(2f);

	@Override
	protected void setStrokeBold(Object g, boolean tf) {
		((Graphics2D) g).setStroke(tf ? strokeBold : strokeBasic);
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	protected void fillArrow(Object g, ArrowType type, int x, int y, boolean doFill) {
		int f = 1;
		switch (type) {
		case LEFT:
		case UP:
			f = -1;
			break;
		}
		int[] axPoints = new int[] { x - 5,   x - 5, x + 5,   x + 5,   x + 8,        x, x - 8 }; 
		int[] ayPoints = new int[] { y + 5*f, y - f, y - f, y + 5*f, y + 5*f, y + 10*f, y + 5*f };
		switch (type) {
		case LEFT:
		case RIGHT:
			if (doFill)
				((Graphics)g).fillPolygon(ayPoints, axPoints, 7);
			else
				((Graphics)g).drawPolygon(ayPoints, axPoints, 7);
			break;
		case UP:
		case DOWN:
			if (doFill)
				((Graphics)g).fillPolygon(axPoints, ayPoints, 7);
			else
				((Graphics)g).drawPolygon(axPoints, ayPoints, 7);

		}
	}

	@Override
	protected void fillCircle(Object g, int x, int y, boolean doFill) {
		if (doFill)
  		((Graphics)g).fillOval(x-4, y-4, 8, 8);
		else
			((Graphics)g).drawOval(x-4, y-4, 8, 8);
	}


}
