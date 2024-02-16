# Pirmasens Digital Twin Visualisation Framework - Routing

This visualization serves as a proof of concept for routing use cases in Pirmasens

<img src="pirmasens.png" alt="Mapbox visualisation" width="100%"/>

## Uploading data via stack-data-uploader
The data structure and config file to upload the raw OSM data in stack-data-uploader is located in [stack-data-uploader-inputs](stack-data-uploader-inputs/) directory. 

### OSM Routing Data (Routable data)
1) Download desired bounding box from [BBBike.org](https://extract.bbbike.org/) (check junk email) or [GeoFabrik](https://download.geofabrik.de/) in `.pbf` format.
2) `.pbf` uploaded via stack-data-uploader in [osm2pgrouting](https://github.com/cambridge-cares/TheWorldAvatar/tree/main/Deploy/stacks/dynamic/stack-data-uploader#osm-data) data type.
3) Place the file [here](stack-data-uploader-inputs/data/pirmasens_toilets/routing/).

### OSM Raw Data (Points and Polygons)
To prepare OSM data in `.gml` format
1) Download desired bounding box from [BBBike.org](https://extract.bbbike.org/) (check junk email) or [GeoFabrik](https://download.geofabrik.de/) in `.pbf` format.
2) Convert the `.pbf` file into `.osm` format using [osmconvert](https://wiki.openstreetmap.org/wiki/Osmconvert). 
3) Import the `.osm` file  into QGIS using [QuickOSM](https://plugins.qgis.org/plugins/QuickOSM/) plugin, then export points and polygons layer as `points.gml` and `polygons.gml`.
4) Place the file in [points directory](stack-data-uploader-inputs/data/pirmasens_toilets/point/) and [polygons directory](stack-data-uploader-inputs/data/pirmasens_toilets/polygon/).


## Starting the visualisation
### Replacing the agent configuration file
On this same directory run, replace `STACK-NAME` with your stack-manager name.
```
./copy.sh start <STACK-NAME>
```

### Spinning up stack-manager
Copy all relevant [config](stack-manager-inputs/config/) and [data](stack-manager-inputs/data) files into the stack's [input](https://github.com/cambridge-cares/TheWorldAvatar/tree/main/Deploy/stacks/dynamic/stack-manager/inputs) folder. Spin up the stack-manager.

### Uploading the data
Upload data following [stack-data-uploader-inputs](stack-data-uploader-inputs) using [stack-data-uploader](https://github.com/cambridge-cares/TheWorldAvatar/tree/main/Deploy/stacks/dynamic/stack-data-uploader). Relevant files need to be placed in each of the folders.

### Running the agent
To begin running the agent, on this same directory, run the command below while replacing `STACK-NAME` with your stack-manager name.
```
./stack.sh start <STACK-NAME>
```

## Accessing the visualisation
Visualization can be seen at [http://localhost:3838/visualisation](http://localhost:3838/visualisation)

<!-- Links -->
[DTVF]: https://github.com/cambridge-cares/TheWorldAvatar/wiki/Digital-Twin-Visualisations
[example Mapbox visualisation]: https://github.com/cambridge-cares/TheWorldAvatar/tree/main/web/digital-twin-vis-framework/example-mapbox-vis
[FeatureInfoAgent]: https://github.com/cambridge-cares/TheWorldAvatar/tree/main/Agents/FeatureInfoAgent

<!-- repositories -->
[FeatureInfoAgent subdirectory]: /DTVF/FeatureInfoAgent
[FeatureInfoAgent queries]: FeatureInfoAgent/queries
[DTVF subdirectory]: /DTVF
[icons]: /DTVF/data/icons
[index.html]: index.html
[data.json]: /DTVF/data.json