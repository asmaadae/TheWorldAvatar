import pandas as pd
import numpy as np
from flask import Blueprint, request, jsonify, Response
import requests
from py4jps import agentlogging
import json
import uuid
from agent.kgclient import KGClient
from agent.stack_configs import retrieve_stack_configs
import os


ROUTE = "/getHeatData"

get_heat_data_bp = Blueprint('get_heat_data_bp', __name__)
logger = agentlogging.get_logger("dev")


rdf_type = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"
rdfs_label = "<http://www.w3.org/2000/01/rdf-schema#label>"
has_value = "<http://www.ontology-of-units-of-measure.org/resource/om-2/hasValue>"
has_numerical_value = "<http://www.ontology-of-units-of-measure.org/resource/om-2/hasNumericalValue>"
has_unit = "<http://www.ontology-of-units-of-measure.org/resource/om-2/hasUnit>"


@get_heat_data_bp.route(ROUTE, methods=['GET'])
def api():
    logger.info("Received request to read and instantiate heat data")
    dirname = os.path.dirname(__file__)
    data_file = os.path.join(dirname, '../data/heat.xlsx')
    triples = get_heat_chemicals(data_file)
    if "endpoint" in request.args:
        update_endpoint = request.args["endpoint"]
    elif "namespace" in request.args:
        update_endpoint = retrieve_stack_configs(request.args["namespace"])
    else:
        logger.error("Neither endpoint nor namespace has been specified.")
        return "Either the endpoint or namespace parameter must be specified", 400

    kgclient_heat = KGClient(update_endpoint, update_endpoint)
    instantiate_data(triples, kgclient_heat)
    return json.dumps({'success': True}), 200, {'ContentType': 'application/json'}


def get_class(iri: str) -> str:
    return "<" + iri.replace("_", ">")


def get_iri(base: str) -> str:
    return "<" + base + str(uuid.uuid4()) + ">"


def get_string(base: str) -> str:
    return "\"" + base + "\"^^xsd:string"


def get_double(base: float) -> str:
    return "\"" + str(base) + "\"^^xsd:double"


def get_integer(base: int) -> str:
    return "\"" + str(base) + "\"^^xsd:integer"


def get_year(base: int) -> str:
    return "\"" + str(base) + "\"^^xsd:gYear"


def instantiate_data(triples: pd.DataFrame, kgclient: KGClient) -> None:
    """
    Instantiates all triples in the specified endpoint
    """
    triples["object"] = triples["object"].apply(lambda x: x + ".")
    data_string = triples.to_string(index=False, header=False)
    update_string = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" + os.linesep + \
        "INSERT DATA {  " + os.linesep + \
        data_string + os.linesep + "} "
    kgclient.performUpdate(update_string)


def get_company(company_name_str: str, year_formation_num: int,
                ssic_code_num: int, business_activity_str: str) -> pd.DataFrame:
    """ 
    Returns a DataFrame containing the triples related to the company's 
    year of formation, SSIC code and business activity. 
    """
    company = "http://www.theworldavatar.com/kg/ontocompany/Company_"
    company_class = get_class(company)
    company_name = get_string(company_name_str)
    company_iri = get_iri(company)
    has_ssic_code = "<http://www.theworldavatar.com/kg/ontocompany/hasSSICCode>"
    has_business_activity = "<http://www.theworldavatar.com/kg/ontocompany/hasBusinessActivity>"
    has_year_formation = "<http://www.theworldavatar.com/kg/ontocompany/hasYearOfEstablishment>"
    ssic_code = get_integer(ssic_code_num)
    business_activity = get_string(business_activity_str)
    year_formation = get_year(year_formation_num)
    company_data = pd.DataFrame(
        {"subject": [company_iri, company_iri, company_iri, company_iri, company_iri],
         "predicate": [rdf_type, rdfs_label, has_year_formation, has_ssic_code, has_business_activity],
         "object": [company_class, company_name, year_formation, ssic_code, business_activity]
         }
    )
    return company_data


def get_factory(company_iri: str, company_name_str: str, factory: str, industry: str) -> pd.DataFrame:
    """
    Returns a DataFrame containing the triples for a company's factory.
    In the case of chemical plants, factory and industry are specified as follows:
    factory = "http://www.theworldavatar.com/kg/ontochemplant/ChemicalPlant_"
    industry = "http://www.theworldavatar.com/kg/ontocompany/ChemicalIndustry_"
    """

    is_owner_of = "<http://www.theworldavatar.com/kg/ontocompany/isOwnerOf>"

    factory_class = get_class(factory)
    industry_class = get_class(industry)
    factory_iri = get_iri(factory)
    factory_name_str_prefix = factory.split("/")[-1]
    factory_name_str = factory_name_str_prefix + company_name_str
    factory_name = get_string(factory_name_str)

    belongs_to_industry = "<http://www.theworldavatar.com/kg/ontocompany/belongsToIndustry>"

    factory_data = pd.DataFrame(
        {"subject": [company_iri, factory_iri, factory_iri, factory_iri],
            "predicate": [is_owner_of, rdf_type, rdfs_label, belongs_to_industry],
            "object": [factory_iri, factory_class, factory_name, industry_class]
         }
    )

    return factory_data


def get_specific_energy(factory_iri: str, company_name_str: str, specific_energy_num: float) -> pd.DataFrame:
    """
    Returns a DataFrame containing the triples related to a factory's specific energy consumption
    """

    has_specific_energy = "<http://www.theworldavatar.com/kg/ontocompany/hasSpecificEnergyConsumption>"
    specific_energy = "http://www.theworldavatar.com/kg/ontocompany/SpecificEnergyConsumption_"
    specific_energy_iri = get_iri(specific_energy)
    specific_energy_class = get_class(specific_energy)
    specific_energy_value = get_double(specific_energy_num)
    specific_energy_unit = "<http://www.ontology-of-units-of-measure.org/resource/om-2/MegaJoulesPerKilogram>"
    specific_energy_name_str = "Specific_Energy_of_" + company_name_str
    specific_energy_name = get_string(specific_energy_name_str)

    measure = "http://www.ontology-of-units-of-measure.org/resource/om-2/Measure_"
    measure_class = get_class(measure)
    measure_iri = get_iri(measure)
    measure_iri_name = get_string("Measure_" + specific_energy_name_str)

    specific_energy_data = pd.DataFrame(
        {"subject": [factory_iri, specific_energy_iri, specific_energy_iri, specific_energy_iri, measure_iri, measure_iri, measure_iri, measure_iri],
            "predicate": [has_specific_energy, has_value, rdf_type, rdfs_label, rdf_type, rdfs_label, has_numerical_value, has_unit],
            "object": [specific_energy_iri, measure_iri, specific_energy_class, specific_energy_name, measure_class, measure_iri_name, specific_energy_value, specific_energy_unit]
         }
    )
    return specific_energy_data


def get_design_capacity(factory_iri: str, company_name_str: str, design_capacity_num: float) -> pd.DataFrame:
    """
    Returns a DataFrame containing the triples related to a factory's design capacity.
    """

    has_design_capacity = "<http://www.theworldavatar.com/kg/ontocompany/hasDesignCapacity>"
    design_capacity = "http://www.theworldavatar.com/kg/ontocompany/DesignCapacity_"
    design_capacity_iri = get_iri(design_capacity)
    design_capacity_class = get_class(design_capacity)
    design_capacity_value = get_double(design_capacity_num)
    design_capacity_unit = "<http://www.ontology-of-units-of-measure.org/resource/om-2/TonsPerYear>"
    design_capacity_name_str = "Design_Capacity_of_" + company_name_str
    design_capacity_name = get_string(design_capacity_name_str)

    measure = "http://www.ontology-of-units-of-measure.org/resource/om-2/Measure_"
    measure_class = get_class(measure)
    measure_iri = get_iri(measure)
    measure_iri_name = get_string("Measure_" + design_capacity_name_str)

    design_capacity_data = pd.DataFrame(
        {"subject": [factory_iri, design_capacity_iri, design_capacity_iri, design_capacity_iri, measure_iri, measure_iri, measure_iri, measure_iri],
         "predicate": [has_design_capacity, has_value, rdf_type, rdfs_label, rdf_type, rdfs_label, has_numerical_value, has_unit],
         "object": [design_capacity_iri, measure_iri, design_capacity_class, design_capacity_name, measure_class, measure_iri_name, design_capacity_value, design_capacity_unit]
         }
    )

    return design_capacity_data


def get_address(factory_iri: str, company_name_str: str, latitude_num: float, longitude_num: float, postal_code_num: int) -> pd.DataFrame:
    """
    Returns a DataFrame containing the triples related to a factory's address.
    """
    address = "http://ontology.eil.utoronto.ca/icontact.owl#Address_"
    address_iri = get_iri(address)
    address_class = get_class(address)
    address_name_str = "Address_of_" + company_name_str
    address_name = get_string(address_name_str)
    has_address = "<http://ontology.eil.utoronto.ca/icontact.owl#hasAddress>"
    has_latitude = "<http://www.theworldavatar.com/kg/ontocompany/hasLatitudeEPSG4326>"
    has_longitude = "<http://www.theworldavatar.com/kg/ontocompany/hasLongitudeEPSG4326>"
    has_postal_code = "<http://ontology.eil.utoronto.ca/icontact.owl#hasPostalCode>"

    latitude = get_double(latitude_num)
    longitude = get_double(longitude_num)
    postal_code = get_integer(postal_code_num)
    address_data = pd.DataFrame(
        {"subject": [factory_iri, address_iri, address_iri, address_iri, address_iri, address_iri],
         "predicate": [has_address, rdf_type, rdfs_label, has_latitude, has_longitude, has_postal_code],
         "object": [address_iri, address_class, address_name, latitude, longitude, postal_code]
         }
    )
    return address_data


def get_thermal_efficiency(factory_iri: str, efficiency: float) -> pd.DataFrame:
    """
    Returns a Dataframe containing the triples for thermal efficiency
    """
    has_thermal_efficiency = "<http://www.theworldavatar.com/kg/ontocompany/hasThermalEfficiency>"
    efficiency_value = get_double(efficiency)
    efficiency_data = pd.DataFrame({"subject": [factory_iri], "predicate": [
                                   has_thermal_efficiency], "object": [efficiency_value]})
    return efficiency_data


def get_heat_chemicals(data_file: str) -> pd.DataFrame:
    """ 
    Read chemical plants data from the provided excel file 
    and populate a dataframe with the required triples.

    """

    plants_data = pd.read_excel(
        data_file, sheet_name="Chemicals", usecols="A:P")

    triples = pd.DataFrame(columns=['subject', 'predicate', 'object'])
    chemical_plant = "http://www.theworldavatar.com/kg/ontochemplant/ChemicalPlant_"
    chemical_industry = "http://www.theworldavatar.com/kg/ontocompany/ChemicalIndustry_"

    for ind, row in plants_data.iterrows():

        company_name = row["Company Name"]
        specific_energy_num = row["Specific energy consumption (MJ/kg)"]
        design_capacity_num = row["Production Volume (tons/year)"]
        ssic_code_num = row["SSIC Code"]
        business_activity = row["Business Activity"]
        latitude_num = row["Latitude"]
        longitude_num = row["Longitude"]
        postal_code_num = row["Postal Code"]
        year_num = row["Year of Formation"]
        thermal_efficiency = row["Thermal efficiency"]

        company_data = get_company(
            company_name, year_num, ssic_code_num, business_activity)
        company_iri = company_data.loc[0, "subject"]
        chemical_plant_data = get_factory(
            company_iri, company_name, chemical_plant, chemical_industry)
        factory_iri = chemical_plant_data.loc[1, "subject"]
        specific_energy_data = get_specific_energy(
            factory_iri, company_name, specific_energy_num)
        design_capacity_data = get_design_capacity(
            factory_iri, company_name, design_capacity_num)
        address_data = get_address(
            factory_iri, company_name, latitude_num, longitude_num, postal_code_num)
        efficiency_data = get_thermal_efficiency(
            factory_iri, thermal_efficiency)

        triples = pd.concat([triples, company_data, chemical_plant_data,
                            specific_energy_data, design_capacity_data, address_data, efficiency_data])

    return triples


if __name__ == '__main__':
    dirname = os.path.dirname(__file__)
    data_file = os.path.join(dirname, '../data/heat.xlsx')
    df = get_heat_chemicals(data_file)
    update_endpoint = "http://localhost:48889/blazegraph/namespace/sgbusinessunits/sparql/"
    # update_endpoint = "http://theworldavatar.com/blazegraph/namespace/sgbusinessunits/sparql/"
    kgclient_heat = KGClient(update_endpoint, update_endpoint)
    instantiate_data(df, kgclient_heat)
