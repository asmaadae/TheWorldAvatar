package uk.ac.cam.cares.jps.agent.cea.utils.geometry;

import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.jps.agent.cea.utils.uri.BuildingURIHelper;
import uk.ac.cam.cares.jps.agent.cea.utils.uri.OntologyURIHelper;

import org.apache.jena.query.Query;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;

public class GeometryQueryHelper {
    public static final String SURFACE_GEOMETRY = "surfacegeometry";
    public static final String THEMATIC_SURFACE = "thematicsurface";
    public static final String DATABASE_SRS = "databasesrs";
    public static final String CITY_OBJECT_GEN_ATT = "cityobjectgenericattrib";
    
    private OntologyURIHelper ontologyUriHelper;

    /**
     * Constructs a GeometryQueryHelper object with the given OntologyURIHelper
     * @param uriHelper OntologyURIHelper
     */
    public GeometryQueryHelper(OntologyURIHelper uriHelper) {
        this.ontologyUriHelper = uriHelper;
    }

    /**
     * Queries for building geometry related information
     * @param uriString city object id
     * @param route route to pass to access agent
     * @param type type of building geometry related information to be queried
     * @return building geometry related information
     */
    public String getBuildingGeometry(String uriString, String route, String type) {
        String result;

        switch(type) {
            case "height":
                // Set default value of 10m if height can not be obtained from knowledge graph
                // Will only require one height query if height is represented in data consistently
                result = getValue(uriString, "HeightMeasuredHeigh", route);
                result = result.length() == 0 ? getValue(uriString, "HeightMeasuredHeight", route) : result;
                result = result.length() == 0 ? getValue(uriString, "HeightGenAttr", route) : result;
                result = result.length() == 0 ? "10.0" : result;
                break;

            case "footprint":
                // Get footprint from ground thematic surface or find from surface geometries depending on data
                result = getValue(uriString, "Lod0FootprintId", route);
                result = result.length() == 0 ? getValue(uriString, "FootprintThematicSurface", route) : result;
                result = result.length() == 0 ? getValue(uriString, "FootprintSurfaceGeom", route) : result;
                break;

            case "crs":
                result = getValue(uriString, "CRS", route);
                result = result.isEmpty() ? getValue(uriString, "DatabasesrsCRS", route) : result;
                break;

            default:
                result = "";
                break;
        }

        return result;
    }

    /**
     * Executes query on SPARQL endpoint and retrieves requested value of building
     * @param uriString city object id
     * @param value building value requested
     * @param route route to pass to access agent
     * @return geometry as string
     */
    public String getValue(String uriString, String value, String route)  {

        String result = "";

        Query q = getQuery(uriString, value);

        //Use access agent
        JSONArray queryResultArray = AccessAgentCaller.queryStore(route, q.toString());

        if(!queryResultArray.isEmpty()){
            if (value.equals("Lod0FootprintId") || value.equals("FootprintThematicSurface")) {
                result = GeometryHandler.extractFootprint(queryResultArray);
            }
            else if (value.equals("FootprintSurfaceGeom")) {
                result = GeometryHandler.extractFootprint(GeometryHandler.getGroundGeometry(queryResultArray));
            }
            else{
                result = queryResultArray.getJSONObject(0).get(value).toString();
            }
        }

        return result;
    }

    /**
     * Calls a SPARQL query for a specific URI for height or geometry.
     * @param uriString city object id
     * @param value building value requested
     * @return returns a query string
     */
    private Query getQuery(String uriString, String value) {
        switch(value) {
            case "Lod0FootprintId":
                return getLod0FootprintIdQuery(uriString);
            case "FootprintSurfaceGeom":
                return getGeometryQuerySurfaceGeom(uriString);
            case "FootprintThematicSurface":
                return getGeometryQueryThematicSurface(uriString);
            case "HeightMeasuredHeigh":
                return getHeightQueryMeasuredHeigh(uriString);
            case "HeightMeasuredHeight":
                return getHeightQueryMeasuredHeight(uriString);
            case "HeightGenAttr":
                return getHeightQueryGenAttr(uriString);
            case "DatabasesrsCRS":
                return getDatabasesrsCrsQuery(uriString);
            case "CRS":
                return getCrsQuery(uriString);
            case "envelope":
                return getEnvelopeQuery(uriString);
        }
        return null;
    }

    /**
     * Builds a SPARQL query for a specific URI to retrieve the building height for data with ocgml:measuredHeigh attribute
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQueryMeasuredHeigh(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                    .addWhere("?s", "ocgml:measuredHeigh", "?HeightMeasuredHeigh")
                    .addFilter("!isBlank(?HeightMeasuredHeigh)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?HeightMeasuredHeigh")
                    .addGraph(NodeFactory.createURI(BuildingURIHelper.getBuildingGraph(uriString)), wb);
            sb.setVar(Var.alloc("s"), NodeFactory.createURI(BuildingURIHelper.getBuildingUri(uriString)));
            return sb.build();
        }catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds a SPARQL query for a specific URI to retrieve the building height for data with ocgml:measuredHeight attribute
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQueryMeasuredHeight(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                    .addWhere("?s", "ocgml:measuredHeight", "?HeightMeasuredHeight")
                    .addFilter("!isBlank(?HeightMeasuredHeight)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?HeightMeasuredHeight")
                    .addGraph(NodeFactory.createURI(BuildingURIHelper.getBuildingGraph(uriString)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(BuildingURIHelper.getBuildingUri(uriString)));
            return sb.build();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds a SPARQL query for a specific URI to retrieve the building height for data with generic attribute with ocgml:attrName 'height'
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQueryGenAttr(String uriString) {
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                .addWhere("?o", "ocgml:attrName", "height")
                .addWhere("?o", "ocgml:realVal", "?HeightGenAttr")
                .addWhere("?o", "ocgml:cityObjectId", "?s");
        sb.addVar("?HeightGenAttr")
                .addGraph(NodeFactory.createURI(BuildingURIHelper.getGraph(uriString, CITY_OBJECT_GEN_ATT)), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        return sb.build();
    }

    /**
     * Builds a SPARQL query for a specific URI to retrieve ground surface geometry from lod0FootprintId
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getLod0FootprintIdQuery(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                    .addWhere("?building", "ocgml:lod0FootprintId", "?footprintSurface")
                    .addWhere("?surface", "ocgml:parentId", "?footprintSurface")
                    .addWhere("?surface", "ocgml:GeometryType", "?geometry");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?geometry")
                    .addVar("datatype(?geometry)", "?datatype")
                    .addWhere(wb);
            sb.setVar(Var.alloc("building"), NodeFactory.createURI(BuildingURIHelper.getBuildingUri(uriString)));
            return sb.build();
        }catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds a SPARQL query for a specific URI to retrieve ground surface geometries for building linked to thematic surfaces with ocgml:objectClassId 35
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getGeometryQueryThematicSurface(String uriString) {
        try {
            WhereBuilder wb1 = new WhereBuilder()
                    .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                    .addWhere("?surf", "ocgml:cityObjectId", "?s")
                    .addWhere("?surf", "ocgml:GeometryType", "?geometry")
                    .addFilter("!isBlank(?geometry)");
            WhereBuilder wb2 = new WhereBuilder()
                    .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                    .addWhere("?s", "ocgml:buildingId", "?building")
                    .addWhere("?s", "ocgml:objectClassId", "?groundSurfId")
                    .addFilter("?groundSurfId = 35"); //Thematic Surface Ids are 33:roof, 34:wall and 35:ground
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?geometry")
                    .addVar("datatype(?geometry)", "?datatype")
                    .addGraph(NodeFactory.createURI(BuildingURIHelper.getGraph(uriString, SURFACE_GEOMETRY)), wb1)
                    .addGraph(NodeFactory.createURI(BuildingURIHelper.getGraph(uriString, THEMATIC_SURFACE)), wb2);
            sb.setVar( Var.alloc( "building" ), NodeFactory.createURI(BuildingURIHelper.getBuildingUri(uriString)));
            return sb.build();

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Builds a SPARQL query for a specific URI to retrieve all surface geometries to a building
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getGeometryQuerySurfaceGeom(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                    .addWhere("?surf", "ocgml:cityObjectId", "?s")
                    .addWhere("?surf", "ocgml:GeometryType", "?geometry")
                    .addFilter("!isBlank(?geometry)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?geometry")
                    .addVar("datatype(?geometry)", "?datatype")
                    .addGraph(NodeFactory.createURI(BuildingURIHelper.getGraph(uriString, SURFACE_GEOMETRY)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(BuildingURIHelper.getBuildingUri(uriString)));
            return sb.build();

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds a SPARQL query for a CRS not in the DatabaseSRS graph using namespace from uri
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getCrsQuery(String uriString) {
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                .addWhere("?s", "ocgml:srid", "?CRS");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?CRS")
                .addWhere(wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(BuildingURIHelper.getNamespace(uriString)));
        return sb.build();
    }

    /**
     * Builds a SPARQL query for a CRS in the DatabaseSRS graph using namespace from uri
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getDatabasesrsCrsQuery(String uriString) {
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                .addWhere("?s", "ocgml:srid", "?CRS");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?CRS")
                .addGraph(NodeFactory.createURI(BuildingURIHelper.getGraph(uriString, DATABASE_SRS)), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(BuildingURIHelper.getNamespace(uriString)));
        return sb.build();
    }

    /**
     * Builds a SPARQL query for the envelope of uriString
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getEnvelopeQuery(String uriString) {
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ontologyUriHelper.getOntologyUri(OntologyURIHelper.ocgml))
                .addWhere("?s", "ocgml:EnvelopeType", "?envelope");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?envelope")
                .addWhere(wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));
        return sb.build();
    }
}