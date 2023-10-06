package net.codecrete.usb;

import net.codecrete.usb.common.ConfigurationParser;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static net.codecrete.usb.ConfigurationDescriptors.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationParserTest {

    @Test
    void simpleDescriptor_canBeParsed() {

        var configuration = ConfigurationParser.parseConfigurationDescriptor(MemorySegment.ofArray(SIMPLE));

        var softly = new SoftAssertions();
        softly.assertThat(configuration.interfaces())
                .hasSize(1)
                .singleElement().satisfies(intf -> {
                    softly.assertThat(intf.number()).isEqualTo(0);
                    softly.assertThat(intf.alternates())
                            .hasSize(1)
                            .singleElement().satisfies(altIntf -> {
                                softly.assertThat(altIntf).isSameAs(intf.alternate());
                                softly.assertThat(altIntf.number()).isEqualTo(0);
                                softly.assertThat(altIntf.endpoints()).isEmpty();
                                softly.assertThat(altIntf.classCode()).isEqualTo(0x0ff);
                                softly.assertThat(altIntf.subclassCode()).isEqualTo(0x0dd);
                                softly.assertThat(altIntf.protocolCode()).isEqualTo(0x0cc);
                            });
                    softly.assertThat(intf.isClaimed()).isFalse();
                });
        softly.assertThat(configuration.functions()).hasSize(1);
        softly.assertThat(configuration.configValue()).isEqualTo(1);
        softly.assertThat(configuration.attributes()).isEqualTo(0x34);
        softly.assertThat(configuration.maxPower()).isEqualTo(0x64);
        softly.assertAll();
    }

    @Test
    @SuppressWarnings("java:S5961")
    void largeCompositeDescriptor_canBeParsed() {
        var configuration = ConfigurationParser.parseConfigurationDescriptor(MemorySegment.ofArray(COMPOSITE_LARGE));

        var softly = new SoftAssertions();

        // 2 functions
        softly.assertThat(configuration.functions())
                .hasSize(2);

        // function 0: 3 interfaces
        softly.assertThat(configuration.functions().get(0)).satisfies(function -> {
            softly.assertThat(function.firstInterfaceNumber()).isEqualTo(0);
            softly.assertThat(function.numInterfaces()).isEqualTo(3);
        });

        // function 1: 1 interface
        softly.assertThat(configuration.functions().get(1)).satisfies(function -> {
            softly.assertThat(function.firstInterfaceNumber()).isEqualTo(3);
            softly.assertThat(function.numInterfaces()).isEqualTo(1);
        });

        softly.assertThat(configuration.interfaces()).hasSize(4);

        // interface 0
        softly.assertThat(configuration.interfaces().get(0)).satisfies(intf -> {
           softly.assertThat(intf.number()).isEqualTo(0);
           softly.assertThat(intf.alternates()).hasSize(1);
           softly.assertThat(intf.alternate().endpoints()).hasSize(1);
           softly.assertThat(intf.alternate().endpoints().get(0)).satisfies(endpoint -> {
              softly.assertThat(endpoint.number()).isEqualTo(5);
               softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.IN);
              softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.INTERRUPT);
           });
        });

        // interface 1
        softly.assertThat(configuration.interfaces().get(1)).satisfies(intf -> {
            softly.assertThat(intf.number()).isEqualTo(1);
            softly.assertThat(intf.alternates()).hasSize(2);
            softly.assertThat(intf.alternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(0);
                softly.assertThat(alternate.endpoints()).isEmpty();
            });
            softly.assertThat(intf.alternates().get(1)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(1);
                softly.assertThat(alternate.endpoints()).hasSize(1);
                softly.assertThat(alternate.endpoints().get(0)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.number()).isEqualTo(1);
                    softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.IN);
                    softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.ISOCHRONOUS);
                });
            });
        });

        // interface 2
        softly.assertThat(configuration.interfaces().get(2)).satisfies(intf -> {
            softly.assertThat(intf.number()).isEqualTo(2);
            softly.assertThat(intf.alternates()).hasSize(2);
            softly.assertThat(intf.alternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(0);
                softly.assertThat(alternate.endpoints()).isEmpty();
            });
            softly.assertThat(intf.alternates().get(1)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(1);
                softly.assertThat(alternate.endpoints()).hasSize(1);
                softly.assertThat(alternate.endpoints().get(0)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.number()).isEqualTo(2);
                    softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.IN);
                    softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.ISOCHRONOUS);
                });
            });
        });

        // interface 3
        softly.assertThat(configuration.interfaces().get(3)).satisfies(intf -> {
            softly.assertThat(intf.number()).isEqualTo(3);
            softly.assertThat(intf.alternates()).hasSize(1);
            softly.assertThat(intf.alternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(0);
                softly.assertThat(alternate.endpoints()).hasSize(1);
                softly.assertThat(alternate.endpoints().get(0)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.number()).isEqualTo(4);
                    softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.IN);
                    softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.INTERRUPT);
                });
            });
        });

        softly.assertAll();
    }


    @Test
    @SuppressWarnings("java:S5961")
    void compositeTestDeviceDescriptor_canBeParsed() {
        var configuration = ConfigurationParser.parseConfigurationDescriptor(MemorySegment.ofArray(COMPOSITE_TEST_DEVICE));

        var softly = new SoftAssertions();

        // 2 functions
        softly.assertThat(configuration.functions())
                .hasSize(2);

        // function 0: 2 interfaces
        softly.assertThat(configuration.functions().get(0)).satisfies(function -> {
            softly.assertThat(function.firstInterfaceNumber()).isEqualTo(0);
            softly.assertThat(function.numInterfaces()).isEqualTo(2);
        });

        // function 1: 2 interfaces
        softly.assertThat(configuration.functions().get(1)).satisfies(function -> {
            softly.assertThat(function.firstInterfaceNumber()).isEqualTo(2);
            softly.assertThat(function.numInterfaces()).isEqualTo(2);
        });

        softly.assertThat(configuration.interfaces()).hasSize(4);

        // interface 0
        softly.assertThat(configuration.interfaces().get(0)).satisfies(intf -> {
            softly.assertThat(intf.number()).isEqualTo(0);
            softly.assertThat(intf.alternates()).hasSize(1);
            softly.assertThat(intf.alternate().endpoints()).hasSize(1);
            softly.assertThat(intf.alternate().endpoints().get(0)).satisfies(endpoint -> {
                softly.assertThat(endpoint.number()).isEqualTo(3);
                softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.IN);
                softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.INTERRUPT);
            });
        });

        // interface 1
        softly.assertThat(configuration.interfaces().get(1)).satisfies(intf -> {
            softly.assertThat(intf.number()).isEqualTo(1);
            softly.assertThat(intf.alternates()).hasSize(1);
            softly.assertThat(intf.alternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(0);
                softly.assertThat(alternate.endpoints()).hasSize(2);
                softly.assertThat(alternate.endpoints().get(0)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.number()).isEqualTo(2);
                    softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.OUT);
                    softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.BULK);
                });
                softly.assertThat(alternate.endpoints().get(1)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.number()).isEqualTo(1);
                    softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.IN);
                    softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.BULK);
                });
            });
        });

        // interface 2
        softly.assertThat(configuration.interfaces().get(2)).satisfies(intf -> {
            softly.assertThat(intf.number()).isEqualTo(2);
            softly.assertThat(intf.alternates()).hasSize(1);
            softly.assertThat(intf.alternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(0);
                softly.assertThat(alternate.endpoints()).isEmpty();
            });
        });

        // interface 3
        softly.assertThat(configuration.interfaces().get(3)).satisfies(intf -> {
            softly.assertThat(intf.number()).isEqualTo(3);
            softly.assertThat(intf.alternates()).hasSize(1);
            softly.assertThat(intf.alternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.number()).isEqualTo(0);
                softly.assertThat(alternate.endpoints()).hasSize(2);
                softly.assertThat(alternate.endpoints().get(0)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.number()).isEqualTo(1);
                    softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.OUT);
                    softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.BULK);
                });
                softly.assertThat(alternate.endpoints().get(1)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.number()).isEqualTo(2);
                    softly.assertThat(endpoint.direction()).isEqualTo(USBDirection.IN);
                    softly.assertThat(endpoint.transferType()).isEqualTo(USBTransferType.BULK);
                });
            });
        });

        softly.assertAll();
    }

    @Test
    void tooShortDescriptor_throwsException() {
        var desc = new byte[COMPOSITE_LARGE.length - 1];
        System.arraycopy(COMPOSITE_LARGE, 0, desc, 0, desc.length);
        var segment = MemorySegment.ofArray(desc);

        assertThatThrownBy(() -> ConfigurationParser.parseConfigurationDescriptor(segment))
                .isInstanceOf(USBException.class)
                .hasMessage("invalid USB configuration descriptor (invalid length)");
    }

    @Test
    void tooLongDescriptor_throwsException() {
        var desc = new byte[COMPOSITE_LARGE.length + 1];
        System.arraycopy(COMPOSITE_LARGE, 0, desc, 0, COMPOSITE_LARGE.length);
        var segment = MemorySegment.ofArray(desc);

        assertThatThrownBy(() -> ConfigurationParser.parseConfigurationDescriptor(segment))
                .isInstanceOf(USBException.class)
                .hasMessage("invalid USB configuration descriptor (invalid length)");
    }

    @Test
    void invalidDescriptor_throwsException() {
        var desc = new byte[] { 0x5a, 0x41, 0x03, 0x07 };
        var segment = MemorySegment.ofArray(desc);

        assertThatThrownBy(() -> ConfigurationParser.parseConfigurationDescriptor(segment))
                .isInstanceOf(USBException.class)
                .hasMessage("invalid USB configuration descriptor");
    }
}
