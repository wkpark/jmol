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
import org.openscience.jmol.FortranFormat;
import org.openscience.jmol.Matrix3D;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.vecmath.Point3f;

public class displayPanel extends Canvas implements java.awt.event.ComponentListener, java.awt.event.ActionListener {
    private String message = "Waiting for structure...";
	private DisplaySettings settings;
    private boolean Perspective=false;
    private float FieldOfView;
    private boolean painted = false;
    private boolean haveFile = false;
    private boolean rubberband = false;
    private boolean AntiAliased = false;
    private int bx, by, rtop, rbottom, rleft, rright;
    private int fileType;
    private int nframes = 0;
	private int frameIndex = -1;
    private int prevx, prevy, outx, outy;
    private float xtheta, ytheta, ztheta;
	private Point3f useMinBound = new Point3f();
	private Point3f useMaxBound = new Point3f();
    private Matrix3D amat = new Matrix3D(); // Matrix to do mouse angular rotations.
    private Matrix3D tmat = new Matrix3D(); // Matrix to do translations.
    private Matrix3D mat = new Matrix3D();  // Final matrix for assembly on screen.
	private boolean matIsValid = false;		// is mat valid ???
	private float zoomFactor=0.7f;
	private java.awt.Label frameLabel = null;
    float[] quat = new float[4];    
    double mtmp[];
    String names[];
    Color colors[];
    private double angle, prevangle;
    ChemFile cf;
    ChemFrame md=null;
    private float xfac0=1;	// Zoom factor determined by screen size/initial settings.
    private float xfac=1;	// Zoom as performed by the user
    private float xfac2=1;	// Square of zoom performed by the user
    public static final int ROTATE = 0;
    public static final int ZOOM = 1;
    public static final int XLATE = 2;
    public static final int PICK = 3;
    public static final int DEFORM = 4;
    public static final int MEASURE = 5;
	public static String customViewPrefix="VIEW.";
	public static String resetViewToken="HOME";
	public static String rotateToken="ROTATE";
	public static String frameToken="FRAME";
	public static String zoomToken="ZOOM";
	public static String translateToken="TRANSLATE";

    private int mode = ROTATE;
    private static Color backgroundColor = (java.awt.Color.black);
    //Added T.GREY for moveDraw support- should be true while mouse is dragged
    private boolean mouseDragged = false;
    private boolean WireFrameRotation = true;
//
//    private boolean showPopupMenu = true;
//    private int popButtonSideLength =10;
//    private javax.swing.JPopupMenu popup;
      private java.awt.Image db=null;

	private int drawWidth;
	private int drawHeight;

    public displayPanel() {
        super();
        resetView();
    //Create the popup menu.
//      popup = createPopupFromString(null);
//      setBackground(java.awt.Color.black);
//      backgroundColor = (java.awt.Color.black);
      setForeground(java.awt.Color.white);
		AtomRenderer.setCanvas(this);
        this.addMouseListener(new MyAdapter());            
        this.addComponentListener(this);
        this.addMouseMotionListener(new MyMotionAdapter());            
        this.addKeyListener(new MyKeyListener());            
//		updateSizes();
    }

	public void updateSizes() {
        java.awt.Dimension s = this.getSize();
		drawWidth=s.width;
		drawHeight=s.height;
		if ((drawWidth>0)&&(drawHeight>0)) {
			db = createImage(drawWidth, drawHeight);
		} else {
			db = null;
		}
		painted=false;
	}

	public void displaySettingsChanged() {
		painted=false;
	}

    public void resetView() {
        amat.unit();
        tmat.unit();
		xfac=1;
		xfac2=1;
		if (settings!=null) {
			settings.setBondScreenScale(xfac0);
			settings.setAtomScreenScale(xfac0);
		}
        quat[0] = 0.0f;
        quat[1] = 0.0f;
        quat[2] = 0.0f;
        quat[3] = 1.0f;                   
		matIsValid=false;
        painted=false;
        if (db!=null) {
			repaint();
		}
    }

	/*
		rotationString is a string which can specify any rotation.
		The string is formatted as "xrot,yrot,xrot".
		There is no Z-rot, since any rotation can be created as a combination
		of X-Y-X rotations, and this way is programmatically much easier !
		Angles are in degrees.
	*/
	public void doRotationString(String rotationString) {
		StringTokenizer st=new StringTokenizer(rotationString, ",", false);
		float rotTheta=0;
		String token;

		try {
			rotTheta=(float) FortranFormat.atof(st.nextToken());
            amat.yrot(rotTheta);
			rotTheta=(float) FortranFormat.atof(st.nextToken());
            amat.xrot(rotTheta);
			rotTheta=(float) FortranFormat.atof(st.nextToken());
            amat.yrot(rotTheta);
		} catch (NoSuchElementException E) {
		}
		matIsValid=false;
        painted=false;
        if (db!=null) {
			repaint();
		}
	}

	public void doZoomString(String zoomString) {
		float zf;

		zf=(float) FortranFormat.atof(zoomString);
		doZoom(zf);
	}

	public void doFrameString(String frameString) {
		int frame;

		frame=Integer.parseInt(frameString);
		setFrame(frame);
	}

	/* Format is "x,y"
	*/
	public void doTranslateString(String translateString) {
		StringTokenizer st=new StringTokenizer(translateString, ",", false);
		float deltax=0;
		float deltay=0;
		String token;
		
		try {
			deltax=(float) FortranFormat.atof(st.nextToken());
			deltay=(float) FortranFormat.atof(st.nextToken());
		} catch (java.util.NoSuchElementException E) {
		}
        tmat.translate(deltax, deltay, 0);
		matIsValid=false;
        painted=false;
        if (db!=null) {
			repaint();
		}
	}

	/* Format is:
		"FRAME=n;ROTATION=x,y,x;ZOOM=n;TRANSLATE=x,y"
	*/
	public void doCustomViewString(String customView) {
		StringTokenizer st=new StringTokenizer(customView, ";", false);
		String viewToken;

//		resetView();
		try {
			while (true) {
				viewToken=st.nextToken();
				processViewString(viewToken);
			}
		} catch (NoSuchElementException E) {
		}
	}

	public void processViewString(String viewToken) {
		StringTokenizer st=new StringTokenizer(viewToken, "=", false);

		String token=st.nextToken();
		String data=null;
		try {
			data=st.nextToken();
		} catch (java.util.NoSuchElementException e) {
		}
		if (token.equals(resetViewToken)) {
			resetView();
		} else if (token.equals(rotateToken)) {
			doRotationString(data);
		} else if (token.equals(frameToken)) {
			doFrameString(data);
		} else if (token.equals(zoomToken)) {
			doZoomString(data);
		} else if (token.equals(translateToken)) {
			doTranslateString(data);
		}
// Ignore unknown tokens.
	}

	public void doZoom(float zoom) {
        xfac *= zoom;
        xfac2 *= zoom*zoom;
		if (settings!=null) {
			settings.setBondScreenScale(xfac2*xfac0);
			settings.setAtomScreenScale(xfac2*xfac0);
		}
        matIsValid=false;               
        painted=false;
        if (db!=null) {
			repaint();
		}
	}

	/*
		This routine sets the initial zoom factor.
	*/
    public void setZoomFactor(float factor) {
        zoomFactor=factor*0.7f;
        if (db!=null) init();
    }

// Make sure AWT knows we are using a buffered image.
	public boolean isDoubleBuffered() {
		return true;
	}

// Make sure AWT knows we repaint the entire canvas.
	public boolean isOpaque() {
		return true;
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
		settings.setBondScreenScale(xfac2*xfac0);
		settings.setAtomScreenScale(xfac2*xfac0);
		painted=false;
    }

//   public boolean getPopupMenuActive(){
//       return showPopupMenu;
//   }

//   public void setPopupMenuActive(boolean visible){
//       showPopupMenu = visible;
//       if (painted){
//         painted = false;
//         repaint();
//       }
//   }
   

    public void setChemFile(ChemFile cf) {
        this.cf = cf;
		painted=false;
        haveFile = true;
        nframes = cf.getNumberFrames();
        this.md = cf.getFrame(0);
        init();
		setFrame(0);
    }

    public void init() {
        md.findBounds();
		useMinBound=md.getMinimumBounds();
		useMaxBound=md.getMaximumBounds();
		updateSizes();
		Point3f size = new Point3f();
		size.sub(useMaxBound, useMinBound);
		float width = size.x;
        if (size.y > width)  width = size.y;
        if (size.z > width)  width = size.z;
        float f1 = drawWidth / width;
		float f2 = drawHeight / width;
        xfac0 = zoomFactor * (f1 < f2 ? f1 : f2);
		matIsValid=false;
		if (settings!=null) {
			settings.setBondScreenScale(xfac2*xfac0);
			settings.setAtomScreenScale(xfac2*xfac0);
		}
    }

// Define a label component which will display the frame label (if one exists)
	public void setFrameLabel(java.awt.Label newLabel) {
		frameLabel=newLabel;
		if ((frameIndex!=-1)&&(frameLabel!=null)) {
			frameLabel.setText(md.getInfo());
			frameLabel.setSize(frameLabel.getPreferredSize());
		}
	}

	public void clearFrameLabel() {
		frameLabel=null;
	}

    public void setFrame(int fr) {
        if (haveFile) {
            if (fr < nframes) {
                md = cf.getFrame(fr);
				frameIndex=fr;
				if (frameLabel!=null) {
					frameLabel.setText(md.getInfo());
					frameLabel.setSize(frameLabel.getPreferredSize());
				}
            }            
			painted=false;
            repaint();
        }
    }

    public ChemFrame getFrame() {
        return md;
    }

/** Sets the status message to read whatever is in msg. 'Status message' here means the text in the corner of the applet.**/
    public void setStatusMessage(String msg){
       message = msg;
       repaint();
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
//            if (showPopupMenu){
//               if (e.getX() < popButtonSideLength){
//                 if (e.getY() < popButtonSideLength){
//                    popup.show(e.getComponent(),e.getX(), e.getY());
//                    return;
//                 }
//               }
//            }
            if (mode == PICK) {
                if (haveFile) {                    
                    if (e.isShiftDown()) {
                        md.shiftSelectAtom(e.getX(), e.getY());
                    } else {
                        md.selectAtom(e.getX(), e.getY());
                    }
					painted=false;
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
                            (2.0f*x - drawWidth) / drawWidth,
                            (drawHeight-2.0f*y) / drawHeight,
                            (2.0f*prevx - drawWidth) / drawWidth,
                            (drawHeight-2.0f*prevy) / drawHeight);
                                
                tb.add_quats(spin_quat, quat, quat);
                tb.build_rotmatrix(amat, quat);                

                */
                xtheta = (prevy - y) * (360.0f / drawWidth);
                ytheta = (x - prevx) * (360.0f / drawHeight);
                
                amat.xrot(xtheta);
                amat.yrot(ytheta);
                matIsValid=false;               
                painted = false;
                repaint();
            }

            if (mode == XLATE) {
                float dx = (x - prevx);
                float dy = (y - prevy);
                tmat.translate(dx, dy, 0);
                matIsValid=false;               
                painted = false;
                repaint();
            }                                                   

            if (mode == ZOOM) {
                float xs = 1.0f + 
                    (float)(x-prevx) / (float)drawWidth;
                float ys = 1.0f + 
                    (float)(prevy-y) / (float)drawHeight;
                float s = (xs + ys)/2.0f;
				doZoom(s);
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
					painted=false;
                    repaint();
                    int n = md.getNpicked();
//                    status.setStatus(2, n + " Atoms Selected");
                }
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
            
    static void setBackgroundColor(Color bg) {
        backgroundColor = bg;
    }
    static Color getBackgroundColor() {
        return backgroundColor;
    }
    public void setForegroundColor(Color fg) {
        setForeground(fg);
		painted=false;
    }
    public Color getForegroundColor() {
        return getForeground();
    }
    public void setPerspective(boolean p) {
        Perspective = p;
		painted=false;
    }
    public boolean getPerspective() {
        return Perspective;
    }
    public void setFieldOfView(float fov) {
        FieldOfView = fov;
		painted=false;
    }
    public float getFieldOfView() {
        return FieldOfView;
    }

    public void update(Graphics g){
        paint(g);
    }

    public void paint(Graphics g){
		Graphics gps;

		if (db == null) {
			return;
       }else if(cf == null) {
          g.drawString(message,10,10);
          return;
		}
		gps=db.getGraphics();
		paintBuffer(gps);
		gps.dispose();		// dispose of graphics to prevent system resource problems.
		g.drawImage(db,0,0,this);
    }


    public void paintBuffer(Graphics g) {
//        if (backgroundColor == null) setBackgroundColor();
        
        Color bg = backgroundColor;
        Color fg = getForeground();
        
        if (md == null) {
            g.setColor(bg);
            g.fillRect(0,0,drawWidth,drawHeight); 
        } else {
			if (!matIsValid) {
// Only rebuild the master matrix if component matrices have changed.
				float xft=xfac2*xfac0;
				painted=false;
				mat.unit();
            	mat.translate(-(useMinBound.x + useMaxBound.x) / 2,
                             -(useMinBound.y + useMaxBound.y) / 2,
                             -(useMinBound.z + useMaxBound.z) / 2);
            	mat.mult(amat);
            	mat.scale(xft, -xft, xft);
            	mat.mult(tmat);
				mat.scale(xfac,xfac,xfac);
            	mat.translate(drawWidth / 2, drawHeight / 2, drawWidth / 2);
				matIsValid=true;
			}
			if (!painted) {
// Only re-render image buffer if image has changed or have new image buffer.
				md.setMat(mat);
		    	settings.setAtomZOffset(drawWidth/2);
			
            	g.setColor(bg);
            	g.fillRect(0,0,drawWidth,drawHeight); 
            	g.setColor(fg);

            	md.paint(g, settings);
            	if (rubberband) {
                	g.setColor(fg);
                	g.drawRect(rleft, rtop, rright-rleft, rbottom-rtop);
            	}                        
//            if (showPopupMenu){
//               g.setColor(fg);
//               g.drawRect(0,0,popButtonSideLength,popButtonSideLength);
//               g.drawLine(0,0,popButtonSideLength,popButtonSideLength);
//               g.drawLine(0,popButtonSideLength,popButtonSideLength,0);
//            }
            	painted = true;
			}
            
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
//      requestFocus();
		init();
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
//     Invoked when component has been shown. 
//      requestFocus();
		init();
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("PREV")){
			if (frameIndex>0) setFrame(frameIndex-1);
        } else if (command.equals("NEXT")){
			if (frameIndex<(nframes-1)) setFrame(frameIndex+1);
        } else if (command.startsWith(customViewPrefix)) {
			doCustomViewString(command.substring(customViewPrefix.length()));
		}
    }

	public void toggleBonds() {
		showBonds(!getShowBonds());
	}

    public boolean getShowBonds(){
       return settings.getShowBonds();
    }

    public void showBonds(boolean doWe){
      settings.setShowBonds(doWe);
      painted = false;
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
