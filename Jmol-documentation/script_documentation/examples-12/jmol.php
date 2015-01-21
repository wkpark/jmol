<?php

/*
 
 http://chemapps.stolaf.edu/jmol/jmol.php
 
 Bob Hanson hansonr@stolaf.edu
 
 Easy Jmol insertion into a web page.

 See end of this file for option info.

*/ 

$VER = "1.1.0 6/11/2014 2:04:21 PM"; // moves to HTML5
//$VER = "0.2.0 27/11/2013"; // moves to trusted-signed applet

$myname = "jmol.php";
$testping = false;



$cactvsHost        = 'cactvs.nci.nih.gov';
$cactvsURL         = "http://$cactvsHost/chemical/structure/MODEL/file?format=sdf&get3d=True";
$timeout           = 5;
$pdbServer         = 'http://www.rcsb.org/pdb/files/PDBID.pdb';
$defaultJmolServer = 'http://chemapps.stolaf.edu/jmol';
$defaultModel      = 'aspirin';
$defaultWidth      = 300;
$defaultHeight     = 300;

$jmolserver = $_REQUEST[jmolserver]; // not implemented
$use        = $_REQUEST["use"];
$isfirst    = $_REQUEST[isfirst];
$title      = $_REQUEST[title];
$model      = $_REQUEST[model];
$pdbid      = $_REQUEST[pdbid];
$width      = $_REQUEST[width];
$height     = $_REQUEST[height];
$script     = $_REQUEST[script];
$scriptvar  = $_REQUEST[scriptvar];
$link       = $_REQUEST[link];
$source     = $_REQUEST[source];
$caption    = $_REQUEST[caption];
$inline     = isset($_REQUEST[inline]);
$get        = isset($_REQUEST[get]);
$isDebug    = isset($_REQUEST[debug]);

# set defaults

$showHelp = 1;
$isPdb = 0;
$isSource = 0;
$haveTitle = 0;

//if (!isset($jmolserver)) {
  $jmolserver = $defaultJmolServer;
//}

$me = "$jmolserver/$myname";

if (isset($source)) {
  $showHelp = 0;
  $isSource = 1;
  if (strrpos($source,"http://") === 0) {
  } else {
    exit(0);
  }
} else if (isset($model)) {
  $showHelp = 0;
} else if (isset($pdbid)) {
  $showHelp = 0;
  $isPdb = 1;
} else {
  # demo -- no parameters
  $model = $defaultModel;
}


if (isset($isfirst) && $isfirst != "true") {
  $addJS = false;
} else {
	$addJS = true;
}

if (isset($width) && isset($height)) {
  $showHelp = 0;
} else if(isset($width)) {
  $height = $width;
} else if (isset($height)) {
  $width = $height;
} else {
  $height = $defaultHeight;
  $width = $defaultWidth;
}

if ($showHelp) {
  $title = '';
} else if (isset($title)) {
  $haveTitle = 1;
} else {
  $title = $model;
}

if ($showHelp || !isset($use) || $use != "JAVA") {
	$isHTML5 = true;
} else {
	$isHTML5 = false;
}

if ($isHTML5) {
  $inlineJS  = "files/JSmolMin2.js";
  $j2s = "Jmol.Info.j2sPath = '$jmolserver/jsmol/j2s';Jmol.Info.serverURL='http://chemapps.stolaf.edu/jmol/jsmol/php/jsmol.php'	";
} else {
  $inlineJS  = "files/Jmol.js";
  $j2s = "";
}

$jmoljs = "$jmolserver/$inlineJS";

if (strrpos($model, "InChI") === 0) {
} else {
  // cannot escape the / here. 
  $model = str_replace("%2F", "/", urlencode($model));
}

if (isset($link)) {

  # we are just producing an anchor tag, not actually getting the model

  if (isset($script)) {
    $script = "&script=".urlencode($script);
  } else {
    $script = '';
  }
  if (isset($caption)) {
    $script = "$script&caption=".urlencode($caption);
  }
  if ($isPdb) {
    $m = "pdbid=$pdbid";
  } else if ($isSource) {
    $m = "source=$source";
  } else {
    $m = "model=$model";
  }
  if ($haveTitle) {
    $m += "title=$title&$m";
  }

  $s = "win".rand(1,100000);
  $s = "<a href='javascript:void(0)' onclick='window.open(\"$me?$m&width=100%&height=100%$script\",\"$s\",\"width=$width,height=$height,scrollbars=no\")'>$link</a>";
  $s = str_replace("\"",'\"', $s);
  $s = "document.write(\"$s\");";
  echo $s;
  exit(1);
}

if ($get) {
  $fname = "";
  if ($isPdb) {
    $fname = str_replace("PDBID",$pdbid,$pdbServer);
  } else if ($isSource) {
    $fname = $source;
  }
  if ($fp = fopen($fname, 'r')) {
    stream_set_timeout($fp, $timeout);
    while ($line = fread($fp, 1024)) {
      echo $line;
    }
    $info = stream_get_meta_data($fp);
    fclose($fp);
    if ($info['timed_out']) {
      echo 'Connection timed out!';
    }
  }
  exit(1);
}

$cactvsCall = '';
if (!$isPdb && !$isSource) {
  $cactvsCall = str_replace("MODEL",$model,$cactvsURL);
}

$data = '';
$isError = 0;

if ($isPdb || $isSource) {

  $data = "none";

} else {

  # check that NIH is up and running

  if ($isDebug) {
    echo "cactvsCall = $cactvsCall";
  }

  if ($testping) {
    exec("ping -c 1 -W 1 $cactvsHost 2>&1", $output, $retval);
  }
  if ($testping && strpos($output[1],"bytes from") === false) {
    $data = "$cactvsHost is unavailable at this time.";
    $isError = 1;
  } else if ($fp = fopen($cactvsCall, 'r')) {

    # get the model data from NIH

    stream_set_timeout($fp, $timeout);
    while ($line = fread($fp, 1024)) {
      $data .= $line;
    }
    $info = stream_get_meta_data($fp);
    fclose($fp);
    if ($info['timed_out']) {
      $data = "Connection timed out at $cactvsHost";
      $isError = 1;
    }
  } else {
    $data = "ERROR opening $cactvsCall";
    $isError = 1;
  }
}


# use the Jmol load DATA command to load the data inline.

if ($data == '') {
  $data = "unknown error";
  $isError = 1;
}


if ($isError) {
  $data = "$data  -- $model could not be loaded.";
  if ($inline) {
    echo "document.write(\"$data\")";
  } else {
    echo $data;
  }
  exit(1);
}

# script can be a JavaScript variable (not applicable to pop-up) or actual script

if (isset($script)) {
  # must escape \" as \\\\" and " as \"
  $script = "+\"".str_replace("\"",'\"',str_replace("\\\"",'\\\"',$script))."\"";
} else if (isset($scriptvar)) {
  $script = "+$scriptvar";
} else {
  $script = '';
}

if ($isPdb) {
    $script = "\"load $me?get&pdbid=$pdbid\;\"$script";
} else if ($isSource) {
#    $script = "\"load \\\"$me?get&source=$source\\\"\;\"$script";
    $script = "\"load \\\"$source\\\"\;\"$script";
} else {
    $script = "\"load data \\\"mydata\\\"\n$data\nend \\\"mydata\\\";\"$script";
    $script = str_replace("\n","|", $script);
}

$js = "\n$j2s\njmolInitialize('$jmolserver/files', true);\njmolApplet(['$width','$height'],$script);";

if (!$inline)
    echo "<!DOCTYPE html><html style='height:100%'><head><title>$title</title></head><body style='height:100%'>";
    
if ($showHelp) {
	# no model, no width or height
	
	# probably just http://chemapps.stolaf.edu/jmol/jmol.php
	
	echo "\n<b>$me version $VER";
	echo "\n<br><br>Easy Jmol insertion into a web page</b>";
	echo "\n<br><br><div style='width:600px'>This site allows insertion of a Jmol applet into a web page with nothing more than a simple &lt;script> or &lt;a> tag. No model files are necessary -- the 3D model is obtained from the <a href='http://www.rcsb.org'>RCSB</a> using a PDB id or from the <a href='http://$cactvsHost'>NIH CACTVS server</a> based on a chemical identifier such as a name, <a href=http://en.wikipedia.org/wiki/Simplified_molecular_input_line_entry_specification>SMILES</a> string, <a href=http://www.iupac.org/inchi/>InChI</a> key, or CAS registry number.";
	echo "\n<br><br>Bob Hanson, St. Olaf College, <a href=mailto:hansonr@stolaf.edu>hansonr@stolaf.edu</a></div>";
	echo "\n<pre>";
	echo "\n anchor format (creates link to opens new tab/page or replaces current page):";
	echo "\n    <b>&lt;a target=_blank href=\"$me?model=acetone\">3D model&lt;/a></b>";
	echo "\n popup format (creates link to pop up a new window of a specified size):";
	echo "\n    <b>&lt;script type=\"text/javascript\" src=\"$me?model=acetone&link=3D model\">&lt;/script></b>";
	echo "\n inline format (displays applet on current page):";
	echo "\n    <b>&lt;script type=\"text/javascript\" src=\"$me?model=acetone&inline\">&lt;/script></b>";
	echo "\n options:";
	echo "\n             &source=...      (option 1) file source of model, starting with http://";
	echo "\n             &model=...       (option 2) compound name or SMILES string for NIH CACTVS server <a href='$cactvsCall'>test</a>";
	echo "\n             &pdbid=xxxx      (option 3) PDB ID for model from RCSB <a href=".str_replace("PDBID","1crn",$pdbServer).">test</a>";
	echo "\n             &use=JAVA        optional Java applet rather than HTML5"; 
	echo "\n             &isfirst=true    set this false if this is not the first applet on the page; only for inline"; 
	echo "\n             &inline          optional return JavaScript to create applet on current page";
	echo "\n             &link=...        optional text for &lt;a href=...>xxxx</a> tag for a popup window";
	echo "\n             &script=...      optional script (should be simple)";
	echo "\n             &scriptvar=...   optional predefined script variable  (JavaScript variable name or function, not the script itself; &inline only)";
	echo "\n             &width=...       optional width, e.g.: 300 or 100%";
	echo "\n             &height=...      optional height, e.g.: 300 or 100%";
	echo "\n             &caption=...     optional caption (only for anchor-type - not &inline and no &link)";
	echo "\n             &title=...       optional title (not &inline)";
	echo "\n\n anchor example: <a target=_blank href=\"$me?model=aspirin&caption=aspirin%20model&lt;script>jmolCheckbox('spin on','spin off','spin on/off')&lt;/script>\">$me?model=aspirin&caption=aspirin model &lt;script>jmolCheckbox('spin on','spin off','spin on/off')&lt;/script></a>";
	echo "\n\n popup example: <script type='text/javascript' src='$me?model=acetone&link=3D model of acetone&caption=acetone&script=background white;rotate x 180'></script>";
	echo "\n\n inline example: ";
	echo "\n</pre>";
	echo "<table border=1 cellpadding=10><tr><td>";
	echo "caffeine";
	echo "<div style=\"width:400px;height:300px\"><script src=\"$me?model=caffeine&inline=1\"></script></div>";
	echo "</td><td>";
	echo "tylenol";
	echo "<div style=\"width:400px;height:300px\"><script src=\"$me?model=tylenol&inline=1&isfirst=false\"></script></div>";
	echo "</td></tr></table>";
	
} else if ($inline) {

  # if sending JavaScript, we inject Jmol.js directly into the page
  # because some browsers won't let us construct a <script src=...> tag
  # to a cross-host location. (Pretty sensible, really.)

	if ($addJS) {
    $data = '';
    if ($fp = fopen($inlineJS, 'r')) {
      while ($line = fread($fp, 1024)) {
        $data .= $line;
      }
    } else {
      echo "document.write('could not find $inlineJS')";
      exit(1);
    }
    echo "// $me\n\n";
    echo "/////// $jmoljs ".gmdate(DATE_RFC822)." ///////\n\n$data\n\n/////// END OF $jmoljs ///////\n\n";
  }
    
  echo "$js\n";    

} else {

  # a pop-up window or from the URL entry line of a browser without the &inline flag
	echo "<div style='height:95%'>";
  echo "<script src=$jmoljs></script>";    
	echo "<script type='text/javascript' language='javascript'>$js</script>";
  if (isset($caption)) {
    echo "<div style='width:$width"."px'>$caption</div>";
  }
  echo "</div>";
  echo "</body></html>";

}

?>
