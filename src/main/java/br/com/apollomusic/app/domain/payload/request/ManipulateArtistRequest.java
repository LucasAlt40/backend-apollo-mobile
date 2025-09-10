package br.com.apollomusic.app.domain.payload.request;

import java.util.Set;
// GENERO AQUI
public record ManipulateArtistRequest(Set<String> artistIds) {
}
