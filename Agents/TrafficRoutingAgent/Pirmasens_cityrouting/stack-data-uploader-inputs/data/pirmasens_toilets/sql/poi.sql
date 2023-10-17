CREATE TABLE poi AS
SELECT
  p.amenity,
  p.operator,
  p.male,
  p.female,
  p.access,
  p.fee,
  p.wheelchair,
  ST_Centroid(p."geometryProperty") as "geometryProperty",
  (
    SELECT id
    FROM routing_ways_vertices_pgr AS r
    ORDER BY ST_Distance(r.the_geom, ST_Centroid(p."geometryProperty"))
    LIMIT 1
  ) as nearest_node_id
FROM points AS p
WHERE p.amenity = 'toilets' OR p.toilets_position IS NOT NULL OR p.toilets_handwashing IS NOT NULL
UNION
SELECT
  poly.amenity,
  poly.operator,
  poly.male,
  poly.female,
  poly.access,
  poly.fee,
  poly.wheelchair,
  ST_Centroid(poly."geometryProperty") as "geometryProperty",
  (
    SELECT id
    FROM routing_ways_vertices_pgr AS r
    ORDER BY ST_Distance(r.the_geom, ST_Centroid(poly."geometryProperty"))
    LIMIT 1
  ) as nearest_node_id
FROM polygons as poly
WHERE poly.amenity = 'toilets';