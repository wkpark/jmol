# Makefile for making Debian GNU/Linux packages
#
DESTDIR  =
BIN      = $(DESTDIR)/usr/bin
LIBS     = ${DESTDIR}/usr/share/java
EXTRA    = ${DESTDIR}/usr/share/jmol/lib
HTMLDOC  = ${DESTDIR}/usr/share/doc/jmol/html

VERSION=7

CLASSPATH=`find /tmp/jmol-${VERSION}/jars -name "*.jar" | tr '\n' ':'`

all: deb

archive: jars/jmol.jar

jars/jmol.jar: 
	@ant -Ddocbook.dir=/usr/share/sgml/docbook/stylesheet/xsl/nwalsh -Dbuild.compiler=jikes doc jar


clean:
	@rm -f -r jmol-*
	@rm -f -r jmol_*

install:
	@install -d ${BIN} ${LIBS} ${EXTRA} ${HTMLDOC}
	@install ./jmol ${BIN}
	@install ./jmol.jar ${LIBS}
	# @install ./jars/*jar ${EXTRA}
	@install ./build/doc/JmolUserGuide/* ${HTMLDOC}

setup: clean
	@echo "Setting up build dir for version ${VERSION}"
	cp ../../build/jmol-${VERSION}.source.tar.gz /tmp
	cd /tmp; tar zxvf jmol-${VERSION}.source.tar.gz
	@echo " ... copying debian specific files"
	cp Makefile /tmp/jmol-${VERSION}/.
	cp jmol /tmp/jmol-${VERSION}/.
	cp -R ../debian /tmp/jmol-${VERSION}/.
	@echo " ... adapting build.xml to match Debian architecture"
	cd /tmp/jmol-${VERSION}; patch < debian/patches/patch.build.xml
	@echo " ... removing files provided by other Debian packages"
	rm -f /tmp/jmol-${VERSION}/jars/junit.jar
	rm -f /tmp/jmol-${VERSION}/jars/saxon.jar
	@echo " ... removing unneeded files"
	rm -f /tmp/jmol-${VERSION}/jars/gnujaxp-saxonly.jar

deb: setup
	@echo "Building package for version ${VERSION}"
	cd /tmp/jmol-${VERSION}; dpkg-buildpackage -rfakeroot
