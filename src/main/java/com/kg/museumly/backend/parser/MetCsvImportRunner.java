package com.kg.museumly.backend.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Component
public class MetCsvImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MetCsvImportRunner.class);

    private static final Set<String> DEPARTMENTS = Set.of(
            "Ancient West Asian Art",                      // 3
            "Arts of Africa, Oceania, and the Americas",   // 5
            "Asian Art",                                   // 6
            "Egyptian Art",                                // 10
            "European Paintings",                          // 11
            "European Sculpture and Decorative Arts",      // 12
            "Greek and Roman Art",                         // 13
            "Islamic Art",                                 // 14
            "Medieval Art"                                 // 17
    );

    private final boolean enabled;
    private final String csvPath;
    private final MetArtworkWriter metArtworkWriter;
    private static final int BATCH_SIZE = 1000;
    public MetCsvImportRunner(
            @Value("${museumly.import.met.enabled:false}") boolean enabled,
            @Value("${museumly.import.met.csv-path:}") String csvPath,
            MetArtworkWriter metArtworkWriter
    ) {
        this.enabled = enabled;
        this.csvPath = csvPath;
        this.metArtworkWriter = metArtworkWriter;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) {
            return;
        }

        Path csv = Path.of(csvPath);
        if (!Files.isReadable(csv)) {
            log.warn("Met CSV import enabled but file not readable at {}, skipping import", csv.toAbsolutePath());
            return;
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        int total = 0;
        int kept = 0;
        List<MetCsvRow> batch = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {
                total = total + 1;

                String publicDomain = record.get("Is Public Domain");
                if (!"True".equals(publicDomain)) {
                    continue;
                }

                String department = record.get("Department");
                if (!DEPARTMENTS.contains(department)) {
                    continue;
                }

                kept = kept + 1;

                MetCsvRow row = MetCsvMapper.toRow(record);
                batch.add(row);
                if(batch.size() == BATCH_SIZE)
                {
                    metArtworkWriter.upsertBatch(batch);
                    batch.clear();
                }
            }
            if(!batch.isEmpty())
            {
                metArtworkWriter.upsertBatch(batch);
            }
        }

        log.info("Met CSV import: {} rows read, {} upserted", total, kept);
    }
}