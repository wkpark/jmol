
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
package org.openscience.jmol;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Central data model for the Jmol application.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class JmolModel {

  /**
   * The active file.
   */
  private ChemFile chemFile;
  
  /**
   * The active frame.
   */
  private ChemFrame chemFrame;
  
  /**
   * Support for property changes.
   */
  private PropertyChangeSupport propertyChangeSupport
      = new PropertyChangeSupport(this);
  
  /**
   * The name of the active file property.
   */
  public static final String chemFileProperty = "chemFile";
  
  /**
   * The name of the active frame property.
   */
  public static final String chemFrameProperty = "chemFrame";
  
  /**
   * Returns the active file.
   *
   * @return the currently active file.
   */
  public ChemFile getChemFile() {
    return chemFile;
  }
  
  /**
   * Sets the active file.
   *
   * @param file the file to be made active.
   */
  public void setChemFile(ChemFile chemFile) {
    ChemFile oldFile = this.chemFile;
    this.chemFile = chemFile;
    propertyChangeSupport.firePropertyChange(chemFileProperty, oldFile, chemFile);
  }
  
  /**
   * Returns the active frame.
   *
   * @return the currently active frame.
   */
  public ChemFrame getChemFrame() {
    return chemFrame;
  }
  
  /**
   * Changes the active frame to the frame with the given index.
   *
   * @param frameIndex the index of the frame to be made active.
   */
  public void setChemFrame(int frameIndex) {
    if (chemFile != null) {
      if (frameIndex < getNumberOfFrames()) {
        ChemFrame oldFrame = chemFrame;
        chemFrame = chemFile.getFrame(frameIndex);
        propertyChangeSupport.firePropertyChange(chemFrameProperty, oldFrame,
            chemFrame);
      }
    }
  }
  
  /**
   * Returns the number of frames available.
   */
  public int getNumberOfFrames() {
    if (chemFile == null) {
      return 0;
    }
    return chemFile.getNumberOfFrames();
  }
  
  /**
   * Adds a listener for property changes.
   *
   * @param listener the listener to be added.
   */
  void addPropertyChangeListener(PropertyChangeListener listener) {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }
  
  /**
   * Adds a listener for specific property changes.
   *
   * @param propertyName the name of the property for which to listen.
   * @param listener the listener to be added.
   */
  void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
  }
  
  /**
   * Removes a listener for property changes.
   *
   * @param listener the listener to be removed.
   */
  void removePropertyChangeListener(PropertyChangeListener listener) {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }
  
  /**
   * Removes a listener for specific property changes.
   *
   * @param propertyName the name of the property for which to listen.
   * @param listener the listener to be removed.
   */
  void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
  }
  
}

