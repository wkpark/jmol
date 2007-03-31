/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:09:49 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7221 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jvxl;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

//import javax.swing.*;
import javax.vecmath.Point4f;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;

public class Jvxl {

  public static void main(String[] args) {

    boolean blockData = false;
    int fileIndex = Integer.MAX_VALUE;
    String inputFile = "t.dx";
    String mapFile = null;
    String outputFile = null;

    float cutoff = Float.NaN;
    boolean isPositiveOnly = false;

    String phase = null;
    Point4f plane = null;

    String colorScheme = null;
    boolean bicolor = false;
    boolean reverseColor = false;
    float min = Float.NaN;
    float max = Float.NaN;

    Options options = new Options();
    options.addOption("h", "help", false, "give this help page");

    /*
     *  examples: 
     *  
     *  jvxl ch3cl-density.cub --min=0.0 --max=0.2 --map ch3cl-esp.cub
     *  jvxl ethene-HOMO.cub --bicolor --output ethene.jvxl
     *  jvxl d_orbitals.jvxl --index 2 --phase yz
     *  jvxl d_orbitals.jvxl --map sets
     */

    // file options
    options.addOption("B", "blockdata", false,
        "multiple cube data are in blocks, not interspersed");

    OptionBuilder.withLongOpt("file");
    OptionBuilder.withDescription("file containing surface data");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("f"));

    OptionBuilder.withLongOpt("index");
    OptionBuilder.withDescription("index of surface in file (starting with 1)");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("i"));

    OptionBuilder.withLongOpt("plane");
    OptionBuilder
        .withDescription("plane: x, y, z, xy, xz, yz, z2, x2-y2, or {a,b,c,d}");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("p"));

    OptionBuilder.withLongOpt("map");
    OptionBuilder
        .withDescription("file containing data to map onto the surface or \"sets\"");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("m"));

    OptionBuilder.withLongOpt("output");
    OptionBuilder.withDescription("JVXL output file");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("o"));

    // surface options

    OptionBuilder.withLongOpt("cutoff");
    OptionBuilder.withDescription("isosurface cutoff value");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("c"));

    // color mapping options

    options.addOption("b", "bicolor", false, "bicolor map (orbital)");
    options.addOption("r", "reversecolor", false, "reverse color");

    OptionBuilder.withLongOpt("colorScheme");
    OptionBuilder
        .withDescription("VMRL color scheme: roygb, bgyor, rwb, bwr, low, high");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("s"));

    OptionBuilder.withLongOpt("phase");
    OptionBuilder
        .withDescription("color by phase: x, y, z, xy, xz, yz, z2, x2-y2");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("P"));

    OptionBuilder.withLongOpt("min");
    OptionBuilder.withDescription("color absolute minimum value");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("n"));

    OptionBuilder.withLongOpt("max");
    OptionBuilder.withDescription("color absolute maximum value");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("x"));

    CommandLine line = null;
    try {
      CommandLineParser parser = new PosixParser();
      line = parser.parse(options, args);
    } catch (ParseException exception) {
      Logger.error("Unexpected exception: " + exception.toString());
    }

    if (line.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Jvxl", options);
      return;
    }

    args = line.getArgs();
    if (args.length > 0) {
      inputFile = args[0];
    }

    // files 

    blockData = (line.hasOption("B"));

    if (line.hasOption("i")) {
      fileIndex = Parser.parseInt(line.getOptionValue("i"));
    }

    if (line.hasOption("f")) {
      inputFile = line.getOptionValue("f");
    }

    if (line.hasOption("m")) {
      mapFile = line.getOptionValue("m");
    }

    if (line.hasOption("p")) {
      plane = getPlane(line.getOptionValue("p"));
      if (plane == null) {
        Logger.error("invalid plane");
        return;
      }
      Logger.info("using plane " + plane);
      if (mapFile == null)
        mapFile = inputFile;
    }

    if (line.hasOption("o")) {
      outputFile = line.getOptionValue("o");
    } else {
      outputFile = inputFile;
      if (outputFile.indexOf(".") < 0)
        outputFile += ".";
      String sIndex = (fileIndex == Integer.MAX_VALUE ? "" : "_" + fileIndex);
      if (sIndex.length() == 0 && outputFile.indexOf(".jvxl") >= 0)
        sIndex += "_new";
      outputFile = outputFile.substring(0, outputFile.lastIndexOf("."))
          + sIndex + ".jvxl";
    }

    // Process more command line arguments
    // these are also passed to viewer

    bicolor = (line.hasOption("b"));
    reverseColor = (line.hasOption("r"));

    if (bicolor && mapFile != null) {
      Logger.warn("--map option ignored; incompatible with --bicolor");
      mapFile = null;
    }

    if (line.hasOption("c")) {
      String s = line.getOptionValue("c");
      if (s.indexOf("+") == 0) {
        isPositiveOnly = true;
        s = s.substring(1);
      }
      cutoff = Parser.parseFloat(s);
    }

    if (line.hasOption("n")) {
      if (bicolor)
        Logger.warn("--min option ignored; incompatible with --bicolor");
      else
        min = Parser.parseFloat(line.getOptionValue("n"));
    }

    if (line.hasOption("x")) {
      if (bicolor)
        Logger.warn("--max option ignored; incompatible with --bicolor");
      else
        max = Parser.parseFloat(line.getOptionValue("x"));
    }

    if (line.hasOption("P")) {
      phase = line.getOptionValue("p");
    }

    // compose the surface

    ColorEncoder colorEncoder = new ColorEncoder();
    JvxlReader jvxlReader = new JvxlReader(colorEncoder);

    // input file

    if (blockData)
      jvxlReader.setProperty("blockData", Boolean.TRUE);
    if (!Float.isNaN(cutoff))
      jvxlReader.setProperty("cutoff" + (isPositiveOnly ? "Positive" : ""),
          new Float(cutoff));
    if (bicolor)
      jvxlReader.setProperty("sign", null);
    if (reverseColor)
      jvxlReader.setProperty("reverseColor", null);
    if (phase != null)
      jvxlReader.setProperty("phase", phase);

    if (plane != null)
      jvxlReader.setProperty("plane", plane);
    else {
      if (fileIndex != Integer.MAX_VALUE)
        jvxlReader.setProperty("fileIndex", new Integer(fileIndex));
      jvxlReader.setProperty("readData", inputFile);
    }

    //color scheme is only for VMRL

    if (colorScheme != null)
      jvxlReader.setProperty("colorScheme", colorScheme);
    if (!Float.isNaN(min))
      jvxlReader.setProperty("red", new Float(min));
    if (!Float.isNaN(max))
      jvxlReader.setProperty("blue", new Float(max));
    if (mapFile != null)
      jvxlReader.setProperty("mapColor", mapFile);

    writeFile(outputFile, (String) jvxlReader.getProperty("jvxlFileData", 0));
    Logger.info((String) jvxlReader.getProperty("jvxlFileInfo", 0));

    Logger.info("\ncreated " + outputFile);
  }

  static void writeFile(String fileName, String text) {
    try {
      FileOutputStream os = new FileOutputStream(fileName);
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os), 8192);
      bw.write(text);
      bw.close();
      os = null;
    } catch (IOException e) {
      Logger.error("IO Exception: " + e.toString());
    }
  }
  
  static Point4f getPlane(String str) {
    if (str.equalsIgnoreCase("xy"))
      return new Point4f(0, 0, 1, 0);
    if (str.equalsIgnoreCase("xz"))
      return new Point4f(0, 1, 0, 0);
    if (str.equalsIgnoreCase("yz"))
      return new Point4f(1, 0, 0, 0);
    if (str.indexOf("x=") == 0) {
      return new Point4f(1, 0, 0, -Parser.parseFloat(str.substring(2)));
    }
    if (str.indexOf("y=") == 0) {
      return new Point4f(0, 1, 0, -Parser.parseFloat(str.substring(2)));
    }
    if (str.indexOf("z=") == 0) {
      return new Point4f(0, 0, 1, -Parser.parseFloat(str.substring(2)));
    }
    if (str.indexOf("{") == 0) {
      str = TextFormat.simpleReplace(str, ",", " ");
      int[] next = new int[1];
      return new Point4f(Parser.parseFloat(str, next), Parser.parseFloat(str,
          next), Parser.parseFloat(str, next), Parser.parseFloat(str, next));
    }
    return null;
  }
}
