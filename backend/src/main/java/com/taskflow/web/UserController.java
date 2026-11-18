package com.taskflow.web;

import com.taskflow.dto.Dtos.*;
import com.taskflow.model.User;
import com.taskflow.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Users")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @GetMapping("/users/me")
    public UserDto me(@AuthenticationPrincipal User me) {
        return users.me(me);
    }

    @PutMapping("/users/me")
    public UserDto update(@AuthenticationPrincipal User me, @Valid @RequestBody ProfileUpdate r) {
        return users.updateProfile(me, r);
    }

    @PutMapping("/users/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal User me, @Valid @RequestBody PasswordChange r) {
        users.changePassword(me, r);
    }

    @PostMapping("/users/me/avatar")
    public UserDto avatar(@AuthenticationPrincipal User me, @RequestParam MultipartFile file) {
        return users.uploadAvatar(me, file);
    }

    @GetMapping("/users/{id}/avatar")
    public ResponseEntity<Resource> avatar(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(users.avatar(id));
    }

    @GetMapping("/admin/users")
    public List<UserDto> all() {
        return users.all();
    }

    @PatchMapping("/admin/users/{id}")
    public UserDto adminUpdate(@AuthenticationPrincipal User me, @PathVariable Long id, @RequestBody AdminUserUpdate r) {
        return users.adminUpdate(me, id, r);
    }
}
