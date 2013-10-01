package jspecview.common;

import java.awt.Dimension;

import javax.print.attribute.standard.MediaSizeName;

/**
 * <code>PrintLayout</code> class stores all the information needed from the
 * <code>PrintLayoutDialog</code>
 */

public class PrintLayout {
	/**
	 * The paper orientation ("portrait" or "landscape")
	 */
	public String layout = "landscape";
	/**
	 * The position of the graph on the paper
	 * ("center", "default", "fit to page")
	 */
	public String position = "fit to page";
	/**
	 * whether or not the grid should be printed
	 */
	public boolean showGrid = true;
	/**
	 * whether or not the X-scale should be printed
	 */
	public boolean showXScale = true;
	/**
	 * whether or not the Y-scale should be printed
	 */
	public boolean showYScale = true;
	/**
	 * whether or not the title should be printed
	 */
	public boolean showTitle = true;
	/**
	 * The font of the elements
	 */
	public String font;
	/**
	 * The size of the paper to be printed on
	 */
	public MediaSizeName paper;
	public boolean asPDF;
	public static Dimension getDimension(MediaSizeName paper) {
		// ftp://ftp.pwg.org/pub/pwg/media-sizes/pwg-media-size-03.pdf
		// at 72 dpi we have...
		if (paper == MediaSizeName.NA_LETTER) {
			return new Dimension((int) (8.5 * 72), 11 * 72);
		} else if (paper == MediaSizeName.NA_LEGAL) {
			return new Dimension((int) (8.5 * 72), 14 * 72);
		} else if (paper == MediaSizeName.ISO_A4) {
			return new Dimension((int) (210 / 25.4 * 72), (int) (297 / 25.4 * 72));
		} else {// if (paper == MediaSizeName.ISO_B4) {
			return new Dimension((int) (250 / 25.4 * 72), (int) (353 / 25.4 * 72));
		}
	}

}