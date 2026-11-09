package vn.devopsnotebook.learning.auth;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.devopsnotebook.learning.config.AppProperties;

@Service
public class AccountService implements UserDetailsService, ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final AppProperties properties;
  private final PasswordEncoder encoder;
  public AccountService(JdbcTemplate jdbc, AppProperties properties, PasswordEncoder encoder) { this.jdbc = jdbc; this.properties = properties; this.encoder = encoder; }

  @Override public UserDetails loadUserByUsername(String username) {
    var rows = jdbc.queryForList("select username, password_hash, enabled from app_user where username = ?", username);
    if (rows.isEmpty()) throw new UsernameNotFoundException("Tài khoản không tồn tại.");
    Map<String, Object> row = rows.getFirst();
    return new User((String) row.get("username"), (String) row.get("password_hash"), (Boolean) row.get("enabled"), true, true, true, AuthorityUtils.NO_AUTHORITIES);
  }

  @Override @Transactional public void run(ApplicationArguments args) {
    var bootstrap = properties.bootstrap();
    if (bootstrap == null || blank(bootstrap.username()) || blank(bootstrap.password())) {
      throw new IllegalStateException("APP_BOOTSTRAP_USERNAME và APP_BOOTSTRAP_PASSWORD phải được đặt trước khi chạy API.");
    }
    Integer count = jdbc.queryForObject("select count(*) from app_user", Integer.class);
    if (count != null && count == 0) {
      jdbc.update("insert into app_user (id, username, password_hash, enabled) values (1, ?, ?, true)", bootstrap.username(), encoder.encode(bootstrap.password()));
      jdbc.update("insert into learner_state (user_id, revision, reading_size) values (1, 0, 'normal')");
    }
  }
  private boolean blank(String value) { return value == null || value.isBlank(); }
}
