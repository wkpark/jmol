/*
 * @(#)JmolResourceHandler.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
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

import java.io.PrintStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.text.*;
import java.util.*;
import java.net.*;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.ImageIcon;

class JmolResourceHandler {
    private static ResourceBundle rb;
    private String baseKey;

    public static void initialize(String resources) {
        JmolResourceHandler.rb = ResourceBundle.getBundle(resources);
    }
    
    JmolResourceHandler(String string) {
        baseKey = string;
    }
    
    synchronized ImageIcon getIcon(String key) {
        String iname = null;  // Image name
        try {
            iname = "org/openscience/jmol/images/" + rb.getString(getQualifiedKey(key));
        } catch (MissingResourceException e) {}
        if (iname != null) {            
            return new ImageIcon(ClassLoader.getSystemResource(iname));
        }
        return null;
    }

    synchronized String getString(String string) {
        String ret = null;
        try {
            ret = rb.getString(getQualifiedKey(string));
        } catch (MissingResourceException e) {}
        if (ret != null) return ret;
        return null;
    }
    
    synchronized Object getObject(String string) {
        Object o = null;
        try {
            o = rb.getObject(getQualifiedKey(string));
        } catch (MissingResourceException e) {}
        if (o != null) return o;
        return null;
    }
    
    synchronized String getQualifiedKey(String string) {
        return baseKey + "." + string;
    }
    
}
