package org.jmol.util;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.script.Token;

public class ContactPair {
  public float[] radii = new float[2];
  public float[] vdws = new float[2];
  public Atom[] myAtoms = new Atom[2];
  public Point3f pt;
  public double volume = 0;
  public float score;
  public float d;
  public float chord;
  public int contactType;
  public float xVdwClash = Float.NaN;

  public ContactPair(Atom[] atoms, int i1, int i2, float R, float r, float vdwA, float vdwB) {
    radii[0] = R;
    radii[1] = r;
    vdws[0] = vdwA;
    vdws[1] = vdwB;
    myAtoms[0] = atoms[i1];
    myAtoms[1] = atoms[i2];
    

    //     ------d------------
    //    i1--------|->vdw1
    //        vdw2<-|--------i2
    //              pt

    Vector3f v = new Vector3f(myAtoms[1]);
    v.sub(myAtoms[0]);
    d = v.length();
    
    // find center of asymmetric lens
    //NOT float f = (vdw1*vdw1 - vdw2*vdw2 + dAB*dAB) / (2 * dAB*dAB);
    // as that would be for truly planar section, but it is not quite planar

    float f = (R - r + d) / (2 * d);
    pt = new Point3f();
    pt.scaleAdd(f, v, myAtoms[0]);

    // http://mathworld.wolfram.com/Sphere-SphereIntersection.html
    //  volume = pi * (R + r - d)^2 (d^2 + 2dr - 3r^2 + 2dR + 6rR - 3R^2)/(12d)

    score = d - vdwA - vdwB;
    contactType = (score < 0 ? Token.clash : Token.vanderwaals);
    if (score < 0) {
      xVdwClash = getVdwClashRadius(R - vdwA, vdwA, vdwB, d); 
      radii[0] = R = vdwA;
      radii[1] = r = vdwB;
    }
    volume = (R + r - d);
    volume *= Math.PI * volume
        * (d * d + 2 * d * r - 3 * r * r + 2 * d * R + 6 * r * R - 3 * R * R)
        / 12 / d;

    // chord check:
    double a = (d * d - r * r + R * R);
    chord = (float) Math.sqrt(4 * d * d * R * R - a * a) / d;
  }
  
  public boolean setForVdwClash(boolean toVdw) {
    if (Float.isNaN(xVdwClash))
      return false;
    if (toVdw) {
    radii[0] = vdws[0] + xVdwClash;
    radii[1] = vdws[1] + xVdwClash;
    contactType = Token.vanderwaals;
    } else {
      radii[0] = vdws[0];
      radii[1] = vdws[1];
      contactType = Token.clash;
    }
    return true;
    
  }
  
  /**
   * 
   * well, heh, heh... This calculates the VDW extension x at a given distance
   * for a clashing pair that will produce a volume that is equivalent to the
   * volume for the vdw contact at the point of touching (d0 = vdwA + vdwB) and
   * the transition to clash. This will provide the surface that will surround
   * the clash until the clash size is larger than it.
   * 
   * @param x0
   * @param vdwA
   * @param vdwB
   * @param d
   * @return new probe radius
   */
  private static float getVdwClashRadius(double x0, double vdwA, double vdwB,
                                         double d) {

    /// Volume = pi/12 * (r + R - d)^2 * (d + 2(r + R) - 3(r-R)^2/d)
    /// for +vdw +x: pi/12 * (va + vb - d + 2x)^2 * (d + 2(va + vb) + 4x - 3(va-vb)^2/d)
    
    double sum = vdwA + vdwB;
    double dif2 = vdwA - vdwB;
    dif2 *= dif2;
    double v0_nopi = x0 * x0 * (sum + 4.0/3 * x0 - dif2 / sum);
    //System.out.println("v0 = " + Math.PI * v0_nopi + " v0_nopi =" + v0_nopi);
    
    /// (a + x)^2(b + 2x) = c; where x here is probe DIAMETER

    double a = (sum - d);
    double b = d + 2 * sum - 3 * dif2 / d;
    double c = v0_nopi * 12;

    
    /* from Sage:
     * 
    
a = var('a')
b = var('b')
c = var('c')
x = var('x')
eqn = (a + x)^2 * (b + 2 * x) == c
solve(eqn, x)

[

x == -1/72*(-I*sqrt(3) + 1)*(4*a^2 - 4*a*b + b^2)/(1/27*a^3 -
1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b +
6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 1/2*(I*sqrt(3) +
1)*(1/27*a^3 - 1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 -
12*a^2*b + 6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 2/3*a -
1/6*b, 

x == -1/72*(I*sqrt(3) + 1)*(4*a^2 - 4*a*b + b^2)/(1/27*a^3 -
1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b +
6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 1/2*(-I*sqrt(3) +
1)*(1/27*a^3 - 1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 -
12*a^2*b + 6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) - 2/3*a -
1/6*b, 

x == -2/3*a - 1/6*b + 1/36*(4*a^2 - 4*a*b + b^2)/(1/27*a^3 -
1/18*a^2*b + 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b +
6*a*b^2 - b^3 + 27*c)*c)*sqrt(3) + 1/4*c)^(1/3) + (1/27*a^3 - 1/18*a^2*b
+ 1/36*a*b^2 - 1/216*b^3 + 1/36*sqrt((8*a^3 - 12*a^2*b + 6*a*b^2 - b^3 +
27*c)*c)*sqrt(3) + 1/4*c)^(1/3)

]

*/
    
/* so...

x1 == f - g*(1/2-I*sqrt(3)/2)/h^(1/3) - (1/2+I*sqrt(3)/2)*h^(1/3)
x2 == f - g*(1/2+I*sqrt(3)/2)/h^(1/3) - (1/2-I*sqrt(3)/2)*h^(1/3)
x3 == f + g/h^(1/3) + h^(1/3)

where

f = -2/3*a - 1/6*b
g = (4*a^2 - 4*a*b + b^2)/36 
h = a^3/27 - a^2*b/18 + a*b^2/36 - b^3/216 + c/4 
     + sqrt(c/432*(8*a^3 - 12*a^2*b + 6*a*b^2 - b^3 + 27*c))

The problem is, that sqrt is imaginary, so the cube root is as well. 

v = a^3/27 - a^2*b/18 + a*b^2/36 - b^3/216 + c/4
u = -c/432*(8*a^3 - 12*a^2*b + 6*a*b^2 - b^3 + 27*c)

*/

    double a2 = a * a;
    double a3 = a * a2;
    double b2 = b * b;
    double b3 = b * b2;

    double f = -a * 2/3 - b/6;
    double g = (4*a2 - 4*a*b + b2)/36;
    double v = a3/27 - a2*b/18 + a*b2/36 - b3/216 + c/4;
    double u = -c/432*(8*a3 - 12*a2*b + 6*a*b2 - b3 + 27*c);
    
    
/*
Then 

h = v + sqrt(u)*I

and we can express h^1/3 as 

vvu (cos theta + i sin theta)

where

vvu = (v^2 + u)^(1/6)
theta = atan2(sqrt(u),v)/3

Now, we know this is a real solution, so we take the real part of that.
The third root seems to be our root (thankfully!)

x3 == f + g/h^(1/3) + h^(1/3)
    = f + (2*g/vvu + vvu) costheta

     */
    

    double theta = Math.atan2(Math.sqrt(u), v);
    
    double vvu = Math.pow(v*v + u, 1.0/6.0);
    double costheta = Math.cos(theta/3);
    
    // x == f + g/h^(1/3) + h^(1/3) = f + g/vvu + vvu)*costheta

    //System.out.println ("a = " + a + ";b = " + b + ";c = " + c + ";f = " + f + ";g = " + g + "");

    double x;
    
    x = f + (g/vvu + vvu) * costheta;
    System.out.println(d + "\t" + x + "\t" + ((a + x)*(a + x) * (b + 2 * x)) + " = " + c);
    return (x > 0 ? (float) (x / 2) : Float.NaN);
  }

  public void switchAtoms() {
    Atom atom = myAtoms[0];
    myAtoms[0] = myAtoms[1];
    myAtoms[1] = atom;
    float r = radii[0];
    radii[0] = radii[1];
    radii[1] = r;
    r = vdws[0];
    vdws[0] = vdws[1];
    vdws[1] = r;
  }
  
  @Override
  public String toString() {
    return "type=" + Token.nameOf(contactType) + " " + myAtoms[0] + " " + myAtoms[1] + " dAB=" + d + " score=" +  score + " chord=" + chord + " volume=" + volume;
  }

}
