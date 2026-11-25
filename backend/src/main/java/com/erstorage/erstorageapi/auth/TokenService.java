package com.erstorage.erstorageapi.auth;

import com.erstorage.erstorageapi.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public IssuedToken createToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.expiration());
        if (user.getExpiresAt() != null && user.getExpiresAt().isBefore(expiresAt)) {
            expiresAt = user.getExpiresAt();
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("erstorage")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedToken(value, Duration.between(now, expiresAt).toSeconds());
    }
}