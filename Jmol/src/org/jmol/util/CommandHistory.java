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

  /**
   * Array of commands.
   */
  private Vector commandList = null;
  private int maxSize;

  /**
   * Position of the next command.
   */
  private int nextCommand;

  /**
   * Current position of the cursor;
   */
  private int cursorPos;

  /**
   * Creates a new instance.
   * 
   * @param maxSize maximum size for the command queue
   */
  public CommandHistory(int maxSize) {
    reset(maxSize);
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
    return getCommand();
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
   * Adds a new command to the bottom of the list, resets list position.
   * 
   * @param command the String value of a command.
   */
  public void addCommand(String command) {
    if(command == null || command.length() == 0)
      return;
    if (nextCommand >= maxSize) {
      commandList.remove(0);
      nextCommand = maxSize - 1;
    }
    commandList.add(nextCommand, command);
    nextCommand++;
    cursorPos = nextCommand;
    commandList.add(nextCommand, "");
  }

  /**
   * Resets maximum size of command queue. Cuts off extra commands.
   * 
   * @param maxSize maximum size for the command queue.
   */
  void setMaxSize(int maxSize) {
    if (maxSize == this.maxSize)
      return;
    while (this.maxSize > maxSize) {
      commandList.remove(0);
      this.maxSize--;
      nextCommand = cursorPos = this.maxSize;
    }
    this.maxSize = maxSize;
  }

  /**
   * Resets instance.
   * 
   * @param maxSize maximum size for the command queue.
   */
  void reset(int maxSize) {
    this.maxSize = maxSize; 
    commandList = new Vector();
    nextCommand = 0;
    commandList.add("");
    cursorPos = 0;
  }
}
