package com.ych.contentfactory.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class PresentationPlan {

    public PresentationPlan() {
    }

    @JsonProperty("title")
    public String title;

    @JsonProperty("subtitle")
    public String subtitle;

    @JsonProperty("openingNarration")
    public String openingNarration;

    @JsonProperty("bodySlides")
    public List<BodySlide> bodySlides = new ArrayList<>();

    @JsonProperty("closingNarration")
    public String closingNarration;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class BodySlide {
        public BodySlide() {
        }

        @JsonProperty("heading")
        public String heading;

        @JsonProperty("bullets")
        public List<String> bullets = new ArrayList<>();

        @JsonProperty("narration")
        public String narration;
    }

    public void normalize() {
        if (title == null) title = "";
        if (subtitle == null) subtitle = "";
        if (openingNarration == null) openingNarration = "";
        if (closingNarration == null) closingNarration = "";
        if (bodySlides == null) bodySlides = new ArrayList<>();
        for (BodySlide s : bodySlides) {
            if (s.heading == null) s.heading = "";
            if (s.bullets == null) s.bullets = new ArrayList<>();
            if (s.narration == null) s.narration = "";
        }
    }

    public int totalSlides() {
        return 1 + bodySlides.size() + 1;
    }
}
