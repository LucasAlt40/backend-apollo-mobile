package br.com.apollomusic.app.application;

import br.com.apollomusic.app.domain.Establishment.NotifiedDevice;
import br.com.apollomusic.app.domain.payload.request.ManipulateArtistRequest;
import br.com.apollomusic.app.domain.payload.request.VerifyLocationRequest;
import br.com.apollomusic.app.infra.config.LocalizationUtils;
import br.com.apollomusic.app.infra.repository.EstablishmentRepository;
import br.com.apollomusic.app.infra.repository.NotifiedRepository;
import ch.qos.logback.core.util.LocationUtil;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    @Autowired
    private EstablishmentService establishmentService;

    @Autowired
    private EstablishmentRepository establishmentRepository;
    
    @Autowired
    private NotifiedRepository notifiedRepository;

    public void verifyLocation(VerifyLocationRequest verifyLocationRequest) {

        // --- Validação correta ---
        if (verifyLocationRequest.latitude() == null ||
                verifyLocationRequest.longitude() == null ||
                verifyLocationRequest.latitude().isBlank() ||
                verifyLocationRequest.longitude().isBlank()) {

            System.out.println("Coordenadas inválidas. Ignorando request.");
            return;
        }

        String latitude = verifyLocationRequest.latitude();
        String longitude = verifyLocationRequest.longitude();
        String deviceToken = verifyLocationRequest.deviceToken();

        var establishments = this.establishmentRepository.findAll();

        establishments.stream()
                .filter(est -> est.getLatitude() != null && est.getLongitude() != null)
                .forEach(est -> {

                    double distance = LocalizationUtils.calculeDistance(
                            latitude,
                            longitude,
                            est.getLatitude(),
                            est.getLongitude()
                    );

                    if (distance <= 50) {

                        boolean alreadyNotified =
                                notifiedRepository.existsByDeviceTokenAndEstablishmentId(deviceToken, est.getId());

                        if (!alreadyNotified) {
                            System.out.println("Usuário está próximo do estabelecimento: " + est.getName());
                            sendNotification(deviceToken);

                            // registra que já enviou
                            notifiedRepository.save(new NotifiedDevice(deviceToken, est.getId()));
                        }
                    }
                });
    }


    private String sendNotification(String token) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle("Estabelicimento por perto!")
                            .setBody("Você está proximo de um estabelecimento.")
                            .build())
                    .build();

            return FirebaseMessaging.getInstance().send(message);

        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao enviar notificação: " + e.getMessage();
        }
    }
}
