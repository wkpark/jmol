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

// Set 1 2014 J Prilusky

if (!defined('MEDIAWIKI')) {
  die('This file is a MediaWiki extension, it is not a valid entry point');
}

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
	var $mInJmol = false;
	var $mOutput, $mDepth;
	var $mCurrentObject, $mCurrentTag, $mCurrentSubTag;

	var $mValCaption;
	var $mValChecked;
	var $mValColor;
	var $mValControls;
	var $mValHeader;
	var $mValFloat;
	var $mValFrame;
	var $mValInlineContents;
	var $mValItems;
	var $mValMenuHeight;
	var $mValName;
	var $mValPlatformSpeed;
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
	var $mValUploadedFileContents;
	var $mValUrlContents;
	var $mValVertical;
	var $mValWikiPageContents;

  function __construct() {
    $this->mOutput = "";
    $this->mDepth = 0;
    $this->resetValues();
    $this->setHooks();
  }

  private function renderJmol( $input ) { // Render Jmol tag
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

  private function renderJmolApplet() { // Renders a Jmol applet directly in the Wiki page
    global $wgJmolAppletID;
    $canvas = $this->appletCanvasRow(); // sets $wgJmolAppletID
    $class = ($this->mValFrame) ? "class='lightborder'" : "";
    $float = ($this->mValFloat) ? "style='float:" . $this->mValFloat . ";margin:8px;'" : ""; 
    $this->mOutput .= "<table $float $class border='0' cellspacing='0' cellpadding='0' width='" . $this->mValSize . "'>"
      . $this->appletTitleRow()
      . $canvas
      . $this->appletControlsRow()
      . $this->appleCaptionRow()
      . "</table>";
    return;
  }

  private function appletCanvasRow() {
    $prefix = "<script>"; $postfix = "</script>";
    return "<tr><td>" . $this->renderInternalJmolApplet( $prefix, $postfix, "'" ) . "</td></tr>";
    // "<div id='colorKeyDiv" . appletSuffix . "' name='colorKeyDiv'></div>"
  }

  private function appletTitleRow() {
    global $wgJmolAppletID;
    $titleRow = null;
    if ($this->mValTitle and !$this->mValInlineContents) {  // TEMPORARY can't display <title> with <inlineContents>
      $content = $this->renderWikiTextString($this->mValTitle);
      $titleRow = '<tr><td><div style="' .  $this->escapeAttribute($this->mValTitleCSS) . '" name="title' . $wgJmolAppletID . '" id="title' . $wgJmolAppletID . '">' . $content . '</div></td></tr>'; 
    }
    return $titleRow;
  }

  private function appletControlsRow() {
    global $wgJmolAppletID;
    $controlsRow = null; $content = null;
    if ($this->mValControls) {
      $jmolApplet = "jmolApplet" . $wgJmolAppletID;
      $popupTitle = $this->mValTitle; 
      if (empty($popupTitle)) { $popupTitle = $this->mValName; }
      if (empty($popupTitle)) { $popupTitle = $jmolApplet; }
      if (strpos($this->mValControls,'all') !== false) { $this->mValControls = 'spin quality popup full'; }
      $this->mValControls = preg_replace('/\s+/',' ',$this->mValControls);
      $controls = explode(' ',$this->mValControls);
      foreach ($controls as $request) {
        switch($request) {
          case "spin":
            $content .= '<script>Jmol.jmolButton(' . $jmolApplet . ',"if(_spinning);spin off;spinflag = false;else;spin on;spinflag = true;endif","toggle spin");</script>' ;
            break;
          case "popup":
            $content .= '<input type="button" value="popup" onclick="cloneJSmol(' . $jmolApplet . ',\'' . $popupTitle . '\')">' ;
            break;
          case "quality":
            $content .= '<script>Jmol.jmolButton(' . $jmolApplet . ',"set refreshing off;if(antialiasDisplay);antialiasDisplay = false;antialiasDisplayFlag = false;else;antialiasDisplay = true;antialiasDisplayFlag = true;endif;set refreshing on;refresh;","toggle quality");</script>';
            break;
          case "full":
            $content .= '<input type="button" value="load full" id="fullloadbutton" style="background:#FBBC40;background:linear-gradient(#FDDEA0,#FBBC40);padding:2px;border-radius:4px; border:1px solid #666;display:none;" onclick="this.style.display=\'none\';Jmol.script(' . $jmolApplet . ',\'set echo off; set echo loading 50% 50%; set echo loading center; color echo [xffa500]; background echo translucent 0.7 gray; echo Loading full model ...; refresh; load;script /wiki/extensions/Proteopedia/spt/initialview01.spt;\');">';
            break;
        }
      }
      $controlsRow = "<tr><td>" . $content  . "</td></tr>";
    }
    return $controlsRow;
  }
  
  private function appleCaptionRow() {
    global $wgParser,$wgTitle,$wgUser,$wgJmolAppletID;
    $captionRow = null;
    if ($this->mValCaption) {  
      $content = $this->renderWikiTextString($this->mValCaption);
      $captionRow = '<tr><td><div style="' . $this->escapeAttribute($this->mValCaptionCSS) . '" name="caption' . $wgJmolAppletID . '" id="caption' . $wgJmolAppletID . '">' . $content . '</div></td></tr>'; 
    }
    return $captionRow;
  }
  
  private function appletSetJmolAppletID() {
    global $wgJmolAppletID,$wgJmolNumID,$wgJmolPageHasApplet;
    if (!empty($this->mValName)) {
      $wgJmolAppletID = $this->mValName;
    } else {
      if (empty($wgJmolNumID)) {  
         $wgJmolNumID++; 
         $wgJmolAppletID = $wgJmolNumID; $wgJmolPageHasApplet = true;
      } else {
        if ($wgJmolPageHasApplet == false) { 
          $wgJmolPageHasApplet = true;
        } else { 
          $wgJmolNumID++; 
          $wgJmolAppletID = $wgJmolNumID;
        }
      }
    }
//     $callers=debug_backtrace();
//     print "DEB APPL " . $callers[1]['function'] . " $wgJmolAppletID<br>\n";
    return;
  }
  
  private function linkSetJmolAppletID() {
    global $wgJmolAppletID,$wgJmolNumID,$wgJmolPageHasApplet;
    if (!empty($this->mValTarget)) {
      $wgJmolAppletID = $this->mValTarget;
      $this->mValTarget = null;
    } else {
      if ($wgJmolPageHasApplet == false) {
        if (empty($wgJmolNumID)) { $wgJmolNumID++; }
      }
      $wgJmolAppletID = $wgJmolNumID;
    }
//     $callers=debug_backtrace();
//     print "DEB LINK " . $callers[1]['function'] . " $wgJmolAppletID<br>\n";
    return;
  }

  private function renderJmolAppletButton() { // Renders a button in the Wiki page that will open a new window containing a Jmol applet
    $this->setValUrlContents(); 
    $postfix = "";
    $prefix  = "<input type='button'";
    if ( !empty($this->mValName) ) {
      $prefix .= " name='" . $this->escapeAttribute( $this->mValName ) . "'" .
      " id='" . $this->escapeAttribute( $this->mValName ) . "'";
    }
    if ( empty($this->mValText) ) { $this->mValText = $this->mValTitle; }
    if ( !empty($this->mValText) ) { $prefix .= " value='" . $this->escapeAttribute( $this->mValText ) . "'"; }
    $prefix .= " href=\"javascript:void(0)\"" .
    " onclick=\"popupJSmol('Info0','" . $this->escapeScript( $this->mValUrlContents ) . $this->escapeScript( $this->mValName ) . "')\"";
    $postfix = "');return true\" />";
    $this->mOutput .= $this->renderInternalJmolApplet( $prefix, $postfix, "\\'" );
  }

  private function renderJmolAppletLink() { // Renders a link in the Wiki page that will open a new window containing a Jmol applet
    global $wgJmolExtensionPath;
    $this->setValUrlContents(); 
    $prefix .= "<a";
    $postfix = "";
    if ( !empty($this->mValName) ) {
      $prefix .= " name='" . $this->escapeAttribute( $this->mValName ) . "'" .
      " id='" . $this->escapeAttribute( $this->mValName ) . "'";
    }
    $prefix .= " href=\"javascript:void(0)\"" .
    " onclick=\"popupJSmol('Info0','" . $this->escapeScript( $this->mValUrlContents ) . "')\"";
    $postfix .= "');\">";
    if ( empty($this->mValText) ) { $this->mValText = $this->mValTitle; }
    if ( !empty($this->mValText)) { $postfix .= $this->mValText; }
    $postfix .= "</a>";
    $this->mOutput .= $prefix . $postfix;
  }

// 	private function renderJmolAppletInlineLink() { // Renders a link in the Wiki page that will insert a div containing a Jmol applet
// 		global $wgJmolExtensionPath;
// 		$prefix = "";
// 		$postfix = "";
// 		$uniqueID = rand(10000,99999);
// 		$hidelink = "[<a href='javascript:void(0)' " .
// 		            " title='hide the Jmol applet'" .
// 		            " style='font-family:Verdana, Arial, Helvetica, sans-serif;'" .
// 		            " onclick='jmolWikiPopInlineHide(\"" . $uniqueID . "\")'>x</a>]";
// 		$prefix .= "<div style='width:".$this->mValSize."px; text-align:center;'>";
// 
// 		if ( $this->mValHeader != "" ) {	
// 			$prefix .= "<div style='font-weight:bold; position:relative; padding-right:2ex;'>" .
// 			           $this->mValHeader .
// 			           "<span id='JmolInlineHide" . $uniqueID . "'" .
// 			           " style='display:none; position:absolute; right:0; bottom:0; font-weight:normal;'>" .
// 			           $hidelink .
// 			           "</span>" .
// 			           "</div>";
// 		} else {
// 			$prefix .= "<div>" .
// 			           "<span id='JmolInlineHide" . $uniqueID . "'" .
// 			           " style='display:none; float:right;'>" .
// 			           $hidelink .
// 			           "</span>" .
// 			           "</div>";
// 		}
// 
// 		$prefix .= "<a id='JmolInlineLink" . $uniqueID . "'";
// 		if ( $this->mValName != "" ) {
// 			$prefix .= " name='" . $this->escapeAttribute( $this->mValName ) . "'" .
// 			           " id='" . $this->escapeAttribute( $this->mValName ) . "'";
// 		}
// 		$prefix .= " href='javascript:void(0)' " .
// 		           " title='" . wfMsg( 'jmol-loadwarning' ) ."'" .
// 		           " onclick=\"jmolWikiPopInline('" . $uniqueID . "','";
// 
// 		$postfix .= "');\"";
// 		$postfix .= ">";
// 
// 		if ( $this->mValText == "" ) {
// 			$this->mValText = "Jmol";
// 		}
// 		$postfix .= $this->mValText;
// 
// 		$postfix .= "</a>";
// 		$postfix .= "<div id='JmolInlineEnv" . $uniqueID . "'></div>";
// 		/*  style='z-index:5;position:absolute;vertical-align:top;' */
// 		$postfix .= "<div style='font-size:0.85em; line-height:1.2; text-align:left; margin:0.3em 1ex;'>" .
// 		            $this->mValCaption . "</div></div>";
// 
// 		$this->mOutput .= $this->renderInternalJmolApplet( $prefix, $postfix, "\\'" );
// 	}

  private function renderJmolButton() { // Renders a button to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script>";
    $this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $wgJmolAppletID ) . "');";
    $this->mOutput .= "jmolButton(" .
    "'" . $this->escapeScript( $this->mValScript ) . "'," .
    "'" . $this->escapeScript( $this->mValText ) . "'";
    if ( !empty($this->mValName) ) { $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'"; }
    $this->mOutput .= ");</script>";
  }

  private function renderJmolCheckbox() { // Renders a checkbox to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script>";
    $this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $wgJmolAppletID ) . "');";
    $this->mOutput .= "jmolCheckbox(" .
    "'" . $this->escapeScript( $this->mValScriptWhenChecked ) . "'," .
    "'" . $this->escapeScript( $this->mValScriptWhenUnchecked ) . "'," .
    "'" . $this->escapeScript( $this->mValText ) . "'";
    if ( $this->mValChecked == "true" ) {
      $this->mOutput .= ",true";
    } else {
      $this->mOutput .= ",false";
    }
    if ( $this->mValName != "" ) { $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'"; }
    $this->mOutput .= ");</script>";
  }

  private function renderJmolLink() { // Renders a link to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script>";
    $this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $wgJmolAppletID ) . "');";
    $this->mOutput .= "jmolLink(" .
    "'" . $this->escapeScript( $this->mValScript ) . "'," .
    "'" . $this->escapeScript( $this->mValText ) . "'";
    if ( $this->mValName != "" ) {
      $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
    }
    $this->mOutput .= ");</script>";
  }

  private function renderJmolMenu() { // Renders a menu to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script>";
    $this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $wgJmolAppletID ) . "');";
    $this->mOutput .= "jmolMenu(" .
    "[" . $this->mValItems . "]," .
    $this->escapeScript( $this->mValMenuHeight );
    if ( $this->mValName != "" ) { $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'"; }
    $this->mOutput .= ");</script>";
  }

  private function renderJmolRadioGroup() { // Renders a radio group to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script>";
    $this->mOutput .= "jmolSetTarget('" . $this->escapeScript( $wgJmolAppletID ) . "');";
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

  private function renderInternalJmolApplet( $prefix, $postfix, $sep ) { // Internal function to make a Jmol applet
      global $wgJmolAuthorizeUrl, $wgJmolAuthorizeUploadedFile, $wgJmolCoverImageGenerator;
      global $wgJmolForceNameSpace, $wgJmolExtensionPath, $wgScriptPath, $wgJmolPlatformSpeed;
      global $wgJmolAppletID,$wgJmolNumID,$wgJmolPageHasApplet;

      $this->appletSetJmolAppletID(); $wgJmolPageHasApplet = true;
      $jmolApplet = "jmolApplet" . $wgJmolAppletID;
      
      if ($this->mValPlatformSpeed) { // user requested platformSpeed
        $wgJmolPlatformSpeed = $this->mValPlatformSpeed;
      }
      $output = $prefix;
      $output .= 'Info0.width = ' . $this->mValSize . ';Info0.height = ' . $this->mValSize . ';';
      $output .= 'Info0.color = "' . $this->mValColor . '";';
      $output .= 'Jmol.getApplet("' . $jmolApplet . '",Info0);Jmol.script(' . $jmolApplet . ',"set platformSpeed ' . $wgJmolPlatformSpeed . '; DEFAULTPSPEED = ' . $wgJmolPlatformSpeed . '; ");';

      $this->setValUrlContents();
      
      if ( $this->mValUrlContents != "" ) {
        $output .= 'Jmol.script(' . $jmolApplet . ',"set echo p 50% 50%;set echo p center;echo ' 
                   . wfMsg( 'jmol-loading' ) . ';refresh;load ' . $this->escapeScript( $this->mValUrlContents ) . ';' 
                   . $this->escapeScript( $this->mValScript ) . '"); ';
      } elseif ( $this->mValInlineContents != "" ) { // mValInlineContents
 //       return $this->showWarning( "inline contents are not implemenmented" );
        $this->mValInlineContents = preg_replace( "/\n/", "\\n\"+\n\"", $this->mValInlineContents );
        $output .= "\nvar s = \"" . $this->mValInlineContents . "\";\n"
                . "function loadInline(d) { return 'data \"model\"' + d + '\\nend \"model\";'}\n"
 //               . "Jmol.script(jmolApplet0,loadInline(s).replace(/\\n/g,'|')) + " 
                . "Jmol.script(" . $jmolApplet . ",loadInline(s) + " 
                . "'" . $this->escapeScript( $this->mValScript ) . "'" 
               . '); ';
                
      } else {
        $output .= 'Jmol.script(' . $jmolApplet . ',"' . $this->escapeScript( $this->mValScript ) . '"); ';
      }
      
//       if ($wgJmolCoverImageGenerator) {
//         $cmd = str_replace(" ","+",$wgJmolCoverImageGenerator . $this->escapeScript( $this->mValScript ));
//         $output .= "Info0.coverImage = \"$cmd\";\n";
//       }

    return $output . $postfix;
  }

	private function showWarning( $message ) { // Function called for outputing a warning
		global $wgJmolShowWarnings;

		$output = "";
		if ( $wgJmolShowWarnings == true ) {
			$output .= $message;
		}
		return $output;
	}

	function startElement( $parser, $name, $attrs ) { // Function called when an opening XML tag is found
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

	function endElement( $parser, $name ) { // Function called when a closing XML tag is found
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
// 				$this->renderJmolAppletInlineLink();
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

	function characterData( $parser, $data ) { // Function called for the content of a XML tag
		global $wgJmolAuthorizeChoosingSignedApplet,$wgJmolMaxAppletSize;

		switch ( $this->mDepth ) {
		case 3:
			// Details of the interesting tags
			switch ( $this->mCurrentTag ) {
			case "CAPTION":
				$this->mValCaption .= $data;
				break;
			case "CAPTIONCSS":
				$this->mValCaptionCSS = $data;
				break;
			case "CHECKED":
				$this->mValChecked = $data;
				break;
			case "COLOR":
				$this->mValColor = $data;
				break;
			case "CONTROLS":
				$this->mValControls = trim($data);
				break;
			case "FLOAT":
				$this->mValFloat = trim($data);
				break;
			case "FRAME":
				$this->mValFrame = $data; 
				break;
			case "HEADER":
				$this->mValHeader = $data;
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
				$this->mValName = $data;
				break;
			case "PSPEED":
				$this->mValPlatformSpeed = intval($data);
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
// 			case "SIGNED":
// 				if ( $wgJmolAuthorizeChoosingSignedApplet ) {
// 					$this->mValSigned = $data;
// 				}
// 				break;
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
			case "TITLECSS":
				$this->mValTitleCSS = $data;
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

    function setValUrlContents ($text = null,$type = null) {
      global $wgJmolAuthorizeUrl, $wgJmolAuthorizeUploadedFile,$wgJmolForceNameSpace;
        
      if ($text) {
        if ($type === 'pdb') {
          $this->mValUrlContents = "http://proteopedia.org/cgi-bin/getlateststructure?" . $text . ".gz"; return;
        } elseif ($type === 'smiles') {
          $this->mValUrlContents = "\$" . $text; return;
        } else {
          $this->mValUploadedFileContents = $text;
        }
      }
      
      if ( $this->mValUploadedFileContents != "" ) {
        if ( $wgJmolAuthorizeUploadedFile == true ) {
          $file = wfLocalFile( $this->mValUploadedFileContents );
          if (!is_null($file) and $file->exists()) {
            $this->mValUrlContents = $file->getURL();
          } else {
            $this->mValUrlContents = "MSG " . wfMsg( 'jmol-requesteddatanotfound' );
          }
        } else {
          $this->mValUrlContents = "MSG " . wfMsg( 'jmol-nouploadedfilecontents' );
        }
      } elseif ( $this->mValWikiPageContents != "" ) {
        if ( $wgJmolAuthorizeUrl == true ) {
          $this->mValUrlContents = $wgScriptPath."/index.php?title=";	// AH - fix for non-root wikis
          if ( $wgJmolForceNameSpace != "" ) {
            $this->mValUrlContents .= $wgJmolForceNameSpace . ":";
          }
          $this->mValUrlContents .= $this->mValWikiPageContents . "&action=raw";
        } else {
          $this->mValUrlContents = "MSG " . wfMsg( 'jmol-nowikipagecontents' );
        }
      } 
      return;
    }

	private function resetValues() { // Resets internal variables to their default values
		global $wgJmolDefaultAppletSize;
		global $wgJmolDefaultScript;
		global $wgJmolUsingSignedAppletByDefault;
		global $wgJmolDefaultCaptionCSS;
		global $wgJmolDefaultTitleCSS;

		$this->mCurrentObject = "";
		$this->mCurrentTag = "";
		$this->mCurrentSubTag = "";

		$this->mValCaption = "";
		$this->mValCaptionCSS = $wgJmolDefaultCaptionCSS; 
		$this->mValChecked = false;
		$this->mValColor = "black";
		$this->mValFloat = "";
		$this->mValFrame = "";
		$this->mValHeader = "";
		$this->mValInlineContents = "";
		$this->mValItems = "";
		$this->mValMenuHeight = "1";
		$this->mValName = "";
		$this->mValPlatformSpeed = null;
		$this->mValPositionX = "100";
		$this->mValPositionY = "100";
		$this->mValScript = $wgJmolDefaultScript;
		$this->mValScriptWhenChecked = "";
		$this->mValScriptWhenUnchecked = "";
		$this->mValSigned = $wgJmolUsingSignedAppletByDefault;
		$this->mValSize = $wgJmolDefaultAppletSize;
		$this->mValTarget = "";
		$this->mValText = "";
		$this->mValTitle = "";
		$this->mValTitleCSS = $wgJmolDefaultTitleCSS; 
		$this->mValUploadedFileContents = "";
		$this->mValUrlContents = "";
		$this->mValVertical = false;
		$this->mValWikiPageContents = "";
        $this->mValControls = null;
	}

  function renderWikiTextString( $value ) {
    global $wgParser;
//    global $wgTitle, $wgUser;
//    $value = $this->escapeAttribute( $value );
    $wgParser->disableCache(); 
    $lparse = clone $wgParser;
//    $opt = ParserOptions::newFromUser($wgUser);  
//    $value = $lparse->parse($value,$wgTitle, $opt, true, false)->getText();
    $value = $lparse->parse($value,$wgParser->mTitle,$wgParser->mOptions)->getText();
    return $value;
  }

	private function escapeAttribute( $value ) { // Functions to escape characters
		return Xml::escapeJsString( $value );
	}
	
	private function escapeScript( $value ) {
		return Xml::escapeJsString( $value );
	}

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
      $this->setValUrlContents($text);
      $result = "<a href=\"javascript:void(0)\"" .
                " onclick=\"popupJSmol('Info0','" . $this->escapeScript( $this->mValUrlContents ) . "');\">";
		if ( isset( $params[ "text" ] ) ) {
			$result .= $params[ "text" ];
		} else {
			$result .= $text;
		}
		$result .= "</a>";
		return $result;
	}

	private function parseJmolPdbTag( $text, $params, $parser ) {
      global $wgJmolExtensionPath, $wgJmolScriptVersion, $wgJmolAuthorizeUploadedFile;
      $this->initializeJSmol($parser);
      $this->setValUrlContents($text,'pdb');
      $result = "<a href=\"javascript:void(0)\"" .
                " onclick=\"popupJSmol('Info0','" . $this->escapeScript( $this->mValUrlContents ) . "');\">";
		if ( isset( $params[ "text" ] ) ) {
			$result .= $params[ "text" ];
		} else {
			$result .= $text;
		}
		$result .= "</a>";
		return $result;
	}

	private function parseJmolSmilesTag( $text, $params, $parser ) {
      global $wgJmolExtensionPath, $wgJmolScriptVersion, $wgJmolAuthorizeUploadedFile;
      $this->initializeJSmol($parser);
      $this->setValUrlContents($text,'smiles');
      $result = "<a href=\"javascript:void(0)\"" .
                " onclick=\"popupJSmol('Info0','" . $this->escapeScript( $this->mValUrlContents ) . "');\">";
		if ( isset( $params[ "text" ] ) ) {
			$result .= $params[ "text" ];
		} else {
			$result .= $text;
		}
		$result .= "</a>";
		return $result;
	}

	function setHooks() { // Initialize the parser hooks
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

	public function jmolTag( $text, $params, $parser ) { // <jmol>
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmol>$text</jmol>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolTag( $text, $params, $parser );
			$this->mInJmol = false;
			return $ret;
		}
	}

	public function jmolFileTag( $text, $params, $parser ) { // <jmolFile>
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmolFile>$text</jmolFile>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolFileTag( $text, $params, $parser );
			$this->mInJmol = false;
			return $ret;
		}
	}

	public function jmolPdbTag( $text, $params, $parser ) { // <jmolPdb>
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmolPdb>$text</jmolPdb>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolPdbTag( $text, $params, $parser );
			$this->mInJmol = false;
			return $ret;
		}
	}

	public function jmolSmilesTag( $text, $params, $parser ) { // <jmolSmiles>
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
  global $wgOut,$wgRequest,$wgUser,$wgScriptPath,$initializeJSmolDone,$reqUse,$wgJmolAppletID,$wgJmolPageHasApplet;
  global $wgJmolDefaultAppletSize,$wgJmolForceHTML5,$wgJmolMaxAppletSize,$wgJmolPlatformSpeed;
  global $wgJmolScriptVersion;
  if ($initializeJSmolDone == false) {
    $wikiDir = dirname(__FILE__);
    $jsmolDir = dirname($wikiDir);
    $extensionsDir = dirname($jsmolDir);
    $mediawikiDir = dirname($extensionsDir);
    require_once ($mediawikiDir . '/includes/Title.php');
    require_once ($wikiDir . '/Mobile_Detect.php');
// rendering engine to use
    if (empty($reqUse)) { $reqUse = 'HTML5';}
    if ($wgJmolForceHTML5 == true) { $reqUse = 'HTML5';}
    $reqUse = strtoupper($wgRequest->getVal('use',$wgRequest->getVal('USE',$wgRequest->getVal('_use',$wgRequest->getVal('_USE',$reqUse)))));
    $detect = new Mobile_Detect;
    if (($wgUser->getOption('jmolusejava') == 1) and !$detect->isMobile()) { $reqUse = 'SIGNED'; } // instead of JAVA
    $reqUse = ($detect->isMobile()) ? 'HTML5' : $reqUse;
    $wgJmolNumID = nul;
    $wgJmolAppletID = null;
    $wgJmolPageHasApplet = false;
    
// set $wgJmolPlatformSpeed based on platform and rendering engine
    if ($detect->isMobile()) {
      if ($detect->isTablet()) {
        $wgJmolPlatformSpeed = 5;    // mid simplified rendering
      } else { // must be a phone
          $wgJmolPlatformSpeed = 2;  // simplified rendering
      }
    } else {
      if ($reqUse == 'HTML5') {
        $wgJmolPlatformSpeed = 5;      // mid simplified rendering
      } else {
        $wgJmolPlatformSpeed = 8;      // full rendering
      }
    }   
     
    $deferApplet = 'false'; // ($detect->isMobile() ? ($detect->isTablet() ? 'false' : 'true') : 'false');
    if( $detect->isMobile() && !$detect->isTablet() ) { $wgJmolMaxAppletSize = 300; }
    $jsmolPath = $wgScriptPath . '/extensions/jsmol';
    $isSigned = ($this->mValSigned == "true") ? "true" : "false";
    if (($reqUse == "JAVA") or ($reqUse == "SIGNED")) {$isSigned = "true"; $reqUse = "SIGNED";} 
    $parser->mOutput->addHeadItem('<link href="' . $jsmolPath . '/wiki/Jmol.css" rel="stylesheet" type="text/css">');
    $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/JSmol.min.js></script>');
    $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/js/Jmol2.js></script>');
    $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/wiki/JSmolPopup.js?' . $wgJmolScriptVersion . '></script>');
    if ($reqUse == "WEBGL") {
      $isSigned = "false";
      $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/js/JSmolThree.js></script>');
      $parser->mOutput->addHeadItem('<script src=' . $jsmolPath . '/js/JSmolGLmol.js></script>');
    }    
    $jarFile = ($isSigned == "true") ? "JmolAppletSigned0.jar" : "JmolApplet0.jar";
    $wgOut->addHTML('<script>//<![CDATA[
var Info0 = {
use: "' . $reqUse . '",
disableJ2SLoadMonitor: true, 
disableInitialConsole: true, 
debug: false, // alerts full Info before JSmol displays
jarPath: "' . $jsmolPath . '/java",
j2sPath: "' . $jsmolPath . '/j2s",
jarFile: "' . $jarFile . '",
isSigned: "' . $isSigned . '",
serverURL: "' . $jsmolPath . '/php/jsmol.php",
coverCommand: null,
coverImage: null,
addSelectionOptions: false,
readyFunction: null,
defaultModel: null,
coverTitle: "Loading ... Please wait.",  // tip that is displayed before model starts to load
deferApplet: ' . $deferApplet . ', // true == the model should not be loaded until the image is clicked 
deferUncover: false, // true == the image should remain until command execution is complete
script: "set pdbGetHeader true; set echo off; set echo loading 50% 50%; set echo loading center; color echo [xffa500]; echo Loading, please wait ...; refresh; ",
width:' . $wgJmolDefaultAppletSize . ', height:' . $wgJmolDefaultAppletSize . '
};
Jmol._alertNoBinary = false;
//]]>
</script>');
    $initializeJSmolDone = true;
  }
  return true;
}

  function hClearState() { // Gets run when Parser::clearState() gets run
    # Don't clear state when we're in the middle of parsing a <jmol> tag
    if ($this->mInJmol === false ) { resetValues(); }
    return true;
  }

} // END CLASS DEFINITION
