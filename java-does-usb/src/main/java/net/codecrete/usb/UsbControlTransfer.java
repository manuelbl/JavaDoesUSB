//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb;

/**
 * USB control transfer parameters.
 * <p>
 * See description of "Setup Data" in the chapter "USB Device Requests" of the
 * USB specification for additional information.
 * </p>
 * <p>
 * For control requests directed to an interface or endpoint,
 * the lower byte of {@code index} contains the interface or endpoint number.
 * </p>
 *
 * @param requestType request type (bits 5 and 6 of {@code bmRequestType})
 * @param recipient   recipient (bits 0–4 of {@code bmRequestType})
 * @param request     request code (value between 0 and 255, called {@code bRequest} in USB specification)
 * @param value       value (value between 0 and 65535, called {@code wValue} in USB specification)
 * @param index       index (value between 0 and 65535, called {@code wIndex} in USB specification).
 */
public record UsbControlTransfer(
        UsbRequestType requestType,
        UsbRecipient recipient,
        int request,
        int value,
        int index
) {
}
