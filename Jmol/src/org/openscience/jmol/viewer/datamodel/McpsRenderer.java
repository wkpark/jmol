/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.*;
import org.openscience.jmol.viewer.pdb.*;
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

abstract class McpsRenderer extends ShapeRenderer {

  void render() {
    if (shape == null)
      return;
    Mcps mcps = (Mcps)shape;
    for (int m = mcps.getModelCount(); --m >= 0; ) {
      Mcps.Model model = mcps.getMcpsModel(m);
      if (displayModel > 0 && displayModel != model.modelNumber)
        continue;
      for (int c = model.getChainCount(); --c >= 0; ) {
        Mcps.Chain mcpsChain = model.getMcpsChain(c);
        if (mcpsChain.polymerCount >= 2)
          renderMcpsChain(mcpsChain);
      }
    }
  }

  abstract void renderMcpsChain(Mcps.Chain mcpsChain);
}
