package uk.ac.cam.cares.jps.agent.buildingidentification;

import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;

import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.RemoteRDBStoreClient;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import uk.ac.cam.cares.jps.agent.buildingidentification.objects.Factory;

@WebServlet(urlPatterns = { BuildingIdentificationAgent.URI_RUN })
public class BuildingIdentificationAgent extends JPSAgent {

    public static final String KEY_REQ_URL = "requestUrl";
    public static final String URI_RUN = "/run";
    public static final String KEY_DISTANCE = "maxDistance";

    private static final Logger LOGGER = LogManager.getLogger(BuildingIdentificationAgent.class);

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {

            String ontology = requestParams.getString("ontology");

            if (ontology.equalsIgnoreCase("ontocompany"))
                return new OntoCompany().processRequestParameters(requestParams);
            else
                return new OntoChemPlant().processRequestParameters(requestParams);

        } else
            throw new JPSRuntimeException("Invalid input.");
    }

    /**
     * Checks validity of incoming request
     * 
     * @param requestParams Request parameters as JSONObject
     * @return Validity of request
     */
    @Override
    public boolean validateInput(JSONObject requestParams) {
        return isNumber(requestParams.getString(KEY_DISTANCE));
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