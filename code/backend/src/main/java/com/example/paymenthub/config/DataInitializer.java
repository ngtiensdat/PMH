package com.example.paymenthub.config;

import com.example.paymenthub.entity.User;
import com.example.paymenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("[DataInitializer] Khởi tạo tài khoản mẫu vào CSDL...");

            User maker = User.builder()
                    .username("make")
                    .passwordHash(passwordEncoder.encode("123"))
                    .fullName("Maker User (make)")
                    .role("MAKER")
                    .failedLoginAttempts(0)
                    .build();

            User checker = User.builder()
                    .username("check")
                    .passwordHash(passwordEncoder.encode("123"))
                    .fullName("Checker User (check)")
                    .role("CHECKER")
                    .failedLoginAttempts(0)
                    .build();

            userRepository.save(maker);
            userRepository.save(checker);

            log.info("[DataInitializer] Đã tạo thành công 2 tài khoản mẫu: make, check.");
        } else {
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
        }
    }
}
