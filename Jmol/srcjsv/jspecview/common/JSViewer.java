package jspecview.common;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import java.util.Map;

import javajs.util.CU;
import javajs.util.OC;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.SB;
import javajs.api.BytePoster;
import javajs.api.GenericFileInterface;
import javajs.api.GenericPlatform;
import javajs.api.PlatformViewer;
import javajs.awt.Dimension;

import org.jmol.api.GenericGraphics;
import org.jmol.util.Logger;

import javajs.util.PT;


import jspecview.api.ExportInterface;
import jspecview.api.JSVFileHelper;
import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.api.JSVPopupMenu;
import jspecview.api.JSVPrintDialog;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.ScriptInterface;
import jspecview.api.VisibleInterface;
import jspecview.common.Annotation.AType;
import jspecview.common.Spectrum.IRMode;
import jspecview.common.PanelData.LinkMode;
import jspecview.dialog.JSVDialog;
import jspecview.dialog.DialogManager;
import jspecview.exception.JSVException;
import jspecview.source.JDXReader;
import jspecview.source.JDXSource;
import jspecview.tree.SimpleTree;

/**
 * This class encapsulates all general functionality of applet and app. Most
 * methods include ScriptInterface parameter, which will be JSVAppletPrivate,
 * JSVAppletPrivatePro, or MainFrame.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class JSViewer implements PlatformViewer, BytePoster  {

	public final static String sourceLabel = "Original...";

	public final static int FILE_OPEN_OK = 0;
	public final static int FILE_OPEN_ALREADY = -1;
	// private final static int FILE_OPEN_URLERROR = -2;
	public final static int FILE_OPEN_ERROR = -3;
	public final static int FILE_OPEN_NO_DATA = -4;
	public static final int OVERLAY_DIALOG = -1;
	public static final int OVERLAY_OFFSET = 99;
	public static final int PORTRAIT = 1; // Printable
	public static final int PAGE_EXISTS = 0;
	public static final int NO_SUCH_PAGE = 1;

	private static String testScript = "<PeakData  index=\"1\" title=\"\" model=\"~1.1\" type=\"1HNMR\" xMin=\"3.2915\" xMax=\"3.2965\" atoms=\"15,16,17,18,19,20\" multiplicity=\"\" integral=\"1\"> src=\"JPECVIEW\" file=\"http://SIMULATION/$caffeine\"";

	private final static int NLEVEL_MAX = 100;

	public ScriptInterface si;
	public GenericGraphics g2d;
	public JSVTree spectraTree;
	public JDXSource currentSource;
	public Lst<PanelNode> panelNodes;
	public ColorParameters parameters;
	public RepaintManager repaintManager;
	public JSVPanel selectedPanel;
	public JSVMainPanel mainPanel;
	public Properties properties; // application only
	public Lst<String> scriptQueue;
	public JSVFileHelper fileHelper;
	public JSVPopupMenu jsvpPopupMenu;
	private DialogManager dialogManager;
	private JSVDialog viewDialog;
	private JSVDialog overlayLegendDialog;

	private IRMode irMode = IRMode.NO_CONVERT;

	public boolean loadImaginary;
	public boolean interfaceOverlaid;
	public boolean autoIntegrate;
	public boolean autoShowLegend;
	public Boolean obscureTitleFromUser;	
	public boolean allowCompoundMenu = true;
	public boolean allowMenu = true;
	public int initialStartIndex = -1;
	public int initialEndIndex = -1;

	public boolean isSingleThreaded;
	public boolean isApplet;
	public boolean isJS;
	public boolean isSigned;

	private String recentScript = "";

	public String appletName;
	public String fullName;
	public String syncID;
	public Object html5Applet; // will be an JavaScript object if this is JavaScript

	public Object display;
	
	private int maximumSize = Integer.MAX_VALUE;
	private final Dimension dimScreen = new Dimension(0,0);
	private int fileCount;
	private int nViews;
	private int scriptLevelCount;
	private String returnFromJmolModel;
	private String integrationRatios;

	public GenericPlatform apiPlatform;

	public void setProperty(String key, String value) {
		if (properties != null)
			properties.setProperty(key, value);
	}

	public void setNode(PanelNode node) {
		if (node.jsvp != selectedPanel)
			si.siSetSelectedPanel(node.jsvp);
		si.siSendPanelChange();
		si.siNodeSet(node);
	}

	/**
	 * @param si
	 * @param isApplet
	 * @param isJS
	 */
	public JSViewer(ScriptInterface si, boolean isApplet, boolean isJS) {
		this.si = si;
		this.isApplet = isApplet;
		this.isJS = isApplet && isJS;
		this.isSigned = si.isSigned();
		apiPlatform = (GenericPlatform) getPlatformInterface("Platform");
		apiPlatform.setViewer(this, this.display);
		g2d = (GenericGraphics) getPlatformInterface("G2D");
		spectraTree = new SimpleTree(this);
		parameters = (ColorParameters) getPlatformInterface("Parameters");
		parameters.setName("applet");
		fileHelper = ((JSVFileHelper) getPlatformInterface("FileHelper")).set(this);
		isSingleThreaded = apiPlatform.isSingleThreaded();
		panelNodes = new Lst<PanelNode>();
		repaintManager = new RepaintManager(this);
		if (!isApplet)
			setPopupMenu(true, true);
	}

	private boolean popupAllowMenu = true;
	private boolean popupZoomEnabled = true;

	private String defaultLoadScript;

	public float nmrMaxY = Float.NaN;

	public void setPopupMenu(boolean allowMenu, boolean zoomEnabled) {
		popupAllowMenu = allowMenu;
		popupZoomEnabled = zoomEnabled;		
	}

	public void showMenu(int x, int y) {
		if (!popupAllowMenu)
			return;
		if (jsvpPopupMenu == null) {
			try {
				jsvpPopupMenu = (JSVPopupMenu) getPlatformInterface("Popup");
				jsvpPopupMenu.jpiInitialize(this, isApplet ? "appletMenu" : "appMenu");
				jsvpPopupMenu.setEnabled(popupAllowMenu, popupZoomEnabled);
			} catch (Exception e) {
				Logger.error(e + " initializing popup menu");
				return;
			}
		}
		jsvpPopupMenu.jpiShow(x, y);
	}

	public boolean runScriptNow(String script) {
		scriptLevelCount++;
		if (script == null)
			script = "";
		script = script.trim();
		if (script.startsWith("!"))
			script = script.substring(1).trim();
		else if (script.startsWith(">")) {
			Logger.error(script);
			return true;
		}
		if (script.indexOf("<PeakData") >= 0) {
			syncScript(script);
			return true;
		}
		Logger.info("RUNSCRIPT " + script);
		boolean isOK = true;
		int nErrorsLeft = 10;
		ScriptTokenizer commandTokens = new ScriptTokenizer(script, true);
		String msg = null;
		while (commandTokens != null && commandTokens.hasMoreTokens() && nErrorsLeft > 0 && isOK) {
			String token = commandTokens.nextToken();
			// now split the key/value pair

			ScriptTokenizer eachParam = new ScriptTokenizer(token, false);
			String key = ScriptToken.getKey(eachParam);
			if (key == null)
				continue;
			ScriptToken st = ScriptToken.getScriptToken(key);
			String value = ScriptToken.getValue(st, eachParam, token);
			//System.out.println("KEY-> " + key + " VALUE-> " + value + " : " + st);
			try {
				switch (st) {
				case UNKNOWN:
					Logger.info("Unrecognized parameter: " + key);
					--nErrorsLeft;
					break;
				default:
					if (selectedPanel == null)
						break;// probably a startup option for the applet
					parameters.set(pd(), st, value);
					si.siUpdateBoolean(st, Parameters.isTrue(value));
					break;
				case PEAKCALLBACKFUNCTIONNAME:
				case SYNCCALLBACKFUNCTIONNAME:
				case COORDCALLBACKFUNCTIONNAME:
				case LOADFILECALLBACKFUNCTIONNAME:
					si.siExecSetCallback(st, value);
					break;
				case DEFAULTLOADSCRIPT:
					value = PT.rep(value, "''", "\"");
					defaultLoadScript = (value.length() > 0 ? value : null);
					break;
				case DEFAULTNMRNORMALIZATION:
					nmrMaxY = PT.parseFloat(value);
					break;
				case AUTOINTEGRATE:
					autoIntegrate = Parameters.isTrue(value);
					break;
				case CLOSE:
					execClose(value);
					break;
				case DEBUG:
					Logger
							.setLogLevel(value.toLowerCase().equals("high") ? Logger.LEVEL_DEBUGHIGH
									: Parameters.isTrue(value) ? Logger.LEVEL_DEBUG
											: Logger.LEVEL_INFO);
					break;
				case GETPROPERTY:
					Map<String, Object> info = (selectedPanel == null ? null
							: getPropertyAsJavaObject(value));
					if (info != null)
						selectedPanel
								.showMessage(PT.toJSON(null, info), value);
					break;
				case HIDDEN:
					si.siExecHidden(Parameters.isTrue(value));
					break;
				case INTEGRATIONRATIOS:
					integrationRatios = value;
					execIntegrate(null);
					break;
				case INTERFACE:
					interfaceOverlaid = checkOvelayInterface(value);
					break;
				case INTEGRALOFFSET:
				case INTEGRALRANGE:
					execSetIntegralParameter(st, Double.parseDouble(value));
					break;
				case JMOL:
					si.syncToJmol(value);
					break;
				case JSV:
					syncScript(PT.trimQuotes(value));
					break;
				case LOAD:
					if (value.length() == 0) {
						if (defaultLoadScript != null)
							runScriptNow(defaultLoadScript);
					  break;
					}
					load(value, (defaultLoadScript == null ? "" : defaultLoadScript + ";") + commandTokens.getRemainingScript());
					msg = (selectedPanel == null ? null : si.siLoaded(value));
					commandTokens = null;
					break;
				case LOADIMAGINARY:
					loadImaginary = Parameters.isTrue(value);
					break;
				case PEAK:
					execPeak(value);
					break;
				case PEAKLIST:
					execPeakList(value);
					break;
				case SCALEBY:
					scaleSelectedBy(panelNodes, value);
					break;
				case SCRIPT:
					if (value.equals("") || value.toLowerCase().startsWith("inline")) {
						execScriptInline(value);
					} else {
						String s = JSVFileManager.getFileAsString(value);
						if (s != null && scriptLevelCount < NLEVEL_MAX)
							runScriptNow(s);
					}
					break;
				case SELECT:
					execSelect(value);
					break;
				case SPECTRUM:
				case SPECTRUMNUMBER:
					if (!setSpectrum(value))
						isOK = false;
					break;
				case STACKOFFSETY:
					execOverlayOffsetY(PT.parseInt("" + PT.parseFloat(value)));
					break;
				case TEST:
					si.siExecTest(value);
					break;
				case OVERLAY: // deprecated
				case VIEW:
					execView(value, true);
					break;
				case HIGHLIGHT:
					isOK = highlight(token);
					break;
				case FINDX:
				case GETSOLUTIONCOLOR:
				case INTEGRATION:
				case INTEGRATE:
				case IRMODE:
				case LABEL:
				case LINK:
				case OVERLAYSTACKED:
				case PRINT:
				case SETPEAK:
				case SETX:
				case SHIFTX:
				case SHOWERRORS:
				case SHOWMEASUREMENTS:
				case SHOWMENU:
				case SHOWKEY:
				case SHOWPEAKLIST:
				case SHOWINTEGRATION:
				case SHOWPROPERTIES:
				case SHOWSOURCE:
				case YSCALE:
				case WRITE:
				case ZOOM:
					if (isClosed()) {
						isOK = false;
						break;
					}
					switch (st) {
					default:
						break;
					case FINDX:
						pd().findX(null, Double.parseDouble(value));
						break;
					case GETSOLUTIONCOLOR:
						show("solutioncolor" + value.toLowerCase());
						break;
					case INTEGRATION:
					case INTEGRATE:
						execIntegrate(value);
						break;
					case IRMODE:
						execIRMode(value);
						break;
					case LABEL:
						pd().addAnnotation(ScriptToken.getTokens(value));
						break;
					case LINK:
						pd().linkSpectra(LinkMode.getMode(value));
						break;
					case OVERLAYSTACKED:
						pd().splitStack(!Parameters.isTrue(value));
						break;
					case PRINT:
						msg = execWrite(null);
						break;
					case SETPEAK:
						// setpeak NONE Double.NaN, Double.MAX_VALUE
						// shiftx NONE Double.MAX_VALUE, Double.NaN
						// setpeak x.x Double.NaN, value
						// setx x.x Double.MIN_VALUE, value
						// shiftx x.x value, Double.NaN
						// setpeak ? Double.NaN, Double.MIN_VALUE
						pd().shiftSpectrum(
								Double.NaN,
								value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE : value
										.equalsIgnoreCase("?") ? Double.MIN_VALUE : Double
										.parseDouble(value));
						break;
					case SETX:
						pd().shiftSpectrum(Double.MIN_VALUE, Double.parseDouble(value));
						break;
					case SHIFTX:
						pd().shiftSpectrum(
								value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE : Double
										.parseDouble(value), Double.NaN);
						break;
					case SHOWERRORS:
						show("errors");
						break;
					case SHOWINTEGRATION:
						pd().showAnnotation(AType.Integration,
								Parameters.getTFToggle(value));
						// execIntegrate(null);
						break;
					case SHOWKEY:
						setOverlayLegendVisibility(Parameters.getTFToggle(value), true);
						break;
					case SHOWMEASUREMENTS:
						pd().showAnnotation(AType.Measurements,
								Parameters.getTFToggle(value));
						break;
					case SHOWMENU:
						showMenu(Integer.MIN_VALUE, 0);
						break;
					case SHOWPEAKLIST:
						pd().showAnnotation(AType.PeakList, Parameters.getTFToggle(value));
						break;
					case SHOWPROPERTIES:
						show("properties");
						break;
					case SHOWSOURCE:
						show("source");
						break;
					case YSCALE:
						setYScale(value);
						break;
					case WINDOW:
						si.siNewWindow(Parameters.isTrue(value), false);
						break;
					case WRITE:
						msg = execWrite(value);
						break;
					case ZOOM:
						isOK = execZoom(value);
						break;
					}
					break;
				}
			} catch (Exception e) {
				msg = e.toString();
				Logger.error(e.toString());
				isOK = false;
				--nErrorsLeft;
			}
		}
		scriptLevelCount--;
		si.siExecScriptComplete(msg, true);
		return isOK;
	}

	private void execClose(String value) {
		boolean fromScript = (!value.startsWith("!"));
		if (!fromScript)
			value = value.substring(1);
		close(PT.trimQuotes(value));
		if (!fromScript || panelNodes.size() == 0)
			si.siValidateAndRepaint(true);
	}

	public boolean checkOvelayInterface(String value) {
		return (value.equalsIgnoreCase("single") || value.equalsIgnoreCase("overlay"));
	}

	private void execPeak(String value) {
		try {
			Lst<String> tokens = ScriptToken.getTokens(value);
			value = " type=\"" + tokens.get(0).toUpperCase() + "\" _match=\""
					+ PT.trimQuotes(tokens.get(1).toUpperCase()) + "\"";
			if (tokens.size() > 2 && tokens.get(2).equalsIgnoreCase("all"))
				value += " title=\"ALL\"";
			processPeakPickEvent(new PeakInfo(value), false); // false == true here
		} catch (Exception e) {
			// ignore
		}
	}

	private void execPeakList(String value) {
		ColorParameters p = parameters;
		Boolean b = Parameters.getTFToggle(value);
		if (value.indexOf("=") < 0) {
			if (!isClosed())
				pd().getPeakListing(null, b);
		} else {
			Lst<String> tokens = ScriptToken.getTokens(value);
			for (int i = tokens.size(); --i >= 0;) {
				String token = tokens.get(i);
				int pt = token.indexOf("=");
				if (pt <= 0)
					continue;
				String key = token.substring(0, pt);
				value = token.substring(pt + 1);
				try {
					if (key.startsWith("thr")) {
						p.peakListThreshold = Double.valueOf(value).doubleValue();
					} else if (key.startsWith("int")) {
						p.peakListInterpolation = (value.equalsIgnoreCase("none") ? "NONE"
								: "parabolic");
					}
					if (!isClosed())
						pd().getPeakListing(p, Boolean.TRUE);
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	private boolean highlight(String value) {
		Lst<String> tokens = ScriptToken.getTokens(value);
		int n = tokens.size();
		switch (n) {
		case 3:
		case 5:
		case 6:
		case 7:
			break;
		case 2:
		case 4:
			if (tokens.get(n - 1).equalsIgnoreCase("OFF"))
				break;
			//$FALL-THROUGH$
		default:
			return false;
		}
		if (!isClosed()) {
			float x1 = PT.parseFloat(n > 1 ? tokens.get(1) : "");
			float x2 = PT.parseFloat(n > 2 ? tokens.get(2) : "");
			int r = getRGB(n > 3 ? tokens.get(3) : "100");
			int g = getRGB(n > 4 ? tokens.get(4) : "100");
			int b = getRGB(n > 5 ? tokens.get(5) : "100");
			int a = getRGB(n > 6 ? tokens.get(6) : "100");
			if (Float.isNaN(x1) || Float.isNaN(x2)) {
				pd().removeAllHighlights();
			} else {
				pd().removeHighlight(x1, x2);
				if (a < 0)
					a = 150;
				if (r >= 0 && g >= 0 && b >= 0)
					pd().addHighlight(null, x1, x2, null, r, g, b, a);
			}
			repaint(true);
		}
		return true;
	}

  private int getRGB(String s) {
		float f = PT.parseFloat(s);
		return (int) (Float.isNaN(f) ? -1 : f > 1 ? f : f * 255);
	}

	private boolean execZoom(String value) {
		double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		value = PT.rep(value, " - ", " ").replace(',',' ');
		Lst<String> tokens = ScriptToken.getTokens(value);
		switch (tokens.size()) {
		default:
			return false;
		case 0:
			ScaleData v = pd().getCurrentGraphSet().getCurrentView();
			value = Math.round(v.minXOnScale * 100) / 100f + "," + Math.round(v.maxXOnScale * 100) / 100f;
			value = selectedPanel.getInput("Enter zoom range x1 x2", "Zoom", value);
			return (value == null || execZoom(value));
		case 1:
			value = tokens.get(0);
			if (value.equalsIgnoreCase("next")) {
				pd().nextView();
			} else if (value.toLowerCase().startsWith("prev")) {
				pd().previousView();
			} else if (value.equalsIgnoreCase("out")) {
				pd().resetView();
			} else if (value.equalsIgnoreCase("clear")) {
				pd().clearAllView();
			}
			return true;
		case 2:
			x1 = Double.parseDouble(tokens.get(0));
			x2 = Double.parseDouble(tokens.get(1));
			break;
		case 4:
			x1 = Double.parseDouble(tokens.get(0));
			y1 = Double.parseDouble(tokens.get(1));
			x2 = Double.parseDouble(tokens.get(2));
			y2 = Double.parseDouble(tokens.get(3));
		}
		pd().setZoom(x1, y1, x2, y2);
		return true;
	}

	private void scaleSelectedBy(Lst<PanelNode> nodes, String value) {
		try {
			double f = Double.parseDouble(value);
			for (int i = nodes.size(); --i >= 0;)
				nodes.get(i).pd().scaleSelectedBy(f);
		} catch (Exception e) {
		}
	}

	public PanelData pd() {
		return (selectedPanel == null ? null : selectedPanel.getPanelData());
	}

	private boolean isClosed() {
		return (pd() == null);
	}
	
	private void execSelect(String value) {
		if (value.startsWith("ID ")) {
			if (!isClosed())
				try {
				  pd().selectSpectrum(null, "ID",
							PT.trimQuotes(value.substring(3)), true);
				} catch (Exception e) {
					//
				}
			return;
		}
		Lst<PanelNode> nodes = panelNodes;
		for (int i = nodes.size(); --i >= 0;)
			nodes.get(i).pd().selectFromEntireSet(Integer.MIN_VALUE);
		Lst<Spectrum> speclist = new Lst<Spectrum>();
		fillSpecList(value, speclist, false);
		// not sure where this is going...
	}

	public void execView(String value, boolean fromScript) {
		if (value.equals("")) {
			checkOverlay();
			return;
		}
		Lst<Spectrum> speclist = new Lst<Spectrum>();
		String strlist = fillSpecList(value, speclist, true);
		if (speclist.size() > 0)
			si.siOpenDataOrFile(null, strlist, speclist, strlist, -1, -1, false, null, null);
		if (!fromScript) {
			si.siValidateAndRepaint(false);
		}
	}

	private void execIRMode(String value) {
		IRMode mode = IRMode.getMode(value); // T, A, or TOGGLE
		String type = pd().getSpectrum().dataType;
		for (int i = panelNodes.size(); --i >= 0;)
			panelNodes.get(i).pd().setIRMode(mode, type);
		setIRmode(value);
		// jsvp.doRepaint();
	}

	private void execIntegrate(String value) {
		if (isClosed())
			return;
		pd().checkIntegral(parameters, value);
		if (integrationRatios != null)
			pd().setIntegrationRatios(integrationRatios);
		integrationRatios = null; // one time only
		repaint(true);
	}

	private void repaint(boolean andTaintAll) {
		selectedPanel.doRepaint(andTaintAll);
	}

	@SuppressWarnings("incomplete-switch")
	private void execSetIntegralParameter(ScriptToken st, double value) {
		ColorParameters p = parameters;
		switch (st) {
		case INTEGRALRANGE:
			p.integralRange = value;
			break;
		case INTEGRALOFFSET:
			p.integralOffset = value;
			break;
		}
		if (!isClosed())
			pd().checkIntegral(parameters, "update");
	}

	private void setYScale(String value) {
		Lst<String> tokens = ScriptToken.getTokens(value);
		int pt = 0;
		boolean isAll = false;
		if (tokens.size() > 1 && tokens.get(0).equalsIgnoreCase("ALL")) {
			isAll = true;
			pt++;
		}
		double y1 = Double.parseDouble(tokens.get(pt++));
		double y2 = Double.parseDouble(tokens.get(pt));
		if (isAll) {
			Spectrum spec = pd().getSpectrum();
			for (int i = panelNodes.size(); --i >= 0;) {
				PanelNode node = panelNodes.get(i);
				if (node.source != currentSource)
					continue;
				if (Spectrum.areXScalesCompatible(spec, node.getSpectrum(), false,
						false))
					node.pd().setZoom(0, y1, 0, y2);
			}
		} else {
			pd().setZoom(0, y1, 0, y2);
		}
	}

	private boolean overlayLegendVisible;

	private void setOverlayLegendVisibility(Boolean tftoggle,
			boolean doSet) {
		if (doSet)
			overlayLegendVisible = (tftoggle == null ? !overlayLegendVisible
					: tftoggle == Boolean.TRUE);
		PanelNode node = PanelNode.findNode(selectedPanel, panelNodes);
		for (int i = panelNodes.size(); --i >= 0;)
			showOverlayLegend(panelNodes.get(i), panelNodes.get(i) == node
					&& overlayLegendVisible);
	}

	private void showOverlayLegend(PanelNode node, boolean visible) {
		JSVDialog legend = node.legend;
		if (legend == null && visible) {
			legend = node.setLegend(node.pd().getNumberOfSpectraInCurrentSet() > 1
					&& node.pd().getNumberOfGraphSets() == 1 ? getDialog(
					AType.OverlayLegend, null) : null);
		}
		if (legend != null)
			legend.setVisible(visible);
	}

	// / from JavaScript

	/**
	 * incoming script processing of <PeakAssignment file="" type="xxx"...> record
	 * from Jmol
	 * 
	 * @param peakScript
	 */

	public void syncScript(String peakScript) {
		if (peakScript.equals("TEST"))
			peakScript = testScript;
		Logger.info("JSViewer.syncScript Jmol>JSV " + peakScript);
		if (peakScript.indexOf("<PeakData") < 0) {
					if (peakScript.startsWith("JSVSTR:")) {
						si.syncToJmol(peakScript);
						return;
					}
			runScriptNow(peakScript);
			if (peakScript.indexOf("#SYNC_PEAKS") >= 0)
			  syncPeaksAfterSyncScript();
			return;
		}
		Logger.info(">>toJSV>> " + peakScript);
		String sourceID = PT.getQuotedAttribute(peakScript, "sourceID");
		String type, model, file, jmolSource, index, atomKey;
		if (sourceID == null) {
			// todo: why the quotes??
			//peakScript = PT.rep(peakScript, "\\\"", "");
			file = PT.getQuotedAttribute(peakScript, "file");
			index = PT.getQuotedAttribute(peakScript, "index");
			if (file == null || index == null)
				return;
			file = PT.rep(file, "#molfile", "");// Jmol has loaded the model from the cache
			model = PT.getQuotedAttribute(peakScript, "model");
			jmolSource = PT.getQuotedAttribute(peakScript, "src");
			String modelSent = (jmolSource != null && jmolSource.startsWith("Jmol") ? null
					: returnFromJmolModel);
			if (model != null && modelSent != null && !model.equals(modelSent)) {
				Logger.info("JSV ignoring model " + model + "; should be " + modelSent);
				return;
			}
			returnFromJmolModel = null;
			if (panelNodes.size() == 0 || !checkFileAlreadyLoaded(file)) {
				Logger.info("file " + file
						+ " not found -- JSViewer closing all and reopening");
				si.siSyncLoad(file);
			}
			type = PT.getQuotedAttribute(peakScript, "type");
			atomKey = null;
		} else {
			file = null;
			index = model = sourceID;
			atomKey = "," + PT.getQuotedAttribute(peakScript, "atom") + ",";
			type = "ID";
			jmolSource = sourceID; //??
		}
		
		PeakInfo pi = selectPanelByPeak(file, index, atomKey);
		PanelData pd = pd();
		pd.selectSpectrum(file, type, model, true);
		si.siSendPanelChange();
		pd.addPeakHighlight(pi);
		repaint(true);
		// round trip this so that Jmol highlights all equivalent atoms
		// and appropriately starts or clears vibration
		if (jmolSource == null || (pi != null && pi.getAtoms() != null))
			si.syncToJmol(jmolSelect(pi));
	}

	private void syncPeaksAfterSyncScript() {
		JDXSource source = currentSource;
		if (source == null)
			return;
		try {
			String file = "file=" + PT.esc(source.getFilePath());
			Lst<PeakInfo> peaks = source.getSpectra().get(0).getPeakList();
			SB sb = new SB();
			sb.append("[");
			int n = peaks.size();
			for (int i = 0; i < n; i++) {
				String s = peaks.get(i).toString();
				s = s + " " + file;
				sb.append(PT.esc(s));
				if (i > 0)
					sb.append(",");
			}
			sb.append("]");
			si.syncToJmol("Peaks: " + sb);
		} catch (Exception e) {
			// ignore bad structures -- no spectrum
		}
	}

	private boolean checkFileAlreadyLoaded(String fileName) {
		if (isClosed())
			return false;
		if (pd().hasFileLoaded(fileName))
			return true;
		for (int i = panelNodes.size(); --i >= 0;)
			if (panelNodes.get(i).pd().hasFileLoaded(fileName)) {
				si.siSetSelectedPanel(panelNodes.get(i).jsvp);
				return true;
			}
		return false;
	}

	/**
	 * @param file 
	 * @param index 
	 * @param atomKey 
	 * @return  PeakInfo entry if appropriate
	 */
	private PeakInfo selectPanelByPeak(String file, String index, String atomKey) {
		if (panelNodes == null)
			return null;
		PeakInfo pi = null;
		for (int i = panelNodes.size(); --i >= 0;)
			panelNodes.get(i).pd().addPeakHighlight(null);
		pi = pd().selectPeakByFileIndex(file, index, atomKey);
		if (pi != null) {
			// found in current panel
			setNode(PanelNode.findNode(selectedPanel, panelNodes));
		} else {
			// must look elsewhere
			for (int i = panelNodes.size(); --i >= 0;) {
				PanelNode node = panelNodes.get(i);
				if ((pi = node.pd().selectPeakByFileIndex(file, index, atomKey)) != null) {
					setNode(node);
					break;
				}
			}
		}
		return pi;
	}

	/**
	 * this method is called as a result of the user clicking on a peak
	 * (eventObject instanceof PeakPickEvent) or from PEAK command execution
	 * 
	 * @param eventObj
	 * @param isApp
	 */
	public void processPeakPickEvent(Object eventObj, boolean isApp) {
		// trouble here is with round trip when peaks are clicked in rapid
		// succession.

		PeakInfo pi;
		if (eventObj instanceof PeakInfo) {
			// this is a call from the PEAK command, above.
			pi = (PeakInfo) eventObj;
			PeakInfo pi2 = pd().findMatchingPeakInfo(pi);
			if (pi2 == null) {
				if (!"ALL".equals(pi.getTitle()))
					return;
				PanelNode node = null;
				for (int i = 0; i < panelNodes.size(); i++)
					if ((pi2 = panelNodes.get(i).pd()
							.findMatchingPeakInfo(pi)) != null) {
						node = panelNodes.get(i);
						break;
					}
				if (node == null)
					return;
				setNode(node);
			}
			pi = pi2;
		} else {
			PeakPickEvent e = ((PeakPickEvent) eventObj);
			si.siSetSelectedPanel((JSVPanel) e.getSource());
			pi = e.getPeakInfo();
		}
		pd().addPeakHighlight(pi);
		// the above line is what caused problems with GC/MS selection
		syncToJmol(pi);
		// System.out.println(Thread.currentThread() +
		// "processPeakEvent --selectSpectrum " + pi);
		if (pi.isClearAll()) // was not in app version??
			repaint(false);
		else
			pd().selectSpectrum(pi.getFilePath(), pi.getType(), pi.getModel(), true);
		si.siCheckCallbacks(pi.getTitle());
	}

	void newStructToJmol(String data) {
	  Logger.info("sending new structure to Jmol:\n" + data);
		si.syncToJmol("struct:" + data);
	}
	
	private void syncToJmol(PeakInfo pi) {
		repaint(true);
		returnFromJmolModel = pi.getModel();
		si.syncToJmol(jmolSelect(pi));
	}

	public void sendPanelChange() {
		PanelData pd = pd();
		Spectrum spec = pd.getSpectrum();
		PeakInfo pi = spec.getSelectedPeak();
		if (pi == null)
			pi = spec.getModelPeakInfoForAutoSelectOnLoad();
		if (pi == null)
			pi = spec.getBasePeakInfo();
		pd.addPeakHighlight(pi);
		Logger.info(Thread.currentThread() + "JSViewer sendFrameChange " + selectedPanel);
		syncToJmol(pi);
	}

	private String jmolSelect(PeakInfo pi) {
		String script = ("IR".equals(pi.getType()) || "RAMAN".equals(pi.getType()) 
				? "vibration ON; selectionHalos OFF;"
				: "vibration OFF; selectionhalos "
						+ (pi.getAtoms() == null ? "OFF" : "ON"));
		return "Select: " + pi + " script=\"" + script + " \" sourceID=\"" + pd().getSpectrum().sourceID + "\"";
	}

	public Map<String, Object> getPropertyAsJavaObject(String key) {

		Map<String, Object> map = new Hashtable<String, Object>();

		if ("SOURCEID".equalsIgnoreCase(key)) {
			// get current spectrum ID
			map.put(key, (pd() == null ? "" : pd().getSpectrum().sourceID));
			return map;
		}
		if (key != null && key.startsWith("DATA_")) {
		  // mol, json, xml, jcamp -- most recent only
			map.put(key, "" + JSVFileManager.cacheGet(key.substring(5)));
			return map;
		}

		boolean isAll = false;
		if (key != null && key.toUpperCase().startsWith("ALL ")
				|| "all".equalsIgnoreCase(key)) {
			key = key.substring(3).trim();
			isAll = true;
		}
		if ("".equals(key))
			key = null;

		Map<String, Object> map0 = pd().getInfo(true, key);
		if (!isAll && map0 != null)
			return map0;
		if (map0 != null)
			map.put("current", map0);
		Lst<Map<String, Object>> info = new Lst<Map<String, Object>>();
		for (int i = 0; i < panelNodes.size(); i++) {
			JSVPanel jsvp = panelNodes.get(i).jsvp;
			if (jsvp == null)
				continue;
			info.addLast(panelNodes.get(i).getInfo(key));
		}
		map.put("items", info);
		return map;
	}

	public String getCoordinate() {
		if (!isClosed()) {
			Coordinate coord = pd().getClickedCoordinate();
			if (coord != null)
				return coord.getXVal() + " " + coord.getYVal();
		}
		return "";
	}

	/**
	 * originally in MainFrame, this method takes the OVERLAY command option and
	 * converts it to a list of spectra
	 * 
	 * @param value
	 * @param speclist
	 * @param isView
	 * @return comma-separated list, for the title
	 */
	private String fillSpecList(String value, Lst<Spectrum> speclist,
			boolean isView) {

		String prefix = "1.";
		Lst<String> list;
		Lst<String> list0 = null;
		boolean isNone = (value.equalsIgnoreCase("NONE"));
		if (isNone || value.equalsIgnoreCase("all"))
			value = "*";
		if (value.indexOf("*") < 0) {
			// replace "3.1.1" with "3.1*1"
			String[] tokens = value.split(" ");
			SB sb = new SB();
			for (int i = 0; i < tokens.length; i++) {
				int pt = tokens[i].indexOf('.');
				if (pt != tokens[i].lastIndexOf('.'))
					tokens[i] = tokens[i].substring(0, pt + 1)
							+ tokens[i].substring(pt + 1).replace('.', '_');
				sb.append(tokens[i]).append(" ");
			}
			value = sb.toString().trim();
		}
		if (value.equals("*")) {
			list = ScriptToken.getTokens(PanelNode
					.getSpectrumListAsString(panelNodes));
		} else if (value.startsWith("\"") || value.startsWith("'")) {
			list = ScriptToken.getTokens(value);
		} else {
			value = PT.rep(value, "_", " _ ");
			value = PT.rep(value, "-", " - ");
			list = ScriptToken.getTokens(value);
			list0 = ScriptToken.getTokens(PanelNode
					.getSpectrumListAsString(panelNodes));
			if (list0.size() == 0)
				return null;
		}

		String id0 = (isClosed() ? prefix : PanelNode.findNode(
				selectedPanel, panelNodes).id);
		id0 = id0.substring(0, id0.indexOf(".") + 1);
		SB sb = new SB();
		int n = list.size();
		String idLast = null;
		for (int i = 0; i < n; i++) {
			String id = list.get(i);
			double userYFactor = Double.NaN;
			int isubspec = -1;
			if (i + 1 < n && list.get(i + 1).equals("*")) {
				i += 2;
				userYFactor = Double.parseDouble(list.get(i));
			} else if (i + 1 < n && list.get(i + 1).equals("_")) {
				i += 2;
				isubspec = Integer.parseInt(list.get(i));
			}
			if (id.equals("-")) {
				if (idLast == null)
					idLast = list0.get(0);
				id = (i + 1 == n ? list0.get(list0.size() - 1) : list.get(++i));
				if (!id.contains("."))
					id = id0 + id;
				int pt = 0;
				while (pt < list0.size() && !list0.get(pt).equals(idLast))
					pt++;
				pt++;
				while (pt < list0.size() && !idLast.equals(id)) {
					PanelNode node = PanelNode.findNodeById((idLast = list0.get(pt++)), panelNodes);
					speclist.addLast(node.pd().getSpectrumAt(0));
					sb.append(",").append(idLast);
				}
				continue;
			}
			PanelNode node;
			if (id.startsWith("'") && id.endsWith("'"))
				id = "\"" + PT.trim(id, "'") + "\"";
			if (id.startsWith("\"")) {
				id = PT.trim(id, "\"");
				int pn = panelNodes.size();
				for (int j = 0; j < pn; j++) {
					node = panelNodes.get(j);
					if (node.fileName != null && node.fileName.startsWith(id)
							|| node.frameTitle != null && node.frameTitle.startsWith(id)) {
						addSpecToList(node.pd(), userYFactor, -1, speclist,
								isView);
						sb.append(",").append(node.id);
					}
				}
				continue;
			}
			if (!id.contains("."))
				id = id0 + id;
			node = PanelNode.findNodeById(id, panelNodes);
			if (node == null)
				continue;
			idLast = id;
			addSpecToList(node.pd(), userYFactor, isubspec, speclist,
					isView);
			sb.append(",").append(id);
			if (isubspec > 0)
				sb.append(".").appendI(isubspec);
		}
		if (isView && speclist.size() > 0) {
			PanelNode node = PanelNode.findNodeById(sb.substring(1), panelNodes);
			if (node != null) {
				setNode(node); // was "fromTree true"
				// possibility of a problem here -- we are not communicating with Jmol
				// our model changes.
				speclist.clear();
			}
		}
		return (isNone ? "NONE" : sb.length() > 0 ? sb.toString().substring(1)
				: null);
	}

	private void addSpecToList(PanelData pd, double userYFactor, int isubspec,
			Lst<Spectrum> list, boolean isView) {
		if (isView) {
			Spectrum spec = pd.getSpectrumAt(0);
			spec.setUserYFactor(Double.isNaN(userYFactor) ? 1 : userYFactor);
			pd.addToList(isubspec - 1, list);
		} else {
			pd.selectFromEntireSet(isubspec - 1);
		}
	}

	public int getSolutionColor(boolean asFitted) {
		Spectrum spectrum = pd().getSpectrum();
		VisibleInterface vi = (spectrum.canShowSolutionColor() ? (VisibleInterface) JSViewer
				.getInterface("jspecview.common.Visible") : null);
		return (vi == null ? -1 : vi.getColour(spectrum, asFitted));
	}

	public int openDataOrFile(Object data, String name, Lst<Spectrum> specs,
			String strUrl, int firstSpec, int lastSpec, boolean isAppend, String id) {
		if ("NONE".equals(name)) {
			close("View*");
			return FILE_OPEN_OK;
		}
		si.writeStatus("");
		String filePath = null;
		String newPath = null;
		String fileName = null;
		boolean isView = false;
		if (strUrl != null && strUrl.startsWith("cache://")) {
			/**
			 * @j2sNative
			 * 
			 *            data = Jmol.Cache.get(name = strUrl);
			 * 
			 */
			{
			}
		}
		if (data != null) {
			try {
				fileName = name;
				newPath = filePath = JSVFileManager.getFullPathName(name);
			} catch (JSVException e) {
				// ok...
			}
		} else if (specs != null) {
			isView = true;
			newPath = fileName = filePath = "View" + (++nViews);
		} else if (strUrl != null) {
			try {
				// System.out.println("strURL=" + strUrl);
				// System.out.println("JSVFileManager.appletDocumentBase=" +
				// JSVFileManager.appletDocumentBase);
				URL u = new URL(JSVFileManager.appletDocumentBase, strUrl, null);
				// System.out.println("u=" + u);
				filePath = u.toString();
				recentURL = filePath;
				fileName = JSVFileManager.getTagName(filePath);
				// System.out.println("fileName=" + fileName);
			} catch (MalformedURLException e) {
				GenericFileInterface file = apiPlatform.newFile(strUrl);
				fileName = file.getName();
				newPath = filePath = file.getFullPath();
				recentURL = null;
			}
		}
		// if (!isView)
		int pt = -1;
		if ((pt = PanelNode.isOpen(panelNodes, filePath)) >= 0
				|| (pt = PanelNode.isOpen(panelNodes, strUrl)) >= 0) {
			if (isView) {
				--nViews;
				setNode(panelNodes.get(pt)); // was fromTree true
			} else {
				si.writeStatus(filePath + " is already open");
			}
			return FILE_OPEN_ALREADY;
		}
		if (!isAppend && !isView)
			close("all"); // with CHECK we may still need to do this
		si.setCursor(GenericPlatform.CURSOR_WAIT);
		try {
			si.siSetCurrentSource(isView ? JDXSource.createView(specs) : JDXReader
					.createJDXSource(JSVFileManager.getBufferedReaderForData(data),
							filePath, obscureTitleFromUser == Boolean.TRUE, loadImaginary,
							firstSpec, lastSpec, nmrMaxY));
		} catch (Exception e) {
			/**
			 * @j2sNative alert(e.toString())
			 */
			{
				Logger.error(e.toString());
				if (Logger.debugging)
					e.printStackTrace();
				si.writeStatus(e.getMessage());
			}
			si.setCursor(GenericPlatform.CURSOR_DEFAULT);
			if (isApplet) {
				selectedPanel.showMessage(e.toString(), "Error Opening File");
			}
			return FILE_OPEN_ERROR;
		}
		si.setCursor(GenericPlatform.CURSOR_DEFAULT);
		System.gc();
		if (newPath == null) {
			newPath = currentSource.getFilePath();
			if (newPath != null)
				fileName = newPath.substring(newPath.lastIndexOf("/") + 1);
		} else {
			currentSource.setFilePath(newPath);
		}
		if (id == null && !isView)
			id = newPath;
		if (id != null)
			currentSource.setID(id);
		si.siSetLoaded(fileName, newPath);

		Spectrum spec = currentSource.getJDXSpectrum(0);
		if (spec == null) {
			return FILE_OPEN_NO_DATA;
		}

		specs = currentSource.getSpectra();
		Spectrum.process(specs, irMode);

		boolean autoOverlay = interfaceOverlaid
				|| spec.isAutoOverlayFromJmolClick();

		boolean combine = isView || autoOverlay && currentSource.isCompoundSource;
		if (combine) {
			combineSpectra((isView ? strUrl : null));
		} else {
			splitSpectra();
		}
		pd().taintedAll = true;
		if (!isView)
			si.siUpdateRecentMenus(filePath);
		return FILE_OPEN_OK;
	}

	public void close(String value) {
		// close * > 1 "close all except one spectrum."
		// close SIMULATIONS > 1 "close simulations until no more than one spectrum is present,
		// or all simulations if that is not possible."
		int n0 = 0;
		int pt = (value == null ? -2 : value.indexOf(">"));
		if (pt > 0) {
			n0 = PT.parseInt(value.substring(pt + 1).trim());
			value = value.substring(0, pt).trim();
		}
		if ("*".equals(value))
			value = "all";
		boolean isAll = (value == "all");
		if (value == null || n0 == 0 && value.equalsIgnoreCase("all")) {
			closeSource(null);
			return;
		}
		boolean isViews = value.equalsIgnoreCase("views");
		Lst<JDXSource> list = new Lst<JDXSource>();
		JDXSource source;
		value = value.replace('\\', '/');
		int n = panelNodes.size();
		int nMax = n - n0;
		if (value.endsWith("*")) {
			value = value.substring(0, value.length() - 1);
			for (int i = n; --i >= 0;)
				if (panelNodes.get(i).fileName.startsWith(value))
					list.addLast(panelNodes.get(i).source);
		} else if (value.equalsIgnoreCase("selected")) {
			JDXSource lastSource = null;
			for (int i = n; --i >= 0;) {
				source = panelNodes.get(i).source;
				if (panelNodes.get(i).isSelected
						&& (lastSource == null || lastSource != source))
					list.addLast(source);
				lastSource = source;
			}
		} else if (isAll || isViews || value.equalsIgnoreCase("simulations")) {
			for (int n1 = 0, i = n; --i >= 0 && n1 < nMax;)
				if (isAll ? true : isViews ? panelNodes.get(i).isView : panelNodes.get(i).isSimulation) {
					list.addLast(panelNodes.get(i).source);
					n1++;
				}
		} else {
			source = (value.length() == 0 ? currentSource : PanelNode
					.findSourceByNameOrId(value, panelNodes));
			if (source != null)
				list.addLast(source);
		}
		for (int i = list.size(); --i >= 0;)
			closeSource(list.get(i));
		if (selectedPanel == null && panelNodes.size() > 0)
			si.siSetSelectedPanel(PanelNode.getLastFileFirstNode(panelNodes));
	}

	public void load(String value, String script) {
		@SuppressWarnings("unused")
		Object applet = html5Applet;
		/**
		 * When part of a view set, route all internal 
		 * database requests through this.html5Applet._search.  
		 * 
		 * @j2sNative
		 * 
		 * if (applet._viewSet != null && !value.startsWith("ID"))
		 *    return applet._search(value);
		 *    
		 */
		// load   (alone) just runs defaultLoadScript
		// load ID "xx"...
		Lst<String> tokens = ScriptToken.getTokens(value);
		String filename = tokens.get(0);
		String id = null;
		int pt = 0;
		if (filename.equalsIgnoreCase("ID")) {
			id = PT.trimQuotes(tokens.get(1));
			filename = tokens.get(2);
			pt = 2;
		}
		boolean isAppend = filename.equalsIgnoreCase("APPEND");
		boolean isCheck = filename.equalsIgnoreCase("CHECK");
		if (isAppend || isCheck)
			pt++;
		if (pt > 0)
			filename = tokens.get(pt);
		if (script == null)
			script = defaultLoadScript;
		if (filename.equals("?")) {
			openFileFromDialog(isAppend, false, null, script);
			return;
		}
		if (filename.equals("http://?")) {
			openFileFromDialog(isAppend, true, null, null);
			return;
		}
		if (filename.equals("$?") || filename.equals("$H1?")) {
			openFileFromDialog(isAppend, true, "H1", null);
			return;
		}
		if (filename.equals("$C13?")) {
			openFileFromDialog(isAppend, true, "C13", null);
			return;
		}
		boolean isH1 = filename.equalsIgnoreCase("MOL") || filename.equalsIgnoreCase("H1");
		boolean isC13 = filename.equalsIgnoreCase("C13");
		if (isH1 || isC13)
			filename = JSVFileManager.SIMULATION_PROTOCOL + (isH1 ? "H1/" : "C13/") + "MOL="
					+ PT.trimQuotes(tokens.get(++pt));
		if (!isCheck && !isAppend) {
			if (filename.equals("\"\"") && currentSource != null)
				filename = currentSource.getFilePath();
			close("all");
		}
		filename = PT.trimQuotes(filename);
		if (filename.startsWith("$")) {
			if (!filename.startsWith("$H1") && !filename.startsWith("$C13"))
				filename = "$H1/" + filename.substring(3);
			filename = JSVFileManager.SIMULATION_PROTOCOL + filename.substring(1);
		}
		int firstSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : -1);
		int lastSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : firstSpec);
		si.siOpenDataOrFile(null, null, null, filename, firstSpec, lastSpec,
				isAppend, script, id);
	}

	public void combineSpectra(String name) {
		JDXSource source = currentSource;
		Lst<Spectrum> specs = source.getSpectra();
		JSVPanel jsvp = si.siGetNewJSVPanel2(specs);
		jsvp.setTitle(source.getTitle());
		if (jsvp.getTitle().equals("")) {
			jsvp.getPanelData().setViewTitle(source.getFilePath());
			jsvp.setTitle(name);
		}
		si.siSetPropertiesFromPreferences(jsvp, true);
		spectraTree.createTree(++fileCount, source, new JSVPanel[] { jsvp }).getPanelNode().isView = true;
		PanelNode node = PanelNode.findNode(selectedPanel, panelNodes);
		node.setFrameTitle(name);
		node.isView = true;
		if (autoShowLegend && pd().getNumberOfGraphSets() == 1)
			node.setLegend(getDialog(AType.OverlayLegend, null));
		si.siSetMenuEnables(node, false);
	}

	public void closeSource(JDXSource source) {
		// Remove nodes and dispose of frames
		JSVTreeNode rootNode = spectraTree.getRootNode();
		String fileName = (source == null ? null : source.getFilePath());
		Lst<JSVTreeNode> toDelete = new Lst<JSVTreeNode>();
		Enumeration<JSVTreeNode> enume = rootNode.children();
		while (enume.hasMoreElements()) {
			JSVTreeNode node = enume.nextElement();
			if (fileName == null
					|| node.getPanelNode().source.getFilePath().equals(fileName)) {
				for (Enumeration<JSVTreeNode> e = node.children(); e.hasMoreElements();) {
					JSVTreeNode childNode = e.nextElement();
					toDelete.addLast(childNode);
					panelNodes.removeObj(childNode.getPanelNode());
				}
				toDelete.addLast(node);
				if (fileName != null)
					break;
			}
		}
		
		spectraTree.deleteNodes(toDelete);
		if (source == null) {
			// jsvpPopupMenu.dispose();
			if (currentSource != null)
				currentSource.dispose();
			currentSource = null;
			// jsvpPopupMenu.dispose();
			if (selectedPanel != null)
				selectedPanel.dispose();
		} else {
			// setFrameAndTreeNode(panelNodes.size() - 1);
		}

		if (currentSource == source) {
			si.siSetSelectedPanel(null);
			si.siSetCurrentSource(null);
		}

		int max = 0;
		for (int i = 0; i < panelNodes.size(); i++) {
			float f = PT.parseFloat(panelNodes.get(i).id);
			if (f >= max + 1)
				max = (int) Math.floor(f);
		}
		fileCount = max;
		System.gc();
		Logger.checkMemory();
		si.siSourceClosed(source);
	}

	public void setFrameAndTreeNode(int i) {
		if (panelNodes == null || i < 0 || i >= panelNodes.size())
			return;
		setNode(panelNodes.get(i));
	}

	public PanelNode selectFrameNode(JSVPanel jsvp) {
		// Find Node in SpectraTree and select it
		PanelNode node = PanelNode.findNode(jsvp, panelNodes);
		if (node == null)
			return null;
		spectraTree.setPath(spectraTree.newTreePath(node.treeNode.getPath()));
		setOverlayLegendVisibility(null, false);
		return node;
	}

	private boolean setSpectrum(String value) {
		if (value.indexOf('.') >= 0) {
			PanelNode node = PanelNode.findNodeById(value, panelNodes);
			if (node == null)
				return false;
			setNode(node);
		} else {
			int n = PT.parseInt(value);
			if (n <= 0) {
				checkOverlay();
				return false;
			}
			setFrameAndTreeNode(n - 1);
		}
		return true;
	}

	public void splitSpectra() {
		JDXSource source = currentSource;
		Lst<Spectrum> specs = source.getSpectra();
		JSVPanel[] panels = new JSVPanel[specs.size()];
		JSVPanel jsvp = null;
		for (int i = 0; i < specs.size(); i++) {
			Spectrum spec = specs.get(i);
			jsvp = si.siGetNewJSVPanel(spec);
			si.siSetPropertiesFromPreferences(jsvp, true);
			panels[i] = jsvp;
		}
		// arrange windows in ascending order
		spectraTree.createTree(++fileCount, source, panels);
		si.siGetNewJSVPanel(null); // end of operation
		PanelNode node = PanelNode.findNode(selectedPanel, panelNodes);
		si.siSetMenuEnables(node, true);
	}

	public void selectedTreeNode(JSVTreeNode node) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			setNode(node.getPanelNode());
		} else {
			System.out.println("not a leaf");
		}
		si.siSetCurrentSource(node.getPanelNode().source);
	}

	public void dispose() {
		fileHelper = null;
		if (viewDialog != null)
			viewDialog.dispose();
		viewDialog = null;
		if (overlayLegendDialog != null)
			overlayLegendDialog.dispose();
		overlayLegendDialog = null;

		if (jsvpPopupMenu != null) {
			jsvpPopupMenu.jpiDispose();
			jsvpPopupMenu = null;
		}
		if (panelNodes != null)
			for (int i = panelNodes.size(); --i >= 0;) {
				panelNodes.get(i).dispose();
				panelNodes.removeItemAt(i);
			}
	}

	public void runScript(String script) {
		if (scriptQueue == null)
			si.siProcessCommand(script);
		else
			scriptQueue.addLast(script);
	}

	public void requestRepaint() {
		if (selectedPanel != null)
			repaintManager.refresh();
	}

	public void repaintDone() {
		repaintManager.repaintDone();
	}

	public void checkOverlay() {
		if (mainPanel != null)
			markSelectedPanels(panelNodes, mainPanel.getCurrentPanelIndex());
		viewDialog = getDialog(AType.Views, null);
	}

	private void markSelectedPanels(Lst<PanelNode> panelNodes, int ip) {
		for (int i = panelNodes.size(); --i >= 0;)
			panelNodes.get(i).isSelected = (ip == i);
	}

	private int recentStackPercent = 5;

	private void execOverlayOffsetY(int offset) {
		if (offset == Integer.MIN_VALUE) {
			if (selectedPanel == null)
				return;
			String soffset = selectedPanel.getInput(
					"Enter a vertical offset in percent for stacked plots", "Overlay", ""
							+ recentStackPercent);
			float f = PT.parseFloat(soffset);
			if (Float.isNaN(f))
				return;
			offset = (int) f;
		}
		recentStackPercent = offset;
		parameters.viewOffset = offset;
		if (isClosed())
			pd().setYStackOffsetPercent(offset);
	}

	private void execScriptInline(String script) {
		if (script.length() > 0)
			script = script.substring(6).trim();
		if (script.length() == 0)
			script = selectedPanel.getInput("Enter a JSpecView script", "Script",
					recentScript);
		if (script == null)
			return;
		recentScript = script;
		runScriptNow(script);
	}

	// / called by JSmol JavaScript

	public void setDisplay(Object canvas) {
		// used by JSmol/HTML5 when a canvas is resized
		apiPlatform.setViewer(this, display = canvas);
		int[] wh = new int[2];
		apiPlatform.getFullScreenDimensions(canvas, wh);
		setScreenDimension(wh[0], wh[1]);
	}

	public void setScreenDimension(int width, int height) {
		// There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
		// so don't try dim1.equals(dim2)
		height = Math.min(height, maximumSize);
		width = Math.min(width, maximumSize);
		if (dimScreen.width == width && dimScreen.height == height)
			return;
		// System.out.println("HMM " + width + " " + height + " " + maximumSize);
		resizeImage(width, height);
	}

	void resizeImage(int width, int height) {
		if (width > 0) {
			dimScreen.width = width;
			dimScreen.height = height;
		} else {
			width = (dimScreen.width == 0 ? dimScreen.width = 500 : dimScreen.width);
			height = (dimScreen.height == 0 ? dimScreen.height = 500
					: dimScreen.height);
		}
		g2d.setWindowParameters(width, height);
	}

	/**
	 * for JavaScript only; this is the call to draw the spectrum
	 * 
	 */
	public void updateJS() {
		if (selectedPanel != null)
			selectedPanel.paintComponent(apiPlatform.getGraphics(null));
	}

	/**
	 * called by JSmol.js mouse event
	 * 
	 * @param id
	 * @param x
	 * @param y
	 * @param modifiers
	 * @param time
	 * @return t/f
	 */
	public boolean processMouseEvent(int id, int x, int y, int modifiers,
			long time) {
		return (selectedPanel != null && selectedPanel.processMouseEvent(id, x,
				y, modifiers, time));
	}

	public void processTwoPointGesture(float[][][] touches) {
		if (!isClosed())
			selectedPanel.processTwoPointGesture(touches);
	}

	public Object getApplet() {
		return html5Applet;
	}

	public void openFileAsyncSpecial(String fileName, int flags) {
		String ans = (currentSource == null ? "NO" : getDialogManager()
				.getDialogInput(this,
						"Do you want to append this file? (Answer NO to replace.)",
						"Drag/Drop Action", DialogManager.QUESTION_MESSAGE, null, null,
						"YES"));
		if (ans == null)
			return;
		String pre = (ans.toLowerCase().startsWith("y") ? "append" : "");
		runScript("load " + pre + " \"" + fileName + "\"");
	}

	public int getHeight() {
		return dimScreen.height;
	}

	public int getWidth() {
		return dimScreen.width;
	}

	public Object getPlatformInterface(String type) {
		return getInterface("jspecview." + (isJS ? "js2d.Js" : "java.Awt")
				+ type);
	}

	public DialogManager getDialogManager() {
		if (dialogManager != null)
			return dialogManager;
		dialogManager = (DialogManager)  getPlatformInterface("DialogManager");
			//Interface.getInterface("jspecview.awtjs2d.JsDialogManager");
		return dialogManager.set(this);
	}

	public JSVDialog getDialog(AType type, Spectrum spec) {
		String root = "jspecview.dialog.";
		switch (type) {
		case Integration:
			return ((JSVDialog) getInterface(root + "IntegrationDialog"))
					.setParams("Integration for " + spec, this, spec);
		case Measurements:
			return ((JSVDialog) getInterface(root + "MeasurementsDialog"))
					.setParams("Measurements for " + spec, this, spec);
		case PeakList:
			return ((JSVDialog) getInterface(root + "PeakListDialog"))
					.setParams("Peak List for " + spec, this, spec);
		case OverlayLegend:
			return overlayLegendDialog = ((JSVDialog) getInterface(root
					+ "OverlayLegendDialog")).setParams(pd().getViewTitle(), this, null);
		case Views:
			return viewDialog = ((JSVDialog) getInterface(root
					+ "ViewsDialog")).setParams("View/Combine/Close Spectra", this, null);
		default:
			return null;
		}
	}

	private void show(String what) {
		getDialogManager();
		if (what.equals("properties")) {
			dialogManager.showProperties(null, pd().getSpectrum());
		} else if (what.equals("errors")) {
			dialogManager.showSourceErrors(null, currentSource);
		} else if (what.equals("source")) {
			if (currentSource == null) {
				if (panelNodes.size() > 0)
					dialogManager.showMessageDialog(null, "Please Select a Spectrum",
							"Select Spectrum", DialogManager.ERROR_MESSAGE);
				return;
			}
			dialogManager.showSource(this, pd().getSpectrum().getFilePath());
		} else if (what.startsWith("solutioncolorfill")) {
			if (what.indexOf("all") >= 0) {
				for (int i = panelNodes.size(); --i >= 0;)
					panelNodes.get(i).pd().setSolutionColor(what);
			} else {
				pd().setSolutionColor(what);
			}
		} else if (what.startsWith("solutioncolor")) {
			String msg = getSolutionColorStr(what.indexOf("false") < 0);
			msg = "background-color:rgb(" + msg
					+ ")'><br />Predicted Solution Colour- RGB(" + msg + ")<br /><br />";
			if (isJS) {
				dialogManager.showMessage(this, "<div style='width:100%;height:100%;"
						+ msg + "</div>", "Predicted Colour");
			} else {
				selectedPanel.showMessage("<html><body style='" + msg
						+ "</body></html>", "Predicted Colour");
			}
		}
	}

	private PrintLayout lastPrintLayout;
	private Object offWindowFrame;

	public PrintLayout getDialogPrint(boolean isJob) {
		if (!isJS)
			try {
				PrintLayout pl = ((JSVPrintDialog) getPlatformInterface("PrintDialog"))
						.set(offWindowFrame, lastPrintLayout, isJob).getPrintLayout();
				if (pl != null)
					lastPrintLayout = pl;
				return pl;
			} catch (Exception e) {
			}
		return new PrintLayout(pd());
	}

	public void setIRmode(String mode) {
		if (mode.equals("AtoT")) {
			irMode = IRMode.TO_TRANS;
		} else if (mode.equals("TtoA")) {
			irMode = IRMode.TO_ABS;
		} else {
			irMode = IRMode.getMode(mode);
		}
	}

	public int getOptionFromDialog(String[] items, String title, String label) {
		return getDialogManager().getOptionFromDialog(null, items, selectedPanel, title,
				label);
	}

	public String print(String fileName) {
		return execWrite("PDF \"" + fileName + "\"");
	}

	private String execWrite(String value) {
		if (isJS && value == null)
			value = "PDF";
		String msg = ((ExportInterface) JSViewer
				.getInterface("jspecview.export.Exporter")).write(this,
				value == null ? null : ScriptToken.getTokens(value), false);
		si.writeStatus(msg);
		return msg;
	}

	public String export(String type, int n) {
		if (type == null)
			type = "XY";
		PanelData pd = pd();
		int nMax = pd.getNumberOfSpectraInCurrentSet();
		if (n < -1 || n >= nMax)
			return "Maximum spectrum index (0-based) is " + (nMax - 1) + ".";
		Spectrum spec = (n < 0 ? pd.getSpectrum() : pd.getSpectrumAt(n));
		try {
			return ((ExportInterface) JSViewer
					.getInterface("jspecview.export.Exporter")).exportTheSpectrum(this,
					ExportType.getType(type), null, spec, 0, spec.getXYCoords().length - 1, null, false);
		} catch (Exception e) {
			Logger.error(e.toString());
			return null;
		}
	}

	@Override
	public String postByteArray(String fileName, byte[] bytes) {
		return JSVFileManager.postByteArray(fileName, bytes);
	}

	@SuppressWarnings("resource")
	public OC getOutputChannel(String fileName, boolean isBinary) throws Exception {
		OutputStream os = null;
		/**
		 * in JavaScript, this will be a string buffer or byte array
		 * 
		 * @j2sNative
		 * 
		 * while (fileName.startsWith("/"))
		 *   fileName = fileName.substring(1);
		 * 
		 * 
		 */
		{
			os = (fileName == null || fileName.equals(";base64,") ? null : new FileOutputStream(fileName));
		}
		return new OC().setParams(this, fileName, !isBinary, os);
	}

	public static Object getInterface(String name) {
	  try {
	    Class<?> x = Class.forName(name);
	    return (x == null ? null : x.newInstance());
	  } catch (Exception e) {
	    Logger.error("Interface.java Error creating instance for " + name + ": \n" + e);
	    return null;
	  }
	}

	public void showMessage(String msg) {
		if (selectedPanel != null && msg != null)
			selectedPanel.showMessage(msg, null);
	}

	public void openFileFromDialog(boolean isAppend, boolean isURL,
			String simulationType, String script) {
		String url = null;
		if (simulationType != null) {
			url = fileHelper.getUrlFromDialog(
					"Enter the name or identifier of a compound", recentSimulation);
			if (url == null)
				return;
			recentSimulation = url;
			load((isAppend ? "APPEND " : "") + "\"$" + simulationType + "/" + url + "\"", script);
		} else if (isURL) {
			url = fileHelper.getUrlFromDialog("Enter the URL of a JCAMP-DX File",
					recentURL == null ? recentOpenURL : recentURL);
			if (url == null)
				return;
			recentOpenURL = url;
			load((isAppend ? "APPEND " : "") + "\"" + url + "\"", script);
		} else {
			Object[] userData = new Object[] { Boolean.valueOf(isAppend), script }; 
			GenericFileInterface file = fileHelper.showFileOpenDialog(mainPanel,
					userData);
			// note that in JavaScript this will be asynchronous and file will be null.
			if (file != null)
				openFile(file.getFullPath(), !isAppend);
			// it is not necessary to run the script in Java; we are not loading asynchronously
		}
	}

	private String recentOpenURL = "http://";
	private String recentURL;
	private String recentSimulation = "tylenol";
	
	/**
	 * Opens and displays a file
	 * @param fileName 
	 * @param closeFirst 
	 * 
	 */
	public void openFile(String fileName, boolean closeFirst) {
		if (closeFirst && panelNodes != null) {
			JDXSource source = PanelNode.findSourceByNameOrId((new File(fileName))
					.getAbsolutePath(), panelNodes);
			if (source != null)
				closeSource(source);
		}
		si.siOpenDataOrFile(null, null, null, fileName, -1, -1, true, defaultLoadScript, null);
		
	}

	public int selectPanel(JSVPanel jsvp, Lst<PanelNode> panelNodes) {
		int iPanel = -1;
		if (panelNodes != null) {
			for (int i = panelNodes.size(); --i >= 0;) {
				JSVPanel j = panelNodes.get(i).jsvp;
				if (j == jsvp) {
					iPanel = i;
				} else {
					j.setEnabled(false);
					j.setFocusable(false);
					j.getPanelData().closeAllDialogsExcept(AType.NONE);
				}
			}
			markSelectedPanels(panelNodes, iPanel);
		}
		return iPanel;
	}

	public void checkAutoIntegrate() {
		if (autoIntegrate)
			pd().integrateAll(parameters);
	}

	/**
	 * Parses the JavaScript call parameters and executes them accordingly
	 * 
	 * @param params
	 *          String
	 */
	public void parseInitScript(String params) {
		if (params == null)
			params = "";
		ScriptTokenizer allParamTokens = new ScriptTokenizer(params, true);
		if (Logger.debugging) {
			Logger.info("Running in DEBUG mode");
		}
		while (allParamTokens.hasMoreTokens()) {
			String token = allParamTokens.nextToken();
			// now split the key/value pair
			ScriptTokenizer eachParam = new ScriptTokenizer(token, false);
			String key = eachParam.nextToken();
			if (key.equalsIgnoreCase("SET"))
				key = eachParam.nextToken();
			key = key.toUpperCase();
			ScriptToken st = ScriptToken.getScriptToken(key);
			String value = ScriptToken.getValue(st, eachParam, token);
			//if (Logger.debugging)
				Logger.info("KEY-> " + key + " VALUE-> " + value + " : " + st);
			try {
				switch (st) {
				default:
					parameters.set(null, st, value);
					break;
				case UNKNOWN:
					break;
				case APPLETID:
					fullName = appletName + "__" 
				      + (appletName = value) + "__";
					Object applet = null;
					/**
					 * @j2sNative
					 * 
					 *            if(typeof Jmol != "undefined") applet =
					 *            Jmol._applets[value];
					 * 
					 * 
					 */
					{
					}
					this.html5Applet = applet;
					break;
				case AUTOINTEGRATE:
					autoIntegrate = Parameters.isTrue(value);
					break;
				case COMPOUNDMENUON:
					allowCompoundMenu = Boolean.parseBoolean(value);
					break;
				case APPLETREADYCALLBACKFUNCTIONNAME:
				case COORDCALLBACKFUNCTIONNAME:
				case LOADFILECALLBACKFUNCTIONNAME:
				case PEAKCALLBACKFUNCTIONNAME:
				case SYNCCALLBACKFUNCTIONNAME:
					si.siExecSetCallback(st, value);
					break;
				case ENDINDEX:
					initialEndIndex = Integer.parseInt(value);
					break;
				case INTERFACE:
					checkOvelayInterface(value);
					break;
				case IRMODE:
					setIRmode(value);
					break;
				case MENUON:
					allowMenu = Boolean.parseBoolean(value);
					break;
				case OBSCURE:
					if (obscureTitleFromUser == null) // once only
						obscureTitleFromUser = Boolean.valueOf(value);
					break;
				case STARTINDEX:
					initialStartIndex = Integer.parseInt(value);
					break;
				// case SPECTRUMNUMBER:
				// initialSpectrumNumber = Integer.parseInt(value);
				// break;
				case SYNCID:
					fullName = appletName + "__" 
							+ (syncID = value) + "__";
					break;
				}
			} catch (Exception e) {
			}
		}
	}

	public String getSolutionColorStr(boolean asFit) {
		P3 pt = CU.colorPtFromInt(getSolutionColor(asFit), null);
		return (int) pt.x + "," + (int) pt.y + "," + (int) pt.z;
	}


}
