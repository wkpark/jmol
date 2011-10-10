package org.openscience.jmolandroid.api;

import android.graphics.Paint;

public class AndroidFont {

	Paint paint;
	
	public AndroidFont(Paint paint) {
		this.paint = paint;
	}

	public int getAscent() {
	}

	public int getDescent() {
		return Math.abs((int)paint.getFontMetrics().descent);
	}

	public int getLeading() {
		return Math.abs((int)paint.getFontMetrics().leading);
	}

	public int calculateWidth(String text) {
		return (int)paint.measureText(text);
	}

}
