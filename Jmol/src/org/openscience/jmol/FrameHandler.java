public class FrameHandler{
    private static ChemFile cf;    
    private static ChemFrame frame;

    public FrameHandler() {
    }
    
    public static void setChemFile(ChemFile cf) {
        this.cf = cf;
        displayPanel.setChemFile(cf);
        Animate.setChemFile(cf);
        Vibrate.setChemFile(cf);
    }

    public static Vector getPropertyList() {
        return cf.getPropertyList();
    }
    
    public static void setFrame(int which) {
        frame = getFrame(which);
        displayPanel.setFrame(fr);
        Measurement.setChemFrame(fr);
    }
           
    public static ChemFrame getFrame(int which) {
        return cf.getFrame(which);
    }

}
        
