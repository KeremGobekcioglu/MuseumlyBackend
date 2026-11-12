package com.kg.museumly.backend;

import com.kg.museumly.backend.parser.MetArtworkWriter;
import com.kg.museumly.backend.parser.MetCsvImportRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Tests: MetCsvImportRunner + MetArtworkWriter against real Postgres (filtering, date cleanup, upsert).
// Extend: add a CSV line in csv(...) and assert on the artworks row; IDs 9000001+ are reserved for these tests.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MetCsvImportIntegrationTest {

    private static final String HEADER =
            "Is Public Domain,Object ID,Department,Object Name,Title,Culture,Period,Artist Display Name,"
                    + "Artist Display Bio,Object Date,Object Begin Date,Object End Date,Medium,Dimensions,"
                    + "Credit Line,Classification";

    @Autowired
    private MetArtworkWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path tempDir;

    @Test
    void import_keepsOnlyPublicDomainRowsFromWantedDepartments() throws IOException {
        Path csv = csv(
                "True,9000001,Egyptian Art,Statue,Cat,,,,,100 BC,-100,-100,Bronze,,,Bronzes",
                "False,9000002,Egyptian Art,Statue,Not public,,,,,100 BC,-100,-100,,,,",
                "True,9000003,The American Wing,Coin,Wrong department,,,,,1853,1853,1853,,,,");

        runImport(csv);

        assertThat(count("9000001")).isEqualTo(1);
        assertThat(count("9000002")).isZero();
        assertThat(count("9000003")).isZero();
    }

    @Test
    void import_mapsColumns_blanksToNull_andNewRowsStartDiscovered() throws IOException {
        Path csv = csv("True,9000010,Asian Art,Vase,  Blue vase  ,China,Ming,,,1500,1400,1600,Porcelain,,,");

        runImport(csv);

        Map<String, Object> row = row("9000010");
        assertThat(row.get("title")).isEqualTo("Blue vase");          // stripped
        assertThat(row.get("culture")).isEqualTo("China");
        assertThat(row.get("begin_year")).isEqualTo(1400);
        assertThat(row.get("end_year")).isEqualTo(1600);
        assertThat(row.get("credit_line")).isNull();                  // blank cell -> NULL
        assertThat(row.get("status")).isEqualTo("DISCOVERED");
    }

    @Test
    void import_treatsZeroZeroDatesAsUnknown() throws IOException {
        Path csv = csv("True,9000020,Islamic Art,Tile,Undated tile,,,,,,0,0,,,,");

        runImport(csv);

        Map<String, Object> row = row("9000020");
        assertThat(row.get("begin_year")).isNull();
        assertThat(row.get("end_year")).isNull();
    }

    @Test
    void reimport_updatesCatalogueFields_withoutDuplicates_andKeepsEnrichmentState() throws IOException {
        runImport(csv("True,9000030,Medieval Art,Cross,Old title,,,,,1200,1200,1200,,,,"));
        jdbcTemplate.update("UPDATE artworks SET status = 'ACCEPTED', image_url = 'https://img/x.jpg' "
                + "WHERE source = 'met' AND source_id = '9000030'");

        runImport(csv("True,9000030,Medieval Art,Cross,New title,,,,,1200,1200,1200,,,,"));

        assertThat(count("9000030")).isEqualTo(1);
        Map<String, Object> row = row("9000030");
        assertThat(row.get("title")).isEqualTo("New title");
        assertThat(row.get("status")).isEqualTo("ACCEPTED");          // upsert must not reset worker state
        assertThat(row.get("image_url")).isEqualTo("https://img/x.jpg");
    }

    @Test
    void import_disabled_doesNothing() throws IOException {
        Path csv = csv("True,9000040,Greek and Roman Art,Vase,Skipped,,,,,,,,,,,");

        new MetCsvImportRunner(false, csv.toString(), writer).run(null);

        assertThat(count("9000040")).isZero();
    }

    private void runImport(Path csv) throws IOException {
        new MetCsvImportRunner(true, csv.toString(), writer).run(null);
    }

    private Path csv(String... lines) throws IOException {
        Path file = tempDir.resolve("met-" + System.nanoTime() + ".csv");
        Files.writeString(file, HEADER + "\n" + String.join("\n", lines) + "\n");
        return file;
    }

    private int count(String sourceId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM artworks WHERE source = 'met' AND source_id = ?", Integer.class, sourceId);
    }

    private Map<String, Object> row(String sourceId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM artworks WHERE source = 'met' AND source_id = ?", sourceId);
    }
}
