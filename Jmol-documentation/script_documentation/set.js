

newType('b','ON/OFF or TRUE/FALSE')
newType('i','integer')
newType('s','quoted string')
newType('f','decimal')
newType('color','(color name), [r,g,b],[xRRGGBB]')
newType('atm','ON/OFF or decimal(Angstroms) or integer(1/250 Angstroms)')
newType('xyz','a coordinate, in braces')
newType('v','variable -- see command')
newType('ix iy','integer pixel offsets')
newType('x','an atom expression, in braces')
newType('c','cell or integer ijk')






newVar('allowEmbeddedScripts','b')
newVar('allowRotateSelected','b')
newVar('ambient','_ambientPercent')
newVar('ambientpercent','i')
newVar('animationFps','i')
newVar('animFrameCallback','s')
newVar('antialiasDisplay','b')
newVar('antialiasImages','b')
newVar('antialiasTranslucent','b')
newVar('appendNew','b')
newVar('appletProxy','s')
newVar('applySymmetryToBonds','b')
newVar('autobond','b')
newVar('autoFPS','b')
newVar('axes','_axesMode')
newVar('axesOrientationRasmol','reserved')
newVar('axesMode','i')
newVar('axesMolecular','b')
newVar('axesScale','f')
newVar('axesUnitcell','b')
newVar('axesWindow','b')
newVar('axis1Color','s')
newVar('axis2Color','s')
newVar('axis3Color','s')
newVar('background','_backgroundColor')
newVar('backgroundColor','s')
newVar('backgroundModel','i')
newVar('bond','_showMultipleBonds')
newVar('bondmode','_bondModeOr')
newVar('bondModeOr','b')
newVar('bondRadiusMilliAngstroms','i')
newVar('bonds','_showMultipleBonds')
newVar('bondTolerance','f')
newVar('boundbox','_see {#.boundbox~}')
newVar('cameraDepth','f')


newVar('cartoonRockets','b')



newVar('chainCaseSensitive','b')
newVar('charge','_formalCharge')
newVar('color','_defaultColorScheme')
newVar('colorRasmol','b')
newVar('colour','_defaultColorScheme')
newVar('dataSeparator','s')

newVar('debugScript','b')
newVar('defaultAngleLabel','s')
newVar('defaultcolors','_defaultColorScheme')
newVar('defaultColorScheme','s')
newVar('defaultDirectory','s')
newVar('defaultDistanceLabel','s')
newVar('defaultDrawArrowScale','f')
newVar('defaultLattice','c')
newVar('defaultLoadScript','s')
newVar('defaults','s')

newVar('defaultTorsionLabel','s')
newVar('defaultTranslucent','f')
newVar('defaultVDW','s')
newVar('diffuse','_diffusePercent')
newVar('diffusepercent','i')
newVar('dipoleScale','f')
newVar('disablePopupMenu','b')
newVar('display','_see {#.selectionHalos~selectionHalos ON/OFF}')
newVar('displayCellParameters','b')
newVar('dotDensity','i')
newVar('dotsSelectedOnly','b')

newVar('dotSurface','b')
newVar('drawHover','b')
newVar('drawPicking','b')
newVar('dynamicMeasurements','b')
newVar('echo','vv')
newVar('ellisoidarcs','#.ellipsoid')
newVar('ellisoidaxes','#.ellipsoid')
newVar('ellisoidballs','#.ellipsoid')
newVar('ellisoiddots','#.ellipsoid')
newVar('ellisoidfill','#.ellipsoid')
newVar('ellisoiddotCount','#.ellipsoid')
newVar('ellisoidAxesDiameter','#.ellipsoid')
newVar('exportDrivers','s')
newVar('fontsize','_see {#.font~font labels}')
newVar('forceAutoBond','b')
newVar('formalcharge','ii')
newVar('frank','_see {#.frank~frank ON/OFF}')
newVar('greyscaleRendering','b')
newVar('hbond','_see {#.set_hbondsbackbone~set hbondsBackbone} and {#.set_hbondssolid~set hbondsSolid}')
newVar('hbonds','_see {#.set_hbondsbackbone~set hbondsBackbone} and {#.set_hbondssolid~set hbondsSolid}')
newVar('hbondsBackbone','b')
newVar('hbondsSolid','b')
newVar('helpPath','s')
newVar('hermiteLevel','i')
newVar('hetero','_selectHetero')
newVar('hideNameInPopup','b')
newVar('hideNavigationPoint','b')
newVar('hideNotSelected','b')
newVar('highResolution','b')
newVar('history','_see {#.history~}')
newVar('historyLevel','i')
newVar('hoverCallback','s')
newVar('hoverDelay','f')
newVar('hoverLabel','s')
newVar('hydrogen','_selectHydrogen')
newVar('hydrogens','_selectHydrogen')
newVar('isosurfacePropertySmoothing','b')
newVar('justifyMeasurements','b')
newVar('label','_see {#.label~} and {#.setlabels~set (labels)}')
newVar('labelAlignment','LEFT,RIGHT,CENTER')
newVar('labelAtom','TRUE,FALSE')
newVar('labelFront','TRUE,FALSE')
newVar('labelGroup','TRUE,FALSE')
newVar('labelOffset','ix iy')
newVar('labelPointer','b,BACKGROUND,NONE')
newVar('labels','_see {#.label~} and {#.setlabels~set (labels)}')
newVar('labelToggle','exp')
newVar('language','s')
newVar('loadFormat','s')
newVar('loadStructCallback','s')
newVar('logLevel','i')
newVar('measure','_see {#.set_measurements~set measurements} and {#.set_measurementlabels~set measurementLabels} and {#.set_measurementunits~set measurementUnits}')
newVar('measureAllModels','b')
newVar('measurement','_see {#.set_measurements~set measurements} and {#.set_measurementlabels~set measurementLabels} and {#.set_measurementunits~set measurementUnits}')
newVar('measurementLabels','b')
newVar('measurementNumbers','_measurementLabels')
newVar('measurements','b,ANGSTROMS, AU, BOHR, NM, NANOMETERS,PM, or PICOMETERS')
newVar('measurementUnits','s')
newVar('measures','_see {#.set_measurements~set measurements} and {#.set_measurementlabels~set measurementLabels} and {#.set_measurementunits~set measurementUnits}')
newVar('messageCallback','s')
newVar('minBondDistance','f')
newVar('monitor','_see {#.set_measurements~set measurements} and {#.set_measurementlabels~set measurementLabels} and {#.set_measurementunits~set measurementUnits}')
newVar('monitors','_see {#.set_measurements~set measurements} and {#.set_measurementlabels~set measurementLabels} and {#.set_measurementunits~set measurementUnits}')

newVar('navigationDepth','f')
newVar('navigationMode','b')
newVar('navigationPeriodic','b')
newVar('navigationSlab','f')
newVar('navigationSpeed','f')
newVar('pdbGetHeader','b')
newVar('pdbSequential','b')
newVar('percentVdwAtom','i')
newVar('perspectiveDepth','b')
newVar('perspectiveModel','i')
newVar('pickCallback','s')
newVar('picking','s')
newVar('pickingSpinRate','i')
newVar('pickingStyle','s')
newVar('pointGroupDistanceTolerance','f')
newVar('pointGroupLinearTolerance','f')
newVar('property','reserved')
newVar('propertyAtomNumberColumnCount','i')
newVar('propertyAtomNumberField','i')
newVar('propertyDataFieldColumnCount','i')
newVar('propertyDataField','i')
newVar('propertyColorScheme','s')
newVar('quaternionFrame','C,P,Q')
newVar('radius','_solventProbeRadius')
newVar('rangeSelected','b')
newVar('refreshing','b')
newVar('resizeCallback','s')
newVar('ribbonAspectRatio','i')
newVar('ribbonBorder','b')
newVar('rocketBarrels','b')
newVar('rotationRadius','f')
newVar('scale3d','_scaleAngstromsPerInch')
newVar('scaleAngstromsPerInch','f')
newVar('scriptQueue','b')
newVar('scriptReportingLevel','i')
newVar('selectionhalo','_see {#.selectionHalos~selectionHalos ON/OFF}')
newVar('selectionHalos','_see {#.selectionHalos~selectionHalos ON/OFF}')
newVar('selectHetero','b')
newVar('selectHydrogen','b')
newVar('sheetSmoothing','f')
newVar('showAxes','b')
newVar('showBoundBox','b')
newVar('showFrank','b')
newVar('showHiddenSelectionHalos','b')
newVar('showHydrogens','b')
newVar('showMeasurements','b')
newVar('showMultipleBonds','b')
newVar('showNavigationPointAlways','b')
newVar('showScript','i')
newVar('showSelections','_selectionHalos')
newVar('showUnitcell','b')
newVar('slabEnabled','b')
newVar('smartAromatic','b')
newVar('solvent','_solventProbe')
newVar('solventProbe','b')
newVar('solventProbeRadius','f')
newVar('specpower','_see {#.set_specularpower~set specularPower} and  {#.set_specularexponent~set specularExponent}')
newVar('specular','_see {#.set_specular~set specular} and  {#.set_specularPercent~set specularPercent}')
newVar('specularExponent','i')
newVar('specPercent','_specularPercent')
newVar('specularPercent','i')
newVar('specularPower','i')
newVar('spin','_see {#.set_spinx~set spinX},  {#.set_spiny~set spinY},  {#.set_spinz~set spinZ}, and  {#.set_spinfps~set spinFPS} ')
newVar('spinX','i')
newVar('spinY','i')
newVar('spinZ','i')
newVar('spinFps','i')
newVar('ssbond','_ssBondsBackbone')
newVar('ssbonds','_ssBondsBackbone')
newVar('ssBondsBackbone','b')
newVar('stateVersion','i')
newVar('statusReporting','b')
newVar('stereoDegrees','f')
newVar('strand','_see {#.set_strandcountformeshribbon~strandCountForMeshRibbon} and  {#.set_strandcountforstrands~set strandCountForStrands}')
newVar('strandCount','_see {#.set_strandcountformeshribbon~strandCountForMeshRibbon} and  {#.set_strandcountforstrands~set strandCountForStrands}')
newVar('strandCountForMeshRibbon','i')
newVar('strandCountForStrands','i')
newVar('strands','_see {#.set_strandcountformeshribbon~strandCountForMeshRibbon} and  {#.set_strandcountforstrands~set strandCountForStrands}')
newVar('syncMouse','b')
newVar('syncScript','b')
newVar('testFlag1','reserved')
newVar('testFlag2','reserved')
newVar('testFlag3','reserved')
newVar('testFlag4','reserved')
newVar('toggleLabel','_labelToggle')
newVar('traceAlpha','b')
newVar('unitcell','_see {#.unitcell~}')
newVar('useNumberLocalization','b')
newVar('vectorScale','f')
newVar('vibrationPeriod','f')
newVar('vibrationScale','f')
newVar('visualRange','f')
newVar('wireframeRotation','b')
newVar('windowCentered','b')
newVar('zeroBasedXyzRasmol','reserved')
newVar('zoomEnabled','b')
newVar('zoomLarge','b')
newVar('zShade','b')
