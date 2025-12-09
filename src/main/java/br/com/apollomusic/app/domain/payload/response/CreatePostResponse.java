package br.com.apollomusic.app.domain.payload.response;

import java.util.List;

public record CreatePostResponse(
        Long id,
        String username,
        String content,
        List<String> imageUrls,
        Long establishmentId
) {}
