# RINES ESP32-C3 FLASHER STANDALONE

Independent Android USB-OTG flasher targeting ESP32-C3 ROM bootloader.

## Important hardware setting

The current controller configures SPI flash capacity as **4 MiB**, which is common on ESP32-C3 Super Mini boards. Confirm the actual flash capacity of your board before flashing. If your board has a different capacity, update `configuredFlashSizeBytes` in `Esp32C3FlashController.kt` to the verified byte count. Do not assume all ESP32-C3 modules have the same flash size.

## Flashing sequence

1. Connect the ESP32-C3 in ROM download mode.
2. Press **SYNC ESP32-C3** and wait for a valid ROM SYNC signature.
3. Select a validated ESP32-C3 firmware image.
4. Flash, then run MD5 verification while the chip remains in download mode.

SPI flash operations now perform `SPI_ATTACH` followed by `SPI_SET_PARAMS` after successful SYNC. `FLASH_END` uses its one-word payload; `SPI_ATTACH` uses the ROM-specific two-word payload.

## Build

The GitHub Actions workflow compiles the debug Kotlin sources and runs protocol unit tests.
