
/*
 * @(#)CMLHandler.java   0.1 99/05/05
 *
 * Copyright (c) 1999 E.L. Willighagen All Rights Reserved.
 *
 */

package org.openscience.miniJmol;

import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Enumeration;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.AttributeList;
import org.openscience.jmol.FortranFormat;

public class CMLHandler extends org.xml.sax.HandlerBase {

	private final int UNKNOWN = -1;

	private final int STRING = 1;
	private final int LINK = 2;
	private final int FLOAT = 3;
	private final int INTEGER = 4;
	private final int STRINGARRAY = 5;
	private final int FLOATARRAY = 6;
	private final int INTEGERARRAY = 7;
	private final int FLOATMATRIX = 8;
	private final int COORDINATE2 = 9;
	private final int COORDINATE3 = 10;
	private final int ANGLE = 11;
	private final int TORSION = 12;
	private final int LIST = 13;
	private final int MOLECULE = 14;
	private final int ATOM = 15;
	private final int ATOMARRAY = 16;
	private final int BOND = 17;
	private final int BONDARRAY = 18;
	private final int ELECTRON = 19;
	private final int REACTION = 20;
	private final int CRYSTAL = 21;
	private final int SEQUENCE = 22;
	private final int FEATURE = 23;

	private final String SYSTEMID = "CML-1999-05-15";

	private int currentElement;
	private String builtin = "";

	private ChemFrame cf;
	private Vector cfs;

	private int frameNo;

	private Vector elsym;
	private Vector elid;
	private Vector x3;
	private Vector y3;
	private Vector z3;
	private boolean bondsEnabled;

	public CMLHandler(boolean bondsEnabled) {
		cfs = new Vector();
		frameNo = 0;
		this.bondsEnabled = bondsEnabled;
	}

	public void startDocument() {
	}

	public void endDocument() {
	}

	public Vector returnChemFrames() {
		return cfs;
	}

	public void doctypeDecl(String name, String publicId, String systemId)
			throws Exception {
		warn("Name: " + name);
		warn("PublicId: " + publicId);
		warn("SystemId: " + systemId);
	}

	public void startElement(String name, AttributeList atts)
			throws SAXException {

		setCurrentElement(name);
		switch (currentElement) {
		case ATOM :
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).equals("id")) {
					elid.addElement(atts.getValue(i));
				}
			}
			break;

		case COORDINATE3 :
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).equals("builtin")) {
					builtin = atts.getValue(i);
				}
			}
			break;

		case STRING :
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).equals("builtin")) {
					builtin = atts.getValue(i);
				}
			}
			break;

		case ATOMARRAY :
			break;

		case STRINGARRAY :
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).equals("builtin")) {
					builtin = atts.getValue(i);
				}
			}
			break;

		case FLOATARRAY :
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).equals("builtin")) {
					builtin = atts.getValue(i);
				}
			}
			break;

		case MOLECULE :
			cf = new ChemFrame(bondsEnabled);
			elsym = new Vector();
			elid = new Vector();
			x3 = new Vector();
			y3 = new Vector();
			z3 = new Vector();
			frameNo++;
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).equals("id")) {
					cf.setInfo(atts.getValue(i));
				}
			}
			break;

		case LIST :
			for (int i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).equals("convention")) {
					warn("Convention: " + atts.getValue(i));
				}
			}
			break;
		}
	}

	public void endElement(String name) {

		setCurrentElement(name);
		builtin = "";
		switch (currentElement) {
		case MOLECULE :
			int atomcount = elsym.size();
			if ((x3.size() == atomcount) && (y3.size() == atomcount)
					&& (z3.size() == atomcount)) {
				Enumeration atoms = elsym.elements();
				Enumeration x3s = x3.elements();
				Enumeration y3s = y3.elements();
				Enumeration z3s = z3.elements();
				while (atoms.hasMoreElements()) {
					String atype = (String) atoms.nextElement();
					double x = FortranFormat.atof((String) x3s.nextElement());
					double y = FortranFormat.atof((String) y3s.nextElement());
					double z = FortranFormat.atof((String) z3s.nextElement());

					try {
						cf.addAtom(atype, (float) x, (float) y, (float) z);
					} catch (Exception e) {
						e.printStackTrace();
						notify("CMLhandler error while adding atom: " + e,
								SYSTEMID, 149, 1);
					}
				}
			}
			cfs.addElement(cf);
			break;
		}
	}

	public void characters(char ch[], int start, int length) {

		String s = toString(ch, start, length).trim();
		switch (currentElement) {
		case STRING :
			if (builtin.equals("elementType")) {
				elsym.addElement(s);
			}
			break;

		case COORDINATE3 :
			if (builtin.equals("xyz3")) {
				try {
					StringTokenizer st = new StringTokenizer(s);
					x3.addElement(st.nextToken());
					y3.addElement(st.nextToken());
					z3.addElement(st.nextToken());
				} catch (Exception e) {
					notify("CMLParsing error: " + e, SYSTEMID, 175, 1);
				}
			}
			break;

		case STRINGARRAY :
			if (builtin.equals("id")) {
				try {
					StringTokenizer st = new StringTokenizer(s);
					while (st.hasMoreTokens()) {
						elid.addElement(st.nextToken());
					}
				} catch (Exception e) {
					notify("CMLParsing error: " + e, SYSTEMID, 186, 1);
				}
			} else if (builtin.equals("elementType")) {
				try {
					StringTokenizer st = new StringTokenizer(s);
					while (st.hasMoreTokens()) {
						elsym.addElement(st.nextToken());
					}
				} catch (Exception e) {
					notify("CMLParsing error: " + e, SYSTEMID, 194, 1);
				}
			}
			break;

		case FLOATARRAY :
			if (builtin.equals("x3")) {
				try {
					StringTokenizer st = new StringTokenizer(s);
					while (st.hasMoreTokens()) {
						x3.addElement(st.nextToken());
					}
				} catch (Exception e) {
					notify("CMLParsing error: " + e, SYSTEMID, 205, 1);
				}
			} else if (builtin.equals("y3")) {
				try {
					StringTokenizer st = new StringTokenizer(s);
					while (st.hasMoreTokens()) {
						y3.addElement(st.nextToken());
					}
				} catch (Exception e) {
					notify("CMLParsing error: " + e, SYSTEMID, 213, 1);
				}
			} else if (builtin.equals("z3")) {
				try {
					StringTokenizer st = new StringTokenizer(s);
					while (st.hasMoreTokens()) {
						z3.addElement(st.nextToken());
					}
				} catch (Exception e) {
					notify("CMLParsing error: " + e, SYSTEMID, 221, 1);
				}
			}
			break;
		}
	}

	private void setCurrentElement(String name) {

		if (name.equals("string")) {
			currentElement = STRING;
		} else if (name.equals("link")) {
			currentElement = LINK;
		} else if (name.equals("float")) {
			currentElement = FLOAT;
		} else if (name.equals("integer")) {
			currentElement = INTEGER;
		} else if (name.equals("stringArray")) {
			currentElement = STRINGARRAY;
		} else if (name.equals("floatArray")) {
			currentElement = FLOATARRAY;
		} else if (name.equals("integerArray")) {
			currentElement = INTEGERARRAY;
		} else if (name.equals("floatMatrix")) {
			currentElement = FLOATMATRIX;
		} else if (name.equals("coordinate2")) {
			currentElement = COORDINATE2;
		} else if (name.equals("coordinate3")) {
			currentElement = COORDINATE3;
		} else if (name.equals("angle")) {
			currentElement = ANGLE;
		} else if (name.equals("torsion")) {
			currentElement = TORSION;
		} else if (name.equals("list")) {
			currentElement = LIST;
		} else if (name.equals("molecule")) {
			currentElement = MOLECULE;
		} else if (name.equals("atom")) {
			currentElement = ATOM;
		} else if (name.equals("atomArray")) {
			currentElement = ATOMARRAY;
		} else if (name.equals("bond")) {
			currentElement = BOND;
		} else if (name.equals("bondArray")) {
			currentElement = BONDARRAY;
		} else if (name.equals("electron")) {
			currentElement = ELECTRON;
		} else if (name.equals("reaction")) {
			currentElement = REACTION;
		} else if (name.equals("crystal")) {
			currentElement = CRYSTAL;
		} else if (name.equals("sequence")) {
			currentElement = SEQUENCE;
		} else if (name.equals("feature")) {
			currentElement = FEATURE;
		} else {
			currentElement = UNKNOWN;
		}
	}

	public void error(String message, String systemId, int line, int column)
			throws Exception {
		notify(message, systemId, line, column);
	}

	public void notify(String message, String systemId, int line,
			int column) {
		System.out.println("Message: " + message);
		System.out.println("SystemId: " + systemId);
		System.out.println("Line: " + line);
		System.out.println("Column: " + column);
	}

	public void warn(String s) {
		System.out.println(s);
	}

	private String toString(char ch[], int start, int length) {

		StringBuffer x = new StringBuffer();
		for (int i = 0; i < length; i++) {
			x.append(ch[start + i]);
		}
		return x.toString();
	}

	public void warning(SAXParseException e) throws SAXException {
		e.printStackTrace();
	}
}
