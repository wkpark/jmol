/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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

package org.jmol.adapter.smarter;


import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import java.io.BufferedReader;
import java.util.StringTokenizer;

// the convention we follow is to use capitalization to separate words and make
// it easy to read ... not necessarily the same as what one would use
// write in a document
class HinReader extends ModelReader {
    
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("hin"); // lower case here 

    readAtoms(reader, 0, 0); // leave this check in
    if (model.atomCount == 0)
      model.errorMessage = "No atoms in file";
    return model;
  }
    

  private String getMolName(String line) {
    if (line == null)
      return ""; // no parenthesis around return statements
    // it is a statement, not a function call;
    StringTokenizer st = new StringTokenizer(line," ");
    int ntok = st.countTokens();
    String[] toks = new String[ntok]; // why are we saving these tokens here?
    for (int j = 0; j < ntok; j++) {
      toks[j] = st.nextToken();
    }
    return toks.length == 3 ? toks[2] : "";
  }
  
  void readAtoms(BufferedReader reader, int modelNumber, int modelCount)
    throws Exception {

    String line;
    StringTokenizer tokenizer;

    /*
      there is almost *never* any reason to write while (true)
      and it adds confusion because one has to go looking for
      why the loop terminates

      I strongly recommend that you use another loop construction

    while ((line = reader.readLine()) != null && ! line.startsWith("mol "))
      ;
    model.setModelName(getMolName(line));

    OR, if you don't like setting the line variable inside the test, 

    do {
      line = reader.readLine();
    } while (line != null && !line.startsWith("mol "));
    model.setModelName(getMolName(line));

    */
    // get past the header stuff
    while (true) {
      line = reader.readLine();
      if (line.indexOf("mol ") == 0) { // don't use indexOf ... it is slow
        String info = getMolName(line);
        model.setModelName(info);
        break;
      }
    }

    /*
      it is not a good idea to have loops that initialize a variable
      before you enter the loop and at the bottom of the loop.
        line = reader.readLine();
        while(true) {
          ...
          line = reader.readLine();
        }
      it is asking for trouble later when someone makes modifications
      to the code. 
      Either:
        1.  set the variable as part of your loop test
        2.  use a for(;;) loop

      this code actually has a problem which often occurs
      with this type of loop construction.

      with both of your while (true) loops, if a comment comes
      in then the code will be stuck in an infinite loop,
      because the value of 'line' never changes

      I did not fix this, but left it alone
    */

    line = reader.readLine();
    while(true) {
      if (line == null) break; // end of file
      // do not use indexOf(...) == 0
      // it searches all the way to the end of the string
      // looking for the match.
      // if (line.length() == 0 || line.charAt(0) == ';')
      if (line.indexOf(';') == 0) continue; // comment line // infinite loop

      Vector cons = new Vector();

      // read data for current molecule
      int atomSerial = 0;
      // this second level of nested looping is somewhat confusing
      while (true) {
        // I think this should be
        // line.startsWith("endmol ")
        // but you have >= 0, which is actually doing a 'contains' search
        if (line.indexOf("endmol ") >= 0) {
          break;
        }
        if (line.indexOf(';') == 0) continue; // comment line // infinite loop!!

        // we should check to ensure that these lines start with "atom "
        // if (! line.startsWith("atom ")) ...

        tokenizer = new StringTokenizer(line, " ");

        int ntoken = tokenizer.countTokens();
        String[] toks = new String[ ntoken ];
        for (int i = 0; i < ntoken; i++)
          toks[i] = tokenizer.nextToken();

        String sym = new String(toks[3]);
        // why are you using a double for this?
        //        double charge = Double.parseDouble(toks[6]);
        float charge = parseFloat(toks[6]);
        // Float.parseFloat(String) does not exist on
        // older JVM platforms.
        // so we should use the ModelReader.parseFloat routines
        float x = parseFloat(toks[7]);
        float y = parseFloat(toks[8]);
        float z = parseFloat(toks[9]);
        int nbond = parseInt(toks[10]);
        // float x = Float.parseFloat(toks[7]);
        // float y = Float.parseFloat(toks[8]);
        // float z = Float.parseFloat(toks[9]);
        // int nbond = Integer.parseInt(toks[10]);

        Atom atom = model.newAtom();
        atom.elementSymbol = sym;
        // this is the formal charge, which must be integer values
        // you actually want the partial charge
        //        atom.formalCharge = (int)charge;
        atom.partialCharge = charge;
        atom.x = x; atom.y = y; atom.z = z;

        // I will look at the vector stuff in detail later
        // you are doing a lot of work here ... I suspect we can
        // simplify it
        // You don't need to use a vector to store everything

        for (int j = 11; j < (11+nbond*2); j += 2) {
          // bond order is an integer
          double bo = 1;
          int s = Integer.parseInt(toks[j]) - 1; // since atoms start from 1 in the file
          char bt = toks[j+1].charAt(0);
          switch(bt) {
          case 's': 
            bo = 1;
            break;
          case 'd': 
            bo = 2;
            break;
          case 't': 
            bo = 3;
            break;      
          case 'a':
            // this should be bondOrder = ModelAdapter.ORDER_AROMATIC;
            bo = 1.5;
            break;
          }
          // ArrayList does not exist on 1.1 JVMs
          // so it won't run under Internet Explorer & old Netscape
          ArrayList ar = new ArrayList(3);
          ar.add(new Integer(atomSerial));
          ar.add(new Integer(s));
          // doubles are generally *not* allowed in the Jmol world
          ar.add(new Double(bo));
          cons.add( ar );
        }
        line = reader.readLine();
      }

      // set up connectivity
      Vector blist = new Vector();
      for (int i = 0; i < cons.size(); i++) {
        ArrayList ar = (ArrayList)cons.get(i);

        // make a reversed list
        ArrayList arev = new ArrayList(3);
        arev.add( ar.get(1) );
        arev.add( ar.get(0) );
        arev.add( ar.get(2) );

        // Now see if ar or arev are already in blist
        if (blist.contains(ar) || blist.contains(arev)) continue;
        else blist.add( ar );
      }

      // now just store all the bonds we have
      for (int i = 0; i < blist.size(); i++) {
        ArrayList ar = (ArrayList)blist.get(i);
        int s = ((Integer)ar.get(0)).intValue();
        int e = ((Integer)ar.get(1)).intValue();
        double bo = ((Double)ar.get(2)).doubleValue();
        //model.addBond( model.newBond(s,e,(int)bo) );
      }
      line = null;
    }
  }
}

