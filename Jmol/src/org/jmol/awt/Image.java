/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.awt;

import java.awt.Container;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;

public class Image {

  public static Object createImage(Object data) {
    if (data instanceof URL)
      return Toolkit.getDefaultToolkit().createImage((URL) data);
    if (data instanceof String)
      return Toolkit.getDefaultToolkit().createImage((String) data);
    if (data instanceof byte[])
      return Toolkit.getDefaultToolkit().createImage((byte[]) data);
    return null;
  }

  public static void waitForDisplay(Container display, Object image) throws InterruptedException {
    MediaTracker mediaTracker = new MediaTracker(display);
    mediaTracker.addImage((java.awt.Image)image, 0);
    mediaTracker.waitForID(0);
  }

  public static int getWidth(Object image) {
    return ((java.awt.Image) image).getWidth(null);
  }
}
