package org.openscience.jmol;

import java.awt.Graphics;

/**
 * Graphical representation of a bond between two atoms.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class BondShape implements Shape {

  Atom atom1;
  Atom atom2;
  DisplaySettings settings;
  
  BondShape(Atom atom1, Atom atom2, DisplaySettings settings) {
    this.atom1 = atom1;
    this.atom2 = atom2;
    this.settings = settings;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Bond shape from ");
    buffer.append(atom1);
    buffer.append(" to ");
    buffer.append(atom2);
    buffer.append(": z = ");
    buffer.append(getZ());
    return buffer.toString();
  }
  
  public void render(Graphics g) {
    BondRenderer bondRenderer = getBondRenderer(settings);
    bondRenderer.paint(g, atom1, atom2, settings);
  }
  
  public int getZ() {
    return (3*atom1.screenZ + atom2.screenZ)/4;
  }
  
  
  private static BondRenderer getBondRenderer(DisplaySettings settings) {

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

  private static BondRenderer quickdrawBondRenderer = new QuickdrawBondRenderer();
  private static BondRenderer lineBondRenderer = new LineBondRenderer();
  private static BondRenderer shadingBondRenderer = new ShadingBondRenderer();
  private static BondRenderer wireframeBondRenderer = new WireframeBondRenderer();
}

