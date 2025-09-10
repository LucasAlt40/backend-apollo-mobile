package br.com.apollomusic.app.domain.Establishment;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.util.Collection;

@Immutable
@Embeddable
public class User {

    private String artists; // GENERO AQUI
    private Long expiresIn;

    public User() {}

    public User(String artists) {
        this.artists = artists;
    }

    public String getArtists() {
        return artists;
    }

    public void setArtists(String genres) {
        this.artists = genres;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void addGenre(Collection<String> genres) {
        this.artists = String.join(",", genres);
    }
}
