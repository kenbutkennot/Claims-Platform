package com.example.claims.api;

import com.example.claims.application.ClaimDtos.UserDto;
import com.example.claims.domain.User;
import com.example.claims.infrastructure.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoUserController {
    private final UserRepository users;

    public DemoUserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return users.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(user -> new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole()))
                .toList();
    }
}
