/*
 * @(#)JmolSimpleBean.java    1.0 3/9/99
 *
 * Copyright Thomas James Grey 1999
 * Heavily Based on Jmol.java by J. DANIEL GEZELTER
 *
 * Thomas James Grey grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  THOMAS JAMES GREY AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL THOMAS JAMES GREY OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF THOMAS JAMES GREY HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */
package org.openscience.miniJmol;

//import javax.swing.AbstractAction;
//import javax.swing.Action;
import java.util.Hashtable;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

/**
 * Subset version of JMol which appears as a componant and can be controlled with strings.
**/
public class JmolSimpleBean extends java.awt.Panel implements java.awt.event.ComponentListener{

	private DisplaySettings settings = new DisplaySettings();
    private displayPanel display;
    private ChemFile cf;

    private boolean ready = false;
    private boolean modelReady = false;
    private boolean typesReady = false;

    public JmolSimpleBean(){      
       setLayout(new BorderLayout());

       display = new displayPanel();
       display.addComponentListener(this);
	   display.setDisplaySettings(settings);
       add(display,"Center");
       setBackgroundColour("#FFFFFF");
       setForegroundColour("#000000");
    }

/**
 * Takes the argument, pharses it as CML and sets it as the current model.
 * @param hugeCMLString The whole of the molecule CML as a single string.
 */
    public void setModelToRenderFromCMLString(String hugeCMLString){
        java.io.Reader r = new java.io.StringReader(hugeCMLString);
        cf = loadModel(r, "CML");
        modelReady = true;
        ready = areWeReady();
     }

/**
 * Takes the argument, reads it as a URL and sets it as the current model.
 * If not all the fields have been set then an IllegalStateException is thrown and we return to waiting.
 * @param modelURL The URL of the model we want.
 * @param type Either "XYZ", "CML" or "PDB"
 */
    public void setModelToRenderFromURL(java.net.URL modelURL, String type) throws IllegalStateException, java.io.IOException{
        if(modelURL != null && type != null) {
              java.io.Reader r = new java.io.InputStreamReader(modelURL.openStream());
              cf = loadModel(r, type);
              modelReady = true;
              ready = areWeReady();
        }else{
           throw new RuntimeException("Null string passed to loadModelFromURL");
        }
     }
/**
 * Takes the argument, reads it as a URL and sets it as the current model.
 * If not all the fields have been set then an IllegalStateException is thrown and we return to waiting.
 * @param modelURL The URL of the model we want.
 * @param type Either "XYZ", "CML" or "PDB"
 */
    public void setModelToRenderFromURL(String modelURL, String type) throws IllegalStateException{
        cf = loadModelFromURL(modelURL,type);
        modelReady = true;
        ready = areWeReady();
     }
/**
 * Takes the argument, reads it as a file and sets it as the current model.
 * If not all the fields have been set then an IllegalStateException is thrown and we return to waiting.
 * @param modelFile The filename of the model we want.
 * @param type Either "XYZ", "CML" or "PDB"
 */
    public void setModelToRenderFromFile(String modelFile, String type) throws IllegalStateException, java.io.FileNotFoundException{
        cf = loadModelFromFile(modelFile,type);
        modelReady = true;
        ready = areWeReady();
     }
/**
 * Takes the argument, reads it as a file and allocates this as the current atom types- eg radius etc.
 * @param propertiesFile The filename of the properties we want.
 */
    public void setAtomPropertiesFromFile(String propertiesFile){
        try {
			AtomTypeSet ats1 = new AtomTypeSet();
			ats1.load(new java.io.FileInputStream(propertiesFile));
        } catch(Exception e1) {
			e1.printStackTrace ();
		}
        
        typesReady = true;
        ready = areWeReady();
    }
/**
 * Takes the argument, reads it as a URL and allocates this as the current atom types- eg radius etc.
 * @param propertiesFileURL The URL of the properties we want.
 */
    public void setAtomPropertiesFromURL(String propertiesURL){
        try {
			AtomTypeSet ats1 = new AtomTypeSet();
			java.net.URL url1 = new java.net.URL(propertiesURL);
			ats1.load(url1.openStream());
        } catch(java.io.IOException e1) {
		}
        
        typesReady = true;
        ready = areWeReady();
    }
/**
 * Takes the argument, reads it and allocates this as the current atom types- eg radius etc.
 * @param propertiesURL The URL of the properties we want.
 */
    public void setAtomPropertiesFromURL(java.net.URL propertiesURL){
        try {
			AtomTypeSet ats1 = new AtomTypeSet();
			ats1.load(propertiesURL.openStream());
        } catch(java.io.IOException e1) {
		}
        
        typesReady = true;
        ready = areWeReady();
    }
/**
 * Set the background colour.
 * @param colourInHex The colour in the format #FF0000 for red etc
 */
    public void setBackgroundColour(String colourInHex){
        display.setBackgroundColor(getColourFromHexString(colourInHex));
    }
/**
 * Set the background colour.
 * @param colour The colour
 */
    public void setBackgroundColour(java.awt.Color colour){
        display.setBackgroundColor(colour);
    }
/**
 * Get the background colour.
 */
    public java.awt.Color getBackgroundColour(){
        return display.getBackgroundColor();
    }

/**
 * Set the foreground colour.
 * @param colourInHex The colour in the format #FF0000 for red etc
 */
    public void setForegroundColour(String colourInHex){
        display.setForegroundColor(getColourFromHexString(colourInHex));
    }
/**
 * Set the foreground colour.
 * @param colour The colour
 */
    public void setForegroundColour(java.awt.Color colour){
        display.setForegroundColor(colour);
    }
/**
 * Get the foreground colour.
 */
    public java.awt.Color getForegroundColour(){
        return display.getForegroundColor();
    }

/*
 * Causes the drop down menu button not to be displayed in the corner of the panel.
 * @param TorF if 'T' then button is displayed, if 'F' then it isn't
 *
   public void setPopupMenuButtonShown(String TorF){
      display.setPopupMenuActive(getBooleanFromString(TorF));
   }

*
 * Causes the drop down menu button not to be displayed in the corner of the panel.
 * @param TorF if true then button is displayed, if false then it isn't
 
   public void setPopupMenuButtonShown(boolean TorF){
      display.setPopupMenuActive(TorF);
   }


 * Is the drop down menu button displayed in the corner of the panel?
 *
   public boolean getPopupMenuButtonShown(){
      return display.getPopupMenuActive();
   }
*/
/**
 * Causes Atoms to be shown or hidden.
 * @param TorF if 'T' then atoms are displayed, if 'F' then they aren't.
 */
   public void setAtomsShown(String TorF){
      display.showAtoms(getBooleanFromString(TorF));
   }
/**
 * Causes Atoms to be shown or hidden.
 * @param TorF if true then atoms are displayed, if false then they aren't.
 */
   public void setAtomsShown(boolean TorF){
      display.showAtoms(TorF);
   }
/**
 * Are Atoms to being shown or hidden?
 */
   public boolean getAtomsShown(){
      return display.getShowAtoms();
   }

/**
 * Causes bonds to be shown or hidden.
 * @param TorF if 'T' then atoms are displayed, if 'F' then they aren't.
 */
   public void setBondsShown(String TorF){
      display.showBonds(getBooleanFromString(TorF));
   }
/**
 * Causes bonds to be shown or hidden.
 * @param TorF if true then bonds are displayed, if false then they aren't.
 */
   public void setBondsShown(boolean TorF){
      display.showBonds(TorF);
   }
/**
 * Are bonds being shown or hidden?
 */
   public boolean getBondsShown(){
      return display.getShowBonds();
   }

/**
 * Sets the rendering mode for atoms. Valid values are 'QUICKDRAW', 'SHADED' and 'WIREFRAME'. 
 */
   public void setAtomRenderingStyle(String style){
      if (style.equalsIgnoreCase("QUICKDRAW")){
		  settings.setAtomDrawMode(DisplaySettings.QUICKDRAW);
      }else if (style.equalsIgnoreCase("SHADED")){
		  settings.setAtomDrawMode(DisplaySettings.SHADING);
      }else if (style.equalsIgnoreCase("WIREFRAME")){
		  settings.setAtomDrawMode(DisplaySettings.WIREFRAME);
      }else{
        throw new IllegalArgumentException("Unknown atom rendering style: "+style);
      }
	  display.repaint();
   }
/**
 * Gets the rendering mode for atoms. Values are 'QUICKDRAW', 'SHADED' and 'WIREFRAME'. 
 */
   public String getAtomRenderingStyleDescription(){
      if (settings.getAtomDrawMode()== DisplaySettings.QUICKDRAW){
       return("QUICKDRAW");
      }else if (settings.getAtomDrawMode()== DisplaySettings.SHADING){
       return("SHADED");
      }else if (settings.getAtomDrawMode()== DisplaySettings.WIREFRAME){
       return("WIREFRAME");
      }
      return "NULL";
   }

/**
 * Sets the rendering mode for bonds. Valid values are 'QUICKDRAW', 'SHADED', 'LINE' and 'WIREFRAME'. 
 */
   public void setBondRenderingStyle(String style){
      if (style.equalsIgnoreCase("QUICKDRAW")){
		  settings.setBondDrawMode(DisplaySettings.QUICKDRAW);
      }else if (style.equalsIgnoreCase("SHADED")){
		  settings.setBondDrawMode(DisplaySettings.SHADING);
      }else if (style.equalsIgnoreCase("LINE")){
		  settings.setBondDrawMode(DisplaySettings.LINE);
      }else if (style.equalsIgnoreCase("WIREFRAME")){
		  settings.setBondDrawMode(DisplaySettings.WIREFRAME);
      }else{
        throw new IllegalArgumentException("Unknown bond rendering style: "+style);
      }
	  display.repaint();
   }
/**
 * Gets the rendering mode for bonds. Values are 'QUICKDRAW', 'SHADED', 'LINE' and 'WIREFRAME'. 
 */
   public String getBondRenderingStyleDescription(){
      if (settings.getBondDrawMode()== DisplaySettings.QUICKDRAW){
       return("QUICKDRAW");
      }else if (settings.getBondDrawMode()== DisplaySettings.SHADING){
       return("SHADED");
      }else if (settings.getBondDrawMode()== DisplaySettings.LINE){
       return("LINE");
      }else if (settings.getBondDrawMode()== DisplaySettings.WIREFRAME){
       return("WIREFRAME");
      }
      return "NULL";
   }

/**
 * Sets the rendering mode for labels. Valid values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'. 
 */
   public void setLabelRenderingStyle(String style){
      if (style.equalsIgnoreCase("NONE")){
		  settings.setLabelMode(DisplaySettings.NOLABELS);
      }else if (style.equalsIgnoreCase("SYMBOLS")){
		  settings.setLabelMode(DisplaySettings.SYMBOLS);
      }else if (style.equalsIgnoreCase("TYPES")){
		  settings.setLabelMode(DisplaySettings.TYPES);
      }else if (style.equalsIgnoreCase("NUMBERS")){
		  settings.setLabelMode(DisplaySettings.NUMBERS);
      }else{
        throw new IllegalArgumentException("Unknown label rendering style: "+style);
      }
	  display.repaint();
   }
/**
 * Gets the rendering mode for labels. Values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'. 
 */
   public String getLabelRenderingStyleDescription(){
      if (settings.getLabelMode()== DisplaySettings.NOLABELS){
       return("NONE");
      }else if (settings.getLabelMode()== DisplaySettings.SYMBOLS){
       return("SYMBOLS");
      }else if (settings.getLabelMode()== DisplaySettings.TYPES){
       return("TYPES");
      }else if (settings.getLabelMode()== DisplaySettings.NUMBERS){
       return("NUMBERS");
      }
      return "NULL";
   }

/**
 * Sets whether they view automatically goes to wireframe when they model is rotated.
 * @param doesIt String either 'T' or 'F'
 */
   public void setAutoWireframe(String doesIt){
      display.setWireframeRotation(getBooleanFromString(doesIt));
   }
/**
 * Sets whether they view automatically goes to wireframe when they model is rotated.
 * @param doesIt If true then wireframe rotation is on, otherwise its off.
 */
   public void setAutoWireframe(boolean doesIt){
      display.setWireframeRotation(doesIt);
   }
/**
 * Gets whether the view automatically goes to wireframe when they model is rotated.
 */
   public boolean getAutoWireframe(){
      return display.getWireframeRotation();
   }

/*
 * Sets the menu displayed by the popup. Complex format...<p><p>
 * Basic form is menu item name then the assosiated command (see below)<p>
 * List is comma-delimited with no spaces.<p>
 * If an item ends in &gt; then the next items are a sub-menu.<p>
 * Sub-menu ends with a &lt; <p>
 *
 * <b>Valid commands are:</b><p>
 *    showBonds - toggles whether bonds are shown.<p>
 *    showAtoms - toggles whether atoms are shown.<p>
 *    showVectors - toggles whether vectors are shown.<p>
 *    atomQuickDraw - Sets atom rendering to quickdraw.<p>
 *    atomShadedDraw - Sets atoms to pretty shaded mode.<p>
 *    atomWireframeDraw - Sets atoms to wireframe... <p>
 *    bondQuickDraw - Sets bonds to Quickdraw.<p>
 *    bondShadedDraw - Sets bonds to shaded.<p>
 *    bondWireframeDraw - Sets bonds to wireframe.<p>
 *    bondLineDraw - Sets bonds to lines.<p>
 *    frontView - Sets the view to the front.<p>
 *    topView - Sets the view to the top.<p>
 *    bottomView - see above<p>
 *    rightView - see above<p>
 *    leftView - see above<p>
 *    homeView - See abo... No wait! Sets the view to the origanal default view- ie untranslated or rotated.<p>
 *    noLabels - Turns off atom labels.<p>
 *    symbolLabels - Atoms labeled by element<p>
 *    typesLabels - Atoms labeled by type.<p>
 *    numbersLabels - Atoms numbered.<p>
 *    wireframeRotation - toggles wireframeRotation.<p> <p>
 *    An example: The default string is:<p>
 *    "View&gt;,Front,frontView,Top,topView,Bottom,bottomView,Right,rightView,Left,leftView,Home,homeView,&lt; <p>
 *    ,Atom Style&gt;,Quick Draw,atomQuickDraw,Shaded,atomShadedDraw,WireFrame,atomWireframeDraw,&lt; <p>
 *    ,Bond Style&gt;,Quick Draw,bondQuickDraw,Shaded,bondShadedDraw,Wireframe,bondWireframeDraw,Line,bondLineDraw,&lt; <p>
 *    ,Atom Labels&gt;,None,noLabels,Atomic Symbols,symbolLabels,Atom Types,typesLabels,Atom Number,numbersLabels,&lt; <p>
 *    ,Toggle Wireframe Rotation,wireframeRotation,Toggle Show Atoms,showAtoms,Toggle Show Bonds,showBonds,Toggle Show Vectors,showVectors<p>
 *
 * @param menuDesc Hmmm... See above!
 *
   public void setMenuDescriptionString(String menuDesc){
      display.setMenuDescription(menuDesc);
   }

*/


//Private and unuseful methods
// Causes the model to be displayed. If not all the fields have been set then an IllegalStateException is thrown and we return to waiting.
    private void setDisplayReady() throws IllegalStateException{
      if(!ready){
         whyArentWeReady();
      }
      display.setChemFile(cf);
    }

    private ChemFile loadModelFromURL(String URL, String type){
       if(URL != null && type != null) {
              java.io.Reader r = new java.io.InputStreamReader(getStreamForFile(URL));
              return loadModel(r, type);
       }else{
           throw new RuntimeException("Null string passed to loadModelFromURL");
       }
    }

    private ChemFile loadModelFromFile(String file, String type) throws java.io.FileNotFoundException{
       if(file != null && type != null) {
              java.io.Reader r = new java.io.FileReader(file);
              return loadModel(r, type);
       }else{
           throw new RuntimeException("Null string passed to loadModelFromFile");
       }
    }

    private ChemFile loadModel(java.io.Reader myReader, String type){
          if (!typesReady){
            System.out.println("Atom properties file defaulting to 'AtomTypes' in working directory");
            setAtomPropertiesFromFile("AtomTypes");
          }
          try {
			  ChemFileReader reader = null;
              if (type.equalsIgnoreCase("PDB")) {
				  reader = new PDBFile(myReader);
              }else if (type.equalsIgnoreCase("CML")) {
				  reader = new CMLFile(myReader);
              }else if(type.equalsIgnoreCase("XYZ")) {
				  reader = new XYZFile(myReader);
              }else if(type.equalsIgnoreCase("Gaussian98")) {
				  reader = new Gaussian98Reader(myReader);
              } else {
                  throw new RuntimeException("Unknown file type in loadModel: "+type);
              }
			  return reader.read();
          }catch (java.lang.Exception e){
              throw new RuntimeException("Sorry! Unhelpful Exception in loadModel: "+e);
          }
    }

/**Fetch an InputStream for the following filename specified relative to the documentbase**/
    protected java.io.InputStream getStreamForFile(String filename) {
            try{
	      java.net.URL modelURL = new java.net.URL(filename);
	      return modelURL.openStream();
           }catch (java.net.MalformedURLException e){
              throw new RuntimeException("MalformedURLException: "+e);            
           }catch (java.io.IOException f){
              throw new RuntimeException("IOException in getStreamForFile: "+f);            
           }
     }


    /**
     * returns the ChemFile that we are currently working with
     *
     * @see ChemFile
     */
    public ChemFile getCurrentFile() {
        return cf;
    }

    /**
     * Returns true if passed 'T' and 'F' if passed false. Throws IllegalArgumentException if parameter is not 'T' ot 'F'
     * @param TorF String equal to either TorF
     */
    protected boolean getBooleanFromString(String TorF) {
       if (TorF.equalsIgnoreCase("T")){
         return true;
       }else if (TorF.equalsIgnoreCase("F")){
         return false;
       }else{
         throw new IllegalArgumentException("Boolean string must be 'T' or 'F'");
       }
    }

    /**
     * Turns a string in the form '#RRGGBB' eg. '#FFFFFF' is white, into a colour
     */
     protected java.awt.Color getColourFromHexString(String colourName){
	if (colourName == null || colourName.length() != 7){
           throw new IllegalArgumentException("Colour name: "+colourName+" is either null ot not seven chars long");
	}
	java.awt.Color colour = null;
	try{
	    int red;
	    int green;
	    int blue;
	    String rdColour = "0x"+colourName.substring(1,3);
	    String gnColour = "0x"+colourName.substring(3,5);
	    String blColour = "0x"+colourName.substring(5,7);
	    red = (Integer.decode(rdColour)).intValue();
	    green = (Integer.decode(gnColour)).intValue();
	    blue = (Integer.decode(blColour)).intValue();
	    colour =  new java.awt.Color(red,green,blue);
	} catch(NumberFormatException e){
	    System.out.println("MDLView: Error extracting colour, using white");
	    colour = new java.awt.Color(255,255,255);
	}
	return colour;
    }

    /**
     * Take the given string and chop it up into a series
     * of strings on whitespace boundries.  This is useful
     * for trying to get an array of strings out of the
     * resource file.
     */
    protected String[] tokenize(String input) {
	java.util.Vector v = new java.util.Vector();
	java.util.StringTokenizer t = new java.util.StringTokenizer(input);
	String cmd[];

	while (t.hasMoreTokens())
	    v.addElement(t.nextToken());
	cmd = new String[v.size()];
	for (int i = 0; i < cmd.length; i++)
	    cmd[i] = (String) v.elementAt(i);

	return cmd;
    }

   private boolean areWeReady(){
      return (modelReady && typesReady);
   }

   private void whyArentWeReady() throws IllegalStateException{
      if (ready){
        throw new RuntimeException("Why aren't we ready? We ARE ready!!");
      }else if (!modelReady){
        throw new IllegalStateException("Model has not been set with setCMLToRender or setModelToRender");
      }else if (!typesReady){
        throw new IllegalStateException("Atom types have not been set with setAtomPropertiesFromFile");
      }else{
        throw new IllegalStateException("Serious Bug-a-roo! ready=false but I think we're ready!");
      }
   }
   public void componentHidden(java.awt.event.ComponentEvent e) {}
   public void componentMoved(java.awt.event.ComponentEvent e){}
   public void componentResized(java.awt.event.ComponentEvent e) {
      setDisplayReady();   
   }
   public void componentShown(java.awt.event.ComponentEvent e) {
//       System.out.println("Holy cow- calling setDisplayReady()");
       setDisplayReady();
   }
/**Warning this adds the mouseListener to the canvas itself to allow following of mouse 'on the bean'.**/
   public void addMouseListener(java.awt.event.MouseListener ml){
      display.addMouseListener(ml);
   }

/**Warning this adds the KeyListener to the canvas itself to allow following of key use 'on the bean'.**/
   public void addKeyListener(java.awt.event.KeyListener kl){
      display.addKeyListener(kl);
   }

}
