package com.kg.museumly.backend.service;

import com.kg.museumly.backend.dtos.artwork.MetArtworkPage;
import com.kg.museumly.backend.entity.Artwork;
import com.kg.museumly.backend.met.MetDepartment;
import com.kg.museumly.backend.repository.ArtworkRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class MetArtworkServiceTest {

    private static final String PAINTINGS = MetDepartment.EUROPEAN_PAINTINGS.getCsvName();

    private final ArtworkRepository repository = mock(ArtworkRepository.class);
    private final MetArtworkService service = new MetArtworkService(repository);

    @Test
    public void fullPageMovesTheCursorToTheLastIdAndIsNotExhausted() {
        List<Artwork> rows = List.of(artwork(5), artwork(9));
        when(repository.findAcceptedPage(any(), anyLong(), any(), any(), any())).thenReturn(rows);

        MetArtworkPage page = service.page(MetDepartment.EUROPEAN_PAINTINGS, 0, null, null, 2);

        assertEquals("9", page.nextCursor());
        assertFalse(page.exhausted());
        verify(repository).findAcceptedPage(eq(PAINTINGS), eq(0L), isNull(), isNull(), any());
    }

    @Test
    public void emptyPageKeepsTheSameCursor() {
        when(repository.findAcceptedPage(any(), anyLong(), any(), any(), any())).thenReturn(List.of());

        MetArtworkPage page = service.page(MetDepartment.EUROPEAN_PAINTINGS, 12, null, null, 20);

        assertEquals("12", page.nextCursor());
        assertTrue(page.exhausted());
    }

    @Test
    public void shortPageIsExhausted() {
        List<Artwork> rows = List.of(artwork(5));
        when(repository.findAcceptedPage(any(), anyLong(), any(), any(), any())).thenReturn(rows);

        MetArtworkPage page = service.page(MetDepartment.EUROPEAN_PAINTINGS, 0, null, null, 20);

        assertEquals("5", page.nextCursor());
        assertTrue(page.exhausted());
    }

    @Test
    public void titleFallsBackToObjectNameThenUntitled() {
        List<Artwork> rows = List.of(
                artwork(1, "Flora and Zephyr", "Painting"),
                artwork(2, null, "Bowl"),
                artwork(3, null, null),
                artwork(4, "  ", "Vase"),
                artwork(5, "", " "));
        when(repository.findAcceptedPage(any(), anyLong(), any(), any(), any())).thenReturn(rows);

        MetArtworkPage page = service.page(MetDepartment.EUROPEAN_PAINTINGS, 0, null, null, 5);

        assertEquals("Flora and Zephyr", page.items().get(0).title());
        assertEquals("Bowl", page.items().get(1).title());
        assertEquals("Untitled", page.items().get(2).title());
        assertEquals("Vase", page.items().get(3).title());
        assertEquals("Untitled", page.items().get(4).title());
    }

    @Test
    void fullPage_doesNotRunWaitingCheck() {
        Artwork first = artwork(1L);
        Artwork second = artwork(2L);
        when(repository.findAcceptedPage(eq(PAINTINGS), eq(0L), isNull(), isNull(), any()))
                .thenReturn(List.of(first, second));

        MetArtworkPage page = service.page(MetDepartment.EUROPEAN_PAINTINGS, 0L, null, null, 2);

        assertFalse(page.exhausted());
        verify(repository, never()).existsWaitingAfter(any(), anyLong(), any(), any());
    }

    @Test
    void shortPage_withRowsWaiting_isNotExhausted_andChecksFromNewPosition() {
        Artwork first = artwork(7L);
        Artwork second = artwork(9L);
        when(repository.findAcceptedPage(eq(PAINTINGS), eq(5L), eq(1600), eq(1699), any()))
                .thenReturn(List.of(first, second));
        when(repository.existsWaitingAfter(PAINTINGS, 9L, 1600, 1699)).thenReturn(true);

        MetArtworkPage page = service.page(MetDepartment.EUROPEAN_PAINTINGS, 5L, 1600, 1699, 20);

        assertEquals("9", page.nextCursor());
        assertFalse(page.exhausted());
        verify(repository).existsWaitingAfter(PAINTINGS, 9L, 1600, 1699);
    }

    private Artwork artwork(long id) {
        return artwork(id, null, null);
    }

    private Artwork artwork(long id, String title, String objectName) {
        Artwork artwork = mock(Artwork.class);
        when(artwork.getId()).thenReturn(id);
        when(artwork.getTitle()).thenReturn(title);
        when(artwork.getObjectName()).thenReturn(objectName);
        return artwork;
    }
}
