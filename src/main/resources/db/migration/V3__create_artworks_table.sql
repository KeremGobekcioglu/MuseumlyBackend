CREATE TABLE artworks (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- which museum record this is (importer)
    source              TEXT        NOT NULL,
    source_id           TEXT        NOT NULL,

    -- catalogue data (importer, from the CSV)
    department          TEXT        NOT NULL,
    title               TEXT,
    object_name         TEXT,
    artist_display_name TEXT,
    artist_display_bio  TEXT,
    object_date         TEXT,
    begin_year          INTEGER,
    end_year            INTEGER,
    classification      TEXT,
    medium              TEXT,
    dimensions          TEXT,
    credit_line         TEXT,
    culture             TEXT,
    period              TEXT,

    -- enrichment state (worker, from the API)
    status              TEXT        NOT NULL DEFAULT 'DISCOVERED',
    attempts            INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ,
    last_error          TEXT,
    image_url           TEXT,
    image_small_url     TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_artworks_source UNIQUE (source, source_id),
    CONSTRAINT ck_artworks_status
        CHECK (status IN ('DISCOVERED', 'ACCEPTED', 'REJECTED', 'FAILED'))
);