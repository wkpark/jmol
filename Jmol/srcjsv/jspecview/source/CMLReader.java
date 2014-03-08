/* Copyright (c) 2007-2010 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Vector;

import javajs.util.PT;


/**
 * Representation of a XML Source.
 * @author Craig A.D. Walters
 * @author Prof. Robert J. Lancashire
 */

class CMLReader extends XMLReader {

  public CMLReader() {
  	// for reflection
  }

  private boolean specfound = false;
  
  @Override
	protected JDXSource getXML(BufferedReader br) {
    try {

      source = new JDXSource(JDXSource.TYPE_SIMPLE, filePath);
      
      getSimpleXmlReader(br);
      processXML(CML_0, CML_1);

      if (!checkPointCount())
        return null;

      populateVariables();

    } catch (Exception pe) {
      //System.out.println(getBufferData());
      System.err.println("Error: " + pe);
    }

    processErrors("CML");

    try {
      br.close();
    } catch (IOException e1) {
      //
    }

    return source;
  }

  String Ydelim = "";

  /**
   * Process the XML events. The while() loop here
   * iterates through XML tags until a </xxxx> tag
   * is found.
   *
   *
   *
   * @param tagId
   * @return true to continue with encapsulated tags
   * @throws Exception
   */
  @Override
  protected boolean processTag(int tagId) throws Exception {
    //System.out.println(tagId + " " + tagNames[tagId]);
    switch (tagId) {
    case CML_SPECTRUM:
      processSpectrum();
      return false; // once only
    case CML_SPECTRUMDATA:
      processSpectrumData();
      return true;
    case CML_PEAKLIST:
      processPeaks();
      return false; // only once
    case CML_SAMPLE:
      processSample();
      return true;
    case CML_METADATALIST:
      processMetadataList();
      return true;
    case CML_CONDITIONLIST:
      processConditionList();
      return true;
    case CML_PARAMETERLIST:
      processParameterList();
      return true;
    case CML_PEAKLIST2:
      // via CML_PEAKLIST
      processPeakList();
      return true;
    default:
      System.out.println("CMLSource not processing tag " + tagNames[tagId]
          + "!");
      // should not be here
      return false;
    }
  }

	@Override
	protected void processEndTag(int tagId) throws Exception {
	}

	private void processSpectrum() throws Exception {
    // title OR id here
    if (attrList.contains("title"))
      title = reader.getAttrValue("title");
    else if (attrList.contains("id"))
      title = reader.getAttrValue("id");

    // "type" is a required tag
    if (attrList.contains("type"))
      techname = reader.getAttrValue("type").toUpperCase() + " SPECTRUM";
  }

  /**
   * Process the metadata CML events
   *@throws Exception
   */
  private void processMetadataList() throws Exception {
    if (tagName.equals("metadata")) {
      tagName = reader.getAttrValueLC("name");
      if (tagName.contains(":origin")) {
        if (attrList.contains("content"))
          origin = reader.getAttrValue("content");
        else
          origin = reader.thisValue();
      } else if (tagName.contains(":owner")) {
        if (attrList.contains("content"))
          owner = reader.getAttrValue("content");
        else
          owner = reader.thisValue();
       }else if (tagName.contains("observenucleus")) {
        if (attrList.contains("content"))
        	obNucleus = reader.getAttrValue("content");
          else
            obNucleus = reader.thisValue();
        }
     }    
  }

  /**
   * Process the parameter CML events
   *@throws Exception
   */
  private void processParameterList() throws Exception {
    if (tagName.equals("parameter")) {
      String title = reader.getAttrValueLC("title");
      if (title.equals("nmr.observe frequency")) {
        StrObFreq = reader.qualifiedValue();
        obFreq = Double.parseDouble(StrObFreq);
      } else if (title.equals("nmr.observe nucleus")) {
        obNucleus = reader.getAttrValue("value");
      } else if (title.equals("spectrometer/data system")) {
        modelType = reader.getAttrValue("value");
      } else if (title.equals("resolution")) {
        resolution = reader.qualifiedValue();
      }
    }
  }

  /**
   * Process the ConditionList CML events (found in NMRShiftDB)
   *@throws Exception
   */
  private void processConditionList() throws Exception {
    if (tagName.equals("scalar")) {
      String dictRef = reader.getAttrValueLC("dictRef");
      if (dictRef.contains(":field")) {
        StrObFreq = reader.thisValue();
        if (StrObFreq.charAt(0) > 47
            && StrObFreq.charAt(0) < 58)
          obFreq = Double.parseDouble(StrObFreq);
      }
    }
  }

  /**
   * Process the sample CML events
   *@throws Exception
   */
  private void processSample() throws Exception {
    if (tagName.equals("formula")) {
      if (attrList.contains("concise"))
        molForm = reader.getAttrValue("concise");
      else if (attrList.contains("inline"))
        molForm = reader.getAttrValue("inline");
    } else if (tagName.equals("name")) {
      casName = reader.thisValue();
    }
  }

  /**
   * Process the spectrumdata CML events
   *@throws Exception
   */
  private void processSpectrumData() throws Exception {
    if (tagName.equals("xaxis")) {
      if (attrList.contains("multipliertodata"))
        xFactor = Double.parseDouble(reader.getAttrValue("multiplierToData"));
      reader.nextTag();
      tagName = reader.getTagName();
      attrList = reader.getAttributeList();
      if (tagName.equals("array")) {
        
        xUnits = checkUnits(reader.getAttrValue("units"));
        npoints = Integer.parseInt(reader.getAttrValue("size"));
        xaxisData = new double[npoints];
        if (attrList.contains("start")) {
          firstX = Double.parseDouble(reader.getAttrValue("start"));
          lastX = Double.parseDouble(reader.getAttrValue("end"));
          deltaX = (lastX - firstX) / (npoints - 1);
          increasing = deltaX > 0 ? true : false;
          continuous = true;
          for (int j = 0; j < npoints; j++)
            xaxisData[j] = firstX + (deltaX * j);
        } else {
          int posDelim = 0;
          int jj = -1;
          String tempX = "";
          Ydelim = " ";
          attrList = reader.getCharacters().replace('\n', ' ').replace('\r', ' ')
              .trim();

          // now that we have the full string should tokenise it to then process individual X values
          // for now using indexOf !!

          do {
            jj++;
            posDelim = attrList.indexOf(Ydelim);
            tempX = attrList.substring(0, posDelim);
            xaxisData[jj] = Double.parseDouble(tempX) * xFactor;
            //                   System.out.println(jj+" a "+xaxisData[jj] );
            attrList = attrList.substring(posDelim + 1, attrList.length())
                .trim();
            posDelim = attrList.indexOf(Ydelim);
            while (posDelim > 0) {
              jj++;
              tempX = attrList.substring(0, posDelim);
              xaxisData[jj] = Double.parseDouble(tempX) * xFactor;
              //                       System.out.println(jj+" b "+xaxisData[jj] );
              attrList = attrList.substring(posDelim + 1, attrList.length())
                  .trim();
              posDelim = attrList.indexOf(Ydelim);
            }
            if (jj < npoints - 1) {
              jj++;
              xaxisData[jj] = Double.parseDouble(attrList) * xFactor;
              //                     System.out.println(jj+" c "+xaxisData[jj] );
            }
          } while (jj < npoints - 1);
          firstX = xaxisData[0];
          lastX = xaxisData[npoints - 1];
          continuous = true;
        } // end of individual X values
      } // end of X array
      //          System.out.println("finished with X");
    } else if (tagName.equals("yaxis")) {
      if (attrList.contains("multipliertodata"))
        yFactor = Double.parseDouble(reader.getAttrValue("multiplierToData"));
      reader.nextTag();
      tagName = reader.getTagName();
      attrList = reader.getAttributeList();
      if (tagName.equals("array")) {
        yUnits = checkUnits(reader.getAttrValue("units"));
        Integer npointsY = Integer.valueOf(reader.getAttrValue("size"));
        if (npoints != npointsY.intValue())
          System.err.println("npoints variation between X and Y arrays");
        yaxisData = new double[npoints];
        Ydelim = reader.getAttrValue("delimeter");
        if (Ydelim.equals(""))
          Ydelim = " ";
        int posDelim = 0;
        int jj = -1;
        String tempY = "";
        attrList = reader.getCharacters().replace('\n', ' ').replace('\r', ' ').trim();

        // now that we have the full string should tokenise it to then process individual Y values
        // for now using indexOf !!

        do {
          jj++;
          posDelim = attrList.indexOf(Ydelim);
          tempY = attrList.substring(0, posDelim);
          yaxisData[jj] = Double.parseDouble(tempY) * yFactor;
          attrList = attrList.substring(posDelim + 1, attrList.length()).trim();
          posDelim = attrList.indexOf(Ydelim);
          while (posDelim > 0) {
            jj++;
            tempY = attrList.substring(0, posDelim);
            yaxisData[jj] = Double.parseDouble(tempY) * yFactor;
            attrList = attrList.substring(posDelim + 1, attrList.length())
                .trim();
            posDelim = attrList.indexOf(Ydelim);
          }
          if (jj < npoints - 1) {
            jj++;
            yaxisData[jj] = Double.parseDouble(attrList) * yFactor;
          }
        } while (jj < npoints - 1);
      }
      firstY = yaxisData[0];
      specfound = true;
    }
  }

  Vector<double[]> peakData;


  /**
   * Process the peakList CML events
   *@throws Exception
   */
  private void processPeaks() throws Exception {

    // this method is run ONCE

    // if a spectrum is found, ignore a peaklist if present as well
    // since without intervention it is not possible to guess
    // which display is required and the spectrum is probably the
    // more critical !?!

    if (specfound)
      return;

    peakData = new Vector<double[]>();

    process(CML_PEAKLIST2, true);

    // now that we have X,Y pairs set JCAMP-DX equivalencies
    // FIRSTX, FIRSTY, LASTX, NPOINTS
    // determine if the data is in increasing or decreasing order
    // since a PeakList the data is not continuous

    npoints = peakData.size();
    xaxisData = new double[npoints];
    yaxisData = new double[npoints];
    for (int i = 0; i < npoints; i++) {
      double[] xy = peakData.get(i);
      xaxisData[i] = xy[0];
      yaxisData[i] = xy[1];
    }
    peakData = null;
    firstX = xaxisData[0];
    lastX = xaxisData[npoints - 1];
    firstY = yaxisData[0];
    increasing = (lastX > firstX);
    continuous = false;

  }

  void processPeakList() {

    if (tagName.equals("peak")) {
      if (attrList.contains("xvalue")) {

        // for CML exports from NMRShiftDB there are no Y values or Y units
        // given in the Peaks, just XValues
        // to use the JCAMP-DX plot routines we assign a Yvalue
        // of 50 for every atom referenced

        double[] xy = new double[2];
        xy[1] = 50;
        xy[0] = Double.parseDouble(reader.getAttrValue("xValue"));
        if (attrList.contains("xunits"))
          xUnits = checkUnits(reader.getAttrValue("xUnits"));
        if (attrList.contains("yvalue"))
          xy[1] = Double.parseDouble(reader.getAttrValue("yValue"));
        if (attrList.contains("yunits"))
          yUnits = checkUnits(reader.getAttrValue("yUnits"));
        if (attrList.contains("atomrefs"))
          xy[1] = 49 * PT.getTokens(reader.getAttrValue("atomRefs")).length;
        peakData.add(xy);
      }
    }
  }

  private static String checkUnits(String units) {
    units = units.substring(units.indexOf(":") + 1).toUpperCase();
    return (units.equals("RELABUNDANCE") ? "RELATIVE ABUNDANCE"
        : units.contains("ARBITRARY") ? "ARBITRARY UNITS"
        : units.equals("MOVERZ") ? "M/Z"
        : units.equals("CM-1") ? "1/CM"
        : units.equals("NM") ? "NANOMETERS"
        : units);
  }

}
