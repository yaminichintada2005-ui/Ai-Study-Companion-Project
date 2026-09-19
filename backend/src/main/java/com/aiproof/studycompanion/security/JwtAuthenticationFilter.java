package com.aiproof.studycompanion.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        System.out.println("======================================");
        System.out.println("JWT FILTER: " + request.getMethod() + " " + path);

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null) {

            System.out.println("JWT FILTER: NO Authorization header");

            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("JWT FILTER: Authorization header received");

        if (!authorizationHeader.startsWith("Bearer ")) {

            System.out.println("JWT FILTER: Invalid Authorization format");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {

            String email = jwtService.extractEmail(token);

            System.out.println(
                    "JWT FILTER: Token email = " + email
            );

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            System.out.println(
                    "JWT FILTER: User found = "
                    + userDetails.getUsername()
            );

            boolean valid =
                    jwtService.isTokenValid(token, userDetails);

            System.out.println(
                    "JWT FILTER: Token valid = " + valid
            );

            if (valid) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                System.out.println(
                        "JWT FILTER: AUTHENTICATION SUCCESSFUL"
                );

            } else {

                System.out.println(
                        "JWT FILTER: TOKEN INVALID"
                );
            }

        } catch (Exception exception) {

            System.out.println(
                    "JWT FILTER ERROR: "
                    + exception.getClass().getSimpleName()
                    + " - "
                    + exception.getMessage()
            );
        }

        filterChain.doFilter(request, response);
    }
}