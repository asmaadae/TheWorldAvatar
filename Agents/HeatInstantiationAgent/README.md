# Heat Instantiation Agent
This agent instantiates data required to calculate heat emissions of various types of factories as triples in a blazegraph namespace according to the OntoCompany ontology (https://github.com/cambridge-cares/TheWorldAvatar/tree/main/JPS_Ontology/ontology/ontocompany). 

The data needs to be in an Excel file called 'heat.xlsx' in the 'data' directory within this folder. This file needs to contain one sheet for each type of factory.

# Running the agent

1. Execute the 'docker-compose build' command in this folder at the same absolute path as this README.md.

2. Place the 'stack-manager-input-config/heat-instantiation.json' file in the 'Deploy/stacks/dynamic/stack-manager/inputs/config/services' folder.

3. Spin up the stack following the instructions at https://github.com/cambridge-cares/TheWorldAvatar/tree/main/Deploy/stacks/dynamic/stack-manager. 

4. Create a namespace in the stack blazegraph. 

5. Assuming the namespace created in step 4 is called 'sgbusinessunits', send a POST reuest to the agent as follows:

```
curl -X GET "localhost:3838/heat-instantiation/getHeatData?namespace=sgbusinessunits"
```

Upon successful completion, the agent returns a JSONObject {'success': True} together with the HTTP status 200.

# Properties instantiated for various types of factories

## Generic properties

- Company Name
- Address of factory
- Latitude of the address in EPSG:4326 CRS
- Longitude of the address in EPSG:4326 CRS
- Year of Formation
- Business Activity 
- SSIC code

## Chemical Plants

- Specific energy consumption
- Production volume
- Thermal efficiency