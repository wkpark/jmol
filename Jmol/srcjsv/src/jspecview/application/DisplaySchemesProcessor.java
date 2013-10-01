/* Copyright (c) 2002-2008 The University of the West Indies
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

package jspecview.application;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.TreeMap;

import jspecview.common.AwtParameters;
import jspecview.common.Parameters;
import jspecview.common.ScriptToken;
import jspecview.util.JSVFileManager;
import jspecview.util.JSVXmlReader;
import jspecview.util.JSVTextFormat;

/**
 * <code>DisplaySchemesProcessor</code> loads and saves the display schemes of
 * Jspecview. The Schemes are loaded from an XML file and saved in a TreeMap.
 * Also saves the schemes out to XML file after modification
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class DisplaySchemesProcessor {

  /** The Name of the XML file that contains the display schemes */
  private String fileName = "displaySchemes.xml";

  /** The list of displaySchemes that is loaded from file */
  private TreeMap<String, Parameters> displaySchemes;

  private JSVXmlReader reader;

  /**
   * Initialises the <code>DisplaySchemesProcessor</code>
   */
  public DisplaySchemesProcessor() {
    displaySchemes = new TreeMap<String, Parameters>();
  }

  /**
   * Load a default DisplayScheme if xml file not found
   * @return boolean
   */
  public Parameters loadDefault() {
    Parameters dsdef = new Parameters("Default");
    dsdef.setDisplayFont("default");
    dsdef.setColor(ScriptToken.TITLECOLOR, Color.BLACK);
    dsdef.setColor(ScriptToken.UNITSCOLOR, Color.BLACK);
    dsdef.setColor(ScriptToken.SCALECOLOR, Color.BLACK);
    dsdef.setColor(ScriptToken.COORDINATESCOLOR, Color.BLACK);
    dsdef.setColor(ScriptToken.PEAKTABCOLOR, Color.RED);
    dsdef.setColor(ScriptToken.HIGHLIGHTCOLOR, Color.DARK_GRAY);
    dsdef.setColor(ScriptToken.GRIDCOLOR, Color.BLACK);
    dsdef.setColor(ScriptToken.PLOTCOLOR, Color.BLACK);
    dsdef.setColor(ScriptToken.PLOTAREACOLOR, Color.WHITE);
    dsdef.setColor(ScriptToken.BACKGROUNDCOLOR, Color.WHITE);
    dsdef.setColor(ScriptToken.INTEGRALPLOTCOLOR, Color.RED);
    displaySchemes.put("Default", dsdef);
    
    return dsdef;
  }
  
  public Parameters getDefaultScheme(){
	  Parameters ds = displaySchemes.get("Default");
	  if(ds == null){
		  ds = loadDefault();
	  }
	  return ds;
  }



  /**
   * Saves the display schemes to file in XML format
   * @throws IOException
   */
  public void store() throws IOException{
    serializeDisplaySchemes(new FileWriter(fileName));
  }

  /**
   * Returns the list of <code>DisplayScheme</code>s that were loaded
   * @return the list of <code>DisplayScheme</code>s that were loaded
   */
  public TreeMap<String, Parameters> getDisplaySchemes(){
    return displaySchemes;
  }

  public boolean load(InputStream stream) {
    try {
      return load(JSVFileManager.getBufferedReaderForInputStream(stream));
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Loads the display schemes into memory and stores them in a <code>Vector</code>
   * @param dispSchemeFileName the name of the file to load
   * @return true if loaded successfully
   */
  public boolean load(String dispSchemeFileName){
    fileName = dispSchemeFileName;        
    try{
      BufferedReader br = JSVFileManager.getBufferedReaderFromName(fileName, null, "##TITLE");
    	return load(br);
    }
    catch(IOException e){
    	return false;
    }
    
  }

  /**
   * Loads the display schemes into memory and stores them in a
   * <code>Vector</code>
   * @param br 
   * @return true if loaded successfully
   */
  public boolean load(BufferedReader br) {

    reader = new JSVXmlReader(br);
    String defaultDS = "Default";
    Parameters ds = null;
    String attr;
    try {
      while (reader.hasNext()) {
        if (reader.nextEvent() != JSVXmlReader.START_ELEMENT)
          continue;
        String theTag = reader.getTagName();
        if (theTag.equals("displayschemes")) {
          defaultDS = reader.getAttrValue("default");
        }
        if (theTag.equals("displayscheme")) {
          String name = reader.getAttrValue("name");
          ds = new AwtParameters(name);
          if (name.equals(defaultDS))
            ds.setDefault(true);
          displaySchemes.put(name, ds);
        }
        if (ds == null)
          continue;
        if (theTag.equals("font")) {
          attr = reader.getAttrValue("face");
          if (attr.length() > 0)
            ds.setDisplayFont(attr);
        } else {
          if (theTag.equals("coordinateColor"))
            theTag = "coordinatesColor";
          ScriptToken st = ScriptToken.getScriptToken(theTag);
          if (st != ScriptToken.UNKNOWN) {
            Color color = getColor();
            if (color == null) {
              String def;
              switch (st) {
              default:
                def = null;
                break;
              case TITLECOLOR:
                def = "#0000ff";
                break;
              case COORDINATESCOLOR:
                def = "#ff0000";
                break;
              case PEAKTABCOLOR:
                def = "#ff0000";
                break;
              case HIGHLIGHTCOLOR:
                def = "#808080";
                break;
              case SCALECOLOR:
                def = "#660000";
                break;
              case UNITSCOLOR:
                def = "#ff0000";
                break;
              case GRIDCOLOR:
                def = "#4e4c4c";
                break;
              case PLOTCOLOR:
                def = "#ff9900";
                break;
              case PLOTAREACOLOR:
                def = "#333333";
                break;
              case BACKGROUNDCOLOR:
                def = "#c0c0c0";
                break;
              }
              if (def != null)
                color = Color.decode(def);
            }
            if (color != null)
              ds.setColor(st, color);
          }
        }
      }
    } catch (IOException e) {
      return false;
    }
    return true;
  }
  
  /**
   * Gets a hex color value from the attribute of a tag and returns a
   * <code>Color</code>
   * @return Returns a <code>Color</code> from the attribute
   */
  private Color getColor(){
    String value = reader.getAttrValueLC("hex");
    return (value.length() == 0 || value.equals("default") ? null
        : AwtParameters.getColorFromString(value));
  }

  /**
   * Serializes the display schemes to the given writer
   * @param writer the writer for the output
   * @throws IOException
   */
  public void serializeDisplaySchemes(Writer writer) throws IOException{
    if(displaySchemes.size() == 0){
      return;
    }

    // find the default scheme
    // set default attr
    StringWriter sw = new StringWriter();
    BufferedWriter buffer = new BufferedWriter(sw);
    String defaultDSName = "";

    for (Parameters ds: displaySchemes.values()) {
      if(ds.isDefault())
        defaultDSName = ds.getName();

      buffer.write("\t<displayScheme name=\"" + ds.getName() + "\">");
      buffer.newLine();
      buffer.write("\t\t<font face = \"" + ds.getDisplayFont() + "\"/>");
      buffer.newLine();
      buffer.write("\t\t<titleColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.TITLECOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<scaleColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.SCALECOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<unitsColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.UNITSCOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<coordinateColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.COORDINATESCOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<highlightColor hex = \"" +
          AwtParameters.colorToHexString(ds.getColor(ScriptToken.HIGHLIGHTCOLOR)) +
          "\"/>");
      buffer.newLine();
      buffer.write("\t\t<peakTabColor hex = \"" +
          AwtParameters.colorToHexString(ds.getColor(ScriptToken.PEAKTABCOLOR)) +
          "\"/>");
      buffer.newLine();
      buffer.write("\t\t<gridColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.GRIDCOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<plotColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.PLOTCOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<plotAreaColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.PLOTAREACOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<backgroundColor hex = \"" +
                   AwtParameters.colorToHexString(ds.getColor(ScriptToken.BACKGROUNDCOLOR)) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t</displayScheme>");
      buffer.newLine();
    }

    buffer.write("</displaySchemes>");
    buffer.flush();

    StringBuffer outBuffer = new StringBuffer();
    outBuffer.append("<?xml version=\"1.0\"?>" + JSVTextFormat.newLine);
    outBuffer.append("<displaySchemes default=\""+ defaultDSName +"\">" + JSVTextFormat.newLine);
    outBuffer.append(sw.getBuffer());

    writer.write(outBuffer.toString());
    writer.flush();
    writer.close();
  }

}
