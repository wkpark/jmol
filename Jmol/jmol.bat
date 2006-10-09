@echo off
rem Set JMOL_HOME to the Jmol installation directory.
rem
set JMOL_HOME=.
if "%JMOL_HOME%x"=="x" set JMOL_HOME=.\build
if "%JMOL_HOME%x"==".x" set JMOL_HOME=.\build
java -Xmx512m -jar "%JMOL_HOME%\Jmol.jar" %1 %2 %3 %4 %5 %6 %7 %8 %9
