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

import java.awt.*;
import java.awt.event.*;
//import javax.swing.*;
//import javax.swing.text.*;

public class displayPanel extends Canvas implements Runnable, java.awt.event.ComponentListener{
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
    private float xfac, xmin, xmax, ymin, ymax, zmin, zmax;
    private float scalefudge = 1;
    public static final int ROTATE = 0;
    public static final int ZOOM = 1;
    public static final int XLATE = 2;
    public static final int PICK = 3;
    public static final int DEFORM = 4;
    public static final int MEASURE = 5;
    private int mode = ROTATE;
    private static Color backgroundColor = null;
    //Added T.GREY for moveDraw support- should be true while mouse is dragged
    private boolean mouseDragged = false;
    private boolean WireFrameRotation = false;
//
//    private boolean showPopupMenu = true;
//    private int popButtonSideLength =10;
//    private javax.swing.JPopupMenu popup;
      private java.awt.Image db;
      private int width;
      private int height;

    public displayPanel() {
      super();
    //Create the popup menu.
//      popup = createPopupFromString(null);
      setBackground(java.awt.Color.black);
      backgroundColor = (java.awt.Color.black);
      setForeground(java.awt.Color.white);
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
   
    public void start() {
        new Thread(this).start();
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setCanvas(this);
        }
        this.addMouseListener(new MyAdapter());            
        this.addComponentListener(this);
        this.addMouseMotionListener(new MyMotionAdapter());            
        this.addKeyListener(new MyKeyListener());            
        java.awt.Dimension s = this.getSize();
        width = s.width;
        height = s.height;
       db = createImage(width, height);
    }

    public void setChemFile(ChemFile cf) {
        this.cf = cf;
        haveFile = true;
        nframes = cf.nFrames();
        this.md = cf.getFrame(0);
        init();
    }

    public void init() {
        md.findBB();
        xmin = md.xmin;
        xmax = md.xmax;
        ymin = md.ymin;
        ymax = md.ymax;
        zmin = md.zmin;
        zmax = md.zmax;
        float xw = md.xmax - md.xmin;
        float yw = md.ymax - md.ymin;
        float zw = md.zmax - md.zmin;
        if (yw > xw)
            xw = yw;
        if (zw > xw)
            xw = zw;
        float f1 = getSize().width / xw;
        float f2 = getSize().height / xw; 
        xfac = 0.7f * (f1 < f2 ? f1 : f2) * scalefudge;
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomScreenScale(xfac);
          cf.getFrame(i).setBondScreenScale(xfac);
        }
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setScreenScale(xfac);
        }
//
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
    
    public void run() {
        try {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY); 
        } catch (Exception e){
            e.printStackTrace();
        } 
        repaint();
    }

    public void stop() {
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
                    repaint();
                    int n = md.getNpicked();
                }
            }
                   
        }
            
        public void mouseReleased(MouseEvent e) {

            //NEW LINE T.GREY
            if(mouseDragged && WireFrameRotation){
                md.setMovingDrawMode(false);
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
                md.setMovingDrawMode(true);
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
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomScreenScale(xfac);
          cf.getFrame(i).setBondScreenScale(xfac);
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
            
    static void setBackgroundColor(Color bg) {
        backgroundColor = bg;
    }
    static void setBackgroundColor() {
        setBackgroundColor(backgroundColor);
    }
    static Color getBackgroundColor() {
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

    public int getAtomRenderMode(){
       return md.atoms[0].getRenderMode();
    }

    public int getBondRenderMode(){
       return md.bonds[0].getRenderMode();
    }

    public int getLabelMode(){
       return md.atoms[0].getLabelMode();
    }

    public void update(Graphics g){
        paint(g);
    }

    public void paint(Graphics g){
       paintBuffer(db.getGraphics());
       g.drawImage(db,0,0,this);
    }


    public void paintBuffer(Graphics g) {
        if (backgroundColor == null) setBackgroundColor();
        
        Color bg = backgroundColor;
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
            cf.getFrame(i).mattranslate(-(xmin + xmax) / 2,
                             -(ymin + ymax) / 2,
                             -(zmin + zmax) / 2);
            cf.getFrame(i).matmult(amat);
            cf.getFrame(i).matscale(xfac, -xfac, xfac);
            cf.getFrame(i).matmult(tmat);
            cf.getFrame(i).matmult(zmat);
            cf.getFrame(i).mattranslate(getSize().width / 2, 
                                   getSize().height / 2, 
                                   getSize().width / 2);
            cf.getFrame(i).setZoffset(getSize().width/2);
        }

            g.setColor(bg);
            g.fillRect(0,0,getSize().width,getSize().height); 
            g.setColor(fg);

            md.paint(g);
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
        width = s.width;
        height = s.height;
        db = createImage(width, height);
    }

    public void componentShown(java.awt.event.ComponentEvent e) {
//     Invoked when component has been shown. 
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

   /*"Sets the menu description- see JmolSimpleBean!
    public void setMenuDescription(String menuString){
       popup = createPopupFromString(menuString);
    }

*/
    // The actions:
/*    static final String showBondsCommand = "showBonds";
    static final String showAtomsCommand = "showAtoms";
    static final String showVectorsCommand = "showVectors";

    static final String rendermodeAtomQuickDrawCommand = "atomQuickDraw";
    static final String rendermodeAtomShadedCommand = "atomShadedDraw";
    static final String rendermodeAtomWireframeCommand = "atomWireframeDraw";
    static final String rendermodeBondQuickDrawCommand = "bondQuickDraw";
    static final String rendermodeBondShadedCommand = "bondShadedDraw";
    static final String rendermodeBondWireframeCommand = "bondWireframeDraw";
    static final String rendermodeBondLineCommand = "bondLineDraw";

    static final String frontCommand = "frontView";
    static final String topCommand = "topView";
    static final String bottomCommand = "bottomView";
    static final String rightCommand = "rightView";
    static final String leftCommand = "leftView";
    static final String homeCommand = "homeView";

    static final String labelsNoneCommand = "noLabels";
    static final String labelsSymbolsCommand = "symbolLabels";
    static final String labelsTypesCommand = "typesLabels";
    static final String labelsNumbersCommand = "numbersLabels";

    static final String wireframeRotationCommand = "wireframeRotation";
*/
//    private javax.swing.JPopupMenu createPopupFromString(String menuDesc){
//        if (menuDesc == null){
//           menuDesc =("View>,Front,frontView,Top,topView,Bottom,bottomView,Right,rightView,Left,leftView,Home,homeView,<"
//                    + ",Atom Style>,Quick Draw,atomQuickDraw,Shaded,atomShadedDraw,WireFrame,atomWireframeDraw,<"
//                    + ",Bond Style>,Quick Draw,bondQuickDraw,Shaded,bondShadedDraw,Wireframe,bondWireframeDraw,Line,bondLineDraw,<"
//                    + ",Atom Labels>,None,noLabels,Atomic Symbols,symbolLabels,Atom Types,typesLabels,Atom Number,numbersLabels,<"
//                    + ",Toggle Wireframe Rotation,wireframeRotation,Toggle Show Atoms,showAtoms,Toggle Show Bonds,showBonds,Toggle Show Vectors,showVectors");
//        }
//        String[] menuStrings = tokenize(menuDesc);
//        javax.swing.JPopupMenu r = new javax.swing.JPopupMenu();
//        for(int t=0; t<menuStrings.length;t++){
//          if(menuStrings[t].endsWith(">")){
//Sub menu
//             javax.swing.JMenu menu = new javax.swing.JMenu(menuStrings[t].substring(0,menuStrings[t].length()-1));
//             t++;
//             while(!menuStrings[t].equals("<")){
//               javax.swing.JMenuItem menusubItem = new javax.swing.JMenuItem(menuStrings[t]);
//               t++;
//               menusubItem.setActionCommand(menuStrings[t]);
//               menusubItem.addActionListener(this);
//               menu.add(menusubItem);
//               t++;
//             }
//             r.add(menu);
//          }else{
//Ordinary item
//             javax.swing.JMenuItem menuItem = new javax.swing.JMenuItem(menuStrings[t]);
//             t++;
//             menuItem.setActionCommand(menuStrings[t]);
//             menuItem.addActionListener(this);
//             r.add(menuItem);
//          }
//       }
//       return r;
//    }
/*
    public void actionPerformed(java.awt.event.ActionEvent e) {

      String command = e.getActionCommand();
      if (command.equals(showBondsCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).toggleBonds();
        }
      }else if (command.equals(showAtomsCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).toggleAtoms();
          cf.getFrame(i).toggleBondsToAtomCenters();
        }
      }else if (command.equals(showVectorsCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).toggleVectors();
        }
      }else if (command.equals(rendermodeAtomQuickDrawCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomRenderMode(AtomType.QUICKDRAW);
        }
      }else if (command.equals(rendermodeAtomShadedCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomRenderMode(AtomType.SHADING);
        }
      }else if (command.equals(rendermodeAtomWireframeCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomRenderMode(AtomType.WIREFRAME);
        }
      }else if (command.equals(rendermodeBondQuickDrawCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.QUICKDRAW);
        }
      }else if (command.equals(rendermodeBondShadedCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.SHADING);
        }
      }else if (command.equals(rendermodeBondLineCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.LINE);
        }
      }else if (command.equals(rendermodeBondWireframeCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.WIREFRAME);
        }
      }else if (command.equals(frontCommand)){
            amat.xrot(0.0f);
            amat.yrot(0.0f);
      }else if (command.equals(topCommand)){
            amat.xrot(90.0f);
            amat.yrot(0.0f);
      }else if (command.equals(bottomCommand)){
            amat.xrot(-90.0f);
            amat.yrot(0.0f);
      }else if (command.equals(rightCommand)){
            amat.xrot(0.0f);
            amat.yrot(90.0f);
      }else if (command.equals(leftCommand)){
            amat.xrot(0.0f);
            amat.yrot(-90.0f);
      }else if (command.equals(homeCommand)){
            amat.unit();
            tmat.unit();
            zmat.unit();
            md.findBB();
            xmin = md.xmin;
            xmax = md.xmax;
            ymin = md.ymin;
            ymax = md.ymax;
            zmin = md.zmin;
            zmax = md.zmax;
            float xw = md.xmax - md.xmin;
            float yw = md.ymax - md.ymin;
            float zw = md.zmax - md.zmin;
            if (yw > xw)
                xw = yw;
            if (zw > xw)
                xw = zw;
            float f1 = getSize().width / xw;
            float f2 = getSize().height / xw; 
            xfac = 0.7f * (f1 < f2 ? f1 : f2) * scalefudge;        
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomScreenScale(xfac);
          cf.getFrame(i).setBondScreenScale(xfac);
        }
      }else if (command.equals(labelsNoneCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.NOLABELS);
        }
      }else if (command.equals(labelsSymbolsCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.SYMBOLS);
        }
      }else if (command.equals(labelsTypesCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.TYPES);
        }
      }else if (command.equals(labelsNumbersCommand)){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.NUMBERS);
        }
      }else if (command.equals(wireframeRotationCommand)){
            WireFrameRotation = !WireFrameRotation;
      }else{
           System.out.println("Unknown command: "+command);
      }
      if (painted){
        painted = false;
      }
      repaint();
    }
*/
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
            cf.getFrame(i).setBondsToAtomCenters(!doWe);
          }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public boolean getShowAtoms(){
      return md.getShowAtoms();
    }

    public void showVectors(boolean doWe){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setShowVectors(doWe);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public boolean getShowVectors(){
      return md.getShowVectors();
    }


    public void setAtomQuickDraw(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomRenderMode(AtomType.QUICKDRAW);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setAtomShaded(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomRenderMode(AtomType.SHADING);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setAtomWireframe(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setAtomRenderMode(AtomType.WIREFRAME);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setBondQuickDraw(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.QUICKDRAW);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setBondShaded(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.SHADING);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setBondLine(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.LINE);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setBondWireframe(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setBondRenderMode(Bond.WIREFRAME);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setLabelsToNone(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.NOLABELS);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setLabelsToSymbols(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.SYMBOLS);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setLabelsToTypes(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.TYPES);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }

    public void setLabelsToAtomNumbers(){
        for (int i=0;i<nframes;i++){
          cf.getFrame(i).setLabelMode(AtomType.NUMBERS);
        }
      if (painted){
        painted = false;
      }
      repaint();
    }
    
    public void setWireframeRotation(boolean OnOrOff){
            WireFrameRotation = OnOrOff;
    }

    public boolean getWireframeRotation(){
           return WireFrameRotation;
    }

}
