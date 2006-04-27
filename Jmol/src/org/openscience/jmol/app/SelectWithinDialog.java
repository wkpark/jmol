// phdana pfaat ->
/*
 * Created on Jun 9, 2005
 *
 * Dialog useful to the user for setting up parameters
 * to be sent to the "select within" command
 */
package org.openscience.jmol.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

/**
 * Dialog useful to the user for setting up parameters
 * to be sent to the "select within" command
 * 
 * @author DanaP
 *
 */
public class SelectWithinDialog extends JDialog 
   implements ActionListener, CaretListener {
    
    private final static Dimension labelSize = new Dimension(100,18);
    private final static Dimension valueSize = new Dimension(150,18);
    private final static Dimension optionalSize = new Dimension(150, 18);
    private final static Dimension previewSize = new Dimension(300,80);
    
    private final static int CommandLegal = 0;
    private final static int CommandTooFewParams = 1;
    private final static int CommandTooManyParams = 2;
    private final static int CommandDistanceNonNumeric = 3;
    private final static int CommandResidueNumberNonNumeric = 4;    
    
    private static SelectWithinDialog dlg;
    
    private String scriptCommand; // final output

    private JPanel optPanel;
    private JComboBox select;
    private JTextField distance;
    private JTextField chain;
    private JTextField residueName;
    private JCheckBox excludeSameNumberedWaters;
    private JTextField residueNumber;
    private JTextField atom;

    private JCheckBox selected;
    
    private String command; // command currently being built (or error message)
    private int commandState;
    
    private JTextField preview;
    
    public static String showDialog(Component comp)
    {
        Frame frm = findFrame(comp);
        if (dlg == null)
           dlg = new SelectWithinDialog(frm);
        dlg.scriptCommand = null;
        dlg.updateCommand();
        dlg.show();
        
        return dlg.scriptCommand;
    }
    
    private static Frame findFrame(Component comp)
    {
        Component parent=comp;
        for (Component p = comp.getParent(); p!=null; p=p.getParent())
            parent=p;
        if (parent instanceof Frame)
            return (Frame)parent;
        return null;
    }

    private SelectWithinDialog(Frame owner)
    {
        super(owner,JmolResourceHandler
        .getStringX("SelectWithinDialog.title"),true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        
        JButton b;
        String text;

        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel southPanel = new JPanel(new BorderLayout());
        
        // option panels get added to this implicitly as they are created
        optPanel = new JPanel();
        optPanel.setLayout(new BoxLayout(optPanel,BoxLayout.Y_AXIS));        
        
        text = JmolResourceHandler.getStringX("SelectWithinDialog.selectLabel");
        createSelectOption(text);
        
        text = JmolResourceHandler.getStringX("SelectWithinDialog.distanceLabel");
        distance = createOption(text,"distance");

        text = JmolResourceHandler.getStringX("SelectWithinDialog.ofLabel");
        createOfOption(text);

        text = JmolResourceHandler.getStringX("SelectWithinDialog.chainLabel");
        chain = createOption(text,"chain");
        
        text = JmolResourceHandler.getStringX("SelectWithinDialog.residueNameLabel");
        residueName = createOption(text,"residueName");
        
        text = JmolResourceHandler.getStringX("SelectWithinDialog.residueNumberLabel");
        createResidueNumberOption(text,"residueNumber");
        
        text = JmolResourceHandler.getStringX("SelectWithinDialog.atomLabel");
        atom = createOption(text,"atom");
        
        createSelectedOption();
                       
        JPanel previewPanel = new JPanel(new BorderLayout());
        text = JmolResourceHandler.getStringX("SelectWithinDialog.previewLabel"); 
        previewPanel.setBorder(BorderFactory.createTitledBorder(text));
        
        preview = new JTextField();
        preview.setPreferredSize(previewSize);
        preview.setEditable(false);
        preview.setBackground(Color.white);
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        previewPanel.add(preview, BorderLayout.CENTER);        
                
        JPanel btnPanel = new JPanel();
        
        text = JmolResourceHandler.getStringX("SelectWithinDialog.okLabel");
        b = new JButton(text);
        b.setActionCommand("ok");
        b.addActionListener(this);
        btnPanel.add(b);
        
        btnPanel.add(Box.createHorizontalStrut(20));

        text = JmolResourceHandler.getStringX("SelectWithinDialog.cancelLabel");
        b = new JButton(text);
        b.setActionCommand("cancel");
        b.addActionListener(this);
        btnPanel.add(b);

        centerPanel.add(optPanel,BorderLayout.CENTER);
        centerPanel.add(southPanel, BorderLayout.SOUTH);

        southPanel.add(previewPanel, BorderLayout.SOUTH);
        
        getContentPane().add(centerPanel,BorderLayout.CENTER);
        getContentPane().add(btnPanel,BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(owner);        
    }
    
    private JTextField createTextField(String cmd)
    {
        //JTextField tf = new JTextField(valueColumns);
        JTextField tf = new JTextField();
        tf.setPreferredSize(valueSize);

        tf.setActionCommand(cmd);
        //tf.addActionListener(this); // <- dont need this
        tf.addCaretListener(this);
        
        return tf;
    }
    
    private JLabel createLabel(String name)
    {
        JLabel label = new JLabel(name);
        label.setPreferredSize(labelSize);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        
        return label;
    }

    // creates a panel with lable & text field
    // adds panel to optPanel memeber variable
    // returns text field
    private JTextField createOption(String name, String cmd)
    {
        JPanel p = new JPanel();
        p.add(createLabel(name));        
        JTextField tf = createTextField(cmd);
        p.add(tf);

        JPanel opt = new JPanel();        
        opt.setPreferredSize(optionalSize);
        p.add(opt);
        
        optPanel.add(p);
        
        return tf;
    }
    
    private void createResidueNumberOption(String name, String cmd)
    {
        JPanel p = new JPanel();
        p.add(createLabel(name));        
        residueNumber = createTextField(cmd);
        p.add(residueNumber);
        
        String text = JmolResourceHandler.getStringX("SelectWithinDialog.excludeSameNumberedWaters");
        excludeSameNumberedWaters = new JCheckBox(text);
        excludeSameNumberedWaters.setSelected(true);
        excludeSameNumberedWaters.addActionListener(this);
        excludeSameNumberedWaters.setActionCommand("exludeSameNumberedWaters");
        excludeSameNumberedWaters.setPreferredSize(optionalSize);
        p.add(excludeSameNumberedWaters);

        optPanel.add(p);
    }

    private void createSelectOption(String name)
    {
        JPanel p = new JPanel();
        p.add(createLabel(name));

        String[] values = new String[] {
                JmolResourceHandler.getStringX("SelectWithinDialog.all"),
                JmolResourceHandler.getStringX("SelectWithinDialog.allExceptWaters")
        };
        select = new JComboBox(values);
        select.setPreferredSize(valueSize);
        select.addActionListener(this);
        p.add(select);        
        
        JPanel opt = new JPanel();        
        opt.setPreferredSize(optionalSize);
        p.add(opt);

        optPanel.add(p);        
    }

    private void createOfOption(String name)
    {
        JPanel p = new JPanel();
        
        JPanel empty = new JPanel();
        empty.setPreferredSize(labelSize);
        p.add(empty);

        JLabel label = new JLabel(name);
        label.setPreferredSize(valueSize);
        p.add(label);        
        
        JPanel opt = new JPanel();        
        opt.setPreferredSize(optionalSize);
        p.add(opt);

        optPanel.add(p);        
    }

    private void createSelectedOption()
    {
        JPanel p = new JPanel();
        
        JPanel empty = new JPanel();
        empty.setPreferredSize(labelSize);
        p.add(empty);

        String text = JmolResourceHandler.getStringX("SelectWithinDialog.selectedLabel"); 
        selected = new JCheckBox(text);
        selected.addActionListener(this);
        selected.setPreferredSize(valueSize);
        p.add(selected);        
        
        JPanel opt = new JPanel();        
        opt.setPreferredSize(optionalSize);
        p.add(opt);

        optPanel.add(p);        
    }
    
    private int updateCommandAndState()
    {        
        String sDistance = distance.getText().trim();
        String sChain = chain.getText().trim();
        String sResidueName = residueName.getText().trim();
        String sResidueNumber = residueNumber.getText().trim();
        String sAtom = atom.getText().trim();
        
        boolean bDistance = (sDistance.length() > 0);
        boolean bChain = (sChain.length() > 0);
        boolean bResidueName = (sResidueName.length() > 0);
        boolean bResidueNumber = (sResidueNumber.length() > 0);
        boolean bAtom = (sAtom.length() > 0);
        boolean bSelected = selected.isSelected();
        boolean bAll = select.getSelectedIndex() == 0;
        boolean bExcludeSameNumberedWaters = excludeSameNumberedWaters.isSelected();

        if (bSelected)
        {
            // if we are going with selected then
            // ignore these other params...
            bChain = bResidueName = bResidueNumber = bAtom = false;
            
            chain.setEnabled(false);            
            residueName.setEnabled(false);            
            residueNumber.setEnabled(false);            
            atom.setEnabled(false);
            
            chain.setBackground(Color.lightGray);            
            residueName.setBackground(Color.lightGray);            
            residueNumber.setBackground(Color.lightGray);            
            atom.setBackground(Color.lightGray);            
        }
        else
        {
            chain.setEnabled(true);            
            residueName.setEnabled(true);            
            residueNumber.setEnabled(true);            
            atom.setEnabled(true);            

            chain.setBackground(Color.white);            
            residueName.setBackground(Color.white);            
            residueNumber.setBackground(Color.white);            
            atom.setBackground(Color.white);            
        }
                
        double dDistance = 0.0;
        if (bDistance)
        {
            try
            {
                dDistance = Double.parseDouble(sDistance);            
            }
            catch (NumberFormatException e)
            {
                return CommandDistanceNonNumeric;
            }
        }

        int iResidueNumber = 0;
        if (bResidueNumber)
        {
            try
            {
                iResidueNumber = Integer.parseInt(sResidueNumber);
            }
            catch (NumberFormatException e)
            {
                return CommandResidueNumberNonNumeric;            
            }
        }

        if (!bDistance)
            return CommandTooFewParams;
                
        if (!bResidueNumber && !bResidueName && !bAtom && !bChain && !bSelected)
            return CommandTooFewParams;
        
        String param="";
        boolean onlyAtom = false;
        if (bSelected)
        {
            param = "selected";
        }
        else if (bResidueName)
        {
            param = sResidueName;
        }
        else if (bResidueNumber)
        {
            param = sResidueNumber;
        }
        else if (bAtom) 
        {
            param = sAtom;
            onlyAtom = true;
        }
        
        // if both name and number ...concatenate
        if (bResidueName && bResidueNumber)
            param = param + iResidueNumber;
        
        if (bChain && !bSelected)
            param = param + ":" + sChain;
        
        if (bAtom && !onlyAtom && !bSelected)
            param = param  + "." + sAtom;
        
        StringBuffer sb = new StringBuffer();
        sb.append("select within(");
        sb.append(dDistance);
        sb.append(",");
        sb.append(param);
        if (bResidueNumber && bExcludeSameNumberedWaters)
            sb.append(" and not waters");
        sb.append(")");
        
        if (!bAll)
            sb.append(" and not waters");
        
        command = sb.toString();
        
        return CommandLegal;
    }
        
    private void updateCommand()
    {
        commandState = updateCommandAndState();
        
        if (commandState != CommandLegal)
           command = "...";
                
        preview.setText(command);
    }
    
    // display error & return true if current settings are not a legal command...
    private boolean isIllegal()
    {        
        if (commandState == CommandLegal)                   
           return false;
        
        String msg;
        if (commandState == CommandTooFewParams)
            msg = JmolResourceHandler.getStringX("SelectWithinDialog.tooFewParamsError"); 
        else if (commandState == CommandTooManyParams)
            msg = JmolResourceHandler.getStringX("SelectWithinDialog.tooManyParamsError"); 
        else if (commandState == CommandDistanceNonNumeric)
            msg = JmolResourceHandler.getStringX("SelectWithinDialog.distanceNonNumericError"); 
        else if (commandState == CommandResidueNumberNonNumeric)
            msg = JmolResourceHandler.getStringX("SelectWithinDialog.residueNumberNonNumericError"); 
        else
            msg = "unknown";
        
        JOptionPane.showMessageDialog(this,msg);
        
        return true;
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        String cmd = e.getActionCommand();
        if (cmd.equals("cancel"))
        {
            scriptCommand = null;
            dispose();
        }
        else if (cmd.equals("ok"))
        {
            if (isIllegal())
                return;
            scriptCommand = command;
            dispose();
        }
        else
        {
            // check boxes...
            updateCommand();
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.event.CaretListener#caretUpdate(javax.swing.event.CaretEvent)
     */
    public void caretUpdate(CaretEvent e) {
        updateCommand();        
    }
            
}
//phdana pfaat <-
