package uk.ac.cam.cares.jps.agent.buildingidentification;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.parsers.wkt.WKTReader;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cmclinnovations.stack.clients.gdal.GDALClient;
import com.cmclinnovations.stack.clients.gdal.Ogr2OgrOptions;
import com.cmclinnovations.stack.clients.geoserver.GeoServerClient;
import com.cmclinnovations.stack.clients.geoserver.GeoServerVectorSettings;
import com.cmclinnovations.stack.clients.geoserver.UpdatedGSVirtualTableEncoder;

import uk.ac.cam.cares.jps.base.query.RemoteRDBStoreClient;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;
import uk.ac.cam.cares.jps.base.util.CRSTransformer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.Point;

import uk.ac.cam.cares.jps.agent.buildingidentification.objects.Building;

public class OntoChemPlant {
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String URI_RUN = "/run";
    public static final String KEY_DISTANCE = "maxDistance";
    private static final String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String geoSparql = "http://www.opengis.net/ont/geosparql#";

    private static final String ontoCompanyPrefix = "http://www.theworldavatar.com/kg/ontocompany/";
    private static final String ontoChemPlantPrefix = "http://www.theworldavatar.com/kg/ontochemplant/";
    private static final String contactPrefix = "http://ontology.eil.utoronto.ca/icontact.owl#";
    private static final String ontoMeasurePrefix = "http://www.ontology-of-units-of-measure.org/resource/om-2/";
    private static final String buildingType = "http://www.purl.org/oema/infrastructure/Building";

    private RemoteRDBStoreClient rdbStoreClient;
    private RemoteStoreClient storeClient;

    private List<Building> buildings = new ArrayList<>();

    private static final Logger LOGGER = LogManager.getLogger(OntoChemPlant.class);

    // Properties of database containing buildings data.
    private String dbUrl = null;
    private String dbUser = null;
    private String dbPassword = null;
    private String dbName = null;
    private int dbSrid;
    private double maxDistance;
    private int numberBuildingsIdentified = 0;
    private int numberBuildingsQueried = 0;

    public JSONObject processRequestParameters(JSONObject requestParams) {

        String endpoint = null;
        EndpointConfig endpointConfig = null;

        if (requestParams.has("endpoint"))
            endpoint = requestParams.getString("endpoint");
        else {
            String namespace = requestParams.getString("namespace");
            endpointConfig = new EndpointConfig();
            endpoint = endpointConfig.getKgurl(namespace);
        }

        storeClient = new RemoteStoreClient(endpoint);

        if (requestParams.has("dbUrl")) {
            dbUrl = requestParams.getString("dbUrl");
            dbUser = requestParams.getString("dbUser");
            dbPassword = requestParams.getString("dbPassword");
        } else {
            dbName = requestParams.getString("dbName");
            dbUrl = endpointConfig.getDbUrl(dbName);
            dbUser = endpointConfig.getDbUser();
            dbPassword = endpointConfig.getDbPassword();
        }

        rdbStoreClient = new RemoteRDBStoreClient(dbUrl, dbUser, dbPassword);
        getDbSrid();
        maxDistance = Double.parseDouble(requestParams.getString(KEY_DISTANCE));
        // Reset all variables
        buildings.clear();
        numberBuildingsQueried = 0;
        numberBuildingsIdentified = 0;

        getBuildingProperties();
        linkBuildings();
        createFactoriesTable();
        createGeoServerLayers();

        JSONObject responseObject = new JSONObject();
        responseObject.put("number_buildings_queried", numberBuildingsQueried);
        responseObject.put("number_buildings_identified", numberBuildingsIdentified);
        return responseObject;
    }

    /**
     * Queries database for the SRID
     * 
     */
    private void getDbSrid() {
        try (Connection conn = rdbStoreClient.getConnection();
                Statement stmt = conn.createStatement();) {
            String sqlString = "SELECT srid,gml_srs_name from citydb.database_srs";
            ResultSet result = stmt.executeQuery(sqlString);
            if (result.next()) {
                dbSrid = result.getInt("srid");
            } else {
                LOGGER.warn("Could not retrieve srid from database.");
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void getBuildingProperties() {

        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ontocompany", ontoCompanyPrefix)
                .addPrefix("con", contactPrefix).addPrefix("rdfs", rdfs)
                .addPrefix("om", ontoMeasurePrefix).addPrefix("rdf", rdf)
                .addPrefix("ocp", ontoChemPlantPrefix)
                .addPrefix("geo", geoSparql);

        String addressVar = "?address";
        wb.addWhere("?building", "rdf:type", NodeFactory.createURI(buildingType))
                .addWhere("?building", "con:hasAddress", addressVar)
                .addWhere("?company",
                        NodeFactory.createURI("http://www.theworldavatar.com/kg/ontocape/upperlevel/system/isownerof"),
                        "?factory")
                .addWhere("?factory", "geo:ehContains", "?building")
                .addWhere("?company", "rdfs:label", "?name")
                .addWhere(addressVar, "ocp:hasLongitudeEPSG24500", "?lon").addWhere(addressVar,
                        "ocp:hasLatitudeEPSG24500", "?lat")
                .addOptional("?building", "ontocompany:hasGeneratedHeat/om:hasValue/om:hasNumericalValue", "?heat");

        SelectBuilder sb = new SelectBuilder()
                .addVar("building")
                .addVar("name")
                .addVar("lon")
                .addVar("lat")
                .addVar("heat")
                .addWhere(wb);

        JSONArray queryResult = storeClient.executeQuery(sb.buildString());

        for (int i = 0; i < queryResult.length(); i++) {
            String buildingIri = queryResult.getJSONObject(i).getString("building");
            Double longitude = queryResult.getJSONObject(i).getDouble("lon");
            Double latitude = queryResult.getJSONObject(i).getDouble("lat");
            String name = queryResult.getJSONObject(i).getString("name");
            Double heat = null;
            if (queryResult.getJSONObject(i).has("heat")) {
                heat = queryResult.getJSONObject(i).getDouble("heat");
            }
            Point buildingLocation = new Point(
                    new CoordinateArraySequence(
                            new Coordinate[] { new Coordinate(longitude, latitude) }),
                    new GeometryFactory());

            buildingLocation.setSRID(dbSrid);
            Building building = new Building(buildingIri, buildingLocation);
            if (heat != null)
                building.heatEmission = heat;
            building.name = name;
            buildings.add(building);

        }

        numberBuildingsQueried = buildings.size();
    }

    /**
     * Identifies the building whose envelope centroid is closest to the coordinate
     * of each factory.
     * 
     * 
     * @return None
     */

    private void linkBuildings() {

        try (Connection conn = rdbStoreClient.getConnection();
                Statement stmt = conn.createStatement();) {

            for (Building bld : buildings) {
                String sqlString = String.format(
                        "select cityobject.id, public.ST_AsText(envelope) as wkt, measured_height, " +
                                "public.ST_DISTANCE(public.ST_Point(%f,%f, %d), envelope) as dist" +
                                System.lineSeparator() +
                                "from citydb.cityobject, citydb.building "
                                +
                                System.lineSeparator() +
                                " WHERE cityobject.objectclass_id = 26 AND cityobject.id = building.id AND public.ST_INTERSECTS(public.ST_Point(%f,%f, %d), envelope)",
                        bld.location.getX(), bld.location.getY(), dbSrid, bld.location.getX(), bld.location.getY(),
                        dbSrid);

                ResultSet result = stmt.executeQuery(sqlString);

                double dist = Double.MAX_VALUE;
                boolean matched = false;

                while (result.next()) {
                    matched = true;

                    Double currentDistance = result.getDouble("dist");
                    if (currentDistance < dist) {
                        bld.buildingId = result.getInt("id");
                        String wktLiteral = result.getString("wkt");
                        bld.buildingFootprint = WKTReader.extract(wktLiteral).getGeometry();
                        bld.buildingHeight = result.getDouble("measured_height");
                        dist = currentDistance;
                    }
                }

                if (matched)
                    numberBuildingsIdentified++;
                else
                    LOGGER.warn("No footprint exists for building with IRI {} ",
                            bld.iri);

                if (dist > maxDistance)
                    LOGGER.warn("Nearest footprint for building with IRI {} is {} meters away",
                            bld.iri, dist);

            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

    }

    /*
     * Creates POSTGIS table with factory properties.
     */

    private void createFactoriesTable() {

        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");
        JSONArray features = new JSONArray();

        for (Building bld : buildings) {

            Geometry footPrint = bld.buildingFootprint;
            if (footPrint == null)
                continue;
            JSONObject feature = new JSONObject();
            feature.put("type", "Feature");
            JSONObject properties = new JSONObject();
            properties.put("iri", bld.iri);
            properties.put("heat", bld.heatEmission);
            properties.put("name", bld.name);
            properties.put("building_id", bld.buildingId);
            feature.put("properties", properties);
            features.put(feature);

        }

        featureCollection.put("features", features);

        GDALClient gdalClient = GDALClient.getInstance();
        gdalClient.uploadVectorStringToPostGIS(dbName, "citydb.chemical_plants_buildings",
                featureCollection.toString(), new Ogr2OgrOptions(), false);
        LOGGER.info("Created chemical_plants_buildings table with {} records.", numberBuildingsIdentified);

    }

    /**
     * Creates Geoserver layers for all industries
     */

    private void createGeoServerLayers() {

        String geoserverWorkspace = "heat";
        GeoServerClient geoServerClient = GeoServerClient.getInstance();
        geoServerClient.createWorkspace(geoserverWorkspace);

        GeoServerVectorSettings geoServerVectorSettings = new GeoServerVectorSettings();
        UpdatedGSVirtualTableEncoder virtualTable = new UpdatedGSVirtualTableEncoder();
        String sqlString = "select building_id, heat, citydb.chemical_plants_buildings.name, envelope, measured_height as height, iri "
                +
                "from citydb.cityobject, citydb.building, citydb.chemical_plants_buildings where  "
                +
                " citydb.building.id = citydb.cityobject.id AND building_id = citydb.building.id";

        virtualTable.setSql(sqlString);
        virtualTable.setEscapeSql(true);
        String tableName = "BuildingsJI";
        virtualTable.setName(tableName);
        virtualTable.addVirtualTableGeometry("wkb_geometry", "Polygon", String.valueOf(dbSrid));
        geoServerVectorSettings.setVirtualTable(virtualTable);
        geoServerClient.createPostGISLayer(geoserverWorkspace, dbName, tableName, geoServerVectorSettings);

    }

}
