/*
 * @(#)CMLSaver.java    1.0 99/06/09
 *
 * Copyright (c) 1999 Egon Willighagen All Rights Reserved.
 *
 */

package org.openscience.jmol;

import java.util.Vector;
import java.io.*;

public class CMLSaver extends FileSaver {

    private static int MODEL = 1;
    private static int ANIMATION = 2;

    private int mode;
    private int frame_count;
    private int frames;

    /**
     * Constructor.
     * @param cf the ChemFile to dump.
     * @param out the stream to write the XYZ file to.
     */
    public CMLSaver( ChemFile cf, OutputStream out ) throws IOException {
      super( cf, out );
      mode = MODEL;
      frame_count = 0;
      frames = cf.nFrames();
    }

    public synchronized void writeFile() throws IOException {
      if (frames > 1) {
        System.out.println("Mode: ANIMATION");
	mode = ANIMATION;
      }
      super.writeFile();
    }

    public void writeFileStart(ChemFile cf, BufferedWriter w) throws IOException{
      // Preamble for CML Files
      w.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
      w.write("<!DOCTYPE molecule SYSTEM \"cml.dtd\" [\n");
      w.write("  <!ATTLIST list convention CDATA #IMPLIED>\n");
      w.write("]>\n");
      
      if (mode == ANIMATION) {
        w.write("<list convention=\"");
	if (mode == ANIMATION) {
	  w.write("JMOL-ANIMATION");
	} else {
	  w.write("JMOL-MODEL");
	}
	w.write("\">\n");
      }
    }
    public void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException{
      if (mode == ANIMATION) {
        w.write("</list>");
      }
    }

    /**
     * writes a single frame in CML format to the Writer.
     * @param cf the ChemFrame to write
     * @param w the Writer to write it to
     */
    public void writeFrame(ChemFrame cf, BufferedWriter w) throws IOException {
    
      Charge c = new Charge(0.0);
      boolean writecharge = false;
      boolean writevect = false;
      
      frame_count++;
	
      Vector fp = cf.getAtomProps();
      // test if we have charges or vectors in this frame:
      for (int i = 0; i < fp.size(); i++) {
          String prop = (String) fp.elementAt(i);
          if (prop.equals(c.getDescriptor())) writecharge = true;
      }
    
      w.write("<molecule id=\"FRAME" + frame_count + "\">\n");

      String name = cf.getInfo();
      if (name == null) name = "unknown";
      w.write("  <string title=\"COMMENT\">" + name + "</string>\n");
      
      String ids = "";
      String elementTypes = "";
      String x3s = "";
      String y3s = "";
      String z3s = "";
      String formalCharges = "";

      int count = 0;

      for (int i = 0; i < cf.getNvert(); i++) {

	if (ids.length() > 0) ids += " ";
        ids += "a" + i;

        AtomType a = cf.getAtomType(i);
	if (elementTypes.length() > 0) elementTypes += " ";
        elementTypes += a.getBaseAtomType().getName();

        double[] pos = cf.getVertCoords(i);
	if (x3s.length() > 0) x3s += " ";
	if (y3s.length() > 0) y3s += " ";
	if (z3s.length() > 0) z3s += " ";
        x3s += new Double(pos[0]).toString(); 
        y3s += new Double(pos[1]).toString(); 
        z3s += new Double(pos[2]).toString(); 
	
	if ((++count == 5) && (i+1 < cf.getNvert())) {
	  count = 0;
	  x3s += "\n     ";
	  y3s += "\n     ";
	  z3s += "\n     ";
	}

        if (writecharge) {
          Vector props = cf.getVertProps(i);
	  if (formalCharges.length() > 0) formalCharges += " ";
	  for (int j = 0; j < props.size(); j++) {
            PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
            String desc = p.getDescriptor();
            if (desc.equals(c.getDescriptor())) {
              Charge ct = (Charge) p;
              formalCharges += ct.stringValue();
            }
          }
	}
      }
      w.write("  <atomArray>\n");
        w.write("    <stringArray builtin=\"id\">\n");
	w.write("      " + ids + "\n");
        w.write("    </stringArray>\n");
        w.write("    <stringArray builtin=\"elementType\">\n");
	w.write("      " + elementTypes + "\n");
        w.write("    </stringArray>\n");
	w.write("    <floatArray builtin=\"x3\">\n");
	w.write("      " + x3s + "\n");
	w.write("    </floatArray>\n");
	w.write("    <floatArray builtin=\"y3\">\n");
	w.write("      " + y3s + "\n");
	w.write("    </floatArray>\n");
	w.write("    <floatArray builtin=\"z3\">\n");
	w.write("      " + z3s + "\n");
	w.write("    </floatArray>\n");
	if (writecharge) {
	  w.write("    <floatArray builtin=\"formalCharge\">\n");
	  w.write("      " + formalCharges + "\n");
	  w.write("    </floatArray>\n");
	}
      w.write("  </atomArray>\n");
      w.write("</molecule>\n");
    }
}
