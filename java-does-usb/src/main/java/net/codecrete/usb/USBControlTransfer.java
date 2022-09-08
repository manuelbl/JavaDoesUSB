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
 *
 * @param requestType request type
 * @param recipient   recipient
 * @param request     request code
 * @param value       value
 * @param index       index
 */
public record USBControlTransfer(USBRequestType requestType, USBRecipient recipient, byte request, short value,
                                 short index) {
}
