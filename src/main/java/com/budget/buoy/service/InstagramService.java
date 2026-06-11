package com.budget.buoy.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.budget.buoy.authentication.InstagramProfile;

@Service
public class InstagramService {

        private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
        private static final String PROFILE_URL = "https://graph.instagram.com/me";
        @Value("${INSTAGRAM_APP_ID}")
        private String clientId;

        @Value("${INSTAGRAM_APP_SECRET}")
        private String clientSecret;

        @Value("${INSTAGRAM_REDIRECT_URI}")
        private String redirectUri;

        private final RestTemplate restTemplate = new RestTemplate();
        private static final Logger logger = LoggerFactory.getLogger(InstagramService.class);


    public InstagramProfile getProfile(String code) {
        // Step 1: exchange code for short-lived access token
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id",     clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type",    "authorization_code");
        params.add("redirect_uri",  redirectUri);
        params.add("code",          code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
            TOKEN_URL,
            new HttpEntity<>(params, headers),
            Map.class
        );

        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                logger.error("Failed to exchange Instagram code for token");
            throw new RuntimeException("Failed to exchange Instagram code for token");
        }

        String accessToken = (String) tokenResponse.getBody().get("access_token");
        logger.info("Response: {}", tokenResponse.getBody());
        // Step 2: fetch profile
        // Scope instagram_business_basic gives us id + username
        String profileUri = UriComponentsBuilder.fromHttpUrl(PROFILE_URL)
            .queryParam("fields", "id,username")
            .queryParam("access_token", accessToken)
            .toUriString();

        ResponseEntity<Map> profileResponse = restTemplate.getForEntity(profileUri, Map.class);

        if (!profileResponse.getStatusCode().is2xxSuccessful() || profileResponse.getBody() == null) {
            throw new RuntimeException("Failed to fetch Instagram profile");
        }

        Map<String, Object> body = profileResponse.getBody();
        logger.info("Profile Response: {}", body);
        return new InstagramProfile(
            (String) body.get("id"),
            (String) body.get("username")
        );
    }
}