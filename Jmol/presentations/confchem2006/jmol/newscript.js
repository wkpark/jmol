function getModelSelect(){
 if(!Models || Models.length==0)return ""
 var sout="<select id=modellist onClick=loadModel(value) onkeyup=\"setTimeout('loadModel()',100)\"><option value='0'>Select a model</option>"
 for (var i=1;i<Models.length;i++){
  sout+="\n<option value='"+i+"'>"+Models[i]+"</option>"
 }
 sout+="</select>"
 return sout
}

NewScript=new Array(" "

	,' This prototype file demonstrates new features in Jmol. Click first on a <font color=red>load command</font>, then go down the list in order (or not).'+ getModelSelect()


	,'### higher zoom capability ###'
	,'load kinesin.pdb;select not protein and not solvent;spacefill 2.0'
	,'zoom 1000'
	,'zoom 5000'
	,'zoom 10000'
	,'zoom 20000'
	,'zoom 20001 # out of range'
	,'reset;set perspectiveDepth off'
	,'zoom 100'
	,'zoom 400'
	,'zoom 800'
	,'zoom 2000'
	,'# Note how zoom is much more powerful with perspectiveDepth off'
	,'reset;set perspectiveDepth on'

	,'### HBONDS CALCULATE ###'
	,'load 1crn.pdb'
	,'hbonds on'
	,'# INTENTIONALLY NO LONGER WORKS TO CREATE HYDROGEN BONDS.'
	,'hbonds calculate'
	,'# the exact equivalent of the old "hbonds on"'
	,'load 1crn.pdb'
	,'select 1-20; hbonds calculate'
	,'# as with hbonds on, hbonds calculate takes into consideration the current selection set.'

	,'### new Label format options ###'
	,'load 1crn.pdb'
	,'set labelOffset 0 10'
	,'select *.CA; label %n'
	,'#  the %n format is still the three-character code for amino acid residues'
	,'select *.CA; label %m'
	,'#  the new %m format is the single-character code for amino acid residues'
	,'select *.CA and atomno<10; label %5.3x %5.3y %5.3z'
	,'# standard C++ "pformat" formating is available for any numeric label option.' 
	,'select atomno=200; label __left:%-8.3x__zero-filled:%08.3y__normal:%8.3z__'


	,'### new rendering options ###'
	,'# NOTE: SET wireframeRotation ON/OFF has been removed.'
	,'# NOTE: SET oversampleAlways ON/OFF has been removed.'
	,'# NOTE: SET oversampleStopped ON/OFF has been removed.'
	,'load ZNQUKROD.MOL'
	,'# optional grey-scale rendering:'
	,'set greyScaleRendering ON'
	,'set greyScaleRendering OFF'
	,'load 1a31.pdb'
	,'set chainCaseSensitive ON'
	,'select *:a; color yellow #none selected'
	,'select *:A; color white #chain A selected'
	,'set chainCaseSensitive OFF #the default'
	,'select *:a; color yellow'

	,'### miscellaneous options ###'
	,'set hideNameInPopup TRUE'
	,'load 1crn.pdb'
	,'# right-click applet to see no name in menu, no way to view file contents in a pop-up window'
	,'set hideNameInPopup FALSE'
	,'load 1crn.pdb'
	,'# right-click applet to see name in menu and access to file contents'
	,'# note that behavior starts with the NEXT LOADED FILE'
	,'set disablePopupMenu TRUE'
	,'set disablePopupMenu FALSE'


	,'### SELECT CONNECTED() ###'
	,'load ZNQUKROD.MOL'
	,'select connected();color green'
	,'select connected(7);color red'
	,'select connected(zinc);color yellow'
	,'select connected(2,hydrogen);color blue'
	,'select hydrogen and connected(connected(2,hydrogen));color orange'

	,'### COLOR [element] ###'
	,'load ZNQUKROD.MOL'
	,'select(hydrogen); color ATOMS yellow'
	,'# Here we are referring to "all hydrogens in the model"'
	,'color hydrogen purple'
	,'# Here we mean "the default color for the element hydrogen"'
	,'# notice that no change occurs in this case, because these hydrogens have already been given a special color, yellow.' 
	,'load caffeine.xyz'
	,'# This use of color is PERSISTENT FOR THE LIFE OF THE APPLET'
	,'# and will only revert when the page is reloaded or we assign hydrogen another color.'
	,'color hydrogen white'
	,'# since we haven\'t changed their color, these atoms do go back to white.'
	,'load ZNQUKROD.MOL'
	,'# and they are white from now on'

	,'### CONNECT ###'
	,'load NaCl.mol'
	,'connect 1.0 3.0 (atomno<100) (atomno>100) SINGLE ModifyOrCreate'
	,'# the full command consists of distances, two atom sets, bond type, and create/modify option'
	,'# options for bond type include SINGLE, DOUBLE, TRIPLE, AROMATIC, PARTIAL, PARTIALDOUBLE, and HBOND'
	,'# options for create/modify include ModifyOrCreate MODIFY CREATE DELETE'
	,'connect (*) (*)'
	,'# don\'t do this!'
	,'connect'
	,'# by itself, returns to defaults for covalent bonds'
	,'connect; select atomno < 100; connect 3.0 (atomno>100)'
	,'# missing one or both atom set implies (selected)'
	,'connect; select atomno < 100;connect 3.0'
	,'# atom set can be pre-selected'

	,'load caffeine.xyz'
	,'connect 1.2 1.26 (carbon) (oxygen) DOUBLE'
	,'connect 1.5 (carbon) (connected(2,nitrogen)) DOUBLE'
	,'connect 1.5 (nitrogen and not connected(3)) (connected(1,hydrogen)) DOUBLE'
	,'connect 2.0 (carbon) (connected(2,nitrogen)) AROMATIC'
	,'connect 2.0 (nitrogen and not connected(3)) (connected(1,hydrogen)) AROMATIC'
	,'connect (oxygen) (oxygen) HBOND'
	,'# new hbonds are thin lines'
	,'connect HBOND DELETE'
	,'connect (atomno=9) (atomno=18) PARTIAL'
	,'# partial bonds are standard-radius and have a slightly different pattern'
	,'connect PARTIAL DELETE'
	,'connect AROMATIC DELETE'
	,'connect 2.0 (*) (not hydrogen) AROMATIC CREATE'
	,'select not (connected(3,hydrogen) or hydrogen);color yellow'
	,'connect HBOND MODIFY'
	,'# modified hbonds take previous bond\'s diameter'

	,'load NaCl.mol'
	,'connect 3.0 (chlorine) (sodium)'
	,'# only nearby Na-Cl formed'
	,'connect (chlorine) (sodium) DELETE'
	,'select (chlorine); connect 3.0 (sodium) #same thing'
	,'connect (chlorine) (sodium) DELETE'
	,'connect 3.0 6.0 (chlorine) (sodium) HBOND'
	,'# only distant Na-Cl formed'
	,'connect (chlorine) (sodium) HBOND DELETE'

	,'### ISOSURFACE ###'
	,'see the <a href=JMOLDOCS/index.htm#isosurface target=_blank>Jmol Script Documentation</a> for examples of drawing orbitals and other information as isosurfaces.'

	,'### PMESH ###'
	,'see the <a href=JMOLDOCS/index.htm#pmesh target=_blank>Jmol Script Documentation</a> for examples of drawing orbitals and other information as isosurfaces.'

	,'### enhanced POLYHEDRA ###'
	,'load NaCl.mol'
	,'connect 3.0 (chlorine) (sodium)'
	,'# this is needed for bonding designation in the polyhedra command'
	,'polyhedra 4,6 BONDS (chlorine) collapsed edges; color polyhedra yellow'
	,'# the new "collapsed polyhedra" idea'
	,'polyhedra 6 RADIUS 3.0 (chlorine) to (sodium)'
	,'# only the chlorines that are within 3 Angstroms of exactly 6 sodiums'
	,'select *; wireframe off; spacefill off'
	,'select connected(4); polyhedra OFF'
	,'select connected(6); polyhedra COLLAPSED'
	,'polyhedra EDGES'
	,'color polyhedra yellow'
	,'spacefill 0.5'
	,'# here we use those connections to provide clickable atoms (for distance and angle measurement)'
	,'select connected(6); spacefill 0.005'
	,'# they can be almost inperceptible but still clickable'
	,'load quartz.xyz'
	,'polyhedra 4 BONDS; color polyhedra red'
	,'# the standard look'
	,'polyhedra 4 BONDS EDGES'
	,'# edges are a nice touch'
	,'polyhedra 4 BONDS (silicon) TO (oxygen) COLLAPSED EDGES'
	,'# the "collapsed" look'
	,'load P4O10.mol'
	,'polyhedra 4 COLLAPSED EDGES'
	,'load ZNQUKROD.MOL'
	,'select zinc; polyhedra BONDS EDGES'
	,'select zinc; polyhedra BONDS COLLAPSED EDGES'
	,'select carbon; polyhedra BONDS COLLAPSED EDGES'
	,'select carbon; polyhedra BONDS COLLAPSED EDGES faceCenterOffset=0.4'
	,'load TCBEWREA.MOL'
	,'polyhedra (*) (*) COLLAPSED EDGES'
	,'# notice the effect of faceCenterOffset:'
	,'polyhedra (carbon) DELETE'
	,'polyhedra BONDS (carbon) COLLAPSED EDGES'
	,'# if you look closely, not all the planes have been drawn. This model is quite distorted, so we increase the value of faceNormalMax from its default value of 30 to 50:'
	,'polyhedra BONDS (carbon) COLLAPSED EDGES faceNormalMax=50'
	,'select not nitrogen; polyhedra BONDS COLLAPSED EDGES faceCenterOffset=0.0'
	,'# faceCenterOffset of 0 can result in lighting problems'
	,'select not nitrogen; polyhedra BONDS COLLAPSED EDGES faceCenterOffset=0.25'
	,'select not nitrogen; polyhedra BONDS COLLAPSED EDGES faceCenterOffset=0.5'
	,'see the <a href=JMOLDOCS/index.htm#polyhedra target=_blank>Jmol Script Documentation</a> for details regarding polyhedra.'

	,'### STARS ###'
	,'load caffeine.xyz'
	,'select oxygen;stars ON'
	,'stars 50% # of Van der Waals radius'
	,'stars 1.0 # Angstroms'
	,'color stars translucent'
	,'stars OFF'
	,'see the <a href=JMOLDOCS/index.htm#star target=_blank>Jmol Script Documentation</a> for additional examples of displaying a thin-lined star at atom positions.'
)
