==============================================================================
===                                  Jmol                                  ===
==============================================================================


Jmol is an open-source molecule viewer and editor written in Java.

Please check out http://www.jmol.org/

Usage questions/comments should be posted to jmol-users@lists.sourceforge.net

Development questions/suggestions/comments should be posted
to jmol-developers@lists.sf.net


Files list:
-----------

- README.txt
		This file.

- COPYRIGHT.txt
		Copyright informations.

- LICENSE.txt
		GNU LGPL (terms of licence for use and distribution).
		
- jmol
		(Some kind of batch file)
	
- jmol.bat
		A batch file to start Jmol application under Windows.
		
- jmol.mac
		(Some kind of batch file)
		
- jmol.sh
		A shell script to start Jmol application under Unix-like systems, 
		like Linux/BSD/Solaris and Cygwin for Windows.

- Jmol.jar
		The application executable file (a program written in Java). 
		This can be run as any other program: opens in its own window, can be 
		resized or minimized, admits drag-and-drop, has a top menu, 
		can open and save files, etc. It can be open from the command line 
		(particulary, using the shell or batch files described below), 
		but if Java is properly configured in your system, it's usually 
		enough to double-click on the file.

- JmolApplet.jar
		The applet, i.e. a version of the program that will only run 
		when embedded in a web page. 
		This is an all-in-one file, kept mainly for compatibility with old pages 
		that call it explicitly. Current recommended procedure is to use the 
		split version (JmolApplet0.jar etc.). In particular, Jmol.js uses 
		the split version.
		You may wish to use this if you want to keep your website simple or you 
		just want to upload a single jar file whenever new versions are released. 
		However, this will load Jmol somewhat slower than the split versions 
		(described below), as all the modules must get loaded onto a user's 
		machine before any structure is displayed.
		To invoke JmolApplet.jar from Jmol.js, either:
		a) just put it in the directory containing the HTML page requiring it and 
			do not use jmolInitialize(), or
		b) identify it explicitly in jmolInitialize(), for example:
			jmolInitialize("directory-containing-jar-files", "JmolApplet.jar")

- JmolAppletSigned.jar
		An equivalent version of the applet, but this is a 
		"signed" applet (a term in Java security language). This means it 
		must be authorized by the web page visitor for it to run, but then it 
		will have less security restrictions for file access. For example, it 
		can access files on any part of the user's hard disk or from any other 
		web server.
		Typically users get a message asking if they want to accept the "certificate" 
		or if they "trust" the applet, but this security feature is not always 
		enabled. JmolAppletSigned.jar should be used with this in mind. 
		Other than reading files, Jmol does not currently utilize other capabilities 
		of signed applets, such as accessing the System clipboard or writing files. 
		Use only if you know what you are doing and have considered the security issues.
		To invoke JmolAppletSigned.jar from Jmol.js, use:
			jmolInitialize("directory-containing-jar-files", "JmolAppletSigned.jar")
			                  
- Jmol.js
		The library, written in JavaScript language, that assists in the 
		programming of web pages that use Jmol applet, without the need to know 
		and write detailed JmolApplet code.
		This library uses by default the split version of the applet 
		(unsigned or signed).
		Fully documented at http://jmol.org/jslibrary/ 

- JmolApplet0.jar  and
  JmolApplet0(severalNamesHere).jar
		The applet is divided up into several pieces according to their function, 
		so that if a page does not require a component, that component is 
		not downloaded from the server. It is still recommended that you put 
		all JmolApplet0*.jar files on your server even if your page does not use 
		the capabilities provided by some of the files, because the pop-up menu 
		and Jmol console both allow users to access parts of Jmol you might 
		not have considered.
		The set of these files is equivalent to the single JmolApplet.jar.
		This split version is the one that will be used by default if you use 
		Jmol.js. For that, use the simplest form of jmolInitialize(), just 
		indicating the directory containing the set of jar files:
			jmolInitialize("directory-containing-jar-files")
		for example,
			jmolInitialize(".")  // jar files are in the same folder as the web page 
			jmolInitialize("../jmol") // jar files are in a parallel folder, named 'jmol'.
  
- JmolAppletSigned0.jar  and
  JmolAppletSigned0(severalNamesHere).jar
		The signed version of the new split applet. This version allows the user 
		to access files anywhere on a hard drive and from any location on the web. 
		Typically users get a message asking if they want to accept the certificate 
		for **each** of the (currently 16) loadable files. For this reason, this 
		version may not be of general use.
		To use this with Jmol.js, use the form:
			jmolInitialize("directory-containing-jar-files", true)
		or
			jmolInitialize("directory-containing-jar-files", "JmolAppletSigned0.jar")

---------------------------
Given the descriptions, you will realize that the distribution package contains 
4 full copies of the applet (signed or unsigned, split or not).
---------------------------
