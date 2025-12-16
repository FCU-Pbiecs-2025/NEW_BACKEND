package Group4.Childcare.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // API 1 & 2: 會員中心家長與幼兒資料 (僅需登入)
                //.requestMatchers("/users/users-familyInfo/**").authenticated()

                // API 3 & 5: 更新審核申請狀態與個案管理狀態 (需登入，角色由 @PreAuthorize 控制)
//                .requestMatchers("/application-participants/**").authenticated()

                // API 4.1: 更新撤銷確認日期 (需登入，角色由 @PreAuthorize 控制)
//                .requestMatchers("/revoke/confirm-date").authenticated()

                // API 6: 執行候補抽籤 (需登入，角色由 @PreAuthorize 控制)
//                .requestMatchers("/waitlist/lottery").authenticated()

                // 其餘所有請求: 不需要 JWT
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

