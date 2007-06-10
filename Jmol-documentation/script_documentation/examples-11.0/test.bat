copy c:\jmol-dev\workspace\jmol-v11_0\build\*.jar .

if %1x == x goto exit
goto OK


start sym.htm
start altloc.htm
start measure.htm
start fraction.htm
start pmesh.htm
start dipole.htm
start poly.htm
start mo.htm
start draw.htm
start lcao.htm
start jmol.jar caffeine.xyz
start timing.htm
start timing2.htm
start isosurface.htm
start draw.htm
start new.htm

:OK

start %1.htm

:exit