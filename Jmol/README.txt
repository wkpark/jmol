Instructions for the Impatient:

1) Unpack it.  (Which you have probably already done if you are
   reading this file...)
 
2) Edit jmol or jmol.bat to reflect the locations where the 
   Java VM and the Jmol directory live on your machine.

3) Run it.

4) Send any problems/bug reports to gezelter@openscience.org

---------------------------------------------------------------------------
Frequently Asked Questions:

1) What is Jmol?

Jmol is an open-source molecule viewer and editor. It was originally
intended to be a fully functional replacement for XMol, but along the
way, we've tried to improve some parts of the XMol interface that we
found annoying.  Jmol is written in Java using the Swing GUI
components. After we duplicate the functionality of XMol, we will be
adding features of particular interest to the chemical community,
including molecule editing, simple force-field based minimizations,
molecular dynamics (microcanonical ensemble), and Langevin dynamics
(canonical ensemble).

2) Why do we need another Molecule Viewer?

There are really three reasons:

  1. Animation. We do chemical dynamics, and we need to see what our
     simulations look like at different times. We weren't aware of any
     molecule viewers out there (besides XMol) that do animations.

  2. The XMol source code is not freely available, and the number of
     machines it runs on is extremely limited (the free binary version does
     not even run under IRIX 6.x). From our reading of the XMol FAQ, the
     authors of XMol are not amenable to making the source available, and
     they do not even sell the binary version as a product. XMol is also
     restricted to a UNIX/X environment, and Jmol works anywhere that you
     can get a Java 1.1 VM to run.

  3. Normal modes.  Jmol animates normal modes computed via a number 
     of other ab initio software packages.

3) How far along is Jmol?

Jmol is beta software.  Many things work: it can be used as an
animator of multi-frame XYZ and CML files, it can animate the 
precalculated normal modes from many ab initio quantum chemistry
programs (Gaussian, GAMESS, ACES2, ADF) it can read PDB files and a
wide range of ab initio quantum chemistry log files, and it can 
output GIFs, JPGs and PPMs of what is displayed on screen.  
PostScript output is of very high quality if you are using Java2.  It
can measure distances, angles, and dihedrals for any frame in 
the animation, and it is a generally useful tool for the chemical 
sciences.

4) Is Jmol an Applet? Can it be run from inside a web browser?

No. Jmol is a Java-based application. It reads and writes files on the
local disk which contain information about atomic coordinates. Applets
are not permitted to write local files, so we made Jmol an application
instead.  An applet version of Jmol is under development and should
be available for general release when Jmol reaches version 0.7.

5) What do I need to run Jmol?

At a minimum, you need a Java1.1 VM.  We like Sun's JDK1.2.2, but 
Jmol should work with other VMs.  Macintosh users should have MRJ 2.2 or 
higher.  Linux users can get information on the latest Linux JVM at 
the Blackdown (www.blackdown.org) site. If (like me) you use 
FreeBSD, you should go to the JDK for FreeBSD page (www.freebsd.org/java).

6) What do I need to compile Jmol?

A Java2 (java1.2) JDK.  We've also been using Jikes, the very fast
java compiler from the IBM's AlphaWorks site to compile Jmol, but you
don't necessarily need to have it.

7) Where can I get Jmol?

The canonical distribution site for Jmol is:

    http://www.openscience.org/jmol

We also have an anonymous read-only CVS account that you can use to
obtain the latest (sometimes broken) version of Jmol:

    cvs -d :pserver:anoncvs@openscience.org:/Jmol login
    cvs -d :pserver:anoncvs@openscience.org:/Jmol checkout Jmol

8) Who is working on Jmol?

Many people, but the coordinator and primary developer is Dan Gezelter
(gezelter@openscience.org).  Some of the original rotation code is
based on a sample atoms-only MoleculeViewer written by James Gosling.
We're also using some icons from Dean S. Jones, GIF and PPM encoders
from Jef Poskanzer, and the JPG encoder is from James Weeks.  The
Gaussian log file importer was written by Chris Steinbeck.  The
CML file parser was written by E.L. Willighagen.  The normal mode
animation code (and many of the file readers) came from Bradley A. Smith.
Anyone who wants to submit code to the Jmol project is welcome to 
contribute. Send email to Dan Gezelter for more details.
