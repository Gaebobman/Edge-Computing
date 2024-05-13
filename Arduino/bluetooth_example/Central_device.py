import asyncio
import re
from bleak import BleakClient, BleakScanner

# Parse header file and extract the configuration values
def read_config_h(file_path):
    config = {}
    with open(file_path, 'r') as file:
        content = file.read()
        matches = re.findall(r'#define\s+(\w+)\s+"([^"]+)"', content)
        for match in matches:
            config[match[0]] = match[1]
    print("Loaded config\n",config)
    return config


config = read_config_h('example_config.h')

# 설정값 추출
SERVICE_UUID = config['SERVICE_UUID']
CHARACTERISTIC_UUID = config['CHARACTERISTIC_UUID']
DEVICE_NAME = config['LOCAL_NAME']

async def connect_and_listen(device_name):
    try:
        print(f"Searching for BLE device with name {device_name}...")
        devices = await BleakScanner.discover()
        device = next((device for device in devices if device.name == device_name), None)

        if not device:
            print(f"No device with name {device_name} found.")
            return

        print(f"Found device {device.name}, address: {device.address}")

        async with BleakClient(device.address, timeout=10.0) as client:
            print(f"Connected to {device.name}")
            
    except Exception as e:
        print(f"Failed to connect or communicate with the device: {e}")
    print(f"Searching for BLE device with name {device_name}...")
    devices = await BleakScanner.discover()
    device = next((device for device in devices if device.name == device_name), None)
    
    if not device:
        print(f"No device with name {device_name} found.")
        return

    print(f"Found device {device.name}, address: {device.address}")

    async with BleakClient(device.address) as client:
        print(f"Connected to {device.name}")

        value = await client.read_gatt_char(CHARACTERISTIC_UUID)
        print(f"Received from device: {value.decode()}")

        def notification_handler(sender, data):
            print(f"Received data: {data.decode()}")

        await client.start_notify(CHARACTERISTIC_UUID, notification_handler)
        print("Notifications enabled.")

        await asyncio.sleep(30)  # Notifications for 30 seconds
        await client.stop_notify(CHARACTERISTIC_UUID)
        print("Notifications disabled.")

if __name__ == "__main__":
    asyncio.run(connect_and_listen(DEVICE_NAME))
