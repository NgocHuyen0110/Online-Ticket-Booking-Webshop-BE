package nl.fontys.s3.museumticket.business.impl;

import nl.fontys.s3.museumticket.business.AccessTokenDecoder;
import nl.fontys.s3.museumticket.business.AccessTokenEncoder;
import nl.fontys.s3.museumticket.business.exception.InvalidAccessTokenException;
import nl.fontys.s3.museumticket.domain.AccessToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccessTokenEncoderDecoderImpl implements AccessTokenEncoder, AccessTokenDecoder {
    private final Key key;

    public AccessTokenEncoderDecoderImpl(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String encode(AccessToken accessToken) {
        Map<String, Object> claimsMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(accessToken.getRoles())) {
            claimsMap.put("roles", accessToken.getRoles());
        }
        if (accessToken.getCustomerId() != null) {
            claimsMap.put("customerId", accessToken.getCustomerId());
        }
        if (accessToken.getAdminId() != null) {
            claimsMap.put("adminId", accessToken.getAdminId());
        }

        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(accessToken.getSubject())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(30, ChronoUnit.MINUTES)))
                .addClaims(claimsMap)
                .signWith(key)
                .compact();
    }

    @Override
    public AccessToken decode(String accessTokenEncoded) {
        try {
            Jwt jwt = Jwts.parserBuilder().setSigningKey(key).build().parse(accessTokenEncoded);
            Claims claims = (Claims) jwt.getBody();

            List<String> roles = claims.get("roles", List.class);

            return AccessToken.builder()
                    .subject(claims.getSubject())
                    .roles(roles)
                    .customerId(claims.get("customerId", Long.class))
                    .adminId(claims.get("adminId", Long.class))
                    .build();
        } catch (JwtException e) {
            throw new InvalidAccessTokenException(e.getMessage());
        }
    }
}