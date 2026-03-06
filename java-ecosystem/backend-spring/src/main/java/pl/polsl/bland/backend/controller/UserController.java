package pl.polsl.bland.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.bland.backend.entity.UserEntity;
import pl.polsl.bland.backend.repository.UserRepository;
import pl.polsl.bland.models.UserDto;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username, email and password are required"));
        }

        // TODO: wymienić na normalne haszowanie (BCrypt)
        UserEntity user = new UserEntity(username, email, password);
        UserEntity saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserDto(saved.getId(), saved.getUsername(), saved.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username and password are required"));
        }

        return userRepository.findByUsername(username)
                .filter(user -> user.getPasswordHash().equals(password)) // TODO: BCrypt
                .map(user -> ResponseEntity.ok(
                        (Object) new UserDto(user.getId(), user.getUsername(), user.getEmail())))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(
                        (Object) new UserDto(user.getId(), user.getUsername(), user.getEmail())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User with id=" + id + " not found")));
    }
}
