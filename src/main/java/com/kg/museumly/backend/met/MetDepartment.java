package com.kg.museumly.backend.met;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public enum MetDepartment {
    ANCIENT_WEST_ASIAN_ART(3, "Ancient West Asian Art"),
    ARTS_OF_AFRICA_OCEANIA_AMERICAS(5, "Arts of Africa, Oceania, and the Americas"),
    ASIAN_ART(6, "Asian Art"),
    EGYPTIAN_ART(10, "Egyptian Art"),
    EUROPEAN_PAINTINGS(11, "European Paintings"),
    EUROPEAN_SCULPTURE_AND_DECORATIVE_ARTS(12, "European Sculpture and Decorative Arts"),
    GREEK_AND_ROMAN_ART(13, "Greek and Roman Art"),
    ISLAMIC_ART(14, "Islamic Art"),
    MEDIEVAL_ART(17, "Medieval Art");

    private final int metId;
    private final String csvName;

    MetDepartment(int metId, String csvName) {
        this.metId = metId;
        this.csvName = csvName;
    }
    public String getCsvName()
    {
        return csvName;
    }

    public static Optional<MetDepartment> fromMetId(int metId)
    {
        for(MetDepartment metDepartment : values())
        {
            if(metDepartment.metId == metId)
                return Optional.of(metDepartment);
        }
        return Optional.empty();
    }

    public static Set<String> csvNames()
    {
        Set<String> result = new HashSet<>();
        for(MetDepartment metDepartment : values())
        {
            result.add(metDepartment.csvName);
        }
        return Set.copyOf(result);
    }
}
