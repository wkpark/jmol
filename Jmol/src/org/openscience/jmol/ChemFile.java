
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

import java.util.Vector;
import java.util.Enumeration;

public class ChemFile {

  private Vector frames = new Vector(1);
  private boolean bondsEnabled = true;
  private Vector propertyList = new Vector();

  /**
   * Very simple class that should be subclassed for each different
   * kind of file that can be read by Jmol.
   */
  public ChemFile() {
  }

  public ChemFile(boolean bondsEnabled) {
    this.bondsEnabled = bondsEnabled;
  }

  public boolean getBondsEnabled() {
    return bondsEnabled;
  }

  /**
   * returns a ChemFrame from a sequence of ChemFrames that make up
   * this ChemFile
   *
   * @see ChemFrame
   * @param whichframe which frame to return
   */
  public ChemFrame getFrame(int whichframe) {
    if (whichframe < frames.size()) {
      return (ChemFrame) frames.elementAt(whichframe);
    }
    return null;
  }

  /**
   * Adds a frame to this file.
   *
   * @param frame the frame to be added
   */
  public void addFrame(ChemFrame frame) {
    frames.addElement(frame);
  }

  /**
   * Set the frame at the given index.
   *
   * @param frame the new frame
   * @param whichframe the index of the frame
   */
  public void setFrame(ChemFrame frame, int whichframe) {
    if (whichframe < frames.size()) {
      frames.setElementAt(frame, whichframe);
    }
  }

  /**
   * Returns the number of frames in this file.
   */
  public int getNumberOfFrames() {
    return frames.size();
  }

  /**
   * Returns the number of frames in this file.
   *
   * @deprecated Use getNumberOfFrames instead.
   */
  public int getNumberFrames() {
    return frames.size();
  }

  /**
   * Returns a list of descriptions for physical properties
   * contained by this file.
   */
  public Vector getPropertyList() {
    return propertyList;
  }

  /**
   * Adds a property description to the property list.
   *
   * @param prop the property description
   */
  public void addProperty(String prop) {
    if (propertyList.indexOf(prop) < 0) {
      propertyList.addElement(prop);
    }
  }

  /**
   * Returns a list of the names of atom properties on frames in this file.
   */
  public Vector getAtomPropertyList() {

    Vector descriptions = new Vector();
    Enumeration frameIter = frames.elements();
    while (frameIter.hasMoreElements()) {
      ChemFrame frame = (ChemFrame) frameIter.nextElement();
      if (frame.getNumberOfAtoms() > 0) {
        Enumeration properties =
          frame.getAtomAt(0).getProperties().elements();
        while (properties.hasMoreElements()) {
          PhysicalProperty property =
            (PhysicalProperty) properties.nextElement();
          if (!descriptions.contains(property.getDescriptor())) {
            descriptions.addElement(property.getDescriptor());
          }
        }
      }
    }
    return descriptions;
  }

  /**
   * Returns a list of the names of frame properties in this file.
   */
  public Vector getFramePropertyList() {

    Vector descriptions = new Vector();
    Enumeration frameIter = frames.elements();
    while (frameIter.hasMoreElements()) {
      ChemFrame frame = (ChemFrame) frameIter.nextElement();
      Enumeration properties = frame.getFrameProperties().elements();
      while (properties.hasMoreElements()) {
        PhysicalProperty property =
          (PhysicalProperty) properties.nextElement();
        if (!descriptions.contains(property.getDescriptor())) {
          descriptions.addElement(property.getDescriptor());
        }
      }
    }
    return descriptions;
  }

}

