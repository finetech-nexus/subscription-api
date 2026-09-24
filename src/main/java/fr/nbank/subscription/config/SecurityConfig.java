package fr.nbank.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.disabled", havingValue = "false", matchIfMissing = true)
public class SecurityConfig {
  @Bean
  public SecurityFilterChain subscriptionSecurity(HttpSecurity http, JwtDecoder subscriptionJwtDecoder) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(subscriptionJwtDecoder)));
    return http.build();
  }

  @Bean
  public JwtDecoder subscriptionJwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
      @Value("${SUBSCRIPTION_JWKS_TLS_VERIFY:true}") boolean verifyTls) throws GeneralSecurityException {
    NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri);
    builder.jwsAlgorithms(algorithms -> {
      algorithms.add(SignatureAlgorithm.RS256);
      algorithms.add(SignatureAlgorithm.RS512);
    });
    if (!verifyTls) builder.restOperations(insecureJwksRestOperations());
    NimbusJwtDecoder decoder = builder.build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
    return decoder;
  }

  /** Only relax TLS checks for JWKS retrieval when explicitly enabled in a development environment. */
  private static RestOperations insecureJwksRestOperations() throws GeneralSecurityException {
    X509TrustManager trustAllCertificates = new X509TrustManager() {
      @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
      @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
      @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    };
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] { trustAllCertificates }, new SecureRandom());
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
      @Override
      protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        if (connection instanceof HttpsURLConnection httpsConnection) {
          httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
          httpsConnection.setHostnameVerifier((hostname, session) -> true);
        }
        super.prepareConnection(connection, httpMethod);
      }
    };
    return new RestTemplate(requestFactory);
  }
}
