/*
 * @(#)JMOLANIMATIONConvention.java   0.1.1 2000/04/07
 *
 * Information can be found at http://openscience.chem.nd.edu/~egonw/cml/
 *
 * Copyright (c) 2000 E.L. Willighagen (egonw@sci.kun.nl)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.openscience.jmol;

import java.util.*;
import org.xml.sax.*;
import com.microstar.xml.*;
import org.openscience.cdopi.*;
import org.openscience.cml.*;

public class JMOLANIMATIONConvention extends Convention {

  public JMOLANIMATIONConvention(CDOInterface cdo) {
    super(cdo);
  };
  
  public JMOLANIMATIONConvention(Convention conv) {
    super(conv);
  }
  
  public CDOInterface returnCDO() {
    return this.cdo;
  };
  
  public void startDocument() {
    super.startDocument();
  };

  public void endDocument() {
    super.endDocument();
  };

  
  public void startElement (String name, AttributeList atts) {
    if (name.equals("list")) {
      System.err.println("Oke, JMOLANIMATION seems to be kicked in :)");
    }
    super.startElement(name, atts);
  };

  public void endElement (String name) {
    super.endElement(name);
  }

  public void characterData (char ch[], int start, int length) {
    super.characterData(ch, start, length);
  }
}
