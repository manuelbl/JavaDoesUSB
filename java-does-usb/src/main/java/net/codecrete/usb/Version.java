//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * Semantic version number.
 */
public final class Version {

    private final int bcdVersion;

    /**
     * Creates a new instance.
     * <p>
     * {@code bcdVersion} contains the version: the high byte is the major
     * version. The low byte is split into two nibbles (4 bits), the high one
     * is minor version, the low one is the subminor version. As an example,
     * 0x0321 represents the version 3.2.1.
     * </p>
     *
     * @param bcdVersion version, encoded as described above
     */
    public Version(int bcdVersion) {
        this.bcdVersion = bcdVersion;
    }

    /**
     * Major version
     *
     * @return major version
     */
    public int major() {
        return bcdVersion >> 8;
    }

    /**
     * Minor version
     *
     * @return minor version
     */
    public int minor() {
        return (bcdVersion >> 4) & 0x0f;
    }

    /**
     * Subminor version
     *
     * @return subminor version
     */
    public int subminor() {
        return bcdVersion & 0x0f;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major(), minor(), subminor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var version = (Version) o;
        return bcdVersion == version.bcdVersion;
    }

    @Override
    public int hashCode() {
        return bcdVersion;
    }
}
