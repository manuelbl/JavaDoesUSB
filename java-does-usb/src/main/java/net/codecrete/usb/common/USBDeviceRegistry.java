//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.common;

import net.codecrete.usb.USBDeviceInfo;

import java.util.ArrayList;
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

    protected void addDevice(USBDeviceInfo device) {
        // check for duplicates
        if (findDeviceIndex(devices, ((USBDeviceInfoImpl)device).getUniqueId()) >= 0)
            return;

        // copy list
        var newDeviceList = new ArrayList<USBDeviceInfo>(devices.size() + 1);
        newDeviceList.addAll(devices);
        newDeviceList.add(device);
        devices = newDeviceList;

        // send notification
        emitOnDeviceConnected(device);
    }

    protected void removeDevice(Object deviceId) {
        // locate device to be removed
        int index = findDeviceIndex(devices, deviceId);
        if (index < 0)
            return; // strange

        // copy list and remove device
        var deviceInfo = devices.get(index);
        var newDeviceList = new ArrayList<>(devices);
        newDeviceList.remove(index);
        devices = newDeviceList;

        // send notification
        emitOnDeviceDisconnected(deviceInfo);
    }

    /**
     * Finds the index of the device with the given ID in the device list
     * @param deviceList the device list
     * @param deviceId the unique device ID
     * @return return the index, or -1 if the device is not found
     */
    protected int findDeviceIndex(List<USBDeviceInfo> deviceList, Object deviceId) {
        for (int i = 0; i < deviceList.size(); i++) {
            var dev = (USBDeviceInfoImpl) deviceList.get(i);
            if (deviceId.equals(dev.getUniqueId()))
                return i;
        }
        return -1;
    }
}
