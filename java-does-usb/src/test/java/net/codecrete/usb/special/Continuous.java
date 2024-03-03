package net.codecrete.usb.special;

import net.codecrete.usb.Usb;
import net.codecrete.usb.UsbDevice;
import net.codecrete.usb.UsbException;

import java.io.IOException;
import java.util.Random;

public class Continuous {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws IOException {
        var device = Usb.findDevice(0xcafe, 0xceaf)
                .or(() -> Usb.findDevice(0xcafe, 0xcea0))
                .orElseThrow(() -> new IllegalStateException("No test device connected"));
        var interfaceNumber = device.getProductId() == 0xceaf ? 0 : 3;

        device.open();
        device.claimInterface(interfaceNumber);

        new Thread(() -> readData(device)).start();
        new Thread(() -> sendData(device)).start();

        System.out.println("Press Enter to exit");
        System.in.read();
        device.close();
    }

    @SuppressWarnings({"java:S2925", "BusyWait"})
    private static void sendData(UsbDevice device) {
        var random = new Random();
        var buffer = new byte[80];

        while (true) {
            random.nextBytes(buffer);
            try {
                device.transferOut(1, buffer);
                Thread.sleep(3000);
            } catch (UsbException e) {
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void readData(UsbDevice device) {
        while (true) {
            try {
                device.transferIn(2);
            } catch (UsbException e) {
                return;
            }
        }
    }
}
