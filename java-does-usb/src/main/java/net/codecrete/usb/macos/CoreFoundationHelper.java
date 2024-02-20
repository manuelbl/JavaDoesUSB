//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.macos.gen.corefoundation.CFRange;
import net.codecrete.usb.macos.gen.corefoundation.CoreFoundation;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;

/**
 * Core Foundation helper functions
 */
class CoreFoundationHelper {

    private CoreFoundationHelper() {
    }

    /**
     * Gets Java string as a copy of the {@code CFStringRef}.
     *
     * @param string the string to copy ({@code CFStringRef})
     * @param arena  the arena to allocate memory
     * @return copied string
     */
    static String stringFromCFStringRef(MemorySegment string, Arena arena) {

        var strLen = CoreFoundation.CFStringGetLength(string);
        var buffer = arena.allocate(JAVA_CHAR, strLen);
        var range = CFRange.allocate(arena);
        CFRange.location(range, 0);
        CFRange.length(range, strLen);
        CoreFoundation.CFStringGetCharacters(string, range, buffer);
        return new String(buffer.toArray(JAVA_CHAR));
    }

    /**
     * Creates a {@code CFStringRef} with a copy of the specified string
     * <p>
     * Ownership follows the <i>Create Rule</i>, i.e. the caller takes
     * ownership without incrementing the reference count.
     * </p>
     *
     * @param string    the string
     * @param allocator the allocator for allocating memory
     * @return {@code CFStringRef}
     */
    static MemorySegment createCFStringRef(String string, SegmentAllocator allocator) {
        var charArray = string.toCharArray();
        var chars = allocator.allocate(JAVA_CHAR, charArray.length);
        chars.copyFrom(MemorySegment.ofArray(charArray));
        return CoreFoundation.CFStringCreateWithCharacters(NULL, chars, string.length());
    }
}
