/*
 * @(#)JmolApplet.java    1.0 3/9/99
 *
 * Copyright Thomas James Grey 1999
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
//MARK
package org.openscience.miniJmol;

import java.awt.event.*;
import org.openscience.jmol.FortranFormat;

public class JmolApplet extends java.applet.Applet implements MouseListener, KeyListener, StatusDisplay{

	JmolSimpleBean myBean;
	int mode;
	int labelMode;
	private String helpMessage = "Keys: S- change style; L- Show labels; B-Toggle Bonds";
	private boolean bondsEnabled=true;
	
	public void init(){
		float zoomFactor=1;
		myBean = new JmolSimpleBean();
		
		String bonds = getParameter("BONDS");
		if (bonds!=null) {
		    if (bonds.equals("OFF")) {
			    myBean.toggleBonds();
		    } else if (bonds.equals("NEVER")) {
			    myBean.toggleBonds();
			    bondsEnabled=false;
			    helpMessage = "Keys: S- change style; L- Show labels";
            }
		}
		String zoom = getParameter("ZOOM");
        if (zoom!=null) {
			zoomFactor = (float) FortranFormat.atof(zoom);
		}
		myBean.setZoomFactor(zoomFactor);
		zoomFactor=1;
		zoom = getParameter("ATOMSIZE");
        if (zoom!=null) {
			zoomFactor = (float) FortranFormat.atof(zoom);
		}
		myBean.setAtomSphereFactor(zoomFactor);
		String customViews=getParameter("CUSTOMVIEWS");
		if (customViews!=null) {
			myBean.setCustomViews(customViews);
		}
		String WFR = getParameter("WIREFRAMEROTATION");
		if ((WFR!=null)&&(WFR.equals("OFF"))) {
			myBean.setWireframeRotation(false);
		}
		
		java.net.URL atURL;
		String atomtypes = getParameter("ATOMTYPES");
		try {
			if (atomtypes==null) {
				AtomTypeSet ats1 = new AtomTypeSet();
				ats1.load(getClass().getResourceAsStream("Data/AtomTypes.txt"));
			} else {
				atURL=new java.net.URL(getDocumentBase(),atomtypes);
				myBean.setAtomPropertiesFromURL(atURL);
			}
		} catch (java.io.IOException ex) {
			System.err.println("Error loading atom types: " + ex);
		}
		String model = getParameter("MODEL");
		if (model != null){
			try {
				ChemFileReader cfr;
				if (getParameter("FORMAT") != null && getParameter("FORMAT").toUpperCase().equals("CMLSTRING")){
					String cmlString = convertEscapeChars(model);
					cfr = ReaderFactory.createReader(new java.io.StringReader(cmlString));
				}else{
					java.net.URL modelURL;
					try{
						modelURL= new java.net.URL(getDocumentBase(),model);
					}catch (java.net.MalformedURLException e){
						throw new RuntimeException(("Got MalformedURL for model: "+e.toString()));
					}
					cfr = ReaderFactory.createReader(new java.io.InputStreamReader(modelURL.openStream()));
				}
				myBean.setModel(cfr.read(this, bondsEnabled));
			}catch(java.io.IOException e){
				e.printStackTrace();
			}
		}
		myBean.addMouseListener(this);
		myBean.addKeyListener(this);
                String bg;
		bg = getParameter("BCOLOUR");
		if (bg != null) {
                    myBean.setBackgroundColour(bg);
		} else {
                    bg = getParameter("BCOLOR");
                    if (bg != null) myBean.setBackgroundColour(bg);
                }
                    
		String fg;
                fg = getParameter("FCOLOUR");
		if (fg != null) {
			myBean.setForegroundColour(fg);
		} else {
                    fg = getParameter("FCOLOR");
                    if (fg != null) myBean.setForegroundColour(fg);
                }
		myBean.setAtomRenderingStyle("QUICKDRAW");
		myBean.setBondRenderingStyle("QUICKDRAW");
		String style = getParameter("STYLE");
		if (style != null){
                    myBean.setAtomRenderingStyle(style);
                    myBean.setBondRenderingStyle(style);
		}
		setLayout(new java.awt.BorderLayout());
		add(myBean,"Center");
	}

	public void setStatusMessage(String statusMessage) {
		showStatus(statusMessage);
	}

	/**
	 * Converts the html escape chars in the input and replaces them
	 * with the required chars. Handles &lt; &gt and &quot;
	 */
	static String convertEscapeChars(String eChars){
        String less = "<";
        char lessThan = less.charAt(0);
        String more = ">";
        char moreThan = more.charAt(0);
        String q = "\"";
        char quote = q.charAt(0);
        String am = "&";
        char amp = am.charAt(0);
        String sc = ";";
        char semi = sc.charAt(0);
        StringBuffer eCharBuffer = new StringBuffer(eChars);
        StringBuffer out = new StringBuffer(0);
//Scan the string for html escape chars and replace them with
        int state = 0;//0=scanning, 1 = reading
        StringBuffer token= new StringBuffer(0);//The escape char we are reading
        for (int position=0;position < eCharBuffer.length();position++){
           char current = eCharBuffer.charAt(position);
           if (state==0){
             if (current==amp){
               state = 1;
//For some reason we have problems with setCharAt so use append
               token = new StringBuffer(0);
               token.append(current);
             }else{
//Copy through to output
               out.append(current);
             }
           }else{
             if (current==semi){
               state = 0;
//Right replace this token
               String tokenString = token.toString();
               if (tokenString.equals("&lt")){
                 out.append(lessThan);
               }else if (tokenString.equals("&gt")){
                 out.append(moreThan);
               }else if (tokenString.equals("&quot")){
                 out.append(quote);
               }
             }else{
               token.append(current);
             }
           }
        }

        String returnValue = out.toString();
        return returnValue;
     }

/** Takes the string and replaces '%' with EOL chars.
* Used by the javascript setModelToRenderFromXYZString- more robust
* than whitescapes you see!
**/
   public String recoverEOLSymbols(String inputString){
        String at = "%";
        char mark = at.charAt(0);
        String dt = ".";
        char dot = dt.charAt(0);
        String min = "-";
        char minus = min.charAt(0);
        String sp = " ";
        char space = sp.charAt(0);
        String nlString = "\n";
        char nl = nlString.charAt(0);//(char)Character.LINE_SEPARATOR;
        StringBuffer eCharBuffer = new StringBuffer(inputString);
        StringBuffer out = new StringBuffer(0);
//Scan the string for & and replace with /n
        boolean lastWasSpace = false;
        for (int position=0;position < eCharBuffer.length();position++){
           char current = eCharBuffer.charAt(position);
             if (current==mark){
//For some reason we have problems with setCharAt so use append
               out.append(nl);
               lastWasSpace = false;
             }else if (current==space){
               if (!lastWasSpace){
                out.append(current);
                lastWasSpace = true;
               }
             }else if (Character.isLetterOrDigit(current)||current==dot||current==minus){
//Copy through to output
               out.append(current);
               lastWasSpace = false;
             }
        }
//No idea why but a space at the very end seems to be unhealthy
        if (lastWasSpace){
          out.setLength(out.length()-1);
        }
        String returnValue = out.toString();
        return returnValue;

   }


	/**
	 * Invoked when the mouse has been clicked on a component. 
	 */
     public void mouseClicked(MouseEvent e) {
       showStatus(helpMessage);
     }

	/**
	 * Invoked when the mouse enters a component. 
	 */
     public void mouseEntered(MouseEvent e) {
       showStatus(helpMessage);
     }

	/**
	 * Invoked when the mouse exits a component. 
	 */
     public void mouseExited(MouseEvent e) {
     }

	/**
	 * Invoked when a mouse button has been pressed on a component. 
	 */
     public void mousePressed(MouseEvent e) {
     }

	/**
	 * Invoked when a mouse button has been released on a component. 
	 */
     public void mouseReleased(MouseEvent e) {
     }

	/**
	 * Invoked when a key has been pressed. 
	 */
     public void keyPressed(KeyEvent e) {
     }

	/**
	 * Invoked when a key has been released. 
	 */
     public void keyReleased(KeyEvent e) {
     }

     public void keyTyped(KeyEvent e) {
         String key = e.getKeyText(e.getKeyChar());
         String keyChar = new Character(e.getKeyChar()).toString();
         if (keyChar.equals("s")||keyChar.equals("S")){
            mode++;
            mode %= 3;
            if (mode == 0){
               showStatus("JmolApplet: Changing rendering style to QUICKDRAW");
               myBean.setAtomRenderingStyle("QUICKDRAW");
               myBean.setBondRenderingStyle("QUICKDRAW");
            }else if (mode == 1){
               showStatus("JmolApplet: Changing rendering style to WIREFRAME");
               myBean.setAtomRenderingStyle("WIREFRAME");
               myBean.setBondRenderingStyle("WIREFRAME");
            }else if (mode == 2){
               showStatus("JmolApplet: Changing rendering style to SHADED");
               myBean.setAtomRenderingStyle("SHADED");
               myBean.setBondRenderingStyle("SHADED");
            }else{
               showStatus("JmolApplet: Changing rendering style to default");
               myBean.setAtomRenderingStyle("WIREFRAME");
               myBean.setBondRenderingStyle("WIREFRAME");
            }
         }else if (keyChar.equals("l")||keyChar.equals("L")){
            labelMode++;
            labelMode %= 4;
            if (labelMode == 0){
               showStatus("JmolApplet: Changing label style to NONE");
               myBean.setLabelRenderingStyle("NONE");
            }else if (labelMode == 1){
               showStatus("JmolApplet: Changing label style to SYMBOLS");
               myBean.setLabelRenderingStyle("SYMBOLS");
            }else if (labelMode == 2){
               showStatus("JmolApplet: Changing label style to TYPES");
               myBean.setLabelRenderingStyle("TYPES");
            }else if (labelMode == 3){
               showStatus("JmolApplet: Changing label style to NUMBERS");
               myBean.setLabelRenderingStyle("NUMBERS");
            }else{
               showStatus("JmolApplet: Changing label style to default");
               myBean.setBondRenderingStyle("NONE");
            }            
         } else if ((bondsEnabled)&&((keyChar.equals("b")||keyChar.equals("B")))) {
			myBean.toggleBonds();
         }
     }

//METHODS FOR JAVASCRIPT
/**
 * <b>For Javascript:<\b> Takes the argument, pharses it as an XYZ file and sets it as the current model.
 * For robustness EOL chars can be ignored and should then be replaced with % symbols.
 * @param hugeXYZString The whole of the molecule XYZ file as a single string.
 * @param aliasedEOL If 'T' then EOL chars should be replaced by % symbols otherwise 'F'.
 */
    public void setModelToRenderFromXYZString(String hugeXYZString, String aliasedEOL){
        aliasedEOL = aliasedEOL.toUpperCase();
        if (aliasedEOL.equals("T")){
           hugeXYZString = recoverEOLSymbols(hugeXYZString);
        }
        try {
            ChemFileReader cfr;
            cfr = ReaderFactory.createReader(new java.io.StringReader(hugeXYZString));
            myBean.setModel(cfr.read(this, bondsEnabled));
	}catch(java.io.IOException e){
            e.printStackTrace();
        }
     }

/**
 * <b>For Javascript:<\b> Takes the argument, pharses it as CML and sets it as the current model.
 * Note that the CML should be straight- it is not necessary to use HTML escape codes.
 * @param hugeCMLString The whole of the molecule CML as a single string.
 */
    public void setModelToRenderFromCMLString(String hugeCMLString){
          try {
	    ChemFileReader cfr;
//            String cmlString = convertEscapeChars(hugeCMLString);
            cfr = ReaderFactory.createReader(new java.io.StringReader(hugeCMLString));
            myBean.setModel(cfr.read(this,bondsEnabled));
	}catch(java.io.IOException e){
            e.printStackTrace();
        }
    }

/**
 * <b>For Javascript:<\b> Takes the argument, reads it as a URL and sets it as the current model.
 * @param modelURL The URL of the model we want.
 */
    public void setModelToRenderFromURL(String modelURLString){
       try {
         ChemFileReader cfr;
         java.net.URL modelURL;
         try{
           modelURL= new java.net.URL(getDocumentBase(),modelURLString);
         }catch (java.net.MalformedURLException e){
           throw new RuntimeException(("Got MalformedURL for model: "+e.toString()));
         }
         cfr = ReaderFactory.createReader(new java.io.InputStreamReader(modelURL.openStream()));
         myBean.setModel(cfr.read(this,bondsEnabled));
       }catch(java.io.IOException e){
         e.printStackTrace();
       }
    }
/**
 * <b>For Javascript:<\b> Takes the argument, reads it as a file and sets it as the current model.
 * @param modelFile The filename of the model we want.
 * @param type Either "XYZ", "CML" or "PDB"
 */
    public void setModelToRenderFromFile(String modelFile, String type){
         setModelToRenderFromFile(modelFile,type);
    }
/**
 * <b>For Javascript:<\b> Takes the argument, reads it as a file and allocates this as the current atom types- eg radius etc.
 * @param propertiesFile The filename of the properties we want.
 */
    public void setAtomPropertiesFromFile(String propertiesFile){
       myBean.setAtomPropertiesFromFile(propertiesFile);
    }
/**
 * <b>For Javascript:<\b> Takes the argument, reads it as a URL and allocates this as the current atom types- eg radius etc.
 * @param propertiesFileURL The URL of the properties we want.
 */
    public void setAtomPropertiesFromURL(String propertiesURL){
       myBean.setAtomPropertiesFromURL(propertiesURL);
    }
/**
 * <b>For Javascript:<\b> Set the background colour.
 * @param colourInHex The colour in the format #FF0000 for red etc
 */
    public void setBackgroundColour(String colourInHex){
        myBean.setBackgroundColour(colourInHex);
    }
/**
 * <b>For Javascript:<\b> Set the foreground colour.
 * @param colourInHex The colour in the format #FF0000 for red etc
 */
    public void setForegroundColour(String colourInHex){
        myBean.setForegroundColour(colourInHex);
    }
/**
 * <b>For Javascript:<\b> Causes Atoms to be shown or hidden.
 * @param TorF if 'T' then atoms are displayed, if 'F' then they aren't.
 */
   public void setAtomsShown(String TorF){
      myBean.setAtomsShown(TorF);
   }
/**
 * <b>For Javascript:<\b> Causes bonds to be shown or hidden.
 * @param TorF if 'T' then atoms are displayed, if 'F' then they aren't.
 */
   public void setBondsShown(String TorF){
      myBean.setBondsShown(TorF);
   }
/**
 * <b>For Javascript:<\b> Sets the rendering mode for atoms. Valid values are 'QUICKDRAW', 'SHADED' and 'WIREFRAME'. 
 */
   public void setAtomRenderingStyle(String style){
       myBean.setAtomRenderingStyle(style);
   }
/**
 * <b>For Javascript:<\b> Gets the rendering mode for atoms. Values are 'QUICKDRAW', 'SHADED' and 'WIREFRAME'. 
 */
   public String getAtomRenderingStyleDescription(){
      return getAtomRenderingStyleDescription();
   }
/**
 * <b>For Javascript:<\b> Sets the rendering mode for bonds. Valid values are 'QUICKDRAW', 'SHADED', 'LINE' and 'WIREFRAME'. 
 */
   public void setBondRenderingStyle(String style){
      myBean.setBondRenderingStyle(style);
   }
/**
 * <b>For Javascript:<\b> Gets the rendering mode for bonds. Values are 'QUICKDRAW', 'SHADED', 'LINE' and 'WIREFRAME'. 
 */
   public String getBondRenderingStyleDescription(){
      return myBean.getBondRenderingStyleDescription();
   }

/**
 * <b>For Javascript:<\b> Sets the rendering mode for labels. Valid values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'. 
 */
   public void setLabelRenderingStyle(String style){
       myBean.setLabelRenderingStyle(style);
   }
/**
 * <b>For Javascript:<\b> Gets the rendering mode for labels. Values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'. 
 */
   public String getLabelRenderingStyleDescription(){
      return myBean.getLabelRenderingStyleDescription();
   }

/**
 * <b>For Javascript:<\b> Sets whether they view automatically goes to wireframe when they model is rotated.
 * @param doesIt String either 'T' or 'F'
 */
   public void setAutoWireframe(String doesIt){
      myBean.setAutoWireframe(doesIt);
   }


}
