//
// Java Does USB
// Copyright (c) 2022 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
// Reference C++ code for Windows
//

#pragma once

#include <string>
#include <vector>
#include <Windows.h>
#undef min
#undef max
#undef LowSpeed
#include <SetupAPI.h>

/// <summary>
/// Helper class for querying device information.
/// </summary>
class usb_device_info
{
private:
    /// <summary>
    /// Get a device property of integer type.
    /// </summary>
    /// <param name="dev_info">handle of device information set containing the device</param>
    /// <param name="dev_info_data">pointer to device information structure representing the device</param>
    /// <param name="prop_key">property key</param>
    /// <returns>property integer value</returns>
    static uint32_t get_device_property_int(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key);

    /// <summary>
    /// Get a device property of string type.
    /// </summary>
    /// <param name="dev_info">handle of device information set containing the device</param>
    /// <param name="dev_info_data">pointer to device information structure representing the device</param>
    /// <param name="prop_key">property key</param>
    /// <returns>property string value</returns>
    static std::wstring get_device_property_string(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key);

    /// <summary>
    /// Get a device property of variable length.
    /// </summary>
    /// <param name="dev_info">handle of device information set containing the device</param>
    /// <param name="dev_info_data">pointer to device information structure representing the device</param>
    /// <param name="prop_key">property key</param>
    /// <param name="expected_type">expected property type (use DEVPROP_TYPE_xxx constants)</param>
    /// <returns>property value as byte array</returns>
    static std::vector<uint8_t> get_device_property_variable_length(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key, DEVPROPTYPE expected_type);

    /// <summary>
    /// Get a device property of string list type.
    /// </summary>
    /// <param name="dev_info">handle of device information set containing the device</param>
    /// <param name="dev_info_data">pointer to device information structure representing the device</param>
    /// <param name="prop_key">property key</param>
    /// <returns>property value as vector of strings</returns>
    static std::vector<std::wstring> get_device_property_string_list(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data, const DEVPROPKEY* prop_key);

    /// <summary>
    /// Split the string list into a vector of strings
    /// <para>
    /// The provides string list is a concatenation of null-terminated string, terminated with an additional zero-length string.
    /// </para>
    /// </summary>
    /// <param name="string_list">string list</param>
    /// <returns>vector of strings</returns>
    static std::vector<std::wstring> split_string_list(const wchar_t* string_list);

    /// <summary>
    /// Get device path for the specified device instance and device interface class. 
    /// </summary>
    /// <param name="instance_id">device instance ID</param>
    /// <param name="interface_guid">device interface class GUID</param>
    /// <returns>device path</returns>
    static std::wstring get_device_path(const std::wstring& instance_id, const GUID* interface_guid);

    /// <summary>
    /// Get a USB descriptor.
    /// <para>
    /// The descriptor is retrieved via the USB hub (the USB device's parent) so it can be
    /// access for USB devices that are already opened by another application.
    /// </para>
    /// </summary>
    /// <param name="hub_handle">handle of USB hub</param>
    /// <param name="usb_port_num">device's port number (at hub)</param>
    /// <param name="descriptor_type">descriptor type (use USB_xxx_DESCRIPTOR_TYPE constants)</param>
    /// <param name="index">descriptor index</param>
    /// <param name="language_id">language ID</param>
    /// <param name="request_size">intial size to request</param>
    /// <returns>descriptor as byte array</returns>
    static std::vector<uint8_t> get_descriptor(HANDLE hub_handle, ULONG usb_port_num, uint16_t descriptor_type, int index, int language_id, int request_size = 0);

    /// <summary>
    /// Get USB string with the specified index.
    /// <para>
    /// The string descriptor is retrieved via the USB hub (the USB device's parent) so it can be
    /// access for USB devices that are already opened by another application.
    /// </para>
    /// <para>
    /// For string index 0, an empty string is returned.
    /// </para>
    /// </summary>
    /// <param name="hub_handle">handle of USB hub</param>
    /// <param name="usb_port_num">device's port number (at hub)</param>
    /// <param name="index">string index</param>
    /// <returns>string</returns>
    static std::string get_string(HANDLE hub_handle, ULONG usb_port_num, int index);

    /// <summary>
    /// Query if device is a composite USB device.
    /// <para>
    /// A composite device consists of multiple interfaces appearing as separate devices
    /// in Windows. In the Setup API, they are represented as child devices.
    /// </para>
    /// </summary>
    /// <param name="dev_info">handle of device information set containing the device</param>
    /// <param name="dev_info_data">pointer to device information structure representing the device</param>
    /// <returns><c>true</c> if device is a composite device, <c>false</c> otherwise</returns>
    static bool is_composite_device(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data);

    /// <summary>
    /// Get the number of the first USB interface.
    /// <para>
    /// The first interface number is relevant for child devices of composite devices.
    /// </para>
    /// </summary>
    /// <param name="dev_info">handle of device information set containing the device</param>
    /// <param name="dev_info_data">pointer to device information structure representing the device</param>
    /// <returns></returns>
    static int get_first_interface(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data);

    /// <summary>
    /// Get the device's interface GUIDs.
    /// </summary>
    /// <param name="dev_info">handle of device information set containing the device</param>
    /// <param name="dev_info_data">pointer to device information structure representing the device</param>
    /// <returns>vector of GUIDs</returns>
    static std::vector<std::wstring> find_device_interface_guids(HDEVINFO dev_info, SP_DEVINFO_DATA* dev_info_data);

    friend class usb_registry;
};

