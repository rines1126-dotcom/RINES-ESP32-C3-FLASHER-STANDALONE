
package com.rines.esp32c3flasher.protocol

object SlipCodec {

    private const val END: Byte = 0xC0.toByte()
    private const val ESC: Byte = 0xDB.toByte()
    private const val ESC_END: Byte = 0xDC.toByte()
    private const val ESC_ESC: Byte = 0xDD.toByte()

    /**
     * Encodes a packet using SLIP framing.
     */
    fun encode(data: ByteArray): ByteArray {
        val output = ArrayList<Byte>()

        output.add(END)

        for (byte in data) {
            when (byte) {
                END -> {
                    output.add(ESC)
                    output.add(ESC_END)
                }

                ESC -> {
                    output.add(ESC)
                    output.add(ESC_ESC)
                }

                else -> output.add(byte)
            }
        }

        output.add(END)

        return output.toByteArray()
    }

    /**
     * Decodes a complete SLIP frame.
     * Returns null if the frame contains invalid escape sequences.
     */
    fun decode(frame: ByteArray): ByteArray? {
        val output = ArrayList<Byte>()
        var escaped = false

        for (byte in frame) {
            if (escaped) {
                when (byte) {
                    ESC_END -> output.add(END)
                    ESC_ESC -> output.add(ESC)
                    else -> return null
                }

                escaped = false
            } else {
                when (byte) {
                    ESC -> escaped = true
                    END -> return null
                    else -> output.add(byte)
                }
            }
        }

        if (escaped) return null

        return output.toByteArray()
    }

    /**
     * Splits incoming serial data into complete SLIP frames.
     * Handles packets received in multiple USB reads.
     */
    class Decoder {

        private val buffer = ArrayList<Byte>()
        private var insideFrame = false
        private var escaped = false

        fun reset() {
            buffer.clear()
            insideFrame = false
            escaped = false
        }

        fun feed(data: ByteArray): List<ByteArray> {
            val packets = mutableListOf<ByteArray>()

            for (byte in data) {
                when (byte) {
                    END -> {
                        if (insideFrame && buffer.isNotEmpty() && !escaped) {
                            packets.add(buffer.toByteArray())
                        }

                        buffer.clear()
                        insideFrame = true
                        escaped = false
                    }

                    ESC -> {
                        if (insideFrame) {
                            if (escaped) {
                                reset()
                            } else {
                                escaped = true
                            }
                        }
                    }

                    else -> {
                        if (!insideFrame) continue

                        if (escaped) {
                            when (byte) {
                                ESC_END -> buffer.add(END)
                                ESC_ESC -> buffer.add(ESC)
                                else -> reset()
                            }

                            escaped = false
                        } else {
                            buffer.add(byte)
                        }
                    }
                }
            }

            return packets
        }
    }
}
