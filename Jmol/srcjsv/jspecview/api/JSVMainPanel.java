package jspecview.api;

import javajs.util.List;
import jspecview.common.JSViewer;
import jspecview.common.PanelNode;


public interface JSVMainPanel extends JSVViewPanel {

	int getCurrentPanelIndex();
	void setSelectedPanel(JSViewer viewer, JSVPanel jsvp, List<PanelNode> panelNodes);

}
