package uk.ac.cam.cares.jps.agent.useragent;

import com.cmclinnovations.stack.clients.blazegraph.BlazegraphEndpointConfig;
import com.cmclinnovations.stack.clients.docker.ContainerClient;
import com.cmclinnovations.stack.clients.ontop.OntopEndpointConfig;
import com.cmclinnovations.stack.clients.postgis.PostGISEndpointConfig;

public class EndpointConfig {
	private String kgurl;
	private String kguser;
	private String kgpassword;

    private String ontopurl;

    private String dbuser;
    private String dbpassword;
    private PostGISEndpointConfig postGISEndpointConfig;
    
    public EndpointConfig() {
        ContainerClient containerClient = new ContainerClient();
        BlazegraphEndpointConfig blazegraphEndpointConfig = containerClient.readEndpointConfig("blazegraph",
                    BlazegraphEndpointConfig.class);
        this.kgurl = blazegraphEndpointConfig.getUrl("kb");
        this.kguser = blazegraphEndpointConfig.getUsername();
        this.kgpassword = blazegraphEndpointConfig.getPassword();

        OntopEndpointConfig ontopEndpointConfig = containerClient.readEndpointConfig("ontop", OntopEndpointConfig.class);
        this.ontopurl = ontopEndpointConfig.getUrl();

        postGISEndpointConfig = containerClient.readEndpointConfig("postgis",
                    PostGISEndpointConfig.class);
        this.dbuser = postGISEndpointConfig.getUsername();
        this.dbpassword = postGISEndpointConfig.getPassword();
    }
    public String getKgurl() {
        return this.kgurl;
    }
    public String getOntopurl() { return this.ontopurl; }
    public String getDburl() {
        return postGISEndpointConfig.getJdbcURL("postgres");
    }
    public String getDburl(String database) {
        return postGISEndpointConfig.getJdbcURL(database);
    }
    public String getDbuser() {
        return this.dbuser;
    }
    public String getDbpassword() {
        return this.dbpassword;
    }
}
