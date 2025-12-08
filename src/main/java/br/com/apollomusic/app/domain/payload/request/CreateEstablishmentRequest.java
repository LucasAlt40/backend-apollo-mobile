package br.com.apollomusic.app.domain.payload.request;

public record CreateEstablishmentRequest(String name, String email, String latitude, String longitude) {
}
