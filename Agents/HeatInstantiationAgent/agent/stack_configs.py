
# The purpose of this module is to retrieve the stack blazegraph endpoint for a specified namespace
# Adapted from Agents/FloodWarningAgent/agent/utils/stack_configs.py.


from agent.stack_gateway import stackClientsGw

# Initialise logger
from py4jps import agentlogging
logger = agentlogging.get_logger("prod")


def retrieve_stack_configs(NAMESPACE):
    """
        Reads settings from Stack clients as global variables,
        i.e. only global within this sub-module
    """

    # Create module views to relevant Stack clients
    stackClientsView = stackClientsGw.createModuleView()
    stackClientsGw.importPackages(
        stackClientsView, "com.cmclinnovations.stack.clients.docker.ContainerClient")
    stackClientsGw.importPackages(
        stackClientsView, "com.cmclinnovations.stack.clients.blazegraph.BlazegraphEndpointConfig")
    stackClientsGw.importPackages(
        stackClientsView, "com.cmclinnovations.stack.clients.postgis.PostGISEndpointConfig")
    stackClientsGw.importPackages(
        stackClientsView, "com.cmclinnovations.stack.clients.ontop.OntopEndpointConfig")

    # Retrieve endpoint configurations from Stack clients
    containerClient = stackClientsView.ContainerClient()
    # Blazegraph
    bg = stackClientsView.BlazegraphEndpointConfig("", "", "", "", "")
    bg_conf = containerClient.readEndpointConfig("blazegraph", bg.getClass())
    # PostgreSQL/PostGIS
    pg = stackClientsView.PostGISEndpointConfig("", "", "", "", "")
    pg_conf = containerClient.readEndpointConfig("postgis", pg.getClass())
    # Ontop
    ont = stackClientsView.OntopEndpointConfig("", "", "", "", "")
    ont_conf = containerClient.readEndpointConfig("ontop", ont.getClass())

    # Extract SPARQL endpoints of KG
    # (i.e. Query and Update endpoints are equivalent for Blazegraph)
    QUERY_ENDPOINT = bg_conf.getUrl(NAMESPACE)

    return QUERY_ENDPOINT
