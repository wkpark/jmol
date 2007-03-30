/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.util;

import java.util.Vector;
/**
 * Implements a queue for a bash-like command history.
 */
final public class CommandHistory {

  
  public final static String ERROR_FLAG = "#??";
  public final static String NOHISTORYLINE_FLAG = "#--";
  public final static String NOHISTORYATALL_FLAG = "#----";
  final static int DEFAULT_MAX_SIZE = 100;
  
  /**
   * Array of commands.
   */
  private Vector commandList = null;
  private int maxSize = DEFAULT_MAX_SIZE;

  /**
   * Position of the next command.
   */
  private int nextCommand;

  /**
   * Current position of the cursor;
   */
  private int cursorPos;


  /**
   * Creates a new instance using the default size (100) 
   *
   */
  public CommandHistory() {
    reset(DEFAULT_MAX_SIZE);
  }
  
  /**
   * Creates a new instance.
   * 
   * @param maxSize maximum size for the command queue
   */
  public CommandHistory(int maxSize) {
    reset(maxSize);
  }

  /**
   * clears the history.
   * 
   */
  public void clear() {
    reset(maxSize);
  }

  /**
   * Resets instance.
   * 
   * @param maxSize maximum size for the command queue.
   */
  public void reset(int maxSize) {
    this.maxSize = maxSize; 
    commandList = new Vector();
    nextCommand = 0;
    commandList.add("");
    cursorPos = 0;
  }

  /**
   * Resets maximum size of command queue. Cuts off extra commands.
   * 
   * @param maxSize maximum size for the command queue.
   */
  public void setMaxSize(int maxSize) {
    if (maxSize == this.maxSize)
      return;
    if (maxSize < 2)
      maxSize = 2;
    while (nextCommand > maxSize) {
      commandList.remove(0);
      nextCommand--;
    }
    if (nextCommand > maxSize)
      nextCommand= maxSize - 1;
    cursorPos = nextCommand;
    this.maxSize = maxSize;
  }

  /**
   * Retrieves the following command from the bottom of the list, updates list position.
   * 
   * @return the String value of a command.
   */
  public String getCommandUp() {
    if (cursorPos <= 0)
      return null;
    cursorPos--;
    String str = getCommand();
    if (str.endsWith(ERROR_FLAG))
      removeCommand(cursorPos--);
    if (cursorPos < 0)
      cursorPos = 0;
    return str;
  }

  /**
   * Retrieves the following command from the top of the list, updates list position.
   * 
   * @return the String value of a command.
   */
  public String getCommandDown() {
    if (cursorPos >= nextCommand)
      return null;
    cursorPos++;
    
    return getCommand();
  }

  /**
   * Calculates the command to return.
   * 
   * @return the String value of a command.
   */
  private String getCommand() {
    return (String)commandList.get(cursorPos);
  }

  /**
   * Adds any number of lines to the command history
   * @param strCommand
   */
  public void addCommand(String strCommand) {
    if (!isOn && !strCommand.endsWith(ERROR_FLAG))
      return;
    if (strCommand.endsWith(NOHISTORYATALL_FLAG))
      return;
    int i;
    
    // I don't think Jmol can deliver a multiline parameter here
    while ((i = strCommand.indexOf("\n")) >= 0) {
      String str = strCommand.substring(0, i);
      if (str.length() > 0)
        addCommandLine(str);
      strCommand = strCommand.substring(i + 1);
    }
    if (strCommand.length() > 0)
      addCommandLine(strCommand);
  }

  boolean isOn = true;

  /**
   * Options include:
   *   all                             Integer.MAX_VALUE
   *   n prev                          n >= 1
   *   next                           -1
   *   set max to -2 - n               n <= -3
   *   just clear                     -2
   *   clear and turn off; return ""   0
   *   clear and turn on; return ""    Integer.MIN_VALUE;  
   * @param n
   * @return one or more lines of command history
   */
  public String getSetHistory(int n) {
    isOn = (n == -2 ? isOn : true);
    switch (n) {
    case 0:
      isOn = false;
      clear();
      return "";
    case Integer.MIN_VALUE:
    case -2:
      clear();
      return "";
    case -1:
      return getCommandUp();
    case 1:
      return getCommandDown();
    default:
      if (n < 0) {
        setMaxSize(-2 - n);
        return "";
      }
      n = Math.max(nextCommand - n, 0);
    }
    String str = "";
    for (int i = n; i < nextCommand; i++)
      str += commandList.get(i) + "\n";
    return str;
  }

  public String removeCommand() {
    return removeCommand(nextCommand - 1);
  }

  public String removeCommand(int n) {
    if (n < 0 || n >= nextCommand)
      return "";
    String str = (String) commandList.get(n);
    commandList.remove(n);
    nextCommand--;
    return str; 
  }
  
  /**
   * Adds a single line to the bottom of the list, resets list position.
   * 
   * @param command the String value of a command.
   */
  private void addCommandLine(String command) {
    if(command == null || command.length() == 0)
      return;
    if (command.endsWith(NOHISTORYLINE_FLAG))
      return;
    if (nextCommand >= maxSize) {
      commandList.remove(0);
      nextCommand = maxSize - 1;
    }
    commandList.add(nextCommand, command);
    nextCommand++;
    cursorPos = nextCommand;
    commandList.add(nextCommand, "");
    //for (int i = 0; i < nextCommand; i++)
      //System.out.println("HISTORY:" + i+" "+commandList.get(i));
  }
  
}
