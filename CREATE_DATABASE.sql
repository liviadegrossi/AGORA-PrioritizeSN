
CREATE EXTENSION postgis;

CREATE TABLE catchments (gid int, CONSTRAINT gid_PK PRIMARY KEY(gid));

SELECT AddGeometryColumn ('public','catchments','area', 4326, 'MULTIPOLYGON', 2);

CREATE TABLE stations (id text, number bigint, shortname text, longname text, km real, agency text, water_shortname text, water_longname text, catchments_gid int, CONSTRAINT id PRIMARY KEY(id), CONSTRAINT catchments_gid FOREIGN KEY (catchments_gid) REFERENCES catchments (gid));

SELECT AddGeometryColumn ('public','stations','spatial_data', 4326, 'POINT', 2);

CREATE TABLE measurements (stations_id text, time_stamp timestamp, shortname text, value real, stateMnwMhw text, stateNswHsw text, CONSTRAINT id_T PRIMARY KEY (stations_id, time_stamp), CONSTRAINT stations_id FOREIGN KEY (stations_id) REFERENCES stations (id));

CREATE TABLE flooded_areas (catchments_gid int, flooded_initial_time timestamp, flooded_final_time timestamp, CONSTRAINT catchments_gid_flooded FOREIGN KEY (catchments_gid) REFERENCES catchments (gid), PRIMARY KEY (catchments_gid, flooded_initial_time));

CREATE TABLE tweets (tweet_id bigint, tweet_time timestamp, username text, tweet_text text, catchments_gid int, flooded_initial_time timestamp, distance float, CONSTRAINT tweet_id PRIMARY KEY(tweet_id), CONSTRAINT catchments_gid_flooded_initial_time FOREIGN KEY (catchments_gid, flooded_initial_time) REFERENCES flooded_areas (catchments_gid, flooded_initial_time));

SELECT AddGeometryColumn ('public','tweets','location', 4326, 'POINT', 2);
