package br.com.apollomusic.app.domain.Establishment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class NotifiedDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceToken;

    private Long establishmentId;

    public NotifiedDevice() {}

    public NotifiedDevice(String deviceToken, Long establishmentId) {
        this.deviceToken = deviceToken;
        this.establishmentId = establishmentId;
    }
}
