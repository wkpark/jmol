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
package org.openscience.cdk.applications.plugin;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.applications.swing.SortedTableModel;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.ListSelectionModel;
import javax.swing.JTabbedPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;


/**
 * Plugin that can read RSS sources and extract molecular content
 * in the CML2 format from it.
 *
 * @author Egon Willighagen <egonw@sci.kun.nl>
 */
public class RSSViewerPlugin implements CDKPluginInterface {

    private CDKEditBus editBus = null;
    private Vector channels = null;
    private JTree rssTree = null;
    private RSSContentModel channelContent = null;
    private SortedTableModel sortedContent = null;
    private RSSContentModel aggregatedContent = null;
    private SortedTableModel sortedAggregatedContent = null;
    private JPanel pluginPanel = null;
    private JComboBox elementFilter = null;
    
    public void setEditBus(CDKEditBus editBus) {
        this.editBus = editBus;
    }
    
    public RSSViewerPlugin() {
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
            String url = props.getProperty("Channel" + new Integer(i).toString(), "");
            String title = props.getProperty("Channel" + new Integer(i).toString() + "Title", "");
            if (url.length() == 0) {
                System.out.println("Could not find URL for " + i + "th channel");
            } else {
                try {
                    URL rssURL = new URL(url);
                    RSSChannel channel = new RSSChannel(rssURL, title);
                    channels.addElement(channel);
                    System.out.println("Added RSS channel: " + url);
                } catch (Exception exception) {
                    System.out.println("URL for " + i + "th channel is not valid: " + url);
                }
            }
        }
    }
    
    public void start() {
        // fill the aggregated table
        if (aggregatedContent == null) {
            aggregatedContent = new RSSContentModel();
        }
        fillAggregatedTable();
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
        if (pluginPanel == null) {
            pluginPanel = createPanel();
        }
        return pluginPanel;
    }
        
    private JPanel createPanel() {
        JPanel RSSViewerPanel = new JPanel();
        
        // The Channel list (possibly grouped, therefor a tree)
        rssTree = new JTree(createChannelTree());
        rssTree.addTreeSelectionListener(
            new RSSChannelTreeListener()
        );
        rssTree.validate();
        JScrollPane treePanel = new JScrollPane(rssTree);
        treePanel.validate();
        
        // A table showing the entries in one channel
        channelContent = new RSSContentModel();
        sortedContent = new SortedTableModel(channelContent);
        JTable channelTable = new JTable(sortedContent);
        sortedContent.addMouseListenerToHeaderInTable(channelTable);
        channelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel rowSM = channelTable.getSelectionModel();
        rowSM.addListSelectionListener(
            new RSSChannelItemsTableListener(sortedContent, channelContent)
        );

        // A table showing the entries of all aggregated channels
        if (aggregatedContent == null) {
            aggregatedContent = new RSSContentModel();
        }
        sortedAggregatedContent = new SortedTableModel(aggregatedContent);
        JTable aggregatedTable = new JTable(sortedAggregatedContent);
        sortedAggregatedContent.addMouseListenerToHeaderInTable(aggregatedTable);
        aggregatedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel aggregatedRowSM = aggregatedTable.getSelectionModel();
        aggregatedRowSM.addListSelectionListener(
            new RSSChannelItemsTableListener(sortedAggregatedContent, aggregatedContent)
        );
        JScrollPane scrollableTable = new JScrollPane(aggregatedTable);
        JPanel filterPane = new JPanel();
        JLabel filterName = new JLabel("Filter: contains element ");
        Vector elements = new Vector();
        elements.add("");
        elements.add("C");
        elements.add("N");
        elements.add("Cr");
        elementFilter = new JComboBox(elements);
        JButton applyFilter = new JButton("Apply");
        applyFilter.addActionListener(new ApplyFilterListener());
        filterPane.add(filterName);
        filterPane.add(elementFilter);
        filterPane.add(applyFilter);
        
        JPanel aggregatedPane = new JPanel(new BorderLayout());
        aggregatedPane.add(filterPane, BorderLayout.NORTH);
        aggregatedPane.add(scrollableTable, BorderLayout.CENTER);

        channelTable.validate();
        JTabbedPane tabbedPane = new JTabbedPane();
        JScrollPane contentPane = new JScrollPane(channelTable);
        contentPane.validate();
        tabbedPane.addTab("Single Channel", null, contentPane, "Displays the content of one RSS channel");
        tabbedPane.addTab("Aggregated", null, aggregatedPane, "Displays the contents of all RSS channels");
        
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treePanel, tabbedPane);
        RSSViewerPanel.add(splitter);
        RSSViewerPanel.validate();
        
        return RSSViewerPanel;
    };
    
    private void fillAggregatedTable() {
        // put all channels into the aggregated table
        if (aggregatedContent != null) { // ok, JPanel is set up
            aggregatedContent.cleanTable();
            Enumeration channels = this.channels.elements();
            while (channels.hasMoreElements()) {
                RSSChannel channel = (RSSChannel)channels.nextElement();
                parseChannelIntoTable(aggregatedContent, channel.getURL());
            }
        } else {
            // should give some warning
        }
    }
    
    public JPanel getPluginConfigPanel() {
        return null;
    };
    
    public JMenu getMenu() {
        return null;
    };
    
    class RSSContentModel extends AbstractTableModel {

        private Vector models;
        
        final String[] columnNames = {
            "title", "date", "chemFormula", "dimension"
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
            AtomContainer container = ChemModelManipulator.getAllInOneContainer(model);
            if (column == 0) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_TITLE);
            } else if (column == 1) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_DATE);
            } else if (column == 2) {
                container = ChemModelManipulator.getAllInOneContainer(model);
                MFAnalyser analyser = new MFAnalyser(container);
                return analyser.getMolecularFormula();
            } else if (column == 3) {
                int dim = 0;
                if (container.getAtomCount() > 0) {
                    if (GeometryTools.has2DCoordinates(container)) {
                        dim += 2;
                    }
                    if (GeometryTools.has3DCoordinates(container)) {
                        dim += 3;
                    }
                    return dim + "D";
                } else {
                    return "";
                }
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
            Enumeration channelEnum = channels.elements();
            while (channelEnum.hasMoreElements()) {
                RSSChannel channel = (RSSChannel)channelEnum.nextElement();
                DefaultMutableTreeNode node = new DefaultMutableTreeNode();
                node.setUserObject(new RSSChannelNode(channel));
                root.add(node);
            }
        }
        return root;
    }
    
    private void parseChannelIntoTable(RSSContentModel channelContent, URL url) {
        System.out.println("Should load this RSS now: " + url.toString());
        ChemSequence channelItems = null;
        try {
            InputStream is = url.openStream();
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
            ChemModel[] models = channelItems.getChemModels();
            System.out.println("#items = " + models.length);
            int itemsAlreadyInTable = channelContent.getRowCount();
            for (int i=0; i<models.length; i++) {
                ChemModel model = models[i];
                boolean passedFilter = false;
                // apply filter
                Atom[] modelAtoms = ChemModelManipulator.getAllInOneContainer(model).getAtoms();
                String mustContainElement = elementFilter.getSelectedItem().toString();
                if (mustContainElement.length() > 0) {
                    // filter is set
                    for (int j=0; j<modelAtoms.length; j++) {
                        Atom atom = modelAtoms[j];
                        if (atom.getSymbol().equals(mustContainElement)) {
                            passedFilter = true;
                            j = modelAtoms.length;
                        }
                    }
                } else {
                    // filter is not set
                    passedFilter = true;
                }
                if (passedFilter) {
                    // filter not set
                    int lastLine = channelContent.getRowCount();
                    channelContent.insertBlankRow(lastLine);
                    channelContent.setValueAt(models[i], lastLine);
                }
            }
        }
    }
    
    class RSSChannelNode {
        
        RSSChannel channel = null;
        
        RSSChannelNode(RSSChannel channel) {
            this.channel = channel;
        }
        
        public String toString() {
            String stringRepresentation = channel.getURL().toString();
            if (channel.getTitle().length() > 0) {
                stringRepresentation = channel.getTitle();
            }
            return stringRepresentation;
        }

        public URL getURL() {
            return channel.getURL();
        }
    }

    class RSSChannelTreeListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)rssTree.getLastSelectedPathComponent();
            
            if (node == null) return;
            
            Object nodeInfo = node.getUserObject();
            if (nodeInfo instanceof RSSChannelNode) {
                RSSChannelNode rssNode = (RSSChannelNode)nodeInfo;
                channelContent.cleanTable();
                parseChannelIntoTable(channelContent, rssNode.getURL());
            }
        }
        
        private void transferRSSProperty(ChemModel model, String propertyName, int row, int column) {
            Object property = model.getProperty(propertyName);
            if (property != null) {
                channelContent.setValueAt(property, row, column);
                System.out.println("Transfered data: " + property.toString());
            }
        }
    }
    
    class RSSChannelItemsTableListener implements ListSelectionListener {
        
        private SortedTableModel sortedModelContent = null;
        private RSSContentModel modelContent = null;
        
        public RSSChannelItemsTableListener(SortedTableModel sortedModelContent, 
                                            RSSContentModel modelContent) {
            this.modelContent = modelContent;
            this.sortedModelContent = sortedModelContent;
        }
        
        public void valueChanged(ListSelectionEvent e) {
            // Ignore extra messages
            if (e.getValueIsAdjusting()) return;
            
            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            if (lsm.isSelectionEmpty()) {
                // no rows are selected
            } else {
                int selectedRow = lsm.getMinSelectionIndex();
                ChemModel model = modelContent.getValueAt(sortedModelContent.getSortedIndex(selectedRow));
                ChemSequence sequence = new ChemSequence();
                sequence.addChemModel(model);
                ChemFile file = new ChemFile();
                file.addChemSequence(sequence);
                editBus.showChemFile(file);
            }
        }
    }
    
    class ApplyFilterListener implements ActionListener {
        
        public ApplyFilterListener() {
        }
        
        public void actionPerformed(ActionEvent e) {
            fillAggregatedTable();
        }
    }

    class RSSChannel {
        
        private URL url;
        private String title;
        
        public RSSChannel(URL url, String title) {
            this.url = url;
            this.title = title;
        }
        
        public URL getURL() {
            return this.url;
        }
        
        public String getTitle() {
            return this.title;
        }
    }
}


