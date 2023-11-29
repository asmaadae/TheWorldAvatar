package uk.ac.cam.cares.jps.agent.buildingidentification;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public class BuildingIdentificationAgentTest {

    @Test
    public void testAgent() {
        JSONObject request = new JSONObject();
        request.put("maxDistance", "1.0");
        request.put("route", System.getenv("route"));
        request.put("requestUrl", "/run");
        request.put("dbUrl", System.getenv("dbUrl"));
        request.put("dbUser", System.getenv("dbUser"));
        request.put("dbPassword", System.getenv("dbPassword"));

        JSONObject result = new BuildingIdentificationAgent().processRequestParameters(request);
        assertTrue(result.getInt("number_factories") > 0);
        assertTrue(result.getInt("number_buildings") > 0);

    }

}