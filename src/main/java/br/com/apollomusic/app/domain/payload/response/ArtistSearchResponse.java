package br.com.apollomusic.app.domain.payload.response;

import java.util.List;

public record ArtistSearchResponse(
        String id,
        String name,
        List<String> images
) {}

