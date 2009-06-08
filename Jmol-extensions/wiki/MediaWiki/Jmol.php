<?php
/**
 * @author Nicolas Vervelle, Jmol Development team
 * @package Jmol
 */
//<source lang=php>
$wgExtensionCredits['parserhook'][] = array(
  'author'      => 'Nicolas Vervelle, Angel Herraez; Jmol Development Team',
  'description' => 'adds the possibility to include [http://www.jmol.org Jmol] applets in MediaWiki.',
  'name'        => 'Jmol Extension for MediaWiki',
  'update'      => '2008-12-09',
  'url'         => 'http://wiki.jmol.org/index.php/MediaWiki',
  'status'      => 'development',
  'type'        => 'hook',
  'version'     => '3.0'
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

$wgJmolAuthorizeChoosingSignedApplet = true;
$wgJmolAuthorizeUploadedFile = true;
$wgJmolAuthorizeUrl = true;
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