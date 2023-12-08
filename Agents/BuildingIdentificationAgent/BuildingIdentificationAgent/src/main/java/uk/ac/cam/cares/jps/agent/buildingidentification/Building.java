package uk.ac.cam.cares.jps.agent.buildingidentification;

import org.locationtech.jts.geom.Geometry;

public class Building {

    private Integer id;
    private String companyName;
    private String iri;
    private Geometry footprint;
    private Double height;

    Building(String companyName, Geometry footprint, Double height) {
        this.companyName = companyName;
        this.footprint = footprint;
        this.height = height;
    }

    public void setFootprint(Geometry footprint) {
        this.footprint = footprint;
    }

    public String getName() {
        return companyName;
    }

    public Geometry getFootprint() {
        return footprint;
    }

    public Double getHeight() {
        return height;
    }

    public Integer getId() {
        return id;
    }

    public String getIri() {
        return iri;
    }

}
