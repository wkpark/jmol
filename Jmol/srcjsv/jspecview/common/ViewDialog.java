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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 * Dialog for managing overlaying spectra and closing files
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class ViewDialog extends AwtDialog implements WindowListener {

	private static final long serialVersionUID = 1L;
	private ScriptInterface si;
	private List<JSVTreeNode> treeNodes;
	private List<JCheckBox> checkBoxes;
	private JPanel spectrumPanel;
	private Insets cbInsets1;
	private Insets cbInsets2;
	private JButton closeSelectedButton;
	private JButton combineSelectedButton;
	private JButton viewSelectedButton;
  
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
  
	/**
	 * Initialises the <code>IntegralDialog</code> with the given values for minY,
	 * offset and factor
	 * @param si 
	 * 
	 * @param panel
	 *          the parent panel
	 * @param modal
	 *          the modality
	 */
	public ViewDialog(ScriptInterface si, Component panel, boolean modal) {
		this.si = si;
		setTitle("View/Combine/Close Spectra");
		setModal(modal);
		setPosition(panel, getPosXY());
		setResizable(true);
		addWindowListener(this);
		setup();
	}

	@Override
	protected int[] getPosXY() {
		return posXY;
	}

	private void setup() {
    try {
      jbInit();
      pack();
      setVisible(true);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    //bounds = getBounds();
	}

  void jbInit() throws Exception {
    layoutCheckBoxes();
        
    JButton selectAllButton = newJButton();
    JButton selectNoneButton = newJButton();
    combineSelectedButton = newJButton();
    viewSelectedButton = newJButton();
    closeSelectedButton = newJButton();
    JButton doneButton = newJButton();

    selectAllButton.setText("Select All");
    selectAllButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        select(true);
      }
    });
    
    selectNoneButton.setText("Select None");
    selectNoneButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        select(false);
      }
    });
    
    viewSelectedButton.setText("View Selected");
    viewSelectedButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        viewSelected();
      }
    });
    
    combineSelectedButton.setText("Combine Selected");
    combineSelectedButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        combineSelected();
      }
    });
    
    closeSelectedButton.setText("Close Selected");
    closeSelectedButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeSelected();
      }
    });
    
    doneButton.setText("Done");
    doneButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        done();
      }
    });

    Insets buttonInsets = new Insets(5, 5, 5, 5);
    JPanel leftPanel = new JPanel(new GridBagLayout());
    leftPanel.setMinimumSize(new Dimension(150, 300));
    int i = 0;
    addButton(leftPanel, selectAllButton, i++, buttonInsets);
    addButton(leftPanel, selectNoneButton, i++, buttonInsets);
    addButton(leftPanel, viewSelectedButton, i++, buttonInsets);
    addButton(leftPanel, combineSelectedButton, i++, buttonInsets);
    addButton(leftPanel, closeSelectedButton, i++, buttonInsets);
    addButton(leftPanel, doneButton, i++, buttonInsets);
        
    JScrollPane scrollPane = new JScrollPane(spectrumPanel);

    JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setResizeWeight(0);
    mainSplitPane.setRightComponent(scrollPane);
    mainSplitPane.setLeftComponent(leftPanel);

    setPreferredSize(new Dimension(500,350));
    getContentPane().removeAll();
    getContentPane().add(mainSplitPane);//, BorderLayout.CENTER);


    //getContentPane().add(mainPanel);
    checkEnables();
  }

	private void addButton(JPanel leftPanel, JButton selectAllButton, int i,
			Insets buttonInsets) {
    leftPanel.add(selectAllButton, new GridBagConstraints(0, i, 1, 1, 0.0, 0.0,
    		GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets, 0, 0));    
	}

	private JButton newJButton() {
		JButton b = new JButton();
		b.setPreferredSize(new Dimension(120,25));
		return b;
	}

	private void layoutCheckBoxes() {
    checkBoxes = new ArrayList<JCheckBox>();
    treeNodes = new ArrayList<JSVTreeNode>();
    cbInsets1 = new Insets(0, 0, 2, 2);
    cbInsets2 = new Insets(0, 20, 2, 2);
		spectrumPanel = new JPanel(new GridBagLayout());
    addCheckBoxes(((JSVTree) si.getSpectraTree()).getRootNode(), 0, true);
    addCheckBoxes(((JSVTree) si.getSpectraTree()).getRootNode(), 0, false);
	}

	@SuppressWarnings("unchecked")
	private void addCheckBoxes(JSVTreeNode rootNode, int level, boolean addViews) {
		Enumeration<JSVTreeNode> enume = rootNode.children();
    while (enume.hasMoreElements()) {
      JSVTreeNode treeNode = enume.nextElement();
    	JSVPanelNode node = treeNode.panelNode;
    	if (node.isView != addViews)
    		continue;
    	JCheckBox cb = new JCheckBox();
    	cb.setSelected(node.isSelected);
    	String title = node.toString();
    	if (title.indexOf("\n") >= 0)
    		title = title.substring(0, title.indexOf('\n'));
    	cb.setText(title);
    	cb.setActionCommand("" + (treeNodes.size()));
      cb.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          check(e);
        }
      });
      Insets insets = (level < 1 ? cbInsets1 : cbInsets2);
      spectrumPanel.add(cb, new GridBagConstraints(0, checkBoxes.size(), 1, 1, 0.0, 0.0,
      		GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
      treeNode.index = treeNodes.size();
    	treeNodes.add(treeNode);
    	checkBoxes.add(cb);
    	addCheckBoxes(treeNode, level + 1, addViews);
    }
	}

	private void checkEnables() {		
		closeSelectedButton.setEnabled(false);
		for (int i = 0; i < checkBoxes.size(); i++) {
			if (checkBoxes.get(i).isSelected() && treeNodes.get(i).panelNode.jsvp == null) {
				closeSelectedButton.setEnabled(true);
				break;
			}
		}
		
		int n = 0;
		for (int i = 0; i < checkBoxes.size(); i++) {
			if (checkBoxes.get(i).isSelected() && treeNodes.get(i).panelNode.jsvp != null) {
				n++;
			}
		}
		combineSelectedButton.setEnabled(n > 1);
		viewSelectedButton.setEnabled(n == 1);
	}
	
	private boolean checking = false; 
	
	@SuppressWarnings("unchecked")
	protected void check(ActionEvent e) {
		int i = Integer.parseInt(e.getActionCommand());
		JSVTreeNode node = treeNodes.get(i);
		JCheckBox cb = (JCheckBox) e.getSource();
		boolean isSelected = cb.isSelected();
		if (node.panelNode.jsvp == null) {
			if (!checking && isSelected && cb.getText().startsWith("Overlay")) {
				checking = true;
				select(false);
				cb.setSelected(true);
				node.panelNode.isSelected = true;
				checking = false;
			}
			Enumeration<JSVTreeNode> enume = node.children();
			while (enume.hasMoreElements()) {
				JSVTreeNode treeNode = enume.nextElement();
				checkBoxes.get(treeNode.index).setSelected(isSelected);
				treeNode.panelNode.isSelected = isSelected;
				node.panelNode.isSelected = isSelected;
			}
		} else {
			// uncheck all Overlays
			node.panelNode.isSelected = isSelected;
		}
		if (isSelected)
			for (i = treeNodes.size(); --i >= 0;)
				if (treeNodes.get(i).panelNode.isView != node.panelNode.isView) {
					checkBoxes.get(treeNodes.get(i).index).setSelected(false);
					treeNodes.get(i).panelNode.isSelected = false;
				}
		checkEnables();
	}

	protected void select(boolean mode) {
		for (int i = checkBoxes.size(); --i >= 0;) {
			checkBoxes.get(i).setSelected(mode);
			treeNodes.get(i).panelNode.isSelected = mode;
		}
		checkEnables();
	}
	
	protected void combineSelected() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < checkBoxes.size(); i++) {
			JCheckBox cb = checkBoxes.get(i);
			JSVPanelNode node = treeNodes.get(i).panelNode;
			if (cb.isSelected() && node.jsvp != null) {
				if (node.isView) {
					si.setNode(node, true);
					return;
				}
				String label = cb.getText();
				sb.append(" ").append(label.substring(0, label.indexOf(":")));
			}
		}
		JSViewer.execView(si, sb.toString().trim(), false);
		setup();
	}

	protected void viewSelected() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < checkBoxes.size(); i++) {
			JCheckBox cb = checkBoxes.get(i);
			JSVPanelNode node = treeNodes.get(i).panelNode;
			if (cb.isSelected() && node.jsvp != null) {
				if (node.isView) {
					si.setNode(node, true);
					return;
				}
				String label = cb.getText();
				sb.append(" ").append(label.substring(0, label.indexOf(":")));
			}
		}
		JSViewer.execView(si, sb.toString().trim(), false);
		setup();
	}

	protected void closeSelected() {
		si.execClose("selected", false);
    setup();
	}

  protected void done() {
  	dispose();
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
		dispose();
	}

	public void windowDeactivated(WindowEvent arg0) {
		dispose();
	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent arg0) {
		dispose();
	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
