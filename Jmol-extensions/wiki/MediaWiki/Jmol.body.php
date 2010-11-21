<?php
/**
 * Jmol extension - adds the possibility to include [http://www.jmol.org Jmol applets] in MediaWiki.
 *
 * @ingroup Extensions
 * 
 * @author Nicolas Vervelle
 * @author Angel Herraez
 * @author Jmol Development team
 *
 * @license http://www.gnu.org/copyleft/gpl.html GNU General Public License 2.0 or later
 * @link http://wiki.jmol.org/index.php/MediaWiki Documentation
 * @package Jmol
 */

//<source lang=php>
if (!defined('MEDIAWIKI')) {
  die('This file is a MediaWiki extension, it is not a valid entry point');
}

require_once("Title.php");

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

class Jmol {
	/**#@+
	 * @access private
	 */

	/**
	 * True when a <jmol> tag is being processed.
	 * Used to avoid infinite recursion
	 * 
	 * @var boolean
	 */
	var $mInJmol = false;

	var $mOutput, $mDepth;
	var $mCurrentObject, $mCurrentTag, $mCurrentSubTag;

	var $mValChecked;
	var $mValColor;
	var $mValInlineContents;
	var $mValItems;
	var $mValMenuHeight;
	var $mValName;
	var $mValPositionX;
	var $mValPositionY;
	var $mValScript;
	var $mValScriptWhenChecked;
	var $mValScriptWhenUnchecked;
	var $mValSigned;
	var $mValSize;
	var $mValTarget;
	var $mValText;
	var $mValTitle;
	var $mValHeader;
	var $mValCaption;
	var $mValUploadedFileContents;
	var $mValUrlContents;
	var $mValVertical;
	var $mValWikiPageContents;

	/**#@-*/

	/**
	 * Constructor
	 */
	function __construct() {
		$this->mOutput = "";
		$this->mDepth = 0;
		$this->resetValues();
		$this->setHooks();
	}

	/**#@+ @access private */

	// *** //
	// XML //
	// *** //

	// Render Jmol tag
	private function renderJmol( $input ) {
		$this->mOutput = "<!-- Jmol -->";
		$this->mDepth = 0;
		$xmlParser = xml_parser_create();
		xml_set_object( $xmlParser, $this );
		xml_set_element_handler( $xmlParser, "startElement", "endElement" );
		xml_set_character_data_handler( $xmlParser, "characterData" );
		$input = "<jmol>$input<jmol>";
		if ( !xml_parse( $xmlParser, $input ) ) {
			die(sprintf(
				"XML error: %s at line %d",
				xml_error_string( xml_get_error_code( $xmlParser ) ),
				xml_get_current_line_number( $xmlParser ) ) );
		}
		xml_parser_free( $xmlParser );
		return $this->mOutput;
	}

	// Renders a Jmol applet directly in the Wiki page
	private function renderJmolApplet() {
		global $wgJmolExtensionPath;
		$prefix = "";
		$postfix = "";

		$this->mValInlineContents = trim( $this->mValInlineContents );
		$this->mValInlineContents = preg_replace( "/\t/", " ", $this->mValInlineContents );
		// $this->mValInlineContents = preg_replace( "/\n/", "\\n'+\n'", $this->mValInlineContents );
		$prefix .= "<script type='text/javascript'>";
		$postfix .= "</script>\n";
		$this->mOutput .= $this->renderInternalJmolApplet( $prefix, $postfix, "'" );
	}

	// Renders a button in the Wiki page that will open a new window containing a Jmol applet
	private function renderJmolAppletButton() {
		global $wgJmolExtensionPath;
		$prefix = "";
		$postfix = "";

		$prefix .= "<input type='button'";
		if ( $this->mValName != "" ) {
			$prefix .= " name='" . $this->escapeAttribute( $this->mValName ) . "'" .
			           " id='" . $this->escapeAttribute( $this->mValName ) . "'";
		}
		if ( $this->mValText == "" ) {
			$this->mValText = $this->mValTitle;
		}
		if ( $this->mValText != "" ) {
			$prefix .= " value='" . $this->escapeAttribute( $this->mValText ) . "'";
		}
		$prefix .= " onclick=\"jmolWikiPopupWindow(" .
		               "'" . $this->escapeScript( $this->mValTitle ) . "'," .
		               "'" . $this->escapeScript( $this->mValSize ) . "'," .
		               "'" . $this->escapeScript( $this->mValPositionX ) . "'," .
		               "'" . $this->escapeScript( $this->mValPositionY ) . "'," .
		               "'";
		$postfix = "');return true\" />";
		$this->mOutput .= $this->renderInternalJmolApplet( $prefix, $postfix, "\\'" );
	}

	// Renders a link in the Wiki page that will open a new window containing a Jmol applet
	private function renderJmolAppletLink() {
		global $wgJmolExtensionPath;
		$prefix = "";
		$postfix = "";

		$prefix .= "<a";
		if ( $this->mValName != "" ) {
			$prefix .= " name='" . $this->escapeAttribute( $this->mValName ) . "'" .
			           " id='" . $this->escapeAttribute( $this->mValName ) . "'";
		}
		$prefix .= " href=\"javascript:void(0)\"" .
		           " onclick=\"jmolWikiPopupWindow(" .
		               "'" . $this->escapeScript( $this->mValTitle ) . "'," .
		               "'" . $this->escapeScript( $this->mValSize ) . "'," .
		               "'" . $this->escapeScript( $this->mValPositionX ) . "'," .
					   "'" . $this->escapeScript( $this->mValPositionY ) . "'," .
		               "'";
		$postfix .= "');\"";
		$postfix .= ">";
		if ( $this->mValText == "" ) {
			$this->mValText = $this->mValTitle;
		}
		if ( $this->mValText != "" ) {
			$postfix .= $this->mValText;
		}
		$postfix .= "</a>";
		$this->mOutput .= $this->renderInternalJmolApplet( $prefix, $postfix, "\\'" );
	}

	// Renders a link in the Wiki page that will insert a div containing a Jmol applet
	private function renderJmolAppletInlineLink() {
		global $wgJmolExtensionPath;
		$prefix = "";
		$postfix = "";

		$uniqueID = rand(10000,99999);
		$hidelink = "[<a href='javascript:void(0)' " .
		            " title='hide the Jmol applet'" .
		            " style='font-family:Verdana, Arial, Helvetica, sans-serif;'" .
		            " onclick='jmolWikiPopInlineHide(\"" . $uniqueID . "\")'>x</a>]";
		$prefix .= "<div style='width:".$this->mValSize."px; text-align:center;'>";

		if ( $this->mValHeader != "" ) {	
			$prefix .= "<div style='font-weight:bold; position:relative; padding-right:2ex;'>" .
			           $this->mValHeader .
			           "<span id='JmolInlineHide" . $uniqueID . "'" .
			           " style='display:none; position:absolute; right:0; bottom:0; font-weight:normal;'>" .
			           $hidelink .
			           "</span>" .
			           "</div>";
		} else {
			$prefix .= "<div>" .
			           "<span id='JmolInlineHide" . $uniqueID . "'" .
			           " style='display:none; float:right;'>" .
			           $hidelink .
			           "</span>" .
			           "</div>";
		}

		$prefix .= "<a id='JmolInlineLink" . $uniqueID . "'";
		if ( $this->mValName != "" ) {
			$prefix .= " name='" . $this->escapeAttribute( $this->mValName ) . "'" .
			           " id='" . $this->escapeAttribute( $this->mValName ) . "'";
		}
		$prefix .= " href='javascript:void(0)' " .
		           " title='this will load a 3D model in a Jmol applet, which may take some time'" .
		           " onclick=\"jmolWikiPopInline('" . $uniqueID . "','";

		$postfix .= "');\"";
		$postfix .= ">";

		if ( $this->mValText == "" ) {
			$this->mValText = "Jmol";
		}
		$postfix .= $this->mValText;

		$postfix .= "</a>";
		$postfix .= "<div id='JmolInlineEnv" . $uniqueID . "'></div>";
		/*  style='z-index:5;position:absolute;vertical-align:top;' */
		$postfix .= "<div style='font-size:0.85em; line-height:1.2; text-align:left; margin:0.3em 1ex;'>" .
		            $this->mValCaption . "</div></div>";

		$this->mOutput .= $this->renderInternalJmolApplet( $prefix, $postfix, "\\'" );
	}

	// Renders a button to control a Jmol applet
	private function renderJmolButton() {
		$this->mOutput .= "<script type='text/javascript'>\n";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolButton(" .
		                      "'" . $this->escapeScript( $this->mValScript ) . "'," .
		                      "'" . $this->escapeScript( $this->mValText ) . "'";
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");\n" .
		                  "</script>\n";
	}

	// Renders a checkbox to control a Jmol applet
	private function renderJmolCheckbox() {
		$this->mOutput .= "<script type='text/javascript'>\n";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolCheckbox(" .
		                      "'" . $this->escapeScript( $this->mValScriptWhenChecked ) . "'," .
		                      "'" . $this->escapeScript( $this->mValScriptWhenUnchecked ) . "'," .
		                      "'" . $this->escapeScript( $this->mValText ) . "'";
		if ( $this->mValChecked == "true" ) {
			$this->mOutput .= ",true";
		} else {
			$this->mOutput .= ",false";
		}
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");\n" .
		                  "</script>\n";
	}

	// Renders a link to control a Jmol applet
	private function renderJmolLink() {
		$this->mOutput .= "<script type='text/javascript'>\n";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolLink(" .
		                  "'" . $this->escapeScript( $this->mValScript ) . "'," .
		                  "'" . $this->escapeScript( $this->mValText ) . "'";
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");\n" .
		                  "</script>\n";
	}

	// Renders a menu to control a Jmol applet
	private function renderJmolMenu() {
		$this->mOutput .= "<script type='text/javascript'>\n";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolMenu(" .
		                      "[" . $this->mValItems . "]," .
		                      $this->escapeScript( $this->mValMenuHeight );
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");\n" .
		                  "</script>\n";
	}

	// Renders a radio group to control a Jmol applet
	private function renderJmolRadioGroup() {
		$this->mOutput .= "<script type='text/javascript'>\n";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolRadioGroup([" . $this->mValItems . "]";
		if ( $this->mValVertical == "true" ) {
			$this->mOutput .= ",jmolBr()";
		} else {
			$this->mOutput .= ",'&nbsp;'";
		}
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");\n" .
		                  "</script>\n";
	}

	// Internal function to make a Jmol applet
	private function renderInternalJmolApplet( $prefix, $postfix, $sep ) {
		global $wgJmolAuthorizeUrl, $wgJmolAuthorizeUploadedFile;
		global $wgJmolForceNameSpace, $wgJmolExtensionPath, $wgScriptPath;

		$output = $prefix;
		// initialize now goes in the body, thus allowing signed applet:
		$output .= "jmolInitialize(" . $sep . $wgJmolExtensionPath . $sep . ", ";
		if ( $this->mValSigned == "true" ) {
			$output .= "true";
		} else { 
			$output .= "false";
		}
		$output .= "); " .
		           "_jmol.noEval = true; ";

		$output .=
			"jmolCheckBrowser(" .
				$sep . "popup" . $sep . ", " .
				$sep . $wgJmolExtensionPath . "/browsercheck" . $sep . ", " .
				$sep . "onclick" . $sep . ");" .
			"jmolSetAppletColor(" .
				$sep . $this->escapeScript( $this->mValColor ) . $sep . ");";
		if ( $this->mValUploadedFileContents != "" ) {
			if ( $wgJmolAuthorizeUploadedFile == true ) {
				$title = Title::makeTitleSafe( NS_IMAGE, $this->mValUploadedFileContents );
				$article = new Article($title);
				if ( !is_null( $title ) && $article->exists() ) {
					$file = new Image($title);
					$this->mValUrlContents = $file->getURL();
				}
			} else {
				return $this->showWarning( "The field uploadedFileContents is not authorized on this wiki." );
			}
		}
		if ( $this->mValWikiPageContents != "" ) {
			if ( $wgJmolAuthorizeUrl == true ) {
				$this->mValUrlContents = $wgScriptPath."/index.php?title=";	// AH - fix for non-root wikis
				if ( $wgJmolForceNameSpace != "" ) {
					$this->mValUrlContents .= $wgJmolForceNameSpace . ":";
				}
				$this->mValUrlContents .= $this->mValWikiPageContents . "&action=raw";
			} else {
				return $this->showWarning( "The field wikiPageContents is not authorized on this wiki." );
			}
		}
		if ( $this->mValUrlContents != "" ) {
			$output .= "jmolApplet(" .
				$this->escapeScript( $this->mValSize ) . ", " .
				$sep . "set echo p 50% 50%;set echo p center;echo loading...;refresh;" .
					"load " . $this->escapeScript( $this->mValUrlContents ) . "; " .
					$this->escapeScript( $this->mValScript ) . $sep;
		} else {
			$output .= "jmolAppletInline(" .
				$this->escapeScript( $this->mValSize ) . ", " .
				$sep . $this->escapeScript( $this->mValInlineContents ) . $sep . ", " .
				$sep . $this->escapeScript( $this->mValScript ) . $sep;
		}
		if ( $this->mValName != "" ) {
			$output .= "," . $sep . $this->escapeScript( $this->mValName ) . $sep;
		}
		$output .= ");";
		$output .= $postfix;

		return $output;
	}

	// Function called for outputing a warning
	private function showWarning( $message ) {
		global $wgJmolShowWarnings;

		$output = "";
		if ( $wgJmolShowWarnings == true ) {
			$output .= $message;
		}
		return $output;
	}

	// ************* //
	// XML CALLBACKS //
	// ************* //

	// Function called when an opening XML tag is found
	function startElement( $parser, $name, $attrs ) {
		$this->mDepth += 1;
		switch ( $this->mDepth ) {
		case 1:
			// JMOL tag itself
			$this->resetValues();
			break;
		case 2:
			// The interesting tags
			$this->resetValues();
			$this->mCurrentObject = $name;
			break;
		case 3:
			// Details of the interesting tags
			$this->mCurrentTag = $name;
			break;
		case 4:
			// Details of sub tags
			$this->mCurrentSubTag = $name;
			break;
		}
	}

	// Function called when a closing XML tag is found
	function endElement( $parser, $name ) {
		switch ( $this->mDepth ) {
		case 1:
			// JMOL tag itself
			$this->resetValues();
			break;
		case 2:
			// The interesting tags
			switch ( $this->mCurrentObject ) {
			case "JMOLAPPLET":
				$this->renderJmolApplet();
				break;
			case "JMOLAPPLETBUTTON":
				$this->renderJmolAppletButton();
				break;
			case "JMOLAPPLETLINK":
				$this->renderJmolAppletLink();
				break;
			case "JMOLAPPLETINLINELINK":
				$this->renderJmolAppletInlineLink();
				break;
			case "JMOLBUTTON":
				$this->renderJmolButton();
				break;
			case "JMOLCHECKBOX":
				$this->renderJmolCheckbox();
				break;
			case "JMOLLINK":
				$this->renderJmolLink();
				break;
			case "JMOLMENU":
				$this->renderJmolMenu();
				break;
			case "JMOLRADIOGROUP":
				$this->renderJmolRadioGroup();
				break;
			}
			$this->resetValues();
			break;
		case 3:
			// Details of the interesting tags
			switch ( $this->mCurrentTag ) {
			case "ITEM":
				if ( $this->mValItems != "" ) {
					$this->mValItems .= ",";
				}
				$this->mValItems .= "['" . $this->escapeScript( $this->mValScript ) . "'";
				$this->mValItems .= ",'" . $this->escapeScript( $this->mValText ) . "'";
				if ( $this->mValChecked == "true" ) {
					$this->mValItems .= ",true]";
				} else {
					$this->mValItems .= ",false]";
				}
				break;
			}
			$this->mCurrentTag = "";
			break;
		case 4:
			// Details of sub tags
			$this->mCurrentSubTag = "";
			break;
		}
		$this->mDepth -= 1;
	}

	// Function called for the content of a XML tag
	function characterData( $parser, $data ) {
		global $wgJmolAuthorizeChoosingSignedApplet;

		switch ( $this->mDepth ) {
		case 3:
			// Details of the interesting tags
			switch ( $this->mCurrentTag ) {
			case "CHECKED":
				$this->mValChecked = $data;
				break;
			case "COLOR":
				$this->mValColor = $data;
				break;
			case "INLINECONTENTS":
			case "ONLINECONTENTS":
				$data = trim( $data );
				if ( $data != "" ) {
					$this->mValInlineContents .= "\n" . $data;
				}
				break;
			case "MENUHEIGHT":
				$this->mValMenuHeight = $data;
				break;
			case "NAME":
				$this->mValName = $data;
				break;
			case "SCRIPT":
				$this->mValScript = str_replace( "%26", "&", $data );
				break;
			case "SCRIPTWHENCHECKED":
				$this->mValScriptWhenChecked = str_replace( "%26", "&", $data );
				break;
			case "SCRIPTWHENUNCHECKED":
				$this->mValScriptWhenUnchecked = str_replace( "%26", "&", $data );
				break;
			case "SIGNED":
				if ( $wgJmolAuthorizeChoosingSignedApplet ) {
					$this->mValSigned = $data;
				}
				break;
			case "SIZE":
				$this->mValSize = $data;
				break;
			case "TARGET":
				$this->mValTarget = $data;
				break;
			case "TEXT":
				$this->mValText = $data;
				break;
			case "TITLE":
				$this->mValTitle = $data;
				break;
			case "HEADER":
				$this->mValHeader = $data;
				break;
			case "CAPTION":
				$this->mValCaption = $data;
				break;
			case "UPLOADEDFILECONTENTS":
				$this->mValUploadedFileContents = $data;
				break;
			case "URLCONTENTS":
				$this->mValUrlContents = str_replace( "%26", "&", $data );
				break;
			case "VERTICAL":
				$this->mValVertical = $data;
				break;
			case "WIKIPAGECONTENTS":
				$this->mValWikiPageContents = $data;
				break;
			case "X":
				$this->mValPositionX = $data;
				break;
			case "Y":
				$this->mValPositionY = $data;
				break;
			}
			break;
		case 4:
			// Details of sub tags
			if ( $this->mCurrentTag == "ITEM" ) {
				switch ( $this->mCurrentSubTag ) {
				case "CHECKED":
					$this->mValChecked = $data;
					break;
				case "SCRIPT":
					$this->mValScript = str_replace( "%26", "&", $data );
					break;
				case "TEXT":
					$this->mValText = $data;
					break;
				}
			}
			break;
		}
	}

	// ********* //
	// UTILITIES //
	// ********* //

	// Resets internal variables to their default values
	private function resetValues() {
		global $wgJmolDefaultAppletSize;
		global $wgJmolDefaultScript;
		global $wgJmolUsingSignedAppletByDefault;

		$this->mCurrentObject = "";
		$this->mCurrentTag = "";
		$this->mCurrentSubTag = "";

		$this->mValChecked = false;
		$this->mValColor = "black";
		$this->mValInlineContents = "";
		$this->mValItems = "";
		$this->mValMenuHeight = "1";
		$this->mValName = "";
		$this->mValPositionX = "100";
		$this->mValPositionY = "100";
		$this->mValScript = $wgJmolDefaultScript;
		$this->mValScriptWhenChecked = "";
		$this->mValScriptWhenUnchecked = "";
		$this->mValSigned = $wgJmolUsingSignedAppletByDefault;
		$this->mValSize = $wgJmolDefaultAppletSize;
		$this->mValTarget = "";
		$this->mValText = "";
		$this->mValTitle = "Jmol";
		$this->mValHeader = "";
		$this->mValCaption = "";
		$this->mValUploadedFileContents = "";
		$this->mValUrlContents = "";
		$this->mValVertical = false;
		$this->mValWikiPageContents = "";
	}

	// Functions to escape characters
	private function escapeAttribute( $value ) {
		return Xml::escapeJsString( $value );
	}
	private function escapeScript( $value ) {
		return Xml::escapeJsString( $value );
	}

	// Add a link to Javascript file in the HTML header
	private function includeScript( &$outputPage, $scriptFile ) {
		$script = "<script type='text/javascript' " .
			"src='" . $scriptFile . "'>" .
			"</script>\n";
		$outputPage->addScript( $script );
	}

	// Add a Javascript script in the HTML header
	private function addScript( &$outputPage, $scriptContents ) {
		$script = "<script type='text/javascript'>" .
			$scriptContents .
			"</script>\n";
		$outputPage->addScript( $script );
	}

	// *********************** //
	// DIRECTING THE EXTENSION //
	// *********************** //

	private function parseJmolTag( &$text, &$params, &$parser ) {
		$parser->disableCache();
		return $this->renderJmol( $text );
	}

	private function beforeHTMLOutput( &$outputPage, &$text ) {
		global $wgJmolExtensionPath;
		if ( preg_match_all( '/<!-- Jmol -->/m', $text, $matches ) === false ) {
			return true;
		}
		$this->includeScript( $outputPage, $wgJmolExtensionPath . "/Jmol.js" );
		$this->includeScript( $outputPage, $wgJmolExtensionPath . "/JmolMediaWiki.js" );
		/*  initialize now goes in the body, thus allowing signed applet
		if ( $this->mValSigned == "true" ) {
			$this->addScript( $outputPage, "jmolInitialize('" . $wgJmolExtensionPath . "', true);" );
		} else {
			$this->addScript( $outputPage, "jmolInitialize('" . $wgJmolExtensionPath . "', false);" );
		}
		*/
		return true;
	}

 	/**
	 * Initialize the parser hooks
	 */
	function setHooks() {
		global $wgParser, $wgHooks;

		$wgParser->setHook( 'jmol' , array( &$this, 'jmolTag' ) );

		$wgHooks['OutputPageBeforeHTML'][] = array( &$this, 'hOutputPageBeforeHTML' );
		$wgHooks['ParserBeforeStrip'][] = array( &$this, 'hParserBeforeStrip' );
		$wgHooks['ParserAfterStrip'][] = array( &$this, 'hParserAfterStrip' );
	}

	// ******************* //
	// MEDIAWIKI CALLBACKS //
	// ******************* //

	/**
	 * Callback function for <jmol>
	 *
	 * @param string $text Input
	 * @param array $param Arguments
	 * @return string
	 */
	public function jmolTag(&$text, &$params, &$parser) {
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmol>$text</jmol>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolTag($text, $params, $parser);
			$this->mInJmol = false;
			return $ret;
		}
	}

	// *************** //
	// MEDIAWIKI HOOKS //
	// *************** //

	/**
	 * Gets run when Parser::clearState() gets run
	 */
	function hClearState() {
		# Don't clear state when we're in the middle of parsing
		# a <jmol> tag
		if ( $this->mInJmol ) {
			return true;
		}
		resetValues();
 
		return true;
	}

	/**
	 * Hook OutputPageBeforeHTML
	 */
	function hOutputPageBeforeHTML(&$out, &$text) {
		return $this->beforeHTMLOutput($out, $text);
	}

	/**
	 * Hook ParserBeforeStrip
	 */
	function hParserBeforeStrip(&$parser, &$text, &$strip_state) {
		return true;
	}

	/**
	 * Hook ParserAfterStrip
	 */
	function hParserAfterStrip(&$parser, &$text, &$strip_state) {
		return true;
	}

	/**
	 * Hook ParserBeforeTidy
	 */
	function hParserBeforeTidy(&$parser, &$text) {
		return true;
	}

	/**
	 * Hook ParserAfterTidy
	 */
	function hParserAfterTidy(&$parser, &$text) {
		return true;
	}

	/**#@-*/

} // END CLASS DEFINITION
//</source>