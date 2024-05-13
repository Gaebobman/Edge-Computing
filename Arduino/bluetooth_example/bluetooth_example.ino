#include <ArduinoBLE.h>
#include "example_config.h"

// User defined Service UUID
BLEService myService(SERVICE_UUID);
// User defined characteristic
BLEStringCharacteristic myCharacteristic(CHARACTERISTIC_UUID, BLERead | BLEWrite | BLENotify, 30);

void setup() {
  Serial.begin(115200);
  while (!Serial);

 if (!BLE.begin()) {
    Serial.println("starting BLE failed!");
    while (1);
  }


  BLE.setLocalName(LOCAL_NAME);
  BLE.setAdvertisedService(myService); 
  myService.addCharacteristic(myCharacteristic);
  BLE.addService(myService);

  myCharacteristic.writeValue("Hello");
  BLE.advertise();
  Serial.println("Bluetooth device active, waiting for connections...");
}

void loop() {
  BLEDevice central = BLE.central();

  if (central) {
    Serial.println("Connected to central device!");
    bool toggle = true;
    
    while (central.connected()) {
      if (toggle) {
        myCharacteristic.writeValue("Hello");
        Serial.println("Sent 'Hello'");
      } else {
        myCharacteristic.writeValue("Bye");
        Serial.println("Sent 'Bye'");
      }
      toggle = !toggle;

      delay(2000);
    }
    Serial.println("Disconnected from central device");
  }
}