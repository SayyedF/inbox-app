package com.jilani.inbox.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public class AuthService {

    public String getLoggedInUserId(@AuthenticationPrincipal OAuth2User principal) {
        if(principal == null)
            return null;
        if(StringUtils.hasText(principal.getAttribute("login"))) {
            return principal.getAttribute("login");
        }
        if(StringUtils.hasText(principal.getAttribute("email"))) {
            return principal.getAttribute("email");
        }

        return null;
    }
}
