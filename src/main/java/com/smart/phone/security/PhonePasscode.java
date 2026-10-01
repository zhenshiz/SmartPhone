package com.smart.phone.security;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/** 仅服务端保存的加盐密码摘要，不放入同步给客户端的 PhoneInfo。 */
public final class PhonePasscode implements IPersistedSerializable {
    @Persisted private byte[] salt = new byte[0];
    @Persisted private byte[] hash = new byte[0];

    public static boolean valid(String pin) {
        return pin != null && pin.matches("[0-9]{6}");
    }

    public static PhonePasscode create(String pin) {
        if (!valid(pin)) throw new IllegalArgumentException("Expected six digits");
        var result = new PhonePasscode();
        result.salt = new byte[16];
        new SecureRandom().nextBytes(result.salt);
        result.hash = result.derive(pin);
        return result;
    }

    public boolean matches(String pin) {
        if (!valid(pin) || salt.length != 16 || hash.length != 32) return false;
        byte[] candidate = derive(pin);
        try { return MessageDigest.isEqual(hash, candidate); }
        finally { Arrays.fill(candidate, (byte) 0); }
    }

    private byte[] derive(String pin) {
        var spec = new PBEKeySpec(pin.toCharArray(), salt, 60_000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException exception) { throw new IllegalStateException(exception); }
        finally { spec.clearPassword(); }
    }
}
