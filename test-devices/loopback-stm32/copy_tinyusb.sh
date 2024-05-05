#!/bin/sh
TINYUSB_DIR=../../../tinyusb
rm -rf lib/tinyusb/*
mkdir lib/tinyusb/osal
mkdir lib/tinyusb/portable
mkdir lib/tinyusb/portable/synopsys
mkdir lib/tinyusb/portable/st
cp -R $TINYUSB_DIR/src/common lib/tinyusb
cp -R $TINYUSB_DIR/src/device lib/tinyusb
cp $TINYUSB_DIR/src/osal/osal.h lib/tinyusb/osal
cp $TINYUSB_DIR/src/osal/osal_none.h lib/tinyusb/osal
cp -R $TINYUSB_DIR/src/portable/synopsys/dwc2 lib/tinyusb/portable/synopsys
cp -R $TINYUSB_DIR/src/portable/st/stm32_fsdev lib/tinyusb/portable/st
cp $TINYUSB_DIR/src/*.c lib/tinyusb
cp $TINYUSB_DIR/src/*.h lib/tinyusb
