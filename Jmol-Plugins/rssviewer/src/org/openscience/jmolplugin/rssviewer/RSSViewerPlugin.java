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

import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.ChemicalRSSReader;
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
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;

/**
 * Plugin that can read RSS sources and extract molecular content
 * in the CML2 format from it.
 *
 * @author Egon Willighagen
 */
public class RSSViewerPlugin implements JmolPluginInterface {

    private Vector channels = null;
    private JTree rssTree = null;
    
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
                    }
                }
            }
        });
        JScrollPane treePanel = new JScrollPane(rssTree);
        
        // A table showing the entries in the channel
        JTable channelTable = new JTable(new RSSContentModel());
        JScrollPane contentPane = new JScrollPane(channelTable);
        
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

        final String[] columnNames = {
            "title", "date", "time", "chemFormula"
        };
    
        public RSSContentModel() {
        }
    
        public int getColumnCount() {
            return columnNames.length;
        }
    
        public int getRowCount() {
            return 1;
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
    
        public Object getValueAt(int row, int col) {
            return new String("empty");
        }
    
        public boolean isCellEditable(int row, int col) {
            return false;
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


