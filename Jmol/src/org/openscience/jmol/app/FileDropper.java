/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetDragEvent;

import  java.awt.datatransfer.DataFlavor;
import  java.awt.datatransfer.Transferable;
import  java.awt.datatransfer.UnsupportedFlavorException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.File;
import java.io.IOException;

import java.util.List;

/** 
 * A simple Dropping class to allow files to be dragged onto a target.
 * It supports drag-and-drop of files from file browsers, and CML text
 * from editors, e.g. jEdit.
 *
 * <p>Note that multiple drops are not thread safe.
 *
 * @author Billy <simon.tyrrell@virgin.net>
 */
public class FileDropper implements DropTargetListener {
		private String fd_oldFileName;
		private PropertyChangeSupport fd_propSupport;

		static public final String FD_PROPERTY_FILENAME = "filename";
		static public final String FD_PROPERTY_INLINE   = "inline";

		public FileDropper () {
			fd_oldFileName = "";
			fd_propSupport = new PropertyChangeSupport (this);
		}

		public synchronized void addPropertyChangeListener (PropertyChangeListener l) {
			fd_propSupport.addPropertyChangeListener (l);
		}

		public synchronized void removePropertyChangeListener (PropertyChangeListener l) {
			fd_propSupport.removePropertyChangeListener (l);
		}

		public void dragOver(DropTargetDragEvent dtde) {
            System.out.println("DropOver detected...");
		}

		public void dragEnter(DropTargetDragEvent dtde) {
            System.out.println("DropEnter detected...");
			dtde.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
		}

		public void dragExit(DropTargetEvent dtde) {
            System.out.println("DropExit detected...");
		}

		public void dropActionChanged(DropTargetDragEvent dtde) {
		}

		public void drop (DropTargetDropEvent dtde) {
            System.out.println("Drop detected...");
			Transferable t = dtde.getTransferable ();
            if (t.isDataFlavorSupported (DataFlavor.javaFileListFlavor)) {
				dtde.acceptDrop (DnDConstants.ACTION_COPY_OR_MOVE);
				Object o = null;

				try {
					o = t.getTransferData (DataFlavor.javaFileListFlavor);
				} catch (UnsupportedFlavorException ufe) {
					ufe.printStackTrace ();
				} catch (IOException ioe) {
					ioe.printStackTrace ();
				}

				// if o is still null we had an exception
				if ((o != null) && (o instanceof List)) {
					List  fileList = (List) o;
					final int length = fileList.size ();

					for (int i = 0; i < length; ++ i) {
						File f = (File) fileList.get (i);
						PropertyChangeEvent pce = new PropertyChangeEvent (this, FD_PROPERTY_FILENAME, fd_oldFileName, f.getAbsolutePath ());
						fd_propSupport.firePropertyChange (pce);
					}

					dtde.getDropTargetContext ().dropComplete (true);
				}
			} else {
				System.out.println ("browsing supported flavours to find something useful...");
				DataFlavor [] df = t.getTransferDataFlavors ();

				if ((df != null) && (df.length > 0)) {
					for (int i = 0; i < df.length; ++ i) {

                        DataFlavor flavor = df[i];
                        System.out.println ("df " + i + " flavor " + flavor);
                        System.out.println ("  class: " + flavor.getRepresentationClass().getName());
                        System.out.println ("  mime : " + flavor.getMimeType());

                        if (flavor.getMimeType().equals("text/uri-list") &&
                            flavor.getRepresentationClass().getName().equals("java.lang.String")) {

                            /* This is one of the (many) flavors that KDE provides:

                               df 2 flavour java.awt.datatransfer.DataFlavor[mimetype=text/uri-list;representationclass=java.lang.String]
                                 java.lang.String
                                 String: file:/home/egonw/data/Projects/SourceForge/Jmol/Jmol-HEAD/samples/cml/methanol2.cml
                            */

                            dtde.acceptDrop (DnDConstants.ACTION_COPY_OR_MOVE);
                            Object o = null;

                            try {
                                o = t.getTransferData(flavor);
                            } catch (UnsupportedFlavorException ufe) {
                                ufe.printStackTrace ();
                            } catch (IOException ioe) {
                                ioe.printStackTrace ();
                            }

                            if ((o != null) && (o instanceof String)) {
                                System.out.println ("  String: " + o.toString());

                                PropertyChangeEvent pce = new PropertyChangeEvent (this, FD_PROPERTY_FILENAME, fd_oldFileName, o.toString());
                                fd_propSupport.firePropertyChange (pce);
                                dtde.getDropTargetContext ().dropComplete (true);
                            }
                            return;
                        } else if (flavor.getMimeType().equals("application/x-java-serialized-object; class=java.lang.String")) {

                            /* This is one of the flavors that jEdit provides:

                               df 2 flavour java.awt.datatransfer.DataFlavor[mimetype=text/uri-list;representationclass=java.lang.String]
                                 java.lang.String
                                 String: file:/home/egonw/data/Projects/SourceForge/Jmol/Jmol-HEAD/samples/cml/methanol2.cml
                            */

                            dtde.acceptDrop (DnDConstants.ACTION_COPY_OR_MOVE);
                            Object o = null;

                            try {
                                o = t.getTransferData(df[i]);
                            } catch (UnsupportedFlavorException ufe) {
                                ufe.printStackTrace ();
                            } catch (IOException ioe) {
                                ioe.printStackTrace ();
                            }

                            if ((o != null) && (o instanceof String)) {
                                System.out.println ("  String: " + o.toString());
                                PropertyChangeEvent pce = new PropertyChangeEvent (this, FD_PROPERTY_INLINE, fd_oldFileName, o.toString());
                                fd_propSupport.firePropertyChange (pce);
                                dtde.getDropTargetContext ().dropComplete (true);
                            }
                            return;
                        }
					}
				}

				dtde.rejectDrop();
			}
	}
}
