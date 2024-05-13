import asyncio
import re
from bleak import BleakClient, BleakScanner
import warnings

    
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

# Extract the configuration values
SERVICE_UUID = config['SERVICE_UUID']
CHARACTERISTIC_UUID = config['CHARACTERISTIC_UUID']
# DEVICE_NAME = config['LOCAL_NAME']
DEVICE_NAME = "Test enhanced advertising"


def detection_callback(device, advertisement_data):
    if (device.name == DEVICE_NAME):
        # Address: mac address of the device
        # RSSI: Received Signal Strength Indicator
        # advertisement_data: dictionary containing the advertisement data
        print(device.address, "RSSI:", device.rssi, advertisement_data)


def on_disconnect(client):
    print("Client with address {} got disconnected!".format(client.address))


def notification_handler(sender, data):
    print(f"Received data: {data.decode()}")



async def connect_and_listen(device_name):
    print(f"Searching for BLE device with name {device_name}...")
    
    scanner = BleakScanner()
    scanner.register_detection_callback(detection_callback)
    await scanner.start()
    await asyncio.sleep(5)
    await scanner.stop()
    devices = await scanner.get_discovered_devices()        
    device = next((device for device in devices if device.name == device_name), None)
    if not device:
        print(f"No device with name {device_name} found.")
        return
    print(f"Found device {device.name}, address: {device.address}")
    
    client =  BleakClient(device.address)
    print("BleakClient created.")
    
    try:
        client.set_disconnected_callback(on_disconnect)
        
        await client.connect()
        print(f"Connected to {device.address}")
            
        services = await client.get_services()
        print("Services:", type(services))
        for service in services:
            print(service)
            print('\tuuid:', service.uuid)
            print('\tcharacteristic list:')

            for characteristic in service.characteristics:

                print('\t\t', characteristic)
                # UUID 
                print('\t\tuuid:', characteristic.uuid)

                print('\t\tdescription :', characteristic.description)
                print('\t\tproperties :', characteristic.properties)

    except Exception as e:
        print(f"Failed to connect to the device: {e}")
        if client.is_connected:
            print("Disconnecting...")
            await client.disconnect()
        
        # value = await client.read_gatt_char(CHARACTERISTIC_UUID)
        # print(f"Received from device: {value.decode()}")
        
        # await client.start_notify(CHARACTERISTIC_UUID, notification_handler)
        # print("Notifications enabled.")
        # await client.stop_notify(CHARACTERISTIC_UUID)
        # print("Notifications disabled.")
        
            
        
if __name__ == "__main__":
    with warnings.catch_warnings():
        warnings.simplefilter(action='ignore', category=FutureWarning)
        loop = asyncio.get_event_loop()
        loop.run_until_complete(connect_and_listen(DEVICE_NAME))
    print("End of program.")