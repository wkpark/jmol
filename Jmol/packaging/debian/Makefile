# Makefile for making Debian GNU/Linux packages
#
DESTDIR  =
BIN      = $(DESTDIR)/usr/bin
LIBS     = ${DESTDIR}/usr/share/java
EXTRA    = ${DESTDIR}/usr/share/jmol/lib
HTMLDOC  = ${DESTDIR}/usr/share/doc/jmol/html

VERSION=`cat VERSION`

all: application

application:
	ant jar

archive: application

clean:
	rm -f jmol-*
	rm -f jmol_*

install:
	install -d ${BIN} ${LIBS} ${EXTRA} ${HTMLDOC}
	install ./jmol ${BIN}
	install ./jars/jmol.jar ${LIBS}
	install ./jars/*jar ${EXTRA}
	install ./generatedSource/org/openscience/jmol/Data/guide/* ${HTMLDOC}

deb: clean
	cd ../../; ant sourcedist.tar.gz	
	mv ../../jmol-${VERSION}.source.tar.gz jmol-${VERSION}.tar.gz
	tar zxvf jmol-${VERSION}.tar.gz
	cp Makefile jmol-${VERSION}/.
	cp -R debian jmol-${VERSION}/.
	cp jmol jmol-${VERSION}/.
	cd jmol-${VERSION}; dpkg-buildpackage -rfakeroot
