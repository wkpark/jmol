package org.openscience.jmolandroid.search;

public class PDBResult {
    private String id;
    private String description;
    
    public PDBResult(String id, String description) {
        this.id = id;
        this.description = description;
    }
    
    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
