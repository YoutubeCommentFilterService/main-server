package com.hanhome.youtube_comments.oauth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private final OAuth2AuthorizationRequestResolver authorizationRequestResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorize"
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest =
                authorizationRequestResolver.resolve(request);

        return authorizationRequest != null
                ? customAuthorizationRequest(authorizationRequest)
                : null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest =
                authorizationRequestResolver.resolve(request, clientRegistrationId);

        return authorizationRequest != null
                ? customAuthorizationRequest(authorizationRequest)
                : null;
    }

    private OAuth2AuthorizationRequest customAuthorizationRequest(
        OAuth2AuthorizationRequest authorizationRequest
    ) {
        Map<String, Object> parameters = new HashMap<>(authorizationRequest.getAdditionalParameters());

        parameters.put("access_type", "offline");
        parameters.put("prompt", "consent");

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(parameters)
                .build();
    }
}
