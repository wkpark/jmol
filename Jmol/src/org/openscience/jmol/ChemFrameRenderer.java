
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

import java.awt.Graphics;
import java.util.Enumeration;

/**
 *  Drawing methods for ChemFrame.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class ChemFrameRenderer {

  /**
   * Paint this model to a graphics context.  It uses the matrix
   * associated with this model to map from model space to screen
   * space.
   *
   * @param g the Graphics context to paint to
   */
  public synchronized void paint(Graphics g, ChemFrame frame, DisplaySettings settings) {

    if ((frame.getAtoms() == null) || (frame.getNumberOfAtoms() <= 0)) {
      return;
    }
    boolean drawHydrogen = settings.getShowHydrogens();
    frame.transform();


    int zs[] = ZsortMap;
    if (zs == null) {
      ZsortMap = zs = new int[frame.getNumberOfAtoms()];
      for (int i = frame.getNumberOfAtoms(); --i >= 0; ) {
        zs[i] = i;
      }
    }

    //Added by T.GREY for quick-draw on move support
    if (!settings.getFastRendering()) {

      /*
       * I use a bubble sort since from one iteration to the next, the sort
       * order is pretty stable, so I just use what I had last time as a
       * "guess" of the sorted order.  With luck, this reduces O(N log N)
       * to O(N)
       */

      for (int i = frame.getNumberOfAtoms() - 1; --i >= 0; ) {
        boolean flipped = false;
        for (int j = 0; j <= i; j++) {
          int a = zs[j];
          int b = zs[j + 1];
          if (frame.getAtoms()[a].getScreenPosition().z > frame.getAtoms()[b].getScreenPosition().z) {
            zs[j + 1] = a;
            zs[j] = b;
            flipped = true;
          }
        }
        if (!flipped) {
          break;
        }
      }
    }
    if (frame.getNumberOfAtoms() <= 0) {
      return;
    }

    for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
      int j = zs[i];
      Atom atom = frame.getAtoms()[j];
      if (drawHydrogen
            || (atom.getType().getAtomicNumber() != 1)) {
        if (settings.getShowBonds()) {
          Enumeration bondIter = atom.getBondedAtoms();
          while (bondIter.hasMoreElements()) {
            Atom otherAtom = (Atom) bondIter.nextElement();
            if (drawHydrogen
                  || (otherAtom.getType().getAtomicNumber() != 1)) {
              if (otherAtom.getScreenPosition().z
                      < atom.getScreenPosition().z) {
                bondRenderer.paint(g, atom, otherAtom, settings);
              }
            }
          }
        }
  
        if (settings.getShowAtoms()) {
          atomRenderer.paint(g, atom, frame.isAtomPicked(j),
                  settings);
        }
  
        if (settings.getShowBonds()) {
          Enumeration bondIter = atom.getBondedAtoms();
          while (bondIter.hasMoreElements()) {
            Atom otherAtom = (Atom) bondIter.nextElement();
            if (drawHydrogen
                  || (otherAtom.getType().getAtomicNumber() != 1)) {
              if (otherAtom.getScreenPosition().z
                      >= atom.getScreenPosition().z) {
                bondRenderer.paint(g, atom, otherAtom, settings);
              }
            }
          }
        }
  
        if (settings.getShowVectors()) {
          if (atom.getVector() != null) {
            double vectorLength = atom.getPosition().distance(atom.getVector());
            double atomRadius =
                settings.getCircleRadius((int) atom.getScreenPosition().z,
                  atom.getType().getVdwRadius());
            double vectorScreenLength =
                settings.getScreenSize((int) (vectorLength * atom.getScreenVector().z));
            
            ArrowLine al = new ArrowLine(g, atom.getScreenPosition().x,
              atom.getScreenPosition().y,
                atom.getScreenVector().x,
                  frame.getAtoms()[j].getScreenVector().y,
                    false, true, atomRadius + vectorScreenLength, vectorScreenLength / 30.0);
          }
        }
      }

    }

    if (frame.getDistanceMeasurements() != null) {
      for (Enumeration e = frame.getDistanceMeasurements().elements(); e.hasMoreElements(); ) {
        Distance d = (Distance) e.nextElement();
        int[] al = d.getAtomList();
        int l = al[0];
        int j = al[1];
        try {
          d.paint(g, settings, (int) frame.getAtoms()[l].getScreenPosition().x,
            (int) frame.getAtoms()[l].getScreenPosition().y,
              (int) frame.getAtoms()[l].getScreenPosition().z,
                (int) frame.getAtoms()[j].getScreenPosition().x,
                  (int) frame.getAtoms()[j].getScreenPosition().y,
                    (int) frame.getAtoms()[j].getScreenPosition().z);
        } catch (Exception ex) {
        }
      }
    }
    if (frame.getAngleMeasurements() != null) {
      for (Enumeration e = frame.getAngleMeasurements().elements(); e.hasMoreElements(); ) {
        Angle an = (Angle) e.nextElement();
        int[] al = an.getAtomList();
        int l = al[0];
        int j = al[1];
        int k = al[2];
        try {
          an.paint(g, settings, (int) frame.getAtoms()[l].getScreenPosition().x,
            (int) frame.getAtoms()[l].getScreenPosition().y,
              (int) frame.getAtoms()[l].getScreenPosition().z,
                (int) frame.getAtoms()[j].getScreenPosition().x,
                  (int) frame.getAtoms()[j].getScreenPosition().y,
                    (int) frame.getAtoms()[j].getScreenPosition().z,
                      (int) frame.getAtoms()[k].getScreenPosition().x,
                        (int) frame.getAtoms()[k].getScreenPosition().y,
                          (int) frame.getAtoms()[k].getScreenPosition().z);
        } catch (Exception ex) {
        }
      }
    }
    if (frame.getDihedralMeasurements() != null) {
      for (Enumeration e = frame.getDihedralMeasurements().elements(); e.hasMoreElements(); ) {
        Dihedral dh = (Dihedral) e.nextElement();
        int[] dhl = dh.getAtomList();
        int l = dhl[0];
        int j = dhl[1];
        int k = dhl[2];
        int m = dhl[3];
        try {
          dh.paint(g, settings, (int) frame.getAtoms()[l].getScreenPosition().x,
            (int) frame.getAtoms()[l].getScreenPosition().y,
              (int) frame.getAtoms()[l].getScreenPosition().z,
                (int) frame.getAtoms()[j].getScreenPosition().x,
                  (int) frame.getAtoms()[j].getScreenPosition().y,
                    (int) frame.getAtoms()[j].getScreenPosition().z,
                      (int) frame.getAtoms()[k].getScreenPosition().x,
                        (int) frame.getAtoms()[k].getScreenPosition().y,
                          (int) frame.getAtoms()[k].getScreenPosition().z,
                            (int) frame.getAtoms()[m].getScreenPosition().x,
                              (int) frame.getAtoms()[m].getScreenPosition().y,
                                (int) frame.getAtoms()[m].getScreenPosition().z);
        } catch (Exception ex) {
        }
      }
    }
  }
  
  /**
   * Renderer for atoms.
   */
  private AtomRenderer atomRenderer = new AtomRenderer();

  /**
   * Renderer for bonds.
   */
  private BondRenderer bondRenderer = new BondRenderer();
  
  private int[] ZsortMap;
}

