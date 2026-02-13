package com.eidd.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
}
