<?php
/**
 * Jmol extension - adds the possibility to include [http://www.jmol.org Jmol applets] in MediaWiki.
 *
 * @file
 * @ingroup Extensions
 * @version 3.3_dev
 * @author Nicolas Vervelle
 * @author Angel Herraez
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
 */

//<source lang=php>

# Not a valid entry point, skip unless MEDIAWIKI is defined
if ( !defined( 'MEDIAWIKI' ) ) {
	echo "Jmol extension";
	exit( 1 );
}

# Internationalisation file
$dir = dirname( __FILE__ ) . '/';
$wgExtensionMessagesFiles['Jmol'] = $dir . 'Jmol.i18n.php';

// Extension credits that will show up on Special:Version
$wgExtensionCredits['specialpage'][] = array(
	'path'           => __FILE__,
	'name'           => 'Jmol',
	'descriptionmsg' => 'jmol-desc',
	'version'        => '3.3_dev',
	'author'         => array( 'Nicolas Vervelle', 'Angel Herraez', 'Jmol Development Team' ),
	'url'            => 'http://wiki.jmol.org/index.php/MediaWiki',
);

// Global configuration parameters
global $wgJmolAuthorizeChoosingSignedApplet;
global $wgJmolAuthorizeUploadedFile;
global $wgJmolAuthorizeUrl;
global $wgJmolDefaultAppletSize;
global $wgJmolDefaultScript;
global $wgJmolExtensionPath;
global $wgJmolForceNameSpace;
global $wgJmolShowWarnings;
global $wgJmolUsingSignedAppletByDefault;

// These are the default (recommended) values.
// They can be changed here, but it is advisable to change them in LocalSettings.php
$wgJmolAuthorizeChoosingSignedApplet = false;
$wgJmolAuthorizeUploadedFile = true;
$wgJmolAuthorizeUrl = false;
$wgJmolDefaultAppletSize = "400";
$wgJmolDefaultScript = "";
$wgJmolExtensionPath = $wgScriptPath."/extensions/Jmol";
$wgJmolForceNameSpace = "";
$wgJmolShowWarnings = true;
$wgJmolUsingSignedAppletByDefault = false;

// Autoload Jmol extension
$wgAutoloadClasses['Jmol'] = $dir . '/Jmol.body.php';

global $wgHooks;
$wgHooks['ParserFirstCallInit'][] = 'wfJmolParserInit';

function wfJmolParserInit( &$parser ) {
	new Jmol;
	return true;
}

//</source>
