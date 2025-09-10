package br.com.apollomusic.app.domain.Establishment;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Immutable;

@Immutable
@Embeddable
public class Song {

    private String uri;
    private String artistId; // GENERO AQUI

    public Song() {
    }

    public Song(String uri, String artistId) {
        this.uri = uri;
        this.artistId = artistId;
    }

    public String getUri() {
        return uri;
    }

    public String getArtist() {
        return artistId;
    }
}