/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.modelset;

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;
import org.jmol.atomdata.AtomData;
import org.jmol.shape.Closest;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Shape;
import org.jmol.shape.Dipoles;

import javax.vecmath.Point3f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

/*
 * An abstract class always created using new ModelLoader(...)
 * 
 * Merged with methods in Mmset and ModelManager 10/2007  Jmol 11.3.32
 * 
 * ModelLoader simply pulls out all private classes that are
 * necessary only for file loading (and structure recalculation).
 * 
 * What is left here are all the methods that are 
 * necessary AFTER a model is loaded, when it is being 
 * accessed by Viewer, primarily.
 * 
 * Please:
 * 
 * 1) designate any methods used only here as private
 * 2) designate any methods accessed only by ModelLoader as protected
 * 3) designate any methods accessed within modelset as nothing
 * 4) designate any methods accessed only by Viewer as public
 * 
 * Bob Hanson, 5/2007, 10/2007
 * 
 */
abstract public class ModelSet extends ModelCollection {

  ////////////////////////////////////////////////////////////////

  protected void releaseModelSet() {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; i++)
      shapes[i] = null;
    models = null;
    closest.atom = null;
    super.releaseModelSet();
  }

  //variables that will be reset when a new frame is instantiated

  private boolean selectionHaloEnabled = false;
  private boolean echoShapeActive = false;

  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    if (this.selectionHaloEnabled != selectionHaloEnabled) {
      this.selectionHaloEnabled = selectionHaloEnabled;
    }
  }

  public boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  public boolean getEchoStateActive() {
    return echoShapeActive;
  }

  public void setEchoStateActive(boolean TF) {
    echoShapeActive = TF;
  }

  protected String modelSetTypeName;

  public String getModelSetTypeName() {
    return modelSetTypeName;
  }

  protected Vector trajectories;

  public int getTrajectoryCount() {
    return (trajectories == null ? 1 : trajectories.size());
  }

  public void setTrajectory(int iTraj) {
    if (trajectories == null || iTraj < 0 || iTraj >= trajectories.size())
      return;
    Point3f[] trajectory = (Point3f[]) trajectories.get(iTraj);
      for (int i = atomCount; --i >= 0;)
        atoms[i].set(trajectory[i]);
  }  

  protected boolean isZeroBased;

  public void setZeroBased() {
    isZeroBased = isXYZ && viewer.getZeroBasedXyzRasmol();
  }

  protected final Shape[] shapes = new Shape[JmolConstants.SHAPE_MAX];
  
  private Shape allocateShape(int shapeID) {
    if (shapeID == JmolConstants.SHAPE_HSTICKS || shapeID == JmolConstants.SHAPE_SSSTICKS)
      return null;
    String className = JmolConstants.getShapeClassName(shapeID);
    try {
      Class shapeClass = Class.forName(className);
      Shape shape = (Shape) shapeClass.newInstance();
      shape.initializeShape(viewer, g3d, this, shapeID);
      return shape;
    } catch (Exception e) {
      Logger.error("Could not instantiate shape:" + className, e);
    }
    return null;
  }

  public Shape getShape(int i) {
    //FrameRenderer
    return shapes[i];
  }
  
  protected final Closest closest = new Closest();

  public int findNearestAtomIndex(int x, int y) {
    if (atomCount == 0)
      return -1;
    closest.atom = null;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    findNearestAtomIndex(x, y, closest);

    for (int i = 0; i < shapes.length && closest.atom == null; ++i)
      if (shapes[i] != null)
        shapes[i].findNearestAtomIndex(x, y, closest);
    int closestIndex = (closest.atom == null ? -1 : closest.atom.atomIndex);
    closest.atom = null;
    return closestIndex;
  }

  public void setShapeSize(int shapeID, int size, BitSet bsSelected) {
    if (size != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null)
      shapes[shapeID].setSize(size, bsSelected);
  }

  public Shape loadShape(int shapeID) {
    if (shapes[shapeID] == null)
      shapes[shapeID] = allocateShape(shapeID);
    return shapes[shapeID];
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value,
                        BitSet bsSelected) {
    if (shapes[shapeID] != null)
      shapes[shapeID].setProperty(propertyName.intern(), value, bsSelected);
  }

  public Object getShapeProperty(int shapeID, String propertyName, int index) {
    return (shapes[shapeID] == null ? null 
        : shapes[shapeID].getProperty(propertyName, index));
  }

  public int getShapeIdFromObjectName(String objectName) {
    for (int i = JmolConstants.SHAPE_MIN_MESH_COLLECTION; i < JmolConstants.SHAPE_MAX_MESH_COLLECTION; ++i) {
      MeshCollection shape = (MeshCollection) shapes[i];
      if (shape != null && shape.getIndexFromName(objectName) >= 0)
        return i;
    }
    Dipoles dipoles = (Dipoles) shapes[JmolConstants.SHAPE_DIPOLES];
    if (dipoles != null && dipoles.getIndexFromName(objectName) >= 0)
      return JmolConstants.SHAPE_DIPOLES;
    return -1;
  }

  public void getBondDipoles() {
    if (partialCharges == null)
      return;
    getBondDipoles((Dipoles) loadShape(JmolConstants.SHAPE_DIPOLES));
  } 

  public void setModelVisibility() {
    //named objects must be set individually
    //in the future, we might include here a BITSET of models rather than just a modelIndex

    // all these isTranslucent = f() || isTranslucent are that way because
    // in general f() does MORE than just check translucency. 
    // so isTranslucent = isTranslucent || f() would NOT work.

    BitSet bs = viewer.getVisibleFramesBitSet();
    //NOT balls (yet)
    for (int i = 1; i < JmolConstants.SHAPE_MAX; i++)
      if (shapes[i] != null)
        shapes[i].setVisibilityFlags(bs);
    //s(bs);
    //
    // BALLS sets the JmolConstants.ATOM_IN_MODEL flag.
    shapes[JmolConstants.SHAPE_BALLS].setVisibilityFlags(bs);

    //set clickability -- this enables measures and such
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null)
        shape.setModelClickability();
    }
  }

  /*
  private Hashtable userProperties;

  void putUserProperty(String name, Object property) {
    if (userProperties == null)
      userProperties = new Hashtable();
    if (property == null)
      userProperties.remove(name);
    else
      userProperties.put(name, property);
  }
*/  

  ////////////////// atomData filling ////////////

  public void fillAtomData(AtomData atomData, int mode) {
    if (mode == AtomData.MODE_GET_ATTACHED_HYDROGENS) {
      int[] nH = new int[1];
      atomData.hAtomRadius = JmolConstants.vanderwaalsMars[1] / 1000f;
      atomData.hAtoms = getAdditionalHydrogens(atomData.bsSelected, nH);
      atomData.hydrogenAtomCount = nH[0];
      return;
    }
    if(atomData.modelIndex < 0)
      atomData.firstAtomIndex = Math.max(0, BitSetUtil.firstSetBit(atomData.bsSelected));
    else
      atomData.firstAtomIndex = getFirstAtomIndexInModel(atomData.modelIndex);
    atomData.lastModelIndex = atomData.firstModelIndex = (atomCount == 0 ? 0 : atoms[atomData.firstAtomIndex].modelIndex);
    atomData.modelName = getModelNumberDotted(atomData.modelIndex);
    super.fillAtomData(atomData, mode);
  }


  ///////// atom and shape selecting /////////
  
  
  public boolean frankClicked(int x, int y) {
    Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
    return (frankShape != null && frankShape.wasClicked(x, y));
  }

  public boolean checkObjectHovered(int x, int y) {
    Shape shape = shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectHovered(x, y))
      return true;
    shape = shapes[JmolConstants.SHAPE_DRAW];
    if (shape == null || !viewer.getDrawHover())
      return false;
    return shape.checkObjectHovered(x, y);
  }

  public boolean checkObjectClicked(int x, int y, int modifiers) {
    Shape shape = shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectClicked(x, y, modifiers))
      return true;
    return ((shape = shapes[JmolConstants.SHAPE_DRAW]) != null
        && shape.checkObjectClicked(x, y, modifiers));
  }
 
  public void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null
          && shape.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers))
        break;
    }
  }

  public Hashtable getShapeInfo() {
    Hashtable info = new Hashtable();
    StringBuffer commands = new StringBuffer();
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null) {
        String shapeType = JmolConstants.shapeClassBases[i];
        Vector shapeDetail = shape.getShapeDetail();
        if (shapeDetail != null) {
          Hashtable shapeinfo = new Hashtable();
          shapeinfo.put("obj", shapeDetail);
          info.put(shapeType, shapeinfo);
        }
      }
    }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }


  ///// super-overloaded methods ///////
  
  
  public void calculatePolymers(int modelIndex) {
    super.calculatePolymers(modelIndex);
    for (int i = 0; i < shapes.length; ++i)
      if (shapes[i] != null && shapes[i].isBioShape)
        shapes[i] = null;
    viewer.getFrameRenderer().clear();
  }

  private final static boolean useRasMolHbondsCalculation = true;

  public int autoHbond(BitSet bsA, BitSet bsB, BitSet bsBonds) {
    bsPseudoHBonds = new BitSet();
    if (useRasMolHbondsCalculation && bondCount > 0) {
      calcHydrogenBonds(bsA, bsB);
      bsBonds = bsPseudoHBonds;
      return BitSetUtil.cardinalityOf(bsBonds);
    }
    return super.autoHbond(bsA, bsB, bsBonds);
  }
  
  protected void assignAromaticBonds(boolean isUserCalculation) {
    super.assignAromaticBonds(isUserCalculation, null);
    // send a message to STICKS indicating that these bonds
    // should be part of the state of the model. They will 
    // appear in the state as bondOrder commands.
    
    if (isUserCalculation)
      setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MIN_VALUE, bsAromatic);

  }

  public int[] makeConnections(float minDistance, float maxDistance, short order,
                             int connectOperation, BitSet bsA, BitSet bsB,
                             BitSet bsBonds, boolean isBonds) {
    if (connectOperation != JmolConstants.CONNECT_IDENTIFY_ONLY) {
      String stateScript = "connect ";
      if (minDistance != JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE)
        stateScript += minDistance + " ";
      if (maxDistance != JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE)
        stateScript += maxDistance + " ";
      if (isBonds)
        stateScript += Escape.escape(bsA, false) + " ";
      else
        stateScript += Escape.escape(bsA) + " " + Escape.escape(bsB) + " ";
      if (connectOperation != JmolConstants.CONNECT_DELETE_BONDS)
        stateScript += JmolConstants.getBondOrderNameFromOrder(order) + " ";
      stateScript += JmolConstants.connectOperationName(connectOperation);
      stateScript += ";";
      stateScripts.addElement(stateScript);
    }
    return super.makeConnections(minDistance, maxDistance, order,
        connectOperation, bsA, bsB, bsBonds, isBonds);
  }
  
  public void rebond() {
    // from eval "connect" or from app preferences panel
    stateScripts.addElement("connect;");
    deleteAllBonds();
    autoBond(null, null, null);
  }

  public void deleteAllBonds() {
    //StateManager
    for (int i = 0; i < stateScripts.size();) 
      if (((String) stateScripts.get(i)).indexOf("connect") == 0)
        stateScripts.removeElementAt(i);
      else
        i++;
    super.deleteAllBonds();
  }

  /* ******************************************************
   * 
   * methods for definining the state 
   * 
   ********************************************************/
 
  public String getState(StringBuffer sfunc, boolean isAll) {
    StringBuffer commands = new StringBuffer();
    if (isAll && sfunc != null) {
      sfunc.append("  _setModelState;\n");
      commands.append("function _setModelState();\n");
    }
    String cmd;

    // properties

    if (isAll) {
      for (byte i = 0; i < TAINT_MAX; i++)
        if(getTaintedAtoms(i) != null) 
          getTaintedState(commands, i);
    }

    // connections

    if (isAll) {
      Vector fs = stateScripts;
      int len = fs.size();
      if (len > 0) {
        commands.append("\n");
        for (int i = 0; i < len; i++)
          commands.append("  ").append(fs.get(i)).append("\n");
      }

      commands.append("\n");
      // shape construction

    }

    setModelVisibility();

    commands.append(getProteinStructureState(null, false));
    
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null && (isAll || JmolConstants.isShapeSecondary(i)) 
          && (cmd = shape.getShapeState()) != null && cmd.length() > 1)
        commands.append(cmd);
    }
    
    for (int i = 0; i < modelCount; i++) {
      String t = models[i].frameTitle; 
      if (t != null && t.length() > 0)
        commands.append("  frame " + getModelNumberDotted(i)
            + "; frame title " + Escape.escape(t) + "\n;");
    }
    
    if (sfunc != null)
      commands.append("\nend function;\n\n");
    return commands.toString();
  }

}

