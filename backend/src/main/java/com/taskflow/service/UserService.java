package com.taskflow.service;

import com.taskflow.dto.Dtos.*;
import com.taskflow.model.User;
import com.taskflow.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.taskflow.web.ApiExceptionHandler.badRequest;
import static com.taskflow.web.ApiExceptionHandler.notFound;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final FileStorage storage;

    private User load(Long id) {
        return users.findById(id).orElseThrow(() -> notFound("User"));
    }

    @Transactional(readOnly = true)
    public UserDto me(User me) {
        return UserDto.from(load(me.getId()));
    }

    public UserDto updateProfile(User me, ProfileUpdate r) {
        User u = load(me.getId());
        u.setFullName(r.fullName().trim());
        u.setSkills(r.skills() == null ? null : r.skills().trim());
        return UserDto.from(u);
    }

    public void changePassword(User me, PasswordChange r) {
        User u = load(me.getId());
        if (!encoder.matches(r.currentPassword(), u.getPassword())) throw badRequest("Current password is incorrect");
        u.setPassword(encoder.encode(r.newPassword()));
    }

    public UserDto uploadAvatar(User me, MultipartFile file) {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/"))
            throw badRequest("Avatar must be an image");
        User u = load(me.getId());
        storage.delete(u.getAvatarPath());
        u.setAvatarPath(storage.save(file));
        return UserDto.from(u);
    }

    @Transactional(readOnly = true)
    public Resource avatar(Long userId) {
        User u = load(userId);
        if (u.getAvatarPath() == null) throw notFound("Avatar");
        return storage.load(u.getAvatarPath());
    }

    // ---- admin ----
    @Transactional(readOnly = true)
    public List<UserDto> all() {
        return users.findAll(Sort.by("id")).stream().map(UserDto::from).toList();
    }

    public UserDto adminUpdate(User admin, Long id, AdminUserUpdate r) {
        User u = load(id);
        if (u.equals(admin) && (Boolean.FALSE.equals(r.active()) || (r.role() != null && r.role() != User.Role.ADMIN)))
            throw badRequest("You cannot deactivate or demote yourself");
        if (r.active() != null) u.setActive(r.active());
        if (r.role() != null) u.setRole(r.role());
        return UserDto.from(u);
    }
}
