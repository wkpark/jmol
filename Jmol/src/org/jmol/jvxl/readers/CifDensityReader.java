/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
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
package org.jmol.jvxl.readers;


import java.io.BufferedReader;
import java.util.List;
import java.util.Map;

import javajs.util.P3;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Logger;


class CifDensityReader extends MapFileReader {

  /*
   * DSN6 map file reader. 
   * 
   * http://eds.bmc.uu.se/eds/
   * 
   * Also referred to as "O" format
   * 
   * see http://www.ks.uiuc.edu/Research/vmd/plugins/doxygen/dsn6plugin_8C-source.html
   *
   */

  
  CifDensityReader(){}
  
  private Map<String, Object> cifData, thisData;
  private boolean isDiff;
  
  @Override
  void init2(SurfaceGenerator sg, BufferedReader brNull) {
    
    allowSigma = true;
    
    init2MFR(sg, br);
    Object[] o2 = (Object[]) sg.getReaderData();
    String fileName = (String) o2[0];
    // TODO  -- what about cached data -- must append "#diff=1" to that
    String data = (String) o2[1];
    isDiff = (fileName != null && fileName.indexOf("&diff=1") >= 0
        || data != null && data.indexOf("#diff=1") >= 0);
    // the initial dummy call just asertains that 
    if (fileName != null && fileName.indexOf("/0,0,0/0,0,0?") >= 0) {
      String oldName = fileName;
      P3[] box = sg.params.boundingBox;
      fileName = fileName.replace("0,0,0/0,0,0",
          box[0].x + "," + box[0].y + ","+ box[0].z + "/"
          + box[1].x + "," + box[1].y + "," + box[1].z);
      Logger.info("reading " + fileName);
      sg.setRequiredFile(oldName, fileName);
    }
    cifData = sg.atomDataServer.readCifData(fileName, data, false);
    // data are HIGH on the inside and LOW on the outside
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    nSurfaces = 1; 
  }

  private P3 readP3(String key, P3 p3) {
    if (p3 == null)
      p3 = new P3();
    float x = getFloat(key + "[0]");
    if (Float.isNaN(x)) {
      p3.x = Float.NaN;
    } else {
      p3.x = x;
      p3.y = getFloat(key + "[1]");
      p3.z = getFloat(key + "[2]");
    }
    return p3;
  }
  
  @SuppressWarnings("unchecked")
  private Map<String, Object> getMap(String type) {
    type = "data_" + type;
    List<Object> list = (List<Object>) cifData.get("models");
    for (int i = 0; i < list.size(); i++) {
      Map<String, Object> map = (Map<String, Object>) list.get(i);
      if (type.equalsIgnoreCase(map.get("name").toString()))
        return thisData = map;
    }
    return null;
  }
  private float getFloat(String key) {
    Object o = thisData.get(key);
    float x = Float.NaN;
    if (o != null) {
      if (o instanceof String) {
        x = PT.parseFloat((String) o);
      } else if (o instanceof Number) {
        x = ((Number) o).floatValue();
      }
    }
    return x;
  }

  private String getString(String key) {
    Object o = thisData.get(key);
    return (o == null ? null : o.toString());
  }
  
  private float[] values;

  //  $ print getProperty("cifInfo","http://www.ebi.ac.uk/pdbe/densities/x-ray/1cbs/box/0.1,0.1,0.1/0.23,0.31,0.18?space=fractional&encoding=cif").models[2]
  //      {
  //        "_volume_data_3d_info_axis_order[0]"  :  "1"
  //        "_volume_data_3d_info_axis_order[1]"  :  "0"
  //        "_volume_data_3d_info_axis_order[2]"  :  "2"
  //        "_volume_data_3d_info_dimensions[0]"  :  "0.2125"
  //        "_volume_data_3d_info_dimensions[1]"  :  "0.144737"
  //        "_volume_data_3d_info_dimensions[2]"  :  "0.083333"
  //        "_volume_data_3d_info_max_sampled"  :  "3.762699"
  //        "_volume_data_3d_info_max_source"  :  "3.762699"
  //        "_volume_data_3d_info_mean_sampled"  :  "0"
  //        "_volume_data_3d_info_mean_source"  :  "0"
  //        "_volume_data_3d_info_min_sampled"  :  "-1.298991"
  //        "_volume_data_3d_info_min_source"  :  "-1.298991"
  //        "_volume_data_3d_info_name"  :  "2Fo-Fc"
  //        "_volume_data_3d_info_origin[0]"  :  "0.1"
  //        "_volume_data_3d_info_origin[1]"  :  "0.092105"
  //        "_volume_data_3d_info_origin[2]"  :  "0.098485"
  //        "_volume_data_3d_info_sample_count[0]"  :  "17"
  //        "_volume_data_3d_info_sample_count[1]"  :  "11"
  //        "_volume_data_3d_info_sample_count[2]"  :  "11"
  //        "_volume_data_3d_info_sample_rate"  :  "1"
  //        "_volume_data_3d_info_sigma_sampled"  :  "0.354201"
  //        "_volume_data_3d_info_sigma_source"  :  "0.354201"
  //        "_volume_data_3d_info_spacegroup_cell_angles[0]"  :  "90"
  //        "_volume_data_3d_info_spacegroup_cell_angles[1]"  :  "90"
  //        "_volume_data_3d_info_spacegroup_cell_angles[2]"  :  "90"
  //        "_volume_data_3d_info_spacegroup_cell_size[0]"  :  "45.65"
  //        "_volume_data_3d_info_spacegroup_cell_size[1]"  :  "47.56"
  //        "_volume_data_3d_info_spacegroup_cell_size[2]"  :  "77.61"
  //        "_volume_data_3d_info_spacegroup_number"  :  "19"
  //        "_volume_data_3d_values"  :
  //        [
  //          0.186828
  //          0.11986

  @Override
  protected void readParameters() throws Exception {

    //    getMap("SERVER");
    //    String type = getString("_density_server_result.query_type");
    //    if (!"box".equals(type))
    //      return;
    //    P3 origin = readP3("_density_server_result.query_box_a", null);
    //    readP3("_density_server_result.query_box_b", p3);
    getMap(isDiff ? "FO-FC" : "2FO-FC");
    boolean isFractional = "fractional"
        .equals(getString("_density_server_result_query_box_type"));

    readP3("_volume_data_3d_info_axis_order", p3);

    //    _volume_data_3d_info.axis_order[0]                1 
    //    _volume_data_3d_info.axis_order[1]                0 
    //    _volume_data_3d_info.axis_order[2]                2 

    P3 axis_order = readP3("_volume_data_3d_info_axis_order", null);

    //    _volume_data_3d_info.origin[0]                    0.6 
    //    _volume_data_3d_info.origin[1]                    0.5 
    //    _volume_data_3d_info.origin[2]                    0.69697

    P3 fracOrigin = readP3("_volume_data_3d_info_origin", null);

    //    _volume_data_3d_info.dimensions[0]                0.4 
    //    _volume_data_3d_info.dimensions[1]                0.5 
    //    _volume_data_3d_info.dimensions[2]                0.30303

    P3 fracDimensions = readP3("_volume_data_3d_info_dimensions", null);

    //    _volume_data_3d_info.sample_count[0]              32 
    //    _volume_data_3d_info.sample_count[1]              38 
    //    _volume_data_3d_info.sample_count[2]              40

    P3 sampleCounts = readP3("_volume_data_3d_info_sample_count", p3);

    mapc = (int) axis_order.x + 1; // fastest "column"  2 --> y
    mapr = (int) axis_order.y + 1; // intermediat "row" 1 --> x
    maps = (int) axis_order.z + 1; // slowest "section" 3 --> z

    // Jmol will run through these z slowest, then y next, then x.

    // TODO check for inversion of inside/outside due to switching x/y axes
    
    
    // these counts are for the dimensions of the data, not the dimensions of the axes

    n0 = (int) sampleCounts.x;
    n1 = (int) sampleCounts.y;
    n2 = (int) sampleCounts.z
        ;
    // these counts are for the dimensions of the axes

    na = (int) getXYZ(sampleCounts, mapc - 1);
    nb = (int) getXYZ(sampleCounts, mapr - 1);
    nc = (int) getXYZ(sampleCounts, maps - 1);

    //  _volume_data_3d_info.spacegroup_cell_size[0]      45.65 
    //  _volume_data_3d_info.spacegroup_cell_size[1]      47.56 
    //  _volume_data_3d_info.spacegroup_cell_size[2]      77.61 

    readP3("_volume_data_3d_info_spacegroup_cell_size", p3);

    a = p3.x;
    b = p3.y;
    c = p3.z;

    float fa = getXYZ(fracDimensions, mapc - 1);
    float fb = getXYZ(fracDimensions, mapr - 1);
    float fc = getXYZ(fracDimensions, maps - 1);

    // fraction is in terms of a and in the units of na
    
    xyzStart[xIndex = 0] = getXYZ(fracOrigin, mapc - 1) * na / fa;
    xyzStart[yIndex = 1] = getXYZ(fracOrigin, mapr - 1) * nb / fb;
    xyzStart[zIndex = 2] = getXYZ(fracOrigin, maps - 1) * nc / fc;

    a *= fa;
    b *= fb;
    c *= fc;
    
    // our "axes" may be shorter than the original ones,
    // but they will be in the same directions


    //  _volume_data_3d_info.spacegroup_cell_angles[0]    90 
    //  _volume_data_3d_info.spacegroup_cell_angles[1]    90 
    //  _volume_data_3d_info.spacegroup_cell_angles[2]    90 

    readP3("_volume_data_3d_info_spacegroup_cell_angles", p3);
    alpha = p3.x;
    beta = p3.y;
    gamma = p3.z;

    values = readFloats("_volume_data_3d_values", new float[na * nb * nc]);

    //  _volume_data_3d_info.spacegroup_number            19   
    //    _volume_data_3d_info.sample_rate                  1 

    getVectorsAndOrigin();
    
    if (params.thePlane == null && (params.cutoffAutomatic || !Float.isNaN(params.sigma))) {
      float sigma = (params.sigma < 0 || Float.isNaN(params.sigma) ? 1 : params.sigma);
      dmean = getFloat("_volume_data_3d_info_mean_source");
      float rmsDeviation = getFloat("_volume_data_3d_info_sigma_source");
      params.cutoff = rmsDeviation * sigma + dmean;
      Logger.info("Cutoff set to (mean + rmsDeviation*" + sigma + " = " + params.cutoff + ")\n");
    }


    //setCutoffAutomatic();

    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("CifDensity reader\n");
    jvxlFileHeaderBuffer
        .append("see http://www.ebi.ac.uk/pdbe/densities/x-ray/1cbs/dbox/\n");

  }
  
  @SuppressWarnings("unchecked")
  private float[] readFloats(String key, float[] values) {
    List<Object> list = (List<Object>) thisData.get(key);
    for (int i = 0, n = values.length; i < n; i++)
      values[i] = PT.parseFloat((String) list.get(i));
    return values;
  }

  private float getXYZ(P3 a, float x) {
    switch ((int) x) {
    case 0:
      return a.x;
    case 1:
      return a.y;
    case 2:
    default:
      return a.z;
    }
  }

  private int pt;

    
  @Override
  protected float nextVoxel() throws Exception {
    //System.out.println("pt " + pt + " " + values[pt]);
    return values[pt++];
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
    pt += nPoints;
  }
}



//data_SERVER
//#
//_density_server_result.server_version     0.9.4 
//_density_server_result.datetime_utc       '2018-01-11 13:05:27' 
//_density_server_result.guid               6b9aaa27-a562-4e00-a888-6f500847868f 
//_density_server_result.is_empty           no 
//_density_server_result.has_error          no 
//_density_server_result.error              . 
//_density_server_result.query_source_id    emd/emd-8003 
//_density_server_result.query_type         box 
//_density_server_result.query_box_type     cartesian 
//_density_server_result.query_box_a[0]     -2 
//_density_server_result.query_box_a[1]     7 
//_density_server_result.query_box_a[2]     10 
//_density_server_result.query_box_b[0]     4 
//_density_server_result.query_box_b[1]     10 
//_density_server_result.query_box_b[2]     15.5 
//#
//data_EM
//#
//_volume_data_3d_info.name                         em 
//_volume_data_3d_info.axis_order[0]                0 
//_volume_data_3d_info.axis_order[1]                1 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0 
//_volume_data_3d_info.origin[1]                    0.015625 
//_volume_data_3d_info.origin[2]                    0.021875 
//_volume_data_3d_info.dimensions[0]                0.009375 
//_volume_data_3d_info.dimensions[1]                0.009375 
//_volume_data_3d_info.dimensions[2]                0.015625 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              3 
//_volume_data_3d_info.sample_count[1]              3 
//_volume_data_3d_info.sample_count[2]              5 
//_volume_data_3d_info.spacegroup_number            1 
//_volume_data_3d_info.spacegroup_cell_size[0]      443.2 
//_volume_data_3d_info.spacegroup_cell_size[1]      443.2 
//_volume_data_3d_info.spacegroup_cell_size[2]      443.2 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0.000239 
//_volume_data_3d_info.mean_sampled                 0.000239 
//_volume_data_3d_info.sigma_source                 0.015579 
//_volume_data_3d_info.sigma_sampled                0.015579 
//_volume_data_3d_info.min_source                   -0.121007 
//_volume_data_3d_info.min_sampled                  -0.121007 
//_volume_data_3d_info.max_source                   0.287133 
//_volume_data_3d_info.max_sampled                  0.287133 
//#
//loop_
//_volume_data_3d.values
//0.000639 
//0.000667 
//0.000444 
//data_SERVER
//#
//_density_server_result.server_version     0.9.4 
//_density_server_result.datetime_utc       '2018-01-11 13:49:16' 
//_density_server_result.guid               d7da5dff-3343-4082-a96f-95fd979d8567 
//_density_server_result.is_empty           no 
//_density_server_result.has_error          no 
//_density_server_result.error              . 
//_density_server_result.query_source_id    x-ray/1cbs 
//_density_server_result.query_type         box 
//_density_server_result.query_box_type     fractional 
//_density_server_result.query_box_a[0]     0.1 
//_density_server_result.query_box_a[1]     0.1 
//_density_server_result.query_box_a[2]     0.1 
//_density_server_result.query_box_b[0]     0.23 
//_density_server_result.query_box_b[1]     0.31 
//_density_server_result.query_box_b[2]     0.18 
//#
//data_2FO-FC
//#
//_volume_data_3d_info.name                         2Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.354201 
//_volume_data_3d_info.sigma_sampled                0.354201 
//_volume_data_3d_info.min_source                   -1.298991 
//_volume_data_3d_info.min_sampled                  -1.298991 
//_volume_data_3d_info.max_source                   3.762699 
//_volume_data_3d_info.max_sampled                  3.762699 
//#
//loop_
//_volume_data_3d.values
//0.186828 
//0.11986 
//-0.015395 
//...
//0.117123 
//0.078306 
//-0.026217 
//-0.135757 
//#
//data_FO-FC
//#
//_volume_data_3d_info.name                         Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.123854 
//_volume_data_3d_info.sigma_sampled                0.123854 
//_volume_data_3d_info.min_source                   -0.688616 
//_volume_data_3d_info.min_sampled                  -0.688616 
//_volume_data_3d_info.max_source                   0.846369 
//_volume_data_3d_info.max_sampled                  0.846369 
//#
//loop_
//_volume_data_3d.values
//0.170378 
//0.08947 
//-0.047389 
//-0.107031 
//...
//0.236658 
//0.068594 
//-0.079848 
//#


//data_SERVER
//#
//_density_server_result.server_version     0.9.4 
//_density_server_result.datetime_utc       '2018-01-11 13:49:16' 
//_density_server_result.guid               d7da5dff-3343-4082-a96f-95fd979d8567 
//_density_server_result.is_empty           no 
//_density_server_result.has_error          no 
//_density_server_result.error              . 
//_density_server_result.query_source_id    x-ray/1cbs 
//_density_server_result.query_type         box 
//_density_server_result.query_box_type     fractional 
//_density_server_result.query_box_a[0]     0.1 
//_density_server_result.query_box_a[1]     0.1 
//_density_server_result.query_box_a[2]     0.1 
//_density_server_result.query_box_b[0]     0.23 
//_density_server_result.query_box_b[1]     0.31 
//_density_server_result.query_box_b[2]     0.18 
//#
//data_2FO-FC
//#
//_volume_data_3d_info.name                         2Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.354201 
//_volume_data_3d_info.sigma_sampled                0.354201 
//_volume_data_3d_info.min_source                   -1.298991 
//_volume_data_3d_info.min_sampled                  -1.298991 
//_volume_data_3d_info.max_source                   3.762699 
//_volume_data_3d_info.max_sampled                  3.762699 
//#
//loop_
//_volume_data_3d.values
//0.186828 
//0.11986 
//-0.015395 
//...
//0.117123 
//0.078306 
//-0.026217 
//-0.135757 
//#
//data_FO-FC
//#
//_volume_data_3d_info.name                         Fo-Fc 
//_volume_data_3d_info.axis_order[0]                1 
//_volume_data_3d_info.axis_order[1]                0 
//_volume_data_3d_info.axis_order[2]                2 
//_volume_data_3d_info.origin[0]                    0.1 
//_volume_data_3d_info.origin[1]                    0.092105 
//_volume_data_3d_info.origin[2]                    0.098485 
//_volume_data_3d_info.dimensions[0]                0.2125 
//_volume_data_3d_info.dimensions[1]                0.144737 
//_volume_data_3d_info.dimensions[2]                0.083333 
//_volume_data_3d_info.sample_rate                  1 
//_volume_data_3d_info.sample_count[0]              17 
//_volume_data_3d_info.sample_count[1]              11 
//_volume_data_3d_info.sample_count[2]              11 
//_volume_data_3d_info.spacegroup_number            19 
//_volume_data_3d_info.spacegroup_cell_size[0]      45.65 
//_volume_data_3d_info.spacegroup_cell_size[1]      47.56 
//_volume_data_3d_info.spacegroup_cell_size[2]      77.61 
//_volume_data_3d_info.spacegroup_cell_angles[0]    90 
//_volume_data_3d_info.spacegroup_cell_angles[1]    90 
//_volume_data_3d_info.spacegroup_cell_angles[2]    90 
//_volume_data_3d_info.mean_source                  0 
//_volume_data_3d_info.mean_sampled                 0 
//_volume_data_3d_info.sigma_source                 0.123854 
//_volume_data_3d_info.sigma_sampled                0.123854 
//_volume_data_3d_info.min_source                   -0.688616 
//_volume_data_3d_info.min_sampled                  -0.688616 
//_volume_data_3d_info.max_source                   0.846369 
//_volume_data_3d_info.max_sampled                  0.846369 
//#
//loop_
//_volume_data_3d.values
//0.170378 
//0.08947 
//-0.047389 
//-0.107031 
//...
//0.236658 
//0.068594 
//-0.079848 
//#

