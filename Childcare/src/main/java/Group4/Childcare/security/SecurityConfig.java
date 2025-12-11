package Group4.Childcare.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration//設定類別，Spring 啟動時會自動讀取這個類別，把裡面的 Bean（例如 PasswordEncoder、SecurityFilterChain）註��進容器。
@EnableWebSecurity//自動幫你接管所有 HTTP 請求，並根據這個類別的設定去判斷：哪些 API 要驗證 Token哪些可以匿名訪問要用哪種加密方式驗證密碼
public class SecurityConfig {

    // 暫時註解掉 JWT 過濾器
    // @Autowired
    //從每個請求的 Header（Authorization）取出 Token
    //驗證 Token 是否有效
    //如果有效，就把使用者資訊放入 Security Context（讓系統知道你是誰）
    // private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }//所有密碼都要用 BCrypt 演算法加密/比對

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())//不使用傳統的 Cookie 登入機制，關掉 CSRF 保護(跨站請求偽造防護)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))//設定成「無狀態模式（stateless）系統不會使用 Session 來記住誰登入過。 每個請求都必須帶上 JWT Token 才能驗證身分
            .authorizeHttpRequests(auth -> auth
                // 暫時讓所有請求都不需要驗證
                .anyRequest().permitAll()
                // .requestMatchers("/api/auth/**").permitAll()
                // .requestMatchers("/api/announcements/**").permitAll()
                // .requestMatchers("/api/email/**").permitAll()
                // .anyRequest().authenticated()
            );
            // 暫時註解掉 JWT 過濾器
            // .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
