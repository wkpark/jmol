<?php
/**
 * @author Nicolas Vervelle, Jmol Development team
 * @package Jmol
 */
//<source lang=php>
$wgExtensionCredits['parserhook'][] = array(
  'author'      => 'Nicolas Vervelle, Jmol Development Team',
  'description' => 'adds the possibility to include [http://www.jmol.org Jmol] applets in MediaWiki.',
  'name'        => 'Jmol Extension for MediaWiki',
  'update'      => '12-01-2007',
  'url'         => 'http://wiki.jmol.org/index.php/MediaWiki',
  'status'      => 'development',
  'type'        => 'hook',
  'version'     => StubManager::getRevisionId( '$Id$' )
);

StubManager::createStub(
    'Jmol',
    dirname(__FILE__).'/Jmol.body.php',
    null, //dirname(__FILE__).'/Jmol.i18n.php',
    array( 'ParserBeforeStrip',
           'ParserAfterStrip' ),
    false,                              // no need for logging support
    array( 'jmol' ),                    // tags
    null,                               //of parser function magic words,
    null
);
//</source>