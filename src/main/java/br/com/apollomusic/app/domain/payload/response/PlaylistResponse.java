package br.com.apollomusic.app.domain.payload.response;

import br.com.apollomusic.app.domain.payload.Image;

import java.util.Collection;
import java.util.Map;
// GENERO AQUI
public record PlaylistResponse(String id, String name, String description, Collection<Image> images, Map<String, Integer> initialArtists, Map<String, Integer> blockedArtists, Map<String, Integer> artists, boolean hasIncrementedArtist ) {
}
