package iuh.fit.se.configs;

import iuh.fit.se.entities.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    CustomJwtDecoder customJwtDecoder;
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };
    private final String[] POST_PUBLIC_ENDPOINT = {
            "/auth-management/api/v1/auth/register",
            "/auth-management/api/v1/auth/log-in",
            "/auth-management/api/v1/auth/introspect",
            "/auth-management/api/v1/auth/refresh",
            "/auth-management/api/v1/auth/forget-password",
            "/auth-management/api/v1/auth/google",
            "/payments/vnpay/create",
            "/payments/vnpay/simulate",
            "/customer-management/api/v1/customers/register",
//            "/category-management/api/v1/categories/**",
            "/api/chat",
            "/gmail-management/api/v1/gmail/otp-forget-password",
            "/gmail-management/api/v1/gmail/verify-otp-forget-password"
    };

    private final String[] POST_CUSTOMER_ENDPOINT = {
            "/customer-management/api/v1/customers/update/**",
            "/gmail-management/api/v1/gmail/otp-forget-password"
    };

    private final String[] DASHBOARD_GET_ENDPOINT = {
            "/admin/api/v1/dashboard/**",
            "/admin/api/v1/statistics/**",
            "/admin/api/v1/orders",
            "/admin/api/v1/orders/**",
            "/admin/api/v1/pastries",
            "/admin/api/v1/pastries/**",
            "/admin/api/v1/customers",
            "/admin/api/v1/customers/**",
            "/customer-management/api/v1/customers",
            "/customer-management/api/v1/customers/**"
    };

    private final String[] GET_PUBLIC_ENDPOINT = {
            "/user-management/api/v1/users/information",
            "/pastry-management/api/v1/pastries/**",
            "/category-management/api/v1/categories/**",
            // Cho phép VNPay redirect về mà không cần JWT
            "/payments/vnpay/return",
            "/payments/vnpay/ipn",
    };

    private final String[] SALES_GET_ENDPOINT = {
            "/pastry-management/api/v1/pastries/**",
            "/category-management/api/v1/categories/**",
            "/admin/api/v1/pastries",
            "/admin/api/v1/pastries/**",
            "/admin/api/v1/orders",
            "/admin/api/v1/orders/**",
            "/admin/api/v1/qr-payments/generate/**",
    };
    private final String[] SALES_POST_ENDPOINT = {
            "/admin/api/v1/orders/create-counter-order",
    };
    private final String[] SALES_PUT_ENDPOINT = {
            "/admin/api/v1/orders/**",
            "/admin/api/v1/qr-payments/confirm/**",
    };
    private final String[] EMPLOYEE_ENDPOINT = {
            "/auth-management/api/v1/auth/**",
            "/gmail-management/api/v1/gmail/**",
            "/admin/api/v1/orders/**",
            "/admin/api/v1/pastries/**",
            "/customer-management/api/v1/customers/**",
    };
    private final String[] ADMIN_ENDPOINT = {
            "/auth-management/api/v1/auth/**",
            "/pastry-management/api/v1/pastries/**",
            "/user-management/api/v1/users/**",
            "/employee-management/api/v1/employees/**",
            "/gmail-management/api/v1/gmail/**",
            "/customer-management/api/v1/customers/**",
            "/admin/api/v1/employees/**",
            "/admin/api/v1/customers/**",
            "/admin/api/v1/pastries/**",
            "/admin/api/v1/categories/**",
            "/admin/api/v1/customers/**",
            "/admin/api/v1/orders/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // CORS
        httpSecurity.cors(cors -> cors.configurationSource(request -> {
            var corConfig = new CorsConfiguration();
            corConfig.addAllowedOrigin("http://localhost:5173");
            corConfig.addAllowedHeader("*");
            corConfig.addAllowedMethod("*");
            corConfig.setAllowCredentials(true);
            return corConfig;
        })).csrf(AbstractHttpConfigurer::disable);

        httpSecurity.authorizeHttpRequests(request -> {
            request.requestMatchers(HttpMethod.POST, POST_PUBLIC_ENDPOINT).permitAll()
                    .requestMatchers(HttpMethod.GET, GET_PUBLIC_ENDPOINT).permitAll()
                    .requestMatchers(HttpMethod.GET, DASHBOARD_GET_ENDPOINT)
                    .hasAnyRole(
                            UserRole.SALES.name(),
                            UserRole.IMPORT_STAFF.name(),
                            UserRole.INVENTORY_MANAGER.name(),
                            UserRole.ACCOUNTANT.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.GET, "/orders/*/qr-payment")
                    .hasAnyRole(
                            UserRole.CUSTOMER.name(),
                            UserRole.GUEST.name(),
                            UserRole.ADMIN.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.SALES.name(),
                            UserRole.IMPORT_STAFF.name(),
                            UserRole.INVENTORY_MANAGER.name(),
                            UserRole.ACCOUNTANT.name()
                    )
                    .requestMatchers(HttpMethod.GET, "/admin/api/v1/qr-payments/generate/**")
                    .hasAnyRole(
                            UserRole.CUSTOMER.name(),
                            UserRole.SALES.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers("/admin/api/v1/orders", "/admin/api/v1/orders/**")
                    .hasAnyRole(
                            UserRole.SALES.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers("/admin/api/v1/inventory-receipts", "/admin/api/v1/inventory-receipts/**")
                    .hasAnyRole(UserRole.IMPORT_STAFF.name(), UserRole.ADMIN.name())
                    .requestMatchers(HttpMethod.GET, "/admin/api/v1/stock-adjustments/low-stock")
                    .hasAnyRole(
                            UserRole.IMPORT_STAFF.name(),
                            UserRole.INVENTORY_MANAGER.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers("/admin/api/v1/stock-adjustments", "/admin/api/v1/stock-adjustments/**")
                    .hasAnyRole(UserRole.INVENTORY_MANAGER.name(), UserRole.ADMIN.name())
                    .requestMatchers("/admin/api/v1/accounting", "/admin/api/v1/accounting/**")
                    .hasAnyRole(UserRole.ACCOUNTANT.name(), UserRole.ADMIN.name())
                    .requestMatchers("/admin/api/v1/employee-performance", "/admin/api/v1/employee-performance/**")
                    .hasRole(UserRole.ADMIN.name())
                    .requestMatchers("/admin/api/v1/warehouse-notifications", "/admin/api/v1/warehouse-notifications/**")
                    .hasAnyRole(
                            UserRole.INVENTORY_MANAGER.name(),
                            UserRole.IMPORT_STAFF.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(
                            "/gmail-management/api/v1/gmail/send-otp",
                            "/gmail-management/api/v1/gmail/verify-otp"
                    )
                    .hasAnyRole(
                            UserRole.ADMIN.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.SALES.name(),
                            UserRole.IMPORT_STAFF.name(),
                            UserRole.INVENTORY_MANAGER.name(),
                            UserRole.ACCOUNTANT.name()
                    )
                    .requestMatchers(HttpMethod.GET, "/admin/api/v1/pastries", "/admin/api/v1/pastries/**")
                    .hasAnyRole(
                            UserRole.INVENTORY_MANAGER.name(),
                            UserRole.IMPORT_STAFF.name(),
                            UserRole.SALES.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.GET, SALES_GET_ENDPOINT).hasAnyRole(
                            UserRole.SALES.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.POST, SALES_POST_ENDPOINT).hasAnyRole(
                            UserRole.SALES.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.PUT, SALES_PUT_ENDPOINT).hasAnyRole(
                            UserRole.SALES.name(),
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.POST, POST_CUSTOMER_ENDPOINT).hasAnyRole(
                            UserRole.CUSTOMER.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.POST, EMPLOYEE_ENDPOINT).hasAnyRole(
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.GET, EMPLOYEE_ENDPOINT).hasAnyRole(
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.PUT, EMPLOYEE_ENDPOINT).hasAnyRole(
                            UserRole.EMPLOYEE.name(),
                            UserRole.ADMIN.name()
                    )
                    .requestMatchers(HttpMethod.GET, ADMIN_ENDPOINT).hasRole(UserRole.ADMIN.name())
                    .requestMatchers(HttpMethod.PUT, ADMIN_ENDPOINT).hasRole(UserRole.ADMIN.name())
                    .requestMatchers(HttpMethod.POST, ADMIN_ENDPOINT).hasRole(UserRole.ADMIN.name())
                    .requestMatchers(SWAGGER_WHITELIST).permitAll()
                    .anyRequest().authenticated();
        }).oauth2ResourceServer(oauth2 -> {
            oauth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(customJwtDecoder)
                            .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(new JwtAuthenticationEntrypoint())
                    .accessDeniedHandler(new CustomAccessDeniedHandler());
        });

        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
