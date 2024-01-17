package uk.ac.cam.cares.jps.agent.buildingidentification.objects;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

public class Building {

    public String iri = null;
    public String name = null;
    public Point location = null;
    public Integer buildingId = -1;
    public Geometry buildingFootprint = null;
    public double buildingHeight = 0.0;
    public double heatEmission = 0.0;

    public Building(String iri, Point location) {
        this.iri = iri;
        this.location = location;
    }

}
