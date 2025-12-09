package br.com.apollomusic.app.domain.Establishment;

import br.com.apollomusic.app.domain.Owner.Owner;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Entity
public class Establishment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String deviceId;

    private boolean isOff;

    private String name;

    private String latitude;

    private String longitude;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @OneToOne(mappedBy = "establishment", cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    private Playlist playlist;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user", joinColumns = @JoinColumn(name = "establishment_id"))
    private Collection<User> users = new HashSet<>();

    @OneToMany(mappedBy = "establishment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();


    public Establishment() {
    }

    public Establishment(Long id, String deviceId, boolean isOff, String name, Owner owner, Playlist playlist, Collection<User> users,  String latitude, String longitude) {
        this.id = id;
        this.deviceId = deviceId;
        this.isOff = isOff;
        this.name = name;
        this.owner = owner;
        this.playlist = playlist;
        this.users = users;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isOff() {
        return isOff;
    }

    public void setOff(boolean off) {
        isOff = off;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public boolean playlistHasInitialGenres(){
        if(playlist != null){
            return playlist.getVotesQuantity() > 0;
        }
        return false;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public Post addPost(Post post) {
        post.setEstablishment(this);
        posts.add(post);
        return post;
    }

    public void removePost(Post post) {
        posts.remove(post);
        post.setEstablishment(null);
    }


    public Collection<User> getUser() {
        return users;
    }

    public void setUser(Collection<User> users) {
        this.users = users;
    }

    public void addUser(User user) {
        user.setExpiresIn(System.currentTimeMillis() + 7200 * 1000);
        users.add(user);
    }
}
