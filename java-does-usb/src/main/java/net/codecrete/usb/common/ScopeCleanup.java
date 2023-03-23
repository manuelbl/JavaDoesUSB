//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import java.util.ArrayList;

/**
 * Auto closeable object for clean up actions.
 *
 * <p>
 * Multiple cleanup actions can be registered and will
 * be executed when this object is closed. They are run
 * in the reverse order they are registered.
 * </p>
 * <p>
 * Use with {@code try (...)} clause:
 * </p>
 * <pre>
 * try (var cleanup = new ScopeCleanup()) {
 *     var service = ...;
 *     cleanup.add(() -> releaseService(service));
 *
 *     ...more code...
 * }
 * </pre>
 */
public class ScopeCleanup implements AutoCloseable {

    private final ArrayList<Runnable> cleanupActions = new ArrayList<>();

    /**
     * Registers a cleanup action to be run later.
     *
     * @param cleanupAction cleanup action
     */
    public void add(Runnable cleanupAction) {
        cleanupActions.add(cleanupAction);
    }

    @Override
    public void close() {
        var size = cleanupActions.size();
        for (int i = size - 1; i >= 0; i--)
            cleanupActions.get(i).run();
    }
}
