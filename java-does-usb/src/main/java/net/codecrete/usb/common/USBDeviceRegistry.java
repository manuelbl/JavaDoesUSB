//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDeviceInfo;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * USB device registry.
 */
public abstract class USBDeviceRegistry {

    protected volatile List<USBDeviceInfo> devices;
    protected Consumer<USBDeviceInfo> onDeviceConnectedHandler;
    protected Consumer<USBDeviceInfo> onDeviceDisconnectedHandler;

    private final Lock lock = new ReentrantLock();
    private final Condition enumerationComplete = lock.newCondition();

    public abstract List<USBDeviceInfo> getAllDevices();

    public void setOnDeviceConnected(Consumer<USBDeviceInfo> handler) {
        onDeviceConnectedHandler = handler;
    }

    public void setOnDeviceDisconnected(Consumer<USBDeviceInfo> handler) {
        onDeviceDisconnectedHandler = handler;
    }

    protected void emitOnDeviceConnected(USBDeviceInfo device) {
        if (onDeviceConnectedHandler != null)
            onDeviceConnectedHandler.accept(device);
    }

    protected void emitOnDeviceDisconnected(USBDeviceInfo device) {
        if (onDeviceDisconnectedHandler != null)
            onDeviceDisconnectedHandler.accept(device);
    }

    /**
     * Starts the background thread and waits until the first device enumeration is complete.
     */
    protected void startDeviceMonitor(Runnable monitorTask) {
        // start new thread
        Thread t = new Thread(monitorTask, "USB device monitor");
        t.setDaemon(true);
        t.start();

        // wait for initial device enumeration
        lock.lock();
        try {
            while (devices == null) {
                enumerationComplete.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Signals completion of initial device enumeration.
     */
    protected void signalEnumerationComplete() {
        lock.lock();
        try {
            enumerationComplete.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
