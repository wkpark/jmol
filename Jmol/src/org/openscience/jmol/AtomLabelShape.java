package org.openscience.jmol;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Enumeration;
import javax.vecmath.Point3f;

class AtomLabelShape implements Shape {

  Atom atom;
  DisplaySettings settings;
  
  AtomLabelShape(Atom atom, DisplaySettings settings) {
    
    this.atom = atom;
    this.settings = settings;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Atom label shape for ");
    buffer.append(atom);
    buffer.append(": z = ");
    buffer.append(getZ());
    return buffer.toString();
  }

  public void render(Graphics gc) {
    int x = atom.screenX;
    int y = atom.screenY;
    int z = atom.screenZ;
    int diameter =
      (int) (2.0f
        * settings.getCircleRadius(z, atom.getType().getVdwRadius()));
    int radius = diameter >> 1;

    if (settings.getLabelMode() != DisplaySettings.NOLABELS) {
      int j = 0;
      String s = null;
      Font font = new Font("Helvetica", Font.PLAIN, radius);
      gc.setFont(font);
      FontMetrics fontMetrics = gc.getFontMetrics(font);
      int k = fontMetrics.getAscent();
      gc.setColor(settings.getTextColor());

      String label = null;
      switch (settings.getLabelMode()) {
      case DisplaySettings.SYMBOLS:
        if (atom.getType() != null) {
          label = atom.getType().getRoot();
        }
        break;

      case DisplaySettings.TYPES:
        if (atom.getType() != null) {
          label = atom.getType().getName();
        }
        break;

      case DisplaySettings.NUMBERS:
        label = Integer.toString(atom.getAtomNumber() + 1);
        break;

      }
      if (label != null) {
        j = fontMetrics.stringWidth(label);
        gc.drawString(label, x - j / 2, y + k / 2);
      }
    }

    if (!settings.getPropertyMode().equals("")) {

      // check to make sure this atom has this property:
      Enumeration propIter = atom.getProperties().elements();
      while (propIter.hasMoreElements()) {
        PhysicalProperty p = (PhysicalProperty) propIter.nextElement();
        if (p.getDescriptor().equals(settings.getPropertyMode())) {

          // OK, we had this property.  Let's draw the value on
          // screen:
          Font font = new Font("Helvetica", Font.PLAIN, radius / 2);
          gc.setFont(font);
          gc.setColor(settings.getTextColor());
          String s = p.stringValue();
          if (s.length() > 5) {
            s = s.substring(0, 5);
          }
          int k = 2 + (int) (radius / 1.4142136f);
          gc.drawString(s, x + k, y - k);
        }
      }
    }

  }
  
  public int getZ() {
    return atom.screenZ + 1;
  }
  
}

