// =============================================================================
// IMPORTS

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
// =============================================================================


// =============================================================================
/**
 * @file   ParityDataLinkLayer.java
 * @author Ikram Gabiyev
 * @date   February 2020
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs no error management.
 */
public class ParityDataLinkLayer extends DataLinkLayer {
// =============================================================================



    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {

		Queue<Byte> framingData = new LinkedList<Byte>();
		
		int currentByteIndex = 0;
		//while there is still some raw data
		while (currentByteIndex < data.length) 
		{
			//storing the number of 1 bits (including all metadata)
			int numOfOneBits = 0;
			//add a start tag to framingData => will have all frames in it
			framingData.add(startTag);

			System.out.println("<start>");
			//count 1 bits for startTag
			numOfOneBits += Integer.bitCount((int)startTag);
			
			//look into the raw data twice
			//this makes sure that we don't exceed 8 bytes in a frame
			for(int j = 0; j < 2; j++)
			{
				if (currentByteIndex < data.length) 
				{
					// If the current data byte is itself a metadata tag, then precede
					// it with an escape tag.
					//	consider parity bytes as metadata
					byte currentByte = data[currentByteIndex];
					
					
					
					if ((currentByte == startTag) ||
					(currentByte == stopTag) ||
					(currentByte == escapeTag) ||
					(currentByte == evenParityByte) ||
					(currentByte == oddParityByte)) {

						framingData.add(escapeTag);
						System.out.println("<esc>");
						numOfOneBits += Integer.bitCount((int)escapeTag);

					}

					// Add the data byte itself.
					framingData.add(currentByte);
					System.out.println("[raw]");
					//count and add 1 bits' count
					numOfOneBits += Integer.bitCount((int)currentByte);

					//incrementing the currentByteIndex
					currentByteIndex++;
				}
			}
			//add the correct parity byte


			// End with a stop tag.
			framingData.add(stopTag);
			System.out.println("<stop>");
			//count and add 1 bits' count
			numOfOneBits += Integer.bitCount((int)stopTag);

			//if wee get even num of 1 bits
			if (numOfOneBits % 2 == 0)
			{
				framingData.add(evenParityByte);
				System.out.println("<even parity>");
			}
			// if the num of 1 bits is odd
			else
			{
				framingData.add(oddParityByte);
				System.out.println("<odd parity>");
			}

			/**
			 * The Frame Structure
			 * <start> --- <esc> --- <stop> <parity byte>
			 */
		}

		// Convert to the desired byte array.
		byte[] framedData = new byte[framingData.size()];
		Iterator<Byte>  i = framingData.iterator();
		int             j = 0;
		while (i.hasNext()) {
			framedData[j++] = i.next();
		}
		
		return framedData;
	
    } // createFrame ()
    // =========================================================================


    
    // =========================================================================
    /**
     * Determine whether the received, buffered data constitutes a complete
     * frame.  If so, then remove the framing metadata and return the original
     * data.  Note that any data preceding an escaped start tag is assumed to be
     * part of a damaged frame, and is thus discarded.
     *
     * @return If the buffer contains a complete frame, the extracted, original
     * data; <code>null</code> otherwise.
     */
    protected byte[] processFrame () {

		//introduce the 1 bit count
		int numOfOneBits = 0;

		// Search for a start tag.  Discard anything prior to it.
		boolean        startTagFound = false;
		Iterator<Byte>             i = byteBuffer.iterator();
		while (!startTagFound && i.hasNext()) {
			byte current = i.next();
			if (current != startTag) {
				i.remove();
			} else {
				startTagFound = true;
				//count number of 1 bits in start tag
				numOfOneBits += Integer.bitCount((int)startTag);
			}
		}

		// If there is no start tag, then there is no frame.
		if (!startTagFound) {
			return null;
		}
		
		// Try to extract data while waiting for an unescaped stop tag.
		Queue<Byte> extractedBytes = new LinkedList<Byte>();
		boolean       stopTagFound = false;

		byte parityByte = 0b00000000; //will store the parityByte if found
		while (!stopTagFound && i.hasNext()) {

			// Grab the next byte.  If it is...
			//   (a) An escape tag: Skip over it and grab what follows as
			//                      literal data.
			//   (b) A stop tag:    Remove all processed bytes from the buffer, go to   
			//						the next element and take it as a parity byte and
			//                      end extraction.
			//   (c) A start tag:   All that precedes is damaged, so remove it
			//                      from the buffer and restart extraction.
			//   (d) Otherwise:     Take it as literal data.
			byte current = i.next();
			
			if (current == escapeTag) {
				if (i.hasNext()) {
					current = i.next();
					extractedBytes.add(current);
					//count the number of 1 bits both in esc and current row data byte
					numOfOneBits += Integer.bitCount((int)escapeTag);
					numOfOneBits += Integer.bitCount((int)current);
				} else {
					// An escape was the last byte available, so this is not a
					// complete frame.
					return null;
				}
			} else if (current == stopTag) {
				//if there is a byte after stop tag
				//take it as a parity byte
				if (i.hasNext())
				{
					parityByte = i.next();
				}
				//if there is no parity byte
				//then frame is incomplete
				else 
				{
					return null;
				}
				cleanBufferUpTo(i); //including the parity byte
				stopTagFound = true;
				//count the number of 1 bits in stop tag
				numOfOneBits += Integer.bitCount((int)stopTag);
			} else if (current == startTag) {
				cleanBufferUpTo(i);
				extractedBytes = new LinkedList<Byte>();
				numOfOneBits += Integer.bitCount((int)startTag);
			} else {
				extractedBytes.add(current);
				numOfOneBits += Integer.bitCount((int)current);
			}

		}

		// If there is no stop tag, then the frame is incomplete.
		if (!stopTagFound) {
			return null;
		}

		
		
		// Convert to the desired byte array.
		if (debug) {
			System.out.println("DumbDataLinkLayer.processFrame(): Got whole frame!");
		}

		byte[] extractedData = new byte[extractedBytes.size()];
		int                j = 0;
		i = extractedBytes.iterator();
		while (i.hasNext()) {
			extractedData[j] = i.next();
				if (debug) {
				System.out.printf("DumbDataLinkLayer.processFrame():\tbyte[%d] = %c\n",
						j,
						extractedData[j]);
				}
			j += 1;
		}

		// if the parity byte is satisfied
		if ((parityByte == evenParityByte && numOfOneBits % 2 == 0) ||
			(parityByte == oddParityByte  && numOfOneBits % 2 == 1))
		{
			return extractedData;
		}
		//if no parityByte found
		else if (parityByte == 0b00000000)
		{
			return null;
		}
		//if parityByte has changed from 00000000
		else
		{
			System.out.println("*************");
			System.out.println("THE FOLLOWING DATA OR ITS METADATA GOT CORRUPTED");
			for (int k = 0; k < extractedData.length; k++)
			{
				
				System.out.println(" Char Form => "+ ((char) extractedData[k])
				 + " | Byte Form => " + extractedData[k]);
				
			}
			System.out.println("*************");

			//CHANGEE THIS LATER
			return extractedData;
		}
    } // processFrame ()
    // ===============================================================



    // ===============================================================
    private void cleanBufferUpTo (Iterator<Byte> end) {

		Iterator<Byte> i = byteBuffer.iterator();
		while (i.hasNext() && i != end) {
			i.next();
			i.remove();
		}

    }
    // ===============================================================



    // ===============================================================
    // DATA MEMBERS
    // ===============================================================



    // ===============================================================
    // The start tag, stop tag, and the escape tag.
    private final byte startTag  = (byte)'{';
    private final byte stopTag   = (byte)'}';
	private final byte escapeTag = (byte)'\\';
	
	//The parity bytes
	private final byte oddParityByte = 0b01010101;
	private final byte evenParityByte = 0b00101010;
    // ===============================================================



// ===================================================================
} // class ParityDataLinkLayer
// ===================================================================
