
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

import java.awt.Polygon;

/**
 * Contains methods which assist in rendering of molecules.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class RendererUtilities {

  /**
   * Do not use objects of this class; only static methods should be used.
   */
  private RendererUtilities() {
    throw new RuntimeException("The RendererUtilities objects should not be used.");
  }

  /**
   * Creates a polygon centered on a imaginary line from the given coordinates
   * and in the direction of the given angle. The angle is represented by the
   * cos and sin of the angle. The starting position is adjusted by the start
   * offset and perpendicular offset parameters. The length is not adjusted for
   * the starting offset, and therefore, the actual length of the polygon will
   * be the length minus the start offset.
   *
   * @param x1 the x coordinate of the starting position.
   * @param y1 the y coordinate of the starting position.
   * @param cosOfAngle the cosine of the direction angle.
   * @param sinOfAngle the sine of the direction angle.
   * @param startOffset the amount to shorted the polygon on the starting side.
   * @param perpendicularOffset the adjustment to the starting position
   *   perpendicular to the direction of the polygon.
   * @param length the length of the polygon.
   * @param halfWidth the width of polygon on each side.
   * @return the polygon created.
   */
  public static Polygon getBondPolygon(int x1, int y1, double cosOfAngle,
      double sinOfAngle, double startOffset, double perpendicularOffset,
      double length, double halfWidth) {
    
    int xpoints[] = new int[4];
    int ypoints[] = new int[4];

    double xOffset = halfWidth * sinOfAngle;
    double yOffset = halfWidth * cosOfAngle;
    double xDisplacement = -perpendicularOffset * sinOfAngle;
    double yDisplacement = perpendicularOffset * cosOfAngle;
    xpoints[0] = x1 + (int) Math.round(-xOffset + xDisplacement + startOffset * cosOfAngle);
    ypoints[0] = y1 + (int) Math.round(yOffset + yDisplacement + startOffset * sinOfAngle);
    
    xpoints[1] = x1 + (int) Math.round(xOffset + xDisplacement + startOffset * cosOfAngle);
    ypoints[1] = y1 + (int) Math.round(-yOffset + yDisplacement + startOffset * sinOfAngle);
    
    xpoints[2] = x1 + (int) Math.round(xOffset + xDisplacement + length * cosOfAngle);
    ypoints[2] = y1 + (int) Math.round(-yOffset + yDisplacement + length * sinOfAngle);
    
    xpoints[3] = x1 + (int) Math.round(-xOffset + xDisplacement + length * cosOfAngle);
    ypoints[3] = y1 + (int) Math.round(yOffset + yDisplacement + length * sinOfAngle);

    return new Polygon(xpoints, ypoints, 4);
  }
  
  /**
   * Creates a line from the given coordinates
   * and in the direction of the given angle. The angle is represented by the
   * cos and sin of the angle. The starting position is adjusted by the start
   * offset and perpendicular offset parameters. The length is not adjusted for
   * the starting offset, and therefore, the actual length of the line will
   * be the length minus the start offset.
   *
   * @param x1 the x coordinate of the starting position.
   * @param y1 the y coordinate of the starting position.
   * @param cosOfAngle the cosine of the direction angle.
   * @param sinOfAngle the sine of the direction angle.
   * @param startOffset the amount to shorted the line on the starting side.
   * @param perpendicularOffset the adjustment to the starting position
   *   perpendicular to the direction of the line.
   * @param length the length of the line.
   * @return the line represented by int[] { x1, y1, x2, y2 }.
   */
  public static int[] getBondLine(int x1, int y1, double cosOfAngle,
      double sinOfAngle, double startOffset, double perpendicularOffset,
      double length) {
    
    int points[] = new int[4];

    double xDisplacement = -perpendicularOffset * sinOfAngle;
    double yDisplacement = perpendicularOffset * cosOfAngle;
    points[0] = x1 + (int) Math.round(xDisplacement + startOffset * cosOfAngle);
    points[1] = y1 + (int) Math.round(yDisplacement + startOffset * sinOfAngle);
    
    points[2] = x1 + (int) Math.round(xDisplacement + length * cosOfAngle);
    points[3] = y1 + (int) Math.round(yDisplacement + length * sinOfAngle);

    return points;
  }

}
