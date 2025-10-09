package hr.algebra.server.controller;

import hr.algebra.server.dto.AuthRequest;
import hr.algebra.server.dto.AuthResponse;
import hr.algebra.server.dto.RefreshRequest;
import hr.algebra.server.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword())
            );

            String accessToken = jwtUtil.generateAccessToken(request.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(request.getUsername());

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Pogrešan username ili password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (jwtUtil.validateRefreshToken(refreshToken)) {
            String username = jwtUtil.extractUsernameFromRefreshToken(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(username);
            return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Neispravan refresh token");
        }
    }



}
