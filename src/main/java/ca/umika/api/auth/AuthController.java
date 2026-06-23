package ca.umika.api.auth;

import ca.umika.api.user.UserDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "authLogin")
public class AuthController {

    private final AuthService authService;


    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginDto loginDto) {
        LoginResponse response = authService.login(loginDto);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody UserDto userDto) {
        LoginResponse response = authService.register(userDto);
        return ResponseEntity.ok(response);
    }
}
