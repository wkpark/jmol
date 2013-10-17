package javajs.swing;

import javajs.api.GenericColor;
import javajs.awt.Dimension;
import jspecview.util.JSVColorUtil;

abstract public class JComponent {
	
	protected boolean visible;	
	protected boolean enabled;
	protected String text;
	protected String name;
	protected int width;
	protected int height;
	protected String id;

	Object actionListener;

	private GenericColor bgcolor;

	protected JComponent(String type) {
		if (type == null)
			return;
		/**
		 * @j2sNative
		 *            
		 *            Jmol.Dialog.register(this, type);
		 */
		{
			id = type + ("" + Math.random()).substring(3);
		}

	}
	
	abstract public String toHTML();
	
	public void setBackground(GenericColor color) {
		bgcolor = color;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPreferredSize(Dimension dimension) {
		this.width = dimension.width;
		this.height = dimension.height;		
	}

	/** 
	 * It will be the function of the JavaScript on the 
	 * page to do with selectionListener what is desired.
	 * @param listener 
	 * 
	 */
	public void addActionListener(Object listener) {
		actionListener = listener;
	}

	public String getText() {
		return text;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.setVisible(this);
		 * 
		 */
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	protected int minWidth = 30;
	protected int minHeight = 30;

	public void setMinimumSize(Dimension d) {
		minWidth = d.width;
		minHeight = d.height;
	}

	protected int getSubcomponentWidth() {
		return width;
	}
	
	protected int getSubcomponentHeight() {
		return height;
	}
	
	protected int renderWidth;
	protected int renderHeight;

	protected String getCSSstyle(int defaultPercent) {
		int width = (renderWidth > 0 ? renderWidth : getSubcomponentWidth());
		int height = (renderHeight > 0 ? renderHeight : getSubcomponentHeight());
		return (width > 0 ? "width:" + width +"px;" : defaultPercent > 0 ? "width:"+defaultPercent+"%;" : "")
		+ (height > 0 ?"height:" + height + "px;" : defaultPercent > 0 ? "height:"+defaultPercent+"%;" : "")
		+ (bgcolor == null ? "" : "background-color:" + JSVColorUtil.colorToCssString(bgcolor) + ";");
	}
}
