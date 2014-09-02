To enable JSmol on your wiki:

Download and expand the latest Jmol ( e.g. Jmol-14.2.4_2014.08.03-binary.zip ) from http://sourceforge.net/projects/jmol/
Find and expand jsmol.zip inside the newly created directory
Move the obtained jsmol/ folder into your MediaWiki extensions/ folder

Create a folder 'wiki' under extensions/jsmol/
Download these files into the folder extensions/jsmol/wiki

Add to your LocalSettings.php file these lines:

require_once( "$IP/extensions/jsmol/wiki/Jmol.php" );
$wgJmolAuthorizeUrl = true;
$wgJmolAuthorizeUploadedFile = true;
$wgJmolAuthorizeJmolPdbTag = true;
$wgJmolAuthorizeChoosingSignedApplet = true;
$wgJmolDrawControls = false; // if true draw Jmol controls under applet
$wgJmolForceHTML5 = true; // false=uses HTML5 for tablets only , true=always HTML5

You may use a different CSS default style for the title under applets with
// $wgJmolDefaultTitleCSS = "border: 1px solid black;";

To disable this extension simply comment out the require_once line with a numeral (#) sign

Supported USE parameter on the URL:
?use=java   signed Jmol applet (Java)
?use=html5  JSmol (HTML5 only) 
?use=webgl  JSmol (WebGL)

NOTE: remember to edit the file mime.types in your mediawiki installation,
and comment the line with chemical/x-pdb, declaring pdb as text/plain, as follows:

# chemical/x-pdb pdb
text/plain txt xyz pdb

This extension is also at http://sourceforge.net/p/jmol/code/HEAD/tree/trunk/Jmol-extensions/wiki/MediaWiki/
