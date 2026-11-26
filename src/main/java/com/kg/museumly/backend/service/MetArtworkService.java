package com.kg.museumly.backend.service;

import com.kg.museumly.backend.dtos.artwork.MetArtworkItem;
import com.kg.museumly.backend.dtos.artwork.MetArtworkPage;
import com.kg.museumly.backend.entity.Artwork;
import com.kg.museumly.backend.met.MetDepartment;
import com.kg.museumly.backend.repository.ArtworkRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MetArtworkService {
    private final ArtworkRepository repository;
    private static final String UNTITLED = "Untitled";
    public MetArtworkService(ArtworkRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MetArtworkPage page(
            MetDepartment department,
            long afterId,
            Integer fromYear,
            Integer toYear,
            int size
    )
    {
        String departmentName = department.getCsvName();
        List<Artwork> artworkList =
                repository.findAcceptedPage(departmentName,afterId, fromYear, toYear, Limit.of(size));
        long nextAfterId = afterId;
        if(!artworkList.isEmpty())
        {
            // update next id with the last items id in the list.
            nextAfterId = artworkList.getLast().getId();
        }
        // Exhausted = "nothing more will ever come, stop asking".
        // Full page: there may be more, so not exhausted, and no second query.
        // Short page: ask the DB if any row in this slice is still waiting (DISCOVERED/FAILED)
        // after nextAfterId. If yes, it may become ACCEPTED later, so not exhausted.
        // Uses nextAfterId, not afterId: the next request starts there, so waiting rows
        // behind it can never reach this user and must not count.
        boolean exhausted;
        if (artworkList.size() < size) {
            exhausted = !repository.existsWaitingAfter(departmentName, nextAfterId, fromYear, toYear);
        } else {
            exhausted = false;
        }
        List<MetArtworkItem> items = new ArrayList<>();
        for(Artwork artwork : artworkList)
        {
            items.add(toItem(artwork));
        }
        return new MetArtworkPage(items,String.valueOf(nextAfterId),exhausted);
    }

    private MetArtworkItem toItem(Artwork artwork) {
        return new MetArtworkItem(
                artwork.getSourceId(),
                displayTitle(artwork),
                firstPart(artwork.getArtistDisplayName()),
                firstPart(artwork.getArtistDisplayBio()),
                artwork.getObjectDate(),
                artwork.getBeginYear(),
                artwork.getClassification(),
                artwork.getDepartment(),
                artwork.getMedium(),
                artwork.getDimensions(),
                artwork.getCreditLine(),
                artwork.getCulture(),
                artwork.getPeriod(),
                artwork.getImageSmallUrl(),
                artwork.getImageUrl(),
                artwork.getImageWidth(),
                artwork.getImageHeight());
    }
    private String displayTitle(Artwork artwork) {
        if (artwork.getTitle() != null && !artwork.getTitle().isBlank()) {
            return artwork.getTitle();
        }
        if (artwork.getObjectName() != null && !artwork.getObjectName().isBlank()) {
            return artwork.getObjectName();
        }
        return UNTITLED;
    }
    // The CSV joins several constituents with '|'. Clients get the first one,
    // as the Met API's display fields did. Name and bio parts line up (checked on the server).
    // indexOf, not split("|"): split takes a regex, where '|' means "or".
    private static String firstPart(String raw) {
        if (raw == null) {
            return null;
        }
        int bar = raw.indexOf('|');
        String first = (bar < 0 ? raw : raw.substring(0, bar)).trim();
        return first.isEmpty() ? null : first;
    }
}
