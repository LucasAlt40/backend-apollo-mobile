package br.com.apollomusic.app.application;

import br.com.apollomusic.app.domain.Establishment.Establishment;
import br.com.apollomusic.app.domain.Owner.Owner;
import br.com.apollomusic.app.domain.Establishment.Playlist;
import br.com.apollomusic.app.domain.Establishment.Song;
import br.com.apollomusic.app.domain.payload.response.ChangePlaylistResponse;
import br.com.apollomusic.app.domain.payload.response.RecommendationsResponse;
import br.com.apollomusic.app.domain.payload.response.TopTracksResponse;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AlgorithmService {

    private final ApiService apiService;
    private final Gson gson;
    private final ThirdPartyService thirdPartyService;

    @Autowired
    public AlgorithmService(ApiService apiService, Gson gson, ThirdPartyService thirdPartyService) {
        this.apiService = apiService;
        this.gson = gson;
        this.thirdPartyService = thirdPartyService;
    }

    public void runAlgorithm(Establishment establishment) {
        Owner owner = establishment.getOwner();
        Playlist playlist = establishment.getPlaylist();

        HashMap<String, Integer> songsQuantityPerArtist = getSongsQuantityPerArtist(playlist);
        if (songsQuantityPerArtist == null) return;

        Collection<Song> songsInPlaylist = playlist.getSongs();

       // resetSongsInPlaylist(playlist.getId(), owner.getAccessToken(), songsInPlaylist, playlist.getSnapshot());
        int songsQuantity;
        Set<Song> songs;

        for (var entry : songsQuantityPerArtist.entrySet()) {
            String artistId = entry.getKey();
            int desiredQuantity = entry.getValue();

            songsQuantity = getQuantityOfSongInPlaylistByArtist(artistId, songsInPlaylist);

            if (songsQuantity != desiredQuantity) {
                if (songsQuantity < desiredQuantity) {
                    songs = getTopTracksByArtist(desiredQuantity - songsQuantity, artistId, owner.getAccessToken());

                    for (Song s : songs) {
                        playlist.addSong(s);
                    }

                    ChangePlaylistResponse changePlaylistResponse = thirdPartyService.addSongsToPlaylist(
                            playlist.getId(), songs, owner.getAccessToken());
                    playlist.setSnapshot(changePlaylistResponse.snapshot_id());
                    establishment.setPlaylist(playlist);

                } else {
                    songs = getRandomSongsInPlaylistByArtist(desiredQuantity - songsQuantity, artistId, songsInPlaylist);

                    for (Song s : songs) {
                        playlist.removeSong(s);
                    }

                    if(!songs.isEmpty()) {
                        ChangePlaylistResponse changePlaylistResponse = thirdPartyService.removeSongsFromPlaylist(
                                playlist.getId(), playlist.getSnapshot(), songs, owner.getAccessToken());
                        playlist.setSnapshot(changePlaylistResponse.snapshot_id());
                        establishment.setPlaylist(playlist);
                    }

                }
            }
        }
    }

    private void resetSongsInPlaylist(String playlistId, String accessToken,Collection<Song> songs, String snapshot) {
       if(!songs.isEmpty()) {
           thirdPartyService.removeSongsFromPlaylist(
                   playlistId, snapshot, new HashSet<>(songs), accessToken);
       }
    }

    private int getQuantityOfSongInPlaylistByArtist(String artistId, Collection<Song> songs) {
        int quantity = 0;
        for (Song s : songs) {
            if (s.getArtist().equals(artistId)) {
                quantity++;
            }
        }
        return quantity;
    }

    private Set<Song> getRandomSongsInPlaylistByArtist(int quantity, String artistId, Collection<Song> playlist) {
        Set<Song> result = new HashSet<>();
        List<Song> songsByArtist = new ArrayList<>();
        Random random = new Random();

        for (Song s : playlist) {
            if (s.getArtist().equals(artistId)) {
                songsByArtist.add(s);
            }
        }

        for (int i = 0; i < quantity && !songsByArtist.isEmpty(); i++) {
            result.add(songsByArtist.remove(random.nextInt(songsByArtist.size())));
        }

        return result;
    }

    private Set<Song> getTopTracksByArtist(Integer quantity, String artistId, String accessToken) {
        Set<Song> songs = new HashSet<>();
        String endpoint = "/artists/" + artistId + "/top-tracks";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("market", "BR");

        String response = apiService.get(endpoint, queryParams, accessToken);
        TopTracksResponse topTracksResponse = gson.fromJson(response, TopTracksResponse.class);

        List<TopTracksResponse.Track> tracks = topTracksResponse.getTracks();

        for (int i = 0; i < Math.min(quantity, tracks.size()); i++) {
            var track = tracks.get(i);
            songs.add(new Song(track.getUri(), artistId));
        }

        return songs;
    }

    private HashMap<String, Integer> getSongsQuantityPerArtist(Playlist playlist){
        if(playlist == null) return null;

        int totalVotes = playlist.getVotesQuantity();
        if(totalVotes == 0) return null;

        HashMap<String, Integer> artistPercent = new HashMap<>();
        HashMap<String, Integer> songQuantityPerArtist = new HashMap<>();

        for(var item : playlist.getArtists().entrySet()){
            int votesInGenre = item.getValue();
            artistPercent.put(item.getKey(), votesInGenre * 100 / totalVotes);
        }

        for(var item : artistPercent.entrySet()){
            songQuantityPerArtist.put(item.getKey(), item.getValue() * Playlist.SONGLIMIT / 100);
        }

        return songQuantityPerArtist;
    }
}
