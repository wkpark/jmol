package org.openscience.jmol;

import java.awt.Graphics;
import java.util.Hashtable;

/**
 * Graphical representation of an atom.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class AtomShape implements Shape {

  Atom atom;
  DisplaySettings settings;
  
  AtomShape(Atom atom, DisplaySettings settings) {
    this.atom = atom;
    this.settings = settings;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Atom shape for ");
    buffer.append(atom);
    buffer.append(": z = ");
    buffer.append(getZ());
    return buffer.toString();
  }

  public void render(Graphics g) {
    AtomRenderer atomRenderer = getAtomRenderer(settings);
    atomRenderer.paint(g, atom, settings.isAtomPicked(atom), settings);
  }
  
  public double getZ() {
    return atom.getScreenPosition().z;
  }
  
  private static AtomRenderer getAtomRenderer(DisplaySettings settings) {

    AtomRenderer renderer;
    if (settings.getFastRendering()
        || (settings.getAtomDrawMode() == DisplaySettings.WIREFRAME)) {
      renderer = wireframeAtomRenderer;
    } else if (settings.getAtomDrawMode() == DisplaySettings.SHADING) {
      renderer = shadingAtomRenderer;
    } else {
      renderer = quickdrawAtomRenderer;
    }
    return renderer;
  }

  private static AtomRenderer quickdrawAtomRenderer = new QuickdrawAtomRenderer();
  private static AtomRenderer shadingAtomRenderer = new ShadingAtomRenderer();
  private static AtomRenderer wireframeAtomRenderer = new WireframeAtomRenderer();
}

