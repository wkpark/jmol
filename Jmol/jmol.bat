@echo off
rem Set JMOL_HOME to the Jmol installation directory.
rem
if "%JMOL_HOME%"=="" set JMOL_HOME=.
java -jar "%JMOL_HOME%\jmol.jar" %1 %2 %3 %4 %5 %6 %7 %8 %9
