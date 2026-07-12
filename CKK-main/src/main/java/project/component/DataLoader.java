package project.component;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import project.model.Role;
import project.model.User;
import project.repository.UserRepository;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            userRepo.save(User.builder()
                    .username("admin")
                    .password(encoder.encode("admin"))
                    .roles(Set.of(Role.ADMIN))
                    .fullName("admin")
                    .enabled(true)
                    .build());
            userRepo.save(User.builder()
                    .username("analyst")
                    .password(encoder.encode("analyst"))
                    .roles(Set.of(Role.ANALYST))
                    .fullName("analyst")
                    .enabled(true)
                    .build());
            userRepo.save(User.builder()
                    .username("tester")

                    .password(encoder.encode("tester"))
                    .roles(Set.of(Role.TESTER))
                            .fullName("tester")
                    .enabled(true)
                    .build());
        }
    }
}
