/*
 * @(#)JmolFileFilter.java    3/9/99
 *
 * Copyright (c) 1999 Thomas James Grey All Rights Reserved.
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

/** Class which implements a basic file filter, filtering by extension.**/

package org.openscience.jmol;

import java.io.File;

public class JmolFileFilter extends javax.swing.filechooser.FileFilter{
    
    private String endMask = ".";
    private String name = "";
    private boolean acceptNoDots = false;
    
    /**Creates a filter which will accept only files ending in
     * mask. If acceptNoDot is true then files without an extension
     * are also accepted.
     * @param mask String in which accepted files will end
     * @param typeName Name of this type and colon, eg "Xmol", use null none for none
     * @param acceptNoDot If true then files without dots (eg Unix files) will be accepted.
     **/
    public JmolFileFilter(String mask, String typeName, boolean acceptNoDot){
        endMask = mask.toLowerCase();
        if (typeName != null){
            name = typeName+" ("+"*"+endMask+")";
        }else{
            name = "*"+endMask;
        }
        acceptNoDots = acceptNoDot;
    }

    /**Overrides accept() in
     * javax.swing.filechooser.FileFilter. Always accepts
     * directories.
     **/
    public boolean accept(File f){
        String fname = f.getName();
        if (f.isDirectory()){
            return true;
        }else if (fname.indexOf(".")==-1){
            return acceptNoDots;
        }else{
            return(fname.toLowerCase().endsWith(endMask));
        }
    }
    
    /**Overrides getDescription() in
     *  javax.swing.filechooser.FileFilter. Returns the current
     *  mask.**/
    public String getDescription(){
        return(name);
    }
}
