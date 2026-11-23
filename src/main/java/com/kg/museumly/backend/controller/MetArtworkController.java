package com.kg.museumly.backend.controller;

import com.kg.museumly.backend.dtos.artwork.MetArtworkPage;
import com.kg.museumly.backend.exception.ApiException;
import com.kg.museumly.backend.met.MetDepartment;
import com.kg.museumly.backend.service.MetArtworkService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/met")
public class MetArtworkController {
    private final MetArtworkService metArtworkService;

    private static final int DEFAULT_SIZE = 20, MIN_SIZE = 1, MAX_SIZE = 50;
    public MetArtworkController(MetArtworkService metArtworkService) {
        this.metArtworkService = metArtworkService;
    }

    @GetMapping("/artworks")
    public MetArtworkPage page(
            @RequestParam int department,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        Optional<MetDepartment> metDepartment = MetDepartment.fromMetId(department);
        if (metDepartment.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEPARTMENT",
                    "Unknown department: " + department);
        }
        if(size != null && (size < MIN_SIZE || size > MAX_SIZE))
        {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SIZE", "size must be between 1 and 50");
        }
        if(cursor != null && cursor < 0 )
        {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "cursor must not be negative");
        }
        if ((fromYear != null && toYear == null) || (fromYear == null && toYear != null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_YEAR_RANGE",
                    "fromYear and toYear must be sent together");
        }
        if (fromYear != null && fromYear > toYear) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_YEAR_RANGE",
                    "fromYear must not be after toYear");
        }
        long afterId;
        if(cursor == null)
        {
            afterId = 0L;
        }
        else afterId = cursor;

        int pageSize;
        if(size == null)
        {
            pageSize = DEFAULT_SIZE;
        }
        else pageSize = size;
        return metArtworkService.page(metDepartment.get(), afterId , fromYear, toYear, pageSize);
    }
}
