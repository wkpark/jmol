/*
 * @(#)displayPanel.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
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
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
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

import org.openscience.jmol.DisplaySettings;
import java.awt.*;
import java.awt.event.*;
import javax.vecmath.Point3f;

public class displayPanel extends Canvas implements java.awt.event.ComponentListener{
    private String message = "Waiting for structure...";
	private DisplaySettings settings;
    private boolean Perspective;
    private float FieldOfView;
    private boolean painted = false;
    private boolean initialized = false;
    private boolean haveFile = false;
    private boolean rubberband = false;
    private boolean AntiAliased = false;
    private int bx, by, rtop, rbottom, rleft, rright;
    private int fileType;
    private int nframes = 0;
    private int prevx, prevy, outx, outy;
    private float xtheta, ytheta, ztheta;
    private Matrix3D amat = new Matrix3D(); // Matrix to do mouse angular rotations.
    private Matrix3D tmat = new Matrix3D(); // Matrix to do translations.
    private Matrix3D zmat = new Matrix3D(); // Matrix to do zooming.
    private Matrix3D mat = new Matrix3D();  // Final matrix for assembly on screen.
    float[] quat = new float[4];    
    double mtmp[];
    String names[];
    Color colors[];
    private double angle, prevangle;
    ChemFile cf;
    ChemFrame md;
    private float xfac;
    private float scalefudge = 1;
    public static final int ROTATE = 0;
    public static final int ZOOM = 1;
    public static final int XLATE = 2;
    public static final int PICK = 3;
    public static final int DEFORM = 4;
    public static final int MEASURE = 5;
    private int mode = ROTATE;
    private Color backgroundColor = null;
    //Added T.GREY for moveDraw support- should be true while mouse is dragged
    private boolean mouseDragged = false;
    private boolean WireFrameRotation = true;
      private java.awt.Image db;

    public displayPanel() {
      super();
    //Create the popup menu.
      setBackground(java.awt.Color.black);
      backgroundColor = (java.awt.Color.black);
      setForeground(java.awt.Color.white);
		AtomRenderer.setCanvas(this);
        this.addMouseListener(new MyAdapter());            
        this.addComponentListener(this);
        this.addMouseMotionListener(new MyMotionAdapter());            
        this.addKeyListener(new MyKeyListener());            
        java.awt.Dimension s = this.getSize();
		db = createImage(s.width, s.height);
    }

/** Sets the status message to read whatever is in msg. 'Status message' here means the text in the corner of the applet.**/
    public void setStatusMessage(String msg){
       message = msg;
       repaint();
    }

    public boolean getAntiAliased() {
        return AntiAliased;
    }

    public void setAntiAliased(boolean aa) {
        AntiAliased = aa;
    }
    
    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setDisplaySettings(DisplaySettings settings) {
        this.settings = settings;
    }


    public void setChemFile(ChemFile cf) {
        this.cf = cf;
        haveFile = true;
        nframes = cf.getNumberFrames();
        this.md = cf.getFrame(0);
        init();
    }

    public void init() {
        md.findBounds();
		Point3f size = new Point3f();
		size.sub(md.getMaximumBounds(), md.getMinimumBounds());
		float width = size.x;
        if (size.y > width)  width = size.y;
        if (size.z > width)  width = size.z;
        float f1 = getSize().width / width;
		float f2 = getSize().height / width;
        xfac = 0.7f * (f1 < f2 ? f1 : f2) * scalefudge;
		settings.setBondScreenScale(xfac);
		settings.setAtomScreenScale(xfac);
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setScreenScale(xfac);
        }
        repaint();
    }


    public void setFrame(int fr) {
        if (haveFile) {
            if (fr < nframes) {
                md = cf.getFrame(fr);
            }            
            repaint();
        }
    }

    public ChemFrame getFrame() {
        return md;
    }
    
    class MyAdapter extends MouseAdapter {

        public void mouseEntered(MouseEvent e) {
            requestFocus();
        }

        public void mousePressed(MouseEvent e) {
            prevx = e.getX();
            prevy = e.getY();
            
            if (mode == PICK) {
                rubberband = true;
                bx = e.getX();
                rright = bx;
                rleft = bx;
                by = e.getY();
                rtop = by;
                rbottom = by;
            }
            
        }
        public void mouseClicked(MouseEvent e) {
            if (mode == PICK) {
                if (haveFile) {                    
                    if (e.isShiftDown()) {
                        md.shiftSelectAtom(e.getX(), e.getY());
                    } else {
                        md.selectAtom(e.getX(), e.getY());
                    }
                    repaint();
                    int n = md.getNpicked();
                }
            }
                   
        }
            
        public void mouseReleased(MouseEvent e) {

            //NEW LINE T.GREY
            if(mouseDragged && WireFrameRotation){
				settings.setFastRendering(false);
                if (painted) {
                    painted = false;
                    repaint();
                }
                mouseDragged = false;
            }
            
            outx = e.getX();
            outy = e.getY();
            
            if (mode == PICK) {
                rubberband = false;
                repaint();
            }
            
        }
    }
    
    class MyMotionAdapter extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            //NEW LINE T.GREY
            if (WireFrameRotation) {
				settings.setFastRendering(true);
                mouseDragged = true;            
            }
            if (mode == ROTATE) {

                /*                   
                float[] spin_quat = new float[4];
                Trackball tb = new Trackball(spin_quat,
                            (2.0f*x - getSize().width) / getSize().width,
                            (getSize().height-2.0f*y) / getSize().height,
                            (2.0f*prevx - getSize().width) / getSize().width,
                            (getSize().height-2.0f*prevy) / getSize().height);
                                
                tb.add_quats(spin_quat, quat, quat);
                tb.build_rotmatrix(amat, quat);                

                */
                xtheta = (prevy - y) * (360.0f / getSize().width);
                ytheta = (x - prevx) * (360.0f / getSize().height);
                
                amat.xrot(xtheta);
                amat.yrot(ytheta);
                
                mat.mult(amat);
            }

            if (mode == XLATE) {
                float dx = (x - prevx);
                float dy = (y - prevy);
                tmat.translate(dx, dy, 0);
                mat.mult(tmat);
            }                                                   

            if (mode == ZOOM) {
                float xs = 1.0f + 
                    (float)(x-prevx) / (float)getSize().width;
                float ys = 1.0f + 
                    (float)(prevy-y) / (float)getSize().height;
                float s = (xs + ys)/2.0f;
                zmat.scale(s, s, s);
                mat.mult(zmat);
                xfac *= s*s;
				settings.setBondScreenScale(xfac);
				settings.setAtomScreenScale(xfac);
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setScreenScale(xfac);
        }
            }
            
            if (mode == PICK) {
                if (x < bx) { 
                    rleft = x; rright= bx;
                } else {
                    rleft = bx; rright = x;
                }
                if (y < by) { 
                    rtop = y; rbottom= by;
                } else {
                    rtop = by; rbottom = y;
                }
                if (haveFile) {                    
                    if (e.isShiftDown()) {
                        md.shiftSelectRegion(rleft, rtop, rright, rbottom);
                    } else {
                        md.selectRegion(rleft, rtop, rright, rbottom);
                    }
                    repaint();
                    int n = md.getNpicked();
//                    status.setStatus(2, n + " Atoms Selected");
                }
            }

            if (painted) {
                painted = false;
                repaint();
            }
            prevx = x;
            prevy = y;
        }
    }

    class MyKeyListener extends java.awt.event.KeyAdapter{
        public void keyPressed(java.awt.event.KeyEvent e){
          if (e.getKeyCode() == e.VK_SHIFT){
             mode = ZOOM;
          }else if (e.getKeyCode() == e.VK_CONTROL){
             mode = XLATE;
          }
        }

        public void keyReleased(java.awt.event.KeyEvent e){
             mode = ROTATE;
        }
    }

    public void rebond() throws Exception {
        if (md != null) {
            md.rebond();
        }
    }
            
    public void setBackgroundColor(Color bg) {
        backgroundColor = bg;
        super.setBackground(bg);
    }
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    public void setForegroundColor(Color fg) {
        setForeground(fg);
    }
    public Color getForegroundColor() {
        return getForeground();
    }
    public void setPerspective(boolean p) {
        Perspective = p;
    }
    public boolean getPerspective() {
        return Perspective;
    }
    public void setFieldOfView(float fov) {
        FieldOfView = fov;
    }
    public float getFieldOfView() {
        return FieldOfView;
    }

    public void update(Graphics g){
        paint(g);
    }

    public void paint(Graphics g){
       if (db == null){
         return;
       }else if(cf == null) {
          g.drawString(message,10,10);
          return;
        }
       paintBuffer(db.getGraphics());
       g.drawImage(db,0,0,this);
    }


    public void paintBuffer(Graphics g) {
        
        Color bg;
        if (backgroundColor == null){
          bg=Color.black;
        }else{
          bg = backgroundColor;
        }
        Color fg = getForeground();
        
        if (md == null) {
            g.setColor(bg);
            g.fillRect(0,0,getSize().width,getSize().height); 
        } else {
            if (!initialized) {
                /*
                  start with the unit matrix
                */
                amat.unit();
                tmat.unit();
                zmat.unit();
                quat[0] = 0.0f;
                quat[1] = 0.0f;
                quat[2] = 0.0f;
                quat[3] = 1.0f;                   
                initialized = true;
            }
           for (int i=0;i<nframes;i++){
            cf.getFrame(i).setScreenScale(xfac);
            cf.getFrame(i).matunit();      
            cf.getFrame(i).mattranslate(-(md.getMinimumBounds().x + md.getMaximumBounds().x) / 2,
                             -(md.getMinimumBounds().y + md.getMaximumBounds().y) / 2,
                             -(md.getMinimumBounds().z + md.getMaximumBounds().z) / 2);
            cf.getFrame(i).matmult(amat);
            cf.getFrame(i).matscale(xfac, -xfac, xfac);
            cf.getFrame(i).matmult(tmat);
            cf.getFrame(i).matmult(zmat);
            cf.getFrame(i).mattranslate(getSize().width / 2, 
                                   getSize().height / 2, 
                                   getSize().width / 2);
        }
		   settings.setAtomZOffset(getSize().width/2);
			
            g.setColor(bg);
            g.fillRect(0,0,getSize().width,getSize().height); 
            g.setColor(fg);

            md.paint(g, settings);
            if (rubberband) {
                g.setColor(fg);
                g.drawRect(rleft, rtop, rright-rleft, rbottom-rtop);
            }                        
            painted = true;
            
        }
    }

    public boolean isPainting() {
        return !painted;
    }

    public void componentHidden(java.awt.event.ComponentEvent e){
//     Invoked when component has been hidden. 
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {
//     Invoked when component has been moved. 
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
//    Invoked when component has been resized. 
        requestFocus();
        java.awt.Dimension s = this.getSize();
        db = createImage(s.width, s.height);
        init();
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
//     Invoked when component has been shown. 
        requestFocus();
        java.awt.Dimension s = this.getSize();
        db = createImage(s.width, s.height);
        init();
    }

   /**
     * Take the given string and chop it up into a series
     * of strings on whitespace boundries.  This is useful
     * for trying to get an array of strings out of the
     * resource file.
     * Shamlessly nicked from Jmol!
     */
    protected String[] tokenize(String input) {
	java.util.Vector v = new java.util.Vector();
	java.util.StringTokenizer t = new java.util.StringTokenizer(input,",",false);
	String cmd[];

	while (t.hasMoreTokens())
	    v.addElement(t.nextToken());
	cmd = new String[v.size()];
	for (int i = 0; i < cmd.length; i++)
	    cmd[i] = (String) v.elementAt(i);

	return cmd;
    }

    public boolean getShowBonds(){
       return md.getShowBonds();
    }

    public void showBonds(boolean doWe){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setShowBonds(doWe);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void showAtoms(boolean doWe){
          for (int i=0;i<nframes;i++){
            cf.getFrame(i).setShowAtoms(doWe);
          }
		  settings.setDrawBondsToAtomCenters(!doWe);
      if (painted){
        painted = false;
      }
      repaint();
    }

    public boolean getShowAtoms(){
      return md.getShowAtoms();
    }

    public void setWireframeRotation(boolean OnOrOff){
            WireFrameRotation = OnOrOff;
    }

    public boolean getWireframeRotation(){
           return WireFrameRotation;
    }

}
