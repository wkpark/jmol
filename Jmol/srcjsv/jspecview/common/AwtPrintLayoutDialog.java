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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

//import javax.print.PrintService;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
//import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

/**
 * Dialog to set print preferences for JSpecview.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class AwtPrintLayoutDialog extends JDialog {

	private static final long serialVersionUID = 1L;
  private TitledBorder titledBorder1;
  private TitledBorder titledBorder2;
  private TitledBorder titledBorder3;
  private TitledBorder titledBorder4;
  private TitledBorder titledBorder5;
  private TitledBorder titledBorder6;
  private TitledBorder titledBorder7;
  private TitledBorder titledBorder8;
  private TitledBorder titledBorder9;
  
  private ButtonGroup layoutButtonGroup = new ButtonGroup();
  private ButtonGroup fontButtonGroup = new ButtonGroup();
  private ButtonGroup positionButtonGroup = new ButtonGroup();

  private PrintLayout pl;
	private PrintLayout plNew;

  private JPanel jPanel1 = new JPanel();
  private JPanel layoutPanel = new JPanel();
  private JPanel positionPanel = new JPanel();
  private JPanel layoutContentPanel = new JPanel();
  private JPanel previewPanel = new JPanel();
  private JPanel fontPanel = new JPanel();
  private JPanel elementsPanel = new JPanel();
  private JPanel jPanel2 = new JPanel();

  private JButton layoutButton = new JButton();
  private JButton previewButton = new JButton();
  private JButton cancelButton = new JButton();
  private JButton printButton = new JButton();
  private JButton pdfButton = new JButton();
  
  private GridBagLayout gridBagLayout7 = new GridBagLayout();
  private GridBagLayout gridBagLayout6 = new GridBagLayout();
  private GridBagLayout gridBagLayout5 = new GridBagLayout();
  private GridBagLayout gridBagLayout4 = new GridBagLayout();
  private GridBagLayout gridBagLayout3 = new GridBagLayout();
  private GridBagLayout gridBagLayout2 = new GridBagLayout();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();

  private JCheckBox scaleXCheckBox = new JCheckBox();
  private JCheckBox scaleYCheckBox = new JCheckBox();
  private JCheckBox gridCheckBox = new JCheckBox();
  private JCheckBox titleCheckBox = new JCheckBox();
  
  private JRadioButton landscapeRadioButton = new JRadioButton();
  private JRadioButton defaultPosRadioButton = new JRadioButton();
  private JRadioButton centerRadioButton = new JRadioButton();
  private JRadioButton portraitRadioButton = new JRadioButton();
  private JRadioButton fitToPageRadioButton = new JRadioButton();
  private JRadioButton chooseFontRadioButton = new JRadioButton();
  private JRadioButton defaultFontRadioButton = new JRadioButton();

  private static JComboBox<String> fontComboBox = new JComboBox<String>();
  private static JComboBox<MediaSizeName> paperComboBox = new JComboBox<MediaSizeName>();
  //private JComboBox printerNameComboBox = new JComboBox();

  /**
   * Initialises a modal <code>PrintLayoutDialog</code> with a default title
   * of "Print Layout".
   * @param frame the parent frame
   * @param pl    null or previous layout
   * @param isJob 
   */
  public AwtPrintLayoutDialog(Frame frame, PrintLayout pl, boolean isJob) {
    super(frame, "Print Layout", true);
    if (pl == null)
      pl = new PrintLayout();
    this.pl = pl;
    try {
      jbInit(isJob);
      setSize(320, 400);
      setResizable(false);
      setVisible(true);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  private static ImageIcon portraitIcon;
  private static ImageIcon landscapeIcon;
  private static ImageIcon previewPortraitCenterIcon;
  private static ImageIcon previewPortraitDefaultIcon;
  private static ImageIcon previewPortraitFitIcon;
  private static ImageIcon previewLandscapeCenterIcon;
  private static ImageIcon previewLandscapeDefaultIcon;
  private static ImageIcon previewLandscapeFitIcon;

	private static void setStaticElements() {
		if (previewLandscapeFitIcon != null)
			return;
		
    paperComboBox.addItem(MediaSizeName.NA_LETTER);
    paperComboBox.addItem(MediaSizeName.NA_LEGAL);
    paperComboBox.addItem(MediaSizeName.ISO_A4);
    paperComboBox.addItem(MediaSizeName.ISO_B4);

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    String allFontNames[] = ge.getAvailableFontFamilyNames();
    for(int i = 0; i < allFontNames.length; i++)
      fontComboBox.addItem(allFontNames[i]);
 		ClassLoader cl = AwtPrintLayoutDialog.class.getClassLoader();
		String path = "jspecview/application/icons/";
		portraitIcon = new ImageIcon(cl.getResource(path + "portrait.gif"));
		landscapeIcon = new ImageIcon(cl.getResource(path + "landscape.gif"));
		previewPortraitCenterIcon = new ImageIcon(cl.getResource(path
				+ "portraitCenter.gif"));
		previewPortraitDefaultIcon = new ImageIcon(cl.getResource(path
				+ "portraitDefault.gif"));
		previewPortraitFitIcon = new ImageIcon(cl.getResource(path
				+ "portraitFit.gif"));
		previewLandscapeCenterIcon = new ImageIcon(cl.getResource(path
				+ "landscapeCenter.gif"));
		previewLandscapeDefaultIcon = new ImageIcon(cl.getResource(path
				+ "landscapeDefault.gif"));
		previewLandscapeFitIcon = new ImageIcon(cl.getResource(path
				+ "landscapeFit.gif"));
	}

	/**
   * Initalises the GUI components
	 * @param isJob 
   * @throws Exception
   */
  private void jbInit(boolean isJob) throws Exception {

    setStaticElements();
    
    titledBorder1 = new TitledBorder("");
    titledBorder2 = new TitledBorder("");
    titledBorder3 = new TitledBorder("");
    titledBorder4 = new TitledBorder("");
    titledBorder5 = new TitledBorder("");
    titledBorder6 = new TitledBorder("");
    titledBorder7 = new TitledBorder("");
    titledBorder8 = new TitledBorder("");
    titledBorder9 = new TitledBorder("");
    titledBorder1.setTitle("Layout");
    titledBorder1.setTitleJustification(2);
    titledBorder2.setTitle("Position");
    titledBorder2.setTitleJustification(2);
    titledBorder3.setTitle("Elements");
    titledBorder3.setTitleJustification(2);
    titledBorder4.setTitle("Font");
    titledBorder4.setTitleJustification(2);
    titledBorder5.setTitle("Preview");
    titledBorder5.setTitleJustification(2);
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    printButton.setToolTipText("");
    printButton.setText("Print");
    printButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(false);
      }
    });
    pdfButton.setToolTipText("");
    pdfButton.setText("Create PDF");
    pdfButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(true);
      }
    });
    titledBorder6.setTitle("Printers");
    titledBorder7.setTitle("Paper");
    titledBorder8.setTitle("Copies");
    layoutPanel.setBorder(titledBorder1);
    layoutPanel.setLayout(gridBagLayout2);
    landscapeRadioButton.setActionCommand("Landscape");
    landscapeRadioButton.setText("Landscape");
    landscapeRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        landscapeRadioButton_actionPerformed(e);
      }
    });
    positionPanel.setBorder(titledBorder2);
    positionPanel.setLayout(gridBagLayout3);
    layoutContentPanel.setLayout(gridBagLayout1);
    scaleXCheckBox.setText("X-Scale");
    scaleYCheckBox.setText("Y-Scale");
    previewPanel.setBorder(titledBorder5);
    previewPanel.setLayout(gridBagLayout6);
    previewButton.setBorder(null);
    previewButton.setIcon(previewLandscapeDefaultIcon);
    gridCheckBox.setText("Grid");
    defaultPosRadioButton.setActionCommand("Default");
    defaultPosRadioButton.setText("Default");
    defaultPosRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        defaultPosRadioButton_actionPerformed(e);
      }
    });
    centerRadioButton.setActionCommand("Center");
    centerRadioButton.setText("Center");
    centerRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        centerRadioButton_actionPerformed(e);
      }
    });
    portraitRadioButton.setActionCommand("Portrait");
    portraitRadioButton.setText("Portrait");
    portraitRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        portraitRadioButton_actionPerformed(e);
      }
    });
    fitToPageRadioButton.setActionCommand("Fit To Page");
    fitToPageRadioButton.setText("Fit to Page");
    fitToPageRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fitToPageRadioButton_actionPerformed(e);
      }
    });
    layoutButton.setBorder(null);
    layoutButton.setIcon(portraitIcon);
    chooseFontRadioButton.setText("Choose font");
    defaultFontRadioButton.setText("Use default");
    fontPanel.setBorder(titledBorder4);
    fontPanel.setLayout(gridBagLayout5);
    titleCheckBox.setText("Title");
    elementsPanel.setBorder(titledBorder3);
    elementsPanel.setLayout(gridBagLayout4);
    jPanel2.setBorder(titledBorder9);
    jPanel2.setLayout(gridBagLayout7);
    titledBorder9.setTitle("Paper");
    titledBorder9.setTitleJustification(2);
    this.getContentPane().add(jPanel1,  BorderLayout.SOUTH);
    if (isJob)
      jPanel1.add(printButton, null);
    jPanel1.add(pdfButton, null);
    jPanel1.add(cancelButton, null);
    this.getContentPane().add(layoutContentPanel,  BorderLayout.CENTER);
    layoutContentPanel.add(previewPanel,   new GridBagConstraints(0, 2, 2, 1, 0.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    previewPanel.add(previewButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(layoutPanel,  new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    layoutPanel.add(portraitRadioButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutPanel.add(landscapeRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutPanel.add(layoutButton, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(positionPanel,  new GridBagConstraints(1, 0, 2, 1, 0.5, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    positionPanel.add(centerRadioButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    positionPanel.add(fitToPageRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    positionPanel.add(defaultPosRadioButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(elementsPanel,            new GridBagConstraints(0, 1, 1, 1, 0.5, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    elementsPanel.add(gridCheckBox, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    elementsPanel.add(scaleXCheckBox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    elementsPanel.add(scaleYCheckBox, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    elementsPanel.add(titleCheckBox, new GridBagConstraints(0, 3, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(fontPanel,          new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    fontPanel.add(defaultFontRadioButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    fontPanel.add(chooseFontRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    fontPanel.add(fontComboBox, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(jPanel2,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    jPanel2.add(paperComboBox,      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    layoutButtonGroup.add(portraitRadioButton);
    layoutButtonGroup.add(landscapeRadioButton);
    positionButtonGroup.add(centerRadioButton);
    positionButtonGroup.add(fitToPageRadioButton);
    positionButtonGroup.add(defaultPosRadioButton);
    fontButtonGroup.add(defaultFontRadioButton);
    fontButtonGroup.add(chooseFontRadioButton);
    
    setDefaults();
  }

	/**
   * Sets the layout to portrait and changes the preview icon according to the
   * position selected
   * @param e the ActionEvent
   */
  void portraitRadioButton_actionPerformed(ActionEvent e) {
    setPreview();
  }

  /**
   * Sets the layout to landscape and changes the preview icon according to the
   * position selected
   * @param e the ActionEvent
   */
  void landscapeRadioButton_actionPerformed(ActionEvent e) {
    setPreview();
  }

  /**
   * Sets the positon to center and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void centerRadioButton_actionPerformed(ActionEvent e) {
  	setPreview();
  }

  /**
   * Sets the positon to "fit to page" and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void fitToPageRadioButton_actionPerformed(ActionEvent e) {
  	setPreview();
  }

  private void setPreview() {
    int layout = " PL".indexOf(layoutButtonGroup.getSelection().getActionCommand().charAt(0));
    layoutButton.setIcon(layout == 1 ? portraitIcon : landscapeIcon);
    int position = " DCF".indexOf(positionButtonGroup.getSelection().getActionCommand().charAt(0));
    ImageIcon icon = null; 
    switch ((layout << 4) + position) {
    default:
    case 0x11:
    	icon = previewPortraitDefaultIcon;
    	break;
    case 0x12:
    	icon = previewPortraitCenterIcon;
    	break;
    case 0x13:
    	icon = previewPortraitFitIcon;
    	break;
    case 0x21:
    	icon = previewLandscapeDefaultIcon;
    	break;
    case 0x22:
    	icon = previewLandscapeCenterIcon;
    	break;
    case 0x23:
    	icon = previewLandscapeFitIcon;
    	break;
    }
    previewButton.setIcon(icon);
	}

	/**
   * Sets the position to default and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void defaultPosRadioButton_actionPerformed(ActionEvent e) {
  	setPreview();
  }

  private void setDefaults() {
    landscapeRadioButton.setSelected(pl.layout.equals("landscape"));
    scaleXCheckBox.setSelected(pl.showXScale);
    scaleYCheckBox.setSelected(pl.showYScale);
    gridCheckBox.setSelected(pl.showGrid);
    titleCheckBox.setSelected(pl.showTitle);    
    defaultPosRadioButton.setSelected(pl.position.equals("default"));
    centerRadioButton.setSelected(pl.position.equals("center"));
    fitToPageRadioButton.setSelected(pl.position.equals("fit to page"));
    defaultFontRadioButton.setSelected(pl.font == null);
    if (pl.font != null)
    	for (int i = fontComboBox.getItemCount(); --i >= 0;) 
    		if (fontComboBox.getItemAt(i).equals(pl.font)) {
    			fontComboBox.setSelectedIndex(i);
    			break;
    		}
  	for (int i = 0; i < paperComboBox.getItemCount(); i++) 
  		if (pl.paper == null || paperComboBox.getItemAt(i).equals(pl.paper)) {
  		  paperComboBox.setSelectedIndex(i);
  			break;
  		}
    setPreview();
	}

	/**
	 * Stored all the layout Information the PrintLayout object and disposes the
	 * dialog
	 * @param asPDF 
	 * 
	 */
	void printButton_actionPerformed(boolean asPDF) {
		plNew = new PrintLayout();
		plNew.layout = layoutButtonGroup.getSelection().getActionCommand()
				.toLowerCase();
		plNew.font = (defaultFontRadioButton.isSelected() ? null
				: (String) fontComboBox.getSelectedItem());
		plNew.position = positionButtonGroup.getSelection().getActionCommand()
				.toLowerCase();
		plNew.showGrid = gridCheckBox.isSelected();
		plNew.showXScale = scaleXCheckBox.isSelected();
		plNew.showYScale = scaleYCheckBox.isSelected();
		plNew.showTitle = titleCheckBox.isSelected();
		plNew.paper = (MediaSizeName) paperComboBox.getSelectedItem();
		// pl.printer = services[printerNameComboBox.getSelectedIndex()];
		// pl.numCopies = ((Integer)numCopiesSpinner.getValue()).intValue();
		plNew.asPDF = asPDF;

		dispose();
	}

  /**
   * Returns the PrintLayout object
   * @return the PrintLayout object
   */
  public PrintLayout getPrintLayout(){
    return plNew;
  }

  /**
   * set the <code>PrintLayout</code> object to null and disposes of the dialog
   * @param e the action (event)
   */
  void cancelButton_actionPerformed(ActionEvent e) {
    dispose();
  }
}
