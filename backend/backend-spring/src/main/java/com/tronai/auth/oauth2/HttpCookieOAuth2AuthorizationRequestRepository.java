package com.tronai.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;

/**
 * Stores OAuth2 authorization requests in short-lived HttpOnly cookies to maintain
 * a completely stateless architecture without server-side HTTP sessions.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "tron_oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }
        return deserialize(cookie.getValue());
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            return;
        }
        String serialized = serialize(authorizationRequest);
        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialized);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest current = loadAuthorizationRequest(request);
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        return current;
    }

    private static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    private static String serialize(OAuth2AuthorizationRequest object) {
        byte[] bytes = SerializationUtils.serialize(object);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    @SuppressWarnings("deprecation")
    private static OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
