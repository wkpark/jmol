

package org.openscience.jmol;

import org.openscience.jmol.render.*;
import org.openscience.jmol.util.*;

import javax.swing.JDialog; //DLG
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.Action;
import java.util.Hashtable;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;  //PNL
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Dimension;
import javax.swing.JTextField; //TXF
import javax.swing.JLabel; //LBL
import javax.swing.border.TitledBorder;
import javax.swing.JButton; //BUT
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.AxisAngle4d;
import javax.swing.table.AbstractTableModel;
import javax.swing.JScrollPane;
import java.awt.FlowLayout;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTable;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;

class TransformDialog 
  extends JDialog 
  implements ActionListener, PropertyChangeListener {

  protected Jmol program;
  private JmolModel model;
  private boolean hasFile;

  private Point3d center;
  private Point3d oldCenter;
  private Vector3d direction;
  private double angle;

  private JTextField pointTXF;
  private JTextField directionTXF;
  private JTextField angleTXF;


  // The actions:
  private TransformAction transformAction = new TransformAction();
  private Hashtable commands;
  
  TransformDialog(JmolModel model, JFrame f, Jmol program) {
    
    // Invoke JDialog constructor
    super(f, "Transform...", false);
    this.model = model;
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }
    
    this.program=program;
    makeTransformDialog();
  }
  
  void makeTransformDialog() {
    
    JmolResourceHandler resources = JmolResourceHandler.getInstance();

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    JPanel containerPNL = new JPanel();
    containerPNL.setLayout(gridbag);


    pointTXF = new JTextField(10);
    pointTXF.setText("0.0, 0.0, 0.0");
    directionTXF = new JTextField(10);
    directionTXF.setText("1.0, 0.0, 0.0");
    angleTXF = new JTextField(10);
    angleTXF.setText("90.0");
    
    JPanel rotationPNL = new JPanel();
    rotationPNL.setBorder(new TitledBorder
			  (resources.getString("Transform.rotationLabel")));
    rotationPNL.setLayout(gridbag);
    JPanel axisPNL = new JPanel();
    //    axisPNL.setBorder(new TitledBorder(resources
    //.getString("Crystprop.cartesianLabel")));
    axisPNL.setBorder(new TitledBorder
		      (resources.getString("Transform.axisLabel")));
    axisPNL.setLayout(gridbag);
    JPanel anglePNL = new JPanel();
    anglePNL.setBorder(new TitledBorder
		       (resources.getString("Transform.anglePanelLabel")));
    anglePNL.setLayout(gridbag);


    
    // axisPanel
    JLabel pointLBL = new JLabel
      (resources.getString("Transform.pointLabel"));
    JLabel directionLBL = new JLabel
      (resources.getString("Transform.directionLabel")); 
    JButton pickPointBUT = new JButton("Pick atoms");
    pickPointBUT.addActionListener(new ActionListener() {
	
	public void actionPerformed(ActionEvent e) {
	  //TODO
	  
	}
	
      });
    JButton pickDirBUT = new JButton("Pick atoms");
    
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.NONE;
    c.weightx = 1;
    c.weighty = 1;
    c.gridheight = 2;
    //c.gridwidth = 3;
    c.gridwidth = 2;
    gridbag.setConstraints(pointLBL, c);
    axisPNL.add(pointLBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(pointTXF, c);
    axisPNL.add(pointTXF);
    //c.gridwidth = GridBagConstraints.REMAINDER;
    //gridbag.setConstraints(pickPointBUT, c);
    //axisPNL.add(pickPointBUT);
    c.gridwidth = 2;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(directionLBL, c);
    axisPNL.add(directionLBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(directionTXF, c);
    axisPNL.add(directionTXF);
    //c.gridwidth = GridBagConstraints.REMAINDER;
    //gridbag.setConstraints(pickDirBUT, c);
    //axisPNL.add(pickDirBUT);


    //anglePanel
    JLabel angleLBL = new JLabel
      (resources.getString("Transform.angleLabel"));
       c.gridwidth = 2;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(angleLBL, c);
    anglePNL.add(angleLBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(angleTXF, c);
    anglePNL.add(angleTXF);

    //rotationPanel
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.gridheight = 2;
    gridbag.setConstraints(axisPNL, c);
    rotationPNL.add(axisPNL);
    gridbag.setConstraints(anglePNL, c);
    rotationPNL.add(anglePNL); 
    
    //main container
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.gridheight = 2;
    gridbag.setConstraints(rotationPNL, c);
    containerPNL.add(rotationPNL);
    JButton applyBUT = new JButton((resources.translate("Apply")));
    applyBUT.addActionListener(new ActionListener() {
	
	public void actionPerformed(ActionEvent e) {
	  center = MathUtil.arrayToPoint3d
	    (FieldReader.readField3(pointTXF));
	  direction = MathUtil.arrayToVector3d
	    (FieldReader.readField3(directionTXF));
	  if(direction.x==0.0f && direction.y==0.0f && direction.z==0.0f) {
	    direction.x=1.0f;
	  }
	  angle = FieldReader.readField1(angleTXF);

	  rotate();
	}
      });
    c.gridheight = GridBagConstraints.REMAINDER;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(applyBUT, c);
    containerPNL.add(applyBUT);

    //Draw main container
    getContentPane().add(containerPNL);
    addWindowListener(new TransformWindowListener());
    pack();
    centerDialog();
    
  } //makeTransformDialog


  void rotate() {  //TODO
    program.control.setCenter(center);
    program.control.rotate(new AxisAngle4d(direction.x,
					   direction.y,
					   direction.z,
					   (double)Math.toRadians(angle)));
  }
  
  
  public void setChemFile(ChemFile cf) {
    hasFile = true;
    transformAction.setEnabled(true);    
  } 

  public void close() {
    
    this.setVisible(false);
    transformAction.setEnabled(true);
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
  
  public void actionPerformed(ActionEvent evt) {
  }

  public Action[] getActions() {
    Action[] defaultActions = {
      transformAction
    };
    return defaultActions;
  }

  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(JmolModel.chemFileProperty)) {
      setChemFile((ChemFile) event.getNewValue());
    }
  }

  class TransformAction extends AbstractAction {

    public TransformAction() {

      super("transform");

      //The transform dialog is available only if a file is loaded
      if (hasFile) {
	this.setEnabled(true);
      } else {
	this.setEnabled(false);
      }
    }

    public void actionPerformed(ActionEvent e) {
      //When the dialog View-->Transform is clicked,
      //this method is executed.

      //The transform dialog is no more available because already opened
      this.setEnabled(false);

      //Update the content of the dialog box

      //Show the dialog box
      show();
    }
  }    //end class TransformAction 

  class TransformWindowListener extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      close();
    }
  }

  class MeasureWindowListener extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      close();
    }
  }
  
}
