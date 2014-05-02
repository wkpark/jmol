package org.jmol.adapter.readers.cif;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.SB;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.MSInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Modulation;
import org.jmol.util.ModulationSet;
import org.jmol.util.Tensor;

/**
 * mCIF reader
 *  
 * @author Bob Hanson hansonr@stolaf.edu 5/2/2014
 * 
 */

public class MagCifReader implements MagCifInterface {

  protected CifReader cr; // Cif or Jana


  public MagCifReader() {
    // for reflection from Jana
  }

  @Override
  public void initialize(CifReader r) {
    cr = r;
  }

}
