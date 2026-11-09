package vn.devopsnotebook.learning.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ProblemDetail api(ApiException ex, HttpServletRequest request) { return problem(ex.status(), ex.getMessage(), request); }
  @ExceptionHandler(BadCredentialsException.class)
  ProblemDetail credentials(BadCredentialsException ex, HttpServletRequest request) { return problem(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu.", request); }
  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception ex, HttpServletRequest request) { return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi máy chủ. Hãy thử lại sau.", request); }
  private ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
    var result = ProblemDetail.forStatusAndDetail(status, detail);
    result.setProperty("path", request.getRequestURI());
    result.setProperty("timestamp", Instant.now());
    return result;
  }
  public static class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus status() { return status; }
  }
}
