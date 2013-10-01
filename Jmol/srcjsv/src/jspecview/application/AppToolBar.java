package jspecview.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelNode;
import jspecview.common.PanelData;
import jspecview.common.ScriptToken;

public class AppToolBar extends JToolBar {

  private static final long serialVersionUID = 1L;
  MainFrame mainFrame;

  public AppToolBar(MainFrame mainFrame) {
    this.mainFrame = mainFrame;
    jbInit();
  }

  private JButton previousButton = new JButton();
  private JButton nextButton = new JButton();
  private JButton resetButton = new JButton();
  private JButton clearButton = new JButton();
  private JButton openButton = new JButton();
  private JButton propertiesButton = new JButton();
  private JButton errorLogButton = new JButton();
  JToggleButton gridToggleButton = new JToggleButton();
  JToggleButton coordsToggleButton = new JToggleButton();
  private JButton printButton = new JButton();
  private JToggleButton revPlotToggleButton = new JToggleButton();
  private JButton aboutButton = new JButton();
  private JButton spectraButton = new JButton();
  private JButton overlayKeyButton = new JButton();

  private ImageIcon openIcon;
  private ImageIcon printIcon;
  private ImageIcon gridIcon;
  private ImageIcon coordinatesIcon;
  private ImageIcon reverseIcon;
  private ImageIcon previousIcon;
  private ImageIcon nextIcon;
  private ImageIcon resetIcon;
  private ImageIcon clearIcon;
  private ImageIcon informationIcon;
  private ImageIcon aboutIcon;
  private ImageIcon spectrumIcon;
  //private ImageIcon splitIcon;
  private ImageIcon overlayKeyIcon;
  private ImageIcon errorLogIcon;
  private ImageIcon errorLogYellowIcon;
  private ImageIcon errorLogRedIcon;

  private void getIcons() {
    Class<? extends AppToolBar> cl = getClass();
    openIcon = new ImageIcon(cl.getResource("icons/open24.gif"));
    printIcon = new ImageIcon(cl.getResource("icons/print24.gif"));
    gridIcon = new ImageIcon(cl.getResource("icons/grid24.gif"));
    coordinatesIcon = new ImageIcon(cl.getResource("icons/coords24.gif"));
    reverseIcon = new ImageIcon(cl.getResource("icons/reverse24.gif"));
    previousIcon = new ImageIcon(cl.getResource("icons/previous24.gif"));
    nextIcon = new ImageIcon(cl.getResource("icons/next24.gif"));
    resetIcon = new ImageIcon(cl.getResource("icons/reset24.gif"));
    clearIcon = new ImageIcon(cl.getResource("icons/clear24.gif"));
    informationIcon = new ImageIcon(cl.getResource("icons/information24.gif"));
    aboutIcon = new ImageIcon(cl.getResource("icons/about24.gif"));
    spectrumIcon = new ImageIcon(cl.getResource("icons/overlay24.gif"));
    //splitIcon = new ImageIcon(cl.getResource("icons/split24.gif"));
    overlayKeyIcon = new ImageIcon(cl.getResource("icons/overlayKey24.gif"));
    errorLogIcon = new ImageIcon(cl.getResource("icons/errorLog24.gif"));
    errorLogRedIcon = new ImageIcon(cl.getResource("icons/errorLogRed24.gif"));
    errorLogYellowIcon = new ImageIcon(cl
        .getResource("icons/errorLogYellow24.gif"));
  }


  public void setSelections(JSVPanel jsvp) {
    if (jsvp != null) {   
      PanelData pd = jsvp.getPanelData();
      gridToggleButton.setSelected(pd.getBoolean(ScriptToken.GRIDON));
      coordsToggleButton.setSelected(pd.getBoolean(ScriptToken.COORDINATESON));
      revPlotToggleButton.setSelected(pd.getBoolean(ScriptToken.REVERSEPLOT));
    }
  }

  private void jbInit() {
    getIcons();
    setButton(previousButton, "Previous View", previousIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("zoom previous");
          }
        });
    setButton(nextButton, "Next View", nextIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.runScript("zoom next");
      }
    });
    setButton(resetButton, "Reset", resetIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.runScript("zoom out");
      }
    });
    setButton(clearButton, "Clear Views", clearIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.runScript("zoom clear");
      }
    });

    setButton(openButton, "Open", openIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.showFileOpenDialog();
      }
    });
    setButton(propertiesButton, "Properties", informationIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showProperties();
          }
        });
    setButton(errorLogButton, "Error Log", errorLogIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TextDialog.showError(mainFrame);
      }
    });

    setButton(gridToggleButton, "Toggle Grid", gridIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setBoolean(ScriptToken.GRIDON, e);
        mainFrame.requestRepaint();
      }
    });
    setButton(coordsToggleButton, "Toggle Coordinates", coordinatesIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setBoolean(ScriptToken.COORDINATESON, e);
            mainFrame.requestRepaint();
          }
        });
    setButton(printButton, "Print", printIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.print("");
      }
    });
    setButton(revPlotToggleButton, "Reverse Plot", reverseIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setBoolean(ScriptToken.REVERSEPLOT, e);
            mainFrame.requestRepaint();
          }
        });
    setButton(aboutButton, "About JSpecView", aboutIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new AboutDialog(mainFrame);
      }
    });
    setButton(spectraButton, "Overlay Display", spectrumIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            spectrumButton_actionPerformed(e);
          }
        });
    setButton(overlayKeyButton, "Display Key for Overlaid Spectra",
        overlayKeyIcon, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.toggleOverlayKey();
          }
        });
    overlayKeyButton.setEnabled(false);

    add(openButton, null);
    add(printButton, null);
    addSeparator();
    add(gridToggleButton, null);
    add(coordsToggleButton, null);
    add(revPlotToggleButton, null);
    addSeparator();
    add(previousButton, null);
    add(nextButton, null);
    add(resetButton, null);
    add(clearButton, null);
    addSeparator();
    add(spectraButton, null);
    add(overlayKeyButton, null);
    addSeparator();
    add(propertiesButton, null);
    add(errorLogButton, null);
    errorLogButton.setVisible(true);
    addSeparator();
    add(aboutButton, null);

  }

  private static void setButton(AbstractButton button, String tip,
                                ImageIcon icon, ActionListener actionListener) {
    button.setBorder(null);
    button.setToolTipText(tip);
    button.setIcon(icon);
    button.addActionListener(actionListener);
  }

  void setError(boolean isError, boolean isWarningOnly) {
    errorLogButton.setIcon(isWarningOnly ? errorLogYellowIcon
        : isError ? errorLogRedIcon : errorLogIcon);
    errorLogButton.setEnabled(isError);
  }

  protected void setBoolean(ScriptToken st, ActionEvent e) {
    boolean isOn = ((JToggleButton) e.getSource()).isSelected();
    mainFrame.runScript(st + " " + isOn);
  }

  public void setMenuEnables(JSVPanelNode node) {
    if (node == null)
      return;
    setSelections(node.jsvp);
    spectraButton.setIcon(spectrumIcon);
    spectraButton.setToolTipText("View Spectra");
  }   
  
  /**
	 * @param e  
	 */
  protected void spectrumButton_actionPerformed(ActionEvent e) {
    mainFrame.checkOverlay();
  }

}
