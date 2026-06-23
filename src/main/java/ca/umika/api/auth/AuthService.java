package ca.umika.api.auth;

import ca.umika.api.user.UserDto;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserMapper;
import ca.umika.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
    public LoginResponse register(UserDto userDto) {

        if (userRepository.findByEmail(userDto.email()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        UserEntity user;
        UserMapper userMapper = new UserMapper();
        user = userMapper.toEntity(userDto);
        user.setId(null);
        // Use the SAME injected PasswordEncoder
        user.setPasswordHash(
                passwordEncoder.encode(userDto.passwordHash())
        );

        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(token);
    }
}
