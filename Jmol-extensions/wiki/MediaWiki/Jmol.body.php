<?php
/**
 * @author Nicolas Vervelle, Jmol Development team
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
  var $mValUploadedFileContents;
  var $mValUrlContents;
  var $mValVertical;
  var $mValWikiPageContents;

  // Constructor
  function __construct() {
    $this->mOutput = "";
    $this->mDepth = 0;
    $this->resetValues();
  }

  // *** //
  // XML //
  // *** //

  // Render Jmol tag
  private function renderJmol($input) {
    $this->mOutput = "<!-- Jmol -->";
    $this->mDepth = 0;
    $xmlParser = xml_parser_create();
    xml_set_object($xmlParser, $this);
    xml_set_element_handler($xmlParser, "startElement", "endElement");
    xml_set_character_data_handler($xmlParser, "characterData");
    $input = "<jmol>$input<jmol>";
    if (!xml_parse($xmlParser, $input)) {
      die(sprintf(
        "XML error: %s at line %d",
        xml_error_string(xml_get_error_code($xmlParser)),
        xml_get_current_line_number($xmlParser)));
    }
    xml_parser_free($xmlParser);
    return $this->mOutput;
  }

  // Renders a Jmol applet directly in the Wiki page
  private function renderJmolApplet() {
    global $wgJmolExtensionPath;
    $prefix = "";
    $postfix = "";

    $this->mValInlineContents = trim($this->mValInlineContents);
    $this->mValInlineContents = preg_replace("/\t/", " ", $this->mValInlineContents);
    $this->mValInlineContents = preg_replace("/\n/", "\\n'+\n'", $this->mValInlineContents);
    $prefix .= "<script language='Javascript' type='text/javascript'>";
    $postfix .= "</script>\n";
    $this->mOutput .= $this->renderInternalJmolApplet($prefix, $postfix, "'");
  }

  // Renders a button in the Wiki page that will open a new window containing a Jmol applet
  private function renderJmolAppletButton() {
    global $wgJmolExtensionPath;
    $prefix = "";
    $postfix = "";

    $prefix .= "<input type='button'";
    if ($this->mValName != "") {
      $prefix .= " name='".$this->escapeAttribute($this->mValName)."'".
                 " id='".$this->escapeAttribute($this->mValName)."'";
    }
    if ($this->mValText == "") {
      $this->mValText = $this->mValTitle;
    }
    if ($this->mValText != "") {
      $prefix .= " value='".$this->escapeAttribute($this->mValText)."'";
    }
    $prefix .= " onclick=\"jmolWikiPopupWindow('".$wgJmolExtensionPath."',".
                                              "'".$this->escapeScript($this->mValTitle)."',".
                                              "'".$this->escapeScript($this->mValSize)."',".
                                              "'".$this->escapeScript($this->mValPositionX)."',".
                                              "'".$this->escapeScript($this->mValPositionY)."',".
                                              "'";
    $postfix = "');return true\" />";
    $this->mOutput .= $this->renderInternalJmolApplet($prefix, $postfix, "\\'");
  }

  // Renders a button in the Wiki page that will open a new window containing a Jmol applet
  private function renderJmolAppletLink() {
    global $wgJmolExtensionPath;
    $prefix = "";
    $postfix = "";

    $prefix .= "<a";
    if ($this->mValName != "") {
      $prefix .= " name='".$this->escapeAttribute($this->mValName)."'".
                 " id='".$this->escapeAttribute($this->mValName)."'";
    }
    $prefix .= " href=\"javascript:jmolWikiPopupWindow('".$wgJmolExtensionPath."',".
                                                      "'".$this->escapeScript($this->mValTitle)."',".
                                                      "'".$this->escapeScript($this->mValSize)."',".
                                                      "'".$this->escapeScript($this->mValPositionX)."',".
                                                      "'".$this->escapeScript($this->mValPositionY)."',".
                                                      "'";
    $postfix .= "');\"";
    $postfix .= ">";
    if ($this->mValText == "") {
      $this->mValText = $this->mValTitle;
    }
    if ($this->mValText != "") {
      $postfix .= $this->mValText;
    }
    $postfix .= "</a>";
    $this->mOutput .= $this->renderInternalJmolApplet($prefix, $postfix, "\\'");
  }

  // Renders a button to control a Jmol applet
  private function renderJmolButton() {
    $this->mOutput .= "<script language='Javascript' type='text/javascript'>\n";
    if ($this->mValTarget != "") {
      $this->mOutput .= "jmolSetTarget('".$this->escapeScript($this->mValTarget)."');\n";
    }
    $this->mOutput .= "jmolButton(".
      "'".$this->escapeScript($this->mValScript)."',".
      "'".$this->escapeScript($this->mValText)."'";
    if ($this->mValName != "") {
      $this->mOutput .= ",'".$this->escapeScript($this->mValName)."'";
    }
    $this->mOutput .= ");\n".
      "</script>\n";
  }

  // Renders a checkbox to control a Jmol applet
  private function renderJmolCheckbox() {
    $this->mOutput .= "<script language='Javascript' type='text/javascript'>\n";
    if ($this->mValTarget != "") {
      $this->mOutput .= "jmolSetTarget('".$this->escapeScript($this->mValTarget)."');\n";
    }
    $this->mOutput .= "jmolCheckbox(".
      "'".$this->escapeScript($this->mValScriptWhenChecked)."',".
      "'".$this->escapeScript($this->mValScriptWhenUnchecked)."',".
      "'".$this->escapeScript($this->mValText)."'";
    if ($this->mValChecked) {
      $this->mOutput .= ",true";
    } else {
      $this->mOutput .= ",false";
    }
    if ($this->mValName != "") {
      $this->mOutput .= ",'".$this->escapeScript($this->mValName)."'";
    }
    $this->mOutput .= ");\n".
      "</script>\n";
  }

  // Renders a link to control a Jmol applet
  private function renderJmolLink() {
    $this->mOutput .= "<script language='Javascript' type='text/javascript'>\n";
    if ($this->mValTarget != "") {
      $this->mOutput .= "jmolSetTarget('".$this->escapeScript($this->mValTarget)."');\n";
    }
    $this->mOutput .= "jmolLink(".
      "'".$this->escapeScript($this->mValScript)."',".
      "'".$this->escapeScript($this->mValText)."'";
    if ($this->mValName != "") {
      $this->mOutput .= ",'".$this->escapeScript($this->mValName)."'";
    }
    $this->mOutput .= ");\n".
      "</script>\n";
  }

  // Renders a menu to control a Jmol applet
  private function renderJmolMenu() {
    $this->mOutput .= "<script language='Javascript' type='text/javascript'>\n";
    if ($this->mValTarget != "") {
      $this->mOutput .= "jmolSetTarget('".$this->escapeScript($this->mValTarget)."');\n";
    }
    $this->mOutput .= "jmolMenu(".
      "[".$this->mValItems."],".
      $this->escapeScript($this->mValMenuHeight);
    if ($this->mValName != "") {
      $this->mOutput .= ",'".$this->escapeScript($this->mValName)."'";
    }
    $this->mOutput .= ");\n".
      "</script>\n";
  }

  // Renders a radio group to control a Jmol applet
  private function renderJmolRadioGroup() {
    $this->mOutput .= "<script language='Javascript' type='text/javascript'>\n";
    if ($this->mValTarget != "") {
      $this->mOutput .= "jmolSetTarget('".$this->escapeScript($this->mValTarget)."');\n";
    }
    $this->mOutput .= "jmolRadioGroup([".$this->mValItems."]";
    if ($this->mValVertical) {
      $this->mOutput .= ",jmolBr()";
    } else {
      $this->mOutput .= ",'&nbsp;'";
    }
    if ($this->mValName != "") {
      $this->mOutput .= ",'".$this->escapeScript($this->mValName)."'";
    }
    $this->mOutput .= ");\n".
      "</script>\n";
  }

  // Internal function to make a Jmol applet
  private function renderInternalJmolApplet($prefix, $postfix, $sep) {
    global $wgJmolAuthorizeUrl, $wgJmolAuthorizeUploadedFile;
    global $wgJmolForceNameSpace, $wgJmolExtensionPath;
    $output = $prefix;

    $output .=
      "jmolCheckBrowser(".$sep."popup".$sep.", ".
                          $sep.$wgJmolExtensionPath."/browsercheck".$sep.", ".
                          $sep."onClick".$sep.");".
      "jmolSetAppletColor(".$sep.$this->escapeScript($this->mValColor).$sep.");";
    if ($this->mValUploadedFileContents != "") {
      if ($wgJmolAuthorizeUploadedFile == true) {
        $title = Title::makeTitleSafe(NS_IMAGE, $this->mValUploadedFileContents);
        $article = new Article($title);
        if (!is_null($title) && $article->exists()) {
          $file = new Image($title);
          $this->mValUrlContents = $file->getURL();
        }
      } else {
        return $this->showWarning("The field uploadedFileContents is not authorized on this wiki.");
      }
    }
    if ($this->mValWikiPageContents != "") {
      if ($wgJmolAuthorizeUrl == true) {
        $this->mValUrlContents = "/index.php?title=";
        if ($wgJmolForceNameSpace != "") {
          $this->mValUrlContents .= $wgJmolForceNameSpace.":";
        }
        $this->mValUrlContents .= $this->mValWikiPageContents."&action=raw";
      } else {
        return $this->showWarning("The field wikiPageContents is not authorized on this wiki.");
      }
    }
    if ($this->mValUrlContents != "") {
      $output .= "jmolApplet(".
        $this->escapeScript($this->mValSize).", ".
        $sep."load ".$this->escapeScript($this->mValUrlContents)."; ".
             $this->escapeScript($this->mValScript).$sep;
    } else {
      $output .= "jmolAppletInline(".
        $this->escapeScript($this->mValSize).", ".
        $sep.$this->escapeScript($this->mValInlineContents).$sep.", ".
        $sep.$this->escapeScript($this->mValScript).$sep;
    }
    if ($this->mValName != "") {
      $output .= ",".$sep.$this->escapeScript($this->mValName).$sep;
    }
    $output .=
      ");".
      "jmolBr();";
    $output .= $postfix;

    return $output;
  }

  // Function called for outputing a warning
  private function showWarning($message) {
    global $wgJmolShowWarnings;

    $output = "";
    if ($wgJmolShowWarnings == true) {
      $output .= $message;
    }
    return $output;
  }

  // ************* //
  // XML CALLBACKS //
  // ************* //

  // Function called when an opening XML tag is found
  function startElement($parser, $name, $attrs) {
    $this->mDepth += 1;
    switch ($this->mDepth) {
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
  function endElement($parser, $name) {
    switch ($this->mDepth) {
    case 1:
      // JMOL tag itself
      $this->resetValues();
      break;
    case 2:
      // The interesting tags
      switch ($this->mCurrentObject) {
      case "JMOLAPPLET":
        $this->renderJmolApplet();
        break;
      case "JMOLAPPLETBUTTON":
        $this->renderJmolAppletButton();
        break;
      case "JMOLAPPLETLINK":
        $this->renderJmolAppletLink();
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
      switch ($this->mCurrentTag) {
      case "ITEM":
        if ($this->mValItems != "") {
          $this->mValItems .= ",";
        }
        $this->mValItems .= "['".$this->escapeScript($this->mValScript)."'";
        $this->mValItems .= ",'".$this->escapeScript($this->mValText)."'";
        if ($this->mValChecked) {
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
  function characterData($parser, $data) {
    global $wgJmolAuthorizeChoosingSignedApplet;
    
    switch ($this->mDepth) {
    case 3:
      // Details of the interesting tags
      switch ($this->mCurrentTag) {
      case "CHECKED":
        $this->mValChecked = $data;
        break;
      case "COLOR":
        $this->mValColor = $data;
        break;
      case "INLINECONTENTS":
      case "ONLINECONTENTS":
        $data = trim($data);
        if ($data != "") {
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
        $this->mValScript = $data;
        break;
      case "SCRIPTWHENCHECKED":
        $this->mValScriptWhenChecked = $data;
        break;
      case "SCRIPTWHENUNCHECKED":
        $this->mValScriptWhenUnchecked = $data;
        break;
      case "SIGNED":
        if ($wgJmolAuthorizeChoosingSignedApplet) {
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
      case "UPLOADEDFILECONTENTS":
        $this->mValUploadedFileContents = $data;
        break;
      case "URLCONTENTS":
        $this->mValUrlContents = $data;
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
      if ($this->mCurrentTag == "ITEM") {
        switch ($this->mCurrentSubTag) {
        case "CHECKED":
          $this->mValChecked = $data;
          break;
        case "SCRIPT":
          $this->mValScript = $data;
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
    $this->mValUploadedFileContents = "";
    $this->mValUrlContents = "";
    $this->mValVertical = false;
    $this->mValWikiPageContents = "";
  }

  // Functions to escape characters
  private function escapeAttribute($value) {
    return Xml::escapeJsString($value);
  }
  private function escapeScript($value) {
    return Xml::escapeJsString($value);
  }

  // Add a link to Javascript file in the HTML header
  private function includeScript(&$outputPage, $scriptFile) {
    $script = "<script language='Javascript' ".
                      "type='text/javascript' ".
                      "src='".$scriptFile."'>".
              "</script>\n";
    $outputPage->addScript($script);
  }

  // Add a Javascript script in the HTML header
  private function addScript(&$outputPage, $scriptContents) {
    $script = "<script language='Javascript' ".
                      "type='text/javascript'>".
                      $scriptContents.
              "</script>\n";
    $outputPage->addScript($script);
  }

  // *********************** //
  // DIRECTING THE EXTENSION //
  // *********************** //

  private function parseJmolTag(&$text, &$params, &$parser) {
    $parser->disableCache();
    return $this->renderJmol($text);
  }

  private function beforeHMTLOutput(&$outputPage, &$text) {
    global $wgJmolExtensionPath;
    //if (preg_match_all('/<!-- Jmol -->/m', $text, $matches) === false) {
    //  return true;
    //}
    $this->includeScript($outputPage, $wgJmolExtensionPath."/Jmol.js");
    $this->includeScript($outputPage, $wgJmolExtensionPath."/JmolMediaWiki.js");
    if ($this->mValSigned) {
      $this->addScript($outputPage, "jmolInitialize('".$wgJmolExtensionPath."', true);");
    } else {
      $this->addScript($outputPage, "jmolInitialize('".$wgJmolExtensionPath."', false);");
    }
    return true;
  }

  private function toto(&$outputPage, &$text) {
    global $wgJmolExtensionPath;
    if (preg_match_all('/<!-- Jmol -->/m', $text, $matches) === false) {
      return true;
    }
    $this->includeScript($outputPage, $wgJmolExtensionPath."/Jmol.js");
    $this->includeScript($outputPage, $wgJmolExtensionPath."/JmolMediaWiki.js");
    if ($this->mValSigned) {
      $this->addScript($outputPage, "jmolInitialize('".$wgJmolExtensionPath."', true);");
    } else {
      $this->addScript($outputPage, "jmolInitialize('".$wgJmolExtensionPath."', false);");
    }
    return true;
  }

  // ******************* //
  // MEDIAWIKI CALLBACKS //
  // ******************* //

  // Called for the <jmol> tag
  public function tag_jmol(&$text, &$params, &$parser) {
    return $this->parseJmolTag($text, $params, $parser);
  }

  // *************** //
  // MEDIAWIKI HOOKS //
  // *************** //

  // OutputPageBeforeHTML
  function hOutputPageBeforeHTML(&$out, &$text) {
    return $this->toto($out, $text); //$this->beforeHTMLOutput($out, $text);
  }

  // ParserBeforeStrip hook
  function hParserBeforeStrip(&$parser, &$text, &$strip_state) {
    return true;
  }

  // ParserAfterStrip hook
  function hParserAfterStrip(&$parser, &$text, &$strip_state) {
    return true;
  }

  // ParserBeforeTidy hook
  function hParserBeforeTidy(&$parser, &$text) {
    return true;
  }

  // ParserAfterTidy hook
  function hParserAfterTidy(&$parser, &$text) {
    return true;
  }

} // END CLASS DEFINITION
//</source>