/*
 * @(#)JmolXMLComponentTester.java    1.0 3/9/99
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

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.WindowEvent;

public class JmolBeanTest{

      static public void main(String[] args){
          JFrame myFrame = new JFrame();
          JmolSimpleBean myXMLThing1 = new JmolSimpleBean();
          JmolSimpleBean myXMLThing2 = new JmolSimpleBean();
          JPanel p2 = new JPanel(new BorderLayout());
          p2.add(myXMLThing2,"Center");
          p2.add(javax.swing.Box.createVerticalStrut(400),"East");
          p2.add(javax.swing.Box.createHorizontalStrut(400),"North");
          myFrame.getContentPane().setLayout(new BorderLayout());
          myFrame.getContentPane().add(myXMLThing1,"Center");
          myFrame.getContentPane().add(javax.swing.Box.createHorizontalStrut(400),"North");
          myFrame.getContentPane().add(p2,"East");
          try{
             myXMLThing1.setModelToRenderFromFile("caffeine.xyz","XYZ");
             myXMLThing2.setModelToRenderFromFile("methanol1.cml","CML");
          }catch(java.io.FileNotFoundException e){
            System.out.println("File not found: "+e);
          }
          myFrame.pack();
          myFrame.show();
      }


      class Windowa implements java.awt.event.WindowListener{
        public void windowClosing(java.awt.event.WindowEvent e){
          System.exit(0);
        }

        public void windowOpened(WindowEvent e){}
        public void windowClosed(WindowEvent e){ }
        public void windowIconified(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowActivated(WindowEvent e){} 
        public void windowDeactivated(WindowEvent e){} 
      }

      public java.awt.Dimension getMinimumSize(){
        return new java.awt.Dimension(200,200);
      }
}
