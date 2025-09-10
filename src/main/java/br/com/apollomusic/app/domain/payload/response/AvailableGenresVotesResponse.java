package br.com.apollomusic.app.domain.payload.response;

import java.util.Map;
// GENERO AQUI
public record AvailableGenresVotesResponse(Map<String, Integer> genresAvailable, Integer totalVotes) {
}
