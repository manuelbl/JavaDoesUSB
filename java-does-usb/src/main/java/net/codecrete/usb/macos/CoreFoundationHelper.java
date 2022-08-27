//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.MemoryAddress.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;

/**
 * Core Foundation helper functions
 */
public class CoreFoundationHelper {

    /**
     * Gets Java string as a copy of the {@code CFStringRef}.
     *
     * @param string the string to copy ({@code CFStringRef})
     * @return copied string
     */
    public static String stringFromCFStringRef(MemoryAddress string) {

        try (var session = MemorySession.openConfined()) {

            long strLen = CoreFoundation.CFStringGetLength(string);
            var buffer = session.allocateArray(JAVA_CHAR, strLen);
            var range = session.allocate(CoreFoundation.CFRange);
            CoreFoundation.CFRange_location.set(range, 0);
            CoreFoundation.CFRange_range.set(range, strLen);
            CoreFoundation.CFStringGetCharacters(string, range, buffer);
            return new String(buffer.toArray(JAVA_CHAR));

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a {@code CFStringRef} with a copy of the specified string
     * <p>
     * Ownership follows the <i>Create Rule</i>, i.e. the caller takes
     * ownership without incrementing the reference count.
     * </p>
     *
     * @param string the string
     * @return {@code CFStringRef}
     */
    public static MemoryAddress createCFStringRef(String string) {
        try (var session = MemorySession.openConfined()) {
            char[] charArray = string.toCharArray();
            var chars = session.allocateArray(JAVA_CHAR, charArray.length);
            chars.copyFrom(MemorySegment.ofArray(charArray));
            return CoreFoundation.CFStringCreateWithCharacters(NULL, chars, string.length());
        }
    }
}
