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

/**
 * Implements a queue for a bash-like command history.
 */
final public class CommandHistory {

  /**
   * Array of commands.
   */
  private String[] commandList = null;

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
    cursorPos--;
    if (cursorPos < 0) {
      cursorPos = commandList.length - 1;
      while ((cursorPos > 0) && (commandList[cursorPos] == null)) {
        cursorPos--;
      }
    }
    return getCommand();
  }

  /**
   * Retrieves the following command from the top of the list, updates list position.
   * 
   * @return the String value of a command.
   */
  public String getCommandDown() {
    cursorPos++;
    if (cursorPos >= commandList.length) {
      cursorPos = 0;
    }
    if (commandList[cursorPos] == null) {
      cursorPos = 0;
    }
    return getCommand();
  }

  /**
   * Calculates the command to return.
   * 
   * @return the String value of a command.
   */
  private String getCommand() {
    return commandList[cursorPos];
  }

  /**
   * Adds a new command to the bottom of the list, resets list position.
   * 
   * @param command the String value of a command.
   */
  public void addCommand(String command) {
    if (nextCommand >= commandList.length) {
      nextCommand = 0;
    }
    commandList[nextCommand] = command;
    nextCommand++;
    if (nextCommand >= commandList.length) {
      nextCommand = 0;
    }
    cursorPos = nextCommand;
    commandList[nextCommand] = "";
  }

  /**
   * Resets maximum size of command queue. Cuts off extra commands.
   * 
   * @param maxSize maximum size for the command queue.
   */
  void setMaxSize(int maxSize) {
    if (maxSize + 1 == commandList.length) {
      return;
    }
    String[] tmpCommandList = new String[maxSize + 1];
    int lastCommand = commandList.length - 1;
    while ((lastCommand > 0) && (commandList[lastCommand] == null)) {
      lastCommand--;
    }
    if (tmpCommandList.length - 1 > lastCommand) {
      for (int i = 0; i < lastCommand; i++) {
        tmpCommandList[i] = commandList[i];
      }
      return;
    }
    for (int i = 0; i < tmpCommandList.length; i++) {
      tmpCommandList[tmpCommandList.length - i - 1] = commandList[commandList.length - i - 1];
    }
    int delta = 0;
    if (nextCommand >= tmpCommandList.length) {
      delta = nextCommand - tmpCommandList.length + 1;
    }
    for (int i = 0; i <= nextCommand - delta; i++) {
      tmpCommandList[nextCommand - delta - i] = commandList[nextCommand - i];
    }
    commandList = tmpCommandList;
  }

  /**
   * Resets instance.
   * 
   * @param maxSize maximum size for the command queue.
   */
  void reset(int maxSize) {
    commandList = new String[maxSize + 1];
    nextCommand = 0;
    commandList[nextCommand] = "";
    cursorPos = 0;
  }
}
