/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either"
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
package org.gennbo;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogBase extends JDialog {

  protected static final int DIALOG_HOME = 0;
  protected static final int DIALOG_MODEL = 1;
  protected static final int DIALOG_RUN = 2;
  protected static final int DIALOG_VIEW = 3;
  protected static final int DIALOG_SEARCH = 4;
  protected static final int DIALOG_CONFIG = 5;
  protected static final int DIALOG_HELP = 6;
  
  final private static String[] dialogNames = new String[] { "Home", "Model",
      "Run", "View", "Search", "Settings", "Help" };

  protected static String getDialogName(int type) {
    return dialogNames[type];
  }

  protected static final int ORIGIN_UNKNOWN = 0;
  protected static final int ORIGIN_NIH = 1;
  protected static final int ORIGIN_LINE_FORMULA = 2;
  protected static final int ORIGIN_FILE_INPUT = 3;
  protected static final int ORIGIN_NBO_ARCHIVE = 4;

  protected int modelOrigin = ORIGIN_UNKNOWN;


  
  protected boolean jmolOptionNOZAP = false; // do no zap between modules
  protected boolean jmolOptionNOSET = false; // do not use NBO settings by default
  protected boolean jmolOptionVIEW = false; // present only the VIEW option
  protected boolean jmolOptionNONBO = false; // do not try to contact NBOServe

  
  /**
   * Allowing passage of Jmol options. Currently: NOZAP;NOSET;JMOL;VIEW
   * 
   * @param jmolOptions
   */
  protected void setJmolOptions(Map<String, Object> jmolOptions) {
    String options = ("" + (jmolOptions == null ? "" : jmolOptions
        .get("options"))).toUpperCase();
    if (options.equals("VIEW"))
      options = "VIEW;NOZAP;NOSET;NONBO";
    jmolOptionVIEW = (options.indexOf("VIEW") >= 0);
    jmolOptionNOZAP = (options.indexOf("NOZAP") >= 0);
    jmolOptionNOSET = (options.indexOf("NOSET") >= 0);
    jmolOptionNONBO = (options.indexOf("NONBO") >= 0);
  }

  protected Viewer vwr;
  protected NBOService nboService;

  /**
   * Jmol plugin object for NBO
   * 
   */
  protected NBOPlugin nboPlugin;

  // private/protected variables

  /**
   * Tracks the last resonance structure type (nrtstra, nrtstrb, alpha, beta);
   * reset to “alpha” by openPanel()
   */
  private String rsTypeLast = "alpha";

  /**
   * String value of what is showing in the session dialog -- persistent
   */
  protected String nboOutputBodyText = "";
  
  /**
   * The input file handler; recreated via openPanel()
   */
  protected NBOFileHandler inputFileHandler;

  protected JLabel icon;
  protected JSplitPane centerPanel;
  protected JPanel modulePanel;

  protected JLabel statusLab;
  protected JTextPane jpNBODialog;

  
  
  abstract protected void setStatus(String statusInfo);

  abstract protected void updatePanelSettings();

  abstract protected NBOFileHandler newNBOFileHandler(String name, String ext,
                                                      int mode, String useExt);

  
  /**
   * true if NBOServe has successfully restarted-- persistent
   */
  protected boolean haveService;

  /**
   * the dialog that is currently open, for example DIALOG_MODEL-- persistent
   */
  protected int dialogMode;
  
  /**
   * configuration information source
   */
  protected NBOConfig config;

  
  
  public NBODialogBase(JFrame f) {
    super(f);
  }

  protected String getJmolWorkingPath() {
    String path = JmolPanel.getJmolProperty("workingPath",
        System.getProperty("user.home"));
    saveWorkingPath(path);
    return path;
  }

  protected String getWorkingPath() {
    String path = nboPlugin.getNBOProperty("workingPath", null);
    return (path == null ? getJmolWorkingPath() : path);
  }

  protected void saveWorkingPath(String path) {
    nboPlugin.setNBOProperty("workingPath", path);
  }

  protected static void colorMeshes() {
    // yeiks! causes file load again! updatePanelSettings();
  }

  protected void resetVariables_c() {
    rsTypeLast = "alpha";
  }

  protected void alertRequiresNBOServe() {
    vwr.alert("This functionality requires NBOServe.");
  }


  protected void logCmd(String msg) {
    log(msg, 'I');
  }

  protected void logValue(String msg) {
    log(msg, 'b');
  }

  protected void logStatus(String msg) {
    log(msg, 'p');
  }

  protected void logError(String msg) {
    log(msg, 'r');
  }

  /**
   * appends output to session dialog panel
   * 
   * @param line
   *        output message to append
   * @param chFormat
   *        p, b, r ("red"), i, etc.
   */
  protected synchronized void log(String line, char chFormat) {
    if (dontLog(line, chFormat))
      return;
    if (line.trim().length() >= 1) {
      line = PT.rep(line.trim(), "<", "&lt;");
      line = PT.rep(line, ">", "&gt;");
      line = PT.rep(line, "&lt;br&gt;", "<br>");
      String format0 = "" + chFormat;
      String format1 = format0;
      //      String fontFamily = jpNBOLog.getFont().getFamily();
      if (chFormat == 'r') {
        format0 = "b style=color:red";
        format1 = "b";
        setStatus("");
      }

      if (!format0.equals("p"))
        line = "<" + format0 + ">" + line + "</" + format1 + ">";
      jpNBODialog.setText("<html><font face=\"Arial\">"
          + (nboOutputBodyText = nboOutputBodyText + line + "\n<br>")
          + "</font></html>");
    }
    jpNBODialog.setCaretPosition(jpNBODialog.getDocument().getLength());
  }

  private boolean dontLog(String line, char chFormat) {
    return (jpNBODialog == null || line.trim().equals("")
        || line.indexOf("read/unit=5/attempt to read past end") >= 0
        || line.indexOf("*end*") >= 0 || !NBOConfig.debugVerbose
        && "b|r|I".indexOf("" + chFormat) < 0);
  }

  protected void alertError(String line) {
    line = PT.rep(line.replace('\r', ' '), "\n\n", "\n");
    logError(line);
    vwr.alert(line);
  }

  protected void logInfo(String msg, int mode) {
    Logger.info(msg);
    log(msg, mode == Logger.LEVEL_INFO ? 'p' : mode == Logger.LEVEL_ERROR ? 'r'
        : mode == Logger.LEVEL_WARN ? 'b' : 'i');
  }

  protected void runScriptQueued(String script) {
    logInfo("_$ " + PT.rep(script, "\n", "<br>"), Logger.LEVEL_DEBUG);
    vwr.script(script);
  }

  protected boolean iAmLoading;

  protected void loadModelFileQueued(File f, boolean saveOrientation) {
    iAmLoading = true;
    String s = "load \"" + f.getAbsolutePath().replace('\\', '/') + "\""
        + NBOConfig.JMOL_FONT_SCRIPT;
    if (saveOrientation)
      s = "save orientation o1;" + s + ";restore orientation o1";
    runScriptQueued(s);
  }

  /**
   * Uses the LOAD DATA option to load data from NBO; just getting all the
   * "load xxx" methods in the same place.
   * 
   * 
   * @param s
   */
  protected void loadModelDataQueued(String s) {
    iAmLoading = true;
    runScriptQueued(s);
  }

  protected String loadModelFileNow(String s) {
    return runScriptNow("load " + s.replace('\\', '/'));
  }

  protected boolean checkEnabled() {
    return (jmolOptionNONBO || nboService.isEnabled()
        && nboService.restartIfNecessary());
  }

  protected String evaluateJmolString(String expr) {
    return vwr.evaluateExpressionAsVariable(expr).asString();
  }

  protected String getJmolFilename() {
    return evaluateJmolString("getProperty('filename')");
  }

  protected void getNewInputFileHandler(int mode) {
    inputFileHandler = newNBOFileHandler(inputFileHandler == null ? ""
        : inputFileHandler.jobStem, "47", mode, "47");
  }

  /**
   * label atoms: (number lone pairs)+atomnum
   * 
   * @param type
   *        alpha or beta
   */
  protected void doSetStructure(String type) {
    doSearchSetResStruct(type, -1);
  }

  /**
   * Changes bonds and labels on the Jmol model when new resonance structure is
   * selected
   * 
   * @param type
   *        one of nrtstra, nrtstrb, alpha, beta
   * @param rsNum
   *        - index of RS in Combo Box
   */
  protected void doSearchSetResStruct(String type, int rsNum) {
    if (!NBOConfig.showAtNum) {
      runScriptNow("measurements off;isosurface off;select visible;label off; select none;refresh");
      return;
    }
    //    boolean atomsOnly = (type == null);
    if (type == null) {
      type = rsTypeLast;
    } else {
      rsTypeLast = type;
    }
    SB sb = new SB();
    sb.append("measurements off;isosurface off;select visible;label %a;");
    String color = (NBOConfig.nboView) ? "black" : "gray";
    sb.append("select visible;color labels white;"
        + "select visible & _H;color labels " + color + ";"
        + "set labeloffset 0 0 {visible}; select none;refresh;");

    String s = inputFileHandler.setStructure(sb, type, rsNum);
    if (s == null) {
      runScriptNow(sb.toString());
      return;
    }
    //sb.append(s);    
    if (NBOConfig.nboView) {
      sb.append("select add {*}.bonds;color bonds lightgrey;"
          + "wireframe 0.1;");
    }
    sb.append(NBOConfig.JMOL_FONT_SCRIPT);
    runScriptQueued(sb.toString());
  }

  protected boolean isOpenShell() {
    return inputFileHandler.isOpenShell;
  }

  synchronized protected String runScriptNow(String script) {
    logInfo("!$ " + script, Logger.LEVEL_DEBUG);
    return PT.trim(vwr.runScript(script.replace('"', '\'')), "\n");
  }

  class HelpBtn extends JButton {

    String page;

    protected HelpBtn(String page) {
      this("Help", page, null);
    }

    protected HelpBtn(String label, String page, String tooltip) {
      super(label);
      setBackground(Color.black);
      setForeground(Color.white);
      this.page = page;
      tooltip = "Help for "
          + (tooltip != null ? tooltip
              : page == null || page.length() == 1 ? "this module" : page);
      setToolTipText(tooltip);
      addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          doHelp();
        }
      });
    }

    protected void doHelp() {
      vwr.showUrl(NBOConfig.NBO_WEB_SITE + "/jmol_help/" + getHelpPage());
    }

    /**
     * Get the proper help page for this context
     * 
     * @return a web page URI
     */
    protected String getHelpPage() {
      if (page != null)
        return page;
      switch (dialogMode) {
      case DIALOG_MODEL:
        return "model_help.htm";
      case DIALOG_RUN:
        return "run_help.htm";
      case DIALOG_VIEW:
        return "view_help.htm";
      case DIALOG_SEARCH:
        return "search_help.htm";
      case DIALOG_CONFIG:
      case DIALOG_HOME:
      default:
        return "Jmol_NBOPro6_help.htm";
      }
    }
  }

}
