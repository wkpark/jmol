
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
import javax.vecmath.Point3f;

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
  public synchronized void paint(Graphics g, ChemFrame frame,
      DisplaySettings settings) {

    if (frame.getNumberOfAtoms() <= 0) {
      return;
    }
    boolean drawHydrogen = settings.getShowHydrogens();
    frame.transform();
    if (!settings.getFastRendering() || (atomReferences == null)) {
      if ((atomReferences == null)
          || (atomReferences.length != frame.getNumberOfAtoms())) {
        atomReferences = new AtomReference[frame.getNumberOfAtoms()];
        for (int i = 0; i < atomReferences.length; ++i) {
          atomReferences[i] = new AtomReference();
        }
      }
      for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
        atomReferences[i].index = i;
        atomReferences[i].z = frame.getAtomAt(i).getScreenPosition().z;
      }

      if (frame.getNumberOfAtoms() > 1) {
        sorter.sort(atomReferences);
      }
    }

    double maxMagnitude = -1.0;
    double minMagnitude = Double.MAX_VALUE;
    for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
      Atom atom = frame.getAtomAt(i);
      Point3f vector = atom.getVector();
      if (vector != null) {
        double magnitude = vector.distance(zeroPoint);
        if (magnitude > maxMagnitude) {
          maxMagnitude = magnitude;
        }
        if (magnitude < minMagnitude) {
          minMagnitude = magnitude;
        }
      }
    }
    double magnitudeRange = maxMagnitude - minMagnitude;

    BondRenderer bondRenderer = getBondRenderer(settings);
    for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
      int j = atomReferences[i].index;
      Atom atom = frame.getAtomAt(j);
      if (drawHydrogen || (atom.getType().getAtomicNumber() != 1)) {
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
          atomRenderer.paint(g, atom, frame.isAtomPicked(j), settings);
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
            double magnitude = atom.getVector().distance(zeroPoint);
            double scaling = (magnitude - minMagnitude) / magnitudeRange
                               + 0.5;
            ArrowLine al = new ArrowLine(g, atom.getScreenPosition().x,
                             atom.getScreenPosition().y,
                             atom.getScreenVector().x,
                             atom.getScreenVector().y, false, true, scaling);
          }
        }

      }
    }
  }

  private BondRenderer getBondRenderer(DisplaySettings settings) {

    BondRenderer renderer;
    if (settings.getFastRendering()
        || (settings.getBondDrawMode() == DisplaySettings.LINE)) {
      renderer = lineBondRenderer;
    } else if (settings.getBondDrawMode() == DisplaySettings.SHADING) {
      renderer = shadingBondRenderer;
    } else if (settings.getBondDrawMode() == DisplaySettings.WIREFRAME) {
      renderer = wireframeBondRenderer;
    } else {
      renderer = quickdrawBondRenderer;
    }
    return renderer;
  }

  /**
   * Renderer for atoms.
   */
  private AtomRenderer atomRenderer = new AtomRenderer();

  /**
   * Renderer for bonds.
   */
  private BondRenderer quickdrawBondRenderer = new QuickdrawBondRenderer();
  private BondRenderer lineBondRenderer = new LineBondRenderer();
  private BondRenderer shadingBondRenderer = new ShadingBondRenderer();
  private BondRenderer wireframeBondRenderer = new WireframeBondRenderer();

  class AtomReference {
    int index = 0;
    float z = 0.0f;
  }

  AtomReference[] atomReferences;

  HeapSorter sorter = new HeapSorter(new HeapSorter.Comparator() {

    public int compare(Object atom1, Object atom2) {

      AtomReference a1 = (AtomReference) atom1;
      AtomReference a2 = (AtomReference) atom2;
      if (a1.z < a2.z) {
        return -1;
      } else if (a1.z > a2.z) {
        return 1;
      }
      return 0;
    }
  });

  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3f zeroPoint = new Point3f();
}

