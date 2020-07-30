# DataLinkLayerSimulation
Simulation of DataLink Layer and Physical Layer in Networks with Parity Bit and CRC error detection

## Description
In this project, we have two hosts consisting of two layers, namely DataLink and Physical Layers, communicating with each other through one low-noise medium.
Medium randomly flips bits sent between two hosts creating errors in the message.
The hosts have to use their Data Link Layers to detect those errors with two methods: Parity Bit method and CRC error detection method

## Parity Bit
> A parity bit, or check bit, is a bit added to a string of binary code. Parity bits are used as the simplest form of error detecting code. Parity bits are generally applied to the smallest units of a communication protocol, typically 8-bit octets (bytes), although they can also be applied separately to an entire message string of bits.
You can read more on this wikipedia page: [Parity Bits or Check Bits](https://en.wikipedia.org/wiki/Parity_bit)

## CRC, a.k.a. Cyclic Redundancy Check
> A cyclic redundancy check (CRC) is an error-detecting code commonly used in digital networks and storage devices to detect accidental changes to raw data. Blocks of data entering these systems get a short check value attached, based on the remainder of a polynomial division of their contents. On retrieval, the calculation is repeated and, in the event the check values do not match, corrective action can be taken against data corruption. CRCs can be used for error correction
You can read more on this wikipedia page: [CRC](https://en.wikipedia.org/wiki/Cyclic_redundancy_check)
