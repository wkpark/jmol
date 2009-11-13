<?php
/**
 * @author Nicolas Vervelle, Angel Herraez, Jmol Development team
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
*/

//<source lang=php>
$wgExtensionCredits['parserhook'][] = array(
  'author'      => 'Nicolas Vervelle, Angel Herraez; Jmol Development Team',
  'description' => 'adds the possibility to include [http://www.jmol.org Jmol] applets in MediaWiki.',
  'name'        => 'Jmol Extension for MediaWiki',
  'update'      => '2009-11-13',
  'url'         => 'http://wiki.jmol.org/index.php/MediaWiki',
  'status'      => 'development',
  'type'        => 'hook',
  'version'     => '3.2'
//  'version'     => StubManager::getRevisionId( '$Id$' )
);

/* Global configuration parameters */
global $wgJmolAuthorizeChoosingSignedApplet;
global $wgJmolAuthorizeUploadedFile;
global $wgJmolAuthorizeUrl;
global $wgJmolDefaultAppletSize;
global $wgJmolDefaultScript;
global $wgJmolExtensionPath;
global $wgJmolForceNameSpace;
global $wgJmolShowWarnings;
global $wgJmolUsingSignedAppletByDefault;

/*	These are the default (recommended) values; they can be changed here, 
	but it is advisable to chenge them in LocalSettings.php
*/
$wgJmolAuthorizeChoosingSignedApplet = false;
$wgJmolAuthorizeUploadedFile = true;
$wgJmolAuthorizeUrl = false;
$wgJmolDefaultAppletSize = "400";
$wgJmolDefaultScript = "";
$wgJmolExtensionPath = $wgScriptPath."/extensions/Jmol";
$wgJmolForceNameSpace = "";
$wgJmolShowWarnings = true;
$wgJmolUsingSignedAppletByDefault = false;

StubManager::createStub(
    'Jmol',
    dirname(__FILE__).'/Jmol.body.php',
    null, //dirname(__FILE__).'/Jmol.i18n.php',
    array( 'OutputPageBeforeHTML',
           'ParserBeforeStrip',
           'ParserAfterStrip' ),
    false,                              // no need for logging support
    array( 'jmol' ),                    // tags
    null,                               //of parser function magic words,
    null
);
//</source>