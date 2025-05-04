package ru.practicum.backend

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.stream.Collectors


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.oauth2ResourceServer { oauth2 ->
            oauth2.jwt { jwt ->
                jwt.jwtAuthenticationConverter(
                    jwtAuthenticationConverter()
                )
            }
        }
        http.cors { cors: CorsConfigurer<HttpSecurity?> -> cors.configurationSource(corsConfigurationSource()) } // Enable CORS
            .authorizeHttpRequests(Customizer { authorize ->
                authorize
                    .requestMatchers("/report").hasRole("prothetic_user")
                    .anyRequest().permitAll()
            }
            )
        .csrf().disable();
        return http.build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        jwtAuthenticationConverter.setPrincipalClaimName("preferred_username")
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
            val roles = jwt.getClaimAsMap("realm_access").get("roles") as List<String>
            val authorities: Collection<GrantedAuthority> = roles.stream()
                .map { role: String? -> SimpleGrantedAuthority("ROLE_$role") }
                .collect(Collectors.toList())
            authorities
        }
        return jwtAuthenticationConverter
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = mutableListOf("http://localhost:3000")
        configuration.allowedMethods = mutableListOf("GET", "POST", "OPTIONS")
        configuration.allowedHeaders = mutableListOf("Authorization", "Content-Type")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val jwkSetUri = "http://keycloak:8080/realms/reports-realm/protocol/openid-connect/certs"
        val decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()

        val customIssuerValidator = OAuth2TokenValidator<Jwt> { jwt ->
            log.info("Validating issuer = ${jwt.issuer}")
            if (jwt.issuer.toString() == "http://localhost:8080/realms/reports-realm") {
                log.info("jwt.issuer.toString() == http://localhost:8080/realms/reports-realm")
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error(
                        "invalid_issuer",
                        "The Issuer \"${jwt.issuer}\" does not match the expected issuer",
                        null
                    )
                )
            }
        }

        // Combine the custom issuer validator with default validators
        val validators = listOf(
            customIssuerValidator,
        )
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(validators))

        return decoder
    }

    companion object {
        private val log = LoggerFactory.getLogger(SecurityConfig::class.java)
    }
}