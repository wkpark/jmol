
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
package org.openscience.miniJmol;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.event.EventListenerList;

/**
 * A reader for Gaussian98 output.
 * Gaussian 98 is a quantum chemistry program
 * by Gaussian, Inc. (http://www.gaussian.com/).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all Gaussian98 output. If you have problems,
 * please contact the author of this code, not the developers
 * of Gaussian98.
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.2
 */
public class Gaussian98Reader implements ChemFileReader {

  /**
   * Create an Gaussian98 output reader.
   *
   * @param input source of Gaussian98 data
   */
  public Gaussian98Reader(Reader input) {
    this.input = new BufferedReader(input);
  }

  /**
   * Whether bonds are enabled in the files and frames read.
   */
  private boolean bondsEnabled = true;
  
  /**
   * Sets whether bonds are enabled in the files and frames which are read.
   *
   * @param bondsEnabled if true, enables bonds.
   */
  public void setBondsEnabled(boolean bondsEnabled) {
    this.bondsEnabled = bondsEnabled;
  }
  
  /**
   * Read the Gaussian98 output.
   *
   * @return a ChemFile with the coordinates, energies, and vibrations.
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(bondsEnabled);
    ChemFrame frame = null;
    String line = input.readLine();
    String levelOfTheory = null;
    int frameNum = 1;

    // Find first set of coordinates
    while (input.ready() && (line != null)) {
      if (line.indexOf("Standard orientation:") >= 0) {

        // Found a set of coordinates
        frame = new ChemFrame(bondsEnabled);
        readCoordinates(frame, frameNum);
        frameNum++;
        break;
      }
      line = input.readLine();
    }
    if (frame != null) {

      // Read all other data
      line = input.readLine();
      while (input.ready() && (line != null)) {
        if (line.indexOf("Standard orientation:") >= 0) {

          // Found a set of coordinates
          // Add current frame to file and create a new one.
          file.addFrame(frame);
          fireFrameRead();
          frame = new ChemFrame(bondsEnabled);
          readCoordinates(frame, frameNum);
          frameNum++;
        } else if (line.indexOf("SCF Done:") >= 0) {

          // Found an energy
          frame.setInfo(line.trim());
        } else if (line.indexOf("GINC") >= 0) {

          // Found calculation level of theory
          levelOfTheory = parseLevelOfTheory(line);
        }
        line = input.readLine();
      }

      // Add current frame to file
      file.addFrame(frame);
      fireFrameRead();
    }
    return file;
  }

  /**
   * Reads a set of coordinates into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  private void readCoordinates(ChemFrame frame, int frameNum)
            throws IOException {

    String line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    while (input.ready()) {
      line = input.readLine();
      if ((line == null) || (line.indexOf("-----") >= 0)) {
        break;
      }
      int atomicNumber = 0;
      StringReader sr = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(sr);
      token.nextToken();

      // ignore first token
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        atomicNumber = (int) token.nval;
        if (atomicNumber == 0) {

          // Skip dummy atoms. Dummy atoms must be skipped
          // if frequencies are to be read because Gaussian
          // does not report dummy atoms in frequencies, and
          // the number of atoms is used for reading frequencies.
          continue;
        }
      } else {
        throw new IOException("Error reading coordinates");
      }
      token.nextToken();

      // ignore third token
      double x = 0.0;
      double y = 0.0;
      double z = 0.0;
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        x = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        y = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        z = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      frame.addAtom(atomicNumber, (float) x, (float) y, (float) z);
    }
  }

  /**
   * Select the theory and basis set from the first archive line.
   */
  private String parseLevelOfTheory(String line) {

    StringTokenizer st1 = new StringTokenizer(line, "\\");

    // Must contain at least 6 tokens
    if (st1.countTokens() < 6) {
      return null;
    }

    // Skip first four tokens
    for (int i = 0; i < 4; ++i) {
      st1.nextToken();
    }
    return st1.nextToken() + "/" + st1.nextToken();
  }

  /**
   * Holder of reader event listeners.
   */
  private Vector listenerList = new Vector();
  
  /**
   * An event to be sent to listeners. Lazily initialized.
   */
  private ReaderEvent readerEvent = null;
  
  /**
   * Adds a reader listener.
   *
   * @param l the reader listener to add.
   */
  public void addReaderListener(ReaderListener l) {
    listenerList.addElement(l);
  }
  
  /**
   * Removes a reader listener.
   *
   * @param l the reader listener to remove.
   */
  public void removeReaderListener(ReaderListener l) {
    listenerList.remove(l);
  }
  
  /**
   * Sends a frame read event to the reader listeners.
   */
  private void fireFrameRead() {
    for (int i = 0; i < listenerList.size(); ++i) {
      ReaderListener listener = (ReaderListener) listenerList.elementAt(i);
      // Lazily create the event:
      if (readerEvent == null) {
        readerEvent = new ReaderEvent(this);
      }
      listener.frameRead(readerEvent);
    }
  }
 

  /**
   * The source for Gaussian98 data.
   */
  private BufferedReader input;
}
