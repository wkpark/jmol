package jspecview.api;

import javajs.util.List;

import jspecview.api.ScriptInterface;
import jspecview.source.JDXSource;

public interface JSVTree {
	
	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp);

	public JSVTreeNode getRootNode();

	public void setPath(JSVTreePath newTreePath);

	public JSVTreePath newTreePath(Object[] path);

	public void deleteNodes(List<JSVTreeNode> toDelete);

	public JSVTreeNode createTree(int fileCount, JDXSource source,
			JSVPanel[] jsvPanels);

}
