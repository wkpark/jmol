.SUFFIXES: $(SUFFIXES) .jar .java .class
           JMOL_HOME=$(PWD)/..
#          JAVA_HOME=/usr/local/jdk1.2.2
#          JAVA_HOME=/usr/java
#          SWING_HOME=${JMOL_HOME}/jars
               JAVAC= javac
                JAVA= java
             JAVADOC= javadoc -private
       JAVA_COMPILER= $(JAVAC)
        THREADS_FLAG=native
# Java 2 classpath:
           CLASSPATH=${JMOL_HOME}/src:${JMOL_HOME}/jars/sax.jar:${JMOL_HOME}/jars/aelfred.jar:${JMOL_HOME}/jars/cml.jar:${JMOL_HOME}/jars/Acme.jar:${JMOL_HOME}/jars/vecmath1.1-1.12.jar:${JMOL_HOME}/jars/junit.jar
#
# Java 1.1 classpath:
#           CLASSPATH=${JAVA_HOME}/lib/classes.zip:${JMOL_HOME}/src:${JAVA_HOME}/lib/rt.jar:${JMOL_HOME}/jars/swing.jar:${JMOL_HOME}/jars/multi.jar:${JMOL_HOME}/jars/sax.jar:${JMOL_HOME}/jars/aelfred.jar:${JMOL_HOME}/jars/cml.jar:${JMOL_HOME}/jars/Acme.jar
#
# put Jmol.java first, so that compilation is fast
#
SOURCES = org/openscience/jmol/Jmol.java \
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

CLASSES = $(SOURCES:%.java=%.class)

APPLET_SOURCES = org/openscience/miniJmol/JmolApplet.java \
		org/openscience/miniJmol/AtomTypeSet.java \
		org/openscience/miniJmol/BaseAtomType.java \
		org/openscience/miniJmol/CMLReader.java \
		org/openscience/miniJmol/CMLHandler.java \
		org/openscience/miniJmol/ChemFile.java \
		org/openscience/miniJmol/ChemFrame.java \
		org/openscience/miniJmol/DTDResolver.java \
		org/openscience/miniJmol/DisplaySettings.java \
		org/openscience/miniJmol/JMolCDO.java \
		org/openscience/miniJmol/JmolBeanTest.java \
		org/openscience/miniJmol/JmolSimpleBean.java \
		org/openscience/miniJmol/Matrix3D.java \
		org/openscience/miniJmol/PDBReader.java \
		org/openscience/miniJmol/XYZReader.java \
		org/openscience/miniJmol/displayPanel.java \
		org/openscience/miniJmol/Atom.java \
		org/openscience/miniJmol/BondRenderer.java

APPLET_CLASSES = $(APPLET_SOURCES:%.java=%.class)

APPLET_JMOL_SOURCES = org/openscience/jmol/FortranFormat.java \
		org/openscience/jmol/PhysicalProperty.java

APPLET_JMOL_CLASSES = $(APPLET_JMOL_SOURCES:%.java=%.class)

TEST_SOURCES = org/openscience/jmol/TestAll.java \
		org/openscience/jmol/TestAtomType.java \
		org/openscience/jmol/TestAtomTypeSet.java \
		org/openscience/jmol/TestAtomTypesModel.java \
		org/openscience/jmol/TestBaseAtomType.java \
		org/openscience/jmol/TestGaussian98Reader.java

TEST_CLASSES = $(TEST_SOURCES:%.java=%.class)

# 
# Some rulesets for compilation
#
# standard java files
#
%.class: %.java
	@echo "compiling $*"
	$(JAVA_COMPILER) -classpath $(CLASSPATH) -O -deprecation $<

all: archive applet_archive

application: $(CLASSES)
applet: $(APPLET_CLASSES) $(APPLET_JMOL_CLASSES)
tests: $(TEST_CLASSES)

applet_archive: applet
	jar uvf ../jars/JmolApplet.jar org/openscience/miniJmol/*.class $(APPLET_JMOL_CLASSES)

archive: application
	/bin/rm -f ../jars/jmol.jar
	jar cvf ../jars/jmol.jar org/openscience/jmol/*.class  org/openscience/jmol/images org/openscience/jmol/Properties org/openscience/jmol/Data
	chmod 664 ../jars/jmol.jar

# suggested by Christoph Steinbeck:
run: application
	$(JAVA) -classpath $(CLASSPATH) -Djmol.home=$(JMOL_HOME) org.openscience.jmol.Jmol

run_tests:
	$(JAVA) -classpath $(CLASSPATH) -Djmol.home=$(JMOL_HOME) org.openscience.jmol.TestAll

# suggested by Hugo Garcia:
doc::
	$(JAVADOC) -classpath $(CLASSPATH) -d doc -sourcepath . org.openscience.jmol org.openscience.miniJmol

clean::
	find . -name '*~' -exec $(RM) {} \;
	find org/openscience -name '*.class' -exec $(RM) {} \;
	/bin/rm -rf doc/*
