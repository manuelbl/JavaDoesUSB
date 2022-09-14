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

    public Version(int bcdVersion) {
        this.bcdVersion = bcdVersion;
    }

    public int major() {
        return bcdVersion >> 8;
    }

    public int minor() {
        return (bcdVersion >> 4) & 0x0f;
    }

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
        Version version = (Version) o;
        return bcdVersion == version.bcdVersion;
    }

    @Override
    public int hashCode() {
        return bcdVersion;
    }
}
