package com.kg.museumly.backend.parser;

import org.apache.commons.csv.CSVRecord;

public final class MetCsvMapper {

    private MetCsvMapper() {
    }

    public static MetCsvRow toRow(CSVRecord record) {
        Integer beginYear = parseYear(record.get("Object Begin Date"));
        Integer endYear = parseYear(record.get("Object End Date"));

        // Both 0 is the Met's "date unknown", not the year 0.
        if (beginYear != null && endYear != null
                && beginYear.intValue() == 0 && endYear.intValue() == 0) {
            beginYear = null;
            endYear = null;
        }

        return new MetCsvRow(
                clean(record.get("Object ID")),
                clean(record.get("Department")),
                clean(record.get("Title")),
                clean(record.get("Object Name")),
                clean(record.get("Artist Display Name")),
                clean(record.get("Artist Display Bio")),
                clean(record.get("Object Date")),
                beginYear,
                endYear,
                clean(record.get("Classification")),
                clean(record.get("Medium")),
                clean(record.get("Dimensions")),
                clean(record.get("Credit Line")),
                clean(record.get("Culture")),
                clean(record.get("Period"))
        );
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        return stripped;
    }

    private static Integer parseYear(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return Integer.parseInt(cleaned);
    }
}