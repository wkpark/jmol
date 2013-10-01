package jspecview.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jspecview.common.DialogHelper;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanel;
import jspecview.common.PanelData;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.util.JSVFileManager;
import jspecview.util.JSVTextFormat;

public class Exporter {

  public final static String sourceLabel = "Original...";
  public enum ExportType {
    UNK, SOURCE, DIF, FIX, SQZ, PAC, XY, DIFDUP, PNG, JPG, SVG, SVGI, CML, AML, PDF;

    public static ExportType getType(String type) {
      type = type.toUpperCase();
      if (type.equalsIgnoreCase(sourceLabel))
        return SOURCE;
      if (type.startsWith("XML"))
        return AML;
      for (ExportType mode : values())
        if (mode.name().equals(type)) 
          return mode;
      return UNK;
    }    
    
    public static boolean isExportMode(String ext) {
      return (getType(ext) != UNK);
    }
  }
  
  public Exporter() {
  }


  /**
   * returns message if path is not null, otherwise full string of text (unsigned applet)
   * @param mode 
   * @param path
   * @param spec
   * @param startIndex
   * @param endIndex
   * @return message or text
   * @throws IOException
   */
  public static String exportTheSpectrum(ExportType mode, String path, 
                                         JDXSpectrum spec, int startIndex, int endIndex)
      throws IOException {
    switch (mode) {
    case XY:
    case DIF:
    case DIFDUP:
    case FIX:
    case PAC:
    case SQZ:
      return JDXExporter.export(mode, path, spec, startIndex, endIndex);      
    case SVG:
    case SVGI:
      return (new SVGExporter()).exportAsSVG(path, spec, startIndex, endIndex, mode == ExportType.SVGI);
    case CML:
      return (new CMLExporter()).exportAsCML(path, spec, startIndex, endIndex);
    case AML:
      return (new AMLExporter()).exportAsAnIML(path, spec, startIndex, endIndex);
    default:
      return null;
    }
  }

  /**
   * 
   * @param si 
   * @param frame
   * @param dialogHelper
   * @param type
   * @return directory saved to or a message starting with "Error:" 
   */
  public static String exportSpectrum(ScriptInterface si, JFrame frame,
                                     DialogHelper dialogHelper, String type) {
  	
  	JSVPanel jsvp = si.getSelectedPanel();
  	boolean isView = si.getCurrentSource().isView;
    // From popup menu click SaveAs or Export
    // if JSVPanel has more than one spectrum...Choose which one to export
    int nSpectra = jsvp.getPanelData().getNumberOfSpectraInCurrentSet();
    if (nSpectra == 1 
    		|| !isView && type.equals(sourceLabel) 
    		|| jsvp.getPanelData().getCurrentSpectrumIndex() >= 0 
    		)
      return exportSpectrum(si, type, -1, dialogHelper);
    
    
    String[] items = new String[nSpectra];
    for (int i = 0; i < nSpectra; i++)
      items[i] = jsvp.getPanelData().getSpectrumAt(i).getTitle();

    final JDialog dialog = new JDialog(frame, "Export", true);
    dialog.setResizable(false);
    dialog.setSize(200, 100);
    Component panel = (Component) jsvp;
    dialog.setLocation((panel.getLocation().x + panel.getSize().width) / 2,
        (panel.getLocation().y + panel.getSize().height) / 2);
    final JComboBox<Object> cb = new JComboBox<Object>(items);
    Dimension d = new Dimension(120, 25);
    cb.setPreferredSize(d);
    cb.setMaximumSize(d);
    cb.setMinimumSize(d);
    JPanel p = new JPanel(new FlowLayout());
    JButton button = new JButton("OK");
    p.add(cb);
    p.add(button);
    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(
        new JLabel("Choose Spectrum to export", SwingConstants.CENTER),
        BorderLayout.NORTH);
    dialog.getContentPane().add(p);
    final int ret[] = new int[] { -1 };
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ret[0] = cb.getSelectedIndex();
        dialog.dispose();
      }
    });
    dialog.setVisible(true);
    dialog.dispose();
    if (ret[0] < 0)
    	return null;    
    return exportSpectrum(si, type, ret[0], dialogHelper);    
  }

  /**
   * from EXPORT command
   * @param jsvp 
   * @param tokens
   * @param forInkscape 
   * 
   * @return message for status line
   */
  public static String exportCmd(JSVPanel jsvp, List<String> tokens, boolean forInkscape) {
    // MainFrame or applet EXPORT command
    String mode = "XY";
    String fileName = null;
    switch (tokens.size()) {
    default:
      return "EXPORT what?";
    case 1:
      fileName = JSVTextFormat.trimQuotes(tokens.get(0));
      int pt = fileName.indexOf(".");
      if (pt < 0)
        return "EXPORT mode?";
      break;
    case 2:
      mode = tokens.get(0).toUpperCase();
      fileName = JSVTextFormat.trimQuotes(tokens.get(1));
      break;
    }
    String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
        .toUpperCase();
    if (ext.equals("JDX")) {
      if (mode == null)
        mode = "XY";
    } else if (ExportType.isExportMode(ext)) {
      mode = ext;
    } else if (ExportType.isExportMode(mode)){
      fileName += "."  + mode;
    }
    ExportType type = ExportType.getType(mode);
    if (forInkscape && type == ExportType.SVG)
      type = ExportType.SVGI;
    return exportSpectrumOrImage(jsvp, type, -1, fileName);
  }

  /**
   * Auxiliary Export method
   * @param si 
   * @param mode
   *        the format to export in
   * @param index
   *        the index of the spectrum
   * @param dialogHelper 
   * @return message or null if canceled
   */
	private static String exportSpectrum(ScriptInterface si,
                                              String mode, int index,
                                              DialogHelper dialogHelper) {
		JSVPanel jsvp = si.getSelectedPanel();
    ExportType imode = ExportType.getType(mode);
		String name = dialogHelper.getSuggestedFileName(imode);
    File file = dialogHelper.getFile(name, (Component) jsvp, true);
    if (file == null)
    	return null;
    String msg = "OK";
    if (imode == ExportType.SOURCE)
      JSVFileManager.fileCopy(jsvp.getPanelData().getSpectrum().getFilePath(), file);
    else
      msg = exportSpectrumOrImage(jsvp, imode, index, file
          .getAbsolutePath());
    boolean isOK = msg.startsWith("OK");
    if (isOK)
    	si.updateRecentMenus(file.getAbsolutePath());
    return msg;
  }
  
  /**
   * 
   * @param jsvp
   * @param imode
   * @param index
   * @param path
   * @return  status line message
   */
  private static String exportSpectrumOrImage(JSVPanel jsvp, ExportType imode,
                                              int index, String path) {
    JDXSpectrum spec;
    PanelData pd = jsvp.getPanelData();
    
    if (index < 0 && (index = pd.getCurrentSpectrumIndex()) < 0)
      return "Error exporting spectrum: No spectrum selected";
    spec = pd.getSpectrumAt(index);
    int startIndex = pd.getStartingPointIndex(index);
    int endIndex = pd.getEndingPointIndex(index);
    String msg;
    try {
      switch (imode) {
      case SVG:
      case SVGI:
        msg = (new SVGExporter()).exportAsSVG(path, spec.getXYCoords(), 
            spec.getTitle(), startIndex, endIndex, spec.getXUnits(), 
            spec.getYUnits(), spec.isContinuous(), spec.isXIncreasing(), spec.isInverted(), 
            (Color)jsvp.getColor(ScriptToken.PLOTAREACOLOR), ((Component) jsvp).getBackground(), 
            (Color)jsvp.getPlotColor(0),
            (Color)jsvp.getColor(ScriptToken.GRIDCOLOR),
            (Color)jsvp.getColor(ScriptToken.TITLECOLOR),
            (Color)jsvp.getColor(ScriptToken.SCALECOLOR),
            (Color)jsvp.getColor(ScriptToken.UNITSCOLOR),
            imode == ExportType.SVGI);
        break;
      default:
        msg = exportTheSpectrum(imode, path, spec, startIndex, endIndex);
      }
      return "OK - Exported " + imode.name() + ": " + path + msg;
    } catch (IOException ioe) {
      return "Error exporting " + path + ": " + ioe.getMessage();
    }
  }
  
}
