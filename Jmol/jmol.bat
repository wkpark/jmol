@echo off
 rem Set JMOL_HOME and JAVA_HOME to match the appropriate installation directories on your system.
 rem
 set JMOL_HOME=c:\java\jmol-0.6.1
 rem set JAVA_HOME=c:\java\jdk1.1.8
 set JAVA_HOME=c:\java\jdk1.2.2
 rem
 echo on
 rem
 rem Use this line if you have jdk1.1.x, rem it out otherwise:
 rem
 rem %JAVA_HOME%\bin\java -classpath %JMOL_HOME%\jars\jmol.jar;%JAVA_HOME%\lib\classes.zip;%JMOL_HOME%\jars\swing.jar;%JMOL_HOME%\jars\multi.jar;%JMOL_HOME%\jars\sax.jar;%JMOL_HOME%\jars\aelfred.jar;%JMOL_HOME\jars\cml.jar;%JMOL_HOME%\jars\Acme.jar;%JMOL_HOME%\jars\jas.jar -Djmol.home=%JMOL_HOME% org.openscience.jmol.Jmol
 rem
 rem Use this line if you have jdk1.2.x, rem it out otherwise:
 rem
 %JAVA_HOME%\bin\java -classpath %JMOL_HOME%\jars\jmol.jar;%JMOL_HOME%\jars\sax.jar;%JMOL_HOME%\jars\aelfred.jar;%JMOL_HOME%\jars\cml.jar;%JMOL_HOME%\jars\Acme.jar;%JMOL_HOME%\jars\plot.jar -Djmol.home=%JMOL_HOME% org.openscience.jmol.Jmol
