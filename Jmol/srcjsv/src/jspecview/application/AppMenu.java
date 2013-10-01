/* Copyright (C) 2002-2012  The JSpecView Development Team
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

// CHANGES to 'mainFrame.java' - Main Application GUI
// University of the West Indies, Mona Campus
//
// 20-06-2005 kab - Implemented exporting JPG and PNG image files from the application
//                - Need to sort out JSpecViewFileFilters for Save dialog to include image file extensions
// 21-06-2005 kab - Adjusted export to not prompt for spectrum when exporting JPG/PNG
// 24-06-2005 rjl - Added JPG, PNG file filters to dialog
// 30-09-2005 kab - Added command-line support
// 30-09-2005 kab - Implementing Drag and Drop interface (new class)
// 10-03-2006 rjl - Added Locale overwrite to allow decimal points to be recognised correctly in Europe
// 25-06-2007 rjl - Close file now checks to see if any remaining files still open
//                - if not, then remove a number of menu options
// 05-07-2007 cw  - check menu options when changing the focus of panels
// 06-07-2007 rjl - close imported file closes spectrum and source and updates directory tree
// 06-11-2007 rjl - bug in reading displayschemes if folder name has a space in it
//                  use a default scheme if the file can't be found or read properly,
//                  but there will still be a problem if an attempt is made to
//                  write out a new scheme under these circumstances!
// 23-07-2011 jak - altered code to support drawing scales and units separately
// 21-02-2012 rmh - lots of additions  -  integrated into Jmol

package jspecview.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelNode;
import jspecview.common.PanelData;
import jspecview.common.AwtPopupMenu;
import jspecview.common.ScriptToken;
import jspecview.common.JDXSpectrum;
import jspecview.common.AwtPopupMenu.EnumOverlay;
import jspecview.source.JDXSource;
import jspecview.util.JSVFileManager;

/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class AppMenu extends JMenuBar {

  private static final long serialVersionUID = 1L;
  protected MainFrame mainFrame;
  AwtPopupMenu jsvpPopupMenu;

  public AppMenu(MainFrame si, AwtPopupMenu popupMenu) throws Exception {
    this.mainFrame = si;
    jsvpPopupMenu = popupMenu;
    jbInit();
  }

  private JMenu fileMenu = new JMenu();
  private JMenuItem openMenuItem = new JMenuItem();
  private JMenuItem openURLMenuItem = new JMenuItem();
  private JMenuItem simulateMenuItem = new JMenuItem();
  private JMenuItem printMenuItem = new JMenuItem();
  private JMenuItem closeMenuItem = new JMenuItem();
  private JMenuItem closeAllMenuItem = new JMenuItem();
  private JMenu saveAsMenu = new JMenu();
  private JMenu saveAsJDXMenu = new JMenu();
  private JMenu exportAsMenu = new JMenu();
  private JMenuItem exitMenuItem = new JMenuItem();
  private JMenu helpMenu = new JMenu();
  private JMenu optionsMenu = new JMenu();
  private JMenu displayMenu = new JMenu();
  private JMenu zoomMenu = new JMenu();
  JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem revPlotCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleXCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleYCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JMenuItem nextZoomMenuItem = new JMenuItem();
  private JMenuItem prevZoomMenuItem = new JMenuItem();
  private JMenuItem fullZoomMenuItem = new JMenuItem();
  private JMenuItem clearZoomMenuItem = new JMenuItem();
  private JMenuItem userZoomMenuItem = new JMenuItem();
  private JMenuItem preferencesMenuItem = new JMenuItem();
  private JMenuItem contentsMenuItem = new JMenuItem();
  private JMenuItem aboutMenuItem = new JMenuItem();
  private JMenu openRecentMenu = new JMenu();
  private JCheckBoxMenuItem toolbarCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem sidePanelCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem statusCheckBoxMenuItem = new JCheckBoxMenuItem();

  private JMenuItem overlayStackOffsetYMenuItem = new JMenuItem();
  private JMenuItem spectraMenuItem = new JMenuItem();
  private JMenuItem sourceMenuItem = new JMenuItem();
  private JMenuItem propertiesMenuItem = new JMenuItem();

  private JMenuItem scriptMenuItem = new JMenuItem();
  JMenu processingMenu = new JMenu();
  private JMenuItem errorLogMenuItem = new JMenuItem();
  JMenuItem overlayKeyMenuItem;

  /**
   * Initializes GUI components
   * 
   * @throws Exception
   */
  private void jbInit() throws Exception {
    fileMenu.setMnemonic('F');
    fileMenu.setText("File");

    AwtPopupMenu.setMenuItem(openMenuItem, 'O', "Open...", 79,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showFileOpenDialog();
          }
        });
    AwtPopupMenu.setMenuItem(openURLMenuItem, 'U', "Open URL...", 85,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.openURL();
          }
        });
    AwtPopupMenu.setMenuItem(simulateMenuItem, 'I', "Simulate...", 73,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.simulate();
          }
        });
    AwtPopupMenu.setMenuItem(printMenuItem, 'P', "Print...", 80,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.print("");
          }
        });
    AwtPopupMenu.setMenuItem(closeMenuItem, 'C', "Close", 115,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("CLOSE");
          }
        });
    AwtPopupMenu.setMenuItem(closeAllMenuItem, 'L', "Close All", 0,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("CLOSE ALL");
          }
        });
    AwtPopupMenu.setMenuItem(exitMenuItem, 'X', "Exit", 115,
        InputEvent.ALT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.exitJSpecView(false);
          }
        });

    helpMenu.setMnemonic('H');
    helpMenu.setText("Help");

    optionsMenu.setMnemonic('O');
    optionsMenu.setText("Options");

    displayMenu.setMnemonic('D');
    displayMenu.setText("Display");
    displayMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        JSVPanel jsvp = mainFrame.getSelectedPanel();
        if (jsvp == null)
          return;
        gridCheckBoxMenuItem.setSelected(jsvp.getPanelData().getBoolean(ScriptToken.GRIDON));
        coordsCheckBoxMenuItem.setSelected(jsvp.getPanelData().getBoolean(ScriptToken.COORDINATESON));
        revPlotCheckBoxMenuItem.setSelected(jsvp.getPanelData().getBoolean(ScriptToken.REVERSEPLOT));
        jsvpPopupMenu.setEnables(mainFrame.getSelectedPanel());
      }

      public void menuDeselected(MenuEvent e) {
      }

      public void menuCanceled(MenuEvent e) {
      }
    });
    zoomMenu.setMnemonic('Z');
    zoomMenu.setText("Zoom");

    AwtPopupMenu.setMenuItem(gridCheckBoxMenuItem, 'G', "Grid", 71,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.GRIDON, e);
          }
        });
    AwtPopupMenu.setMenuItem(coordsCheckBoxMenuItem, 'C', "Coordinates",
        67, InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.COORDINATESON, e);
          }
        });
    AwtPopupMenu.setMenuItem(revPlotCheckBoxMenuItem, 'R', "Reverse Plot",
        82, InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.REVERSEPLOT, e);
          }
        });
    AwtPopupMenu.setMenuItem(scaleXCheckBoxMenuItem, 'X', "X Scale", 88,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.XSCALEON, e);
          }
        });
    AwtPopupMenu.setMenuItem(scaleYCheckBoxMenuItem, 'Y', "Y Scale", 89,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.YSCALEON, e);
          }
        });
    AwtPopupMenu.setMenuItem(nextZoomMenuItem, 'N', "Next View", 78,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("zoom next");
          }
        });
    AwtPopupMenu.setMenuItem(prevZoomMenuItem, 'P', "Previous View", 80,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("zoom previous");
          }
        });
    AwtPopupMenu.setMenuItem(fullZoomMenuItem, 'F', "Full View", 70,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("zoom out");
          }
        });
    AwtPopupMenu.setMenuItem(clearZoomMenuItem, 'C', "Clear Views", 67,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("zoom clear");
          }
        });
    AwtPopupMenu.setMenuItem(userZoomMenuItem, 'Z', "Set Zoom...", 90,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jsvpPopupMenu.userZoom(mainFrame.getSelectedPanel());
          }
        });
    AwtPopupMenu.setMenuItem(scriptMenuItem, 'T', "Script...", 83,
        InputEvent.ALT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jsvpPopupMenu.script(mainFrame.getSelectedPanel());
          }
        });
    AwtPopupMenu.setMenuItem(preferencesMenuItem, 'P', "Preferences...",
        0, 0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            showPreferencesDialog();
          }
        });
    AwtPopupMenu.setMenuItem(contentsMenuItem, 'C', "Contents...", 112, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showNotImplementedOptionPane();
          }
        });
    AwtPopupMenu.setMenuItem(aboutMenuItem, 'A', "About", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            new AboutDialog(mainFrame);
          }
        });
    openRecentMenu.setActionCommand("OpenRecent");
    openRecentMenu.setMnemonic('R');
    openRecentMenu.setText("Open Recent");

    saveAsMenu.setMnemonic('A');
    saveAsJDXMenu.setMnemonic('J');
    exportAsMenu.setMnemonic('E');

    AwtPopupMenu.setMenuItem(toolbarCheckBoxMenuItem, 'T', "Toolbar", 84,
        InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            mainFrame.enableToolbar(e.getStateChange() == ItemEvent.SELECTED);
          }
        });
    toolbarCheckBoxMenuItem.setSelected(true);

    AwtPopupMenu.setMenuItem(sidePanelCheckBoxMenuItem, 'S', "Side Panel",
        83, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            mainFrame.setSplitPane(e.getStateChange() == ItemEvent.SELECTED);
          }
        });
    sidePanelCheckBoxMenuItem.setSelected(true);

    AwtPopupMenu.setMenuItem(statusCheckBoxMenuItem, 'B', "Status Bar",
        66, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            mainFrame.enableStatus(e.getStateChange() == ItemEvent.SELECTED);
          }
        });
    statusCheckBoxMenuItem.setSelected(true);
    
    AwtPopupMenu.setMenuItem(spectraMenuItem, 'S', "Spectra...", 83,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jsvpPopupMenu.overlay(mainFrame.getSelectedPanel(), AwtPopupMenu.EnumOverlay.DIALOG);
          }
        });
    AwtPopupMenu.setMenuItem(overlayStackOffsetYMenuItem, 'y', "Overlay Offset...", 0,
        0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jsvpPopupMenu.overlay(mainFrame.getSelectedPanel(), EnumOverlay.OFFSETY);
          }
        });
    AwtPopupMenu.setMenuItem(sourceMenuItem, 'S', "Source ...", 83,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            TextDialog.showSource(mainFrame);
          }
        });
    AwtPopupMenu.setMenuItem(propertiesMenuItem, 'P', "Properties", 72,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showProperties();
          }
        });
    overlayKeyMenuItem = jsvpPopupMenu.overlayKeyMenuItem;
    AwtPopupMenu.setMenuItem(overlayKeyMenuItem, '\0', "Overlay Key", 0,
        0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.toggleOverlayKey();
          }
        });

    AwtPopupMenu.setMenuItem(errorLogMenuItem, '\0', "Error Log ...", 0,
        0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            TextDialog.showError(mainFrame);
          }
        });

    processingMenu.setMnemonic('P');
    processingMenu.setText("Processing");
    processingMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        jsvpPopupMenu.setEnables(mainFrame.getSelectedPanel());
      }

      public void menuDeselected(MenuEvent e) {
      }

      public void menuCanceled(MenuEvent e) {
      }
    });
    jsvpPopupMenu.setProcessingMenu(processingMenu);

    add(fileMenu);
    add(displayMenu).setEnabled(false);
    add(optionsMenu);
    add(processingMenu).setEnabled(false);
    add(helpMenu);
    fileMenu.add(openMenuItem);
    fileMenu.add(openURLMenuItem);
    fileMenu.add(openRecentMenu);
    fileMenu.add(simulateMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(closeMenuItem).setEnabled(false);
    fileMenu.add(closeAllMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(scriptMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(saveAsMenu).setEnabled(false);
    fileMenu.add(exportAsMenu).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(printMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(sourceMenuItem).setEnabled(false);
    fileMenu.add(errorLogMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);
    displayMenu.add(spectraMenuItem);
    displayMenu.add(overlayStackOffsetYMenuItem);
    displayMenu.add(overlayKeyMenuItem).setEnabled(false);
    displayMenu.addSeparator();
    displayMenu.add(gridCheckBoxMenuItem);
    displayMenu.add(coordsCheckBoxMenuItem);
    displayMenu.add(scaleXCheckBoxMenuItem);
    displayMenu.add(scaleYCheckBoxMenuItem);
    displayMenu.add(revPlotCheckBoxMenuItem);
    displayMenu.addSeparator();
    displayMenu.add(zoomMenu);
    displayMenu.addSeparator();
    displayMenu.add(propertiesMenuItem);
    zoomMenu.add(nextZoomMenuItem);
    zoomMenu.add(prevZoomMenuItem);
    zoomMenu.add(fullZoomMenuItem);
    zoomMenu.add(clearZoomMenuItem);
    zoomMenu.add(userZoomMenuItem);
    optionsMenu.add(preferencesMenuItem);
    optionsMenu.addSeparator();
    optionsMenu.add(toolbarCheckBoxMenuItem);
    optionsMenu.add(sidePanelCheckBoxMenuItem);
    optionsMenu.add(statusCheckBoxMenuItem);
    helpMenu.add(aboutMenuItem);
    AwtPopupMenu.setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu,
        (new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.exportSpectrumViaMenu(e.getActionCommand());
          }
        }));
  }

  protected void setBoolean(ScriptToken st, ItemEvent e) {
    boolean isOn = (e.getStateChange() == ItemEvent.SELECTED);
    mainFrame.runScript(st + " " + isOn);
  }

  public void setSourceEnabled(boolean b) {
    closeAllMenuItem.setEnabled(b);
    displayMenu.setEnabled(b);
    processingMenu.setEnabled(b);
    printMenuItem.setEnabled(b);
    sourceMenuItem.setEnabled(b);
    errorLogMenuItem.setEnabled(b);
    exportAsMenu.setEnabled(b);
    saveAsMenu.setEnabled(b);
  }

  void setCloseMenuItem(String fileName) {
    closeMenuItem.setEnabled(fileName != null);
    closeMenuItem.setText(fileName == null ? "Close" : "Close " + fileName);
  }

  /**
	 * @param isError 
   * @param isWarningOnly  
	 */
  void setError(boolean isError, boolean isWarningOnly) {
    errorLogMenuItem.setEnabled(isError);
  }

  public void setMenuEnables(JSVPanelNode node) {
    if (node == null) {
      setCloseMenuItem(null);
      setSourceEnabled(false);
    } else {
      setSourceEnabled(true);
      PanelData pd = node.jsvp.getPanelData();
      JDXSpectrum spec = pd.getSpectrum();
      setCheckBoxes(pd);
      overlayKeyMenuItem.setEnabled(pd.getNumberOfGraphSets() > 1);
      setCloseMenuItem(JSVFileManager.getName(node.source.getFilePath()));
      exportAsMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
      saveAsJDXMenu.setEnabled(spec.canSaveAsJDX());
    }

  }

  public boolean toggleOverlayKeyMenuItem() {
    overlayKeyMenuItem.setSelected(overlayKeyMenuItem.isSelected());
    return overlayKeyMenuItem.isSelected();
  }

  ////////// MENU ACTIONS ///////////

  /**
   * Shows the preferences dialog
   * 
   */
  void showPreferencesDialog() {
    mainFrame.showPreferences();
  }

  public void setSelections(boolean sidePanelOn, boolean toolbarOn,
                            boolean statusbarOn, JSVPanel jsvp) {
    // hide side panel if sidePanelOn property is false
    sidePanelCheckBoxMenuItem.setSelected(sidePanelOn);
    toolbarCheckBoxMenuItem.setSelected(toolbarOn);
    statusCheckBoxMenuItem.setSelected(statusbarOn);
    if (jsvp != null)
      setCheckBoxes(jsvp.getPanelData());
  }

  private void setCheckBoxes(PanelData pd) {
    gridCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.GRIDON));
    coordsCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.COORDINATESON));
    revPlotCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.REVERSEPLOT));
    scaleXCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.XSCALEON));
    scaleYCheckBoxMenuItem.setSelected(pd.getBoolean(ScriptToken.YSCALEON));
  }

  public void setRecentMenu(List<String> recentFilePaths) {
    openRecentMenu.removeAll();
    for (int i = 0; i < recentFilePaths.size(); i++) {
      String path = recentFilePaths.get(i);
      JMenuItem menuItem;
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          mainFrame.openFile(((JMenuItem) e.getSource()).getText(), true);
        }
      });
    }
  }

  public void updateRecentMenus(List<String> recentFilePaths) {
    JMenuItem menuItem;
    openRecentMenu.removeAll();
    for (int i = 0; i < recentFilePaths.size(); i++) {
      String path = recentFilePaths.get(i);
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          mainFrame.openFile(((JMenuItem) e.getSource()).getText(), true);
        }
      });
    }
  }

  public void clearSourceMenu(JDXSource source) {
    if (source == null) {
      setMenuEnables(null);
    } else {
      saveAsJDXMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
    }
    //setCloseMenuItem(null);

  }


}
