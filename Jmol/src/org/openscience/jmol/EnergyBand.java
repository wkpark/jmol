/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2001-2003  The Jmol Development Team
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
package org.openscience.jmol;
import java.util.Vector;
import javax.vecmath.Point3d;

/**
 * A class to store the "Energy Band" property for a frame.
 *
 * <p>Usage: <br><br>
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
  
  private int energyUnits;
  private double maxE=0;
  private double minE=0;
  private double fermiE=0;
  private Vector kLines = new Vector(0);  //a Vector of KLine

  private int kLineIndex=-1;
  private int kPosIndex=-1;
  private int bandIndex=-1;
  
  /**
   * Constructor for energyBand
   * @param c The electronic energy bands of a frame
   */
  public EnergyBand(int energyUnits) {
    super("EnergyBand", null);
    this.energyUnits = energyUnits;
    System.out.println("New ENERGY Band set!!!!!!!!!!!!!!!!!!!!!!");
  }
  
  public void addKLine(Point3d orig, String origName, Point3d end,
		       String endName, int nkpt, int nband) {
    kLines.addElement(new KLine(orig, origName, end, endName, nkpt, nband));
    kLineIndex++;
    kPosIndex=-1;
  }
  
  public void addKPoint(double redpos) {
    kPosIndex++;
    ((KLine)kLines.elementAt(kLineIndex)).pos[kPosIndex] = redpos;
    bandIndex=-1;
  }

  public void addKPoint(Point3d pos3d) {
    KLine kLine = (KLine)kLines.elementAt(kLineIndex);
    double redpos = 
      Math.sqrt( 
      ((pos3d.x-kLine.orig.x)*(pos3d.x-kLine.orig.x) +
       (pos3d.y-kLine.orig.y)*(pos3d.y-kLine.orig.y) +
       (pos3d.z-kLine.orig.z)*(pos3d.z-kLine.orig.z) )) /
      Math.sqrt( 
      ((kLine.end.x-kLine.orig.x)*(kLine.end.x-kLine.orig.x) +
       (kLine.end.y-kLine.orig.y)*(kLine.end.y-kLine.orig.y) +
       (kLine.end.z-kLine.orig.z)*(kLine.end.z-kLine.orig.z) ));
    kPosIndex++;
    ((KLine)kLines.elementAt(kLineIndex)).pos[kPosIndex] = redpos;
    bandIndex=-1;
  }

  public void addEPoint(double energy) {
    bandIndex++;    
    ((KLine)kLines.elementAt(kLineIndex))
      .energy[kPosIndex][bandIndex] = energy;
    
    if(energy > maxE) {
      maxE = energy;
    } else if (energy < minE) {
      minE = energy;
    }

  }
  
  public int getNumberOfKLines() {
    return kLines.size();
  }
  
  public KLine getKLine(int index) {
    return (KLine)kLines.elementAt(index);
  }

  public int getEnergyUnits() {
    return energyUnits;
  }

  public double getMinE() {
    return minE;
  }

  public double getMaxE() {
    return maxE;
  }
  
  public double getFermiE() {
    return fermiE;
  }
  
  public void setFermiE(double fermiE) {
    this.fermiE = fermiE;
  }

  public class KLine {
    
    private Point3d orig; // for example (0, 0, 0)
    private String origName; // "Gamma"
    private Point3d end;  // for example (0, 0, pi/a)
    private String endName;  // "X"
    private int nkpt;
    private int nband;
    
    //enrergy[k][n] is the enregy value at wave vector pos[k] and band n
    //pos[k] is the reduce coordinate along this KLine.
    private double[][] energy;
    private double[] pos;   // pos belongs to [O,1]
      
    public KLine(Point3d orig, String origName, Point3d end, String endName,
		 int nkpt, int nband) {
      this.orig = orig;
      this.origName = origName;
      this.end = end;
      this.endName = endName;
      this.nkpt = nkpt;
      this.nband = nband;
      pos = new double[nkpt];
      energy = new double[nkpt][nband];
    }

    public Point3d getOrigin() {
      return orig;
    }
    
    public String getOriginName() {
      return origName;
    }

    public void setOriginName(String origName) {
      this.origName = origName;
    }

    public Point3d getEnd() {
      return end;
    }
    
    public String getEndName() {
      return endName;
    }
    
    public void setEndName(String endName) {
      this.endName = endName;
    }
    
    public int getNumberOfkPoints() {
      return nkpt;
    }
    
    public int getNumberOfBands() {
      return nband;
    }

    public double[] getkPoints(){
      return pos;
    }
    
    /**
     * Get the reduce coordinate of the k-point along the segment line.
     */

    public double getkPoint(int index) {
      return pos[index];
    }
    
    public double[] getEnergies(int index) {
      return energy[index];
    }

    public double getDistance(int indexa, int indexb) {
      return orig.distance(end) * Math.abs(pos[indexa] - pos[indexb]); 
    }
    
  }   //end KLine
  
  
} //end class EnergyBand
