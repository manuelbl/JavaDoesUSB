//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.dfu;

/**
 * Page of flash memory, RAM or other type of memory.
 * <p>
 * If count is > 1, it represents a sector consisting of multiple equal pages.
 * </p>
 * @param segment the memory segment this page belongs to
 * @param startAddress start address
 * @param count number of pages
 * @param pageSize page size (in bytes)
 * @param attributes page attributes
 */
public record Page(Segment segment, int startAddress, int count, int pageSize, int attributes) {

    /**
     * Gets the end address of the page or sector.
     * @return the end address
     */
    public int endAddress() {
        return startAddress + count * pageSize;
    }

    /**
     * Indicates if the page or sector is readable.
     * @return {@code true} if it is readable
     */
    public boolean isReadable() {
        return (attributes & 1) != 0;
    }

    /**
     * Indicates if the page or sector is erasable.
     * @return {@code true} if it is erasable
     */
    public boolean isErasable() {
        return (attributes & 2) != 0;
    }

    /**
     * Indicates if the page or sector is writable.
     * @return {@code true} if it is writable
     */
    public boolean isWritable() {
        return (attributes & 4) != 0;
    }
}
