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
import java.util.Set;


@Component
public class MetCsvImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MetCsvImportRunner.class);

    private static final Set<String> DEPARTMENTS = Set.of(
            "European Paintings",
            "European Sculpture and Decorative Arts",
            "Islamic Art",
            "Asian Art"
    );

    private final boolean enabled;
    private final String csvPath;

    public MetCsvImportRunner(
            @Value("${museumly.import.met.enabled:false}") boolean enabled,
            @Value("${museumly.import.met.csv-path:}") String csvPath) {
        this.enabled = enabled;
        this.csvPath = csvPath;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) {
            return;
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        int total = 0;
        int kept = 0;

        try (Reader reader = Files.newBufferedReader(Path.of(csvPath), StandardCharsets.UTF_8);
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
            }
        }

        log.info("Met CSV dry run: {} rows read, {} kept", total, kept);
    }
}