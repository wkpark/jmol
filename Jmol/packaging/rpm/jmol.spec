Summary: A molecular viewer and editor.
Name: jmol
Version: @version@
Release: @release@
Copyright: GPL
Group: Applications/Science
Packager: Bradley A. Smith <bradley@baysmith.com>
Source0: jmol-@version@.source.tar.gz
Source1: docbook-xsl-1.40.tar.gz
BuildArchitectures: noarch
BuildRoot: /var/tmp/%{name}-buildroot

%description
Jmol is an open-source molecule viewer and editor written in Java. Jmol
provides a platform-independent means of viewing 3D molecular models
produced by various software packages (ACES II, ADF, GAMESS, PC GAMESS,
Gaussian 9x, XYZ, PDB, and CML). Molecular models can be translated and
rotated. Geometric properties can be calculated, such as bond lengths,
bond angles, or torsions. Multi-step files and files with frequency
information can be animated. The molecules displayed can printed or
exported in several graphics formats (JPG, GIF, PPM).

Jmol was developed through a voluntary collaboration of researchers
around the world. For more information, please visit the Jmol Web site
at http://jmol.sourceforge.net/.

%prep
%setup -qb 1

%build
ant

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/bin
mkdir -p $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars
install -m 755 jmol $RPM_BUILD_ROOT/usr/share/jmol-%{version}
ln -s /usr/share/jmol-%{version}/jmol $RPM_BUILD_ROOT/usr/bin/jmol
install -m 755 jars/jmol.jar $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars
install -m 755 jars/Acme.jar $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars
install -m 755 jars/cdk-cml.jar $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars
install -m 755 jars/gnujaxp.jar $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars
install -m 755 jars/jas.jar $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars
install -m 755 jars/multi.jar $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars
install -m 755 jars/vecmath1.1-1.12.jar $RPM_BUILD_ROOT/usr/share/jmol-%{version}/jars

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%doc README.txt COPYRIGHT.txt
/usr/bin/jmol
/usr/share/jmol-%{version}

%changelog
* Sun May 6 2001 Bradley A. Smith <bradley@baysmith.com>
- Initial RPM spec.


