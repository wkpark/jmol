To enable JSmol on your wiki:

Download and expand the latest Jmol ( e.g. Jmol-14.0.3-binary.zip ) from http://sourceforge.net
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

To disable this extension simply comment out the require_once line with a numeral (#) sign

Supported _USE parameter on the URL:
?_USE=JAVA   unsigned Jmol applet (Java)
?_USE=SIGNED signed Jmol applet (Java)
?_USE=HTML5  JSmol (HTML5 only) 
?_USE=WEBGL  JSmol (WebGL)

