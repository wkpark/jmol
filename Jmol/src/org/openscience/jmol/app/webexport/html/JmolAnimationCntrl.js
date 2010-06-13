/*jmolAnimationCntrl.js

J. Gutow May 2010

This includes one function that controls the highlighting of the
animation mode buttons
*/

function jmol_animationmode(selected, n){
    var cellID = "jmol_loop_"+n;
    document.getElementById(cellID).removeAttribute("style");
    cellID = "jmol_playOnce_"+n;
    document.getElementById(cellID).removeAttribute("style");
    cellID = "jmol_palindrome_"+n;
    document.getElementById(cellID).removeAttribute("style");
    if (selected=="loop") {
        cellID = "jmol_loop_"+n;
        document.getElementById(cellID).setAttribute("style","background: blue;");
        var result  = jmolScript('animation mode loop', n);
    }
    if (selected=="playOnce") {
        cellID = "jmol_playOnce_"+n;
        document.getElementById(cellID).setAttribute("style","background: blue;");
        var result  = jmolScript('animation mode once', n);
    }
    if (selected=="palindrome") {
        cellID = "jmol_palindrome_"+n;
        document.getElementById(cellID).setAttribute("style","background: blue;");
        var result  = jmolScript('animation mode palindrome',n);
    }
}
