/*
 * @(#)ListNode.java	1.0 99/08/19
 *
 * Copyright (c) 1999 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */
package org.openscience.jmol;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.Vector;

public class ListNode extends DefaultMutableTreeNode {

    /** Have the children of this node been loaded yet? */
    protected boolean           hasLoaded;
    protected Vector            v;

    /**
     * Constructs a new ListNode instance with o as the user
     * object.
     */
    public ListNode(Object o) {
	super(o);
    }

    public ListNode(String s, Vector v) {
        super((Object) s);
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
	if(!hasLoaded) {
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
            
            for (Enumeration e = v.elements() ; e.hasMoreElements() ;) {
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
