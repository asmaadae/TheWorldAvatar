SELECT
  amenity,
  operator,
  male,
  female,
  access,
  fee,
  wheelchair,
  ST_Centroid("geometryProperty") as "geometryProperty"
FROM points
WHERE amenity = 'toilets' OR toilets_position IS NOT NULL OR toilets_handwashing IS NOT NULL
UNION
SELECT
  amenity,
  operator,
  male,
  female,
  access,
  fee,
  wheelchair,
  ST_Centroid("geometryProperty") as "geometryProperty"
FROM polygons
WHERE amenity = 'toilets'
