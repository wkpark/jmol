
/*
 * Copyright 2002 The Jmol Development Team
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
  public static final int ADD = 1;

  /** Identifies a change to existing measurements. */
  public static final int UPDATE = 0;

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
  public MeasurementListEvent(MeasurementList source, int type, int which,
      int op) {
    super(source);
    this.type = type;
    this.which = which;
    this.op = op;
  }

  /** Returns the type of measurement that changed. */
  public int getType() {
    return type;
  }
  ;

  /** Returns the position in the given measurement list that
   * underwent a change.
   */
  public int getWhich() {
    return which;
  }
  ;

  /** Returns the type of event - one of ADD, DELETE, or UPDATE */
  public int getOp() {
    return op;
  }
  ;
}
