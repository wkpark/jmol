package jspecview.common;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;

import javax.swing.JOptionPane;

import jspecview.util.JSVEscape;
import jspecview.util.JSVLogger;

public class JSVDropTargetListener implements DropTargetListener {

  private ScriptInterface si;
  //private boolean allowAppend = true;

  public JSVDropTargetListener(ScriptInterface si) {
    this.si = si;
  }

  //
  //   Abstract methods that are used to perform drag and drop operations
  //

  public void dragEnter(DropTargetDragEvent dtde) {
    // Called when the user is dragging and enters this drop target.
    // accept all drags
    dtde.acceptDrag(dtde.getSourceActions());
  }

  public void dragOver(DropTargetDragEvent dtde) {
  }

  public void dragExit(DropTargetEvent dtde) {
  }

  public void dropActionChanged(DropTargetDragEvent dtde) {
    // Called when the user changes the drag action between copy or move
  }

  static int lastSelection = 0;
  
	// Called when the user finishes or cancels the drag operation.
	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent dtde) {
		JSVLogger.debug("Drop detected...");
		Transferable t = dtde.getTransferable();
		boolean isAccepted = false;
		boolean doAppend = false;
		if (si.getCurrentSource() != null) {
			Object[] options = { "Replace", "Append", "Cancel" };
			int ret = JOptionPane.showOptionDialog(null, "Select an option",
					"JSpecView File Drop", JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[lastSelection]);

			if (ret < 0 || ret == 2)
				return;
			lastSelection = ret;
			doAppend = (ret == 1);
		}
		String prefix = (doAppend ? "" : "close ALL;");
		String postfix = (doAppend ? "" : "overlay ALL");
		String cmd = "LOAD APPEND ";
		String fileToLoad = null;
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			Object o = null;
			try {
				dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				o = t.getTransferData(DataFlavor.javaFileListFlavor);
				isAccepted = true;
			} catch (Exception e) {
				JSVLogger.error("transfer failed");
			}
			// if o is still null we had an exception
			if (o instanceof List) {
				List<File> list = (List<File>) o;
				dtde.getDropTargetContext().dropComplete(true);
				dtde = null;
				StringBuffer sb = new StringBuffer(prefix);
				for (int i = 0; i < list.size(); i++)
					sb.append(cmd + JSVEscape.escape(list.get(i).getAbsolutePath()) + ";");
				sb.append(postfix);
				cmd = sb.toString();
				JSVLogger.info("Drop command = " + cmd);
				si.runScript(cmd);
				/*
				 * 
				 * 
				 * final int length = fileList.size(); if (length == 1) { String
				 * fileName = fileList.get(0).getAbsolutePath().trim(); if
				 * (fileName.endsWith(".bmp")) break; // try another flavor -- Mozilla
				 * bug dtde.getDropTargetContext().dropComplete(true);
				 * loadFile(fileName); return; }
				 */
				return;
			}
		}

		JSVLogger.debug("browsing supported flavours to find something useful...");
		DataFlavor[] df = t.getTransferDataFlavors();

		if (df == null || df.length == 0)
			return;
		for (int i = 0; i < df.length; ++i) {
			DataFlavor flavor = df[i];
			Object o = null;
			if (true) {
				JSVLogger.info("df " + i + " flavor " + flavor);
				JSVLogger.info("  class: " + flavor.getRepresentationClass().getName());
				JSVLogger.info("  mime : " + flavor.getMimeType());
			}

			if (flavor.getMimeType().startsWith("text/uri-list")
					&& flavor.getRepresentationClass().getName().equals(
							"java.lang.String")) {

				/*
				 * This is one of the (many) flavors that KDE provides: df 2 flavour
				 * java.awt.datatransfer.DataFlavor[mimetype=text/uri-list;
				 * representationclass=java.lang.String] java.lang.String String: file
				 * :/home/egonw/data/Projects/SourceForge/Jmol/Jmol-HEAD/samples/
				 * cml/methanol2.cml
				 * 
				 * A later KDE version gave me the following. Note the mime!! hence the
				 * startsWith above
				 * 
				 * df 3 flavor java.awt.datatransfer.DataFlavor[mimetype=text/uri-list
				 * ;representationclass=java.lang.String] class: java.lang.String mime :
				 * text/uri-list; class=java.lang.String; charset=Unicode
				 */

				try {
					if (!isAccepted)
						dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					isAccepted = true;
					o = t.getTransferData(flavor);
				} catch (Exception e) {
					JSVLogger.error(null, e);
				}

				if (o instanceof String) {
					dtde.getDropTargetContext().dropComplete(true);
					if (JSVLogger.debugging)
						JSVLogger.debug("  String: " + o.toString());
					fileToLoad = o.toString();
					break;
				}
			} else if (flavor.getMimeType().equals(
					"application/x-java-serialized-object; class=java.lang.String")) {

				/*
				 * This is one of the flavors that jEdit provides:
				 * 
				 * df 0 flavor java.awt.datatransfer.DataFlavor[mimetype=application/
				 * x-java-serialized-object;representationclass=java.lang.String] class:
				 * java.lang.String mime : application/x-java-serialized-object;
				 * class=java.lang.String String: <molecule title="benzene.mol"
				 * xmlns="http://www.xml-cml.org/schema/cml2/core"
				 * 
				 * But KDE also provides:
				 * 
				 * df 24 flavor java.awt.datatransfer.DataFlavor[mimetype=application
				 * /x-java-serialized-object;representationclass=java.lang.String]
				 * class: java.lang.String mime : application/x-java-serialized-object;
				 * class=java.lang.String String: file:/home/egonw/Desktop/1PN8.pdb
				 */

				try {
					if (!isAccepted)
						dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					isAccepted = true;
					o = t.getTransferData(df[i]);
				} catch (Exception e) {
					JSVLogger.error(null, e);
				}
				if (o instanceof String) {
					String content = (String) o;
					dtde.getDropTargetContext().dropComplete(true);
					if (JSVLogger.debugging)
						JSVLogger.debug("  String: " + content);
					if (content.startsWith("file:/")) {
						fileToLoad = content;
						break;
					}
				}
			}
		}
		if (!isAccepted)
			dtde.rejectDrop();
		if (fileToLoad != null) {
			cmd = prefix + cmd + JSVEscape.escape(fileToLoad) + "\";" + postfix;
			JSVLogger.info("Drop command = " + cmd);
			si.runScript(cmd);
		}
	}
  
}
