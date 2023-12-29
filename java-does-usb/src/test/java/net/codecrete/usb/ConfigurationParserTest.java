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
                    softly.assertThat(intf.getNumber()).isEqualTo(0);
                    softly.assertThat(intf.getAlternates())
                            .hasSize(1)
                            .singleElement().satisfies(altIntf -> {
                                softly.assertThat(altIntf).isSameAs(intf.getCurrentAlternate());
                                softly.assertThat(altIntf.getNumber()).isEqualTo(0);
                                softly.assertThat(altIntf.getEndpoints()).isEmpty();
                                softly.assertThat(altIntf.getClassCode()).isEqualTo(0x0ff);
                                softly.assertThat(altIntf.getSubclassCode()).isEqualTo(0x0dd);
                                softly.assertThat(altIntf.getProtocolCode()).isEqualTo(0x0cc);
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
           softly.assertThat(intf.getNumber()).isEqualTo(0);
           softly.assertThat(intf.getAlternates()).hasSize(1);
           softly.assertThat(intf.getCurrentAlternate().getEndpoints()).hasSize(1);
           softly.assertThat(intf.getCurrentAlternate().getEndpoints().getFirst()).satisfies(endpoint -> {
              softly.assertThat(endpoint.getNumber()).isEqualTo(5);
               softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
              softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.INTERRUPT);
           });
        });

        // interface 1
        softly.assertThat(configuration.interfaces().get(1)).satisfies(intf -> {
            softly.assertThat(intf.getNumber()).isEqualTo(1);
            softly.assertThat(intf.getAlternates()).hasSize(2);
            softly.assertThat(intf.getAlternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(0);
                softly.assertThat(alternate.getEndpoints()).isEmpty();
            });
            softly.assertThat(intf.getAlternates().get(1)).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(1);
                softly.assertThat(alternate.getEndpoints()).hasSize(1);
                softly.assertThat(alternate.getEndpoints().getFirst()).satisfies(endpoint -> {
                    softly.assertThat(endpoint.getNumber()).isEqualTo(1);
                    softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.ISOCHRONOUS);
                });
            });
        });

        // interface 2
        softly.assertThat(configuration.interfaces().get(2)).satisfies(intf -> {
            softly.assertThat(intf.getNumber()).isEqualTo(2);
            softly.assertThat(intf.getAlternates()).hasSize(2);
            softly.assertThat(intf.getAlternates().get(0)).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(0);
                softly.assertThat(alternate.getEndpoints()).isEmpty();
            });
            softly.assertThat(intf.getAlternates().get(1)).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(1);
                softly.assertThat(alternate.getEndpoints()).hasSize(1);
                softly.assertThat(alternate.getEndpoints().getFirst()).satisfies(endpoint -> {
                    softly.assertThat(endpoint.getNumber()).isEqualTo(2);
                    softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.ISOCHRONOUS);
                });
            });
        });

        // interface 3
        softly.assertThat(configuration.interfaces().get(3)).satisfies(intf -> {
            softly.assertThat(intf.getNumber()).isEqualTo(3);
            softly.assertThat(intf.getAlternates()).hasSize(1);
            softly.assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(0);
                softly.assertThat(alternate.getEndpoints()).hasSize(1);
                softly.assertThat(alternate.getEndpoints().getFirst()).satisfies(endpoint -> {
                    softly.assertThat(endpoint.getNumber()).isEqualTo(4);
                    softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.INTERRUPT);
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
            softly.assertThat(intf.getNumber()).isEqualTo(0);
            softly.assertThat(intf.getAlternates()).hasSize(1);
            softly.assertThat(intf.getCurrentAlternate().getEndpoints()).hasSize(1);
            softly.assertThat(intf.getCurrentAlternate().getEndpoints().getFirst()).satisfies(endpoint -> {
                softly.assertThat(endpoint.getNumber()).isEqualTo(3);
                softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.INTERRUPT);
            });
        });

        // interface 1
        softly.assertThat(configuration.interfaces().get(1)).satisfies(intf -> {
            softly.assertThat(intf.getNumber()).isEqualTo(1);
            softly.assertThat(intf.getAlternates()).hasSize(1);
            softly.assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(0);
                softly.assertThat(alternate.getEndpoints()).hasSize(2);
                softly.assertThat(alternate.getEndpoints().get(0)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.getNumber()).isEqualTo(2);
                    softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.OUT);
                    softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
                });
                softly.assertThat(alternate.getEndpoints().get(1)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.getNumber()).isEqualTo(1);
                    softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
                });
            });
        });

        // interface 2
        softly.assertThat(configuration.interfaces().get(2)).satisfies(intf -> {
            softly.assertThat(intf.getNumber()).isEqualTo(2);
            softly.assertThat(intf.getAlternates()).hasSize(1);
            softly.assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(0);
                softly.assertThat(alternate.getEndpoints()).isEmpty();
            });
        });

        // interface 3
        softly.assertThat(configuration.interfaces().get(3)).satisfies(intf -> {
            softly.assertThat(intf.getNumber()).isEqualTo(3);
            softly.assertThat(intf.getAlternates()).hasSize(1);
            softly.assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                softly.assertThat(alternate.getNumber()).isEqualTo(0);
                softly.assertThat(alternate.getEndpoints()).hasSize(2);
                softly.assertThat(alternate.getEndpoints().get(0)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.getNumber()).isEqualTo(1);
                    softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.OUT);
                    softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
                });
                softly.assertThat(alternate.getEndpoints().get(1)).satisfies(endpoint -> {
                    softly.assertThat(endpoint.getNumber()).isEqualTo(2);
                    softly.assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    softly.assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
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
                .isInstanceOf(UsbException.class)
                .hasMessage("invalid USB configuration descriptor (invalid length)");
    }

    @Test
    void tooLongDescriptor_throwsException() {
        var desc = new byte[COMPOSITE_LARGE.length + 1];
        System.arraycopy(COMPOSITE_LARGE, 0, desc, 0, COMPOSITE_LARGE.length);
        var segment = MemorySegment.ofArray(desc);

        assertThatThrownBy(() -> ConfigurationParser.parseConfigurationDescriptor(segment))
                .isInstanceOf(UsbException.class)
                .hasMessage("invalid USB configuration descriptor (invalid length)");
    }

    @Test
    void invalidDescriptor_throwsException() {
        var desc = new byte[] { 0x5a, 0x41, 0x03, 0x07 };
        var segment = MemorySegment.ofArray(desc);

        assertThatThrownBy(() -> ConfigurationParser.parseConfigurationDescriptor(segment))
                .isInstanceOf(UsbException.class)
                .hasMessage("invalid USB configuration descriptor");
    }
}
