package org.example.cloudstorage.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/home")
@Slf4j
public class HomeController {
    @GetMapping
    public String home() {
        return "Hello World!";
    }
}
