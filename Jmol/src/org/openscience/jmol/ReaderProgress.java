
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


/**
 * Displays progress of file reader activities in the status display.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class ReaderProgress implements ReaderListener {
  
  /**
   * Used for reporting the number of frames read.
   */
  private int frameCount = 0;
  
  /**
   * Prefix for the progress message.
   */
  private String prefix = "Frame ";
  
  /**
   * Used to display the progress message.
   */
  private StatusDisplay statusDisplay;
  
  /**
   * Creates a progress monitor which displays messages on the given status
   * display.
   *
   * @param statusDisplay the object for displaying progress messages.
   */
  public ReaderProgress(StatusDisplay statusDisplay) {
    this.statusDisplay = statusDisplay;
  }
  
  /**
   * Sets the name of the file being read. This information is included in the
   * progress message.
   *
   * @param fileName the name of the file being read.
   */
  public void setFileName(String fileName) {
    if (fileName == null) {
      this.prefix = "Frame ";
    } else {
      this.prefix = fileName + ": frame ";
    }
  }
  
  /**
   * Updates status when a new frame is read.
   *
   * @param event information about the event.
   */
  public void frameRead(ReaderEvent event) {
    ++frameCount;
    
    statusDisplay.setStatusMessage(prefix + frameCount + " read.");
  }
  
}


