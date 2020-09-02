package org.mickael.librarymsreservation.security;

import com.auth0.jwt.JWT;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;


public class JwtAuthorizationFilter extends BasicAuthenticationFilter {


    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Read the Authorization header, where the JWT token should be
        String header = request.getHeader(JwtProperties.HEADER_STRING);

        // If header does not contain BEARER or is null delegate to Spring impl and exit
        if (header == null || !header.startsWith(JwtProperties.TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // If header is present, try grab user principal from database and perform authorization
        Authentication authentication = getUsernamePasswordAuthentication(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Continue filter execution
        chain.doFilter(request, response);
    }

    private Authentication getUsernamePasswordAuthentication(HttpServletRequest request) {


        String token = request.getHeader(JwtProperties.HEADER_STRING)
                               .replace(JwtProperties.TOKEN_PREFIX,"");

        if (token != null) {
            // parse the token and validate it
            String username = JWT.require(HMAC512(JwtProperties.SECRET.getBytes()))
                                      .build()
                                      .verify(token)
                                      .getSubject();

            // Search in the DB if we find the user by token subject (username)
            // If so, then grab user details and create spring auth token using username, pass, authorities/roles
            if (username != null) {
                String authoritiesJwt = JWT.require(HMAC512(JwtProperties.SECRET.getBytes()))
                                                   .build()
                                                   .verify(token)
                                            .getClaim("authorities").toString();
                List<String> authoritiesList = Arrays.asList(authoritiesJwt.split(","));
                List<GrantedAuthority> authorities = new ArrayList<>();
                authoritiesList.forEach(p -> {
                    GrantedAuthority authority = new SimpleGrantedAuthority(p);
                    authorities.add(authority);
                });

                return new UsernamePasswordAuthenticationToken(username, null, authorities);
            }
            return null;
        }
        return null;
    }
}
