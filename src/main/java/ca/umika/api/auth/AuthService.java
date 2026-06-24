package ca.umika.api.auth;

import ca.umika.api.user.UserDto;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserWriteRequest;
import ca.umika.api.user.UserRepository;
import ca.umika.api.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    public LoginResponse login(LoginDto loginDto) {
        UserEntity user = userRepository.findByEmail(loginDto.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(loginDto.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(token);
    }

    public LoginResponse register(UserWriteRequest userRequest) {
        if (userRepository.findByEmail(userRequest.email()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        UserDto user = userService.create(userRequest);
        String token = jwtUtil.generateToken(user.email());
        return new LoginResponse(token);
    }
}
