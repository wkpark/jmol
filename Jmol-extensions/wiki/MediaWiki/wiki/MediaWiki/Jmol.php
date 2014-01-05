<?php
/**
 * Jmol extension - adds the possibility to include [http://www.jmol.org Jmol applets] in MediaWiki.
 *
 * @file
 * @ingroup Extensions
 * @version 4.0_dev
 * @author Nicolas Vervelle
 * @author Angel Herraez
 * @author Jaime Prilusky
 * @author Jmol Development team
 * @license http://www.gnu.org/copyleft/gpl.html GNU General Public License 2.0 or later
 * @link http://wiki.jmol.org/index.php/MediaWiki Documentation
 * @package Jmol
 */

/* Nov-Dec. 08 - several fixes by AH
    Compatibility with wikis residing in a non-root folder of the server.
    Javascript command blocked from inside Jmol.
    Removes <br> inserted after applets.
 * Jun. 09 - version 3.1 - by AH
    Adds support for pop-in applet.
		Some code cleanup (extensionPath is no longer used).
 * Nov. 09 - version 3.2 - by AH
    Adds support for URLs that have '&' in them (as, for example, PHP pages).
		This may be used with <urlContents> subtag of <jmolApplet> 
		and with the <script> subtag of nearly all tags.
		Note that %26 must be used in the wikicode instead of ampersands in the URL.
		E.g.:  <urlContents>http://some.server.com/myMols/?a=value1%26b=value2%26c=value3</urlContents>
		  meaning http://some.server.com/myMols/?a=value1&b=value2&c=value3
		or: <script>load http://some.server.com/myMols/?a=value1%26b=value2%26c=value3</script>
 * Nov. 10 - version 3.3 - by NV
    Adds Compatibility with MW 1.16
	Removes the dependency on StubManager
 * Oct. 2013 - version 4.0 - by JP
    Adds support for JSmol
 */

//<source lang=php>

# Not a valid entry point, skip unless MEDIAWIKI is defined
if ( !defined( 'MEDIAWIKI' ) ) {
	echo "Jmol extension";
	exit( 1 );
}

# Initialisation
// $jsmolWikiDir = dirname(__FILE__);
// $wgAutoloadClasses['Jmol'] = "$jsmolWikiDir/Jmol.body.php";
// $wgExtensionMessagesFiles['Jmol'] = "$jsmolWikiDir/Jmol.i18n.php";

$dir = dirname(__FILE__) . '/';
$wgExtensionMessagesFiles['Jmol'] = $dir  . 'Jmol.i18n.php';
$wgAutoloadClasses['Jmol'] = $dir  . 'Jmol.body.php';

$wgJmolVersion = '4.0_dev';

// Bump this when updating Jmol.js or JmolMediaWiki.js to help update caches
$wgJmolScriptVersion = $wgJmolVersion . '_1';

// Extension credits that will show up on Special:Version
$wgExtensionCredits['parserhook'][] = array(
	'path'           => __FILE__,
	'name'           => 'JmolExtension',
	'description'    => currentJmolVersion(),
	'version'        => $wgJmolScriptVersion,
	'author'         => array( 'Nicolas Vervelle', 'Angel Herraez', 'Jaime Prilusky', 'Jmol Development Team' ),
	'url'            => 'http://www.mediawiki.org/wiki/Extension:Jmol',
);

// Global configuration parameters
global $wgJmolAuthorizeChoosingSignedApplet;
global $wgJmolAuthorizeJmolFileTag;
global $wgJmolAuthorizeJmolPdbTag;
global $wgJmolAuthorizeJmolSmilesTag;
global $wgJmolAuthorizeJmolTag;
global $wgJmolAuthorizeUploadedFile;
global $wgJmolAuthorizeUrl;
global $wgJmolDefaultAppletSize;
global $wgJmolDefaultScript;
global $wgJmolExtensionPath;
global $wgJmolForceNameSpace;
global $wgJmolDrawControls;
global $wgJmolForceHTML5;
global $wgJmolShowWarnings;
global $wgJmolUsingSignedAppletByDefault;
global $wgJmolMaxAppletSize;

// These are the default (recommended) values.
// They can be changed here, but it is advisable to change them in LocalSettings.php
$wgJmolAuthorizeChoosingSignedApplet = false;
$wgJmolAuthorizeJmolFileTag = true;
$wgJmolAuthorizeJmolPdbTag = true;
$wgJmolAuthorizeJmolSmilesTag = true;
$wgJmolAuthorizeJmolTag = true;
$wgJmolAuthorizeUploadedFile = true;
$wgJmolAuthorizeUrl = false;
$wgJmolDefaultScript = "";
$wgJmolExtensionPath = $wgScriptPath."/extensions/jsmol/wiki"; // Jmol";
$wgJmolForceNameSpace = "";
$wgJmolShowWarnings = true;
$wgJmolUsingSignedAppletByDefault = false;
$wgJmolDefaultAppletSize = "400";
$wgJmolMaxAppletSize = "600";
$wgJmolDrawControls = false;

global $wgHooks;
if ( defined( 'MW_SUPPORTS_PARSERFIRSTCALLINIT' ) ) {
  $wgHooks['ParserFirstCallInit'][] = 'wfJmolParserInit';
} else { # support mediawiki < 1.20
  $wgExtensionFunctions[] = 'wfJmolParserInit';
}

function wfJmolParserInit( ) {
  new Jmol;  return true;
}

function currentJmolVersion() {
  $propF = dirname(dirname(__FILE__)) . '/j2s/Jmol.properties';
  $version = 'Enables access to local Jmol ';
  if (file_exists($propF)) {
    $txt = file_get_contents($propF);
    preg_match('/___JmolVersion=(.+)/', $txt, $matches);
    $version .= "( now loaded Jmol version " . preg_replace('/\"/','',$matches[1]) . ")";
  }
  return $version;
}

//</source>
