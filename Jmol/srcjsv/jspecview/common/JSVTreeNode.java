/**
 * 
 */
package jspecview.common;

import javax.swing.tree.DefaultMutableTreeNode;
import jspecview.common.JSVPanelNode;

public class JSVTreeNode extends DefaultMutableTreeNode {


  private static final long serialVersionUID = 1L;
	
  public JSVPanelNode panelNode;
	public int index;

  public JSVTreeNode(String text, JSVPanelNode panelNode) {
    super(text);
    this.panelNode = panelNode;
  }


}