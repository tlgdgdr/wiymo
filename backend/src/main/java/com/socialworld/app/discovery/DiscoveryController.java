package com.socialworld.app.discovery;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.discovery.dto.DiscoveredUserResponse;
import com.socialworld.app.intention.Intention;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
@Tag(name = "Discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @GetMapping("/users")
    @Operation(summary = "Recommended people: weighted by intention, presence, languages, shared room, recency")
    public List<DiscoveredUserResponse> discover(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Intention intention,
            @RequestParam(required = false) String languageCode,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false, defaultValue = "false") boolean onlineOnly,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax) {
        return discoveryService.discover(user.id(), new DiscoveryService.Filters(
                intention, languageCode, countryCode, onlineOnly, ageMin, ageMax));
    }
}
