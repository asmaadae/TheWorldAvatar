import pandas as pd
import numpy as np
from flask import Blueprint, request, jsonify, Response
import requests
from py4jps import agentlogging
import json
import uuid
from kgclient import KGClient
import os


ROUTE = "/getHeatData"

get_heat_data_bp = Blueprint('get_heat_data_bp', __name__)
logger = agentlogging.get_logger("dev")


@get_heat_data_bp.route(ROUTE, methods=['GET'])
def api():
    logger.info("Received request to read and instantiate heat data")
    data_file = request.args["dataFile"]
    triples = get_heat_chemicals(data_file)
    update_endpoint = request.args["endPoint"]
    kgclient_heat = KGClient(update_endpoint, update_endpoint)
    return instantiate_data(triples, kgclient_heat)


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


def get_heat_chemicals(data_file: str) -> pd.DataFrame:
    """ 
    Read chemical plants data from the provided excel file 
    and populate a dataframe with the required triples.

    """

    plants_data = pd.read_excel(
        data_file, sheet_name="Chemicals", usecols="A:P")

    triples = pd.DataFrame(columns=['subject', 'predicate', 'object'])

    # Classes and Concepts for each company
    company = "http://www.theworldavatar.com/kg/ontocompany/Company_"
    company_class = get_class(company)
    chemical_plant = "http://www.theworldavatar.com/kg/ontochemplant/ChemicalPlant_"
    chemical_plant_class = get_class(chemical_plant)
    chemical_industry = "http://www.theworldavatar.com/kg/ontocompany/ChemicalIndustry_"
    chemical_industry_class = get_class(chemical_industry)
    measure = "http://www.ontology-of-units-of-measure.org/resource/om-2/Measure_"
    measure_class = get_class(measure)
    design_capacity = "http://www.theworldavatar.com/kg/ontocompany/DesignCapacity_"
    design_capacity_class = get_class(design_capacity)
    specific_energy = "http://www.theworldavatar.com/kg/ontocompany/SpecificEnergy_"
    specific_energy_class = get_class(specific_energy)

    address = "http://ontology.eil.utoronto.ca/icontact.owl#Address_"
    address_class = get_class(address)
    has_address = "<http://ontology.eil.utoronto.ca/icontact.owl#hasAddress>"
    has_latitude = "<http://www.theworldavatar.com/kg/ontocompany/hasLatitudeEPSG4326>"
    has_longitude = "<http://www.theworldavatar.com/kg/ontocompany/hasLongitudeEPSG4326>"
    is_owner_of = "<http://www.theworldavatar.com/kg/ontocompany/isOwnerOf>"
    has_ssic_code = "<http://www.theworldavatar.com/kg/ontocompany/hasSSICCode>"
    has_business_activity = "<http://www.theworldavatar.com/kg/ontocompany/hasBusinessActivity>"
    belongs_to_industry = "<http://www.theworldavatar.com/kg/ontocompany/belongsToIndustry>"
    rdf_type = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"
    rdfs_label = "<http://www.w3.org/2000/01/rdf-schema#label>"
    has_year_formation = "<http://www.theworldavatar.com/kg/ontocompany/hasYearOfEstablishment>"
    has_postal_code = "<http://ontology.eil.utoronto.ca/icontact.owl#hasPostalCode>"
    has_specific_energy = "<http://www.theworldavatar.com/kg/ontocompany/hasSpecificEnergyConsumption>"
    has_design_capacity = "<http://www.theworldavatar.com/kg/ontocompany/hasDesignCapacity>"
    specific_energy_unit = "<http://www.ontology-of-units-of-measure.org/resource/om-2/MegaJoulesPerKilogram>"
    design_capacity_unit = "<http://www.ontology-of-units-of-measure.org/resource/om-2/TonsPerYear>"
    has_value = "<http://www.ontology-of-units-of-measure.org/resource/om-2/hasValue>"
    has_numerical_value = "<http://www.ontology-of-units-of-measure.org/resource/om-2/hasNumericalValue>"
    has_unit = "<http://www.ontology-of-units-of-measure.org/resource/om-2/hasUnit>"

    for ind, row in plants_data.iterrows():

        company_name = row["Company Name"]
        company_name_str = get_string(company_name)
        year_formation = get_year(row["Year of Formation"])
        company_iri = get_iri(company)
        chemical_plant_iri = get_iri(chemical_plant)
        chemical_plant_name = get_string(
            "Chemical_Plant_of_" + row["Company Name"])
        address_iri = get_iri(address)
        measure_iri_1 = get_iri(measure)
        measure_iri_2 = get_iri(measure)
        measure_iri_1_name = get_string(
            "Measure_Specific_Energy_" + company_name)
        measure_iri_2_name = get_string(
            "Measure_Design_Capacity_" + company_name)
        specific_energy_iri = get_iri(specific_energy)
        design_capacity_iri = get_iri(design_capacity)
        specific_energy_name = get_string(
            "Specific_Energy_of_" + company_name)
        design_capacity_name = get_string(
            "Design_Capacity_of_" + company_name)
        specific_energy_value = get_double(
            row["Specific energy consumption (MJ/kg)"])
        design_capacity_value = get_double(
            row["Production Volume (tons/year)"])
        ssic_code = get_integer(row["SSIC Code"])
        business_activity = get_string(row["Business Activity"])
        address_name = get_string("Address_of_" + company_name)
        latitude = get_double(row["Latitude"])
        longitude = get_double(row["Longitude"])
        postal_code = get_integer(row["Postal Code"])

        company_data = pd.DataFrame(
            {"subject": [company_iri, company_iri, company_iri, company_iri, company_iri, company_iri],
             "predicate": [rdf_type, rdfs_label, is_owner_of, has_year_formation, has_ssic_code, has_business_activity],
             "object": [company_class, company_name_str, chemical_plant_iri, year_formation, ssic_code, business_activity]
             }
        )

        chemical_plant_data = pd.DataFrame(
            {"subject": [chemical_plant_iri, chemical_plant_iri, chemical_plant_iri, chemical_plant_iri, chemical_plant_iri, chemical_plant_iri],
             "predicate": [rdf_type, rdfs_label, has_address, has_specific_energy, has_design_capacity, belongs_to_industry],
             "object": [chemical_plant_class, chemical_plant_name, address_iri, specific_energy_iri, design_capacity_iri, chemical_industry_class]
             }
        )

        specific_energy_data = pd.DataFrame(
            {"subject": [specific_energy_iri, specific_energy_iri, specific_energy_iri, measure_iri_1, measure_iri_1, measure_iri_1, measure_iri_1],
             "predicate": [has_value, rdf_type, rdfs_label, rdf_type, rdfs_label, has_numerical_value, has_unit],
             "object": [measure_iri_1, specific_energy_class, specific_energy_name, measure_class, measure_iri_1_name, specific_energy_value, specific_energy_unit]
             }
        )

        design_capacity_data = pd.DataFrame(
            {"subject": [design_capacity_iri, design_capacity_iri, design_capacity_iri, measure_iri_2, measure_iri_2, measure_iri_2, measure_iri_2],
             "predicate": [has_value, rdf_type, rdfs_label, rdf_type, rdfs_label, has_numerical_value, has_unit],
             "object": [measure_iri_2, design_capacity_class, design_capacity_name, measure_class, measure_iri_2_name, design_capacity_value, design_capacity_unit]
             }
        )

        address_data = pd.DataFrame(
            {"subject": [address_iri, address_iri, address_iri, address_iri, address_iri],
             "predicate": [rdf_type, rdfs_label, has_latitude, has_longitude, has_postal_code],
             "object": [address_class, address_name, latitude, longitude, postal_code]
             }
        )

        triples = pd.concat([triples, company_data, chemical_plant_data,
                            specific_energy_data, design_capacity_data, address_data])

    return triples


if __name__ == '__main__':
    dirname = os.path.dirname(__file__)
    data_file = os.path.join(dirname, '../data/heat.xlsx')
    df = get_heat_chemicals(data_file)
    update_endpoint = "http://localhost:8080/blazegraph/namespace/sgbusinessunits/sparql/"
    kgclient_heat = KGClient(update_endpoint, update_endpoint)
    instantiate_data(df, kgclient_heat)
