CREATE OR REPLACE FUNCTION handle_speed_equal_zero(speed double precision)
    RETURNS double precision as
$$
BEGIN
    IF speed = 0::double precision then return 0.00001::double precision;
    else return speed;
    end if;
end;
$$
    language plpgsql;

CREATE OR REPLACE FUNCTION astar(start_id BIGINT, end_id BIGINT, v int, w double precision, heur int)
     RETURNS TABLE(
         seq INT,
         path_seq INT,
         node BIGINT,
         edge BIGINT,
         cost DOUBLE PRECISION,
         agg_cost DOUBLE PRECISION
                  ) AS $$
    BEGIN
        RETURN QUERY
            SELECT * FROM pgr_astar('SELECT gid AS id,
                         source::integer,
                         target::integer,
                         sign(cost)*(ST_LENGTH(the_geom)/1000)*('||w||'+((1-'||w||')/(least(maxspeed_forward,'||v||'))))::double precision AS cost,
                         sign(reverse_cost)*(ST_LENGTH(the_geom)/1000)*('||w||'+((1-'||w||')/(least(handle_speed_equal_zero(maxspeed_backward),'||v||'))))::double precision AS reverse_cost,
                         x1, y1, x2, y2
                         FROM ways', start_id, end_id, true, heur);
    END; $$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION calculate_route_stats(table_name text)
    RETURNS TABLE(
                     path_config text,
                     total_length DOUBLE PRECISION,
                     total_time DOUBLE PRECISION
                 ) AS $$
    BEGIN
        RETURN QUERY
            EXECUTE format('SELECT $1 as path_config, SUM(res.length) as total_length,
                           SUM(case when begin_node=source_node then res.length/max_for else res.length/max_back end) as total_time
                           FROM %I res',
                table_name) USING table_name;
    END; $$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION calculate_stats(append_to_names text)
RETURNS void AS $$
    BEGIN
        EXECUTE format('CREATE TABLE %I AS SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_length_dijkstra';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_time_v50_dijkstra';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_time_v200_dijkstra';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_w05_v50_dijkstra';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_w05_v200_dijkstra';


        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_length_heur4';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_time_v50_heur4';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_time_v200_heur4';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_w05_v50_heur4';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_w05_v200_heur4';

        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_length_heur5';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_time_v50_heur5';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_time_v200_heur5';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_w05_v50_heur5';
        EXECUTE format('INSERT INTO %I SELECT * FROM calculate_route_stats($1)',
                       append_to_names||'_Stats') USING append_to_names||'_best_w05_v200_heur5';
    END; $$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION test_routes(start_id BIGINT, end_id BIGINT, append_to_names text)
RETURNS void AS $$
    BEGIN
        EXECUTE format('CREATE TABLE %I AS SELECT id, the_geom FROM ways_vertices_pgr WHERE id=$1 OR id=$2',
            append_to_names) USING start_id, end_id;


        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 1, 0) res join ways w on res.edge=w.gid', append_to_names||'_best_length_dijkstra')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 50, 0, 0) res join ways w on res.edge=w.gid', append_to_names||'_best_time_v50_dijkstra')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 0, 0) res join ways w on res.edge=w.gid', append_to_names||'_best_time_v200_dijkstra')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 50, 0.5, 0) res join ways w on res.edge=w.gid', append_to_names||'_best_w05_v50_dijkstra')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 0.5, 0) res join ways w on res.edge=w.gid', append_to_names||'_best_w05_v200_dijkstra')
            USING start_id, end_id;


        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 1, 4) res join ways w on res.edge=w.gid', append_to_names||'_best_length_heur4')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 50, 0, 4) res join ways w on res.edge=w.gid', append_to_names||'_best_time_v50_heur4')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 0, 4) res join ways w on res.edge=w.gid', append_to_names||'_best_time_v200_heur4')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 50, 0.5, 4) res join ways w on res.edge=w.gid', append_to_names||'_best_w05_v50_heur4')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 0.5, 4) res join ways w on res.edge=w.gid', append_to_names||'_best_w05_v200_heur4')
            USING start_id, end_id;


        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 1, 5) res join ways w on res.edge=w.gid', append_to_names||'_best_length_heur5')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 50, 0, 5) res join ways w on res.edge=w.gid', append_to_names||'_best_time_v50_heur5')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 0, 5) res join ways w on res.edge=w.gid', append_to_names||'_best_time_v200_heur5')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 50, 0.5, 5) res join ways w on res.edge=w.gid', append_to_names||'_best_w05_v50_heur5')
            USING start_id, end_id;
        EXECUTE format('CREATE TABLE %I AS SELECT w.gid as edge, w.the_geom as the_geom, w.source as begin_node, res.node as source_node, w.length as length, w.maxspeed_forward as max_for, w.maxspeed_backward as max_back
                                             FROM astar($1, $2, 200, 0.5, 5) res join ways w on res.edge=w.gid', append_to_names||'_best_w05_v200_heur5')
            USING start_id, end_id;

        PERFORM calculate_stats(append_to_names);
    END; $$
LANGUAGE plpgsql;


SELECT * FROM test_routes(1457211, 3263579, 'GDANSK_KRAKOW');

SELECT * FROM test_routes(1284691, 3901916, 'GDYNIA_WARSZAWA');

SELECT * FROM test_routes(3574004, 4004750, 'POZNAN_WARSZAWA');

SELECT * FROM test_routes(2055950, 4003182, 'LUBLIN_WARSZAWA');

SELECT * FROM test_routes(3889813, 3910079, 'SGGW_WAWURSUS');

