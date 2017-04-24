type ..\..\workspace\jmol\src\org\jmol\symmetry\CIPChirality.java | find /V "}"|find /V "//" |find /V "import" | find /V "*" |find ";"|find /V "Logger"  > t


type ..\..\workspace\jmol\src\org\jmol\symmetry\CIPChirality.java | find /V "}"|find /V "//" |find /V "import" | find /V "*" |find "if"|find /V "Logger" >> t

type t |find " " /C