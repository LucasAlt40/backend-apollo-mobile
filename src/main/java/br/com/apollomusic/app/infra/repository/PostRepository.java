package br.com.apollomusic.app.infra.repository;

import br.com.apollomusic.app.domain.Establishment.Post;
import br.com.apollomusic.app.domain.Owner.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository  extends JpaRepository<Post, Long> {
}
