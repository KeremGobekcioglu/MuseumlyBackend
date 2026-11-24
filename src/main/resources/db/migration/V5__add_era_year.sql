ALTER TABLE artworks
    ADD COLUMN era_year integer
        GENERATED ALWAYS AS (floor((begin_year + end_year) / 2.0 )::integer ) STORED;
