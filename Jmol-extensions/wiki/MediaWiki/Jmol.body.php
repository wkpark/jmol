<?php
/**
 * Jmol extension - adds the possibility to include [http://www.jmol.org Jmol applets] in MediaWiki.
 *
 * @ingroup Extensions
 * 
 * @author Nicolas Vervelle
 * @author Angel Herraez
 * @author Jaime Prilusky
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

// require_once("Title.php");

/* Global configuration parameters */
global $wgJmolAuthorizeChoosingSignedApplet;
global $wgJmolAuthorizeUploadedFile;
global $wgJmolAuthorizeUrl;
global $wgJmolDefaultAppletSize;
global $wgJmolMaxAppletSize;
global $wgJmolDefaultScript;
global $wgJmolExtensionPath;
global $wgJmolForceNameSpace;
global $wgJmolForceHTML5;
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
		$input = "<jmol>$input</jmol>";
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

//		$this->mValInlineContents = trim( $this->mValInlineContents );
//		$this->mValInlineContents = preg_replace( "/\t/", " ", $this->mValInlineContents );
		// $this->mValInlineContents = preg_replace( "/\n/", "\\n'+\n'", $this->mValInlineContents );
		$prefix .= "<script>";
		$postfix .= "</script>";
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
		           " title='" . wfMsg( 'jmol-loadwarning' ) ."'" .
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
		$this->mOutput .= "<script>";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolButton(" .
		                      "'" . $this->escapeScript( $this->mValScript ) . "'," .
		                      "'" . $this->escapeScript( $this->mValText ) . "'";
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");</script>";
	}

	// Renders a checkbox to control a Jmol applet
	private function renderJmolCheckbox() {
		$this->mOutput .= "<script>";
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
		$this->mOutput .= ");</script>";
	}

	// Renders a link to control a Jmol applet
	private function renderJmolLink() {
		$this->mOutput .= "<script>";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolLink(" .
		                  "'" . $this->escapeScript( $this->mValScript ) . "'," .
		                  "'" . $this->escapeScript( $this->mValText ) . "'";
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");</script>";
	}

	// Renders a menu to control a Jmol applet
	private function renderJmolMenu() {
		$this->mOutput .= "<script>";
		if ( $this->mValTarget != "" ) {
			$this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $this->mValTarget ) . "');\n";
		}
		$this->mOutput .= "jmolMenu(" .
		                      "[" . $this->mValItems . "]," .
		                      $this->escapeScript( $this->mValMenuHeight );
		if ( $this->mValName != "" ) {
			$this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
		}
		$this->mOutput .= ");</script>";
	}

	// Renders a radio group to control a Jmol applet
	private function renderJmolRadioGroup() {
		$this->mOutput .= "<script>";
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
		$this->mOutput .= ");</script>";
	}

	// Internal function to make a Jmol applet
	private function renderInternalJmolApplet( $prefix, $postfix, $sep ) {
      global $wgJmolAuthorizeUrl, $wgJmolAuthorizeUploadedFile, $wgJmolDrawControls;
      global $wgJmolForceNameSpace, $wgJmolExtensionPath, $wgScriptPath;
      
      $output = $prefix;
      $output .= 'Info0.width = ' . $this->mValSize . ';Info0.height = ' . $this->mValSize . ';';
      $output .= 'Info0.color = "' . $this->mValColor . '";';
      if ( $this->mValSigned == "true" ) {
        $output .= 'Info0.jarFile = "JmolAppletSigned.jar"; Info0.isSigned = true;'; 
      } else {
        $output .= 'Info0.jarFile = "JmolApplet.jar"; Info0.isSigned = false;'; 
      }
      
// 		if ( $this->mValName != "" ) {
// 			$output .= "," . $sep . $this->escapeScript( $this->mValName ) . $sep;
// 		}

      $output .= 'jmolApplet0 = Jmol.getApplet("jmolApplet0",Info0);';
      
      if ($wgJmolDrawControls != '') {
        $output .= 
'Jmol.jmolButton(jmolApplet0,"if(_spinning);spin off;spinflag = false;else;spin on;spinflag = true;endif","toggle spin");' .
'Jmol.jmolButton(jmolApplet0,"if(antialiasDisplay);set refreshing off;antialiasDisplay = false;set refreshing on;antialiasDisplayFlag = false;else;set refreshing off;antialiasDisplay = true;set refreshing on;antialiasDisplayFlag = true;endif;set refreshing on;","toggle quality");' .
'</script><input type="button" value="popup" onClick="cloneJSmol(jmolApplet0)"><script>';
       }

      if ( $this->mValUploadedFileContents != "" ) {
			if ( $wgJmolAuthorizeUploadedFile == true ) {
				$file = wfLocalFile( $this->mValUploadedFileContents );
				if (!is_null($file)) {
				  $this->mValUrlContents = $file->getURL();
				}
			} else {
              return $this->showWarning( wfMsg( 'jmol-nouploadedfilecontents' ) );
            }
      } elseif ( $this->mValWikiPageContents != "" ) {
        if ( $wgJmolAuthorizeUrl == true ) {
          $this->mValUrlContents = $wgScriptPath."/index.php?title=";	// AH - fix for non-root wikis
          if ( $wgJmolForceNameSpace != "" ) {
            $this->mValUrlContents .= $wgJmolForceNameSpace . ":";
          }
          $this->mValUrlContents .= $this->mValWikiPageContents . "&action=raw";
		} else {
		  return $this->showWarning( wfMsg( 'jmol-nowikipagecontents' ) );
		}
      } 
      if ( $this->mValUrlContents != "" ) {
        $output .= 'Jmol.script(jmolApplet0,"set echo p 50% 50%;set echo p center;echo ' 
                   . wfMsg( 'jmol-loading' ) . ';refresh;load ' . $this->escapeScript( $this->mValUrlContents ) . ';' 
                   . $this->escapeScript( $this->mValScript ) . '"); ';
      } elseif ( $this->mValInlineContents != "" ) { // mValInlineContents
      
        return $this->showWarning( "inline contents are not implemented" );
        
        $this->mValInlineContents = preg_replace( "/\n/", "\\n\"+\n\"", $this->mValInlineContents );
        $output .= "\nvar s = \"" . $this->mValInlineContents . "\";\n"
                . "function loadInline(d) { return 'data \"model\"\\n' + d + '\\nend \"model\";'}\n"
                . 'Jmol.script(jmolApplet0,loadInline(s) + ' 
                . "'" . $this->escapeScript( $this->mValScript ) . "'" . '); ';
                
      } else {
        $output .= 'Jmol.script(jmolApplet0,"' . $this->escapeScript( $this->mValScript ) . '"); ';
      }
      return $output . $postfix;
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
		global $wgJmolAuthorizeChoosingSignedApplet,$wgJmolMaxAppletSize;

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
					$this->mValInlineContents .= "\n" . addslashes($data);
				}
				break;
			case "MENUHEIGHT":
				$this->mValMenuHeight = $data;
				break;
			case "NAME":
// TEMPORARY needs implementation of global management
// 				$this->mValName = $data;
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
			    if ($data > $wgJmolMaxAppletSize) { $data = $wgJmolMaxAppletSize; }
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

	// *********************** //
	// DIRECTING THE EXTENSION //
	// *********************** //

	private function parseJmolTag( $text, $params, $parser ) {
		global $wgJmolExtensionPath, $wgJmolScriptVersion;
		$parser->disableCache();
        $this->initializeJSmol($parser);
		// Add element to display file
		return $this->renderJmol( $text );
	}

	private function parseJmolFileTag( $text, $params, $parser ) {
		global $wgJmolExtensionPath, $wgJmolScriptVersion, $wgJmolAuthorizeUploadedFile;
        $this->initializeJSmol($parser);
		// Add element to display file
		$result =
			"<a href=\"javascript:void(0)\"" .
			  " onclick=\"jmolWikiPopupWindow(" .
				"'" . $this->escapeScript( $text ) . "'," .
				"'" . $this->escapeScript( "800" ) . "'," .
				"'" . $this->escapeScript( "50" ) . "'," .
				"'" . $this->escapeScript( "50" ) . "'," .
				"'";
		$result .= "jmolInitialize(\\'" . $wgJmolExtensionPath . "\\', false); ";
		$result .= "_jmol.noEval = true; ";
		$result .= "jmolCheckBrowser(\\'popup\\', \\'" . $wgJmolExtensionPath . "/browsercheck\\', \\'onclick\\'); ";
		if ( $wgJmolAuthorizeUploadedFile != true ) {
			return $this->showWarning( wfMsg( 'jmol-nouploadedfilecontents' ) );
		}
		$file = wfLocalFile( $this->mValUploadedFileContents );
		if ( !is_null( $file )) {
			$file = new Image($title);
			$result .= "jmolApplet( 500, \\'" .
				"set echo p 50% 50%;" .
				"set echo p center;" .
				"echo " . wfMsg( 'jmol-loading' ) . ";" .
				"refresh;" .
				"load " . $file->getURL() . ";\\' ); ";
		}
		$result .= "');\">";
		if ( isset( $params[ "text" ] ) ) {
			$result .= $params[ "text" ];
		} else {
			$result .= $text;
		}
		$result .= "</a>";

		return $result;
	}

	private function parseJmolPdbTag( $text, $params, $parser ) {
		global $wgJmolExtensionPath, $wgJmolScriptVersion;
        $this->initializeJSmol($parser);
		// Add element to display file
		$result =
			"<a href=\"javascript:void(0)\"" .
			  " onclick=\"jmolWikiPopupWindow(" .
				"'" . $this->escapeScript( $text ) . "'," .
				"'" . $this->escapeScript( "800" ) . "'," .
				"'" . $this->escapeScript( "50" ) . "'," .
				"'" . $this->escapeScript( "50" ) . "'," .
				"'";
		$result .= "jmolInitialize(\\'" . $wgJmolExtensionPath . "\\', true); ";
		$result .= "_jmol.noEval = true; ";
		$result .= "jmolCheckBrowser(\\'popup\\', \\'" . $wgJmolExtensionPath . "/browsercheck\\', \\'onclick\\'); ";
		$result .= "jmolApplet( 500, \\'" .
			"set echo p 50% 50%;" .
			"set echo p center;" .
			"echo " . wfMsg( 'jmol-loading' ) . ";" .
			"refresh;" .
			"load =" . $text . ";\\' ); ";
		$result .= "');\">";
		if ( isset( $params[ "text" ] ) ) {
			$result .= $params[ "text" ];
		} else {
			$result .= $text;
		}
		$result .= "</a>";

		return $result;
	}

	private function parseJmolSmilesTag( $text, $params, $parser ) {
		global $wgJmolExtensionPath, $wgJmolScriptVersion;
        $this->initializeJSmol($parser);
		// Add element to display file
		$result =
			"<a href=\"javascript:void(0)\"" .
			  " onclick=\"jmolWikiPopupWindow(" .
				"'" . $this->escapeScript( $text ) . "'," .
				"'" . $this->escapeScript( "800" ) . "'," .
				"'" . $this->escapeScript( "50" ) . "'," .
				"'" . $this->escapeScript( "50" ) . "'," .
				"'";
		$result .= "jmolInitialize(\\'" . $wgJmolExtensionPath . "\\', true); ";
		$result .= "_jmol.noEval = true; ";
		$result .= "jmolCheckBrowser(\\'popup\\', \\'" . $wgJmolExtensionPath . "/browsercheck\\', \\'onclick\\'); ";
		$result .= "jmolApplet( 500, \\'" .
			"set echo p 50% 50%;" .
			"set echo p center;" .
			"echo " . wfMsg( 'jmol-loading' ) . ";" .
			"refresh;" .
			"load \$" . $text . ";\\' ); ";
		$result .= "');\">";
		if ( isset( $params[ "text" ] ) ) {
			$result .= $params[ "text" ];
		} else {
			$result .= $text;
		}
		$result .= "</a>";

		return $result;
	}

 	/**
	 * Initialize the parser hooks
	 */
	function setHooks() {
		global $wgParser, $wgHooks;
		global $wgJmolAuthorizeJmolFileTag,
			   $wgJmolAuthorizeJmolPdbTag,
			   $wgJmolAuthorizeJmolSmilesTag,
			   $wgJmolAuthorizeJmolTag;

		if ( $wgJmolAuthorizeJmolTag == true ) {
			$wgParser->setHook( 'jmol' , array( &$this, 'jmolTag' ) );
		}
		if ( $wgJmolAuthorizeJmolFileTag == true ) {
			$wgParser->setHook( 'jmolFile', array( &$this, 'jmolFileTag' ) );
		}
		if ( $wgJmolAuthorizeJmolPdbTag == true ) {
			$wgParser->setHook( 'jmolPdb', array( &$this, 'jmolPdbTag' ) );
		}
		if ( $wgJmolAuthorizeJmolSmilesTag == true ) {
			$wgParser->setHook( 'jmolSmiles', array( &$this, 'jmolSmilesTag' ) );
		}
	}

	// ******************* //
	// MEDIAWIKI CALLBACKS //
	// ******************* //

	/**
	 * Callback function for <jmol>
	 *
	 * @param string $text Text inside <jmol> tag
	 * @param array $param Arguments inside <jmol> tag
	 * @param Parser &$parser Parser
	 * @return string
	 */
	public function jmolTag( $text, $params, $parser ) {
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmol>$text</jmol>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolTag( $text, $params, $parser );
			$this->mInJmol = false;
			return $ret;
		}
	}

	/**
	 * Callback function for <jmolFile>
	 *
	 * @param string $text Text inside <jmolFile> tag
	 * @param array $param Arguments inside <jmolFile> tag
	 * @param Parser &$parser Parser
	 * @return string
	 */
	public function jmolFileTag( $text, $params, $parser ) {
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmolFile>$text</jmolFile>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolFileTag( $text, $params, $parser );
			$this->mInJmol = false;
			return $ret;
		}
	}

	/**
	 * Callback function for <jmolPdb>
	 *
	 * @param string $text Text inside <jmolPdb> tag
	 * @param array $param Arguments inside <jmolPdb> tag
	 * @param Parser &$parser Parser
	 * @return string
	 */
	public function jmolPdbTag( $text, $params, $parser ) {
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmolPdb>$text</jmolPdb>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolPdbTag( $text, $params, $parser );
			$this->mInJmol = false;
			return $ret;
		}
	}

	/**
	 * Callback function for <jmolSmiles>
	 *
	 * @param string $text Text inside <jmolSmiles> tag
	 * @param array $param Arguments inside <jmolSmiles> tag
	 * @param Parser &$parser Parser
	 * @return string
	 */
	public function jmolSmilesTag( $text, $params, $parser ) {
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmolSmiles>$text</jmolSmiles>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolSmilesTag( $text, $params, $parser );
			$this->mInJmol = false;
			return $ret;
		}
	}

function initializeJSmol($parser) {
  global $wgOut,$wgRequest,$wgScriptPath,$initializeJSmolDone,$reqUse;
  global $wgJmolDefaultAppletSize,$wgJmolForceHTML5,$wgJmolMaxAppletSize;
  if ($initializeJSmolDone == false) {
    $wikiDir = dirname(__FILE__);
    $jsmolDir = dirname($wikiDir);
    $extensionsDir = dirname($jsmolDir);
    $mediawikiDir = dirname($extensionsDir);
    require_once ($mediawikiDir . '/includes/Title.php');
    require_once ($wikiDir . '/Mobile_Detect.php');
    if (empty($reqUse)) { $reqUse = 'HTML5';}
    if ($wgJmolForceHTML5 == true) { $reqUse = 'HTML5';}
    $reqUse = strtoupper($wgRequest->getVal('_USE',$reqUse));
    $detect = new Mobile_Detect;
    $reqUse = ($detect->isMobile()) ? 'HTML5' : $reqUse;
    $deferApplet = 'false'; // ($detect->isMobile() ? ($detect->isTablet() ? 'false' : 'true') : 'false');
    if( $detect->isMobile() && !$detect->isTablet() ) { $wgJmolMaxAppletSize = 300; }
    $jsmolPath = $wgScriptPath . '/extensions/jsmol';
    $isSigned = ($this->mValSigned == "true") ? "true" : "false";
    if ($reqUse == "SIGNED") {$isSigned = "true"; $reqUse = "JAVA";} 
    $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/JSmol.min.js></script>');
    $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/js/Jmol2.js></script>');
    $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/wiki/JSmolPopup.js></script>');
    if ($reqUse == "WEBGL") {
      $isSigned = "false";
      $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/js/JSmolThree.js></script>');
      $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/js/JSmolGLmol.js></script>');
    }    
    $jarFile = ($isSigned == "true") ? "JmolAppletSigned.jar" : "JmolApplet.jar";
    $wgOut->addHTML('<script>//<![CDATA[
var Info0 = {
use: "' . $reqUse . '",
disableJ2SLoadMonitor: true, 
disableInitialConsole: true, 
jarPath: "' . $jsmolPath . '/java",
j2sPath: "' . $jsmolPath . '/j2s",
jarFile: "' . $jarFile . '",
isSigned: ' . $isSigned . ',
serverURL: "' . $jsmolPath . '/php/jsmol.php",
coverCommand: null,
coverImage: null,
addSelectionOptions: false,
readyFunction: null,
defaultModel: null,
coverTitle: "Loading ... Please wait.",  // tip that is displayed before model starts to load
deferApplet: ' . $deferApplet . ', // true == the model should not be loaded until the image is clicked 
deferUncover: false, // true == the image should remain until command execution is complete
width:' . $wgJmolDefaultAppletSize . ', height:' . $wgJmolDefaultAppletSize . '
};
Jmol._alertNoBinary = false;
//]]>
</script>');
    $initializeJSmolDone = true;
  }
  return true;
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

	/**#@-*/

} // END CLASS DEFINITION
//</source>
