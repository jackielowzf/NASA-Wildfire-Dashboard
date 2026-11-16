
package wildfire;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/sync")
            )
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET,
                    "/",
                    "/index.html",
                    "/assets/**",
                    "/favicon.ico",
                    "/vite.svg",
                    "/api/fires",
                    "/api/harmonized",
                    "/api/history"
                ).permitAll()

                .requestMatchers(HttpMethod.POST,
                    "/api/sync"
                ).permitAll()

                .anyRequest().denyAll()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
