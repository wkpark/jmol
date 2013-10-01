package jspecview.common;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.util.JSVColorUtil;

public class AwtParameters extends Parameters {

  public AwtParameters(String name) {
    super(name);
    setDefaultColors(defaultColors);
    setParamDefaults();
    plotColors = new Color[defaultPlotColors.length];
    System.arraycopy(defaultPlotColors, 0, plotColors, 0, defaultPlotColors.length);
  }

  static final Object[] defaultColors = new Object[] { 
    Color.BLACK, Color.RED, Color.BLACK, Color.RED, 
    Color.LIGHT_GRAY, Color.BLUE, Color.WHITE, 
    new Color(192, 192, 192), Color.RED, Color.RED, Color.darkGray };
 
  public final static Color[] defaultPlotColors = { 
    Color.blue, 
    AwtParameters.getColorFromString("darkGreen"),
    AwtParameters.getColorFromString("darkred"),
    AwtParameters.getColorFromString("orange"),
    AwtParameters.getColorFromString("magenta"),
    AwtParameters.getColorFromString("cyan"),
    AwtParameters.getColorFromString("maroon"),
    AwtParameters.getColorFromString("darkGray"),
  };


  @Override
  protected Object getPlotColors(String plotColorsStr) {
    if (plotColorsStr == null) {
      ((Color[]) plotColors)[0] = (Color) getColor(ScriptToken.PLOTCOLOR);
      return plotColors;
    }
    StringTokenizer st = new StringTokenizer(plotColorsStr, ",;.- ");
    List<Color> colors = new ArrayList<Color>();
    try {
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        colors.add(AwtParameters.getColorFromString(token));
      }
    } catch (NoSuchElementException nsee) {
      return null;
    } catch (NumberFormatException nfe) {
      return null;
    }
    return colors.toArray(new Color[colors.size()]);
  }

  @Override
  protected Object setColorFromString(ScriptToken st, String value) {
    return setColor(st, AwtParameters.getColorFromString(value));
  }

  @SuppressWarnings("incomplete-switch")
	@Override
  protected Object getFontName(ScriptToken st, String value) {
    switch (st) {
    case TITLEFONTNAME:
      GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
      List<String> fontList = Arrays.asList(g.getAvailableFontFamilyNames());
      for (String s : fontList)
        if (value.equalsIgnoreCase(s)) {
          titleFont = value;
          break;
        }
      return titleFont;
    case DISPLAYFONTNAME:
      GraphicsEnvironment g2 = GraphicsEnvironment
          .getLocalGraphicsEnvironment();
      List<String> fontList2 = Arrays.asList(g2.getAvailableFontFamilyNames());
      for (String s2 : fontList2)
        if (value.equalsIgnoreCase(s2)) {
          displayFont = value;
          break;
        }
      return displayFont;
    }
    return null;
  }

  /**
   * Returns a hex string representation of a <code>Color</color> object
   * 
   * @param oColor
   *        the Color
   * @return a hex string representation of a <code>Color</color> object
   */
  public static String colorToHexString(Object oColor) {
    if (oColor == null)
      return "";
    Color color = (Color) oColor;
    String r = Integer.toHexString(color.getRed());
    if (r.length() == 1)
      r = "0" + r;
    String g = Integer.toHexString(color.getGreen());
    if (g.length() == 1)
      g = "0" + g;
    String b = Integer.toHexString(color.getBlue());
    if (b.length() == 1)
      b = "0" + b;
    return "#" + r + g + b;
  }

  /**
   * Returns a <code>Color</code> from a string representation as a hex value or
   * a delimiter separated rgb values. The following are all valid arguments:
   * 
   * <pre>
   * "#ffffff"
   * "#FFFFFF"
   * "255 255 255"
   * "255,255,255"
   * "255;255;255"
   * "255-255-255"
   * "255.255.255"
   * </pre>
   * @param strColor 
   * 
   * @return a <code>Color</code> from a string representation
   */
  public static Color getColorFromString(String strColor) {
    return new Color(JSVColorUtil.getArgbFromString(strColor.trim()));
  }

}
