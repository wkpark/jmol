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

package jspecview.common;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import jspecview.common.JDXSpectrum;
import jspecview.common.Annotation.AType;
import jspecview.export.Exporter;
import jspecview.util.JSVLogger;
import jspecview.util.JSVParser;

/**
 * Popup Menu for JSVPanel.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.JSVPanel
 */
public class AwtPopupMenu extends JPopupMenu {

  protected boolean isApplet;

  public enum EnumOverlay { DIALOG, OFFSETY }
  

  private static final long serialVersionUID = 1L;

  private ScriptInterface scripter;
  
  public void dispose() {
    pd = null;
    //scripter = null;
  }

  /**
   * Menu Item that allows user to navigate to the next view of a JSVPanel
   * that has been zoomed
   */
  public JMenuItem nextMenuItem = new JMenuItem();
  /**
   * Menu Item for navigating to previous view
   */
  public JMenuItem previousMenuItem = new JMenuItem();
  /**
   * Allows for all view to be cleared
   */
  public JMenuItem clearMenuItem = new JMenuItem();
  /**
   * Allows for the JSVPanel to be reset to it's original display
   */
  public JMenuItem resetMenuItem = new JMenuItem();
  /**
   * Allows for the viewing of the properties of the Spectrum that is
   * displayed on the <code>JSVPanel</code>
   */
  public JMenuItem properties = new JMenuItem();

  protected JMenuItem userZoomMenuItem = new JMenuItem();
  protected JMenuItem scriptMenuItem = new JMenuItem();
  public JMenuItem overlayStackOffsetMenuItem = new JMenuItem();

  public JMenuItem integrationMenuItem = new JMenuItem();
  public JMenuItem measurementsMenuItem = new JMenuItem();
  public JMenuItem peakListMenuItem = new JMenuItem();
  public JMenuItem transAbsMenuItem = new JMenuItem();
  public JMenuItem solColMenuItem = new JMenuItem();
  
  public JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  public JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  public JCheckBoxMenuItem reversePlotCheckBoxMenuItem = new JCheckBoxMenuItem();


// applet only:
  
  protected JMenu appletSaveAsJDXMenu; // applet only
  protected JMenu appletExportAsMenu;  // applet only
  //protected JMenuItem appletAdvancedMenuItem;
  protected JMenuItem spectraMenuItem = new JMenuItem();
  public JMenuItem overlayKeyMenuItem = new JMenuItem();
  
  public AwtPopupMenu(ScriptInterface scripter) {
    super();
    this.scripter = scripter;
    jbInit();
  }

  /**
   * Initialises GUI components
   */
  protected void jbInit() {
    final ScriptInterface scripter = this.scripter;
    nextMenuItem.setText("Next View");
    nextMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().nextView();
        reboot();
      }
    });
    previousMenuItem.setText("Previous View");
    previousMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().previousView();
        reboot();
      }
    });
    clearMenuItem.setText("Clear Views");
    clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().resetView();
      }
    });
    resetMenuItem.setText("Reset View");
    resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().clearAllView();
      }
    });
    
    setOverlayItems();
    
    scriptMenuItem.setText("Script...");
    scriptMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         script(thisJsvp);
       }
     });
    userZoomMenuItem.setText("Set Zoom...");
    userZoomMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         userZoom(thisJsvp);
       }
     });
    properties.setActionCommand("Properties");
    properties.setText("Properties");
    properties.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.showProperties();
      }
    });
    gridCheckBoxMenuItem.setText("Show Grid");
    gridCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        runScript(scripter, "GRIDON " + (e.getStateChange() == ItemEvent.SELECTED));
        reboot();
      }
    });
    coordsCheckBoxMenuItem.setText("Show Coordinates");
    coordsCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        runScript(scripter, "COORDINATESON " + (e.getStateChange() == ItemEvent.SELECTED));
        reboot();
      }
    });
    reversePlotCheckBoxMenuItem.setText("Reverse Plot");
    reversePlotCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        runScript(scripter, "REVERSEPLOT " + (e.getStateChange() == ItemEvent.SELECTED));
        reboot();
      }
    });
    
    setPopupMenu();
  }

  protected void setOverlayItems() {
    spectraMenuItem.setText("Views...");
    spectraMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         overlay(thisJsvp, EnumOverlay.DIALOG
        		 );
       }
     });
    overlayStackOffsetMenuItem.setEnabled(false);
    overlayStackOffsetMenuItem.setText("Overlay Offset...");
    overlayStackOffsetMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         overlay(thisJsvp, EnumOverlay.OFFSETY);
       }
     });
	}

	/**
   * overridded in applet
   */
  protected void setPopupMenu() {
    add(gridCheckBoxMenuItem);
    add(coordsCheckBoxMenuItem);
    add(reversePlotCheckBoxMenuItem);
    addSeparator();
    add(nextMenuItem);
    add(previousMenuItem);
    add(clearMenuItem);
    add(resetMenuItem);
    add(userZoomMenuItem);
    addSeparator();
    add(spectraMenuItem);
    add(overlayStackOffsetMenuItem);
    add(scriptMenuItem);
    addSeparator();
    add(properties);
  }
  protected void reboot() {
    if (thisJsvp == null)
      return;
    thisJsvp.doRepaint();
    show((Container) thisJsvp, thisX, thisY);
  }

  private String recentZoom = "";

  public void userZoom(JSVPanel jsvp) {
  	if (jsvp == null)
  		return;
    String zoom = jsvp.getInput("Enter zoom range", "Zoom", recentZoom);
    if (zoom == null)
      return;
    recentZoom = zoom;
    runScript(scripter, "zoom " + zoom);
  }

  private String recentScript = "";

  public void script(JSVPanel jsvp) {
  	if (jsvp == null)
  		return;
    String script = jsvp.getInput("Enter a JSpecView script", 
    		"Script", recentScript);
    if (script == null)
      return;
    recentScript = script;
    runScript(scripter, script);
  }

  public static void setMenuItem(JMenuItem item, char c, String text,
                           int accel, int mask, EventListener el) {
    if (c != '\0')
      item.setMnemonic(c);
    item.setText(text);
    if (accel > 0)
      item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(accel,
          mask, false));
    if (el instanceof ActionListener)
      item.addActionListener((ActionListener) el);
    else if (el instanceof ItemListener)
      item.addItemListener((ItemListener) el);
  }

  public void setProcessingMenu(JComponent menu) {
    final ScriptInterface scripter = this.scripter;
    setMenuItem(integrationMenuItem, 'I', "Integration", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            scripter.getSelectedPanel().showDialog(AType.Integration);
          }
        });
    setMenuItem(measurementsMenuItem, 'M', "Measurements", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
          	scripter.getSelectedPanel().showDialog(AType.Measurements);
          }
        });
    setMenuItem(peakListMenuItem, 'P', "Peaks", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
          	scripter.getSelectedPanel().showDialog(AType.PeakList);
          }
        });
    setMenuItem(transAbsMenuItem, '\0', "Transmittance/Absorbance", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            runScript(scripter, "IRMODE IMPLIED");
          }
        });
    setMenuItem(solColMenuItem, 'C', "Predicted Solution Colour", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            runScript(scripter, "GETSOLUTIONCOLOR");
          }
        });
    menu.add(measurementsMenuItem);
    menu.add(peakListMenuItem);
    menu.add(integrationMenuItem);
    menu.add(transAbsMenuItem);
    menu.add(solColMenuItem);
  }

  protected static void runScript(ScriptInterface scripter, String cmd) {
    if (scripter == null)
      JSVLogger.error("scripter was null for " + cmd);
    else
      scripter.runScript(cmd);
  }

  protected String recentStackPercent = "5";

  private PanelData pd;

	public void overlay(JSVPanel jsvp, EnumOverlay overlay) {
		switch (overlay) {
		case DIALOG:
			scripter.checkOverlay();
			break;
		case OFFSETY:
			if (jsvp == null)
				return;
			String offset = jsvp.getInput(
					"Enter a vertical offset in percent for stacked plots", "Overlay",
					recentStackPercent);
			if (offset == null || Float.isNaN(JSVParser.parseFloat(offset)))
				return;
			recentStackPercent = offset;
			runScript(scripter, ScriptToken.STACKOFFSETY + " " + offset);
			break;
		}
	}


  private int thisX, thisY;
  JSVPanel thisJsvp;
  
  public void show(JSVPanel jsvp, int x, int y) {
    setEnables(jsvp);
    thisX = x;
    thisY = y;
    thisJsvp = jsvp;
    super.show((Container) jsvp, x, y);
 }

  public void setEnables(JSVPanel jsvp) {
    pd = jsvp.getPanelData();
    JDXSpectrum spec0 = pd.getSpectrum();
    setSelected(gridCheckBoxMenuItem, pd.getBoolean(ScriptToken.GRIDON));
    setSelected(coordsCheckBoxMenuItem, pd.getBoolean(ScriptToken.COORDINATESON));
    setSelected(reversePlotCheckBoxMenuItem, pd.getBoolean(ScriptToken.REVERSEPLOT));

    boolean isOverlaid = pd.isShowAllStacked();
    boolean isSingle = pd.haveSelectedSpectrum();
    
    integrationMenuItem.setEnabled(pd.getSpectrum().canIntegrate());
    measurementsMenuItem.setEnabled(pd.hasCurrentMeasurements(AType.Measurements));
    peakListMenuItem.setEnabled(pd.getSpectrum().is1D());
    
    solColMenuItem.setEnabled(isSingle && spec0.canShowSolutionColor());
    transAbsMenuItem.setEnabled(isSingle && spec0.canConvertTransAbs());
    overlayKeyMenuItem.setEnabled(isOverlaid && pd.getNumberOfGraphSets() == 1);
    overlayStackOffsetMenuItem.setEnabled(isOverlaid);
    // what about its selection???
    if (appletSaveAsJDXMenu != null)
      appletSaveAsJDXMenu.setEnabled(spec0.canSaveAsJDX());
    if (appletExportAsMenu != null)
      appletExportAsMenu.setEnabled(true);
    //if (appletAdvancedMenuItem != null)
      //appletAdvancedMenuItem.setEnabled(!isOverlaid);
  }

  private void setSelected(JCheckBoxMenuItem item, boolean TF) {
    item.setEnabled(false);
    item.setSelected(TF);
    item.setEnabled(true);
  }

  public static void setMenus(JMenu saveAsMenu, JMenu saveAsJDXMenu,
                              JMenu exportAsMenu, ActionListener actionListener) {
    saveAsMenu.setText("Save As");
    addMenuItem(saveAsMenu, Exporter.sourceLabel, '\0', actionListener);
    saveAsJDXMenu.setText("JDX");
    addMenuItem(saveAsJDXMenu, "XY", '\0', actionListener);
    addMenuItem(saveAsJDXMenu, "DIF", '\0', actionListener);
    addMenuItem(saveAsJDXMenu, "DIFDUP", 'U', actionListener);
    addMenuItem(saveAsJDXMenu, "FIX", '\0', actionListener);
    addMenuItem(saveAsJDXMenu, "PAC", '\0', actionListener);
    addMenuItem(saveAsJDXMenu, "SQZ", '\0', actionListener);
    saveAsMenu.add(saveAsJDXMenu);
    addMenuItem(saveAsMenu, "CML", '\0', actionListener);
    addMenuItem(saveAsMenu, "XML (AnIML)", '\0', actionListener);
    if (exportAsMenu != null) {
      exportAsMenu.setText("Export As");
      addMenuItem(exportAsMenu, "JPG", '\0', actionListener);
      addMenuItem(exportAsMenu, "PNG", 'N', actionListener);
      addMenuItem(exportAsMenu, "SVG", '\0', actionListener);
      addMenuItem(exportAsMenu, "PDF", '\0', actionListener);
    }
  }

	private static void addMenuItem(JMenu m, String key, char keyChar,
			ActionListener actionListener) {
		JMenuItem jmi = new JMenuItem();
		jmi.setMnemonic(keyChar == '\0' ? key.charAt(0) : keyChar);
		jmi.setText(key);
		jmi.addActionListener(actionListener);
		m.add(jmi);
	}


}
