type ..\..\workspace\jmol\src\org\jmol\symmetry\CIPChirality.java | find /V "}"|find /V "//" |find /V "import" | find /V "*" |find ";"|find /V "Logger"|find /V "System.out"  > t


type ..\..\workspace\jmol\src\org\jmol\symmetry\CIPChirality.java | find /V "}"|find /V "//" |find /V "import" | find /V "*" |find "if"|find /V "Logger"|find /V "System.out"  >> t

type t |find " " /C