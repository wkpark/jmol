=== Jmol (JSmol) Extension for MediaWiki ===
=== version 5.0 ===

Adds the possibility to include JSmol objects in MediaWiki pages.


To enable JSmol in your wiki:

Create a Jmol/ folder inside your MediaWiki extensions/ folder.
Download and expand the latest Jmol ( e.g. Jmol-14.xx.xx-binary.zip ) from http://sourceforge.net/projects/jmol/
Extract into a temporary location the jsmol.zip file.
Extract from it into the extensions/Jmol/ folder these files and folders:
    the JSmol.min.js file,
    the JSmol.GLmol.min.js file,
    the j2s, php and idioma folders
If you want users to be able to use the Java applet modality of Jmol (in compatible browsers), extract from the downloaded distribution the contents of the java folder and put them in a java folder under the extensions/Jmol/ folder.

The result should look like this:
📂Mediawiki
  └─📂extensions
       └─📂Jmol
           ├──📁j2s       $
           ├──📁idioma    $
           ├──📁php       $
           ├──📁java      #
           ├──Jmol.php            *
           ├──Jmol.body.php       *
           ├──Jmol.i18n.php       *
           ├──Jmol.css            *
           ├──Mobile_Detect.php   *
           ├──JSmolPopup.htm      *
           ├──JSmolPopup.js       *
           ├──readme.txt          *
           ├──JSmol.min.js        $
           └──JSmol.GLmol.min.js  $
*) files from the extension
$) files from JSmol
#) files from Jmol


Add to your LocalSettings.php file this line:

require_once( "extensions/Jmol/Jmol.php" );

To disable this extension simply comment out the require_once line with a numeral (#) sign


You may use a different CSS default style for the title above applets with
// $wgJmolDefaultTitleCSS = "border: 1px solid black;";


Supported USE parameter on the URL:
?use=html5  JSmol (HTML5 only, default) 
?use=webgl  JSmol (WebGL)
?use=java   signed Jmol applet (Java, if the browser supports it)

NOTE: remember to edit the file mime.types in your mediawiki installation,
and comment the line with chemical/x-pdb, declaring pdb as text/plain, as follows:

# chemical/x-pdb pdb
text/plain txt xyz pdb

This extension is also at http://sourceforge.net/p/jmol/code/HEAD/tree/trunk/Jmol-extensions/wiki/MediaWiki/
