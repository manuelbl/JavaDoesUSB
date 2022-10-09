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
 * See USB specification for additional information
 * </p>
 * <p>
 * For control requests directed to an interface or endpoint,
 * the lower byte of {@code index} contains the interface or endpoint number.
 * </p>
 *
 * @param requestType request type (bits 5 and 6 of {@code bmRequestType})
 * @param recipient   recipient (bits 0–4 of {@code bmRequestType})
 * @param request     request code ({@code bRequest})
 * @param value       value ({@code wValue})
 * @param index       index ({@code wIndex}).
 */
public record USBControlTransfer(USBRequestType requestType, USBRecipient recipient, int request, int value,
                                 int index) {
}
