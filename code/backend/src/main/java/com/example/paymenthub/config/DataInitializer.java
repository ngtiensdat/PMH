package com.example.paymenthub.config;

import com.example.paymenthub.entity.Role;
import com.example.paymenthub.entity.User;
import com.example.paymenthub.repository.RoleRepository;
import com.example.paymenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role makerRole = roleRepository.findByRoleCode("ROLE_MAKER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleCode("ROLE_MAKER")
                        .roleName("Người lập đề xuất")
                        .description("Có quyền tạo mới, chỉnh sửa, gửi duyệt và hủy duyệt")
                        .build()));

        Role checkerRole = roleRepository.findByRoleCode("ROLE_CHECKER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleCode("ROLE_CHECKER")
                        .roleName("Người kiểm soát")
                        .description("Có quyền phê duyệt và từ chối đề xuất")
                        .build()));

        if (userRepository.count() == 0) {
            log.info("[DataInitializer] Khởi tạo 2 tài khoản mẫu vào CSDL...");

            User maker = User.builder()
                    .username("make")
                    .passwordHash(passwordEncoder.encode("123"))
                    .fullName("Maker User (make)")
                    .roles(Set.of(makerRole))
                    .failedLoginAttempts(0)
                    .build();

            User checker = User.builder()
                    .username("check")
                    .passwordHash(passwordEncoder.encode("123"))
                    .fullName("Checker User (check)")
                    .roles(Set.of(checkerRole))
                    .failedLoginAttempts(0)
                    .build();

            userRepository.save(maker);
            userRepository.save(checker);

            log.info("[DataInitializer] Đã tạo thành công 2 tài khoản mẫu: make, check.");
        } else {
            // Khởi tạo nếu tài khoản make/check chưa tồn tại
            if (userRepository.findByUsernameIgnoreCase("make").isEmpty()) {
                User maker = User.builder()
                        .username("make")
                        .passwordHash(passwordEncoder.encode("123"))
                        .fullName("Maker User (make)")
                        .roles(Set.of(makerRole))
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(maker);
                log.info("[DataInitializer] Đã khởi tạo mới tài khoản 'make'.");
            }

            if (userRepository.findByUsernameIgnoreCase("check").isEmpty()) {
                User checker = User.builder()
                        .username("check")
                        .passwordHash(passwordEncoder.encode("123"))
                        .fullName("Checker User (check)")
                        .roles(Set.of(checkerRole))
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(checker);
                log.info("[DataInitializer] Đã khởi tạo mới tài khoản 'check'.");
            }

            // Đảm bảo 2 tài khoản test mẫu 'make' và 'check' luôn được mở khóa và reset mật khẩu khi Restart Backend
            userRepository.findByUsernameIgnoreCase("make").ifPresent(user -> {
                user.setPasswordHash(passwordEncoder.encode("123"));
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
                log.info("[DataInitializer] Đã tự động mở khóa và reset tài khoản 'make'.");
            });

            userRepository.findByUsernameIgnoreCase("check").ifPresent(user -> {
                user.setPasswordHash(passwordEncoder.encode("123"));
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
                log.info("[DataInitializer] Đã tự động mở khóa và reset tài khoản 'check'.");
            });
        }
    }
}
