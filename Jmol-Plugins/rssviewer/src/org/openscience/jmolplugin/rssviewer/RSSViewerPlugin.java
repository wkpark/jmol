/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.plugin;

import org.openscience.jmol.ChemFile;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.Convertor;
import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.app.SortedTableModel;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.ChemicalRSSReader;
import org.openscience.cdk.tools.ChemModelManipulator;
import org.openscience.cdk.tools.MFAnalyser;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.ListSelectionModel;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;


/**
 * Plugin that can read RSS sources and extract molecular content
 * in the CML2 format from it.
 *
 * @author Egon Willighagen
 */
public class RSSViewerPlugin implements JmolPluginInterface {

    private DisplayControl control = null;
    private Vector channels = null;
    private JTree rssTree = null;
    private RSSContentModel channelContent = null;
    private SortedTableModel sortedContent = null;
    
    public void setDisplayControl(DisplayControl control) {
        this.control = control;
    }
    
    public void start() {
        channels = new Vector();
        Properties props = readProperties();
        int rssChannels = 0;
        if (props != null) {
            try {
                rssChannels = Integer.parseInt(props.getProperty("ChannelCount", "0"));
            } catch (NumberFormatException exception) {
                System.out.println("Error while parsing RSSViewer property field: ChannelCount. Value is not an integer");
            }
        }
        if (rssChannels == 0) {
            System.out.println("No channels found");
        }
        for (int i=0; i<rssChannels; i++) {
            String channel = props.getProperty("Channel" + new Integer(i).toString(), "");
            if (channel.length() == 0) {
                System.out.println("Could not find URL for " + i + "th channel");
            } else {
                try {
                    URL rssURL = new URL(channel);
                    channels.addElement(rssURL);
                    System.out.println("Added RSS channel: " + channel);
                } catch (Exception exception) {
                    System.out.println("URL for " + i + "th channel is not valid: " + channel);
                }
            }
        }
    }
    
    public void stop() {
        channels.clear();
    }
    
    public String getName() {
        return "RSS Viewer";
    }
    
    public String getAPIVersion() {
        return "1.0";
    }
    
    public JPanel getPluginPanel() {
        JPanel RSSViewerPanel = new JPanel();
        
        // The Channel list (possibly grouped, therefor a tree)
        rssTree = new JTree(createChannelTree());
        rssTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)rssTree.getLastSelectedPathComponent();
                
                if (node == null) return;
                
                Object nodeInfo = node.getUserObject();
                if (nodeInfo instanceof RSSChannelNode) {
                    RSSChannelNode rssNode = (RSSChannelNode)nodeInfo;
                    System.out.println("Should load this RSS now: " + nodeInfo.toString());
                    ChemSequence channelItems = null;
                    try {
                        InputStream is = rssNode.getURL().openStream();
                        InputStreamReader isReader = new InputStreamReader(is);
                        ChemicalRSSReader reader = new ChemicalRSSReader(isReader);
                        channelItems = (ChemSequence)reader.read(new ChemSequence());
                    } catch (CDKException exception) {
                        System.out.println("Error while reading RSS file");
                        exception.printStackTrace();
                    } catch (IOException exception) {
                        System.out.println("IOException while reading RSS file");
                        exception.printStackTrace();
                    }
                    if (channelItems != null) {
                        System.out.println("YES!");
                        System.out.println("Clearing current table...");
                        channelContent.cleanTable();
                        ChemModel[] models = channelItems.getChemModels();
                        System.out.println("#items = " + models.length);
                        for (int i=0; i<models.length; i++) {
                            ChemModel model = models[i];
                            channelContent.insertBlankRow(i);
                            channelContent.setValueAt(models[i], i);
                        }
                    }
                }
            }
            
            private void transferRSSProperty(ChemModel model, String propertyName, int row, int column) {
                Object property = model.getProperty(propertyName);
                if (property != null) {
                    channelContent.setValueAt(property, row, column);
                    System.out.println("Transfered data: " + property.toString());
                }
            }
        });
        rssTree.validate();
        JScrollPane treePanel = new JScrollPane(rssTree);
        treePanel.validate();
        
        // A table showing the entries in the channel
        channelContent = new RSSContentModel();
        sortedContent = new SortedTableModel(channelContent);
        JTable channelTable = new JTable(sortedContent);
        sortedContent.addMouseListenerToHeaderInTable(channelTable);
        channelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel rowSM = channelTable.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                // Ignore extra messages
                if (e.getValueIsAdjusting()) return;
                
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if (lsm.isSelectionEmpty()) {
                    // no rows are selected
                } else {
                    int selectedRow = lsm.getMinSelectionIndex();
                    ChemModel model = channelContent.getValueAt(sortedContent.getSortedIndex(selectedRow));
                    AtomContainer container = ChemModelManipulator.getAllInOneContainer(model);
                    ChemFrame frame = Convertor.convert(control, container);
                    ChemFile file = new ChemFile(control, true);
                    file.addFrame(frame);
                    control.setChemFile(file);
                }
            }
        });
        
        channelTable.validate();
        JScrollPane contentPane = new JScrollPane(channelTable);
        contentPane.validate();
        
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treePanel, contentPane);
        RSSViewerPanel.add(splitter);
        RSSViewerPanel.validate();
        
        return RSSViewerPanel;
    };
    
    public JPanel getPluginConfigPanel() {
        return null;
    };
    
    public JMenu getMenu() {
        return null;
    };
    
    class RSSContentModel extends AbstractTableModel {

        private Vector models;
        
        final String[] columnNames = {
            "title", "date", "time", "chemFormula", "description", "link"
        };
    
        public RSSContentModel() {
            models = new Vector();
        }
    
        public void setValueAt(Object value, int row, int column) {
            return;
        }
        
        public void setValueAt(ChemModel model, int row) {
            if (row > getRowCount()) {
                return; // skip everything outside current table
            }
            models.setElementAt(model, row);
            fireTableCellUpdated(row, 1);
        }
    
        public int getColumnCount() {
            return columnNames.length;
        }
    
        public int getRowCount() {
            return models.size();
        }
    
        public String getColumnName(int col) {
            return columnNames[col];
        }
    
        public Class getColumnClass(int col) {
            Object o = getValueAt(0,col);
            if (o == null) {
                return (new String()).getClass();
            } else {
                return o.getClass();
            }
        }
    
        public Object getValueAt(int row, int column) {
            if (row > getRowCount()-1 || column > getColumnCount()-1) {
                return "Error"; // skip everything outside current table
            }
            // "title", "date", "time", "chemFormula", "description", "link"
            ChemModel model = (ChemModel)models.elementAt(row);
            if (model == null) {
                return "";
            }
            if (column == 0) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_TITLE);
            } else if (column == 1) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_DATE);
            } else if (column == 2) {
                return "";
            } else if (column == 3) {
                AtomContainer container = ChemModelManipulator.getAllInOneContainer(model);
                // System.out.println(container);
                MFAnalyser analyser = new MFAnalyser(container);
                return analyser.getMolecularFormula();
            } else if (column == 4) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_DESCRIPTION);
            } else if (column == 5) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_LINK);
            }
            return "Error";
        }
    
        public ChemModel getValueAt(int row) {
            return (ChemModel)models.elementAt(row);
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public void cleanTable() {
            models.clear();
            fireTableDataChanged();
        }

        private void insertBlankRow(int row) {
            models.addElement(null);
            fireTableRowsInserted(row+1, row+1);
        }

    }
    
    private Properties readProperties() {
        Properties props = null;
        File uhome = new File(System.getProperty("user.home"));
        File propsFile = new File(uhome + "/.jmol/rssviewer.props");
        System.out.println("User plugin dir: " + propsFile);
        System.out.println("       exists: " + propsFile.exists());
        if (propsFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(propsFile);
                props = new Properties();
                props.load(fis);
                fis.close();
            } catch (Exception exception) {
                System.out.println("Error while reading rssviewer props: " + exception.toString());
            }
        }
        return props;
    }
    
    private DefaultMutableTreeNode createChannelTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Channels");
        if (channels != null) {
            Enumeration urls = channels.elements();
            while (urls.hasMoreElements()) {
                URL url = (URL)urls.nextElement();
                DefaultMutableTreeNode node = new DefaultMutableTreeNode();
                node.setUserObject(new RSSChannelNode(url));
                root.add(node);
            }
        }
        return root;
    }
    
    class RSSChannelNode {
        
        URL url = null;
        
        RSSChannelNode(URL channel) {
            this.url = channel;
        }
        
        public String toString() {
            return url.toString();
        }

        public URL getURL() {
            return url;
        }
    }

}


