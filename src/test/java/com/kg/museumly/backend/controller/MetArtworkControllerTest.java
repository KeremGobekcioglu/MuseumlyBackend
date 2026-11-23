package com.kg.museumly.backend.controller;
import com.kg.museumly.backend.dtos.artwork.MetArtworkItem;
import com.kg.museumly.backend.dtos.artwork.MetArtworkPage;
import com.kg.museumly.backend.met.MetDepartment;
import com.kg.museumly.backend.service.MetArtworkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(MetArtworkController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MetArtworkControllerTest {
    private static final String URL = "/met/artworks";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetArtworkService metArtworkService;

    private static MetArtworkPage emptyPage() {
        return new MetArtworkPage(List.of(), "0", true);
    }

    @Test
    void okPage_sendsTheContractJson() throws Exception {
        // Every field gets a different value, so a swapped pair also fails.
        MetArtworkItem item = new MetArtworkItem(
                "436535", "Wheat Field", "Vincent van Gogh", "Dutch, 1853-1890",
                "1889", 1889, "Paintings", "European Paintings", "Oil on canvas",
                "29 x 36 in.", "Purchase", "Dutch", "Post-Impressionism",
                "https://img/small.jpg", "https://img/full.jpg", 800, 600);
        MetArtworkPage page = new MetArtworkPage(List.of(item), "42", false);
        when(metArtworkService.page(any(), anyLong(), any(), any(), anyInt())).thenReturn(page);

        // STRICT: a renamed, missing or extra key fails.
        mockMvc.perform(get(URL).param("department", "11"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "items": [
                            {
                              "sourceId": "436535",
                              "title": "Wheat Field",
                              "artist": "Vincent van Gogh",
                              "artistBio": "Dutch, 1853-1890",
                              "objectDate": "1889",
                              "beginYear": 1889,
                              "classification": "Paintings",
                              "department": "European Paintings",
                              "medium": "Oil on canvas",
                              "dimensions": "29 x 36 in.",
                              "creditLine": "Purchase",
                              "culture": "Dutch",
                              "period": "Post-Impressionism",
                              "imageSmallUrl": "https://img/small.jpg",
                              "imageUrl": "https://img/full.jpg",
                              "imageWidth": 800,
                              "imageHeight": 600
                            }
                          ],
                          "nextCursor": "42",
                          "exhausted": false
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void missingCursorAndSize_useDefaults() throws Exception {
        when(metArtworkService.page(any(), anyLong(), any(), any(), anyInt())).thenReturn(emptyPage());

        mockMvc.perform(get(URL).param("department", "11"))
                .andExpect(status().isOk());

        verify(metArtworkService).page(MetDepartment.EUROPEAN_PAINTINGS, 0L, null, null, 20);
    }

    @Test
    void sentParameters_reachTheService() throws Exception {
        when(metArtworkService.page(any(), anyLong(), any(), any(), anyInt())).thenReturn(emptyPage());

        mockMvc.perform(get(URL)
                        .param("department", "11")
                        .param("fromYear", "1600")
                        .param("toYear", "1699")
                        .param("cursor", "67626")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(metArtworkService).page(MetDepartment.EUROPEAN_PAINTINGS, 67626L, 1600, 1699, 5);
    }

    @Test
    void edgeValues_areAccepted() throws Exception {
        when(metArtworkService.page(any(), anyLong(), any(), any(), anyInt())).thenReturn(emptyPage());

        mockMvc.perform(get(URL + "?department=11&size=1")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "?department=11&size=50")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "?department=11&cursor=0")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "?department=11&fromYear=1600&toYear=1600")).andExpect(status().isOk());
    }

    // ---- 400 from our checks: status, our code, and the service never called ----

    private void expectBadRequest(String query, String code) throws Exception {
        mockMvc.perform(get(URL + "?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code));
        verifyNoInteractions(metArtworkService);
    }

    @Test
    void unknownDepartment_is400() throws Exception {
        expectBadRequest("department=99", "INVALID_DEPARTMENT");
    }

    @Test
    void onlyFromYear_is400() throws Exception {
        expectBadRequest("department=11&fromYear=1600", "INVALID_YEAR_RANGE");
    }

    @Test
    void onlyToYear_is400() throws Exception {
        expectBadRequest("department=11&toYear=1699", "INVALID_YEAR_RANGE");
    }

    @Test
    void fromYearAfterToYear_is400() throws Exception {
        expectBadRequest("department=11&fromYear=1700&toYear=1600", "INVALID_YEAR_RANGE");
    }

    @Test
    void sizeBelowRange_is400() throws Exception {
        expectBadRequest("department=11&size=0", "INVALID_SIZE");
    }

    @Test
    void sizeAboveRange_is400() throws Exception {
        expectBadRequest("department=11&size=51", "INVALID_SIZE");
    }

    @Test
    void negativeCursor_is400() throws Exception {
        expectBadRequest("department=11&cursor=-1", "INVALID_CURSOR");
    }

    // ---- 400 from Spring itself (no code): must stay 400, never the catch-all 500 ----

    @Test
    void missingDepartmentOrNonNumber_is400() throws Exception {
        mockMvc.perform(get(URL + "?size=2")).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "?department=11&cursor=abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(metArtworkService);
    }
}
