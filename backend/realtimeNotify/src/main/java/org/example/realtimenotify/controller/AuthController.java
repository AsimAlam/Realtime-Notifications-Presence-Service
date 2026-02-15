package org.example.realtimenotify.controller;

import java.util.Map;
import org.example.realtimenotify.service.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  private final JwtService jwtService = new JwtService();

  // Dev-only: for local testing.
  @GetMapping("/auth/token")
  public Map<String, String> token(@RequestParam String username) {
    String token = jwtService.generateToken(username);
    return Map.of("token", token);
  }
}
