package br.com.apollomusic.app.domain.Establishment;

import jakarta.persistence.*;

import java.util.*;

@Entity
public class Playlist {

    public static final int SONGLIMIT = 20;

    @Id
    private String id;

    private String snapshot;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establishment_id")
    private Establishment establishment;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "artists_playlist", joinColumns = @JoinColumn(name = "playlist_id"))
    @MapKeyColumn(name = "artists")
    @Column(name = "votes")
    private Map<String, Integer> artists = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "playlist_songs", joinColumns = @JoinColumn(name = "playlist_id"))
    private Collection<Song> songs = new HashSet<>();

    public Playlist() {
    }

    public Playlist(String id, String snapshot, Establishment establishment, Map<String, Integer> artists, Collection<Song> songs) {
        this.id = id;
        this.snapshot = snapshot;
        this.establishment = establishment;
        this.artists = artists;
        this.songs = songs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public Establishment getEstablishment() {
        return establishment;
    }

    public void setEstablishment(Establishment establishment) {
        this.establishment = establishment;
    }

    public Map<String, Integer> getArtists() {
        return artists;
    }

    public void setArtists(Map<String, Integer> genres) {
        this.artists = genres;
    }

    public Collection<Song> getSongs() {
        return songs;
    }

    public void setSongs(Collection<Song> songs) {
        this.songs = songs;
    }

    public String getUri(){
        return "spotify:playlist:" + id;
    }

    public Integer getVotesQuantity(){
        Integer votesQuantity = 0;
        for(Map.Entry<String, Integer> entry: artists.entrySet()){
            if(entry.getValue() != 0){
                votesQuantity++;
            }
        }
        return votesQuantity;
    }

    public void addSong(Song song){
        this.songs.add(song);
    }

    public void removeSong(Song song){
        if(song != null){
            this.songs.remove(song);
        }
    }

    public void incrementVoteArtist(Set<String> artists){
        for(String artist: artists){
            if(this.artists.containsKey(artist)){
                this.artists.put(artist, this.artists.get(artist) + 1);
            }
            else{
                this.artists.put(artist, 1); // Remover quando corrigir armazenamento de artistas
            }
        }
    }

    public void decrementVoteArtist(Set<String> artists){
        for(String artist: artists){
            if(this.artists.containsKey(artist)){
                if(this.artists.get(artist) > 0){
                    this.artists.put(artist, this.artists.get(artist) - 1);
                }
            }
        }
    }

    private void removeArtists(Set<String> artists){
        for(String artist : artists){
            this.artists.remove(artist);
        }
    }

    private void addArtists(Set<String> artists){
        for(String artist : artists){
            this.artists.put(artist, 0);
        }
    }
}