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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'JSVPanel.java'
// University of the West Indies, Mona Campus
//
// 25-06-2007 rjl - bug in ReversePlot for non-continuous spectra fixed
//                - previously, one point less than npoints was displayed
// 25-06-2007 cw  - show/hide/close modified
// 10-02-2009 cw  - adjust for non zero baseline in North South plots
// 24-08-2010 rjl - check coord output is not Internationalised and uses decimal point not comma
// 31-10-2010 rjl - bug fix for drawZoomBox suggested by Tim te Beek
// 01-11-2010 rjl - bug fix for drawZoomBox
// 05-11-2010 rjl - colour the drawZoomBox area suggested by Valery Tkachenko
// 23-07-2011 jak - Added feature to draw the x scale, y scale, x units and y units
//					independently of each other. Added independent controls for the font,
//					title font, title bold, and integral plot color.
// 24-09-2011 jak - Altered drawGraph to fix bug related to reversed highlights. Added code to
//					draw integration ratio annotations
// 03-06-2012 rmh - Full overhaul; code simplification; added support for Jcamp 6 nD spectra

package jspecview.common;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import jspecview.common.Annotation.AType;
import jspecview.export.Exporter;
import jspecview.util.JSVLogger;
import jspecview.util.JSVTextFormat;

/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtPanel extends JPanel implements JSVPanel, Printable, MouseListener,
    MouseMotionListener, KeyListener {

  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    JSVLogger.info("JSVPanel " + this + " finalized");
  }

  private AwtPopupMenu popup;
  private ScriptInterface si;

  public PanelData pd;

  
  public PanelData getPanelData() {
    return pd;
  }

  public String getTitle() {
  	return pd.getTitle();
  }
  
  public AwtPopupMenu getPopup() {
    return popup;
  }

  public void dispose() {
    if (popup != null) {
      popup.dispose();
      popup = null;
    }
    //toolTip = null;
    if (pd != null)
      pd.dispose();
    pd = null;
    removeKeyListener(this);
    removeMouseListener(this);
    removeMouseMotionListener(this);
  }

  public void setTitle(String title) {
    pd.title = title;
    setName(title);
  }

  
  ////////// settable colors //////////

  private Color coordinatesColor;
  private Color gridColor;
  private Color integralPlotColor;
  private Color peakTabColor;
  private Color plotAreaColor;
  private Color scaleColor;
  private Color titleColor;
  private Color unitsColor;
  // potentially settable; 

  private Color highlightColor = new Color(255, 0, 0, 200);
  private Color zoomBoxColor = new Color(150, 150, 100, 130);
  private Color zoomBoxColor2 = new Color(150, 100, 100, 130);

	private static int MAC_COMMAND = InputEvent.BUTTON1_MASK + InputEvent.BUTTON3_MASK;

  public void setPlotColors(Object oColors) {
    Color[] colors = (Color[]) oColors;
    for (int i = pd.graphSets.size(); --i >= 0;)
      pd.graphSets.get(i).setPlotColors(colors);
  }


  @SuppressWarnings("incomplete-switch")
	public void setColorOrFont(Parameters ds, ScriptToken st) {
    if (st == null) {
      Map<ScriptToken, Object> colors = ds.getColors();
      for (Map.Entry<ScriptToken, Object> entry : colors.entrySet())
        setColorOrFont(ds, entry.getKey());
      setColorOrFont(ds, ScriptToken.DISPLAYFONTNAME);
      setColorOrFont(ds, ScriptToken.TITLEFONTNAME);
      return;
    }
    switch (st) {
    case DISPLAYFONTNAME:
      pd.setFontName(st, ds.getDisplayFont());
      return;
    case TITLEFONTNAME:
      pd.setFontName(st, ds.getTitleFont());
      return;
    }
    setColor(st, ds.getColor(st));
  }

  public void setColor(ScriptToken st, Object oColor) {
    Color color = (Color) oColor;
    if (color != null)
      pd.options.put(st, AwtParameters.colorToHexString(color));
    switch (st) {
    case BACKGROUNDCOLOR:
      setBackground(color);
      break;
    case COORDINATESCOLOR:
      coordinatesColor = color;
      break;
    case GRIDCOLOR:
      gridColor = color;
      break;
    case HIGHLIGHTCOLOR:
      highlightColor = color;
      break;
    case INTEGRALPLOTCOLOR:
      integralPlotColor = color;
      break;
    case PEAKTABCOLOR:
    	peakTabColor = color;
    	break;
    case PLOTCOLOR:
      for (int i = pd.graphSets.size(); --i >= 0;)
        pd.graphSets.get(i).setPlotColor0(color);
      break;
    case PLOTAREACOLOR:
      plotAreaColor = color;
      break;
    case SCALECOLOR:
      scaleColor = color;
      break;
    case TITLECOLOR:
      titleColor = color;
      break;
    case UNITSCOLOR:
      unitsColor = color;
      break;
    case ZOOMBOXCOLOR:
      zoomBoxColor = color;
      break;
    case ZOOMBOXCOLOR2:
      zoomBoxColor2 = color;
      break;
    default:
      JSVLogger.warn("AwtPanel --- unrecognized color: " + st);
      break;
    }
  }

  /**
   * Constructs a new JSVPanel
   * @param si 
   * 
   * @param spectrum
   *        the spectrum
   * @param popup 
   */
  public AwtPanel(ScriptInterface si, JDXSpectrum spectrum, AwtPopupMenu popup) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx
  	ToolTipManager.sharedInstance().setInitialDelay(0);
  	//toolTip = new AwtToolTip(this);
  	this.si = si;
    pd = new PanelData(this);
    this.popup = popup;
    pd.initSingleSpectrum(spectrum);
  }

  public JSVPanel getNewPanel(ScriptInterface si, JDXSpectrum spectrum) {
    return new AwtPanel(si, spectrum, popup);
  }

  public static AwtPanel getJSVPanel(ScriptInterface si, List<JDXSpectrum> specs, int startIndex, int endIndex, AwtPopupMenu popup) {
    return new AwtPanel(si, specs, startIndex, endIndex, popup);
  }

  /**
   * Constructs a <code>JSVPanel</code> with List of spectra and corresponding
   * start and end indices of data points that should be displayed
   * @param si 
   * 
   * @param spectra
   *        the List of <code>Graph</code> instances
   * @param startIndex
   *        the start index
   * @param endIndex
   *        the end index
   * @param popup 
   */
  private AwtPanel(ScriptInterface si, List<JDXSpectrum> spectra, int startIndex,
      int endIndex, AwtPopupMenu popup) {
    pd = new PanelData(this);
    this.si = si;
    this.popup = popup;
  	//toolTip = new AwtToolTip(this);
    pd.initJSVPanel(spectra, startIndex, endIndex);
  }

  /**
   * generates a single panel or an integrated panel, as appropriate
   * @param si 
   * 
   * @param spec
   * @param jsvpPopupMenu
   * @return new panel
   */
  public static AwtPanel getNewPanel(ScriptInterface si, JDXSpectrum spec,
                                     AwtPopupMenu jsvpPopupMenu) {
    return new AwtPanel(si, spec, jsvpPopupMenu);
  }

  public GraphSet getNewGraphSet() {
    return new AwtGraphSet(this);
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  public Color getPlotColor(int index) {
    return ((AwtGraphSet) pd.getCurrentGraphSet()).getPlotColor(index);
  }

  public Color getColor(int r, int g, int b, int a) {
    return new Color(r, g, b, a);
  }
  
  public Color getColor(ScriptToken whatColor) {
    switch (whatColor) {
    default:
      JSVLogger.error("awtgraphset missing color " + whatColor);
      return Color.BLACK;
    case ZOOMBOXCOLOR2:
      return zoomBoxColor2;
    case ZOOMBOXCOLOR:
      return zoomBoxColor;
    case HIGHLIGHTCOLOR:
      return highlightColor;
    case INTEGRALPLOTCOLOR:
      return integralPlotColor;
    case GRIDCOLOR:
      return gridColor;
    case PEAKTABCOLOR:
    	return peakTabColor;
    case PLOTAREACOLOR:
      return plotAreaColor;
    case SCALECOLOR:
      return scaleColor;
    case TITLECOLOR:
      return titleColor;
    case UNITSCOLOR:
      return unitsColor;
    }
  }

  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  public void doRepaint() {
  	// to the system
  	if (!pd.isPrinting)
      si.requestRepaint();
  }
  
  @Override
	public void update(Graphics g) {
  	// from the system
  	// System: Do not clear rectangle -- we are opaque and will take care of that.
  	// seems unnecessary, but apparently for the Mac it is critical. Still not totally convinced!
      paint(g);
  }
 

  /**
   * Overrides paintComponent in class JPanel in order to draw the spectrum
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  @Override
  public void paintComponent(Graphics g) {
  	
  	// from the system, via update or applet/app repaint
  	
    if (si == null || pd == null || pd.graphSets == null || pd.isPrinting)
      return;
    
    super.paintComponent(g); // paint background 
    
    pd.drawGraph(g, getWidth(), getHeight(), false);
    si.repaintCompleted();
  }

  public void setFont(Object g, String name, int width, int mode, int size,  
  		boolean isLabel) {
    if (isLabel) {
      if (width < 400)
        size = ((width * size) / 400);
    } else {
      if (width < 250)
        size = ((width * size) / 250);
    }
    ((Graphics) g).setFont(new Font(name, mode, size));
  }

  /**
   * Draws the Coordinates
   * @param og 
   *        the <code>Graphics</code> object
   * @param top 
   */
  public void drawCoordinates(Object og, int top) {
  	if (pd.coordStr == null)
  		return;
    Graphics g = (Graphics) og;
    g.setColor(coordinatesColor);
    pd.setFont(g, getWidth(), Font.PLAIN, 12, true);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(pd.coordStr, 
    		pd.thisWidth - pd.right - fm.stringWidth(pd.coordStr), 
    		top - 20);
  }

	/**
	 * draws the file path only for printing
	 * @param og 
	 * @param x 
	 * @param y 
	 * @param s
	 */

	public void printFilePath(Object og, int x, int y, String s) {
		x *= pd.scalingFactor;
		y *= pd.scalingFactor;
		if (s.indexOf("?") > 0)
			s = s.substring(s.indexOf("?") + 1);
		s = s.substring(s.lastIndexOf("/") + 1);
		s = s.substring(s.lastIndexOf("\\") + 1);
		Graphics g = (Graphics) og;
		g.setColor(Color.BLACK);
		pd.setFont(g, 1000, Font.PLAIN, 9, true);
		FontMetrics fm = g.getFontMetrics();
		if (x != pd.left * pd.scalingFactor)
			x -= fm.stringWidth(s);
		g.drawString(s, x, y - fm.getHeight());
	}

	public void printVersion(Object og, int pageHeight) {
		Graphics g = (Graphics) og;
		g.setColor(Color.BLACK);
		pd.setFont(g, 1000, Font.PLAIN, 9, true);
		FontMetrics fm = g.getFontMetrics();
		String s = DateFormat.getInstance().format(new Date()) + " JSpecView "
				+ JSVersion.VERSION_SHORT;
		int w = fm.stringWidth(s);
		g.drawString(s, (pd.thisWidth - pd.right) * pd.scalingFactor - w,
				pageHeight * pd.scalingFactor - fm.getHeight());
	}


  /**
   * Draws Title
   * 
   * @param pageHeight
   *        the height to be drawn in pixels -- after scaling
   * @param pageWidth
   *        the width to be drawn in pixels -- after scaling
   */
  public void drawTitle(Object og, int pageHeight, int pageWidth, String title) {
  	title = title.replace('\n', ' ');
    Graphics g = (Graphics) og;
    pd.setFont(g, pageWidth, pd.isPrinting || pd.getBoolean(ScriptToken.TITLEBOLDON) ? Font.BOLD
        : Font.PLAIN, 14, true);
    FontMetrics fm = g.getFontMetrics();
    int nPixels = fm.stringWidth(title);
    if (nPixels > pageWidth) {
    	int size = (int) (14.0 * pageWidth / nPixels);
    	if (size < 10)
    		size = 10;
      pd.setFont(g, pageWidth, pd.isPrinting || pd.getBoolean(ScriptToken.TITLEBOLDON) ? Font.BOLD
          : Font.PLAIN, size, true);
      fm = g.getFontMetrics();
    }
    g.setColor(titleColor);
    g.drawString(title, (pd.isPrinting ? pd.left * pd.scalingFactor : 5), 
    		pageHeight - (int) (fm.getHeight() * (pd.isPrinting ? 2 : 0.5)));
  }



  /*----------------- METHODS IN INTERFACE Printable ---------------------- */

  /**
   * uses itext to create the document, either to a file or a byte stream
   * @param os 
   * @param pl 
   */
  private void createPdfDocument(OutputStream os, PrintLayout pl) {
  	PdfCreatorInterface pdfCreator = (PdfCreatorInterface) Interface.getInterface("jspecview.common.PdfCreator");
  	if (pdfCreator == null)
  		return;
  	pdfCreator.createPdfDocument(this, pl, os);
  }

	/**
	 * Send a print job of the spectrum to the default printer on the system
	 * 
	 * @param pl
	 *          the layout of the print job
	 * @param os
	 * @param title 
	 */
	public void printPanel(PrintLayout pl, OutputStream os, String title) {

		// MediaSize size = MediaSize.getMediaSizeForName(pl.paper);

		// Set Graph Properties
		pd.printingFont = (os == null ? pl.font : "Helvetica");
		pd.printGraphPosition = pl.position;

		// save original values

		boolean gridOn = pd.gridOn;
		boolean titleOn = pd.titleOn;
		boolean xScaleOn = pd.getBoolean(ScriptToken.XSCALEON);
		boolean xUnitsOn = pd.getBoolean(ScriptToken.XUNITSON);
		boolean yScaleOn = pd.getBoolean(ScriptToken.YSCALEON);
		boolean yUnitsOn = pd.getBoolean(ScriptToken.YUNITSON);

		pd.gridOn = pl.showGrid;
		pd.titleOn = pl.showTitle;
		pd.setBoolean(ScriptToken.XSCALEON, pl.showXScale);
		pd.setBoolean(ScriptToken.XUNITSON, pl.showXScale);
		pd.setBoolean(ScriptToken.YSCALEON, pl.showYScale);
		pd.setBoolean(ScriptToken.YUNITSON, pl.showYScale);

		/* Create a print job */

		PrinterJob pj = (os == null ? PrinterJob.getPrinterJob() : null);
		pd.printJobTitle = title;
		if (title.length() > 30)
			title = title.substring(0, 30);
		if (pj != null) {
			pj.setJobName(title);
			pj.setPrintable(this);
		}
		if (pj == null || pj.printDialog()) {
			try {
				if (pj == null) {
					createPdfDocument(os, pl);
				} else {
					PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
					aset.add(pl.layout.equals("landscape") ? OrientationRequested.LANDSCAPE
									: OrientationRequested.PORTRAIT);
					aset.add(pl.paper);
					pj.print(aset);
				}
			} catch (PrinterException ex) {
				String s = ex.getMessage();
				if (s == null)
					return;
				s = JSVTextFormat.simpleReplace(s, "not accepting job.",
						"not accepting jobs.");
				// not my fault -- Windows grammar error!
				showMessage(s, "Printer Error");
			}
		}

		// restore original values

		pd.gridOn = gridOn;
		pd.titleOn = titleOn;
		pd.setBoolean(ScriptToken.XSCALEON, xScaleOn);
		pd.setBoolean(ScriptToken.XUNITSON, xUnitsOn);
		pd.setBoolean(ScriptToken.YSCALEON, yScaleOn);
		pd.setBoolean(ScriptToken.YUNITSON, yUnitsOn);

	}


  /**
   * Implements method print in interface printable
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param pf
   *        the <code>PageFormat</code> object
   * @param pi
   *        the page index -- -1 for PDF creation
   * @return an int that depends on whether a print was successful
   * @throws PrinterException
   */
  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if (pi == 0) {
      Graphics2D g2D = (Graphics2D) g;
      pd.isPrinting = true;

      double height, width;
      boolean addFilePath = false;
      if (pd.printGraphPosition.equals("default")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = PanelData.defaultPrintHeight;
          width = PanelData.defaultPrintWidth;
        } else {
          height = PanelData.defaultPrintWidth;
          width = PanelData.defaultPrintHeight;
        }
      } else if (pd.printGraphPosition.equals("fit to page")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        addFilePath = true;
        height = pf.getImageableHeight();
        width = pf.getImageableWidth();
      } else { // center
        Paper paper = pf.getPaper();
        double paperHeight = paper.getHeight();
        double paperWidth = paper.getWidth();
        int x, y;

        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = PanelData.defaultPrintHeight;
          width = PanelData.defaultPrintWidth;
          x = (int) (paperWidth - width) / 2;
          y = (int) (paperHeight - height) / 2;
        } else {
          height = PanelData.defaultPrintWidth;
          width = PanelData.defaultPrintHeight;
          y = (int) (paperWidth - PanelData.defaultPrintWidth) / 2;
          x = (int) (paperHeight - PanelData.defaultPrintHeight) / 2;
        }
        g2D.translate(x, y);
      }

      g2D.scale(0.1, 0.1); // high resolution vector graphics for PDF
      pd.drawGraph(g2D, (int) width, (int) height, addFilePath);

      pd.isPrinting = false;
      return Printable.PAGE_EXISTS;
    }
    pd.isPrinting = false;
    return Printable.NO_SUCH_PAGE;
  }

  
  public void setupPlatform() {
    setBorder(BorderFactory.createLineBorder(Color.lightGray));
    if (popup == null) {
      // preferences dialog
      pd.coordStr = "(0,0)";
    } else {
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
    }
  }

  public String export(String type, int n) {
    if (type == null)
      type = "XY";
    if (n < -1 || pd.getNumberOfSpectraInCurrentSet() <= n)
      return "only " + pd.getNumberOfSpectraInCurrentSet()
          + " spectra available.";
    try {
      JDXSpectrum spec = (n < 0 ? pd.getSpectrum() : pd.getSpectrumAt(n));
      return Exporter.exportTheSpectrum(Exporter.ExportType.getType(type), null, spec, 0, spec.getXYCoords().length - 1);
    } catch (IOException ioe) {
      // not possible
    }
    return null;
  }
  
  @Override
  public String toString() {
    return pd.getSpectrumAt(0).toString();
  }

  public String getInput(String message, String title, String sval) {
    String ret = (String) JOptionPane.showInputDialog(this, message, title,
        JOptionPane.QUESTION_MESSAGE, null, null, sval);
    getFocusNow(true);
    return ret;
  }

	public void showMessage(String msg, String title) {
		JSVLogger.info(msg);
		JOptionPane.showMessageDialog(this, msg, title, (msg.startsWith("<html>") ? JOptionPane.INFORMATION_MESSAGE 
				: JOptionPane.PLAIN_MESSAGE));	
		getFocusNow(true);
	}

	public void showHeader(Object jsvApplet) {
		JDXSpectrum spectrum = pd.getSpectrum();
		String[][] rowData = spectrum.getHeaderRowDataAsArray();
		String[] columnNames = { "Label", "Description" };
		JTable table = new JTable(rowData, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(400, 195));
		JScrollPane scrollPane = new JScrollPane(table);
		JOptionPane.showMessageDialog((Container) jsvApplet, scrollPane, "Header Information",
				JOptionPane.PLAIN_MESSAGE);
		getFocusNow(true);
	}

	public AnnotationDialog showDialog(AType type) {
		AwtAnnotationDialog dialog = null; 
		AnnotationData ad = pd.getDialog(type);
		pd.closeAllDialogsExcept(type);
		if (ad != null && ad instanceof AwtAnnotationDialog) {
			((AwtAnnotationDialog) ad).reEnable();
			return (AnnotationDialog) ad;
		}
		
		int iSpec = pd.getCurrentSpectrumIndex();
		if (iSpec < 0) {
			showMessage("To enable " + type + " first select a spectrum by clicking on it.", "" + type);
			return null;
		}
		JDXSpectrum spec = pd.getSpectrum();
		switch (type) {
		case Integration:
			dialog = new AwtIntegralListDialog("Integration for " + spec, si, spec, this);
			break;
		case Measurements:
			dialog = new AwtMeasurementListDialog("Measurements for " + spec, si, spec, this);
			break;
		case PeakList:
			dialog = new AwtPeakListDialog("Peak List for " + spec, si, spec, this);
			break;
		case NONE:
		}
		if (ad != null)
			dialog.setData(ad);
		pd.addDialog(iSpec, type, dialog);
		dialog.reEnable();
		return dialog;
	}

	public void getFocusNow(boolean asThread) {
		if (asThread)
			SwingUtilities.invokeLater(new RequestThread());
		else
  		requestFocusInWindow();
    pd.dialogsToFront();
	}

  public class RequestThread implements Runnable {
		public void run() {
			requestFocusInWindow();
		}
  }

  /*--------------the rest are all mouse and keyboard interface -----------------------*/

  public void mousePressed(MouseEvent e) {
		if (pd.isPrinting)
			return;
		//System.out.println("mousePressed " + e);
    if (e.getButton() != MouseEvent.BUTTON1)
      return;
    pd.doMousePressed(e.getX(), e.getY());
  }

  private boolean isControlDown(InputEvent e) {
  	// Mac does not allow Ctrl-drag. The CMD key is indicated using code 157 
  	return pd.ctrlPressed |= e.isControlDown() || (e.getModifiers() & MAC_COMMAND ) == MAC_COMMAND;
	}

	public void mouseMoved(MouseEvent e) {
		if (pd.isPrinting)
			return;
    getFocusNow(false);
		if (e.getButton() != 0) {
			mouseDragged(e);
			return;
		}
    pd.doMouseMoved(e.getX(), e.getY());
    if (pd.coordStr != null)
      doRepaint();
  }

  public void mouseDragged(MouseEvent e) {
		if (pd.isPrinting)
			return;
    pd.doMouseDragged(e.getX(), e.getY());
    doRepaint();
  }
  
  public void mouseReleased(MouseEvent e) {
		if (pd.isPrinting)
			return;
    pd.doMouseReleased(e.getButton() == MouseEvent.BUTTON1);
    doRepaint();
  }

  public void mouseClicked(MouseEvent e) {
		if (pd.isPrinting)
			return;
		//System.out.println("mouseClicked " + e);
    if (e.getButton() == MouseEvent.BUTTON3) {
      popup.show((JSVPanel) this, e.getX(), e.getY());
      return;
    }
    pd.doMouseClicked(e.getX(), e.getY(), e.getClickCount(), e.isControlDown());
  }

  public void mouseEntered(MouseEvent e) {
    getFocusNow(false);
  }

  public void mouseExited(MouseEvent e) {
		pd.thisWidget = null;
		pd.isIntegralDrag = false;
		pd.integralShiftMode = 0;
  }

	public void keyPressed(KeyEvent e) {
		if (pd.isPrinting)
			return;
		checkControl(e, true);

		// should be only in panel region, though.
		switch (e.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE: // Mac
			pd.escapeKeyPressed(e.getKeyCode() != KeyEvent.VK_ESCAPE);
			pd.isIntegralDrag = false;
			doRepaint();
			e.consume();
			return;
		}
		int code = e.getKeyCode();
		double scaleFactor = 0;
		if (e.getModifiers() == 0) {
			switch (code) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
				pd.doMouseMoved(
						(code == KeyEvent.VK_RIGHT ? ++pd.mouseX : --pd.mouseX), pd.mouseY);
				e.consume();
				doRepaint();
				break;
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
				scaleFactor = (code == KeyEvent.VK_PAGE_UP ? GraphSet.RT2
						: 1 / GraphSet.RT2);
				e.consume();
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_UP:
				int dir = (code == KeyEvent.VK_DOWN ? -1 : 1);
				if (pd.getSpectrumAt(0).getSubSpectra() == null) {
					pd.notifySubSpectrumChange(dir, null);
				} else {
					pd.advanceSubSpectrum(dir);
					doRepaint();
				}
				e.consume();
				break;
			}
		} else if (e.isControlDown()) {
			switch (code) {
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_UP:
			case 45: // '-'
			case 61: // '=/+'
				scaleFactor = (code == 61 || code == KeyEvent.VK_UP ? GraphSet.RT2
						: 1 / GraphSet.RT2);
				e.consume();
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
				pd.toPeak(code == KeyEvent.VK_RIGHT ? 1 : -1);
				e.consume();
				break;
			}
		}
		if (scaleFactor != 0) {
			pd.scaleYBy(scaleFactor);
			doRepaint();
		}
	}

	public void keyReleased(KeyEvent e) {
		if (pd.isPrinting)
			return;
		checkControl(e, false);
  }

  private void checkControl(KeyEvent e, boolean isPressed) {
  	switch(e.getKeyCode()) {
  	case KeyEvent.VK_CONTROL:
  	case KeyEvent.VK_META:
    	pd.ctrlPressed = isPressed;
    	break;
  	case KeyEvent.VK_SHIFT:
  		pd.shiftPressed = isPressed;
  		break;
    default:
    	pd.ctrlPressed = isControlDown(e);
  		pd.shiftPressed = e.isShiftDown();
  	}
	}

  public void keyTyped(KeyEvent e) {
		if (pd.isPrinting)
			return;
  	if (e.getKeyChar() == 'n') {
  		pd.normalizeIntegral();
      doRepaint();
  		e.consume();
  		return;
  	}
    if (e.getKeyChar() == 'z') {
      pd.previousView();
      e.consume();
      return;
    }
    if (e.getKeyChar() == 'y') {
      pd.nextView();
      e.consume();
      return;
    }
  }


}
