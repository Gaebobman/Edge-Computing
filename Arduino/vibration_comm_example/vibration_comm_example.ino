/* Edge Impulse ingestion SDK
 * Copyright (c) 2022 EdgeImpulse Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/* Includes ---------------------------------------------------------------- */
#include <vibration_inferencing.h>
#include <Arduino_LSM9DS1.h>  //Click here to get the library: https://www.arduino.cc/reference/en/libraries/arduino_lsm9ds1/
#include <ArduinoBLE.h>
#include "example_config.h"

// User defined Service UUID
BLEService myService(SERVICE_UUID);
// User defined characteristic
BLEStringCharacteristic myCharacteristic(CHARACTERISTIC_UUID, BLERead | BLEWrite | BLENotify, 150);

/* Constant defines -------------------------------------------------------- */
#define CONVERT_G_TO_MS2 9.80665f
#define MAX_ACCEPTED_RANGE 2.0f  // starting 03/2022, models are generated setting range to +-2, but this example use Arudino library which set range to +-4g. If you are using an older model, ignore this value and use 4.0f instead

/*
 ** NOTE: If you run into TFLite arena allocation issue.
 **
 ** This may be due to may dynamic memory fragmentation.
 ** Try defining "-DEI_CLASSIFIER_ALLOCATION_STATIC" in boards.local.txt (create
 ** if it doesn't exist) and copy this file to
 ** `<ARDUINO_CORE_INSTALL_PATH>/arduino/hardware/<mbed_core>/<core_version>/`.
 **
 ** See
 ** (https://support.arduino.cc/hc/en-us/articles/360012076960-Where-are-the-installed-cores-located-)
 ** to find where Arduino installs cores on your machine.
 **
 ** If the problem persists then there's not enough memory for this model and application.
 */

/* Private variables ------------------------------------------------------- */
static bool debug_nn = false;  // Set this to true to see e.g. features generated from the raw signal

/**
* @brief      Arduino setup function
*/
void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  // comment out the below line to cancel the wait for USB connection (needed for native USB)
  while (!Serial)
    ;
  ei_printf("Edge Impulse Inferencing Demo");
  if (!BLE.begin()) {
    ei_printf("starting BLE failed!");
    while (1)
      ;
  }
  BLE.setLocalName(LOCAL_NAME);
  BLE.setAdvertisedService(myService);
  myService.addCharacteristic(myCharacteristic);
  BLE.addService(myService);

  myCharacteristic.writeValue("Hello");
  BLE.advertise();
  Serial.println("Bluetooth device active, waiting for connections...");
  if (!IMU.begin()) {
    ei_printf("Failed to initialize IMU!\r\n");
  } else {
    ei_printf("IMU initialized\r\n");
  }

  if (EI_CLASSIFIER_RAW_SAMPLES_PER_FRAME != 3) {
    ei_printf("ERR: EI_CLASSIFIER_RAW_SAMPLES_PER_FRAME should be equal to 3 (the 3 sensor axes)\n");
    return;
  }
}

/**
 * @brief Return the sign of the number
 * 
 * @param number 
 * @return int 1 if positive (or 0) -1 if negative
 */
float ei_get_sign(float number) {
  return (number >= 0.0) ? 1.0 : -1.0;
}

/**
* @brief      Get data and run inferencing
*
* @param[in]  debug  Get debug info if true
*/
void loop() {
  BLEDevice central = BLE.central();

  if (central) {
    while (central.connected()) {
      ei_printf("Connected to central device!");
      ei_printf("\nStarting inferencing in 2 seconds...\n");

      delay(2000);

      ei_printf("Sampling...\n");

      // Allocate a buffer here for the values we'll read from the IMU
      float buffer[EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE] = { 0 };

      for (size_t ix = 0; ix < EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE; ix += 3) {
        // Determine the next tick (and then sleep later)
        uint64_t next_tick = micros() + (EI_CLASSIFIER_INTERVAL_MS * 1000);

        IMU.readAcceleration(buffer[ix], buffer[ix + 1], buffer[ix + 2]);

        for (int i = 0; i < 3; i++) {
          if (fabs(buffer[ix + i]) > MAX_ACCEPTED_RANGE) {
            buffer[ix + i] = ei_get_sign(buffer[ix + i]) * MAX_ACCEPTED_RANGE;
          }
        }

        buffer[ix + 0] *= CONVERT_G_TO_MS2;
        buffer[ix + 1] *= CONVERT_G_TO_MS2;
        buffer[ix + 2] *= CONVERT_G_TO_MS2;

        delayMicroseconds(next_tick - micros());
      }

      // Turn the raw buffer in a signal which we can the classify
      signal_t signal;
      int err = numpy::signal_from_buffer(buffer, EI_CLASSIFIER_DSP_INPUT_FRAME_SIZE, &signal);
      if (err != 0) {
        ei_printf("Failed to create signal from buffer (%d)\n", err);
        return;
      }

      // Run the classifier
      ei_impulse_result_t result = { 0 };

      err = run_classifier(&signal, &result, debug_nn);
      if (err != EI_IMPULSE_OK) {
        ei_printf("ERR: Failed to run classifier (%d)\n", err);
        return;
      }

      // print the predictions
      ei_printf("Predictions ");
      ei_printf("(DSP: %d ms., Classification: %d ms., Anomaly: %d ms.)",
                result.timing.dsp, result.timing.classification, result.timing.anomaly);
      ei_printf(": \n");
      char stream_buffer[150] = {};
      size_t stream_index = 0;
      for (size_t ix = 0; ix < EI_CLASSIFIER_LABEL_COUNT; ix++) {
        stream_index += snprintf(&stream_buffer[stream_index], sizeof(stream_buffer) - stream_index,
                                 "    %s: %.5f\n", result.classification[ix].label, result.classification[ix].value);

        ei_printf("    %s: %.5f\n", result.classification[ix].label, result.classification[ix].value);
      }
      delay(2000);
#if EI_CLASSIFIER_HAS_ANOMALY == 1
      stream_index += snprintf(&stream_buffer[stream_index], sizeof(stream_buffer) - stream_index,
                               "    anomaly score: %.3f\n", result.anomaly);
#endif
      myCharacteristic.writeValue(stream_buffer);
    }
  }
}


#if !defined(EI_CLASSIFIER_SENSOR) || EI_CLASSIFIER_SENSOR != EI_CLASSIFIER_SENSOR_ACCELEROMETER
#error "Invalid model for current sensor"
#endif
