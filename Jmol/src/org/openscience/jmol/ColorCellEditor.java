
package org.openscience.jmol;

import javax.swing.*;
import javax.swing.DefaultCellEditor;
import java.awt.Color;
import java.awt.AWTEvent;
import java.util.EventObject;


public class ColorCellEditor extends DefaultCellEditor {

    protected JComponent editorComponent;
    protected EditorDelegate delegate;

    public ColorCellEditor(JButton x) {
        super(new JTextField("Edit Me"));
        this.editorComponent = x;
        this.clickCountToStart = 2;
        this.delegate = new EditorDelegate() {
            public void setValue(Object y) {
                super.setValue(y);
                if (y instanceof Color) {
                    ((JButton)editorComponent).setBackground((Color) y);
                } else {
                    ((JButton)editorComponent).setBackground(Color.black);
                }
            }

            public Object getCellEditorValue() {
                return (Object) editorComponent.getBackground();
            }

            public boolean startCellEditing(EventObject anEvent) {
                if (anEvent instanceof AWTEvent) {
                    Color color = JColorChooser.showDialog((JButton)editorComponent, 
                                                           "Choose and Atom Color", 
                                                           editorComponent.getBackground());
                    
                    return true;
                }
                return false;
            }
            
            public boolean stopCellEditing() {
                return true;
            }
        };
        ((JButton)editorComponent).addItemListener(delegate);
    }    
}
