ALTER TABLE artworks
    ADD COLUMN additional_images JSONB,
    ADD COLUMN constituents      JSONB,
    ADD COLUMN measurements      JSONB;