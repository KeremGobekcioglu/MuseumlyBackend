package com.kg.museumly.backend.service;

import com.kg.museumly.backend.dtos.artwork.MetArtworkPage;
import com.kg.museumly.backend.entity.Artwork;
import com.kg.museumly.backend.met.MetDepartment;
import com.kg.museumly.backend.repository.ArtworkRepository;
import org.junit.Test;

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

    private Artwork artwork(long id) {
        Artwork artwork = mock(Artwork.class);
        when(artwork.getId()).thenReturn(id);
        return artwork;
    }
}
