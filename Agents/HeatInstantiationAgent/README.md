# Heat Instantiation Agent
This agent instantiates data required to calculate heat emissions of various types of factories as triples in a blazegraph namespace according to the [OntoCompany](https://github.com/cambridge-cares/TheWorldAvatar/tree/main/JPS_Ontology/ontology/ontocompany) ontology. 



## 1. Agent Deployment

The agent can be run within a standalone Docker container or as part of a stack. 

### 1.1 Preparation

#### Data format

The data containing the relevant properties of various factories needs to be compiled in an Excel file called 'heat.xlsx' in the 'data' directory within this folder. This file needs to contain one sheet for each type of factory. 

The following properties need to be specified for all factories. The corresponding column in the Excel file needs to have the header shown in parantheses:

- Company Name ('Company Name)
- Address of factory ('Address')
- Latitude of the address in EPSG:4326 CRS ('Latitude')
- Longitude of the address in EPSG:4326 CRS ('Longitude')
- Year of Formation ('Year of Formation')
- Business Activity ('Business Activity')
- SSIC code ('SSIC Code')

Additional properties to be specified for various types of factories are listed below.

Chemical plants:

- Specific energy consumption in units of megaJoules per kilogram ('Specific Energy Consumption (MJ/kg)')
- Production volume in units of tons per year ('Production Volume (tons/year)')
- Thermal efficiency ('Thermal efficiency') 

#### Blazegraph endpoint

There needs to be a blazegraph namespace where the agent can instantiate the data in the Excel file as triples. This namespace should be created before running the agent.

#### Stack containers

If the agent is being run as part of a stack, the user can opt to use a namespace located in the stack blazegraph. The procedure for spinning up the stack is described at [stack manager page](https://github.com/cambridge-cares/TheWorldAvatar/tree/main/Deploy/stacks/dynamic/stack-manager).


### 1.2 Docker deployment

From the same directory location as this README, build the agent's image using the `docker compose build` command. If the container is run as part of a stack, copy the `heat-instantiation.json` file from the `stack-manager-input-config` folder into the `Deploy/stacks/dynamic/stack-manager/inputs/config/services` folder of the stack manager before starting the stack.


## 2. Agent Route

The agent has a single API route which requires a GET request. It accepts the following input parameters:

- ```namespace```: The name of the Blazegraph namespace from which the companies' properties are queried. This parameter should be included for cases in which this data is stored in a namespace in the stack blazegraph.
-```endpoint```: The full endpoint of the Blazegraph namespace from which the companies' properties are queried. This parameter should be used if this data is stored in a blzegraph namespace outside the stack.

Assuming that the blazegraph endpoint is called "http://localhost:48889/blazegraph/namespace/sgbusinessunits/sparql/", the agent can be run using the following example request:

```
curl -X GET "localhost:3838/heat-instantiation/getHeatData?namespace=sgbusinessunits"
```
Upon successful completion, the agent returns the JSON obect '{"success":"true"}' and the HTTP status 200.



