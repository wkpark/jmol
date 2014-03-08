package jspecview.api;


import javajs.api.GenericMenuInterface;
import javajs.util.List;
import jspecview.common.PanelNode;


public interface JSVPopupMenu extends GenericMenuInterface {

	@Override
	void jpiShow(int x, int y);

	void setSelected(String key, boolean b);

	boolean getSelected(String key);

	void setCompoundMenu(List<PanelNode> panelNodes,
			boolean allowCompoundMenu);

	void setEnabled(boolean allowMenu, boolean zoomEnabled);

}
