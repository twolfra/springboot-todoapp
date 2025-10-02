package com.example.todoapp;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

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
            user.setRoles("ROLE_ADMIN");
        } else {
            user.setRoles("ROLE_USER");
        }

        User saved = userRepo.save(user);

        return ResponseEntity.ok(
                new AuthResponseDTO(
                        "User registered successfully",
                        new UserDTO(saved.getUsername(), Set.of(saved.getRoles())) // wrap single string into Set
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody User req) {
        return userRepo.findByUsername(req.getUsername())
                .filter(u -> encoder.matches(req.getPassword(), u.getPassword()))
                .map(u -> {
                    // generate token with single role
                    String token = jwtUtil.generateToken(u.getUsername(), Set.of(u.getRoles()));

                    ResponseCookie cookie = ResponseCookie.from("JWT", token)
                            .httpOnly(true)
                            .secure(true) // ⚠️ set to true in production with HTTPS
                            .path("/")
                            .sameSite("None") // set none to enable different localhost ports
                            .maxAge(Duration.ofHours(4))
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(new AuthResponseDTO(
                                    "Login successful",
                                    new UserDTO(u.getUsername(), Set.of(u.getRoles()))
                            ));
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
    // endpoint for returning current user
    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("Not logged in");
        }

        return userRepo.findByUsername(principal.getUsername())
                .map(u -> ResponseEntity.ok(
                        new UserDTO(
                                u.getUsername(),
                                Set.of(u.getRoles()) // wrap single string into Set
                        )
                ))
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }


}
