
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;
import java.util.Vector;
import javax.vecmath.Point3f;

/**
 * A class to store the "Energy Band" property for a frame <br><br>
 *
 * Usage: <br><br>
 *
 * 1) Create a new EnergyBand object:<br>
 * <code>EnergyBand energyBand = new EnergyBand(orig, origName, end, endName, nkpt, nband);</code> <br><br>
 *
 * 2) Create a new Line in k-space (usually a high symmetry line):<br>
 * <code>energyBand.addKLine(...)</code> <br><br>
 *
 * 3) Add a k point to the just created KLine (in reduce 
 *    coordinate of the line)<br>
 * <code>energyBand.addKPoint(0.1f)</code><br><br>
 *
 * 4) Add an energy to the current kline at the current k position.<br>
 * <code>energyBand.addEPoint(25.34f)</code><br><br>
 * 
 * *Repeat step 4 <code>nband</code> times. <br>
 * *Repeat step 3 <code>nkpt</code> times. <br>
 * *Repeat step 2 while there is a new line to add<br>
 */
public class EnergyBand extends PhysicalProperty {
  
  private Vector kLines = new Vector(0);  //a Vector of KLine

  private int kLineIndex=-1;
  private int kPosIndex=-1;
  private int bandIndex=-1;
  
  /**
   * Constructor for energyBand
   * @param c The electronic energy bands of a frame
   */
  public EnergyBand() {
    super("EnergyBand", null);
    System.out.println("New ENERGY Band set!!!!!!!!!!!!!!!!!!!!!!");
  }
  
  public void addKLine(Point3f orig, String origName, Point3f end,
		       String endName, int nkpt, int nband) {
    kLines.addElement(new KLine(orig, origName, end, endName, nkpt, nband));
    kLineIndex++;
    kPosIndex=-1;
  }
  
  public void addKPoint(float redpos) {
    kPosIndex++;
    ((KLine)kLines.elementAt(kLineIndex)).pos[kPosIndex] = redpos;
    bandIndex=-1;
  }

  public void addKPoint(Point3f pos3d) {
    KLine kLine = (KLine)kLines.elementAt(kLineIndex);
    float redpos = 
      (float) Math.sqrt( (double)
      ((pos3d.x-kLine.orig.x)*(pos3d.x-kLine.orig.x) +
       (pos3d.y-kLine.orig.y)*(pos3d.y-kLine.orig.y) +
       (pos3d.z-kLine.orig.z)*(pos3d.z-kLine.orig.z) )) /
      (float) Math.sqrt( (double)
      ((kLine.end.x-kLine.orig.x)*(kLine.end.x-kLine.orig.x) +
       (kLine.end.y-kLine.orig.y)*(kLine.end.y-kLine.orig.y) +
       (kLine.end.z-kLine.orig.z)*(kLine.end.z-kLine.orig.z) ));
    kPosIndex++;
    ((KLine)kLines.elementAt(kLineIndex)).pos[kPosIndex] = redpos;
    bandIndex=-1;
  }

  public void addEPoint(float energy) {
    bandIndex++;    
    ((KLine)kLines.elementAt(kLineIndex))
      .energy[kPosIndex][bandIndex] = energy;
    
  }
  
  public int getNumberOfKLines() {
    return kLines.size();
  }
  
  public KLine getKLine(int index) {
    return (KLine)kLines.elementAt(index);
  }

  public class KLine {
    
    private Point3f orig; // for example (0, 0, 0)
    private String origName; // "Gamma"
    private Point3f end;  // for example (0, 0, pi/a)
    private String endName;  // "X"
    private int nkpt;
    private int nband;

    //enrergy[k][n] is the enregy value at wave vector pos[k] and band n
    //pos[k] is the reduce coordinate along this KLine.
    private float[][] energy;
    private float[] pos;   // pos belongs to [O,1]
      
    public KLine(Point3f orig, String origName, Point3f end, String endName,
		 int nkpt, int nband) {
      this.orig = orig;
      this.origName = origName;
      this.end = end;
      this.endName = endName;
      this.nkpt = nkpt;
      this.nband = nband;
      pos = new float[nkpt];
      energy = new float[nkpt][nband];
    }

    public Point3f getOrigin() {
      return orig;
    }

    public Point3f getEnd() {
      return end;
    }
    
    public int getNumberOfkPoints() {
      return nkpt;
    }

    public int getNumberOfBands() {
      return nband;
    }

    public float[] getkPoints(){
      return pos;
    }
    
    public float getkPoint(int index) {
      return pos[index];
    }
    
    public float[] getEnergies(int index) {
      return energy[index];
    }
  }   //end KLine
  
} //end class EnergyBand
