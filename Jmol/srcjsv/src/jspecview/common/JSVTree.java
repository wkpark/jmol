package jspecview.common;

import java.awt.Cursor;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jspecview.source.JDXSource;
import jspecview.util.JSVFileManager;
import jspecview.util.JSVLogger;
import jspecview.util.JSVParser;
import jspecview.util.JSVTextFormat;

public class JSVTree extends JTree {

  public final static int FILE_OPEN_OK = 0;
  public final static int FILE_OPEN_ALREADY = -1;
  //private final static int FILE_OPEN_URLERROR = -2;
  public final static int FILE_OPEN_ERROR = -3;
  public final static int FILE_OPEN_NO_DATA = -4;

  public static final int OVERLAY_DIALOG = -1;
	public static final int OVERLAY_OFFSET = 99;
	
	private static final long serialVersionUID = 1L;
	protected ScriptInterface si;
  private JSVTreeNode rootNode;
  private DefaultTreeModel spectraTreeModel;

	public JSVTreeNode getRootNode() {
		return rootNode;
	}

	public DefaultTreeModel getDefaultModel() {
		return spectraTreeModel;
	}

	public JSVTree(final ScriptInterface si) {
		super();
		this.si = si;
    rootNode = new JSVTreeNode("Spectra", null);
    spectraTreeModel = new DefaultTreeModel(rootNode);
    setModel(spectraTreeModel);
    getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        JSVTreeNode node = (JSVTreeNode) getLastSelectedPathComponent();
       if (node == null) {
          return;
        }
        if (node.isLeaf()) {
          si.setNode(node.panelNode, true);
        }
        si.setCurrentSource(node.panelNode.source);
      }
    });
    setRootVisible(false);

	}
	
	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp) {
		if (jsvp != null) {
			JSVTreeNode treeNode = (JSVTreeNode) JSVPanelNode.findNode(jsvp, si
					.getPanelNodes()).treeNode;
			scrollPathToVisible(new TreePath(treeNode.getPath()));
			setSelectionPath(new TreePath(treeNode.getPath()));
		}
	}
	
  @SuppressWarnings("unchecked")
	public static void closeSource(ScriptInterface si,
			JDXSource source) {
    // Remove nodes and dispose of frames
		List<JSVPanelNode> panelNodes = si.getPanelNodes();
  	JSVTree tree = (JSVTree) si.getSpectraTree();
		JSVTreeNode rootNode = tree.getRootNode();
		DefaultTreeModel spectraTreeModel = tree.getDefaultModel();

    String fileName = (source == null ? null : source.getFilePath());
    List<JSVTreeNode> toDelete = new ArrayList<JSVTreeNode>();
    Enumeration<JSVTreeNode> enume = rootNode.children();
    while (enume.hasMoreElements()) {
      JSVTreeNode node = enume.nextElement();
      if (fileName == null
          || node.panelNode.source.getFilePath().equals(fileName)) {
        for (Enumeration<JSVTreeNode> e = node.children(); e.hasMoreElements();) {
          JSVTreeNode childNode = e.nextElement();
          toDelete.add(childNode);
          panelNodes.remove(childNode.panelNode);
        }
        toDelete.add(node);
        if (fileName != null)
          break;
      }
    }
    for (int i = 0; i < toDelete.size(); i++) {
      spectraTreeModel.removeNodeFromParent(toDelete.get(i));
    }

    if (source == null) {
      JDXSource currentSource = si.getCurrentSource();
      //jsvpPopupMenu.dispose();
      if (currentSource != null)
        currentSource.dispose();
      //jsvpPopupMenu.dispose();
      if (si.getSelectedPanel() != null)
        si.getSelectedPanel().dispose();
    } else {
      //setFrameAndTreeNode(si, panelNodes.size() - 1);
    }
    
    if(si.getCurrentSource() == source) {
      si.setSelectedPanel(null);
      si.setCurrentSource(null);
    }

    int max = 0;
    for (int i = 0; i < panelNodes.size(); i++) {
      float f = JSVParser.parseFloat(panelNodes.get(i).id);
      if (f >= max + 1)
        max = (int) Math.floor(f);
    }
    si.setFileCount(max);
    System.gc();
    JSVLogger.checkMemory();
	}

	public static void setFrameAndTreeNode(ScriptInterface si, int i) {
    List<JSVPanelNode> panelNodes = si.getPanelNodes();
		if (panelNodes  == null || i < 0 || i >= panelNodes.size())
      return;
    si.setNode(panelNodes.get(i), false);
	}

	public static JSVPanelNode selectFrameNode(ScriptInterface si, JSVPanel jsvp) {
    // Find Node in SpectraTree and select it
    JSVPanelNode node = JSVPanelNode.findNode(jsvp, si.getPanelNodes());
    if (node == null)
      return null;

    JTree spectraTree = (JTree) si.getSpectraTree();
    spectraTree.setSelectionPath(new TreePath(((JSVTreeNode) node.treeNode)
        .getPath()));
    return si.setOverlayVisibility(node);
	}
	
	public static JSVPanel setSpectrum(ScriptInterface si, String value) {
		if (value.indexOf('.') >= 0) {
			JSVPanelNode node = JSVPanelNode.findNodeById(value, si.getPanelNodes());
			if (node == null)
				return null;
			si.setNode(node, false);
		} else {
			int n = JSVParser.parseInt(value);
			if (n <= 0) {
				si.checkOverlay();
				return null;
			}
      setFrameAndTreeNode(si, n - 1);
		}
		return si.getSelectedPanel();
	}

	public static JSVTreeNode createTree(ScriptInterface si,
			JDXSource source, JSVPanel[] panels) {

  	JSVTree tree = (JSVTree) si.getSpectraTree();
		JSVTreeNode rootNode = tree.getRootNode();
		DefaultTreeModel spectraTreeModel = tree.getDefaultModel();
    List<JSVPanelNode> panelNodes = si.getPanelNodes();

    String fileName = JSVFileManager.getName(source.getFilePath());
    JSVPanelNode panelNode = new JSVPanelNode(null, fileName, source, null);
    JSVTreeNode fileNode = new JSVTreeNode(fileName, panelNode);
    panelNode.setTreeNode(fileNode);
		spectraTreeModel.insertNodeInto(fileNode, rootNode, rootNode
        .getChildCount());
		tree.scrollPathToVisible(new TreePath(fileNode.getPath()));

		int fileCount = si.getFileCount() + 1;
    si.setFileCount(fileCount);
    for (int i = 0; i < panels.length; i++) {
      JSVPanel jsvp = panels[i];
      String id = fileCount + "." + (i + 1);
      panelNode = si.getNewPanelNode(id, fileName, source, jsvp);
      JSVTreeNode treeNode = new JSVTreeNode(panelNode.toString(), panelNode);
      panelNode.setTreeNode(treeNode);
			panelNodes.add(panelNode);
      spectraTreeModel.insertNodeInto(treeNode, fileNode, fileNode
          .getChildCount());
      tree.scrollPathToVisible(new TreePath(treeNode.getPath()));
    }
    selectFrameNode(si, panels[0]);
    return fileNode;
	}

	public static void close(ScriptInterface si, String value) {
		System.out.println("JSVTree close " + value);
		if (value == null || value.equalsIgnoreCase("all") || value.equals("*")) {
			si.closeSource(null);
			return;
		}
		List<JSVPanelNode> panelNodes = si.getPanelNodes();
		value = value.replace('\\', '/');
		if (value.endsWith("*")) {
			value = value.substring(0, value.length() - 1);
			for (int i = panelNodes.size(); --i >= 0;)
				if (i < panelNodes.size() && panelNodes.get(i).fileName.startsWith(value))
					si.closeSource(panelNodes.get(i).source);
		} else if (value.equals("selected")) {
			List<JDXSource> list = new ArrayList<JDXSource>();
			JDXSource lastSource = null;
			for (int i = panelNodes.size(); --i >= 0;) {
				JDXSource source = panelNodes.get(i).source;
				if (panelNodes.get(i).isSelected 
						&& (lastSource == null || lastSource != source))
					list.add(source);
				lastSource = source;
			}
			for (int i = list.size(); --i >= 0;)
				si.closeSource(list.get(i));
		} else {
			JDXSource source = (value.length() == 0 ? si.getCurrentSource()
					: JSVPanelNode.findSourceByNameOrId(value, panelNodes));
			if (source == null)
				return;
			si.closeSource(source);
		}
		if (si.getSelectedPanel() == null && panelNodes.size() > 0)
			si.setSelectedPanel(JSVPanelNode.getLastFileFirstNode(panelNodes));
	}

	public static void load(ScriptInterface si, String value) {
		List<String> tokens = ScriptToken.getTokens(value);
		String filename = tokens.get(0);
		int pt = 0;
		boolean isAppend = filename.equalsIgnoreCase("APPEND");
		boolean isCheck = filename.equalsIgnoreCase("CHECK");
		if (isAppend || isCheck)
			filename = tokens.get(++pt);
		boolean isSimulation = filename.equalsIgnoreCase("MOL");
		if (isSimulation)
			filename = JSVFileManager.SIMULATION_PROTOCOL + "MOL="
					+ JSVTextFormat.trimQuotes(tokens.get(++pt));
		if (!isCheck && !isAppend) {
			if (filename.equals("\"\"") && si.getCurrentSource() != null)
				filename = si.getCurrentSource().getFilePath();
			close(si, "all");
		}
		filename = JSVTextFormat.trimQuotes(filename);
		if (filename.startsWith("$")) {
			isSimulation = true;
			filename = JSVFileManager.SIMULATION_PROTOCOL + filename;
		}
		int firstSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : -1);
		int lastSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : firstSpec);
		si
				.openDataOrFile(null, null, null, filename, firstSpec, lastSpec,
						isAppend);
	}

	public static int openDataOrFile(ScriptInterface si, String data,
			String name, List<JDXSpectrum> specs, String url, int firstSpec,
			int lastSpec, boolean isAppend) {
		if ("NONE".equals(name)) {
			close(si, "View*");
			return FILE_OPEN_OK;
		}
		si.writeStatus("");
		String filePath = null;
		String newPath = null;
		String fileName = null;
		File file = null;
		URL base = null;
		boolean isView = false;
		if (data != null) {
		} else if (specs != null) {
			isView = true;
			newPath = fileName = filePath = "View" + si.incrementViewCount(1);
		} else if (url != null) {
			try {
				base = si.getDocumentBase();
				URL u = (base == null ? new URL(url) : new URL(base, url));
				filePath = u.toString();
				si.setRecentURL(filePath);
				fileName = JSVFileManager.getName(url);
			} catch (MalformedURLException e) {
				file = new File(url);
			}
		}
		if (file != null) {
			fileName = file.getName();
			newPath = filePath = file.getAbsolutePath();
			// recentJmolName = (url == null ? filePath.replace('\\', '/') : url);
			si.setRecentURL(null);
		}
		// TODO could check here for already-open view
		if (!isView)
			if (JSVPanelNode.isOpen(si.getPanelNodes(), filePath)
					|| JSVPanelNode.isOpen(si.getPanelNodes(), url)) {
				si.writeStatus(filePath + " is already open");
				return FILE_OPEN_ALREADY;
			}
		if (!isAppend && !isView)
			close(si, "all"); // with CHECK we may still need to do this
		si.setCursorObject(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			si.setCurrentSource(isView ? JDXSource.createView(specs) : si
					.createSource(data, filePath, base, firstSpec, lastSpec));
		} catch (Exception e) {
			JSVLogger.error(e.getMessage());
			si.writeStatus(e.getMessage());
			si.setCursorObject(Cursor.getDefaultCursor());
			return FILE_OPEN_ERROR;
		}
		si.setCursorObject(Cursor.getDefaultCursor());
		System.gc();
		JDXSource currentSource = si.getCurrentSource();
		if (newPath == null) {
			newPath = currentSource.getFilePath();
			if (newPath != null)
				fileName = newPath.substring(newPath.lastIndexOf("/") + 1);
		} else {
			currentSource.setFilePath(newPath);
		}
		si.setLoaded(fileName, newPath);

		JDXSpectrum spec = si.getCurrentSource().getJDXSpectrum(0);
		if (spec == null) {
			return FILE_OPEN_NO_DATA;
		}

		specs = currentSource.getSpectra();
		JDXSpectrum.process(specs, si.getIRMode());

		boolean autoOverlay = si.getAutoCombine()
				|| spec.isAutoOverlayFromJmolClick();

		boolean combine = isView || autoOverlay && currentSource.isCompoundSource;
		if (combine) {
			combineSpectra(si, (isView ? url : null));
		} else {
			splitSpectra(si);
		}
		if (!isView)
			si.updateRecentMenus(filePath);
		return FILE_OPEN_OK;
	}

  private static void combineSpectra(ScriptInterface si, String name) {
  	JDXSource source = si.getCurrentSource();
    List<JDXSpectrum> specs = source.getSpectra();
    JSVPanel jsvp = si.getNewJSVPanel(specs);
    jsvp.setTitle(source.getTitle());
    if (jsvp.getTitle().equals("")) {
      jsvp.getPanelData().setViewTitle(source.getFilePath());
    	jsvp.setTitle(name);
    }
    si.setPropertiesFromPreferences(jsvp, true);
    createTree(si, source, new JSVPanel[] { jsvp }).panelNode.isView = true;
    JSVPanelNode node = JSVPanelNode.findNode(si.getSelectedPanel(), si.getPanelNodes());
    node.setFrameTitle(name);
    node.isView = true;
    if (si.getAutoShowLegend()
        && si.getSelectedPanel().getPanelData().getNumberOfGraphSets() == 1)
      node.setLegend(si.getOverlayLegend(jsvp));
    si.setMenuEnables(node, false);
  }

  private static void splitSpectra(ScriptInterface si) {
  	JDXSource source = si.getCurrentSource();
    List<JDXSpectrum> specs = source.getSpectra();
    JSVPanel[] panels = new JSVPanel[specs.size()];
    JSVPanel jsvp = null;
    for (int i = 0; i < specs.size(); i++) {
      JDXSpectrum spec = specs.get(i);
      jsvp = si.getNewJSVPanel(spec);
      si.setPropertiesFromPreferences(jsvp, true);
      panels[i] = jsvp;
    }
    // arrange windows in ascending order
    createTree(si, source, panels);
    si.getNewJSVPanel((JDXSpectrum) null); // end of operation
    JSVPanelNode node = JSVPanelNode.findNode(si.getSelectedPanel(), si.getPanelNodes());
    si.setMenuEnables(node, true);
  }

}
