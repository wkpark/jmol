# Makefile for making Debian GNU/Linux packages
#
DESTDIR  =
BIN      = $(DESTDIR)/usr/bin
LIBS     = ${DESTDIR}/usr/share/java
EXTRA    = ${DESTDIR}/usr/share/jmol/lib
HTMLDOC  = ${DESTDIR}/usr/share/doc/jmol/html

#VERSION=`cat VERSION`
VERSION=1

all: application

application:
	@ant jar -Ddocbook.dir=/usr/lib/sgml/stylesheet/xsl/docbook/nwalsh/

archive: application

clean:
	@rm -f -r jmol-*
	@rm -f -r jmol_*

install:
	@install -d ${BIN} ${LIBS} ${EXTRA} ${HTMLDOC}
	@install ./jmol ${BIN}
	@install ./jars/jmol.jar ${LIBS}
	@install ./jars/*jar ${EXTRA}
	@install ./generatedSource/org/openscience/jmol/Data/guide/* ${HTMLDOC}

deb: clean
	@echo "Building package for version ${VERSION}"
	cd ../../; ant sourcedist.tar.gz	
	mv ../../jmol-${VERSION}.source.tar.gz /tmp/jmol-${VERSION}.tar.gz
	cd /tmp; tar zxvf jmol-${VERSION}.tar.gz
	cp Makefile /tmp/jmol-${VERSION}/.
	cp -R ../debian /tmp/jmol-${VERSION}/.
	cp jmol /tmp/jmol-${VERSION}/.
	cd /tmp/jmol-${VERSION}; dpkg-buildpackage -rfakeroot
