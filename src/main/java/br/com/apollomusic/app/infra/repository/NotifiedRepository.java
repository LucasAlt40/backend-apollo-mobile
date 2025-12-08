package br.com.apollomusic.app.infra.repository;

import br.com.apollomusic.app.domain.Establishment.NotifiedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifiedRepository extends JpaRepository<NotifiedDevice, Long> {
    boolean existsByDeviceTokenAndEstablishmentId(String deviceToken, Long establishmentId);
}