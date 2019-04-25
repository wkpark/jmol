<?php
/**
 * Jmol extension - adds the possibility to include [http://www.jmol.org Jmol/JSmol objects] in MediaWiki.
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
 * March 2019 - version 5.0 - changes by AH (for MW 1.32 with PHP7)
*/

// Set 1 2014 J Prilusky

if (!defined('MEDIAWIKI')) {
  die('This file is a MediaWiki extension, it is not a valid entry point');
}

/* Global configuration parameters */
global $wgJmolAuthorizeUploadedFile;
global $wgJmolAuthorizeUrl;
global $wgJmolDefaultAppletSize;
global $wgJmolMaxAppletSize;
global $wgJmolDefaultScript;
global $wgJmolForceNameSpace;
global $wgJmolForceHTML5;
global $wgJmolShowWarnings;
global $wgJmolUsingSignedAppletByDefault;

global $wgJmolPdbServer;
global $wgJmolMolServer;

	
class Jmol {
	var $mInJmol = false;
	var $mOutput, $mDepth;
	var $mCurrentObject, $mCurrentTag, $mCurrentSubTag;

	var $mValCaption;
	var $mValChecked;
	var $mValColor;
	var $mValControls;
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
	var $mValImage;

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
      . $this->appletCaptionRow()
      . "</table>";
    return;
  }

  private function appletCanvasRow() {
    return "<tr><td><script type='text/javascript'>" . $this->renderInternalJmolApplet() . "</script></td></tr>";
    // "<div id='colorKeyDiv" . appletSuffix . "' name='colorKeyDiv'></div>"
  }

  private function appletTitleRow() {
    global $wgJmolAppletID;
    $titleRow = null;
    if ($this->mValTitle) {
      $content = trim($this->mValTitle);
      $titleRow = "<tr><td><div style='" .  $this->escapeScript($this->mValTitleCSS) . 
          "' name='title$wgJmolAppletID' id='title$wgJmolAppletID'>" . $content . "</div></td></tr>"; 
    }
    return $titleRow;
  }

  private function appletControlsRow() {
    global $wgJmolAppletID;
    $controlsRow = null; $content = null;
    if ($this->mValControls) {
      $jmolApplet = "JSmol_" . $wgJmolAppletID;
      $popupTitle = $this->mValTitle; 
      if (empty($popupTitle)) { $popupTitle = $this->mValName; }
      if (empty($popupTitle)) { $popupTitle = $jmolApplet; }
      if (strpos($this->mValControls,'all') !== false) { $this->mValControls = 'spin quality popup full'; }
      $this->mValControls = preg_replace('/\s+/',' ',$this->mValControls);
      $controls = explode(' ',$this->mValControls);
      foreach ($controls as $request) {
        switch($request) {
          case "spin":
            $spt = "'if(_spinning);spin off;spinflag = false;else;spin on;spinflag = true;endif;'";
            $content .= "<script type='text/javascript'>" .
                "Jmol.jmolButton($jmolApplet , $spt , " .
                "'" . wfMessage( "tog-jmolspin" ) . "');</script>" ;
            break;
          case "quality":
            $spt = "'set refreshing off;if(antialiasDisplayFlag);antialiasDisplayFlag = false;set antialiasDisplay off;set cartoonFancy off;else;antialiasDisplayFlag = true;set antialiasDisplay on;set cartoonFancy on;endif;set refreshing on;refresh;'";
            $content .= "<script type='text/javascript'>" .
                "Jmol.jmolButton($jmolApplet , $spt , " .
                "'" . wfMessage( "tog-jmolquality" ) . "');</script>";
            break;
          case "popup":
            $content .= "<input type='button' class='JSmolBtn'" .
             " onclick='cloneJSmol($jmolApplet , \"JSmolPopup_" . $this->mValName . "\")'" .
             " value='" . wfMessage( "jmol-popup" ) . "'>";
            break;
          case "full": //special for Proteopedia
            $spt = "&quot;set echo off; set echo loading 50% 50%; set echo loading center;" .
                   "color echo [xffa500]; background echo translucent 0.7 gray;" .
                   "echo " . wfMessage( "jmol-loadingfull" ) . "; refresh; load;" .
                   "script /wiki/extensions/Proteopedia/spt/initialview01.spt;&quot;";
            $content .= "<input type='button' id='fullloadbutton'" .
                " class='JSmolBtnFullLoad' style='display:none;'" .
                " onclick='this.style.display=\"none\";Jmol.script($jmolApplet , $spt);'" .
                " value='" . wfMessage( "jmol-full" ) . "'" .
                " title='" . wfMessage( "tog-jmolloadfullmodel" ) . "'>";
            break;
        }
      }
      $controlsRow = "<tr><td>" . $content  . "</td></tr>";
    }
    return $controlsRow;
  }
  
  private function appletCaptionRow() {
    global $wgParser,$wgTitle,$wgUser,$wgJmolAppletID;
    $captionRow = null;
    if ($this->mValCaption) {  
      $content = $this->renderWikiTextString($this->mValCaption);
      $captionRow = "<tr><td><div style='" . $this->escapeAttribute($this->mValCaptionCSS) . 
          "' name='caption$wgJmolAppletID' id='caption$wgJmolAppletID'>" . $content . "</div></td></tr>"; 
    }
    return $captionRow;
  }
  
  private function appletSetJmolAppletID() {
    global $wgJmolAppletID,$wgJmolNumID,$wgJmolPageHasApplet;
    if (!empty($this->mValName)) {
      $wgJmolAppletID = trim($this->mValName);
    } else {
      if (empty($wgJmolNumID)) {  
         $wgJmolNumID++; 
         $wgJmolAppletID = $wgJmolNumID; 
				 $wgJmolPageHasApplet = true;
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

  private function renderJmolAppletPopup($t1,$t2) {
    $res = " title='" . wfMessage( 'jmol-loadwarning' ) ."'";
    if ( !empty($this->mValName) ) {
      $res .= " name='" . $this->escapeAttribute( $this->mValName ) . "'" .
        " id='" . $this->escapeAttribute( $this->mValName ) . "'";
    }
    if ( empty($this->mValTitle) ) { $this->mValTitle = " "; }
    if ( empty($this->mValText) ) { $this->mValText = $this->mValTitle; }
    if ( empty($this->mValCaption) ) { $this->mValCaption = $this->mValTitle; }
    $dat = "{" .
      "type: 'HTML5'," .
      "spt: '" . $this->escapeScript($this->mValUrlContents) . "'," .
      "tit: '" . $this->escapeScript($this->mValTitle) . "'," .
      "cap: '" . $this->escapeScript($this->mValCaption) . "'," .
      "wname: '" . $this->escapeScript($this->mValName) . "'," .
      "pspeed: '" . $this->mValPlatformSpeed . "'," .
      "x: " . $this->mValPositionX . "," .
      "y: " . $this->mValPositionY .
    "}";
    $res .= "' onclick=\"popupJSmol(" . $dat . ")\"";
    $this->mOutput .= $t1 . $res . $t2;
  }

  private function renderJmolAppletButton() {
		// Renders a button in the Wiki page that will open a new window containing a JSmol object
    $t1 = "<input type='button' class='JSmolBtn'";
    $t2 = " value=" . $this->escapeAttribute( $this->mValText ) . ">";
    $this->renderJmolAppletPopup($t1,$t2);
  }

  private function renderJmolAppletLink() {
		// Renders a link in the Wiki page that will open a new window containing a JSmol object
    $t1 = "<a href='javascript:void(0)' class='JSmolLnk'";
    $t2 = ">" . $this->escapeScript( $this->mValText ) . "</a>";
    $this->renderJmolAppletPopup($t1,$t2);
  }


  private function renderJmolAppletInline($t1,$t2) {
    global $wgJmolAppletID;
    if ( $this->mValText == "" ) {
     $this->mValText = "JSmol";
    }

    $res = "<script type='text/javascript'>Jmol.setDocument(false);" .
          $this->renderInternalJmolApplet() .  //sets wgJmolAppletID
          "Jmol.setDocument(document);</script>";

    $hidelink = "<a href='javascript:void(0)'" .
              " title='" . wfMessage( 'jmol-hide' ) . "'" . 
              " onclick=\"" .
              "$('#JSmolInlineLink$wgJmolAppletID').css('display','inline');" .
              "$('#JSmolInlineEnv$wgJmolAppletID').html('');" .
              "$('#JSmolInlineOuter$wgJmolAppletID').css('display','none');" .
              "\">✖</a>";

    $res .= str_replace('##', $wgJmolAppletID, $t1);
    if ( $this->mValName != "" ) {
     $res .= " name='" . $this->escapeAttribute( $this->mValName ) . "'";
    }
    $res .= " title='" . wfMessage( 'jmol-loadwarning' ) ."'" .
          " onclick=\"" .
          "$('#JSmolInlineLink$wgJmolAppletID').css('display','none');" .
          "$('#JSmolInlineOuter$wgJmolAppletID').css('display','block');" .
          "$('#JSmolInlineEnv$wgJmolAppletID').html(" . 
          "Jmol.getAppletHtml(JSmol_$wgJmolAppletID));\"" .
          $t2;

    $res .= "<div id='JSmolInlineOuter$wgJmolAppletID' class='JSmolInlineOuterDiv'" .
          " style='display:none;width:" . $this->mValSize . "px; background-color:" . $this->mValColor . "'>" .
          "<div class='JSmolInlineTitleDiv'>" .
          $this->mValTitle .
          "<span id='JSmolInlineHide$wgJmolAppletID' class='JSmolInlineHideBtn'>" .
          $hidelink .
          "</span></div>" .
          "<div id='JSmolInlineEnv$wgJmolAppletID'></div>" .
          "<div style='" . $this->escapeAttribute($this->mValCaptionCSS) . "'>" .
          $this->mValCaption . "</div>" . 
          "</div>";
  
    $this->mOutput .= $res;
  }

  private function renderJmolAppletInlineButton() { 
    // Renders a button in the Wiki page that will insert a div containing a JSmol object
    $t1 = "<input type='button' id='JSmolInlineLink##' class='JSmolBtn'";
    $t2 = " value='" . $this->mValText . "'>";
    $this->renderJmolAppletInline($t1,$t2);
  }

  private function renderJmolAppletInlineLink() { 
    // Renders a link in the Wiki page that will insert a div containing a JSmol object
    $t1 = "<a href='javascript:void(0)' id='JSmolInlineLink##' class='JSmolLnk'";
    $t2 = ">" . $this->mValText . "</a>";
    $this->renderJmolAppletInline($t1,$t2);
  }

  private function renderJmolButton() { // Renders a button to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script type='text/javascript'>";
    $this->mOutput .= "Jmol.jmolButton(JSmol_$wgJmolAppletID" .
    ",'" . $this->escapeScript( $this->mValScript ) .
    "','" . $this->escapeScript( $this->mValText ) . "'";
    if ( !empty($this->mValName) ) { $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'"; }
    $this->mOutput .= ");</script>";
  }

  private function renderJmolCheckbox() { // Renders a checkbox to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script type='text/javascript'>";
    $this->mOutput .= "Jmol.jmolCheckbox(JSmol_$wgJmolAppletID" .
    ",'" . $this->escapeScript( $this->mValScriptWhenChecked ) . 
    "','" . $this->escapeScript( $this->mValScriptWhenUnchecked ) . 
    "','" . $this->escapeScript( $this->mValText ) . "'";
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
    $this->mOutput .= "<script type='text/javascript'>";
    $this->mOutput .= "Jmol.jmolLink(JSmol_$wgJmolAppletID" .
    ",'" . $this->escapeScript( $this->mValScript ) . 
    "','" . $this->escapeScript( $this->mValText ) . "'";
    if ( $this->mValName != "" ) {
      $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
    }
    $this->mOutput .= ");</script>";
  }

  private function renderJmolMenu() { // Renders a menu to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script type='text/javascript'>";
    $this->mOutput .= "Jmol.jmolMenu(JSmol_$wgJmolAppletID" .
    ",[" . $this->mValItems . "]," .
    $this->escapeScript( $this->mValMenuHeight );
    if ( $this->mValName != "" ) { $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'"; }
    $this->mOutput .= ");</script>";
  }

  private function renderJmolRadioGroup() { // Renders a radio group to control a Jmol applet
    global $wgJmolAppletID;
    $this->linkSetJmolAppletID();
    $this->mOutput .= "<script type='text/javascript'>";
    $this->mOutput .= "Jmol.jmolRadioGroup(JSmol_$wgJmolAppletID" .
    ",[" . $this->mValItems . "]";
    if ( $this->mValVertical == "true" ) {
      $this->mOutput .= ",'<br>'";
    } else {
      $this->mOutput .= ",'&nbsp;'";
    }
    if ( $this->mValName != "" ) {
      $this->mOutput .= ",'" . $this->escapeScript( $this->mValName ) . "'";
    }
    $this->mOutput .= ");</script>";
  }

  private function renderInternalJmolApplet() { // Internal function to make a Jmol applet
    global $wgJmolAuthorizeUrl, $wgJmolAuthorizeUploadedFile, $wgJmolCoverImageGenerator;
    global $wgJmolForceNameSpace, $wgScriptPath, $wgJmolPlatformSpeed;
    global $wgJmolAppletID,$wgJmolNumID,$wgJmolPageHasApplet;
    $this->appletSetJmolAppletID(); 
		$wgJmolPageHasApplet = true;
    $jmolApplet = "JSmol_$wgJmolAppletID";
    $output = "Info0.width = " . $this->mValSize . ";Info0.height = " . $this->mValSize .
              ";Info0.color = '" . $this->mValColor . "';";
 
    if ($this->mValPlatformSpeed) { // user requested platformSpeed
      $wgJmolPlatformSpeed = $this->mValPlatformSpeed;
    }
    $spt = "set platformSpeed $wgJmolPlatformSpeed" .
           "; DEFAULTPSPEED = $wgJmolPlatformSpeed" . ";";

    if ($this->mValImage) {
      $output .= "Info0.deferApplet = true; Info0.coverTitle = '" .
        wfMessage( 'jmol-click2load' ) . "'; "; 
      if (preg_match('/^http/', $this->mValImage) === 1) {
        $output .= "Info0.coverImage = '" . $this->mValImage . "';";
      } else {
        //$imgFile = wfFindFile( $this->mValImage );
        $imgFile = wfLocalFile( $this->mValImage );
        if (!is_null($imgFile) and $imgFile->exists()) {
          $imgUrl = $imgFile->getUrl();
          $output .= "Info0.coverImage = '$imgUrl';";
        } else {
          // Use some replacement for image not found? Seems unnecessary, just shows broken icon.
          $output .= "Info0.coverImage = 'imageNotFound.png';";
        }
      }
    } else {
      $output .= "Info0.deferApplet = false; Info0.coverImage = null; ";
    }

    $this->setValUrlContents();
    if ( $this->mValUrlContents != "" ) {
      $spt0 = trim($this->escapeScript( $this->mValUrlContents ));
      if ( strpos($spt0, "MSG ") !== false ) {  //AH
        $spt0 = str_replace("MSG ", "", $spt0);
        $spt0 = str_replace("Contents", "Contents|", $spt0); //break line so that echo fits
        $spt .= "set echo off; set echo top left; echo " . $spt0 . "; ";
      } else if ( strpos($spt0,"action=raw") !== false ) {
        $spt .= "modelData = load(\"" . $spt0 . "\"); " .
              "modelData = modelData.trim(\"\\n\"); load \"@modelData\"; " .
              $this->escapeScript( $this->mValScript ) . "; ";
      } else {
        $spt .= "load " . $spt0 . ";" .
              $this->escapeScript( $this->mValScript ) . "; ";
      }
    } elseif ( $this->mValInlineContents != "" ) {
      $this->mValInlineContents = preg_replace( "/\n/", "|", trim( $this->mValInlineContents ));
      $spt .= "data \"model\"|" . $this->mValInlineContents . "|end \"model\";" .
              $this->escapeScript( $this->mValScript ) . "; ";
    } else {
      $spt .= $this->escapeScript( $this->mValScript ) . "; ";
    }
    $output .= "Info0.script = Info0script0 + '$spt';";

			/*
			if ($wgJmolCoverImageGenerator) {
				 $cmd = str_replace(" ","+",$wgJmolCoverImageGenerator . $this->escapeScript(( $this->mValScript )));
				 $output .= "Info0.coverImage = \"$cmd\";\n";
			}
			*/

    $output .= "Jmol.getApplet('" . $jmolApplet . "',Info0);";
    return $output;
  }

	private function showWarning( $message ) { // Function called for outputing a warning // not currently used
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
 				$this->renderJmolAppletInlineLink();
				break;
			case "JMOLAPPLETINLINEBUTTON":
 				$this->renderJmolAppletInlineButton();
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
		global $wgJmolMaxAppletSize;

		switch ( $this->mDepth ) {
		case 3:
			// Details of the interesting tags
			switch ( $this->mCurrentTag ) {
			case "CAPTION":
				$this->mValCaption .= trim($data);
				break;
			case "CAPTIONCSS":
				$this->mValCaptionCSS = trim($data);
				break;
			case "CHECKED":
				$this->mValChecked = trim($data);
				break;
			case "COLOR":
				$this->mValColor = trim($data);
				break;
			case "CONTROLS":
				$this->mValControls = trim($data);
				break;
			case "FLOAT":
				$this->mValFloat = trim($data);
				break;
			case "FRAME":
				$this->mValFrame = trim($data); 
				break;
			case "INLINECONTENTS":
			case "ONLINECONTENTS":
				$data = trim( $data );
				if ( $data != "" ) {
					$this->mValInlineContents .= "\n" . addslashes($data);
				}
				break;
			case "MENUHEIGHT":
				$this->mValMenuHeight = intval($data);
				break;
			case "NAME":
				$this->mValName = trim($data);
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
			case "SIZE":
			    $data = intval($data);
			    if ($data > $wgJmolMaxAppletSize) { $data = $wgJmolMaxAppletSize; }
				$this->mValSize = $data;
				break;
			case "TARGET":
				$this->mValTarget = trim($data);
				break;
			case "TEXT":
				$this->mValText .= $data;
				break;
			case "TITLE":
				$this->mValTitle .= $data;
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
				$this->mValPositionX = intval($data);
				break;
			case "Y":
				$this->mValPositionY = intval($data);
				break;
			case "IMAGE":
				$this->mValImage = $data;
				break;
			}
			break;
		case 4:
			// Details of sub tags
			if ( $this->mCurrentTag == "ITEM" ) {
				switch ( $this->mCurrentSubTag ) {
				case "CHECKED":
					$this->mValChecked = trim($data);
					break;
				case "SCRIPT":
					$this->mValScript = str_replace( "%26", "&", $data );
					break;
				case "TEXT":
					if ( isset($this->prevSubTag) && $this->prevSubTag == "TEXT" ) {
						$this->mValText .= $data; //accumulate successive calls for same item
					} else {
						$this->mValText = $data;
					}
					break;
				}
				$this->prevSubTag = $this->mCurrentSubTag; //remember for next call
			}
			break;
		}
	}

    function setValUrlContents ($text = null,$type = null) {
      global $wgJmolAuthorizeUrl, $wgJmolAuthorizeUploadedFile,$wgJmolForceNameSpace,$wgJmolPdbServer,$wgJmolMolServer,$wgScriptPath;
        
      if ($text) {
        switch ( $type ) {
        case 'pdb':
          $this->mValUrlContents = str_replace("####",$text,$wgJmolPdbServer);
          return;;
        case 'mol':
          $this->mValUrlContents = str_replace("####",$text,$wgJmolMolServer);
          return;
        case 'smiles':
          $this->mValUrlContents = "\$" . $text; 
          return;
        default:
          $this->mValUploadedFileContents = $text;
        }
      }
      
      if ( $this->mValUploadedFileContents != "" ) {
        if ( $wgJmolAuthorizeUploadedFile == true ) {
          $file = wfLocalFile( $this->mValUploadedFileContents );
          if (!is_null($file) and $file->exists()) {
            $this->mValUrlContents = $file->getURL();
          } else {
            $this->mValUrlContents = "MSG " . wfMessage( 'jmol-requesteddatanotfound' );
          }
        } else {
          $this->mValUrlContents = "MSG " . wfMessage( 'jmol-nouploadedfilecontents' );
        }
      } elseif ( $this->mValWikiPageContents != "" ) {
        if ( $wgJmolAuthorizeUrl == true ) {
          $this->mValUrlContents = $wgScriptPath."/index.php?title="; //fix for non-root wikis
          if ( $wgJmolForceNameSpace != "" ) {
            $this->mValUrlContents .= $wgJmolForceNameSpace . ":";
          }
          $this->mValUrlContents .= trim($this->mValWikiPageContents) . "&action=raw";
        } else {
          $this->mValUrlContents = "MSG " . wfMessage( 'jmol-nowikipagecontents' );
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
    $this->mValInlineContents = "";
    $this->mValItems = "";
    $this->mValMenuHeight = "1";
    $this->mValName = "";
    $this->mValPlatformSpeed = null;
    $this->mValPositionX = 100;
    $this->mValPositionY = 100;
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
    $this->mValImage = null;
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
		return Xml::encodeJsVar( $value );
	}
	
  private function escapeScript( $value ) {
    //This code is a copy of fn. escapeJsString from Xml.php in an older MW, 1.27.1
      $pairs = [
          "\\" => "\\\\",
          "\"" => "\\\"",
          '\'' => '\\\'',
          "\n" => "\\n",
          "\r" => "\\r",
          "<" => "\\x3c",
          ">" => "\\x3e",
          "&" => "\\x26",
          "\xe2\x80\x8c" => "\\u200c", // ZERO WIDTH NON-JOINER
          "\xe2\x80\x8d" => "\\u200d", // ZERO WIDTH JOINER
			];
      return trim(strtr( $value, $pairs )); //it's important to remove final space(s)
	}

	private function parseJmolTag( $text, $params, $parser ) {
		global $wgJmolScriptVersion;
		$parser->disableCache();
        $this->initializeJSmol($parser);
		// Add element to display file
		return $this->renderJmol( $text );
	}

  private function makeLink( $text, $params ) {
		//text is empty, params has text data
    if ( isset( $params[ "text" ] ) ) {
      $txt = $params[ "text" ];
    } else {
      $txt = $text;
    }
    if ( isset( $params[ "title" ] ) ) {
      $tit = $params[ "title" ];
    } else {
      $tit = $txt;
    }
    $dat = "{" .
      "type: 'HTML5'," .
      "spt: '" . $this->escapeScript($this->mValUrlContents) . "'," .
      "tit: '$tit'," .
      "wname: '" . $this->escapeScript($this->mValName) . "'," .
      "pspeed: '" . $this->mValPlatformSpeed . "'" .
    "}";
    $result = "<a href='javascript:void(0)' class='JSmolLnk'" .
              " title='" . wfMessage( 'jmol-loadwarning' ) ."'" .
              " onclick=\"popupJSmol($dat)\">" . $txt . "</a>";
    return $result;
  }

  private function parseJmolFileTag( $text, $params, $parser ) {
    global $wgJmolScriptVersion, $wgJmolAuthorizeUploadedFile;
    $this->initializeJSmol($parser);
    $this->setValUrlContents($text);
    return $this->makeLink($text, $params);
  }

  private function parseJmolPdbTag( $text, $params, $parser ) {
    global $wgJmolScriptVersion, $wgJmolAuthorizeUploadedFile;
    $this->initializeJSmol($parser);
    $this->setValUrlContents($text,'pdb');
    return $this->makeLink($text, $params);
  }

	private function parseJmolMolTag( $text, $params, $parser ) {
    global $wgJmolScriptVersion, $wgJmolAuthorizeUploadedFile;
    $this->initializeJSmol($parser);
    $this->setValUrlContents($text,'mol');
    return $this->makeLink($text, $params);
  }

	private function parseJmolSmilesTag( $text, $params, $parser ) {
    global $wgJmolScriptVersion, $wgJmolAuthorizeUploadedFile;
    $this->initializeJSmol($parser);
    $this->setValUrlContents($text,'smiles');
    return $this->makeLink($text, $params);
  }

	function setHooks() { // Initialize the parser hooks
		global $wgParser, $wgHooks;
		global $wgJmolAuthorizeJmolFileTag,
			   $wgJmolAuthorizeJmolPdbTag,
			   $wgJmolAuthorizeJmolMolTag,
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
		if ( $wgJmolAuthorizeJmolMolTag == true ) {
			$wgParser->setHook( 'jmolMol', array( &$this, 'jmolMolTag' ) );
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

	public function jmolMolTag( $text, $params, $parser ) { // <jmolMol>
		if ( $this->mInJmol ) {
			return htmlspecialchars( "<jmolMol>$text</jmolMol>" );
		} else {
			$this->mInJmol = true;
			$ret = $this->parseJmolMolTag( $text, $params, $parser );
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
	global $IP; // that gives MW installed path
  if ($initializeJSmolDone == false) {
    $extensionDir = $wgScriptPath . str_replace($IP, '', dirname(__FILE__)) . '/'; 
      //in this way, a server absolute path valid for loading css and js files
    $parser->mOutput->addHeadItem('<script type="text/javascript">' .
      'var extensionDir = "' . str_replace ('\\', '/', $extensionDir) . '";' .
      'var JSmolControlBtns = {};' .
      'JSmolControlBtns.toggle = "' . wfMessage('jmol-tog') . '";' .
      'JSmolControlBtns.spin = "' . wfMessage('tog-jmolspin') . '";' .
      'JSmolControlBtns.quality = "' . wfMessage('tog-jmolquality') . '";' .
      'JSmolControlBtns.full = "' . wfMessage('jmol-full') . '";' .
      '</script>');
    require_once ($IP . '/includes/Title.php');
    require_once ('Mobile_Detect.php');

      // rendering engine to use
    if (empty($reqUse)) { $reqUse = 'HTML5';}
    if ($wgJmolForceHTML5 == true) { $reqUse = 'HTML5';}
    $reqUse = strtoupper($wgRequest->getVal('use',$wgRequest->getVal('USE',$wgRequest->getVal('_use',$wgRequest->getVal('_USE',$reqUse)))));
    $detect = new Mobile_Detect;
    if (($wgUser->getOption('jmolusejava') == 1) and !$detect->isMobile()) { $reqUse = 'SIGNED'; } // instead of JAVA
    $reqUse = ($detect->isMobile()) ? 'HTML5' : $reqUse;
    if ($wgUser->getOption('jmoluseWebGL') == 1) { $reqUse = 'WEBGL'; }
    $wgJmolNumID = null;
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
    $isSigned = ($this->mValSigned == "true") ? "true" : "false";
    if (($reqUse == "JAVA") or ($reqUse == "SIGNED")) {$isSigned = "true"; $reqUse = "SIGNED";} 
		$parser->mOutput->addHeadItem('<link href="' . $extensionDir . 'Jmol.css" rel="stylesheet" type="text/css">');
    $parser->mOutput->addHeadItem('<script src="' . $extensionDir . 'JSmol.min.js"></script>');
    $parser->mOutput->addHeadItem('<script src="' . $extensionDir . 'JSmolPopup.js?' . $wgJmolScriptVersion . '"></script>');
		if ($reqUse == "WEBGL") {
      $isSigned = "false";
      $parser->mOutput->addHeadItem('<script src="' . $extensionDir . 'JSmol.GLmol.min.js"></script>');
    }
    $jarFile = ($isSigned == "true") ? "JmolAppletSigned0.jar" : "JmolApplet0.jar";
    $wgOut->addHTML('<script type="text/javascript">//<![CDATA[
var Info0script0 = "set pdbGetHeader true; set echo off; set echo loading 50% 50%; "+
 "set echo loading center; color echo [xffa500]; echo ' . wfMessage( 'jmol-loading' ) . '; refresh; ";
var Info0 = {
use: "' . $reqUse . '",
disableJ2SLoadMonitor: true, 
disableInitialConsole: true, 
debug: false, 
jarPath: extensionDir + "java",
j2sPath: extensionDir + "j2s",
jarFile: "' . $jarFile . '",
isSigned: "' . $isSigned . '",
serverURL: extensionDir + "php/jsmol.php",
coverCommand: null,
coverImage: null,
makeLiveImage: extensionDir + "makeLiveImage.jpg",
addSelectionOptions: false,
readyFunction: null,
defaultModel: null,
coverTitle: "' . wfMessage( 'jmol-loading' ) . '",
deferApplet: ' . $deferApplet . ',
deferUncover: false,
script: Info0script0,
width:' . $wgJmolDefaultAppletSize . ', height:' . $wgJmolDefaultAppletSize . '
};
Jmol._alertNoBinary = false;
delete Jmol._tracker; // avoid error messages on AWS
Jmol.setAppletCss("JSmolObj");
Jmol.setLinkCss("JSmolLnk");
Jmol.setButtonCss("JSmolBtn");
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

