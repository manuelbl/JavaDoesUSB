//
// Java Does USB
// Copyright (c) 2023 Manuel Bleichenbacher
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

/**
 * Device information set (of Windows Setup API).
 *
 * An instance of this class represents a device information set (DEVINFO)
 * and a current element within the set.
 */
class device_info_set
{
public:
	/**
	 * Creates a new device info set containing the present devices of the specified device class and
	 * optionally device instance ID.
	 *
	 * After creation, there is no current element. `next()` should be called to iterate the first
	 * and all subsequent elements.
	 *
	 * @param interface_guid device interface class GUID
	 * @param instance_id device instance ID
	 * @return device info set
	 */
	static device_info_set of_present_devices(const GUID& interface_guid, const std::wstring& instance_id = L"");

	/**
	 * Creates a new device info set containing a single device with the specified instance ID.
	 *
	 * The device becomes the current element. The set cannot be iterated.
	 *
	 * @param instance_id instance ID
	 * @return device info set
	 */
	static device_info_set of_instance(const std::wstring& instance_id);

	/**
	 * Creates a new device info set containing a single device with the specified path.
	 *
	 * The device becomes the current element. The set cannot be iterated.
	 *
	 * @param device_path device path
	 * @return device info set
	 */
	static device_info_set of_path(const std::wstring& device_path);

	/**
	 * Creates a new empty device info set.
	 *
	 * @return device info set
	 */
	static device_info_set of_empty();

	/**
	 * Iterates to the next element in this set.
	 *
	 * @return `true` if there is a current element, `false` if the iteration moved beyond the last element
	 */
	bool next();

	/**
	 * Gets the integer device property of the current element.
	 *
	 * @param prop_key property key (`DEVPKEY_xxx`)
	 * @return property value
	 */
	uint32_t get_device_property_int(const DEVPROPKEY& prop_key);

	/**
	 * Gets the string device property of the current element.
	 *
	 * @param prop_key property key (`DEVPKEY_xxx`)
	 * @return property value
	 */
	std::wstring get_device_property_string(const DEVPROPKEY& prop_key);

	/**
	 * Gets the string list device property of the current element.
	 *
	 * @param prop_key property key (`DEVPKEY_xxx`)
	 * @return property value
	 */
	std::vector<std::wstring> get_device_property_string_list(const DEVPROPKEY& prop_key);

	/**
	 * Checks if the current element is a composite device.
	 * 
	 * @return `true` if it is a composite device
	 */
	bool is_composite_device();

	device_info_set(device_info_set&& info_set) noexcept;
	~device_info_set();

	/**
	 * Gets the device path for the device with the given device instance ID and device interface class.
	 *
	 * @param instance_id    device instance ID
	 * @param interface_guid device interface class GUID
	 * @return the device path
	 */
	static std::wstring get_device_path(const std::wstring& instance_id, const GUID& interface_guid);

	/**
	 * Gets the device path for the device with the given instance ID.
	 *
	 * The device path is looked up by checking the GUIDs associated with the current element.
	 *
	 * @param instance_id  device instance ID
	 * @return the device path, `nullptr` if not found
	 */
	std::wstring get_device_path_by_guid(const std::wstring& instance_id);

private:
	device_info_set(HDEVINFO dev_info_set);
	device_info_set() = delete;
	device_info_set(const device_info_set& info_set) = delete;
	device_info_set& operator=(const device_info_set&) = delete;
	device_info_set& operator=(device_info_set&& info_set) = delete;

	void add_instance(const std::wstring& instance_id);
	void add_device_path(const std::wstring& device_path);
	std::vector<std::wstring> find_device_interface_guids();

	std::vector<uint8_t> get_device_property_variable_length(const DEVPROPKEY& prop_key, DEVPROPTYPE expected_type);
	static std::vector<std::wstring> split_string_list(const wchar_t* str_list_raw);


	HDEVINFO dev_info_set_;
	SP_DEVINFO_DATA dev_info_data_;
	bool has_dev_intf_data_;
	SP_DEVICE_INTERFACE_DATA dev_intf_data_;
	int iteration_index;
};

