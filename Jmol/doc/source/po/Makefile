
SHELL = /bin/sh

DATE = $(shell date +%F)
TIME = $(shell date +%R)
ZONE = $(shell date +%z)

XML2PO = xml2po

XMLSUFFIX = docbook.xml

LANG = \
	de

GUIDES = \
	JmolAppletGuide \
	JmolDevelopersGuide \
	JmolGuide

update-pot:
	for guide in $(GUIDES); do \
		$(XML2PO) -e -o tmp.pot ../$${guide}.$(XMLSUFFIX) ; \
		cat template.pot tmp.pot > $${guide}.tmp ; \
		sed -e 's!YEAR-MO-DATE HO:MI+ZONE!$(DATE) $(TIME)$(ZONE)!' < $${guide}.tmp > $${guide}.pot; \
		rm $${guide}.tmp tmp.pot; \
	done

update-trans:
	for guide in $(GUIDES); do \
		for lang in $(LANG); do \
			$(XML2PO) -e -u $${guide}.$${lang}.po ../$${guide}.$(XMLSUFFIX) ; \
		done ; \
	done

translate:
	for guide in $(GUIDES); do \
		for lang in $(LANG); do \
			$(XML2PO) -e -p $${guide}.$${lang}.po -o ../$${guide}_$${lang}.tmp ../$${guide}.$(XMLSUFFIX) ; \
			sed -e 's!$${guide}\.$${lang}!$${lang}!' < ../$${guide}_$${lang}.tmp > ../$${guide}_$${lang}.$(XMLSUFFIX) ; \
			rm ../$${guide}_$${lang}.tmp ; \
		done ; \
	done