//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.usb.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides the names of known USB classes, subclasses and protocols.
 */
public class USBClassInfo {

    private static final List<ClassCode> classCodes = new ArrayList<>();
    private static final List<SubclassCode> subclassCodes = new ArrayList<>();
    private static final List<ProtocolCode> protocolCodes = new ArrayList<>();

    // List of known device classes, subclasses and
    // from http://www.linux-usb.org/usb.ids
    private static final String RAW_DATA = """
C 00  (Defined at Interface level)
C 01  Audio
	01  Control Device
	02  Streaming
	03  MIDI Streaming
C 02  Communications
	01  Direct Line
	02  Abstract (modem)
		00  None
		01  AT-commands (v.25ter)
		02  AT-commands (PCCA101)
		03  AT-commands (PCCA101 + wakeup)
		04  AT-commands (GSM)
		05  AT-commands (3G)
		06  AT-commands (CDMA)
		fe  Defined by command set descriptor
		ff  Vendor Specific (MSFT RNDIS?)
	03  Telephone
	04  Multi-Channel
	05  CAPI Control
	06  Ethernet Networking
	07  ATM Networking
	08  Wireless Handset Control
	09  Device Management
	0a  Mobile Direct Line
	0b  OBEX
	0c  Ethernet Emulation
		07  Ethernet Emulation (EEM)
C 03  Human Interface Device
	00  No Subclass
		00  None
		01  Keyboard
		02  Mouse
	01  Boot Interface Subclass
		00  None
		01  Keyboard
		02  Mouse
C 05  Physical Interface Device
C 06  Imaging
	01  Still Image Capture
		01  Picture Transfer Protocol (PIMA 15470)
C 07  Printer
	01  Printer
		00  Reserved/Undefined
		01  Unidirectional
		02  Bidirectional
		03  IEEE 1284.4 compatible bidirectional
		ff  Vendor Specific
C 08  Mass Storage
	01  RBC (typically Flash)
		00  Control/Bulk/Interrupt
		01  Control/Bulk
		50  Bulk-Only
	02  SFF-8020i, MMC-2 (ATAPI)
	03  QIC-157
	04  Floppy (UFI)
		00  Control/Bulk/Interrupt
		01  Control/Bulk
		50  Bulk-Only
	05  SFF-8070i
	06  SCSI
		00  Control/Bulk/Interrupt
		01  Control/Bulk
		50  Bulk-Only
C 09  Hub
	00  Unused
		00  Full speed (or root) hub
		01  Single TT
		02  TT per port
C 0a  CDC Data
	00  Unused
		30  I.430 ISDN BRI
		31  HDLC
		32  Transparent
		50  Q.921M
		51  Q.921
		52  Q.921TM
		90  V.42bis
		91  Q.932 EuroISDN
		92  V.120 V.24 rate ISDN
		93  CAPI 2.0
		fd  Host Based Driver
		fe  CDC PUF
		ff  Vendor specific
C 0b  Chip/SmartCard
C 0d  Content Security
C 0e  Video
	00  Undefined
	01  Video Control
	02  Video Streaming
	03  Video Interface Collection
C 58  Xbox
	42  Controller
C dc  Diagnostic
	01  Reprogrammable Diagnostics
		01  USB2 Compliance
C e0  Wireless
	01  Radio Frequency
		01  Bluetooth
		02  Ultra WideBand Radio Control
		03  RNDIS
	02  Wireless USB Wire Adapter
		01  Host Wire Adapter Control/Data Streaming
		02  Device Wire Adapter Control/Data Streaming
		03  Device Wire Adapter Isochronous Streaming
C ef  Miscellaneous Device
	01  ?
		01  Microsoft ActiveSync
		02  Palm Sync
	02  ?
		01  Interface Association
		02  Wire Adapter Multifunction Peripheral
	03  ?
		01  Cable Based Association
	05  USB3 Vision
C fe  Application Specific Interface
	01  Device Firmware Update
	02  IRDA Bridge
	03  Test and Measurement
		01  TMC
		02  USB488
C ff  Vendor Specific Class
	ff  Vendor Specific Subclass
		ff  Vendor Specific Protocol""";


    /**
     * Provides the name of the specified USB class.
     * @param classCode the USB class code
     * @return an optional name
     */
    public static Optional<String> lookupClass(int classCode) {
        loadData();
        return classCodes.stream()
                .filter((cc) -> cc.classCode == classCode)
                .map((cc) -> cc.name)
                .findFirst();
    }

    /**
     * Provides the name of the specified USB subclass.
     * @param classCode the USB class code
     * @param subclassCode the USB subclass code
     * @return an optional name
     */
    public static Optional<String> lookupSubclass(int classCode, int subclassCode) {
        loadData();
        return subclassCodes.stream()
                .filter((scc) -> scc.classCode == classCode && scc.subclassCode == subclassCode)
                .map((cc) -> cc.name)
                .findFirst();
    }

    /**
     * Provides the name of the specified USB protocol.
     * @param classCode the USB class code
     * @param subclassCode the USB subclass code
     * @param protocolCode the USB protocol code
     * @return an optional name
     */
    public static Optional<String> lookupProtocol(int classCode, int subclassCode, int protocolCode) {
        loadData();
        return protocolCodes.stream()
                .filter((scc) -> scc.classCode == classCode && scc.subclassCode == subclassCode
                        && scc.protocolCode == protocolCode)
                .map((cc) -> cc.name)
                .findFirst();
    }

    private static void loadData() {
        if (classCodes.size() > 0)
            return;

        try (var stringReader = new StringReader(RAW_DATA);
            var reader = new BufferedReader(stringReader)) {

            int classCode = 0;
            int subclassCode = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                // protocol line
                if (line.startsWith("\t\t")) {
                    int protocol = Integer.parseInt(line.substring(2, 4), 16);
                    protocolCodes.add(new ProtocolCode(classCode, subclassCode, protocol, line.substring(6)));
                // subclass line
                } else if (line.startsWith("\t")) {
                    subclassCode = Integer.parseInt(line.substring(1, 3), 16);
                    subclassCodes.add(new SubclassCode(classCode, subclassCode, line.substring(5)));
                // class line
                } else if (line.startsWith("C ")) {
                    classCode = Integer.parseInt(line.substring(2, 4), 16);
                    classCodes.add(new ClassCode(classCode, line.substring(6)));

                } else {
                    throw new IllegalStateException("Invalid raw data");
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record ClassCode(int classCode, String name) { }
    record SubclassCode(int classCode, int subclassCode, String name) { }
    record ProtocolCode(int classCode, int subclassCode, int protocolCode, String name) { }
}
