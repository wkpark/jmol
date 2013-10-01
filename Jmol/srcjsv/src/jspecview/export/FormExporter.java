/* Copyright (c) 2002-2008 The University of the West Indies
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.export;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import jspecview.util.JSVFileManager;
import jspecview.util.JSVLogger;

/**
 * The XMLExporter should be a totally generic exporter
 *
 * no longer uses Velocity.
 *
 * @author Bob Hanson, hansonr@stolaf.edu
 *
 */
abstract class FormExporter {

  FormContext context = new FormContext();
  String errMsg;
  String outputFile;
  FileWriter writer;
  Calendar now;
  SimpleDateFormat formatter;
  String currentTime;

  protected void initForm(String fileName) {
    outputFile = fileName;
    Calendar now = Calendar.getInstance();
    SimpleDateFormat formatter = new SimpleDateFormat(
        "yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    currentTime = formatter.format(now.getTime());
  }

  protected String writeForm(String templateFile) throws IOException {
    String[] error = new String[1];
    String template = JSVFileManager.getResourceString(this, templateFile, error);
    if (template == null) {
      JSVLogger.error(error[0]);
      return error[0];
    }
    errMsg = context.setTemplate(template);
    if (errMsg != null) {
      JSVLogger.error(errMsg);
      return errMsg;
    }

    FileWriter writer = (outputFile == null ? null : new FileWriter(outputFile));

    errMsg = context.merge(writer);
    if (writer == null)
      return errMsg;
    if (errMsg != null) {
      JSVLogger.error(errMsg);
      throw new IOException(errMsg);
    }

    try {
      writer.flush();
      writer.close();
    } catch (IOException ioe) {
    }

    return " OK";
  }
}
