package br.com.apollomusic.app.domain.Establishment;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String content;

    @ElementCollection
    @CollectionTable(
            name = "post_images",
            joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "image_url")
    private List<String> imageUrls;

    @ManyToOne
    @JoinColumn(name = "establishment_id")
    private Establishment establishment;

    public Post() {}

    public Post(String username, String content, List<String> imageUrls, Establishment establishment) {
        this.username = username;
        this.content = content;
        this.imageUrls = imageUrls;
        this.establishment = establishment;
    }

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public Establishment getEstablishment() { return establishment; }
    public void setEstablishment(Establishment establishment) { this.establishment = establishment; }
}
