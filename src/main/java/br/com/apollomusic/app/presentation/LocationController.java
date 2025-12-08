package br.com.apollomusic.app.presentation;

import br.com.apollomusic.app.application.LocationService;
import br.com.apollomusic.app.domain.payload.request.VerifyLocationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/location")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @PostMapping
    public ResponseEntity<?> verifyLocation(@RequestBody VerifyLocationRequest verifyLocationRequest) {
        this.locationService.verifyLocation(verifyLocationRequest);
        return ResponseEntity.ok().build();
    }
}
