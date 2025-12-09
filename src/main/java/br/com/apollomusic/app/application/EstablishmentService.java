package br.com.apollomusic.app.application;

import br.com.apollomusic.app.domain.Establishment.Establishment;
import br.com.apollomusic.app.domain.Establishment.Playlist;
import br.com.apollomusic.app.domain.Establishment.Post;
import br.com.apollomusic.app.domain.Establishment.User;
import br.com.apollomusic.app.domain.Owner.Owner;
import br.com.apollomusic.app.domain.payload.request.CreateEstablishmentRequest;
import br.com.apollomusic.app.domain.payload.request.CreatePostRequest;
import br.com.apollomusic.app.domain.payload.request.SetDeviceRequest;
import br.com.apollomusic.app.domain.payload.response.*;
import br.com.apollomusic.app.infra.config.ImageUploader;
import br.com.apollomusic.app.infra.repository.EstablishmentRepository;
import br.com.apollomusic.app.infra.repository.OwnerRepository;
import br.com.apollomusic.app.infra.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EstablishmentService {

    private final EstablishmentRepository establishmentRepository;
    private final OwnerRepository ownerRepository;
    private final ThirdPartyService thirdPartyService;
    private final AlgorithmService algorithmService;
    private final ImageUploader imageUploader;
    private final PostRepository postRepository;

    @Autowired
    public EstablishmentService(EstablishmentRepository establishmentRepository, OwnerRepository ownerRepository, ThirdPartyService thirdPartyService, AlgorithmService algorithmService, ImageUploader imageUploader, PostRepository postRepository) {
        this.establishmentRepository = establishmentRepository;
        this.ownerRepository = ownerRepository;
        this.thirdPartyService = thirdPartyService;
        this.algorithmService = algorithmService;
        this.imageUploader = imageUploader;
        this.postRepository = postRepository;
    }

    public ResponseEntity<?> createEstablishment(CreateEstablishmentRequest createEstablishmentRequest){
        if(createEstablishmentRequest.name() != null && !createEstablishmentRequest.name().isEmpty()
                && createEstablishmentRequest.email() != null && !createEstablishmentRequest.email().isEmpty()){

            Owner owner = ownerRepository.findByEmail(createEstablishmentRequest.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if(owner.getEstablishment() != null) return new ResponseEntity<>(HttpStatus.CONFLICT);

            Establishment newEstablishment = new Establishment();
            newEstablishment.setName(createEstablishmentRequest.name());
            newEstablishment.setOff(true);
            newEstablishment.setOwner(owner);
            newEstablishment.setLatitude(createEstablishmentRequest.latitude());
            newEstablishment.setLongitude(createEstablishmentRequest.longitude());

            establishmentRepository.save(newEstablishment);
            return new ResponseEntity<>(newEstablishment.getId(), HttpStatus.CREATED);
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> turnOn(Long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (establishment.isOff()){
            if(establishment.getPlaylist() == null){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            if (establishment.getPlaylist().getVotesQuantity() > 0){
                establishment.setOff(false);

                if (establishment.getDeviceId() == null || establishment.getDeviceId().isBlank()){
                    return new ResponseEntity<>(HttpStatus.CONFLICT);
                }

                algorithmService.runAlgorithm(establishment);

                establishmentRepository.save(establishment);

                thirdPartyService.setRepeatMode("context", establishment.getDeviceId(), establishment.getOwner().getAccessToken());

                thirdPartyService.setShuffleMode("true", establishment.getDeviceId(), establishment.getOwner().getAccessToken());

                thirdPartyService.startPlayback(establishment.getPlaylist().getUri(), establishment.getOwner().getAccessToken(), establishment.getDeviceId());

                return new ResponseEntity<>(HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
    }

    public ResponseEntity<?> turnOff(Long establishmentId) {
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (establishment.isOff()) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        establishment.setOff(true);

        if (establishment.getDeviceId() == null || establishment.getDeviceId().isBlank()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        Playlist playlist = establishment.getPlaylist();

        Map<String, Integer> artistsVotesReset = new HashMap<>();
        for (String artistId : playlist.getArtists().keySet()) {
            artistsVotesReset.put(artistId, 0);
        }

        playlist.setArtists(artistsVotesReset);

        Set<String> artists = Arrays.stream(
                playlist.getInitialArtists().split(",")
        ).map(String::trim).collect(Collectors.toSet());

        playlist.incrementVoteArtist(artists);

        establishment.setUser(new HashSet<>());

        establishmentRepository.save(establishment);

        thirdPartyService.pausePlayback(establishment.getDeviceId(), establishment.getOwner().getAccessToken());

        return new ResponseEntity<>(HttpStatus.OK);
    }


    public ResponseEntity<?> createPlaylist(long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NO_CONTENT));
        CreatePlaylistResponse createPlaylistResponse = thirdPartyService.createPlaylist(establishment.getName(), "", establishment.getOwner().getAccessToken());

        Map<String, Integer> artists = new HashMap<>();

        Playlist playlist = new Playlist(createPlaylistResponse.id(), createPlaylistResponse.snapshot_id(), establishment, artists, new HashSet<>());

        establishment.setPlaylist(playlist);
        establishmentRepository.save(establishment);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    public ResponseEntity<?> addBlockGenres(long establishmentId, Set<String> genres){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();

        // Collection<String> initialGenres = playlist.getInitialArtists();
//        for(String g : genres){
//            if(initialGenres.contains(g)){
//                return new ResponseEntity<>("Você não pode bloquear artistas que estão sendo utilizados", HttpStatus.NOT_ACCEPTABLE);
//            }
//        }

        // playlist.addBlockArtists(genres);

        establishment.setPlaylist(playlist);
        establishmentRepository.save(establishment);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<?> removeBlockGenres(long establishmentId, Set<String> genres){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();
        // playlist.removeBlockArtists(genres);

        establishment.setPlaylist(playlist);
        establishmentRepository.save(establishment);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<?> incrementVoteArtists(long establishmentId, Set<String> artistIds) {
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();
        if (playlist == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        playlist.incrementVoteArtist(artistIds);

        establishment.setPlaylist(playlist);

        algorithmService.runAlgorithm(establishment);

        establishmentRepository.save(establishment);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    public ResponseEntity<?> incrementInitialVotesArtists(long establishmentId, Set<String> artistIds) {
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();
        if (playlist == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        playlist.setInitialArtists(String.join(",", artistIds));

        playlist.incrementVoteArtist(artistIds);

        establishment.setPlaylist(playlist);

        algorithmService.runAlgorithm(establishment);

        establishmentRepository.save(establishment);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    public void resetPlaylistSongs(long establishmentId) {
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();
        playlist.setSongs(new HashSet<>());

        establishment.setPlaylist(playlist);

        establishmentRepository.save(establishment);
    }


    public ResponseEntity<?> decrementVoteArtists(long establishmentId, Set<String> artistIds) {
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();
        if (playlist == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        playlist.decrementVoteArtist(artistIds);

        establishment.setPlaylist(playlist);

        algorithmService.runAlgorithm(establishment);

        establishmentRepository.save(establishment);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    public ResponseEntity<?> getPlaylist(long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Playlist playlist = establishment.getPlaylist();
        if(playlist == null) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        ThirdPartyPlaylistResponse thirdPartyPlaylistResponse = thirdPartyService.getPlaylist(playlist.getId(), establishment.getOwner().getAccessToken());

        PlaylistResponse playlistResponse = new PlaylistResponse(playlist.getId(), thirdPartyPlaylistResponse.name(), thirdPartyPlaylistResponse.description(), thirdPartyPlaylistResponse.images(), playlist.getArtists(), playlist.getArtists(), playlist.getArtists(), playlist.getVotesQuantity() > 0);
        return ResponseEntity.ok(playlistResponse);
    }

    public ResponseEntity<?> setPlaylistInitialArtists(long establishmentId, Set<String> artistIds) {
        if (artistIds.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();
        if (playlist == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        Set<String> toIncrement = new HashSet<>(artistIds);

        if (!playlist.getArtists().isEmpty()) {
            Set<String> toDecrement = playlist.getArtists().keySet().stream()
                    .filter(oldArtist -> !artistIds.contains(oldArtist))
                    .collect(Collectors.toSet());

            decrementVoteArtists(establishmentId, toDecrement);

            toIncrement = artistIds.stream()
                    .filter(newArtist -> !playlist.getArtists().containsKey(newArtist))
                    .collect(Collectors.toSet());
        }

        for (String id : artistIds) {
            playlist.getArtists().putIfAbsent(id, 0);
        }

        incrementVoteArtists(establishmentId, toIncrement);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    public ResponseEntity<?> getEstablishment(long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Playlist playlist = establishment.getPlaylist();
        PlaylistResponse playlistResponse =  null;
        if(playlist != null){
            ThirdPartyPlaylistResponse thirdPartyPlaylistResponse = thirdPartyService.getPlaylist(playlist.getId(), establishment.getOwner().getAccessToken());
            playlistResponse = new PlaylistResponse(playlist.getId(), thirdPartyPlaylistResponse.name(), thirdPartyPlaylistResponse.description(), thirdPartyPlaylistResponse.images(), playlist.getArtists(), playlist.getArtists(), playlist.getArtists(), playlist.getVotesQuantity() > 0);
        }

        EstablishmentResponse response = new EstablishmentResponse(establishment.getId(), establishment.getName(), establishment.getDeviceId(), establishment.isOff(), playlistResponse);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getEstablishmentUserInfos(long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        EstablishmentUserInfosResponse response = new EstablishmentUserInfosResponse(establishment.isOff(), establishment.getName(), establishment.getUser().size());

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getAvailableGenres(long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NO_CONTENT));

        Map<String, Integer> genres =  establishment.getPlaylist().getArtists();

        Integer totalVotes = 0;
        for (Map.Entry<String, Integer> g : genres.entrySet()){
            totalVotes += g.getValue();
        }

        AvailableGenresVotesResponse response = new AvailableGenresVotesResponse(genres, totalVotes);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getDevices(long establishmentId, String ownerEmail){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Owner owner = ownerRepository.findByEmail(ownerEmail).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(owner.getAccessToken() == null) return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);

        Map<String, Collection<DeviceResponse>> devices = thirdPartyService.getDevices(owner.getAccessToken());

        return ResponseEntity.ok(devices);
    }

    public ResponseEntity<?> getPlaybackState(long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        PlaybackStateResponse response = thirdPartyService.getPlaybackState(establishment.getOwner().getAccessToken());

        if (response != null){
            return ResponseEntity.ok(response);
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> setMainDevice(long establishmentId, SetDeviceRequest setDeviceRequest){
        Establishment establishment = establishmentRepository.findById(establishmentId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        establishment.setDeviceId(setDeviceRequest.id());
        establishmentRepository.save(establishment);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    public void removeUsers(Long establishmentId) {
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        long currentTime = System.currentTimeMillis();
        List<User> usersCopy = new ArrayList<>(establishment.getUser());
        List<User> remainingUsers = new ArrayList<>();

        for (User user : usersCopy) {
            if (user.getExpiresIn() <= currentTime) {
                Set<String> userGenres = new HashSet<>(Arrays.asList(user.getArtists().split(",")));
                decrementVoteArtists(establishment.getId(), userGenres);
            } else {
                remainingUsers.add(user);
            }
        }

        establishment.setUser(remainingUsers);
        establishmentRepository.save(establishment);
    }

    public ResponseEntity<?> resumePlayback(Long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (establishment.getDeviceId() == null || establishment.getDeviceId().isBlank()){
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        if (thirdPartyService.getPlaybackState(establishment.getOwner().getAccessToken()).is_playing()){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        thirdPartyService.resumePlayback(establishment.getPlaylist().getUri(), establishment.getOwner().getAccessToken(), establishment.getDeviceId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<?> pausePlayback(Long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (establishment.getDeviceId() == null || establishment.getDeviceId().isBlank()){
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        if (!thirdPartyService.getPlaybackState(establishment.getOwner().getAccessToken()).is_playing()){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        thirdPartyService.pausePlayback(establishment.getDeviceId(), establishment.getOwner().getAccessToken());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<?> skipToNext(Long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        thirdPartyService.skipToNext(establishment.getOwner().getAccessToken());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<?> skipToPrevious(Long establishmentId){
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        thirdPartyService.skipToPrevious(establishment.getOwner().getAccessToken());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public List<ArtistSearchResponse> searchArtists(Long establishmentId, String query) {
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String accessToken = establishment.getOwner().getAccessToken();

        return thirdPartyService.searchArtists(query, accessToken);
    }

    public CreatePostResponse createPost(CreatePostRequest createPostRequest) throws Exception {

        Establishment establishment = establishmentRepository.findById(createPostRequest.establishmentId().get())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Estabelecimento não encontrado"
                ));

        List<String> urls = new ArrayList<>();
        for (MultipartFile img : createPostRequest.images()) {
            String url = imageUploader.upload(img);
            urls.add(url);
        }

        Post post = new Post(
                createPostRequest.username(),
                createPostRequest.content(),
                urls,
                establishment
        );

        post = postRepository.save(post);

        establishment.addPost(post);

        return new CreatePostResponse(
                post.getId(),
                post.getUsername(),
                post.getContent(),
                post.getImageUrls(),
                establishment.getId()
        );
    }



    public List<CreatePostResponse> findAllPosts(Long establishmentId) {

        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Estabelecimento não encontrado"
                ));

        return establishment.getPosts()
                .stream()
                .map(post -> new CreatePostResponse(
                        post.getId(),
                        post.getUsername(),
                        post.getContent(),
                        post.getImageUrls(),
                        establishment.getId()
                ))
                .toList();
    }


    public ResponseEntity<?> removePost(Long establishmentId, Long postId) {

        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estabelecimento não encontrado"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado"));

        if (!post.getEstablishment().getId().equals(establishmentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este post não pertence ao estabelecimento informado");
        }

        establishment.getPosts().remove(post);
        post.setEstablishment(null);

        postRepository.delete(post);

        establishmentRepository.save(establishment);

        return ResponseEntity.ok("Post removido com sucesso");
    }


}
