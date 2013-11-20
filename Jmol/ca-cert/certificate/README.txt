To build a Jmol version signed with your own certificate, do the following :
- Put the PKCS12 file containing your own certificate in this directory. It must be named Jmol.p12
- Run build.xml as usual, but define the following values:
  - Jmol.p12.password: password for the PKCS12 file
  - Jmol.p12.alias: key alias in the PKCS12 file to be used for signing Jmol
  - Jmol.p12key.password: password for the key to be used for signing Jmol
