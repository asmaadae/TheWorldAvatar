package uk.ac.cam.cares.jps.agent.buildingidentification;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.geosparql.implementation.parsers.wkt.WKTReader;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cmclinnovations.stack.clients.core.StackClient;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.config.JPSConstants;
import uk.ac.cam.cares.jps.base.query.RemoteRDBStoreClient;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import uk.ac.cam.cares.jps.base.util.CRSTransformer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.Point;

@WebServlet(urlPatterns = {
        BuildingIdentificationAgent.URI_RUN,
        BuildingIdentificationAgent.URI_DELETE
})
public class BuildingIdentificationAgent extends JPSAgent {
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_REQ_URL = "requestUrl";

    public static final String URI_RUN = "/run";
    public static final String URI_DELETE = "/delete";

    public static final String KEY_ROUTE = "route";
    public static final String KEY_DISTANCE = "maxDistance";
    private String ontocompanyUri, contactUri;
    private static final String rdfs = "http://www.w3.org/2000/01/rdf-schema#";

    public static final String STACK_NAME = "<STACK NAME>";
    private String stackName;
    private String stackAccessAgentBase;
    // EndpointConfig endpointConfig = new EndpointConfig();

    private RemoteRDBStoreClient rdbStoreClient;
    private RemoteStoreClient storeClient;
    private String defaultLabel;
    private Map<String, String> iriToLabel = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(BuildingIdentificationAgent.class);

    // Properties of database containing buildings data.
    // TODO: The database parameters should be sent in the POST request to this
    // agent.
    private String dbUrl = null;
    private String dbUser = null;
    private String dbPassword = null;
    private int dbSrid;
    private String sridName;
    private double maxDistance;
    private int numberBuildingsIdentified = 0, numberFactoriesQueried = 0;
    private static final String predicate = "http://data.ordnancesurvey.co.uk/ontology/spatialrelations/hasGmlId";
    private static final String cityObjectId = "http://www.theworldavatar.com/kg/ontocitygml/cityObjectId";

    public BuildingIdentificationAgent() {
        readConfig();
        stackName = StackClient.getStackName();
        stackAccessAgentBase = stackAccessAgentBase.replace(STACK_NAME, stackName);
        // rdbStoreClient = new RemoteRDBStoreClient(dbUrl, dbUser, dbPassword);
        // rdbStoreClient = new RemoteRDBStoreClient(endpointConfig.getDbUrl(dbName),
        // endpointConfig.getDbUser(),
        // endpointConfig.getDbPassword());
    }

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams, HttpServletRequest request) {
        return processRequestParameters(requestParams);
    }

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {
            if (requestParams.getString(KEY_REQ_URL).contains(URI_RUN)) {

                // Initialize values of storeClient and rdbStoreClient
                String route = requestParams.has(KEY_ROUTE) ? requestParams.getString(KEY_ROUTE)
                        : stackAccessAgentBase + defaultLabel;
                setStoreClient(route);
                dbUrl = requestParams.getString("dbUrl");
                dbUser = requestParams.getString("dbUser");
                dbPassword = requestParams.getString("dbPassword");
                rdbStoreClient = new RemoteRDBStoreClient(dbUrl, dbUser, dbPassword);
                getDbSrid();
                maxDistance = Double.parseDouble(requestParams.getString(KEY_DISTANCE));
                Map<String, double[]> factoryLocations = getFactoryLocations(route);
                Map<String, Integer> factoryToBuilding = linkBuildings(factoryLocations);
                List<Integer> cityObjectIdList = new ArrayList<>(factoryToBuilding.values());
                Map<Integer, List<Coordinate[]>> objectGeometry = getSurfaceGeometry(cityObjectIdList);
                writeCesiumInput(factoryToBuilding, objectGeometry);
                // instantiateBuildingsTriples(route, factoryToBuilding);

            } else if (requestParams.getString(KEY_REQ_URL).contains(URI_DELETE)) {
                String route = requestParams.has(KEY_ROUTE) ? requestParams.getString(KEY_ROUTE)
                        : stackAccessAgentBase + defaultLabel;
                setStoreClient(route);
                deleteBuildingsTriples(route);

            }
        }

        JSONObject responsObject = new JSONObject();
        responsObject.put("number_factories", numberFactoriesQueried);
        responsObject.put("number_buildings", numberBuildingsIdentified);
        return responsObject;
    }

    /**
     * Gets variables from config.properties
     */
    private void readConfig() {
        ResourceBundle config = ResourceBundle.getBundle("config");
        ontocompanyUri = config.getString("uri.ontology.ontocompany");
        contactUri = config.getString("uri.ontology.contact");
        stackAccessAgentBase = config.getString("access.url");
        defaultLabel = config.getString("route.label");

    }

    /**
     * Queries database for the SRID
     * 
     */
    private void getDbSrid() {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                Statement stmt = conn.createStatement();) {
            String sqlString = "SELECT srid,gml_srs_name from database_srs";
            ResultSet result = stmt.executeQuery(sqlString);
            if (result.next()) {
                dbSrid = result.getInt("srid");
                sridName = result.getString("gml_srs_name");
            } else {
                LOGGER.warn("Could not retrieve srid from database.");
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Checks validity of incoming request
     * 
     * @param requestParams Request parameters as JSONObject
     * @return Validity of request
     */
    @Override
    public boolean validateInput(JSONObject requestParams) {
        boolean validate = true;

        if (requestParams.getString(KEY_REQ_URL).contains(URI_RUN))
            validate = isNumber(requestParams.getString(KEY_DISTANCE));

        return validate;
    }

    private Map<String, double[]> getFactoryLocations(String route) {
        Map<String, double[]> factoryLocations = new HashMap<>();

        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ontocompany", ontocompanyUri)
                .addPrefix("con", contactUri).addPrefix("rdfs", rdfs);

        String addressVar = "?address";
        wb.addWhere("?factory", "con:hasAddress", addressVar)
                .addWhere("?factory", "rdfs:label", "?name")
                .addWhere(addressVar, "ontocompany:hasLongitudeEPSG4326", "?lon").addWhere(addressVar,
                        "ontocompany:hasLatitudeEPSG4326", "?lat");
        SelectBuilder sb = new SelectBuilder()
                .addVar("factory")
                .addVar("lon")
                .addVar("lat")
                .addVar("name")
                .addWhere(wb);

        JSONArray queryResult = AccessAgentCaller.queryStore(route, sb.buildString());

        for (int i = 0; i < queryResult.length(); i++) {
            String factoryIri = queryResult.getJSONObject(i).getString("factory");
            Double longitude = queryResult.getJSONObject(i).getDouble("lon");
            Double latitude = queryResult.getJSONObject(i).getDouble("lat");
            String factoryName = queryResult.getJSONObject(i).getString("name");
            String originalSrid = "EPSG:4326";
            double[] xyOriginal = { longitude, latitude };
            double[] xyTransformed = CRSTransformer.transform(originalSrid, "EPSG:" + dbSrid, xyOriginal);
            factoryLocations.put(factoryIri, xyTransformed);
            iriToLabel.put(factoryIri, factoryName);
        }

        numberFactoriesQueried = factoryLocations.size();
        return factoryLocations;

    }

    /**
     * Identifies the building whose envelope centroid is closest to the coordinate
     * of each factory.
     * 
     * @param factoryLocations HashMap whose keys are factory IRIs and values are
     *                         its coordinates
     * 
     * @return HashMap containing factory IRIs as keys and the cityObjectIds of
     *         matched buildings as values.
     */

    private Map<String, Integer> linkBuildings(Map<String, double[]> factoryLocations) {

        Map<String, Integer> factoryToBuilding = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                Statement stmt = conn.createStatement();) {

            for (String key : factoryToBuilding.keySet()) {

                double[] coords = factoryLocations.get(key);
                String sqlString = String.format("select id, ST_AsText(envelope) as wkt from cityobject" +
                        System.lineSeparator() +
                        "where public.ST_Intersects(public.ST_Point(%f,%f, %d),envelope)" +
                        System.lineSeparator() +
                        "AND objectclass_id = 26;",
                        coords[0], coords[1], dbSrid);

                ResultSet result = stmt.executeQuery(sqlString);
                Double dist = Double.MAX_VALUE;
                Point reference = new Point(
                        new CoordinateArraySequence(new Coordinate[] { new Coordinate(coords[0], coords[1]) }),
                        new GeometryFactory());
                Integer finalId = null;

                while (result.next()) {
                    Integer id = result.getInt("id");
                    String wktLiteral = result.getString("wkt");
                    Geometry scopePolygon = WKTReader.extract(wktLiteral).getGeometry();
                    Point centre = scopePolygon.getCentroid();
                    Double currentDistance = reference.distance(centre);
                    if (currentDistance < dist) {
                        dist = currentDistance;
                        finalId = id;
                    }
                }

                if (dist <= maxDistance) {
                    factoryToBuilding.put(key, finalId);
                } else {
                    LOGGER.warn("No building found for factory with IRI {} within the specified maximum distance.",
                            key);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        numberBuildingsIdentified = factoryToBuilding.size();

        return factoryToBuilding;

    }

    /**
     * 
     * @param cityObjectIdList: List of cityObject Ids which are integers.
     * @return HashMap with cityobject IDs as keys and a list of polygons
     *         (represented as Coordinate[]) as values.
     */

    private Map<Integer, List<Coordinate[]>> getSurfaceGeometry(List<Integer> cityObjectIdList) {

        String sql = "select id, cityobject_id, public.ST_AsText(geometry) as wkt from surface_geometry "
                + System.lineSeparator() +
                "where geometry is not NULL and cityobject_id in ";

        String cityObjectListString = cityObjectIdList.stream().map(x -> String.valueOf(x))
                .collect(Collectors.joining(",", "(", ")"));

        String query = sql + cityObjectListString;
        Map<Integer, List<Coordinate[]>> objectGeometry = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                Statement stmt = conn.createStatement();) {

            ResultSet result = stmt.executeQuery(query);

            while (result.next()) {
                Integer cityObjectId = result.getInt("cityobject_id");
                String wktLiteral = result.getString("wkt");
                Geometry buildingPolygon = WKTReader.extract(wktLiteral).getGeometry();
                Coordinate[] polyCoords = buildingPolygon.getCoordinates();

                List<List<Double>> polygonCoords = Arrays.stream(polyCoords)
                        .map(coord -> Arrays.asList(coord.x, coord.y, coord.z))
                        .collect(Collectors.toList());

                new JSONArray(polyCoords[0]);

                if (objectGeometry.containsKey(cityObjectId)) {
                    objectGeometry.get(cityObjectId).add(polyCoords);
                } else {
                    List<Coordinate[]> buildingSurfaces = new ArrayList<>();
                    buildingSurfaces.add(polyCoords);
                    objectGeometry.put(cityObjectId, buildingSurfaces);
                }
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return objectGeometry;

    }

    private void writeCesiumInput(Map<String, Integer> factoryToBuilding,
            Map<Integer, List<Coordinate[]>> objectGeometry) {

        JSONObject result = new JSONObject();
        result.put("type", "FeatureCollection");
        result.put("name", "data");

        JSONObject crsProperties = new JSONObject();
        crsProperties.put("properties", new JSONObject().put("name", sridName));
        crsProperties.put("type", "name");
        result.put("crs", crsProperties);

        JSONArray featureArray = new JSONArray();

        for (String factoryIri : factoryToBuilding.keySet()) {
            int cityObjectId = factoryToBuilding.get(factoryIri);
            List<Coordinate[]> polygons = objectGeometry.get(cityObjectId);
            String factoryName = iriToLabel.get(factoryIri);

            JSONObject featureObject = new JSONObject();
            featureObject.put("type", "feature");
            JSONObject propertiesObject = new JSONObject();
            propertiesObject.put("name", factoryName);
            propertiesObject.put("description", "Building of " + factoryName);
            // hard-coded for now
            propertiesObject.put("color", "rgb(2,243,6)");
            featureObject.put("properties", propertiesObject);
            // Geometry
            JSONObject geometryObject = new JSONObject();
            geometryObject.put("type", "Multipolygon");

            JSONArray geomArray = new JSONArray();
            polygons.stream().forEach(polygon -> {
                // Array of arrays
                JSONArray polyArray = new JSONArray();
                Arrays.stream(polygon)
                        .forEach(coord -> polyArray.put(new JSONArray(new double[] { coord.x, coord.y, coord.z })));
                geomArray.put(polygons);
            });
            geometryObject.put("coordinates", geomArray);
            featureObject.put("geometry", geometryObject);
            featureArray.put(featureObject);
        }

        result.put("features", featureArray);

        // Output heat emission data in GeoJSON format for DUCT

        String outputFP = Paths.get(System.getProperty("user.dir"), "output", "data.geojson").toString();
        String resultString = result.toString();
        File file1 = new File(outputFP);
        FileWriter fw;
        try {
            fw = new FileWriter(file1);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(resultString);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Creates triples that specify the cityObjectId associated with the building
     * matched to each
     * factory.
     * 
     * @param iri
     * @param route
     */

    private void instantiateBuildingsTriples(String route, Map<String, Integer> factoryToBuildings) {
        UpdateBuilder ub = new UpdateBuilder();

        for (String key : factoryToBuildings.keySet()) {
            Integer objectId = factoryToBuildings.get(key);
            ub.addInsert(key, cityObjectId, objectId);
        }

        AccessAgentCaller.updateStore(route, ub.toString());

    }

    /**
     * Deletes all triples specifying the GmlId of a particular factory.
     * 
     * @param iri   IRI to delete
     * @param route access agent route
     */
    private void deleteBuildingsTriples(String route) {
        UpdateBuilder db = new UpdateBuilder()
                .addWhere("?s", predicate, "?o");

        db.addDeleteQuads(db.buildDeleteWhere().getQuads());
        AccessAgentCaller.updateStore(route, db.buildRequest().toString());
    }

    /**
     * Sets store client to the query and update endpoint of route
     * 
     * @param route access agent route
     */
    private void setStoreClient(String route) {
        JSONObject queryResult = AccessAgentCaller.getEndpoints(route);

        String queryEndpoint = queryResult.getString(JPSConstants.QUERY_ENDPOINT);
        String updateEndpoint = queryResult.getString(JPSConstants.UPDATE_ENDPOINT);

        if (!isDockerized()) {
            queryEndpoint = queryEndpoint.replace("host.docker.internal", "localhost");
            updateEndpoint = updateEndpoint.replace("host.docker.internal", "localhost");
        }

        storeClient = new RemoteStoreClient(queryEndpoint, updateEndpoint);
    }

    /**
     * Check if the agent is running in Docker
     * 
     * @return true if running in Docker, false otherwise
     */
    private boolean isDockerized() {
        File f = new File("/.dockerenv");
        return f.exists();
    }

    /**
     * Checks if a string is able to be parsable as a number
     * 
     * @param number string to check
     * @return boolean value of check
     */
    public boolean isNumber(String number) {
        try {
            Double.parseDouble(number);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}