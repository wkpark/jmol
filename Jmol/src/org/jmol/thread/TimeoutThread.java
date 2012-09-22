/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.thread;

import java.util.Iterator;
import java.util.Map;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class TimeoutThread extends JmolThread {
  public String script;
  private int ms;
  private long targetTime;
  private int status;
  private boolean triggered = true;
  private Viewer viewer;
  
  public TimeoutThread(Viewer viewer, String name, int ms, String script) {
    this.viewer = viewer;
    setMyName(name);
    set(ms, script);
  }
  
  public void set(int ms, String script) {
    this.ms = ms;
    targetTime = System.currentTimeMillis() + Math.abs(ms);
    if (script != null)
      this.script = script; 
  }

  public void trigger() {
    triggered = (ms < 0);
  }
  
  @Override
  public String toString() {
    return "timeout name=" + name + " executions=" + status + " mSec=" + ms 
    + " secRemaining=" + (targetTime - System.currentTimeMillis())/1000f + " script=" + script + " thread=" + Thread.currentThread().getName();      
  }
  
  @Override
  public void run() {
    if (script == null || script.length() == 0 || ms == 0)
      return;
    //if (true || Logger.debugging) 
    //Logger.info(toString());
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    try {
      Map<String, Object> timeouts = viewer.getTimeouts();
      while (true) {
        Thread.sleep(26);
        if (targetTime > System.currentTimeMillis())
          continue;
        status++;
        boolean looping = (ms < 0);
        targetTime += Math.abs(ms);
        if (timeouts.get(name) == null)
          break;
        if (!looping)
          timeouts.remove(name);
        if (triggered) {
          triggered = false;
          viewer.evalStringQuiet((looping ? script + ";\ntimeout ID \"" + name + "\";" : script));
        } else {
        }
        if (!looping)
          break;
      }
    } catch (InterruptedException ie) {
      //Logger.info("Timeout " + this + " interrupted");
    } catch (Exception ie) {
      Logger.info("Timeout " + name + " Exception: " + ie);
    }
    viewer.getTimeouts().remove(name);
  }

  public static void clear(Map<String, Object> timeouts) {
    Iterator<Object> e = timeouts.values().iterator();
    while (e.hasNext()) {
      TimeoutThread t = (TimeoutThread) e.next();
      if (!t.script.equals("exitJmol"))
        t.interrupt();
    }
    timeouts.clear();
  }

  public static void setTimeout(Viewer viewer, Map<String, Object> timeouts, String name, int mSec, String script) {
    TimeoutThread t = (TimeoutThread) timeouts.get(name);
    if (mSec == 0) {
      if (t != null) {
        t.interrupt();
        timeouts.remove(name);
      }
      return;
    }
    if (t != null) {
      t.set(mSec, script);
      return;
    }
    t = new TimeoutThread(viewer, name, mSec, script);
    timeouts.put(name, t);
    t.start();
  }

  public static void trigger(Map<String, Object> timeouts, String name) {
    TimeoutThread t = (TimeoutThread) timeouts.get(name);
    if (t != null)
      t.trigger();
  }

  public static String showTimeout(Map<String, Object> timeouts, String name) {
    StringBuffer sb = new StringBuffer();
    if (timeouts != null) {
      Iterator<Object> e = timeouts.values().iterator();
      while (e.hasNext()) {
        TimeoutThread t = (TimeoutThread) e.next();
        if (name == null || t.name.equalsIgnoreCase(name))
          sb.append(t.toString()).append("\n");
      }
    }
    return (sb.length() > 0 ? sb.toString() : "<no timeouts set>");
  }
}