package br.com.apollomusic.app.domain.payload.response;

import java.util.Collection;
// GENERO AQUI
public record UserReponse(Long id, String username, Collection<String> genres, Long establishmentId) {
}
