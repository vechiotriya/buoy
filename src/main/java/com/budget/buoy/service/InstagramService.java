package com.budget.buoy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.budget.buoy.authentication.InstagramProfile;
import com.budget.buoy.authentication.InstagramTokenResponse;

@Service
public class InstagramService {

    @Value("${INSTAGRAM_APP_ID}")
    private String clientId;

    @Value("${INSTAGRAM_APP_SECRET}")
    private String clientSecret;

    @Value("${INSTAGRAM_REDIRECT_URI}")
    private String redirectUri;

    
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(InstagramService.class);

    public InstagramProfile getProfile(String code) {

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
logger.info("APP_ID = {}", clientId);
logger.info("SECRET = {}", clientSecret);
logger.info("REDIRECT = {}", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> request =
                new HttpEntity<>(body, headers);

        InstagramTokenResponse tokenResponse =
                restTemplate.postForObject(
                        "https://api.instagram.com/oauth/access_token",
                        request,
                        InstagramTokenResponse.class);
        logger.info("Access token:{} {}",tokenResponse, tokenResponse.access_token());
        String accessToken = tokenResponse.access_token();

        String url ="https://graph.instagram.com/v21.0/me?fields=id,username&access_token=" + accessToken;
        
        return restTemplate.getForObject(
                url,
                InstagramProfile.class);
    }
}
