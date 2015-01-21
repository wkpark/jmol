<?php

/*
 
 http://chemapps.stolaf.edu/jmol/mpstatus.php

 parameters:

 q=html|image|status|getlog  |  clear|update|setimage|setbanner|log
 password required for clear or update or setimage or setbanner or log



*/

$imagefile  = "/tmp/mp.png";
$statusfile = "/tmp/mpstatus.txt";
$bannerfile = "/tmp/mpbanner.txt";
$logfile   = "/tmp/mplog.txt";

$defaultTopic = "biophysics";
$defaultDelay = 30;

$q = $_REQUEST[q];
if (!isset($q)) {
  $q = "html";
}

$secretCode = "daf324a!";
$code = $_REQUEST[password];

if (!isset($code)) {

  // image, status, getlog, html

  if ($q == "image") {
    header("Content-type: image/png");
    echo file_get_contents($imagefile);
  } else if ($q == "status") {
    echo file_get_contents($statusfile);
  } else if ($q == "getlog" || $q == "log") {
    $q = "getlog";
    header("Content-type: text/plain");
    echo file_get_contents($logfile);
  }

} else if ($code == $secretCode) {

  // update, clear, setimage, setbanner, log, clearlog

  if ($q == "update") {
    $topic = str_replace("\"","'",trim($_REQUEST[topic]));
    $subtopic = str_replace("\"","'",trim($_REQUEST[subtopic]));
    $mydelay = $_REQUEST[delay];
    logInfo("$topic\t$subtopic\t$mydelay");
    $info = getHtml($topic, $subtopic, $mydelay);
    if (file_put_contents($statusfile, $info) > 0) {
      $q = "html";
    }
  } else if ($q == "clear") {
    unlink($statusfile);
    unlink($bannerfile);
    unlink($logfile);
  } else if ($q == "setimage") {
    $info = base64_decode(str_replace(" ", "+", $_REQUEST["?POST?_PNG_"].$_REQUEST["?POST?_PNGJ_"].$_REQUEST[_PNG_].$_REQUEST[_PNGJ_]));
    echo file_put_contents($imagefile, $info);
  } else if ($q == "setbanner") {
    echo file_put_contents($bannerfile, $_REQUEST[banner]);
    echo $_REQUEST[banner];
  } else if ($q == "log") {
    logInfo($_REQUEST[info]);
  } else if ($q == "clearlog") {
    unlink($logfile);
  }

}

if ($q == "html") {
  echo packageHtml();
}

function logInfo($info) {
  global $logfile;
  file_put_contents($logfile, date('r')."\t$info\r", FILE_APPEND);
}

function getHtml($topic, $subtopic, $mydelay) {
  return "#<table>"
		."<tr><td>topic</td><td><input type=text name=topic size=30 value=\"$topic\"></td></tr>"
		."<tr><td>subtopic</td><td><input type=text name=subtopic size=30 value=\"$subtopic\"></td></tr>"
		."<tr><td>delay</td><td><input type=text name=delay size=30 value=\"$mydelay\"></td></tr>"
		."<tr><td>updated</td><td>".date('r')."</td></tr>"
		."<tr><td>password</td><td><input type=password name=password size=10> <input type=submit name=q value=\"update\"></td></tr>"
	."</table>\n"
	."#<div style='display:none'>\n"
		." topic='$topic';\n subtopic='$subtopic';\n mydelay=0+$mydelay;\n updated='".date('r')."';\n"
	."#</div>";
}

function packageHtml() {

  global $statusfile, $bannerfile, $imagefile, $defaultTopic, $defaultDelay;

  $status = file_get_contents($statusfile);
  if ($status == "") {
    $status = getHtml($defaultTopic, "", $defaultDelay);
  }
  return "<html><body><table>"
	."<tr>"
	."<td valign=top width=300><h3>St. Olaf Molecular Playground<br />Status Page</h3>"
	."The image on the right was created ".date("r", filemtime($imagefile)).". Shown below are the current parameters for the display.<br /><form method=POST>".str_replace("#"," ",$status)."</form>"
	."</td>"
	."<td align=center><h3>".file_get_contents($bannerfile)."</h3><img width=512 height=346 src=mpstatus.php?q=image><br/><a href=mpstatus.php?q=log>log file</a></td>"
	."</tr></table></body></html>";
}


?>
