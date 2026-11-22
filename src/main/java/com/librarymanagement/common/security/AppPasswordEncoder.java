package com.librarymanagement.common.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AppPasswordEncoder implements PasswordEncoder {
    // OWASP Recommended Argon2id parameters:
    // saltLength: 16 bytes, hashLength: 32 bytes, parallelism: 1, memory: 65536 KiB (64 MB), iterations: 3
    private final Argon2PasswordEncoder delegate = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || rawPassword == null) {
            return false;
        }
        return delegate.matches(rawPassword, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }
}
