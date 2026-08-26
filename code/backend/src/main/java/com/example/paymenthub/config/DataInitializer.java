package com.example.paymenthub.config;

import com.example.paymenthub.entity.User;
import com.example.paymenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.paymenthub.entity.Role;
import com.example.paymenthub.repository.RoleRepository;
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
            log.info("[DataInitializer] Khởi tạo tài khoản mẫu vào CSDL...");

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

            User admin = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("123"))
                    .fullName("Admin User (admin)")
                    .roles(Set.of(makerRole, checkerRole))
                    .failedLoginAttempts(0)
                    .build();

            userRepository.save(maker);
            userRepository.save(checker);
            userRepository.save(admin);

            log.info("[DataInitializer] Đã tạo thành công 3 tài khoản mẫu: make, check, admin.");
        } else {
            // Đảm bảo tạo mới tài khoản admin nếu chưa tồn tại
            if (userRepository.findByUsernameIgnoreCase("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("123"))
                        .fullName("Admin User (admin)")
                        .roles(Set.of(makerRole, checkerRole))
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(admin);
                log.info("[DataInitializer] Đã khởi tạo mới tài khoản 'admin' sở hữu cả 2 quyền.");
            }

            // Đảm bảo các tài khoản test mẫu luôn được reset trạng thái mở khóa khi Restart Backend trong quá trình Dev
            userRepository.findByUsernameIgnoreCase("make").ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
                log.info("[DataInitializer] Đã tự động mở khóa tài khoản 'make'.");
            });

            userRepository.findByUsernameIgnoreCase("check").ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
                log.info("[DataInitializer] Đã tự động mở khóa tài khoản 'check'.");
            });

            userRepository.findByUsernameIgnoreCase("admin").ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
                log.info("[DataInitializer] Đã tự động mở khóa tài khoản 'admin'.");
            });
        }
    }
}
