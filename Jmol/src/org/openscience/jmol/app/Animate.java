
/*
 * Copyright 2002 The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app;
import org.openscience.jmol.*;

import java.io.File;
import java.awt.Container;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.EventObject;
import javax.swing.JDialog;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.BoxLayout;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.TitledBorder;
import java.util.Vector;
import javax.vecmath.Point3d;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class Animate extends JDialog implements ActionListener,
    PropertyChangeListener, Runnable {

  private DisplayControl control;
  
  private Thread animThread = null;
  private boolean haveFile = false;
  private int nframes = 1;
  private int speed = 10;

  private boolean repeat = true;

  public static int currentFrame;
  private JSlider progressSlider = new JSlider(JSlider.HORIZONTAL, 1, 1, 1);
  private JSlider iSlider;
  private JLabel infoLabel = new JLabel(" ");
  private ChemFile inFile, cf;

  // The actions:

  private AnimateAction animateAction = new AnimateAction();
  private Hashtable commands;

  private static int numberExtraFrames = 0;

  private void restoreInFile() {

    nframes = inFile.getNumberOfFrames();
    cf = inFile;
    progressSlider.setMaximum(nframes);
    currentFrame = 0;
    haveFile = true;
    control.setChemFile(cf);
    setFrame(currentFrame, true);
  }

  private void createExtraFrames() {

    restoreInFile();
    if (nframes < 2) {
      return;
    }

    // Create set of new frames
    ChemFile newFile = new ChemFile();
    int frameNumber = 0;
    ChemFrame fromFrame = inFile.getFrame(frameNumber);

    // Add first frame
    newFile.addFrame(fromFrame);
    ++frameNumber;
    ChemFrame toFrame;
    while (frameNumber < nframes) {
      toFrame = inFile.getFrame(frameNumber);

      // Interpolate to get extra frames
      ChemFrame[] extraFrames = new ChemFrame[numberExtraFrames];
      int numberVertices = Math.min(fromFrame.getNumberOfAtoms(),
          toFrame.getNumberOfAtoms());
      for (int i = 0; i < numberExtraFrames; i++) {
	if (fromFrame instanceof CrystalFrame) {
	  extraFrames[i] = new CrystalFrame(numberVertices);
	} else {
	  extraFrames[i] = new ChemFrame(numberVertices);
	}
      }

      // Linearly interpolate new coordinates for extra frames
      for (int k = 0; k < numberVertices; ++k) {
        Atom atom = (org.openscience.jmol.Atom)fromFrame.getAtomAt(k);
        double[] fromCoord = fromFrame.getAtomCoords(k);
        double[] toCoord = toFrame.getAtomCoords(k);
        double[] step = new double[3];
        step[0] = (toCoord[0] - fromCoord[0]) / (numberExtraFrames + 1);
        step[1] = (toCoord[1] - fromCoord[1]) / (numberExtraFrames + 1);
        step[2] = (toCoord[2] - fromCoord[2]) / (numberExtraFrames + 1);
        for (int i = 0; i < numberExtraFrames; i++) {
          double[] newCoord = new double[3];
          newCoord[0] = fromCoord[0] + (i + 1) * step[0];
          newCoord[1] = fromCoord[1] + (i + 1) * step[1];
          newCoord[2] = fromCoord[2] + (i + 1) * step[2];
          try {
            extraFrames[i].addAtom(atom.getType(), newCoord[0],
                newCoord[1], newCoord[2]);
          } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
          }
        }
      }

      // Linearly interpolate primitive vectors and unit cell edges
      if (fromFrame instanceof CrystalFrame) {
	  double[][] fromRprimd = ((CrystalFrame)fromFrame).getRprimd();
	  double[][] toRprimd = ((CrystalFrame)toFrame).getRprimd();
	  double[][] stepRprimd = new double[3][3];
	  Vector fromBoxEdges = ((CrystalFrame)fromFrame).getBoxEdges();
	  Vector toBoxEdges = ((CrystalFrame)toFrame).getBoxEdges();
	  Vector stepBoxEdges = new Vector(fromBoxEdges.size());

	  for (int i=0; i<3;i++) { //Primitive vectors steps
	    for (int j=0; j<3;j++) {
	      stepRprimd[i][j] = (toRprimd[i][j] - fromRprimd[i][j])/
		(numberExtraFrames + 1);
	    }
	  }
	  
	  for (int i=0; i< fromBoxEdges.size(); i++) { //Box edges steps
	    Point3d step = new Point3d();
	    step.x = (((Point3d)toBoxEdges.elementAt(i)).x 
		      - ((Point3d)fromBoxEdges.elementAt(i)).x) /
	      (numberExtraFrames + 1);
	    step.y = (((Point3d)toBoxEdges.elementAt(i)).y 
		      - ((Point3d)fromBoxEdges.elementAt(i)).y) /
	      (numberExtraFrames + 1);
	    step.z = (((Point3d)toBoxEdges.elementAt(i)).z 
		      - ((Point3d)fromBoxEdges.elementAt(i)).z) /
	      (numberExtraFrames + 1);
	    stepBoxEdges.addElement(step);
	  }

	  for (int k = 0; k < numberExtraFrames; k++) {
	    double[][] newRprimd = new double[3][3];
	    for (int i=0; i<3;i++) {
	      for (int j=0; j<3;j++) {
		newRprimd[i][j]=fromRprimd[i][j] + (k + 1)* stepRprimd[i][j];
	      }
	    }
	    Vector newBoxEdges = new Vector(stepBoxEdges.size());
	    for (int i=0; i < stepBoxEdges.size(); i++) {
	      Point3d newPoint = new Point3d();
	      newPoint.x = ((Point3d)fromBoxEdges.elementAt(i)).x + (k + 1) 
		* ((Point3d)stepBoxEdges.elementAt(i)).y;
	      newPoint.y = ((Point3d)fromBoxEdges.elementAt(i)).y + (k + 1) 
		* ((Point3d)stepBoxEdges.elementAt(i)).z;
	      newPoint.z = ((Point3d)fromBoxEdges.elementAt(i)).z + (k + 1) 
		* ((Point3d)stepBoxEdges.elementAt(i)).x;
	      newBoxEdges.addElement(newPoint);
	    }

	    ((CrystalFrame)extraFrames[k]).setRprimd(newRprimd);
	    ((CrystalFrame)extraFrames[k]).setBoxEdges(newBoxEdges);

	  }
      }
      

      // Add interpolated frames
      for (int i = 0; i < numberExtraFrames; i++) {
        newFile.addFrame(extraFrames[i]);
      }

      // Add original frame
      newFile.addFrame(toFrame);

      // Increment to next frame
      fromFrame = toFrame;
      ++frameNumber;
    }

    haveFile = true;
    nframes = newFile.getNumberOfFrames();
    cf = newFile;
    progressSlider.setMaximum(nframes);
    currentFrame = 0;
    control.setChemFile(cf);
  }

  /**
   * Set file for the animation at a particular ChemFile
   *
   * @param cf the ChemFile
   */
  private void setChemFile(ChemFile cf) {

    stop();
    setVisible(false);
    this.inFile = cf;
    restoreInFile();
    if (nframes > 1) {
      animateAction.setEnabled(true);
      progressSlider.setMaximum(nframes);
      currentFrame = 0;
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
    myThread.setPriority(Thread.MIN_PRIORITY);
    while (animThread == myThread) {
      if (haveFile) {
        if (currentFrame < nframes - 1) {
          currentFrame++;
        } else {

          /* leave the pointer on the last frame and halt if
             user doesn't want to repeat.  Otherwise, reset to
             frame zero and keep going.
          */
          if (repeat) {
            currentFrame = 0;
          } else {
            animThread = null;
          }
        }
        setFrame(currentFrame, true);

        /*
           We have to sleep some, or we don't actually have
           time to display each frame.  This is, of course,
           somewhat processor dependent, and a better way to do
           this animation might be to schedule the next redraw
           using javax.swing.Timer.
        */
        int sleepiness = 50 * (11 - speed);
        try {
          Thread.sleep(sleepiness);
        } catch (InterruptedException e) {

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
   */
  public Animate(DisplayControl control, JFrame f) {

    super(f, "Animation", false);
    this.control = control;
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }

    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

    JPanel progressPanel = new JPanel();
    progressPanel.setLayout(new BorderLayout());
    progressPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Animate.progressLabel")));
    progressSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    progressSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        int fr = source.getValue();
        if (fr - 1 != currentFrame) {
          currentFrame = fr - 1;
          setFrame(fr - 1, false);
        }
      }
    });
    progressPanel.add(progressSlider);
    container.add(progressPanel);

    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    infoPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Animate.infoLabel")));
    infoPanel.add(infoLabel);
    container.add(infoPanel);

    JPanel rcPanel = new JPanel();
    rcPanel.setLayout(new BoxLayout(rcPanel, BoxLayout.X_AXIS));
    rcPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Animate.controlsLabel")));

    JCheckBox rC =
      new JCheckBox(JmolResourceHandler.getInstance()
        .getString("Animate.repeatCBLabel"), false);
    rC.setSelected(repeat);
    rC.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        repeat = !repeat;
      }
    });

    JButton rwb =
      new JButton(JmolResourceHandler.getInstance()
        .getIcon("Animate.rewindImage"));
    rwb.setMargin(new Insets(1, 1, 1, 1));
    rwb.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Animate.rewindTooltip"));
    rwb.setActionCommand("rewind");
    rwb.addActionListener(this);

    JButton plb =
      new JButton(JmolResourceHandler.getInstance()
        .getIcon("Animate.playImage"));
    plb.setMargin(new Insets(1, 1, 1, 1));
    plb.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Animate.playTooltip"));
    plb.setActionCommand("play");
    plb.addActionListener(this);

    JButton pb =
      new JButton(JmolResourceHandler.getInstance()
        .getIcon("Animate.pauseImage"));
    pb.setMargin(new Insets(1, 1, 1, 1));
    pb.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Animate.pauseTooltip"));
    pb.setActionCommand("pause");
    pb.addActionListener(this);

    JButton nb =
      new JButton(JmolResourceHandler.getInstance()
        .getIcon("Animate.nextImage"));
    nb.setMargin(new Insets(1, 1, 1, 1));
    nb.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Animate.nextTooltip"));
    nb.setActionCommand("next");
    nb.addActionListener(this);

    JButton prb =
      new JButton(JmolResourceHandler.getInstance()
        .getIcon("Animate.prevImage"));
    prb.setMargin(new Insets(1, 1, 1, 1));
    prb.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Animate.prevTooltip"));
    prb.setActionCommand("prev");
    prb.addActionListener(this);

    JButton ffb =
      new JButton(JmolResourceHandler.getInstance()
        .getIcon("Animate.ffImage"));
    ffb.setMargin(new Insets(1, 1, 1, 1));
    ffb.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Animate.ffTooltip"));
    ffb.setActionCommand("ff");
    ffb.addActionListener(this);

    rcPanel.add(rC);
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
    speedPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Animate.speedLabel")));
    JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 11, speed);
    speedSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    speedSlider.setPaintTicks(true);
    speedSlider.setMajorTickSpacing(2);
    speedSlider.setPaintLabels(true);
    speedSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        int s = source.getValue();
        speed = s;
      }
    });
    speedPanel.add(speedSlider);
    container.add(speedPanel);

    JPanel iPanel = new JPanel();
    iPanel.setLayout(new BorderLayout());
    iPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Animate.interpLabel")));

    iSlider = new JSlider(JSlider.HORIZONTAL, 0, 20, numberExtraFrames);
    iSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    iSlider.setPaintTicks(true);
    iSlider.setMajorTickSpacing(4);
    iSlider.setPaintLabels(true);
    iSlider.addChangeListener(new NondragChangeListener());

    JCheckBox iC =
      new JCheckBox(JmolResourceHandler.getInstance()
        .getString("Animate.interpCBLabel"), false);
    iC.setSelected(false);
    iC.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        stop();
        JCheckBox source = (JCheckBox) e.getSource();
        if (source.isSelected()) {
          iSlider.setEnabled(true);
          createExtraFrames();
        } else {
          restoreInFile();
          iSlider.setEnabled(false);
        }
      }
    });

    JLabel iL =
      new JLabel(JmolResourceHandler.getInstance()
        .getString("Animate.interpSLabel"), SwingConstants.CENTER);
    iL.setLabelFor(iSlider);
    iPanel.add(iC, BorderLayout.NORTH);
    iPanel.add(iSlider, BorderLayout.CENTER);
    iPanel.add(iL, BorderLayout.SOUTH);
    container.add(iPanel);


    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton dis =
      new JButton(JmolResourceHandler.getInstance()
        .getString("Animate.dismissLabel"));
    dis.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        close();
      }
    });
    buttonPanel.add(dis);
    getRootPane().setDefaultButton(dis);

    container.add(buttonPanel);
    getContentPane().add(container);
    addWindowListener(new AnimateWindowListener());
    pack();
    centerDialog();
  }

  protected void centerDialog() {

    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension size = this.getSize();
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    this.setLocation(x, y);
  }

  public void close() {
    stop();
    restoreInFile();
    this.setVisible(false);
    animateAction.setEnabled(true);
  }

  public void actionPerformed(ActionEvent evt) {

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
        currentFrame = nframes - 1;
        setFrame(currentFrame, true);
      }
    }

    if (arg == "next") {
      if (haveFile) {
        stop();
        if (currentFrame < nframes - 1) {
          currentFrame++;
        }
        setFrame(currentFrame, true);
      }
    }

    if (arg == "prev") {
      if (haveFile) {
        stop();
        if (currentFrame > 0) {
          currentFrame--;
        }
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
   * sets the frame of the model and updates the slider
   *
   * @param which the frame number
   * @param setSlider true if we should set the slider position also
   */
  synchronized void setFrame(int which, boolean setSlider) {

    control.setFrame(which);
    ChemFrame frame = control.getFrame();
    String inf = frame.getInfo();
    if (inf != null) {
      infoLabel.setText(inf);
    }
    if (setSlider) {
      progressSlider.setValue(which + 1);
    }
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

  class NondragChangeListener implements ChangeListener {

    private boolean overrideIsAdjusting = false;
    public NondragChangeListener() {

      // Workaround for documented bug 4246117 for JDK 1.2.2
      if (System.getProperty("java.version").equals("1.2.2")
          && System.getProperty("java.vendor").startsWith("Sun Micro")) {
        overrideIsAdjusting = true;
      }
    }

    public void stateChanged(ChangeEvent e) {

      stop();
      JSlider source = (JSlider) e.getSource();
      if (overrideIsAdjusting || !source.getValueIsAdjusting()) {
        int n = source.getValue();
        if (n != numberExtraFrames) {
          numberExtraFrames = n;
          createExtraFrames();
        }
      }
    }
  }

  public void propertyChange(PropertyChangeEvent event) {
    
    if (event.getPropertyName().equals(DisplayControl.PROP_CHEM_FILE)) {
      if (event.getNewValue() != inFile && event.getNewValue() != cf) {
        setChemFile((ChemFile) event.getNewValue());
      }
    }
  }
}
