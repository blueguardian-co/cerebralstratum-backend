package co.blueguardian.cerebralstratum.utils.uuid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public class UUIDv5Generator {

    public static void main(String[] args) {
        // DNS namespace UUID
        String namespaceDNS = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

        // Name for which UUID needs to be generated
        String name = "cerebral-stratum.blueguardian.co";

        String nameWithTimestamp = name + ":" + System.currentTimeMillis();
        System.out.println("Name with timestamp: " + nameWithTimestamp);

        UUID uuid = generateUUIDv5(UUID.fromString(namespaceDNS), nameWithTimestamp);

        // Print the generated UUID
        System.out.println("Generated v5 UUID: " + uuid);
    }

    public static UUID generateUUIDv5(UUID namespace, String name) {
        try {
            // Create a SHA-1 digest
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

            // Combine namespace and name
            sha1.update(toBytes(namespace));
            sha1.update(name.getBytes(StandardCharsets.UTF_8));

            // Compute hash
            byte[] hash = sha1.digest();

            // Set the version and variant for UUIDv5
            hash[6] &= 0x0f; // clear version
            hash[6] |= 0x50; // set to version 5
            hash[8] &= 0x3f; // clear variant
            hash[8] |= 0x80; // set to IETF variant

            // Convert hash to UUID
            return toUUID(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate UUIDv5", e);
        }
    }

    private static byte[] toBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) ((msb >>> 8 * (7 - i)) & 0xFF);
            bytes[8 + i] = (byte) ((lsb >>> 8 * (7 - i)) & 0xFF);
        }
        return bytes;
    }

    private static UUID toUUID(byte[] hash) {
        long msb = 0;
        long lsb = 0;

        // Generate most significant bits
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (hash[i] & 0xff);
        }

        // Generate least significant bits
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (hash[i] & 0xff);
        }

        return new UUID(msb, lsb);
    }
}