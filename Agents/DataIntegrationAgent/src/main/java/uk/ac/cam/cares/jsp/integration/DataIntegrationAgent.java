package uk.ac.cam.cares.jsp.integration;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/sql", "/status"})
public class DataIntegrationAgent extends JPSAgent {
    private static final Logger LOGGER = LogManager.getLogger(DataIntegrationAgent.class);
    // Agent starts off in valid state, and will be invalid when running into exceptions
    private static boolean VALID = true;
    private static boolean AGENT_IN_STACK = false;
    private static final String INVALID_PARAMETER_ERROR_MSG = "Parameters are invalid, please check logs for more details.";
    private static final String INVALID_ROUTE_ERROR_MSG = "Invalid request type! Route ";
    private static final String KEY_DATABASE = "database";
    private static final String KEY_SOURCE_DATABASE = "srcDbName";
    private static final String DATABASE_2D = "2d";
    private static final String DATABASE_3D = "3d";
    private static final String TABLE_2D = "table2d";
    private static final String KEY_VALUES = "values";
    @Override
    public synchronized void init() {
        try {
            super.init();
            // Ensure logging are properly working
            LOGGER.debug("This is a test DEBUG message");
            LOGGER.info("This is a test INFO message");
            LOGGER.warn("This is a test WARN message");
            LOGGER.error("This is a test ERROR message");
            LOGGER.fatal("This is a test FATAL message");
        } catch (Exception exception) {
            DataIntegrationAgent.VALID = false;
            LOGGER.error("Could not initialise an agent instance!", exception);
        }
    }

    /**
     * An overloaded method to process all the different HTTP (GET/POST/PULL..) requests.
     * Do note all requests to JPS agents are processed similarly and will only return response objects.
     *
     * @return A response to the request called as a JSON Object.
     */
    // @Override
    // public JSONObject processRequestParameters(JSONObject requestParams, HttpServletRequest request) {
    //     return processRequestParameters(requestParams);
    // }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JSONObject jsonMessage = new JSONObject();
		processRequestParameters(jsonMessage);

	}
    /**
     * A method that process all the different HTTP (GET/POST/PULL..) requests.
     * This will validate the incoming request type and parameters against their route options.
     *
     * @return A response to the request called as a JSON Object.
     */
    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject jsonMessage = new JSONObject();
        // Validate input and if it is false, do not continue with the task
        if (!validateInput(requestParams)) {
            jsonMessage.put("Result", INVALID_PARAMETER_ERROR_MSG);
            return jsonMessage;
        }
        // Retrieve the request type and route
        String requestType = requestParams.get("method").toString();
        String route = requestParams.get("requestUrl").toString();
        // Retrieve the route name
        route = route.substring(route.lastIndexOf("/") + 1);
        LOGGER.info("Passing request to the Data Integration Agent...");
        // Run logic based on request path
        switch (route) {
            case "sql":
                if (requestType.equals("GET")) {
                    String[] config = requestParams.has(KEY_SOURCE_DATABASE) ? Config.retrieveSQLConfig(requestParams.get(KEY_SOURCE_DATABASE).toString(), true) :
                                    requestParams.has(DATABASE_2D) ? Config.retrieveSQLConfig(requestParams.get(DATABASE_2D).toString(), false) :
                                    requestParams.has(DATABASE_3D) ? Config.retrieveSQLConfig(requestParams.get(DATABASE_3D).toString(), false) :
                                    requestParams.has(TABLE_2D) ? Config.retrieveSQLConfig(requestParams.get(TABLE_2D).toString(), false) :
                                    Config.retrieveSQLConfig();
                    AGENT_IN_STACK = requestParams.has(KEY_SOURCE_DATABASE) ;
                    jsonMessage = sqlRoute(config);
                } else {
                    LOGGER.fatal(INVALID_ROUTE_ERROR_MSG + route + " can only accept GET request.");
                    jsonMessage.put("Result", INVALID_ROUTE_ERROR_MSG + route + " can only accept GET request.");
                }
                break;
            case "status":
                if (requestType.equals("GET")) {
                    jsonMessage = statusRoute();
                } else {
                    LOGGER.fatal(INVALID_ROUTE_ERROR_MSG + route + " can only accept GET request.");
                    jsonMessage.put("Result", INVALID_ROUTE_ERROR_MSG + route + " can only accept GET request.");
                }
                break;
        }
        return jsonMessage;
    }

    /**
     * Validates the request parameters.
     *
     * @return true or false depending on valid parameter status.
     */
    @Override
    public boolean validateInput(JSONObject requestParams) {
        boolean validate = false;
        // If request is sent to status route, there are no parameters to validate
        if (requestParams.get("requestUrl").toString().contains("status")) return true;

        // If there are parameters passed for the sql route
        if (requestParams.get("requestUrl").toString().contains("sql")) {
            if (requestParams.has(KEY_SOURCE_DATABASE) ) {
                LOGGER.fatal("Detected `srcDbName` parameters!");
                return false;
            }
            if (requestParams.has(KEY_SOURCE_DATABASE)) {
                if (!(requestParams.get(KEY_SOURCE_DATABASE) instanceof String)) {
                    LOGGER.fatal("`srcDbName` is not a string!");
                    return false;
                }
            }
            validate = true;
        }
        return validate;
    }

    /**
     * Run logic for the "/status" route that indicates the agent's current status.
     *
     * @return A response to the request called as a JSON Object.
     */
    protected JSONObject statusRoute() {
        JSONObject response = new JSONObject();
        LOGGER.info("Detected request to get agent status...");
        if (DataIntegrationAgent.VALID) {
            response.put("Result", "Agent is ready to receive requests.");
        } else {
            response.put("Result", "Agent could not be initialised!");
        }
        return response;
    }

    /**
     * Run logic for the "/sql" route to do spatial link.
     *
     * @return A response to the request called as a JSON Object.
     */
    protected JSONObject sqlRoute(String[] config) {
        LOGGER.debug("Creating the SQL connector..");
        JSONObject response = new JSONObject();
        SpatialLink spatialLink = new SpatialLink();
        spatialLink.SpatialLink(config);
//        SqlBridge connector = new SqlBridge(config);
//        LOGGER.debug("Transfer data from source to target database...");
//        JSONObject response = connector.transfer(AGENT_IN_STACK);
//        LOGGER.info("Data have been successfully transferred from " + config[0] + " to " + config[3]);
        if (response.isEmpty()) {
            response.put("Result", "Data have been successfully integrated");
        }
        return response;
    }

    public static void main(String[] args) {
        new DataIntegrationAgent();
    }
    
}
