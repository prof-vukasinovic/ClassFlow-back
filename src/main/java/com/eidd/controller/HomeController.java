package com.eidd.controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eidd.dto.UserInfoDto;

@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "<!doctype html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<title>ClassFlow API</title>" +
                "</head>" +
                "<body style=\"font-family: Arial, sans-serif; padding: 24px;\">" +
                "<h1>ClassFlow API</h1>" +
                "<p>Backend is running.</p>" +
                "</body>" +
                "</html>";
    }

    @GetMapping("/me")
    @ResponseBody
    public UserInfoDto getCurrentUser(Principal principal) {
        if (principal == null) {
            return new UserInfoDto("anonymous", List.of());
        }

        String username = principal.getName();
        List<String> roles = List.of();

        if (principal instanceof Authentication auth) {
            roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .collect(Collectors.toList());
        }

        return new UserInfoDto(username, roles);
    }
}
