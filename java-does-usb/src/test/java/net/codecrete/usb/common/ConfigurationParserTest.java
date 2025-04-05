package net.codecrete.usb.common;

import net.codecrete.usb.UsbDirection;
import net.codecrete.usb.UsbException;
import net.codecrete.usb.UsbTransferType;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static net.codecrete.usb.common.ConfigurationDescriptors.COMPOSITE_LARGE;
import static net.codecrete.usb.common.ConfigurationDescriptors.COMPOSITE_TEST_DEVICE;
import static net.codecrete.usb.common.ConfigurationDescriptors.SIMPLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationParserTest {

    @Test
    void simpleDescriptor_canBeParsed() {

        var configuration = ConfigurationParser.parseConfigurationDescriptor(MemorySegment.ofArray(SIMPLE));

        assertThat(configuration.interfaces())
                .hasSize(1)
                .singleElement().satisfies(intf -> {
                    assertThat(intf.getNumber()).isZero();
                    assertThat(intf.getAlternates())
                            .hasSize(1)
                            .singleElement().satisfies(altIntf -> {
                                assertThat(altIntf).isSameAs(intf.getCurrentAlternate());
                                assertThat(altIntf.getNumber()).isZero();
                                assertThat(altIntf.getEndpoints()).isEmpty();
                                assertThat(altIntf.getClassCode()).isEqualTo(0x0ff);
                                assertThat(altIntf.getSubclassCode()).isEqualTo(0x0dd);
                                assertThat(altIntf.getProtocolCode()).isEqualTo(0x0cc);
                            });
                    assertThat(intf.isClaimed()).isFalse();
                });
        assertThat(configuration.functions()).hasSize(1);
        assertThat(configuration.configValue()).isEqualTo(1);
        assertThat(configuration.attributes()).isEqualTo(0x34);
        assertThat(configuration.maxPower()).isEqualTo(0x64);
    }

    @Test
    @SuppressWarnings("java:S5961")
    void largeCompositeDescriptor_canBeParsed() {
        var configuration = ConfigurationParser.parseConfigurationDescriptor(MemorySegment.ofArray(COMPOSITE_LARGE));

        // 2 functions
        assertThat(configuration.functions())
                .hasSize(2);

        // function 0: 3 interfaces
        assertThat(configuration.functions().get(0)).satisfies(function -> {
            assertThat(function.firstInterfaceNumber()).isZero();
            assertThat(function.numInterfaces()).isEqualTo(3);
        });

        // function 1: 1 interface
        assertThat(configuration.functions().get(1)).satisfies(function -> {
            assertThat(function.firstInterfaceNumber()).isEqualTo(3);
            assertThat(function.numInterfaces()).isEqualTo(1);
        });

        assertThat(configuration.interfaces()).hasSize(4);

        // interface 0
        assertThat(configuration.interfaces().get(0)).satisfies(intf -> {
            assertThat(intf.getNumber()).isZero();
            assertThat(intf.getAlternates()).hasSize(1);
            assertThat(intf.getCurrentAlternate().getEndpoints()).hasSize(1);
            assertThat(intf.getCurrentAlternate().getEndpoints().getFirst()).satisfies(endpoint -> {
                assertThat(endpoint.getNumber()).isEqualTo(5);
                assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.INTERRUPT);
            });
        });

        // interface 1
        assertThat(configuration.interfaces().get(1)).satisfies(intf -> {
            assertThat(intf.getNumber()).isEqualTo(1);
            assertThat(intf.getAlternates()).hasSize(2);
            assertThat(intf.getAlternates().get(0)).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isZero();
                assertThat(alternate.getEndpoints()).isEmpty();
            });
            assertThat(intf.getAlternates().get(1)).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isEqualTo(1);
                assertThat(alternate.getEndpoints()).hasSize(1);
                assertThat(alternate.getEndpoints().getFirst()).satisfies(endpoint -> {
                    assertThat(endpoint.getNumber()).isEqualTo(1);
                    assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.ISOCHRONOUS);
                });
            });
        });

        // interface 2
        assertThat(configuration.interfaces().get(2)).satisfies(intf -> {
            assertThat(intf.getNumber()).isEqualTo(2);
            assertThat(intf.getAlternates()).hasSize(2);
            assertThat(intf.getAlternates().get(0)).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isZero();
                assertThat(alternate.getEndpoints()).isEmpty();
            });
            assertThat(intf.getAlternates().get(1)).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isEqualTo(1);
                assertThat(alternate.getEndpoints()).hasSize(1);
                assertThat(alternate.getEndpoints().getFirst()).satisfies(endpoint -> {
                    assertThat(endpoint.getNumber()).isEqualTo(2);
                    assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.ISOCHRONOUS);
                });
            });
        });

        // interface 3
        assertThat(configuration.interfaces().get(3)).satisfies(intf -> {
            assertThat(intf.getNumber()).isEqualTo(3);
            assertThat(intf.getAlternates()).hasSize(1);
            assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isZero();
                assertThat(alternate.getEndpoints()).hasSize(1);
                assertThat(alternate.getEndpoints().getFirst()).satisfies(endpoint -> {
                    assertThat(endpoint.getNumber()).isEqualTo(4);
                    assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.INTERRUPT);
                });
            });
        });
    }


    @Test
    @SuppressWarnings("java:S5961")
    void compositeTestDeviceDescriptor_canBeParsed() {
        var configuration = ConfigurationParser.parseConfigurationDescriptor(MemorySegment.ofArray(COMPOSITE_TEST_DEVICE));

        // 2 functions
        assertThat(configuration.functions())
                .hasSize(2);

        // function 0: 2 interfaces
        assertThat(configuration.functions().get(0)).satisfies(function -> {
            assertThat(function.firstInterfaceNumber()).isZero();
            assertThat(function.numInterfaces()).isEqualTo(2);
        });

        // function 1: 2 interfaces
        assertThat(configuration.functions().get(1)).satisfies(function -> {
            assertThat(function.firstInterfaceNumber()).isEqualTo(2);
            assertThat(function.numInterfaces()).isEqualTo(2);
        });

        assertThat(configuration.interfaces()).hasSize(4);

        // interface 0
        assertThat(configuration.interfaces().get(0)).satisfies(intf -> {
            assertThat(intf.getNumber()).isZero();
            assertThat(intf.getAlternates()).hasSize(1);
            assertThat(intf.getCurrentAlternate().getEndpoints()).hasSize(1);
            assertThat(intf.getCurrentAlternate().getEndpoints().getFirst()).satisfies(endpoint -> {
                assertThat(endpoint.getNumber()).isEqualTo(3);
                assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.INTERRUPT);
            });
        });

        // interface 1
        assertThat(configuration.interfaces().get(1)).satisfies(intf -> {
            assertThat(intf.getNumber()).isEqualTo(1);
            assertThat(intf.getAlternates()).hasSize(1);
            assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isZero();
                assertThat(alternate.getEndpoints()).hasSize(2);
                assertThat(alternate.getEndpoints().get(0)).satisfies(endpoint -> {
                    assertThat(endpoint.getNumber()).isEqualTo(2);
                    assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.OUT);
                    assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
                });
                assertThat(alternate.getEndpoints().get(1)).satisfies(endpoint -> {
                    assertThat(endpoint.getNumber()).isEqualTo(1);
                    assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
                });
            });
        });

        // interface 2
        assertThat(configuration.interfaces().get(2)).satisfies(intf -> {
            assertThat(intf.getNumber()).isEqualTo(2);
            assertThat(intf.getAlternates()).hasSize(1);
            assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isZero();
                assertThat(alternate.getEndpoints()).isEmpty();
            });
        });

        // interface 3
        assertThat(configuration.interfaces().get(3)).satisfies(intf -> {
            assertThat(intf.getNumber()).isEqualTo(3);
            assertThat(intf.getAlternates()).hasSize(1);
            assertThat(intf.getAlternates().getFirst()).satisfies(alternate -> {
                assertThat(alternate.getNumber()).isZero();
                assertThat(alternate.getEndpoints()).hasSize(2);
                assertThat(alternate.getEndpoints().get(0)).satisfies(endpoint -> {
                    assertThat(endpoint.getNumber()).isEqualTo(1);
                    assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.OUT);
                    assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
                });
                assertThat(alternate.getEndpoints().get(1)).satisfies(endpoint -> {
                    assertThat(endpoint.getNumber()).isEqualTo(2);
                    assertThat(endpoint.getDirection()).isEqualTo(UsbDirection.IN);
                    assertThat(endpoint.getTransferType()).isEqualTo(UsbTransferType.BULK);
                });
            });
        });
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
        var desc = new byte[]{0x5a, 0x41, 0x03, 0x07};
        var segment = MemorySegment.ofArray(desc);

        assertThatThrownBy(() -> ConfigurationParser.parseConfigurationDescriptor(segment))
                .isInstanceOf(UsbException.class)
                .hasMessage("invalid USB configuration descriptor");
    }
}
