//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.macos;

import net.codecrete.usb.common.Transfer;

class MacosTransfer extends Transfer {
    private long id;

    public long id() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
