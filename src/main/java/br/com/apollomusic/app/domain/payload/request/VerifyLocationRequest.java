package br.com.apollomusic.app.domain.payload.request;

public record VerifyLocationRequest(
   String latitude,
   String longitude,
   String deviceToken
) {}
