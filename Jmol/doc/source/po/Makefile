
SHELL = /bin/sh

DATE = $(shell date -I)

XML2PO = xml2po

XMLSUFFIX = docbook.xml

GUIDES = \
	JmolAppletGuide \
	JmolDevelopersGuide \
	JmolGuide

update:
	for guide in $(GUIDES); do \
		$(XML2PO) -e -o tmp.pot ../$${guide}.$(XMLSUFFIX) ; \
		cat template.pot tmp.pot > $${guide}2.pot ; \
		sed -e 's!YEAR-MO-DATE!$(DATE)!' < $${guide}2.pot > $${guide}.pot; \
		rm $${guide}2.pot tmp.pot; \
	done