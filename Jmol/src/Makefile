.SUFFIXES: $(SUFFIXES) .jar .java .class
           JMOL_HOME=$(PWD)/..
           JAVA_HOME= /usr/local/jdk1.2.2
               JIKES= /usr/local/bin/jikes
               JAVAC= $(JAVA_HOME)/bin/javac
                JAVA= $(JAVA_HOME)/bin/java
             JAVADOC= $(JAVA_HOME)/bin/javadoc -private
        THREADS_FLAG=native
           CLASSPATH=${JAVA_HOME}/jre/lib/rt.jar:${JMOL_HOME}/src:${JMOL_HOME}/jars/sax.jar:${JMOL_HOME}/jars/aelfred.jar:${JMOL_HOME}/jars/cml.jar:${JMOL_HOME}/jars/Acme.jar:${JMOL_HOME}/jars/vecmath1.1-1.12.jar:${JMOL_HOME}/jars/junit.jar
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
		org/openscience/jmol/AtomTypeSet.java \
		org/openscience/jmol/AtomTypeTable.java \
		org/openscience/jmol/AtomTypesModel.java \
		org/openscience/jmol/BaseAtomType.java \
		org/openscience/jmol/Bond.java \
		org/openscience/jmol/CMLReader.java \
		org/openscience/jmol/CMLSaver.java \
		org/openscience/jmol/Charge.java \
		org/openscience/jmol/ChemFile.java \
		org/openscience/jmol/ChemFileReader.java \
		org/openscience/jmol/ChemFrame.java \
		org/openscience/jmol/ConsoleTextArea.java \
		org/openscience/jmol/DTDResolver.java \
		org/openscience/jmol/DecimalNumberField.java \
		org/openscience/jmol/Dihedral.java \
		org/openscience/jmol/DisplaySettings.java \
		org/openscience/jmol/Distance.java \
		org/openscience/jmol/Energy.java \
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
		org/openscience/jmol/JMolCDO.java \
		org/openscience/jmol/JmolException.java \
		org/openscience/jmol/JmolResourceHandler.java \
		org/openscience/jmol/JmolFileFilter.java \
		org/openscience/jmol/JpegEncoder.java \
		org/openscience/jmol/ListNode.java \
		org/openscience/jmol/LoopedStreams.java \
		org/openscience/jmol/Matrix3D.java \
		org/openscience/jmol/Measure.java \
		org/openscience/jmol/Measurement.java \
		org/openscience/jmol/MeasurementInterface.java \
		org/openscience/jmol/MeasurementList.java \
		org/openscience/jmol/MeasurementListEvent.java \
		org/openscience/jmol/MeasurementListListener.java \
		org/openscience/jmol/MultiLineHeaderRenderer.java \
		org/openscience/jmol/NMRShielding.java \
		org/openscience/jmol/PDBReader.java \
		org/openscience/jmol/PhysicalProperty.java \
		org/openscience/jmol/Preferences.java \
		org/openscience/jmol/PropertyGraph.java \
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
		org/openscience/miniJmol/BondRenderer.java \
		org/openscience/miniJmol/ChemFileReader.java \
		org/openscience/miniJmol/ReaderFactory.java \
		org/openscience/miniJmol/PDBReader.java \
		org/openscience/miniJmol/XYZReader.java \
		org/openscience/miniJmol/Gaussian98Reader.java

APPLET_CLASSES = $(APPLET_SOURCES:%.java=%.class)

APPLET_JMOL_SOURCES = org/openscience/jmol/FortranFormat.java \
		org/openscience/jmol/PhysicalProperty.java \
		org/openscience/jmol/ChemFileReader.java

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
	$(JIKES) -classpath $(CLASSPATH) -O -deprecation $<

all: archive applet_archive

archive: $(CLASSES)
application: $(CLASSES)
applet: $(APPLET_CLASSES) $(APPLET_JMOL_CLASSES)
tests: $(TEST_CLASSES)

applet_archive: applet
	jar cvf ../jars/JmolApplet.jar org/openscience/miniJmol/*.class $(APPLET_JMOL_CLASSES)

archive: application
	/bin/rm -f ../jars/jmol.jar
	jar cvmf manifest ../jars/jmol.jar org/openscience/jmol/*.class  org/openscience/jmol/images/*.gif org/openscience/jmol/images/*.jpg org/openscience/jmol/Properties/Jmol.properties org/openscience/jmol/Data/AtomTypes org/openscience/jmol/Data/NMRReferences org/openscience/jmol/Data/cml.dtd org/openscience/jmol/Data/*.html
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
	# Messes with CVS:
	# /bin/rm -rf doc/*
