//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDevice;
import net.codecrete.usb.USBException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Base class for USB device registry.
 * <p>
 * This singleton class maintains a list of connected USB devices.
 * It starts a background thread monitoring the USB devices being
 * connected and disconnected.
 * </p>
 * <p>
 * The background thread enumerates the already present devices
 * and builds the initial device list.
 * </p>
 */
public abstract class USBDeviceRegistry {

    private volatile List<USBDevice> devices;
    private volatile Throwable failureCause;
    protected Consumer<USBDevice> onDeviceConnectedHandler;
    protected Consumer<USBDevice> onDeviceDisconnectedHandler;

    private final Lock lock = new ReentrantLock();
    private final Condition enumerationComplete = lock.newCondition();

    /**
     * Start this device registry.
     * <p>
     * This method blocks until the initial device enumeration has
     * completed.
     * </p>
     */
    public void start() {
        startDeviceMonitor(this::monitorDevices);
    }

    /**
     * Enumerate the already present devices and then start monitoring
     * devices being connected and disconnected.
     * <p>
     * This function is run in a background thread.
     * </p>
     * <p>
     * Implementors of this method are expected to call {@link #setInitialDeviceList(List)}
     * after the initial device implementation.
     * </p>
     */
    protected abstract void monitorDevices();

    /**
     * Gets the list of the currently connected USB devices.
     *
     * @return list of devices
     */
    public List<USBDevice> getAllDevices() {
        return Collections.unmodifiableList(devices);
    }

    public void setOnDeviceConnected(Consumer<USBDevice> handler) {
        onDeviceConnectedHandler = handler;
    }

    public void setOnDeviceDisconnected(Consumer<USBDevice> handler) {
        onDeviceDisconnectedHandler = handler;
    }

    protected void emitOnDeviceConnected(USBDevice device) {
        if (onDeviceConnectedHandler != null) onDeviceConnectedHandler.accept(device);
    }

    protected void emitOnDeviceDisconnected(USBDevice device) {
        if (onDeviceDisconnectedHandler != null) onDeviceDisconnectedHandler.accept(device);
    }

    /**
     * Starts the background thread and waits until the first device enumeration is complete.
     * <p>
     * In order to signal that the initial enumeration is complete, the monitor task is expected
     * to call {@link #setInitialDeviceList(List)}.
     * </p>
     *
     * @param monitorTask the task to start in the background
     */
    protected void startDeviceMonitor(Runnable monitorTask) {
        // start new thread
        Thread t = new Thread(monitorTask, "USB device monitor");
        t.setDaemon(true);
        t.start();

        // wait for initial device enumeration
        lock.lock();
        try {
            while (devices == null && failureCause == null) {
                enumerationComplete.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }

        if (failureCause != null)
            throw new USBException("Initial device enumeration has failed", failureCause);
    }

    /**
     * Signals completion of initial device enumeration.
     */
    private void signalEnumerationComplete() {
        lock.lock();
        try {
            enumerationComplete.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Signal failure of initial device enumeration.
     *
     * @param e cause of failure
     */
    protected void enumerationFailed(Throwable e) {
        failureCause = e;
        signalEnumerationComplete();
    }

    /**
     * Sets the device list of the initial device enumeration.
     * <p>
     * This function signals to the spawning thread that the enumeration is complete.
     * </p>
     *
     * @param deviceList the device list
     */
    protected void setInitialDeviceList(List<USBDevice> deviceList) {
        devices = deviceList;
        signalEnumerationComplete();
    }

    /**
     * Adds a device to the list of connected USB devices.
     *
     * @param device device to add
     */
    protected void addDevice(USBDevice device) {
        // check for duplicates
        if (findDeviceIndex(devices, ((USBDeviceImpl) device).getUniqueId()) >= 0) return;

        // copy list
        var newDeviceList = new ArrayList<USBDevice>(devices.size() + 1);
        newDeviceList.addAll(devices);
        newDeviceList.add(device);
        devices = newDeviceList;

        // send notification
        emitOnDeviceConnected(device);
    }

    /**
     * Removes a device from the list of connected USB devices.
     *
     * @param deviceId the unique ID of the device to remove
     */
    protected void removeDevice(Object deviceId) {
        // locate device to be removed
        int index = findDeviceIndex(devices, deviceId);
        if (index < 0)
            return; // strange

        // copy list and remove device
        var device = devices.get(index);
        var newDeviceList = new ArrayList<>(devices);
        newDeviceList.remove(index);
        devices = newDeviceList;

        // send notification
        emitOnDeviceDisconnected(device);
    }

    /**
     * Finds the index of the device with the given ID in the device list
     *
     * @param deviceList the device list
     * @param deviceId   the unique device ID
     * @return return the index, or -1 if the device is not found
     */
    protected int findDeviceIndex(List<USBDevice> deviceList, Object deviceId) {
        for (int i = 0; i < deviceList.size(); i++) {
            var dev = (USBDeviceImpl) deviceList.get(i);
            if (deviceId.equals(dev.getUniqueId())) return i;
        }
        return -1;
    }
    /**
     * Finds the device with the given ID in the device list
     *
     * @param deviceId   the unique device ID
     * @return return device, or {@code null} if not found.
     */
    protected USBDevice findDevice(Object deviceId) {
        int index = findDeviceIndex(devices, deviceId);
        if (index < 0)
            return null;
        return devices.get(index);
    }
}
