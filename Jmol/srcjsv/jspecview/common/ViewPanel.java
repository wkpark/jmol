/**
 * 
 */
package jspecview.common;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;

import jspecview.common.JSVContainer;
import jspecview.common.Annotation.AType;

public class ViewPanel extends JPanel implements JSVContainer {

	private static final long serialVersionUID = 1L;
	private JSVPanel selectedPanel;
	private int currentPanelIndex;
	public int getCurrentSpectrumIndex() {
		return currentPanelIndex;
	}

	public ViewPanel(BorderLayout borderLayout) {
		super(borderLayout);
	}

	public void dispose() {
	}

	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTitle(String title) {
		// TODO Auto-generated method stub

	}

	public void setSelectedPanel(JSVPanel jsvp, List<JSVPanelNode> panelNodes) {
		if (jsvp != selectedPanel) {
			if (selectedPanel != null) {
				remove((AwtPanel) selectedPanel);
				//jsvApplet.removeKeyListener((AwtPanel) selectedPanel);
			}
			if (jsvp != null)
  			add((AwtPanel) jsvp, BorderLayout.CENTER);
			//jsvApplet.addKeyListener((AwtPanel) jsvp);
			selectedPanel = jsvp;
		}
		for (int i = panelNodes.size(); --i >= 0;) {
			JSVPanel j = panelNodes.get(i).jsvp;
			if (j == jsvp) {
				currentPanelIndex = i;
			} else {
				j.setEnabled(false);
				j.setFocusable(false);
				j.getPanelData().closeAllDialogsExcept(AType.NONE);
			}
		}
		markSelectedPanels(panelNodes);
		setVisible(jsvp != null);
	}

	public void markSelectedPanels(List<JSVPanelNode> panelNodes) {
		for (int i = panelNodes.size(); --i >= 0;)
			panelNodes.get(i).isSelected = (currentPanelIndex == i);
	}

}