/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.viewer;

class SasFlattenedPointList {
  int count;
  short[] vertexes = new short[32];
  float[] angles = new float[32];
  float[] distances = new float[32];
  
  void reset() {
    count = 0;
  }
  
  void generateTorusSegment(short startingVertex, short vertexIncrement,
                            float startingAngle, float angleIncrement,
                            int segmentCount) {
    if (count < segmentCount) {
      vertexes = Util.ensureLength(vertexes, segmentCount);
      angles = Util.ensureLength(angles, segmentCount);
      distances = Util.ensureLength(distances, segmentCount);
    }
    short vertex = startingVertex;
    float angle = startingAngle;
    for (int i = 0; i < segmentCount; ++i) {
      vertexes[i] = vertex;
      vertex += vertexIncrement;
      angles[i] = angle;
      angle += angleIncrement;
      distances[i] = 0;
    }
  }

  void add(short vertex, float angle, float distance) {
    if (count == vertexes.length) {
      vertexes = Util.doubleLength(vertexes);
      angles = Util.doubleLength(angles);
      distances = Util.doubleLength(distances);
    }
    vertexes[count] = vertex;
    angles[count] = angle;
    distances[count] = distance;
    ++count;
  }
  
  void duplicateFirstPointPlus2Pi() {
    add(vertexes[0], angles[0] + (float)(2*Math.PI), distances[0]);
  }
  
  void sort() {
    for (int i = count; --i > 0; )
      for (int j = i; --j >= 0; )
        if (angles[j] > angles[i]) {
          Util.swap(angles, i, j);
          Util.swap(distances, i, j);
          Util.swap(vertexes, i, j);
        }
  }
  
  int find(float angle) {
    int i;
    for (i = 0; i < count; ++i)
      if (angles[i] >= angle)
        return i;
    return -1;
  }
  
  int findGE(float angle) {
    int min = 0;
    int max = count;
    while (min != max) {
      int mid = (min + max) / 2;
      float midAngle = angles[mid];
      if (midAngle < angle)
        min = mid + 1;
      else
        max = mid;
    }
    return min;
  }

  int findGT(float angle) {
    int min = 0;
    int max = count;
    while (min != max) {
      int mid = (min + max) / 2;
      float midAngle = angles[mid];
      if (midAngle <= angle)
        min = mid + 1;
      else
        max = mid;
    }
    return min;
  }
}
