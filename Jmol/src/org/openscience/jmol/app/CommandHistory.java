/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

/*
 * Creat el 28/07/2003
 *
 */
package org.openscience.jmol.app;

import java.util.*;

/**
 * Implements a queue for a bash-like command history.
 * 
 * 
 * @author Agust� S�nchez Furrasola
 * @version $Revision$
 *
 */
final class CommandHistory
{
   private LinkedList commandList = new LinkedList();

   private int maxSize;

   private int pos = 0;

   /**
    * Creates a new instance.
    * 
    * @param maxSize maximum size for the command queue
    */
   CommandHistory(int maxSize)
   {
      this.maxSize = maxSize;
   }

   /**
    * Retrieves the following command from the bottom of the list, updates
    * list position.
    * @return the String value of a command
    */
   String getCommandUp()
   {
      pos--;
      return getCommand();
   }

   /**
    * Retrieves the following command from the top of the list, updates 
    * list position.
    * @return the String value of a command
    */
   String getCommandDown()
   {
      pos++;
      return getCommand();
   }

   /**
    * Calculates the command to return.
    *  
    * @return the String value of a command
    */
   private String getCommand()
   {
      if (pos == 0)
      {
         return "";
      }
      else
      {
         int size = commandList.size();

         if (size > 0)
         {
            if (pos > 0)
            {
               return (String) commandList.get((pos - 1) % size);
            }
            else
            {
               return (String) commandList.get(
                  size - (Math.abs(1 + pos)) % size - 1);
            }
         }
         else
         {
            return "";
         }
      }
   }

   /**
    * Adds a new command to the bottom of the list, resets
    * list position.
    * @param the String value of a command
    */
   void addCommand(String command)
   {
       //      System.out.println(command);

      pos = 0;

      commandList.addLast(command);

      if (commandList.size() > maxSize)
      {
         commandList.removeFirst();
      }
   }

   /**
    * Resets maximum size of command queue. Cuts off extra commands.
    * 
    * @param maxSize maximum size for the command queue
    */
   void setMaxSize(int maxSize)
   {
      this.maxSize = maxSize;

      while (maxSize < commandList.size())
      {
         commandList.removeFirst();
      }
   }
   
   /**
    * Resets instance.
    * 
    * @param maxSize maximum size for the command queue
    */
   void reset(int maxSize)
   {
      this.maxSize = maxSize;
      commandList = new LinkedList();
   }

   public static void main(String[] args) throws Exception
   {
      CommandHistory h = new CommandHistory(4);

      h.addCommand("a");
      h.addCommand("b");
      h.addCommand("c");
      h.addCommand("d");

      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());

      System.out.println("******");
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      
      
      h.setMaxSize(2);


      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());
      System.out.println(h.getCommandUp());

      System.out.println("******");
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
      System.out.println(h.getCommandDown());
   }

}
