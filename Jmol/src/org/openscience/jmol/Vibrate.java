
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.vecmath.Point3f;

/**
 * A dialog for controling the animation of a molecular vibration.
 * This dialog provides controls for selecting the ChemFrame containing
 * vibrations within the frames in a ChemFile, for selecting which
 * of the vibrations to display, for selecting a frame of the vibration
 * animation, for controling the playback, and for selecting the
 * speed of animation.
 *
 * <p> The dialog also provides the functionality for creating the
 * vibrations from a ChemFile.
 *
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 */
public class Vibrate extends JDialog implements ActionListener, Runnable {

  /**
   * Sets the scale factor for the amplitude of vibrations.
   *
   * @param s amplitude scale factor
   */
  public static void setAmplitudeScale(double s) {
    amplitudeScale = s;
  }

  /**
   * Gets the scale factor for the amplitude of vibrations.
   */
  public static double getAmplitudeScale() {
    return amplitudeScale;
  }

  /**
   * Sets the scale factor for the atomic force vectors of vibrations.
   *
   * @param s vector scale factor
   */
  public static void setVectorScale(double s) {
    vectorScale = s;
  }

  /**
   * Gets the scale factor for the atomic force vectors of vibrations.
   */
  public static double getVectorScale() {
    return vectorScale;
  }

  /**
   * Sets the number of frames to be created for the vibration animations.
   *
   * @param n number of animation frames
   */
  public static void setNumberFrames(int n) {
    numberFrames = n;
  }

  /**
   * Gets the number of frames to be created for the vibration animations.
   */
  public static int getNumberFrames() {
    return numberFrames;
  }

  /**
   * Creates a dialog.
   *
   * @param f the parent frame
   * @param dp the DisplayPanel in which the vibration will be displayed
   */
  public Vibrate(JFrame f, DisplayPanel dp) {

    super(f, "Vibration", false);
    display = dp;
    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    JPanel framePanel = new JPanel();
    framePanel.setLayout(new BoxLayout(framePanel, BoxLayout.X_AXIS));
    framePanel.setBorder(new TitledBorder(jrh.getString("frameLabel")));
    frameCombo.addItemListener(new FrameComboItemListener());
    framePanel.add(frameCombo);
    container.add(framePanel);
    JPanel vibPanel = new JPanel();
    vibPanel.setLayout(new BoxLayout(vibPanel, BoxLayout.X_AXIS));
    vibPanel.setBorder(new TitledBorder(jrh.getString("vibrationLabel")));
    vibCombo.addItemListener(new VibComboItemListener());
    vibPanel.add(vibCombo);
    container.add(vibPanel);
    JPanel progressPanel = new JPanel();
    progressPanel.setLayout(new BorderLayout());
    progressPanel.setBorder(new TitledBorder(jrh.getString("progressLabel")));
    progressSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    progressSlider.addChangeListener(new ProgressSliderChangeListener());
    progressPanel.add(progressSlider);
    container.add(progressPanel);
    JPanel rcPanel = new JPanel();
    rcPanel.setLayout(new BoxLayout(rcPanel, BoxLayout.X_AXIS));
    rcPanel.setBorder(new TitledBorder(jrh.getString("controlsLabel")));
    JButton rwb = new JButton(jrh.getIcon("rewindImage"));
    rwb.setMargin(new Insets(1, 1, 1, 1));
    rwb.setToolTipText(jrh.getString("rewindTooltip"));
    rwb.setActionCommand("rewind");
    rwb.addActionListener(this);
    JButton plb = new JButton(jrh.getIcon("playImage"));
    plb.setMargin(new Insets(1, 1, 1, 1));
    plb.setToolTipText(jrh.getString("playTooltip"));
    plb.setActionCommand("play");
    plb.addActionListener(this);
    JButton pb = new JButton(jrh.getIcon("pauseImage"));
    pb.setMargin(new Insets(1, 1, 1, 1));
    pb.setToolTipText(jrh.getString("pauseTooltip"));
    pb.setActionCommand("pause");
    pb.addActionListener(this);
    JButton nb = new JButton(jrh.getIcon("nextImage"));
    nb.setMargin(new Insets(1, 1, 1, 1));
    nb.setToolTipText(jrh.getString("nextTooltip"));
    nb.setActionCommand("next");
    nb.addActionListener(this);
    JButton prb = new JButton(jrh.getIcon("prevImage"));
    prb.setMargin(new Insets(1, 1, 1, 1));
    prb.setToolTipText(jrh.getString("prevTooltip"));
    prb.setActionCommand("prev");
    prb.addActionListener(this);
    JButton ffb = new JButton(jrh.getIcon("ffImage"));
    ffb.setMargin(new Insets(1, 1, 1, 1));
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

    // Slider for selecting a frame of the vibration animation.
    // Values in milliseconds, labels in seconds
    JPanel speedPanel = new JPanel();
    speedPanel.setLayout(new BorderLayout());
    speedPanel.setBorder(new TitledBorder(jrh.getString("speedLabel")));
    JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
    speedSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    speedSlider.setPaintTicks(true);
    speedSlider.setMajorTickSpacing(50);
    speedSlider.setMinorTickSpacing(10);
    Hashtable labelTable = new Hashtable();
    labelTable.put(new Integer(0), new JLabel("0.0"));
    labelTable.put(new Integer(50), new JLabel("0.5"));
    labelTable.put(new Integer(100), new JLabel("1.0"));
    speedSlider.setLabelTable(labelTable);
    speedSlider.setPaintLabels(true);
    speedSlider.addChangeListener(new SpeedSliderChangeListener());
    speedPanel.add(speedSlider);
    container.add(speedPanel);

    JPanel savePanel = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    savePanel.setLayout(gridbag);
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.weighty = 1.0;
    savePanel.setBorder(new TitledBorder(jrh.getString("saveLabel")));

    JButton snmb = new JButton(jrh.getIcon("saveImage"));
    snmb.setMargin(new Insets(1, 1, 1, 1));
    snmb.setActionCommand("save");
    snmb.addActionListener(this);
    c.gridx = 0;
    c.gridy = 0;
    gridbag.setConstraints(snmb, c);
    savePanel.add(snmb);

    JLabel snml = new JLabel(jrh.getString("saveTooltip"));
    c.gridx = 1;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(snml, c);
    savePanel.add(snml);

    JButton sgb = new JButton(jrh.getIcon("movieImage"));
    sgb.setMargin(new Insets(1, 1, 1, 1));
    sgb.setActionCommand("movie");
    sgb.addActionListener(this);
    sgb.setEnabled(false);
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 1;
    gridbag.setConstraints(sgb, c);
    savePanel.add(sgb);

    JLabel sgl = new JLabel(jrh.getString("movieTooltip"));
    c.gridx = 1;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(sgl, c);
    savePanel.add(sgl);

    container.add(savePanel);

    addWindowListener(new VibrateWindowListener());
    getContentPane().add(container);
    pack();
    centerDialog();
  }

  /**
   * Set ChemFile from which the vibrations will be loaded.
   * If no vibration data is found, the Vibrate action will
   * be disabled; otherwise, it will be enabled.
   *
   * @param inputFile the ChemFile containing vibration data
   */
  public void setChemFile(ChemFile inputFile) {

    stop();
    if (isVisible()) {
      setVisible(false);
    }
    restoreConflictingActions();
    hasVibrations = false;
    this.inputFile = inputFile;
    if (frameCombo.getItemCount() > 0) {
      frameCombo.removeAllItems();
    }
    frameIds.removeAllElements();
    for (int i = 0; i < inputFile.nFrames(); ++i) {
      ChemFrame frame2 = inputFile.getFrame(i);
      if (frame2.getNumberVibrations() > 0) {
        hasVibrations = true;

        // Add to list of frames with vibrations.
        frameCombo.addItem("Frame " + i + " (" + frame2.getNumberVibrations()
                + " normal modes)");
        frameIds.addElement(new Integer(i));
      }
    }
    if (frameCombo.getItemCount() > 1) {
      frameCombo.setEnabled(true);
    } else {
      frameCombo.setEnabled(false);
    }
    if (hasVibrations) {
      vibrateAction.setEnabled(true);
    } else {
      vibrateAction.setEnabled(false);
    }

    // Setup default vibration file
    // Input frame and vibration selection
    if (frameIds.size() > 0) {
      inputFrameNumber = ((Integer) frameIds.firstElement()).intValue();
      ChemFrame frame2 = inputFile.getFrame(inputFrameNumber);
      if (vibCombo.getItemCount() > 0) {
        vibCombo.removeAllItems();
      }
      Enumeration iter = frame2.getVibrations();
      while (iter.hasMoreElements()) {
        Vibration vib = (Vibration) iter.nextElement();
        vibCombo.addItem(" " + vib.getLabel() + " cm^-1");
      }
      vibrationNumber = 0;
      createVibration();
    }
  }

  /**
   * Sets the frame of the display panel and updates the slider.
   *
   * @param which the frame number
   * @param setSlider true if we should set the slider position also
   */
  private void setFrame(int which, boolean setSlider) {

    display.setFrame(which);
    ChemFrame frame = vibFile.getFrame(which);
    if (setSlider) {
      progressSlider.setValue(which + 1);
    }
  }

  /**
   * Creates vibration from selected vibration and selected frame
   * of the input file.
   */
  private void createVibration() {

    // Create set of frames animating the vectors
    ChemFrame inputFrame = inputFile.getFrame(inputFrameNumber);
    Vibration vib = inputFrame.getVibration(vibrationNumber);
    vibFile = new ChemFile();
    for (int n = 0; n < numberFrames; ++n) {
      int numberVerticies = inputFrame.getNumberOfAtoms();
      ChemFrame newFrame = new ChemFrame(numberVerticies);
      for (int i = 0; i < numberVerticies; ++i) {
        double scaling = amplitudeScale
                           * Math.sin(2.0 * Math.PI * n / numberFrames);
        Atom atom = inputFrame.getAtomAt(i);
        double[] coord = inputFrame.getAtomCoords(i);
        double[] force = vib.getAtomVector(i);
        double[] forceVector = new double[3];
        forceVector[0] = vectorScale * force[0];
        forceVector[1] = vectorScale * force[1];
        forceVector[2] = vectorScale * force[2];
        coord[0] += force[0] * scaling;
        coord[1] += force[1] * scaling;
        coord[2] += force[2] * scaling;
        try {
          int atomIndex = newFrame.addAtom(atom.getType(),
                  (float) coord[0], (float) coord[1], (float) coord[2]);
          newFrame.getAtomAt(atomIndex).setVector(
              new Point3f((float) forceVector[0], (float) forceVector[1],
                (float) forceVector[2]));
        } catch (Exception ex) {
          System.out.println(ex);
          ex.printStackTrace();
        }
      }
      vibFile.frames.addElement(newFrame);
    }
    progressSlider.setMaximum(vibFile.nFrames());
    currentFrame = 0;
  }

  /**
   * Shows or hides this component depending on the value of
   * parameter b.
   *
   * @param b If true, shows this component; otherwise, hides
   *   this component.
   */
  public void setVisible(boolean b) {

    if (b) {
      vibrateAction.setEnabled(false);
      display.setChemFile(vibFile);
      currentFrame = 0;
      setFrame(currentFrame, true);
      disableConflictingActions();
    } else {
      stop();
      if (inputFile != null) {
        display.setChemFile(inputFile);
      }
      vibrateAction.setEnabled(true);
      restoreConflictingActions();
    }
    super.setVisible(b);
  }

  /**
   * Start the animation thread for vibrating the molecule
   */
  public void start() {

    if ((vibThread == null) && (vibFile != null)) {
      vibThread = new Thread(this, "Vibration");
      vibThread.setPriority(Thread.MIN_PRIORITY);
      vibThread.start();
      playing = true;
    }
  }

  /**
   * Runs animation of the selected vibration.
   * The vibration runs until <code>stop()</code> is called.
   * Each frame in the vibration is displayed in sequence.
   * The speed of the animation is determined by <code>sleepiness</code>
   * which is controlled by the speed slider.
   */
  public void run() {

    while (playing) {
      if (currentFrame < vibFile.nFrames() - 1) {
        currentFrame++;
      } else {
        currentFrame = 0;
      }
      setFrame(currentFrame, true);
      try {
        Thread.sleep(sleepiness);
      } catch (InterruptedException e) {

        // Ignore
      }
    }
    vibThread = null;
  }

  /**
   * Stop the animation thread.
   */
  public void stop() {
    playing = false;
  }

  /**
   * Controls animation playback.
   */
  public void actionPerformed(ActionEvent evt) {

    if (vibFile != null) {
      String arg = evt.getActionCommand();
      if (arg.equals("rewind")) {
        stop();
        currentFrame = 0;
        setFrame(currentFrame, true);
      }
      if (arg.equals("ff")) {
        stop();
        currentFrame = vibFile.nFrames() - 1;
        setFrame(currentFrame, true);
      }
      if (arg.equals("next")) {
        stop();
        if (currentFrame < vibFile.nFrames() - 1) {
          currentFrame++;
        }
        setFrame(currentFrame, true);
      }
      if (arg.equals("prev")) {
        stop();
        if (currentFrame > 0) {
          currentFrame--;
        }
        setFrame(currentFrame, true);
      }
      if (arg.equals("pause")) {
        stop();
      }
      if (arg.equals("play")) {
        start();
      }
      if (arg.equals("save")) {
        createVibration();
        FileTyper ft = new FileTyper(saveChooser);
        saveChooser.setAccessory(ft);
        if (System.getProperty("user.dir") != null) {
          File currentDir = new File(System.getProperty("user.dir"));
          saveChooser.setCurrentDirectory(currentDir);
        }
        int retval = saveChooser.showSaveDialog(this);
        if (retval == 0) {
          File theFile = saveChooser.getSelectedFile();
          if (theFile != null) {
            try {
              if (ft.getType().equals("XYZ (xmol)")) {
                FileOutputStream os = new FileOutputStream(theFile);
                XYZSaver xyzs = new XYZSaver(vibFile, os);
                xyzs.writeFile();
                os.flush();
                os.close();
              } else if (ft.getType().equals("CML")) {
                FileOutputStream os = new FileOutputStream(theFile);
                CMLSaver cs = new CMLSaver(vibFile, os);
                cs.writeFile();
                os.flush();
                os.close();
              } else if (ft.getType().equals("PDB")) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "The PDB format is not currently supported."
                          + "\nPlease use XYZ (.xyz) or CML (.cml)",
                            "File type error",
                              javax.swing.JOptionPane.ERROR_MESSAGE);
              } else if (ft.getType().equals("Automatic")) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "No file type found for file \"" + theFile.getName()
                          + "\"." + "\nPlease use XYZ (.xyz) or CML (.cml)",
                            "File type error",
                              javax.swing.JOptionPane.ERROR_MESSAGE);
              }

            } catch (Exception exc) {
              System.out.println(exc.toString());
            }
          }
        }
      }
      if (arg.equals("movie")) {
        javax.swing.JOptionPane.showMessageDialog(null,
                "Saving movies is not yet implemented.",
                  "Incomplete implementation",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Returns the preferred size of the Vibrate dialog.
   *
   * @return the Dimension preferred by the dialog
   */
  public Dimension getPreferredSize() {
    return new Dimension(280, 385);
  }

  /**
   * Centers the dialog on the screen.
   */
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

  /**
   * The action used to activate this dialog.
   */
  class VibrateAction extends AbstractAction {

    /**
     * Create the action.
     */
    public VibrateAction() {
      super("vibrate");
      this.setEnabled(false);
    }

    /**
     * Shows the Vibrate dialog when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      setVisible(true);
    }
  }

  /**
   * Returns the actions available from the Vibrate dialog.
   *
   * @return the actions for the Vibrate dialog
   */
  public Action[] getActions() {
    return new Action[] {
      vibrateAction
    };
  }

  /**
   * Gets the Vibrate action identified by the string given.
   *
   * @param cmd key for the desired action
   * @return the action associated with cmd, or null if not found
   */
  public Action getAction(String cmd) {
    if (cmd.equals(vibrateAction.getValue(Action.NAME))) {
      return vibrateAction;
    }
    return null;
  }

  /**
   * Add a action to be disabled when this dialog is shown.
   *
   * @param a1  a conflicting action
   */
  public void addConflictingAction(Action a1) {
    conflictingActions.addElement(a1);
  }

  /**
   * Disables conflicting actions. Saves action enabled
   * states for restoration.
   */
  private void disableConflictingActions() {

    actionStates = new Vector();
    Enumeration iter = conflictingActions.elements();
    while (iter.hasMoreElements()) {
      Action a1 = (Action) iter.nextElement();
      actionStates.addElement(new Boolean(a1.isEnabled()));
      a1.setEnabled(false);
    }
  }

  /**
   * Restores conflicting actions to saved state.
   */
  private void restoreConflictingActions() {

    if (actionStates != null) {
      Enumeration iter = conflictingActions.elements();
      Enumeration stateIter = actionStates.elements();
      while (iter.hasMoreElements() && stateIter.hasMoreElements()) {
        Action a1 = (Action) iter.nextElement();
        boolean state = ((Boolean) stateIter.nextElement()).booleanValue();
        a1.setEnabled(state);
      }
      actionStates = null;
    }
  }

  /**
   * Listener for responding to changes in the progress slider.
   */
  class ProgressSliderChangeListener implements ChangeListener {

    /**
     * Changes the current frame displayed to remain current with
     * the slider state.
     */
    public void stateChanged(ChangeEvent e) {

      JSlider source = (JSlider) e.getSource();
      int fr = source.getValue();
      if (fr - 1 != currentFrame) {
        currentFrame = fr - 1;
        setFrame(fr - 1, false);
      }
    }
  }

  /**
   * Listener for responding to changes in the speed slider.
   */
  class SpeedSliderChangeListener implements ChangeListener {

    /**
     * Changes the animation speed to remain current with
     * the slider state.
     */
    public void stateChanged(ChangeEvent e) {
      JSlider source = (JSlider) e.getSource();
      int speed = source.getValue();
      sleepiness = speedScale * source.getValue();
    }
  }

  /**
   * Listener for responding to dialog window events.
   */
  class VibrateWindowListener extends WindowAdapter {

    /**
     * Closes the dialog when window closing event occurs.
     */
    public void windowClosing(WindowEvent e) {
      setVisible(false);
    }
  }

  /**
   * Listener for responding to frame selection combo box.
   */
  class FrameComboItemListener implements ItemListener {

    /**
     * Changes the source frame of the input file to new selection.
     */
    public void itemStateChanged(ItemEvent e) {

      JComboBox source = (JComboBox) e.getSource();
      int value = source.getSelectedIndex();
      if ((value >= 0) && (value < frameIds.size())) {
        inputFrameNumber = ((Integer) frameIds.elementAt(value)).intValue();
        ChemFrame frame2 = inputFile.getFrame(inputFrameNumber);
        vibCombo.removeAllItems();
        Enumeration iter = frame2.getVibrations();
        while (iter.hasMoreElements()) {
          Vibration vib = (Vibration) iter.nextElement();
          vibCombo.addItem(" " + vib.getLabel());
        }
        setSize(getPreferredSize());
      }
    }
  }

  /**
   * Listener for responding to vibration selection combo box.
   */
  class VibComboItemListener implements ItemListener {

    /**
     * Changes the source vibration of the input frame
     * to new selection.
     */
    public void itemStateChanged(ItemEvent e) {

      JComboBox source = (JComboBox) e.getSource();
      vibrationNumber = source.getSelectedIndex();
      if (isVisible()) {
        createVibration();
        display.setChemFile(vibFile);
        currentFrame = 0;
        setFrame(currentFrame, true);
      }
    }
  }

  /**
   * Combo box for selecting the frame containing vibration data.
   */
  private JComboBox frameCombo = new JComboBox();

  /**
   * Combo box for selecting the vibration to animate.
   */
  private JComboBox vibCombo = new JComboBox();

  /**
   * Slider for selecting a frame of the vibration animation.
   */
  private JSlider progressSlider = new JSlider(JSlider.HORIZONTAL, 1, 1, 1);

  /**
   * Thread for animation of vibration
   */
  private Thread vibThread = null;

  /**
   * Is the animation playing.
   */
  private boolean playing = false;

  /**
   * Scale units on speed slider to sleepiness in milliseconds.
   */
  private static final int speedScale = 10;

  /**
   * Sleep time in milliseconds during animation playback.
   */
  private int sleepiness = 100;

  /**
   * Current frame of the vibration animation being displayed.
   */
  private static int currentFrame;

  /**
   * Reference to the panel for displaying frames. Used to load and unload
   * the vibration ChemFile, and to set the frame to be displayed.
   */
  private DisplayPanel display;

  /**
   * Does the dialog have any vibration data.
   */
  private boolean hasVibrations;

  /**
   * The current frame in inputFile to be used for obtaining vibration data.
   */
  private int inputFrameNumber;

  /**
   * The current vibration within frame <code>inputFrameNumber</code>
   * to be used for creating the animation (i.e., <code>vibFile</code>).
   */
  private int vibrationNumber;

  /**
   * The ChemFile containing frames with vibration data.
   */
  private ChemFile inputFile;

  /**
   * List of indexes for vibration-containing frames of the input file.
   * Used to map frame combo box selection to the frame in the input file.
   */
  private Vector frameIds = new Vector();

  /**
   * The ChemFile created for animating a vibration.
   */
  private ChemFile vibFile;

  /**
   * The action used to activate this dialog.
   */
  private VibrateAction vibrateAction = new VibrateAction();

  /**
   * List of actions which will interfere with the operation of this
   * dialog.
   */
  private Vector conflictingActions = new Vector();

  /**
   * Enabled states of conficting actions which are disabled
   * during the operation of this dialog. Used to restore states
   * when the dialog is closed.
   */
  private Vector actionStates = null;

  /**
   * Scale factor for the amplitude of vibrations.
   */
  private static double amplitudeScale = 0.7;

  /**
   * Scale factor for the atomic force vectors of vibrations.
   */
  private static double vectorScale = 1.0;

  /**
   * Sets the number of frames to be created for the vibration animations.
   */
  private static int numberFrames = 20;

  /**
   * Resource handler for loading interface strings and icons.
   */
  private static JmolResourceHandler jrh = new JmolResourceHandler("Vibrate");

  /**
   * FileChooser for saving normal mode files.
   */
  private static JFileChooser saveChooser = new JFileChooser();
}
