@echo off
rem Set JMOL_HOME to the Jmol installation directory.
rem
if "%JMOL_HOME%"=="" set JMOL_HOME=.

set libDir=%JMOL_HOME%\jars
java -Djmol.home="%JMOL_HOME%" -cp "%libDir%\jmol.jar" org.openscience.jmol.Jmol %1 %2 %3 %4 %5 %6 %7 %8 %9
