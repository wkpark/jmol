/*
 * @(#)Animate.java    1.0 98/08/27
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
import java.beans.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.metal.*;
import javax.swing.JColorChooser.*;

public class Animate extends JDialog implements ActionListener,Runnable {

    private Thread animThread = null;    
    private boolean haveFile = false;
    private int nframes = 1;
    private int speed = 10;
    private displayPanel display;

    public static int currentFrame;
    private JSlider progressSlider = new JSlider(JSlider.HORIZONTAL, 1, 1, 1);
    private JLabel infoLabel = new JLabel(" ");
    private ChemFile cf;
    private static JmolResourceHandler jrh;
    
    // The actions:
    
    private AnimateAction animateAction = new AnimateAction();
    private Hashtable commands;

    static {
        jrh = new JmolResourceHandler("Animate");
    }
    
    private static int numberExtraFrames = 0;
    
    static void setNumberExtraFrames(int n) {
        numberExtraFrames = n;
    }
    
    static int getNumberExtraFrames() {
        return numberExtraFrames;
    }

    private void createExtraFrames() {
        if (nframes < 2)  return;
        
        // Create set of new frames
        ChemFile inputFile = cf;
        ChemFile newFile = new ChemFile();
        int frameNumber = 0;
        ChemFrame fromFrame = inputFile.getFrame(frameNumber);
        // Add first frame
        newFile.frames.addElement(fromFrame);
        ++frameNumber;
        ChemFrame toFrame;
        while (frameNumber < nframes) {
            toFrame = inputFile.getFrame(frameNumber);            
            // Interpolate to get extra frames
            ChemFrame[] extraFrames = new ChemFrame[numberExtraFrames];
            int numberVertices = fromFrame.getNvert();
            for (int i=0; i < numberExtraFrames; i++) {
                extraFrames[i] = new ChemFrame(numberVertices);
            }            
            // Linearly interpolate new coordinates for extra frames
            for (int k=0; k < numberVertices; ++k) {
                AtomType atomType = fromFrame.getAtomType(k);
                double[] fromCoord = fromFrame.getVertCoords(k);
                double[] toCoord = toFrame.getVertCoords(k);
                double[] step = new double[3];
                step[0] = (toCoord[0] - fromCoord[0])/(numberExtraFrames+1);
                step[1] = (toCoord[1] - fromCoord[1])/(numberExtraFrames+1);
                step[2] = (toCoord[2] - fromCoord[2])/(numberExtraFrames+1);
                for (int i = 0; i < numberExtraFrames; i++) {
                    double[] newCoord = new double[3];
                    newCoord[0] = fromCoord[0] + (i+1)*step[0];
                    newCoord[1] = fromCoord[1] + (i+1)*step[1];
                    newCoord[2] = fromCoord[2] + (i+1)*step[2];
                    Vector newProps = new Vector();
                    try {
                        extraFrames[i].addPropertiedVert(atomType.getName(), 
                                                         (float)newCoord[0], 
                                                         (float)newCoord[1], 
                                                         (float)newCoord[2], 
                                                         newProps);
                    } catch (Exception ex) {
                        System.out.println(ex);
                        ex.printStackTrace();
                    }
                }
            }
            
            // Add interpolated frames
            for (int i=0; i < numberExtraFrames; i++) {
                newFile.frames.addElement(extraFrames[i]);
            }
            
            // Add original frame
            newFile.frames.addElement(toFrame);
            
            // Increment to next frame
            fromFrame = toFrame;
            ++frameNumber;
        }
        
        haveFile = true;
        nframes = newFile.nFrames();
        cf = newFile;
        progressSlider.setMaximum(nframes);        
        currentFrame = 0;
        display.setChemFile(cf);
    }

    /**
     * Set file for the animation at a particular ChemFile
     *
     * @param cf the ChemFile
     */
    public void setChemFile(ChemFile cf) {
        stop();
        setVisible(false);
        this.cf = cf;
	System.out.println(this.cf);
        haveFile = true;
        nframes = cf.nFrames();
        if (nframes > 1) {
            animateAction.setEnabled(true);
            progressSlider.setMaximum(nframes);        
            currentFrame = 0;
            createExtraFrames();        
        } else {
            animateAction.setEnabled(false);
        }
    }

    /**
     * Start the thread for animating the dynamics
     *
     */
    public void start() {
        if (animThread == null) {
            animThread = new Thread(this, "Animation");
            animThread.start();
        }
    }
    
    /**
     * Run the animation, which means incrementing the frame, sleeping
     * for a while (depending on animation speed) and then exiting.
     */
    public void run() {
        Thread myThread = Thread.currentThread();
        while (animThread == myThread) {
            if (haveFile) {
                if (currentFrame < nframes-1) {
                    currentFrame++;
                } else {
                    currentFrame = 0;
                }
                setFrame(currentFrame, true);                    
                int sleepiness = 100*(11-speed);
                try {
                    Thread.sleep(sleepiness);
                } catch (InterruptedException e){
                    // the VM doesn't want us to sleep anymore,
                    // so get back to work
                }
            } else {
                try {
                    myThread.setPriority(Thread.MIN_PRIORITY);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Stop the animation.
     */
    public void stop() {
        animThread = null;
    }
    
    /**
     * Constructor 
     *
     * @param f the parent frame
     * @param dp the displayPanel in which the animation will take place
     */
    public Animate(JFrame f, displayPanel dp) {
        super(f, "Animation", false);
        this.display = dp;
        commands = new Hashtable();
        Action[] actions = getActions();
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            commands.put(a.getValue(Action.NAME), a);
        }

        JPanel container = new JPanel();
        container.setLayout( new BoxLayout(container, BoxLayout.Y_AXIS) );

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.setBorder(new TitledBorder(jrh.getString("progressLabel")));
        progressSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        progressSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                int fr = source.getValue();
                if (fr-1 != currentFrame) {
                    currentFrame = fr-1;
                    setFrame(fr-1, false);
                }
            }
        });        
        progressPanel.add(progressSlider);
        container.add(progressPanel);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setBorder(new TitledBorder(jrh.getString("infoLabel")));
        infoPanel.add(infoLabel);
        container.add(infoPanel);

        JPanel rcPanel = new JPanel();
        rcPanel.setLayout( new BoxLayout(rcPanel, BoxLayout.X_AXIS) );
        rcPanel.setBorder(new TitledBorder(jrh.getString("controlsLabel")));
        
        JButton rwb = new JButton(jrh.getIcon("rewindImage"));
        rwb.setMargin(new Insets(1,1,1,1));
        rwb.setToolTipText(jrh.getString("rewindTooltip"));    
        rwb.setActionCommand("rewind");
        rwb.addActionListener(this);

        JButton plb = new JButton(jrh.getIcon("playImage"));
        plb.setMargin(new Insets(1,1,1,1));
        plb.setToolTipText(jrh.getString("playTooltip"));    
        plb.setActionCommand("play");
        plb.addActionListener(this);

        JButton pb = new JButton(jrh.getIcon("pauseImage"));
        pb.setMargin(new Insets(1,1,1,1));
        pb.setToolTipText(jrh.getString("pauseTooltip"));    
        pb.setActionCommand("pause");
        pb.addActionListener(this);

        JButton nb = new JButton(jrh.getIcon("nextImage"));
        nb.setMargin(new Insets(1,1,1,1));
        nb.setToolTipText(jrh.getString("nextTooltip"));    
        nb.setActionCommand("next");
        nb.addActionListener(this);

        JButton prb = new JButton(jrh.getIcon("prevImage"));
        prb.setMargin(new Insets(1,1,1,1));
        prb.setToolTipText(jrh.getString("prevTooltip"));    
        prb.setActionCommand("prev");
        prb.addActionListener(this);

        JButton ffb = new JButton(jrh.getIcon("ffImage"));
        ffb.setMargin(new Insets(1,1,1,1));
        ffb.setToolTipText(jrh.getString("ffTooltip"));    
        ffb.setActionCommand("ff");
        ffb.addActionListener(this);
        
        rcPanel.add(rwb);
        rcPanel.add(Box.createHorizontalGlue()); 
        rcPanel.add(prb);
        rcPanel.add(Box.createHorizontalGlue());
        rcPanel.add(plb);
        rcPanel.add(Box.createHorizontalGlue());
        rcPanel.add(pb);
        rcPanel.add(Box.createHorizontalGlue());
        rcPanel.add(nb);
        rcPanel.add(Box.createHorizontalGlue());
        rcPanel.add(ffb);
        container.add(rcPanel);
        
        JPanel speedPanel = new JPanel();
        speedPanel.setLayout(new BorderLayout());
        speedPanel.setBorder(new TitledBorder(jrh.getString("speedLabel")));
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, speed);
        speedSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        speedSlider.setPaintTicks(true);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintLabels(true);
        speedSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                int s = source.getValue();
                speed = s;
            }
        });
        speedPanel.add(speedSlider);
        container.add(speedPanel);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
        JButton dis = new JButton(jrh.getString("dismissLabel"));
        dis.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }});
        buttonPanel.add( dis );
        getRootPane().setDefaultButton( dis );
       
        container.add(buttonPanel);
        getContentPane().add(container);
        addWindowListener(new AnimateWindowListener());
        pack();
        centerDialog();
    }
    
    protected void centerDialog() {
        Dimension screenSize = this.getToolkit().getScreenSize();
        Dimension size = this.getSize();
        screenSize.height = screenSize.height/2;
        screenSize.width = screenSize.width/2;
        size.height = size.height/2;
        size.width = size.width/2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        this.setLocation(x,y);
    }

    public void close() {
        stop();
 	this.setVisible(false);
	animateAction.setEnabled(true);
    }
        
    public void actionPerformed(ActionEvent evt)
    {
        String arg = evt.getActionCommand();

        if (arg == "rewind") {
            if (haveFile) {
                stop();
                currentFrame = 0;
                setFrame(currentFrame, true);
            }
        } 

        if (arg == "ff") {
            if (haveFile) {
                stop();
                currentFrame = nframes-1;
                setFrame(currentFrame, true);
            }
        } 

        if (arg == "next") {
            if (haveFile) {
                stop();
                if (currentFrame < nframes-1) currentFrame++;                
                setFrame(currentFrame, true);
            }
        }

        if (arg == "prev") {
            if (haveFile) {
                stop();
                if (currentFrame > 0) currentFrame--;
                setFrame(currentFrame, true);
            }
        }
               
        if (arg == "pause") {
            stop();
        }
        
        if (arg == "play") {
            start();
        }

    }
    
    /**
     * sets the frame pointer of the display panel and updates the slider
     *
     * @param which the frame number
     * @param setSlider true if we should set the slider position also
     */
    void setFrame(int which, boolean setSlider) {
        display.setFrame(which);
	System.out.println("SetFrame: " + cf.nFrames());
        ChemFrame frame = cf.getFrame(which);
        String inf = frame.getInfo();
        if (inf != null) infoLabel.setText(inf);
        if (setSlider) progressSlider.setValue(which+1);                
    }
         
    class AnimateAction extends AbstractAction {
        
        public AnimateAction() {
            super("animate");
            if (haveFile) {                
                this.setEnabled(true);
            } else {
                this.setEnabled(false);
            }
        }
        
        public void actionPerformed(ActionEvent e) {
            currentFrame = 0;
            setFrame(currentFrame, true);
            this.setEnabled(false);
            show();
        }
    }
    
    public Action[] getActions() {
        Action[] defaultActions = {
            animateAction
        };
        return defaultActions;
    }
    
    protected Action getAction(String cmd) {
        return (Action) commands.get(cmd);
    }

	class AnimateWindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			close();
		}
	}

} 


