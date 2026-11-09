package vn.devopsnotebook.learning.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthenticationManager authenticationManager;
  public AuthController(AuthenticationManager authenticationManager) { this.authenticationManager = authenticationManager; }

  @GetMapping("/csrf") Map<String, String> csrf(CsrfToken token) { return Map.of("headerName", token.getHeaderName(), "token", token.getToken()); }
  @GetMapping("/session") Map<String, Object> session(org.springframework.security.core.Authentication auth) {
    return auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()) ? Map.of("authenticated", false) : Map.of("authenticated", true, "username", auth.getName());
  }
  @PostMapping("/login") Map<String, Object> login(@RequestBody LoginRequest input, HttpServletRequest request) {
    var auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password()));
    request.getSession(true);
    request.changeSessionId();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    request.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", context);
    return Map.of("authenticated", true, "username", auth.getName());
  }
  @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) void logout(HttpServletRequest request, HttpServletResponse response) {
    var session = request.getSession(false); if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
  }
  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}
