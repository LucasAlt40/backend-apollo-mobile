package br.com.apollomusic.app.infra.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() throws IOException {
        InputStream serviceAccount = getClass()
                .getClassLoader()
                .getResourceAsStream("firebase/apollo-music-2aaa4-firebase-adminsdk-fbsvc-be5dc58fe9.json");

        if (serviceAccount == null) {
            throw new RuntimeException("❌ Firebase service account NOT FOUND! Verifique o caminho em src/main/resources/firebase/");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
            System.out.println("🔥 Firebase inicializado com sucesso!");
        }
    }

}
