/*
 * @(#)PovraySaver.java    1.0 99/06/09
 *
 * Copyright (c) 1999 THOMAS JAMES GREY All Rights Reserved.
 *
 * Thomas James Grey grants you ("Licensee") a non-exclusive, royalty
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
 * EXCLUDED.  THOMAS JAMES GREY AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL THOMAS JAMES GREY OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF THOMAS JAMES GREY HAS BEEN
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

import java.util.Vector;
import java.io.*;
/** Generates files for viewing in the freeware povray reaytracer (http://www.povray.org)<p>
    The types of atoms and bonds is controlled by PovrayStyleWriter**/
public class PovraySaver extends FileSaver{
    
    private String viewMat = null;
    private String background = null;
    private PovrayStyleWriter myStyle;
    private int framenumber =0;
    protected ChemFile cf;
    
    /**
     * Constructor.
     */
    public PovraySaver( ChemFile cf, OutputStream out) throws IOException {
        super(cf,out);
        //Hack hack- keep a pointer of our own as FileSaver's is private
        this.cf =cf;
        myStyle = new PovrayStyleWriter();
    }
    
    /**Set the matrix transfrom assosiated with the current viewpoint**/
    public void setTransformMatrix(Matrix3D mat){
        viewMat = "<"+mat.xx+","+mat.yx+","+mat.zx+",\n"
            +mat.xy+","+mat.yy+","+mat.zy+",\n"
            +mat.xz+","+mat.yz+","+mat.zz+",\n"
            +mat.xo+","+mat.yo+","+mat.zo+">\n";
        /*        viewMat ="<"+mat.xx+","+mat.xy+","+mat.xz+","+mat.xo+",\n"
                  +mat.yx+","+mat.yy+","+mat.yz+","+mat.yo+",\n"
                  +mat.zx+","+mat.zy+","+mat.zz+","+mat.zo+",\n";*/
    }
    
    /**Sets the number of the actual frame that is written out, by default this is frame 1.**/
    public void setFramenumber(int framenumber){
        this.framenumber = framenumber;
    }
    
    /**Sets the background colour of the renderered scene to the colour given.**/
    public void setBackgroundColor(java.awt.Color bgColor){
        background = povrayColor(bgColor);
    }
    
    /**Sets the style controller for this file to that given. Style controllers must subclass PovrayStyleWriter and control the appearence of the atoms and bonds.**/
    public void setStyleController(PovrayStyleWriter style){
        myStyle = style;
    }
    
    public void writeFileStart(ChemFile cf, BufferedWriter w) throws IOException{
        // POvray files don't work like this! Each frame is a separate file so this method is redundant.
    }
    
    public void writeFileEnd(ChemFile cf, BufferedWriter w) throws IOException{
        //Again this is meaningless as each frame is a file.
    }
    
    /**
     * writes a single frame in povray format to the Writer.
     * @param cf the ChemFrame to write
     * @param w the Writer to write it to
     */
    public void writeFrame(ChemFrame cf, BufferedWriter w) throws IOException {

        w.write("//Povray file output from Jmol by THOMAS JAMES GREY\n");
        w.write("//#include \"colors.inc\"\n");
        w.write("//#include \"textures.inc\"\n");
        w.write("#declare White   = rgb 1;\n");
        w.write("// Shiny creates a small, tight highlight on the object's surface\n");
        w.write("#declare Shiny = finish {specular 1 roughness 0.001}\n");
        w.write("camera{\n");
        w.write("  location <0,0,0>\n");
        w.write("  look_at <0,0,10>\n");
        w.write("}\n");
        if(background !=null){
            w.write("background { color "+background+" }\n");
        }
        w.write(" light_source { <0,0,-20> color rgb<0.2,0.2,0.2>}\n");
        w.write(" light_source { <0,16,-20> color White}\n");
        myStyle.writeAtomsAndBondsDeclare(w,cf);
        
        String st = "";
        boolean writevect = false;
        
        try {
            
            String s2 = cf.getInfo();
            if (s2 != null) {
                w.write("//COMMENT:"+ s2 + "\n");
            }
            
            w.write("#declare molecule=union{\n");
            
            Vector fp = cf.getFrameProps();
            
            // Create some dummy properties:
            double[] vect = new double[3];
            vect[0] = 0.0;
            vect[1] = 0.0;
            vect[2] = 0.0;
            VProperty vp = new VProperty(vect);
            
            // test if we have vectors in this frame:
            for (int i = 0; i < fp.size(); i++) {
                String prop = (String) fp.elementAt(i);
                if (prop.equals(vp.getDescriptor())) writevect = true;
            }
            
            // Loop through the atoms and write them out:
            
            for (int i = 0; i < cf.getNvert(); i++) {
                
                BaseAtomType a = cf.getAtomType(i).getBaseAtomType();
                st = a.getName();

                myStyle.writeAtom(w,i,cf);
                
                Vector props = cf.getVertProps(i);
                /*
                  if (writevect) {
                  for (int j = 0; j < props.size(); j++) {
                  PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
                  String desc = p.getDescriptor();
                  if (desc.equals(vp.getDescriptor())) {
                  VProperty vt = (VProperty) p;
                  double[] vtmp;
                  vtmp = vt.getVector();
                  st = st + tab + 
                  new Double(vtmp[0]).toString() + tab +
                  new Double(vtmp[1]).toString() + tab + 
                  new Double(vtmp[2]).toString();
                  }
                  }
                  }
                st = st + "\n";
                w.write(st, 0, st.length());
                */                
            }
            w.write("}\n");
            w.write("\n");
            w.write("object{\n");
            w.write("molecule\n");
            if (viewMat !=null){
                w.write("matrix"+viewMat+"\n");
                w.write("matrix<1,0,0,\n");
                w.write("       0,1,0,\n");
                w.write("       0,0,-1,\n");
                w.write("       1.0,1.0,1.0>\n");
                
            }
            w.write("}\n");
        } catch (IOException e) {
            throw e;
        }
        
    }
    
    /**The default implemention of this method writes all frames to one file- we override to open a new file for each frame.**/
    public synchronized void writeFile() throws IOException {
        try {
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out), 
                                                  1024);                         
            //System.out.println("MARK "+framenumber);
            ChemFrame cfr = cf.getFrame(framenumber);
            writeFrame(cfr, w);
            w.flush();
            w.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            System.out.println("Got IOException "+e+" trying to write frame.");
        }
    }

    /** Takes a java colour and returns a String representing the colour in povray eg 'rgb<1.0,0.0,0.0>'**/
    protected String povrayColor(java.awt.Color col){
        float tff = (float)255.0;
        return "rgb<"+((float)col.getRed()/tff)+","+((float)col.getGreen()/tff)+","+((float)col.getBlue()/tff)+">";
    }
}
