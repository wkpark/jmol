.SUFFIXES: $(SUFFIXES) .jar .java .class
           JMOL_HOME=/export/home/gezelter/gezelter/java/jmol
           JAVA_HOME=/usr/local/jdk1.2.2
#          JAVA_HOME=/usr/java
          SWING_HOME=${JMOL_HOME}/jars
               JAVAC= $(JAVA_HOME)/bin/javac
                JAVA= $(JAVA_HOME)/bin/java
             JAVADOC= $(JAVA_HOME)/bin/javadoc
               JIKES= /usr/local/bin/jikes
       JAVA_COMPILER= $(JIKES)
        THREADS_FLAG=native
# Java 2 classpath:
           CLASSPATH=${JAVA_HOME}/jre/lib/rt.jar:${JMOL_HOME}/src:${JMOL_HOME}/jars/sax.jar:${JMOL_HOME}/jars/aelfred.jar:${JMOL_HOME}/jars/cml.jar:${JMOL_HOME}/jars/Acme.jar
#
# Java 1.1 classpath:
#           CLASSPATH=${JAVA_HOME}/lib/classes.zip:${JMOL_HOME}/src:${JAVA_HOME}/lib/rt.jar:${JMOL_HOME}/jars/swing.jar:${JMOL_HOME}/jars/multi.jar:${JMOL_HOME}/jars/sax.jar:${JMOL_HOME}/jars/aelfred.jar:${JMOL_HOME}/jars/cml.jar:${JMOL_HOME}/jars/Acme.jar
#
# put Jmol.java first, so that compilation is fast
#
SOURCE = org/openscience/jmol/Jmol.java \
        org/openscience/jmol/ADFReader.java \
        org/openscience/jmol/AboutDialog.java \
        org/openscience/jmol/Aces2Reader.java \
        org/openscience/jmol/Angle.java \
        org/openscience/jmol/Animate.java \
        org/openscience/jmol/ArrowLine.java \
        org/openscience/jmol/AtomPropsMenu.java \
        org/openscience/jmol/AtomType.java \
        org/openscience/jmol/AtomTypeTable.java \
        org/openscience/jmol/AtomTypesModel.java \
        org/openscience/jmol/Bond.java \
        org/openscience/jmol/CMLFile.java \
        org/openscience/jmol/CMLSaver.java \
        org/openscience/jmol/Charge.java \
        org/openscience/jmol/ChemFile.java \
        org/openscience/jmol/ChemFileReader.java \
        org/openscience/jmol/ChemFrame.java \
        org/openscience/jmol/ColorCellEditor.java \
        org/openscience/jmol/DecimalNumberField.java \
        org/openscience/jmol/Dihedral.java \
        org/openscience/jmol/Distance.java \
        org/openscience/jmol/FileSaver.java \
        org/openscience/jmol/FileTyper.java \
        org/openscience/jmol/FortranFormat.java \
        org/openscience/jmol/GamessReader.java \
        org/openscience/jmol/Gaussian92Reader.java \
        org/openscience/jmol/Gaussian94Reader.java \
        org/openscience/jmol/Gaussian98Reader.java \
        org/openscience/jmol/GaussianFile.java \
        org/openscience/jmol/HelpDialog.java \
        org/openscience/jmol/ImageTyper.java \
        org/openscience/jmol/JmolException.java \
        org/openscience/jmol/JmolResourceHandler.java \
        org/openscience/jmol/JpegEncoder.java \
        org/openscience/jmol/Matrix3D.java \
        org/openscience/jmol/Measure.java \
        org/openscience/jmol/MeasurementList.java \
        org/openscience/jmol/MultiLineHeaderRenderer.java \
        org/openscience/jmol/NMRShielding.java \
        org/openscience/jmol/PDBReader.java \
        org/openscience/jmol/PhysicalProperty.java \
        org/openscience/jmol/Preferences.java \
        org/openscience/jmol/ReaderFactory.java \
        org/openscience/jmol/Splash.java \
        org/openscience/jmol/StatusBar.java \
        org/openscience/jmol/TableMap.java \
        org/openscience/jmol/TableSorter.java \
        org/openscience/jmol/VProperty.java \
        org/openscience/jmol/Vibrate.java \
        org/openscience/jmol/Vibration.java \
        org/openscience/jmol/WhatsNewDialog.java \
        org/openscience/jmol/WholeNumberField.java \
        org/openscience/jmol/XYZReader.java \
        org/openscience/jmol/XYZSaver.java \
        org/openscience/jmol/displayPanel.java \
        org/openscience/jmol/poshTableSorter.java
#
# Put Jmol.class first
#
CLASSES = org/openscience/jmol/Jmol.class \
        org/openscience/jmol/ADFReader.class \
        org/openscience/jmol/AboutDialog.class \
        org/openscience/jmol/Aces2Reader.class \
        org/openscience/jmol/Angle.class \
        org/openscience/jmol/Animate.class \
        org/openscience/jmol/ArrowLine.class \
        org/openscience/jmol/AtomPropsMenu.class \
        org/openscience/jmol/AtomType.class \
        org/openscience/jmol/AtomTypeTable.class \
        org/openscience/jmol/AtomTypesModel.class \
        org/openscience/jmol/Bond.class \
        org/openscience/jmol/CMLFile.class \
        org/openscience/jmol/CMLSaver.class \
        org/openscience/jmol/Charge.class \
        org/openscience/jmol/ChemFile.class \
        org/openscience/jmol/ChemFileReader.class \
        org/openscience/jmol/ChemFrame.class \
        org/openscience/jmol/DecimalNumberField.class \
        org/openscience/jmol/Dihedral.class \
        org/openscience/jmol/Distance.class \
        org/openscience/jmol/FileSaver.class \
        org/openscience/jmol/FileTyper.class \
        org/openscience/jmol/FortranFormat.class \
        org/openscience/jmol/GamessReader.class \
        org/openscience/jmol/Gaussian92Reader.class \
        org/openscience/jmol/Gaussian94Reader.class \
        org/openscience/jmol/Gaussian98Reader.class \
        org/openscience/jmol/GaussianFile.class \
        org/openscience/jmol/HelpDialog.class \
        org/openscience/jmol/ImageTyper.class \
        org/openscience/jmol/JmolException.class \
        org/openscience/jmol/JmolResourceHandler.class \
        org/openscience/jmol/JpegEncoder.class \
        org/openscience/jmol/Matrix3D.class \
        org/openscience/jmol/Measure.class \
        org/openscience/jmol/MeasurementList.class \
        org/openscience/jmol/MultiLineHeaderRenderer.class \
        org/openscience/jmol/NMRShielding.class \
        org/openscience/jmol/PDBReader.class \
        org/openscience/jmol/PhysicalProperty.class \
        org/openscience/jmol/Preferences.class \
        org/openscience/jmol/ReaderFactory.class \
        org/openscience/jmol/Splash.class \
        org/openscience/jmol/StatusBar.class \
        org/openscience/jmol/TableMap.class \
        org/openscience/jmol/TableSorter.class \
        org/openscience/jmol/VProperty.class \
        org/openscience/jmol/Vibrate.class \
        org/openscience/jmol/Vibration.class \
        org/openscience/jmol/WhatsNewDialog.class \
        org/openscience/jmol/WholeNumberField.class \
        org/openscience/jmol/XYZReader.class \
        org/openscience/jmol/XYZSaver.class \
        org/openscience/jmol/displayPanel.class \
        org/openscience/jmol/poshTableSorter.class
# 
# Some rulesets for compilation
#
# standard java files
#
org/openscience/jmol/%.class: org/openscience/jmol/%.java
	@echo "compiling $*"
	$(JAVA_COMPILER) -classpath $(CLASSPATH) -O -deprecation $<

all: archive

archive: $(CLASSES)
	/bin/rm -f ../jars/jmol.jar
	$(JAVA_HOME)/bin/jar -cvf ../jars/jmol.jar org/openscience/jmol/*.class  org/openscience/jmol/images org/openscience/jmol/Properties org/openscience/jmol/Data
	chmod 664 ../jars/jmol.jar
	chgrp jmol ../jars/jmol.jar

# suggested by Christoph Steinbeck:
run: $(CLASSES)
	$(JAVA) -classpath $(CLASSPATH) -Djmol.home=$(JMOL_HOME) org.openscience.jmol.Jmol

# suggested by Hugo Garcia:
doc::
	$(JAVADOC) -classpath $(CLASSPATH) -d doc org/openscience/jmol/*.java

clean::
	find . -name '*~' -exec $(RM) {} \;
	find . -name '*.class' -exec $(RM) {} \;
	/bin/rm -rf doc/*
