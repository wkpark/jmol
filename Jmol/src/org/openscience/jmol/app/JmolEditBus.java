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
*  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
*  02111-1307  USA.
*/
package org.openscience.jmol.app;

import org.openscience.cdk.applications.plugin.CDKEditBus;
import org.openscience.cdk.*;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.geometry.CrystalGeometryTools;
import org.openscience.cdk.config.AtomTypeFactory;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import org.jmol.api.JmolViewer;

import java.io.IOException;
import java.io.Reader;

import javax.vecmath.*;

public class JmolEditBus implements CDKEditBus {
    
    private final static String APIVersion = "1.8";
    
    private JmolViewer viewer = null;
    
    public JmolEditBus(JmolViewer viewer) {
        this.viewer = viewer;
    }
    
    public String getAPIVersion() {
        return APIVersion;
    }
    
    public void runScript(String mimeType, String script) {
        if ("chemical/x-rasmol".equals(mimeType)) {
            viewer.evalString(script);
        } else {
            // ignore
            System.out.println("Ignoring script with unknown MIME type: " + mimeType);
        }
    }
    
    public void showChemFile(Reader file) {
        viewer.openReader("", "", file);
    }
    
    public void showChemFile(ChemFile file) {
        AtomContainer atomContainer = ChemFileManipulator.getAllInOneContainer(file);
        Atom[] atoms = atomContainer.getAtoms();
        // check if there is any content
        if (atoms.length == 0) {
            System.err.println("ChemFile does not contain atoms.");
            return;
        }
        // check wether there are 3D coordinates
        if (!GeometryTools.has3DCoordinates(atomContainer) &&
        !CrystalGeometryTools.hasCrystalCoordinates(atomContainer)) {
            System.err.println("Cannot display chemistry without 3D coordinates");
            return;
        }
        try {
            AtomTypeFactory factory = AtomTypeFactory.getInstance("org/openscience/cdk/config/data/jmol_atomtypes.txt");
            for (int i=0; i<atoms.length; i++) {
                try {
                    factory.configure(atoms[i]);
                } catch (CDKException exception) {
                    System.out.println("Could not configure atom: " + atoms[i]);
                }
            }
        } catch (ClassNotFoundException exception) {
            // could not configure atoms... what to do?
            System.err.println(exception.getMessage());
            exception.printStackTrace();
        } catch (IOException exception) {
            // could not configure atoms... what to do?
            System.err.println(exception.getMessage());
            exception.printStackTrace();
        }
        viewer.openClientFile("", "", file);
    }
    
    public void showChemModel(ChemModel model) {
        ChemFile file = new ChemFile();
        ChemSequence sequence = new ChemSequence();
        sequence.addChemModel(model);
        file.addChemSequence(sequence);
        showChemFile(file);
    }
    
    public ChemModel getChemModel() {
        if (viewer.getModelCount() != 1) {
            // cannot deal with no models, or more than one model
            return null;
        } else {
            ChemModel model = new ChemModel();
            SetOfMolecules moleculeSet = new SetOfMolecules();
            Molecule molecule = new Molecule();
            // copy the atoms
            Point3f[] atomPoints = new Point3f[viewer.getAtomCount()];
            for (int i=0; i<viewer.getAtomCount(); i++) {
                Atom atom = new Atom(viewer.getAtomName(i));
                atomPoints[i] = viewer.getAtomPoint3f(i);
                atom.setPoint3d(new Point3d(atomPoints[i]));
                molecule.addAtom(atom);
            }
            // copy the bonds
            for (int i=0; i<viewer.getBondCount(); i++) {
                // Ok, at the time or writing the JmolViewer no longer has
                // something like getBondAtomNumber1() :(
                int atomNumber1 = -1;
                int atomNumber2 = -1;
                Point3f atomCoord = viewer.getBondPoint3f1(i);
                for (int j=0; j<atomPoints.length; j++) {
                    if (atomCoord.distance(atomPoints[j]) < 0.01) {
                        atomNumber1 = j;
                        j=atomPoints.length;
                    }
                }
                atomCoord = viewer.getBondPoint3f2(i);
                for (int j=0; j<atomPoints.length; j++) {
                    if (atomCoord.distance(atomPoints[j]) < 0.01) {
                        atomNumber2 = j;
                        j=atomPoints.length;
                    }
                }
                // just assume this is working ...
                molecule.addBond(atomNumber1, atomNumber2, viewer.getBondOrder(i));
            }
            moleculeSet.addMolecule(molecule);
            model.setSetOfMolecules(moleculeSet);
            return model;
        }
    }
    
    public ChemFile getChemFile() {
        ChemFile file = new ChemFile();
        ChemSequence sequence = new ChemSequence();
        sequence.addChemModel(getChemModel()); // better to get all models
        file.addChemSequence(sequence);
        return file;
    }
}
