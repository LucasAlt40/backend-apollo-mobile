package br.com.apollomusic.app.domain.payload.request;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public record CreatePostRequest(
        String username,
        String content,
        List<MultipartFile> images,
        Optional<Long> establishmentId
) {}
