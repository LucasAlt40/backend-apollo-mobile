package br.com.apollomusic.app.domain.payload.request;

import java.util.Set;
// GENERO AQUI
public record LoginUserRequest(String username, Set<String> genres, Long establishmentId, String latitude, String longitude) {
}
