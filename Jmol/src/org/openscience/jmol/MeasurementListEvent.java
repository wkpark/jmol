/*
 * @(#)MeasurementListEvent.java    1.0 99/10/26
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

import java.util.EventObject;

/**
 * MeasurementListEvent is used to notify listeners that a measurement
 * list has changed.  
 * 
 * Depending on the parameters used in the constructors, the
 * MeasurementListEvent can be used to specify the following types of
 * changes: <p>
 *
 * <pre>
 * MeasurementListEvent(source);            //  All measurements changed 
 * MeasurementListEvent(source, DISTANCE, 1, DELETE); // Distance 1 was deleted
 * MeasurementListEvent(source, ANGLE, 3, ADD);        // Angle 3 was added
 * MeasurementListEvent(source, DIHEDRAL, ALL, UPDATE);// All Dihedrals updated
 * </pre>
 *
 * @version 1.0 10/26/99
 * @author J. Daniel Gezelter
 * @see org.openscience.jmol.MeasurementList
 */
public class MeasurementListEvent extends java.util.EventObject {
    /** Identifies the addtion of new measurements. */
    public static final int ADD =  1;
    /** Identifies a change to existing measurements. */
    public static final int UPDATE =  0;
    /** Identifies the removal of measurements. */
    public static final int DELETE = -1;
    
    /** Identifies the distance measurements. */
    public static final int DISTANCE = 2;
    /** Identifies the angle measurements. */
    public static final int ANGLE = 3;
    /** Identifies the dihedral measurements. */
    public static final int DIHEDRAL = 4;
    
    /** Specifies all measurements of a class; */
    public static final int ALL = -1;
    
    protected int type;
    protected int which;
    protected int op;

    /** 
     *  All measurement requests have changed, listeners should
     *  discard any state that was based on the measurement list and
     *  requery the MeasurementList to get the vectors.
     */
    public MeasurementListEvent(MeasurementList source) {
        this(source, ALL, ALL, UPDATE);
    }
    
    /**
     *  This type of measurement has been updated.
     */
    public MeasurementListEvent(MeasurementList source, int type) {
        this(source, type, ALL, UPDATE);
    }

    /**
     *  The measurement of type <i>type</i> in position <i>which</i>
     *  has been updated.  
     */
    public MeasurementListEvent(MeasurementList source, int type, int which) {
        this(source, type, which, UPDATE);
    }
    
    /**
     * The measurement of type <i>type</i> in poisition <i>which</i>
     * has had operation <i>op</i> done to it.  When <I>which</I> is
     * ALL, all measurements of the specified type are considered
     * changed.
     *
     * <p> The <I>op</I> should be one of: ADD, UPDATE and DELETE.  */
    public MeasurementListEvent(MeasurementList source, int type, int which, int op) {
        super(source);
        this.type = type;
        this.which = which;
        this.op = op;
    }

    /** Returns the type of measurement that changed. */
    public int getType() { return type; };

    /** Returns the position in the given measurement list that
     * underwent a change. 
     */
    public int getWhich() { return which; };

    /** Returns the type of event - one of ADD, DELETE, or UPDATE */
    public int getOp() { return op; };
}
