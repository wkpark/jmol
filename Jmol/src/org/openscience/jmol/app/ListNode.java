
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
package org.openscience.jmol.app;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Vector;
import java.util.Enumeration;

public class ListNode extends DefaultMutableTreeNode {

  /** Have the children of this node been loaded yet? */
  protected boolean hasLoaded;
  protected Vector v;

  /**
   * Constructs a new ListNode instance with o as the user
   * object.
   */
  public ListNode(Object o) {
    super(o);
  }

  public ListNode(String s, Vector v) {
    super(s);
    this.v = v;
  }

  public boolean isLeaf() {
    return false;
  }

  /**
   * If hasLoaded is false, meaning the children have not yet been
   * loaded, loadChildren is messaged and super is messaged for
   * the return value.
   */
  public int getChildCount() {
    if (!hasLoaded) {
      loadChildren();
    }
    return super.getChildCount();
  }

  /**
   * Messaged the first time getChildCount is messaged.  Creates
   * children with names from the Vector.
   */
  protected void loadChildren() {

    DefaultMutableTreeNode newNode;

    if (v.isEmpty()) {
      DefaultMutableTreeNode emptyNode = new DefaultMutableTreeNode("Empty");
      insert(emptyNode, 0);
    } else {

      int counter = 0;

      Enumeration e = v.elements();
      while (e.hasMoreElements()) {
        newNode = new DefaultMutableTreeNode(e.nextElement());
        insert(newNode, counter);
        counter++;
      }
    }
    hasLoaded = true;
  }

  protected void update() {
    removeAllChildren();
    hasLoaded = false;
    loadChildren();
  }

}
