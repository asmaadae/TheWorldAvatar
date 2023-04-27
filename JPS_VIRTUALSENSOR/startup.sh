#!/bin/bash
# pull docker images from docker.cmclinnovations.com because stack manager can't pull these
docker pull docker.cmclinnovations.com/dispersion-interactor:1.0.0
docker pull docker.cmclinnovations.com/aermod-agent:1.0.1
docker pull docker.cmclinnovations.com/python-service:1.0.1
docker pull docker.cmclinnovations.com/emissions-agent:1.0.0
docker pull docker.cmclinnovations.com/ship-input-agent:1.0.0
docker pull docker.cmclinnovations.com/weatheragent:1.1.1
docker pull docker.cmclinnovations.com/file-server:1.0.0
# starts up all required components of virtual sensor
(cd stack-manager && ./stack.sh start ship-stack)
# copy required files into containers
(cd DispersionVis && ./copy_vis_file.sh)
(cd ShipInputAgent && ./copy_ship_file.sh)
