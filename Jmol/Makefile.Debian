# Edited for Debian GNU/Linux.
DESTDIR  =
BIN      = $(DESTDIR)/usr/bin
LIBS     = ${DESTDIR}/usr/share/java
EXTRA    = ${DESTDIR}/usr/share/jmol/lib
HTMLDOC  = ${DESTDIR}/usr/share/doc/jmol/html

VERSION="20010505"

all: application

application:
	ant jar
	cp debian/jmol.exec jmol

archive: application

clean:
	ant clean

install:
	install -d ${BIN} ${LIBS} ${EXTRA} ${HTMLDOC}
	install ./jmol ${BIN}
	install ./jars/jmol.jar ${LIBS}
	install ./jars/*jar ${EXTRA}
	install ./generatedSource/org/openscience/jmol/Data/guide/* ${HTMLDOC}

deb: clean
	ant sourcedist.tar.gz	
	cp jmol-${VERSION}.source.tar.gz /tmp/jmol-${VERSION}.tar.gz
	cd /tmp; tar zxvf jmol-${VERSION}.tar.gz
	cp Makefile.Debian /tmp/jmol-${VERSION}/.
	cp -R debian /tmp/jmol-${VERSION}/.
	cd /tmp/jmol-${VERSION}; dpkg-buildpackage -rfakeroot
