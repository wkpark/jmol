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
package org.openscience.miniJmol;

public class JmolApplet extends java.applet.Applet implements java.awt.event.MouseListener, java.awt.event.KeyListener{

      JmolSimpleBean myBean;
      int mode;
      int labelMode;
      private String helpMessage = "Keys: Tab- change style; L- Show labels";

      public void init(){
          myBean = new JmolSimpleBean();
          String format= getParameter("FORMAT");
          format = format.toUpperCase();
          if (format == null){
              throw new RuntimeException("Please specify a format with <PARAM NAME=FORMAT VALUE=whatever>");
          }else{
              verifyFormat(format);
          }

          String atomtypes = getParameter("ATOMTYPES");
          if (atomtypes == null){
            atomtypes = "AtomTypes";
          }
          java.net.URL atURL;
          try{
            atURL= new java.net.URL(getDocumentBase(),atomtypes);
          }catch (java.net.MalformedURLException e){
            throw new RuntimeException(("Got MalformedURL for Atomtypes: "+e.toString()));
          }
          myBean.setAtomPropertiesFromURL(atURL);

          String model = getParameter("MODEL");
          if (model == null){
            throw new RuntimeException("No model specified, use <PARAM NAME=MODEL VALUE=whatever>");
          }
          if (format.equals("CMLSTRING")){
            String cmlString = convertEscapeChars(model);
            myBean.setModelToRenderFromCMLString(cmlString);
          }else{
            java.net.URL modelURL;
            try{
              modelURL= new java.net.URL(getDocumentBase(),model);
            }catch (java.net.MalformedURLException e){
              throw new RuntimeException(("Got MalformedURL for model: "+e.toString()));
            }
            try{
               myBean.setModelToRenderFromURL(modelURL,format);
            }catch(java.io.IOException e){
              System.out.println("IOException: "+e);
            }
          }
         myBean.addMouseListener(this);
         myBean.addKeyListener(this);
         String bg = getParameter("BCOLOUR");
         if (bg != null){myBean.setBackgroundColour(bg);}
         String fg = getParameter("FCOLOUR");
         if (fg != null){myBean.setForegroundColour(fg);}
         String style = getParameter("STYLE");
         if (style != null){
          myBean.setAtomRenderingStyle(style);
          myBean.setBondRenderingStyle(style);
         }
         setLayout(new java.awt.BorderLayout());
         add(myBean,"Center");
          myBean.setAtomRenderingStyle("SHADED");
      }

      private void verifyFormat(String format){
         if (format.equals("CML")||format.equals("CMLSTRING")||format.equals("XYZ")||format.equals("PDB")){
             return;
         }else{
             throw new RuntimeException("Format: "+format+" is not a valid value (CML, CMLSTRING, XYZ, PDB)");
         }
      }
/**Converts the html escape chars in the input and replaces them with the required chars. Handles &lt; &gt and &quot; **/
     protected String convertEscapeChars(String eChars){
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
//        int outPosition = 0;
//Scan the string for html escape chars and replace them with
        int state = 0;//0=scanning, 1 = reading
        StringBuffer token= new StringBuffer(0);//The escape char we are reading
//        int tokenPosition = 0;//Where to put next char in the token
        for (int position=0;position < eCharBuffer.length();position++){
           char current = eCharBuffer.charAt(position);
           if (state==0){
             if (current==amp){
               state = 1;
//               tokenPosition =0;
//For some reason we have problems with setCharAt so use append
               token = new StringBuffer(0);
               token.append(current);
//               tokenPosition++;
             }else{
//Copy through to output
               out.append(current);
//               outPosition++;               
             }
           }else{
             if (current==semi){
               state = 0;
//Right replace this token
               String tokenString = token.toString();
               if (tokenString.equals("&lt")){
                 out.append(lessThan);
//                 outPosition++;                                
               }else if (tokenString.equals("&gt")){
                 out.append(moreThan);
//                 outPosition++;                                
               }else if (tokenString.equals("&quot")){
                 out.append(quote);
//                 outPosition++;                                
               }
             }else{
               token.append(current);
//               tokenPosition++;
             }
           }
        }

        String returnValue = out.toString();
//        System.out.println("After: "+returnValue);
        return returnValue;
     }

     public void mouseClicked(java.awt.event.MouseEvent e) {
//     Invoked when the mouse has been clicked on a component. 
//       requestFocus();
       showStatus(helpMessage);
     }

     public void mouseEntered(java.awt.event.MouseEvent e) {
//     Invoked when the mouse enters a component. 
//       requestFocus();
       showStatus(helpMessage);
     }

     public void mouseExited(java.awt.event.MouseEvent e) {
//     Invoked when the mouse exits a component. 
     }

     public void mousePressed(java.awt.event.MouseEvent e) {
//     Invoked when a mouse button has been pressed on a component. 
     }

     public void mouseReleased(java.awt.event.MouseEvent e) {
//     Invoked when a mouse button has been released on a component. 
     }

     public void keyPressed(java.awt.event.KeyEvent e) {
//     Invoked when a key has been pressed. 
     }
     public void keyReleased(java.awt.event.KeyEvent e) {
//     Invoked when a key has been released. 
     }
     public void keyTyped(java.awt.event.KeyEvent e) {
//       System.out.println("CHAR: "+e.getKeyChar());
         String key = e.getKeyText(e.getKeyChar());
         String keyChar = new Character(e.getKeyChar()).toString();
         if (key.equals("Tab")){
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
         }
     }

}
