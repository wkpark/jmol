package javajs.swing;

import org.jmol.java.BS;
import javajs.util.SB;


abstract public interface AbstractTableModel extends TableColumn {

	TableColumn getColumn(int i);

	void toHTML(SB sb, String id, BS bsSelectedRows);

}
