/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.util;

import java.util.*;

public class GT {
    
    private static GT getTextWrapper = null;
    private static ResourceBundle translationResources = null;

    private GT() {
        System.out.println("Instantiating gettext wrapper...");
        try {
            translationResources = ResourceBundle.getBundle(
                "Messages"
            );
        } catch (MissingResourceException mre) {
            System.out.println("Translations do not seem to have been installed!");
            System.out.println(mre.getMessage());
            mre.printStackTrace();
            translationResources = null;
        } catch (Exception exception) {
            System.out.println("Some exception occured!");
            System.out.println(exception.getMessage());
            exception.printStackTrace();
            translationResources = null;
        }
    }
    
    public static String _(String string) {
        if (getTextWrapper == null) { 
            getTextWrapper = new GT();
        }
        return getTextWrapper.getString(string);
    }
    
    private String getString(String string) {
        if (translationResources != null) {
            String trans = translationResources.getString(string);
            System.out.println("trans: " + string  + " ->" + trans);
            return trans;
        }
        System.out.println("No trans, using default: " + string);
        return string;
    }
}

