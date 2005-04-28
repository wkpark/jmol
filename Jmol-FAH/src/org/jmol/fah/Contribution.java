/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.fah;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Manage contributions informations
 */
public class Contribution {

  /**
   * @return Contribution singleton
   */
  public static Contribution getContribution() {
    if (_contrib == null) {
      _contrib = new Contribution();
    }
    return _contrib;
  }

  /**
   * Constructor for Contribution
   */
  private Contribution() {
    //Empty
  }

  /**
   * Add information from stanford stats site
   * 
   * @param userName User name
   * @param teamNum Team number
   */
  public void addInformation(String userName, int teamNum) {
    for (int i = 0; i < 4; i++) {
      addInformation(userName, teamNum, i * 1000);
    }
  }

  /**
   * Add information from stanford stats site for a range
   * 
   * @param userName User name
   * @param teamNum Team number
   * @param range Range
   */
  private void addInformation(String userName, int teamNum, int range) {
    //Load new information
    StringBuffer urlName = new StringBuffer();
    urlName.append("http://vspx27.stanford.edu/"); //$NON-NLS-1$
    urlName.append("cgi-bin/main.py?qtype=userpagedet"); //$NON-NLS-1$
    urlName.append("&username="); //$NON-NLS-1$
    urlName.append(userName);
    urlName.append("&teamnum="); //$NON-NLS-1$
    urlName.append(teamNum);
    urlName.append("&prange="); //$NON-NLS-1$
    urlName.append(range);
    try {
      URL url = new URL(urlName.toString());
      InputStream stream = url.openStream();
      InputStreamReader reader = new InputStreamReader(stream);
      HTMLDocument htmlDoc = new HTMLDocumentContribution();
      HTMLEditorKit htmlEditor = new HTMLEditorKit() {
        protected HTMLEditorKit.Parser getParser() {
          return new ParserDelegator() {
            public void parse(Reader r,
                              HTMLEditorKit.ParserCallback cb,
                              boolean ignoreCharSet) throws IOException {
              super.parse(r, cb, true);
            }
          };
        }
      };
      htmlEditor.read(reader, htmlDoc, 0);
    } catch (MalformedURLException mue) {
      mue.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (BadLocationException ble) {
      ble.printStackTrace();
    }
  }

  // Contribution (Singleton)
  private static Contribution _contrib;

  /**
   * HTML Document for Contribution page
   */
  private class HTMLDocumentContribution extends HTMLDocument {

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.text.html.HTMLDocument#getReader(int)
     */
    public HTMLEditorKit.ParserCallback getReader(int pos) {
      return new ContributionReader(pos);
    }

    /**
     * Reader for Contrubition
     */
    private class ContributionReader extends HTMLDocument.HTMLReader {

      /**
       * @param offset
       */
      public ContributionReader(int offset) {
        super(offset);
      }

      /*
       * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleStartTag(
       *      javax.swing.text.html.HTML.Tag,
       *      javax.swing.text.MutableAttributeSet, int)
       */
      public void handleStartTag(HTML.Tag tag, MutableAttributeSet att, int pos) {
        if (tag.equals(HTML.Tag.TABLE)) {
          this._table++;
          this._tableNum++;
          this._column = 0;
        }
        if (tag.equals(HTML.Tag.TR)) {
          this._row++;
          this._column = 0;
        }
        if (tag.equals(HTML.Tag.TD)) {
          this._column++;
        }
        if ((this._table > 0) && (this._row == 1)) {
          //System.out.println(tag);
        }
        super.handleStartTag(tag, att, pos);
      }
	
      /*
       * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleText(
       *      char[],
       *      int)
       */
      public void handleText(char[] data, int pos) {
        if ((this._table > 0) && (this._row > 1) && (this._tableNum == 8)) {
          switch (this._column) {
          case 1: // Project
            this._project = new String(data);
            break;
          case 2: // Count
            this._count = Integer.parseInt(new String(data));
            break;
          }
        }
        super.handleText(data, pos);
      }

      /*
       * @see javax.swing.text.html.HTMLEditorKit.ParserCallback#handleEndTag(
       *      javax.swing.text.html.HTML.Tag,
       *      int)
       */
      public void handleEndTag(HTML.Tag tag, int pos) {
        if ((this._table > 0) && (this._row == 1)) {
          //System.out.println("/" + tag); //$NON-NLS-1$
        }
        if (tag.equals(HTML.Tag.TABLE)) {
          this._table--;
          if (this._table == 0) {
            this._row = 0;
          }
        }
        if (tag.equals(HTML.Tag.TR)) {
          if ((this._project != null) && (this._count > 0)) {
            System.out.print(this._project);
            System.out.print("\t"); //$NON-NLS-1$
            System.out.print(this._count);
            System.out.println();
          }
          this._column = 0;
          this._project = null;
          this._count = 0;
        }
        super.handleEndTag(tag, pos);
      }
	
      // Position
      private int    _column   = 0;
      private int    _row      = 0;
      private int    _table    = 0;
      private int    _tableNum = 0;

      // Current informations
      private String _project  = null;
      private int    _count    = 0;
    }
  }

  /**
   * Main enabling checking getting contribution informations
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    Contribution contrib = getContribution();
    String userName = System.getProperty("org.jmol.fah.user");
    if (userName == null) {
      System.err.println("You must define org.jmol.fah.user");
      return;
    }
    String team = System.getProperty("org.jmol.fah.team");
    if (team == null) {
      System.err.println("You must define org.jmol.fah.team");
      return;
    }
    try {
      int teamNumber = Integer.parseInt(team);
      contrib.addInformation(userName, teamNumber); //$NON-NLS-1$
    } catch (NumberFormatException e) {
      System.err.println("org.jmol.fag.team must be an integer");
    }
  }
}
