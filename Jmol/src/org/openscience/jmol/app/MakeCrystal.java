/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2001-2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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


import java.awt.event.*;
import javax.swing.AbstractAction;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;

/**
 * Make crystal. Calls CrystalPropertiesDialog in the case
 * no crystal data is available.
 *
 * @author  Fabian Dortu (Fabian Dortu@wanadoo.be)
 * @version 1.3
 */
public class MakeCrystal
  implements ActionListener, PropertyChangeListener {
  
  // This class is instanciated in Jmol.java as "makecrystal". 
  // In Properties/Jmol.Properties, must
  // be defined as "makecrystal" exactly!
  
  
  boolean hasCrystalInfo;
  static private MakecrystalAction makecrystalAction;
  private CrystalPropertiesDialog crystalPropertiesDialog ;

  
  /**
   * Constructor
   */
  public MakeCrystal(DisplayControl control,
		     CrystalPropertiesDialog crystalPropertiesDialog) {
    makecrystalAction = new MakecrystalAction();
    this.crystalPropertiesDialog = crystalPropertiesDialog;
    makecrystalAction.setEnabled(false);
  }
  
  /**
   * Enable or disable Makecrystal action.
   * This method is intend to be call by CrystalPropertiesDialog
   */
  static public void setEnabled(boolean isEnabled) {
    makecrystalAction.setEnabled(isEnabled);
  }
  
  /**
   * Set the Crystal properties of the file.
   * This function is called when a new file is opened.
   *
   * @param cf the ChemFile
   */
  public void setChemFile(ChemFile cf) {
    if (cf instanceof CrystalFile) {
      hasCrystalInfo = true;
    } else if (cf instanceof ChemFile) {
      hasCrystalInfo = false;
    }
    
    // Set if the makecrystal Action is enabled 
    // or not (appear black or gray in the menu)
    if (hasCrystalInfo) {                 
      makecrystalAction.setEnabled(false);    //appear gray (disabled)
    } else {
      makecrystalAction.setEnabled(true);     //appear black (enabled)
    }
  }  //end setChemFile
  

  class MakecrystalAction extends AbstractAction {

    public MakecrystalAction() {

      super("makecrystal");

      //The makecrystal action is available only if a ChemFile 
      // and not a CrystalFile file is loaded
      if (hasCrystalInfo) {
        this.setEnabled(false);
      } else {
        this.setEnabled(true);
      }
    }

    public void actionPerformed(ActionEvent e) {

      //When the dialog Edit-->Make crystal is clicked,
      //this method is executed.

      //The makecrystal dialog is no more available because already opened
      //this.setEnabled(false); //FIX

      //Popup the crystalPropertiesDialog
      crystalPropertiesDialog.show();
      
    }
  }    //end class MakecrystalAction 


  /**
   * Describe <code>actionPerformed</code> method here.
   *
   * @param evt an <code>ActionEvent</code> value
   */
  public void actionPerformed(ActionEvent evt) {
    //System.out.println(evt.getActionCommand());
  }

  /**
   * Describe <code>propertyChange</code> method here.
   *
   * @param event a <code>PropertyChangeEvent</code> value
   */

  /**
   * Describe <code>getActions</code> method here.
   *
   * @return an <code>Action[]</code> value
   */
  public Action[] getActions() {
    Action[] defaultActions = {
      makecrystalAction
    };
    return defaultActions;
  }

  public void propertyChange(PropertyChangeEvent event) {

    if (event.getPropertyName().equals(DisplayControl.PROP_CHEM_FILE)) {
      setChemFile((ChemFile) event.getNewValue());
    }
  } 

} //end class MakeCrystal
