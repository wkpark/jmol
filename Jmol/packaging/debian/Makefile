# Makefile for making Debian GNU/Linux packages
#
DESTDIR  =
BIN      = $(DESTDIR)/usr/bin
LIBS     = ${DESTDIR}/usr/share/java
EXTRA    = ${DESTDIR}/usr/share/jmol/lib
HTMLDOC  = ${DESTDIR}/usr/share/doc/jmol/html

VERSION=3

CLASSPATH=`find /tmp/jmol-${VERSION}/jars -name "*.jar" | tr '\n' ':'`

all: deb

archive: jars/jmol.jar

jars/jmol.jar: 
	@ant -Ddocbook.dir=/usr/share/sgml/docbook/stylesheet/xsl/nwalsh

clean:
	@rm -f -r jmol-*
	@rm -f -r jmol_*

install:
	@install -d ${BIN} ${LIBS} ${EXTRA} ${HTMLDOC}
	@install ./jmol ${BIN}
	@install ./jars/jmol.jar ${LIBS}
	@install ./jars/*jar ${EXTRA}
	@install ./src/org/openscience/jmol/Data/guide/* ${HTMLDOC}

setup: clean
	@echo "Building package for version ${VERSION}"
	# cp /tmp/jmol-${VERSION}.source.tar.gz /tmp/jmol_${VERSION}.orig.tar.gz
	cd /tmp; tar zxvf jmol-${VERSION}.source.tar.gz
	cp Makefile /tmp/jmol-${VERSION}/.
	cp jmol /tmp/jmol-${VERSION}/.
	cp -R ../debian /tmp/jmol-${VERSION}/.
	cp -R build.xml /tmp/jmol-${VERSION}/.

guide:
	@echo "Compiling Jmol guide..."
	mkdir -p /tmp/jmol-${VERSION}/src/org/openscience/jmol/Data/guide/
	cd /tmp/jmol-${VERSION}/src/org/openscience/jmol/Data/guide; xsltproc --nonet --novalid /usr/share/sgml/docbook/stylesheet/xsl/nwalsh/html/chunk.xsl ../../../../../../doc/source/JMolGuide.docbook.xml

deb: setup guide
	cd /tmp/jmol-${VERSION}; dpkg-buildpackage -rfakeroot
