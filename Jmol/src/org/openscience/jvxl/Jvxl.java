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

import javax.swing.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;

public class Jvxl extends JPanel {

  public static void main(String[] args) {

    String inputFile = "t.dx";
    String outputFile;

    Options options = new Options();
    options.addOption("h", "help", false, "give this help page");
    options.addOption("o", "output", false, "JVXL output file");
    options.addOption("c", "cutoff", false, "cutoff value");

    CommandLine line = null;
    try {
      CommandLineParser parser = new PosixParser();
      line = parser.parse(options, args);
    } catch (ParseException exception) {
      System.err.println("Unexpected exception: " + exception.toString());
    }

    if (line.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Jvxl", options);
      System.exit(0);
    }

    args = line.getArgs();
    if (args.length > 0) {
      inputFile = args[0];
    }

    // Process more command line arguments
    // these are also passed to viewer

    float cutoff = Float.NaN;

    if (line.hasOption("c")) {
      cutoff = Parser.parseFloat(line.getOptionValue("c"));
    }

    if (line.hasOption("o")) {
      outputFile = line.getOptionValue("o");
    } else {
      outputFile = inputFile;
      if (outputFile.indexOf(".") < 0)
        outputFile += ".";
      outputFile = outputFile.substring(0, outputFile.lastIndexOf("."))
          + (outputFile.indexOf(".jvxl") >= 0 ? "_new.jvxl" : ".jvxl");
    }
    Logger.info("creating " + outputFile);
    JvxlReader jvxlReader = new JvxlReader();
    if (!Float.isNaN(cutoff))
      jvxlReader.setProperty("cutoff", new Float(cutoff));
    jvxlReader.setProperty("readData", inputFile);
    writeFile(outputFile, (String) jvxlReader.getProperty("jvxlFileData", 0));
    Logger.info((String) jvxlReader.getProperty("jvxlFileInfo", 0));
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
}
