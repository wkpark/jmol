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

package org.openscience.jmol;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

public class displayPanel extends JPanel 
    implements Runnable,MeasurementListListener {
    private static boolean Perspective;
    private static float FieldOfView;
    private boolean painted = false;
    private boolean initialized = false;
    private boolean haveFile = false;
    private boolean rubberband = false;
    private boolean AntiAliased = false;
    private int bx, by, rtop, rbottom, rleft, rright;
    private int fileType;
    private int nframes = 0;
    private static int prevx, prevy, outx, outy;
    private static float xtheta, ytheta, ztheta;
    private static Matrix3D amat = new Matrix3D(); // Matrix to do mouse angular rotations.
    private static Matrix3D tmat = new Matrix3D(); // Matrix to do translations.
    private static Matrix3D zmat = new Matrix3D(); // Matrix to do zooming.
    private static Matrix3D mat = new Matrix3D();  // Final matrix for assembly on screen.
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
    StatusBar status;
    //Added T.GREY for moveDraw support- should be true while mouse is dragged
    private boolean mouseDragged = false;
    private boolean WireFrameRotation = false;
    private boolean movingDrawMode = false;
    private Measure m = null;
    private MeasurementList mlist = null;
    private DisplaySettings settings;

    public displayPanel(StatusBar status, DisplaySettings settings) {
        this.status = status;
		this.settings = settings;
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

    public void setMeasure(Measure m) {
        this.m = m;
    }
    
    public void start() {
        new Thread(this).start();
        AtomType.setJPanel(this);
        this.addMouseListener(new MyAdapter());            
        this.addMouseMotionListener(new MyMotionAdapter());            
    }

    public void setChemFile(ChemFile cf) {
        this.cf = cf;
        haveFile = true;
        nframes = cf.nFrames();
        this.md = cf.getFrame(0);
        Measurement.setChemFrame(md);
        if (mlist != null) {
            mlistChanged(new MeasurementListEvent(mlist));
        }                    
        init();
    }

//Added by T.GREY for POVRAY support
/**Returns transform matrix assosiated with the current viewing transform**/
    public Matrix3D getViewTransformMatrix(){
       Matrix3D viewMatrix = new Matrix3D();
       viewMatrix.translate(-(xmin + xmax) / 2,
                             -(ymin + ymax) / 2,
                             -(zmin + zmax) / 2);
       viewMatrix.mult(amat);
       viewMatrix.scale(xfac, -xfac, xfac);
       viewMatrix.mult(tmat);
       viewMatrix.mult(zmat);
       viewMatrix.translate(getSize().width / 2,
                            getSize().height / 2,
                            getSize().width / 2);
       return viewMatrix;
    }

    public void init() {
        md.findBB();
        if (mlist != null) {
            mlistChanged(new MeasurementListEvent(mlist));
        }                    
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
        settings.setAtomScreenScale(xfac);
        settings.setBondScreenScale(xfac);
        settings.setVectorScreenScale(xfac);
        repaint();
    }

    public void setFrame(int fr) {
        if (haveFile) {
            if (fr < nframes) {
                md = cf.getFrame(fr);
                Measurement.setChemFrame(md);
                if (mlist != null) {
                    mlistChanged(new MeasurementListEvent(mlist));
                }                    
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

    public void mlistChanged(MeasurementListEvent mle) {
        MeasurementList source = (MeasurementList)mle.getSource();
        mlist = source;
        md.updateMlists(mlist.getDistanceList(), 
                        mlist.getAngleList(), 
                        mlist.getDihedralList());
    }
       
    class MyAdapter extends MouseAdapter {
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
                    status.setStatus(2, n + " Atoms Selected");
                }
            } else if (mode == MEASURE) {
                if (haveFile) {                    
                    m.firePicked(md.pickMeasuredAtom(e.getX(), e.getY()));
                }
            }
                   
        }
            
        public void mouseReleased(MouseEvent e) {

            //NEW LINE T.GREY
            if(mouseDragged && WireFrameRotation){
                md.setMovingDrawMode(false);
                movingDrawMode = false;
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
                movingDrawMode = true;
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
                settings.setAtomScreenScale(xfac);
                settings.setBondScreenScale(xfac);
                settings.setVectorScreenScale(xfac);
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
                    status.setStatus(2, n + " Atoms Selected");
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

    public void rebond() throws Exception {
        if (md != null) {
            md.rebond();
        }
    }
            
    static void setBackgroundColor() {
        backgroundColor = Color.getColor("backgroundColor");
    }
    static void setBackgroundColor(Color bg) {
        backgroundColor = bg;
    }
    static Color getBackgroundColor() {
        return backgroundColor;
    }
    static void setPerspective(boolean p) {
        Perspective = p;
    }
    static boolean getPerspective() {
        return Perspective;
    }
    static void setFieldOfView(float fov) {
        FieldOfView = fov;
    }
    static float getFieldOfView() {
        return FieldOfView;
    }

    public void paint(Graphics g) {
        if (AntiAliased && !movingDrawMode) {
            String vers = System.getProperty("java.version");
            if (vers.compareTo("1.2") >= 0) {
            //comment out the next 5 lines if compiling under 1.1
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, 
                                    RenderingHints.VALUE_RENDER_QUALITY);
            }
        }
        
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
            ChemFrame.matunit();      
            ChemFrame.mattranslate(-(xmin + xmax) / 2,
                             -(ymin + ymax) / 2,
                             -(zmin + zmax) / 2);
            ChemFrame.matmult(amat);
            ChemFrame.matscale(xfac, -xfac, xfac);
            ChemFrame.matmult(tmat);
            ChemFrame.matmult(zmat);
            ChemFrame.mattranslate(getSize().width / 2, 
                                   getSize().height / 2, 
                                   getSize().width / 2);
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

    public Image takeSnapshot() {        
        Image snapImage = createImage(this.getSize().width, 
                                      this.getSize().height);
        Graphics snapGraphics = snapImage.getGraphics();
        paint(snapGraphics);
        return snapImage;
    }
    
    // The actions:
    
    private PickAction pickAction = new PickAction();
    private RotateAction rotateAction = new RotateAction();
    private ZoomAction zoomAction = new ZoomAction();
    private XlateAction xlateAction = new XlateAction();
    private HomeAction homeAction = new HomeAction();
    private FrontAction frontAction = new FrontAction();
    private TopAction topAction = new TopAction();
    private BottomAction bottomAction = new BottomAction();
    private RightAction rightAction = new RightAction();
    private LeftAction leftAction = new LeftAction(); 
    private aQuickdrawAction aquickdrawAction = new aQuickdrawAction();
    private aShadingAction ashadingAction = new aShadingAction();
    private aWireframeAction awireframeAction = new aWireframeAction();
    private bQuickdrawAction bquickdrawAction = new bQuickdrawAction();
    private bShadingAction bshadingAction = new bShadingAction();
    private bLineAction blineAction = new bLineAction();
    private bWireframeAction bwireframeAction = new bWireframeAction();
    private PlainAction plainAction = new PlainAction();
    private SymbolsAction symbolsAction = new SymbolsAction();
    private TypesAction typesAction = new TypesAction();
    private NumbersAction numbersAction = new NumbersAction();
    private BondsAction bondsAction = new BondsAction();
    private AtomsAction atomsAction = new AtomsAction();
    private VectorsAction vectorsAction = new VectorsAction();
    private HydrogensAction hydrogensAction = new HydrogensAction();
    private SelectallAction selectallAction = new SelectallAction();    
    private DeselectallAction deselectallAction = new DeselectallAction();
    private WireFrameRotationAction wireframerotationAction = new WireFrameRotationAction();
    
    class BondsAction extends AbstractAction {
        
        public BondsAction() {
            super("bonds");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.toggleBonds();
            repaint();
        }
    }
    class AtomsAction extends AbstractAction {
        
        public AtomsAction() {
            super("atoms");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.toggleAtoms();
            settings.toggleDrawBondsToAtomCenters();
            repaint();
        }
    }
    class VectorsAction extends AbstractAction {
        
        public VectorsAction() {
            super("vectors");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.toggleVectors();
            repaint();
        }
    }   
    class HydrogensAction extends AbstractAction {
        
        public HydrogensAction() {
            super("hydrogens");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.toggleHydrogens();
            repaint();
        }
    }
    class SelectallAction extends AbstractAction {
        
        public SelectallAction() {
            super("selectall");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            if (haveFile) {                    
                md.selectAll();
                int n = md.getNpicked();
                status.setStatus(2, n + " Atoms Selected");
                repaint();
            }
        }
    }
    class DeselectallAction extends AbstractAction {
        
        public DeselectallAction() {
            super("deselectall");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            if (haveFile) {                    
                md.deselectAll();
                int n = md.getNpicked();
                status.setStatus(2, n + " Atoms Selected");
                repaint();
            }
        }
    }
    class aQuickdrawAction extends AbstractAction {
        
        public aQuickdrawAction() {
            super("aquickdraw");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setAtomDrawMode(DisplaySettings.QUICKDRAW);
            repaint();
        }
    }
    class aShadingAction extends AbstractAction {
        
        public aShadingAction() {
            super("ashading");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setAtomDrawMode(DisplaySettings.SHADING);
            repaint();
        }
    }
    class aWireframeAction extends AbstractAction {
        
        public aWireframeAction() {
            super("awireframe");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setAtomDrawMode(DisplaySettings.WIREFRAME);
            repaint();
        }
    }
    class bQuickdrawAction extends AbstractAction {
        
        public bQuickdrawAction() {
            super("bquickdraw");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setBondDrawMode(DisplaySettings.QUICKDRAW);
            repaint();
        }
    }
    class bShadingAction extends AbstractAction {
        
        public bShadingAction() {
            super("bshading");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setBondDrawMode(DisplaySettings.SHADING);
            repaint();
        }
    }
    class bLineAction extends AbstractAction {
        
        public bLineAction() {
            super("bline");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setBondDrawMode(DisplaySettings.LINE);
            repaint();
        }
    }
    class bWireframeAction extends AbstractAction {
        
        public bWireframeAction() {
            super("bwireframe");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setBondDrawMode(DisplaySettings.WIREFRAME);
            repaint();
        }
    }

    class PickAction extends AbstractAction {
        
        public PickAction() {
            super("pick");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            // switch mode;
            mode = PICK;
            status.setStatus(1, "Select Atoms");
        }
    }

    class RotateAction extends AbstractAction {
        
        public RotateAction() {
            super("rotate");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            // switch mode;
            mode = ROTATE;
            status.setStatus(1, "Rotate Camera");
        }
    }

    class ZoomAction extends AbstractAction {
        
        public ZoomAction() {
            super("zoom");            
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            // switch mode;
            mode = ZOOM;
            status.setStatus(1, "Zoom Camera");
        }
    }

    class XlateAction extends AbstractAction {
        
        public XlateAction() {
            super("xlate");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            // switch mode;
            mode = XLATE;
            status.setStatus(1, "Translate Camera");
        }
    }

    class FrontAction extends AbstractAction {
        
        public FrontAction() {
            super("front");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            amat.xrot(0.0f);
            amat.yrot(0.0f);
            repaint();
        }
    }

    class TopAction extends AbstractAction {
        
        public TopAction() {
            super("top");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            amat.xrot(90.0f);
            amat.yrot(0.0f);
            repaint();
        }
    }

    class BottomAction extends AbstractAction {
        
        public BottomAction() {
            super("bottom");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            amat.xrot(-90.0f);
            amat.yrot(0.0f);
            repaint();
        }
    }

    class RightAction extends AbstractAction {
        
        public RightAction() {
            super("right");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            amat.xrot(0.0f);
            amat.yrot(90.0f);
            repaint();
        }
    }

    class LeftAction extends AbstractAction {
        
        public LeftAction() {
            super("left");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            amat.xrot(0.0f);
            amat.yrot(-90.0f);
            repaint();
        }
    }

    class PlainAction extends AbstractAction {
        
        public PlainAction() {
            super("plain");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setLabelMode(DisplaySettings.NOLABELS);
            repaint();
        }
    }
    class SymbolsAction extends AbstractAction {
        
        public SymbolsAction() {
            super("symbols");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setLabelMode(DisplaySettings.SYMBOLS);
            repaint();
        }
    }
    class TypesAction extends AbstractAction {
        
        public TypesAction() {
            super("types");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setLabelMode(DisplaySettings.TYPES);
            repaint();
        }
    }
    class NumbersAction extends AbstractAction {
        
        public NumbersAction() {
            super("numbers");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            settings.setLabelMode(DisplaySettings.NUMBERS);
            repaint();
        }
    }

    class HomeAction extends AbstractAction {
        
        public HomeAction() {
            super("home");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
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
            settings.setAtomScreenScale(xfac);
            settings.setBondScreenScale(xfac);
            repaint();
        }
    }

    class WireFrameRotationAction extends AbstractAction {
        
        public WireFrameRotationAction() {
            super("wireframerotation");
            this.setEnabled(true);
        }
        
        public void actionPerformed(ActionEvent e) {            
            JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
            WireFrameRotation = cbmi.isSelected();
        }
    }

    public Action[] getActions() {
        Action[] defaultActions = {
            pickAction,
            rotateAction,
            zoomAction,
            xlateAction,
            frontAction,
            topAction,
            bottomAction,
            rightAction,
            leftAction,
            aquickdrawAction,
            ashadingAction,
            awireframeAction,
            bquickdrawAction,
            bshadingAction,
            blineAction,
            bwireframeAction,
            plainAction,
            symbolsAction,
            typesAction,
            numbersAction,
            bondsAction,
            atomsAction,
            vectorsAction,
            hydrogensAction,
            selectallAction,
            deselectallAction,
            homeAction,
            wireframerotationAction
        };
        return defaultActions;
    }
    
    DisplaySettings getSettings() {
		return settings;
	}
}
