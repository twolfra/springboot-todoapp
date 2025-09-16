package com.example.todoapp;

import com.example.todoapp.dto.UserDTO;
import com.example.todoapp.dto.AuthResponseDTO;
import com.example.todoapp.dto.LogoutResponseDTO;
import com.example.todoapp.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody User user,
                                                    @RequestParam(defaultValue = "user") String role) {
        user.setPassword(encoder.encode(user.getPassword()));
        if ("admin".equalsIgnoreCase(role)) {
            user.setRoles(Set.of("ROLE_ADMIN"));
        } else {
            user.setRoles(Set.of("ROLE_USER"));
        }
        User saved = userRepo.save(user);

        return ResponseEntity.ok(
                new AuthResponseDTO("User registered successfully",
                        new UserDTO(saved.getUsername(), saved.getRoles()))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody User req) {
        return userRepo.findByUsername(req.getUsername())
                .filter(u -> encoder.matches(req.getPassword(), u.getPassword()))
                .map(u -> {
                    String token = jwtUtil.generateToken(u.getUsername(), u.getRoles());

                    ResponseCookie cookie = ResponseCookie.from("JWT", token)
                            .httpOnly(true)
                            .secure(false) // set true in production with HTTPS
                            .path("/")
                            .sameSite("Lax")
                            .maxAge(Duration.ofHours(4))
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(new AuthResponseDTO("Login successful",
                                    new UserDTO(u.getUsername(), u.getRoles())));
                })
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDTO> logout() {
        ResponseCookie deleteCookie = ResponseCookie.from("JWT", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new LogoutResponseDTO("Logged out"));
    }
}
