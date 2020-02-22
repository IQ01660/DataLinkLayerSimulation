// =============================================================================
// IMPORTS
import java.lang.Math;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
// =============================================================================

//import com.oracle.jrockit.jfr.EventDefinition;


// =============================================================================
/**
 * @file   ParityDataLinkLayer.java
 * @author Ikram Gabiyev
 * @date   February 2020
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs parity byte error management.
 */
public class CRCDataLinkLayer extends DataLinkLayer {
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

            //return the next frame's raw data in bits as a linked list
            LinkedList<Integer> frameBits = this.nextFrameBits(data, currentByteIndex);

            //return integer formed from frameBits
            int msgDataInt = this.toDataInteger(frameBits);



            //calculate the CRC checksum of the next frame message
            byte checksum = this.calculateChecksum(msgDataInt, frameBits.size());

			//add a start tag to framingData => will have all frames in it
			framingData.add(startTag);
			System.out.println("<start>");
			
			//look into the raw data 8 times

			for(int j = 0; j < frameSize; j++)
			{
				if (currentByteIndex < data.length) 
				{
					// If the current data byte is itself a metadata tag, then precede
					// it with an escape tag.
					byte currentByte = data[currentByteIndex];
					
					
					
					if ((currentByte == startTag) ||
					(currentByte == stopTag) ||
					(currentByte == escapeTag)) {

						//add an escape tag before the special raw byte
						framingData.add(escapeTag);
						System.out.println("<esc>");
					}

					// Add the data byte itself.
					framingData.add(currentByte);
					System.out.println("[raw]");

					//incrementing the currentByteIndex
					//if I took the raw data
					currentByteIndex++;
				}
			}

			// End with a stop tag.
			framingData.add(stopTag);
			System.out.println("<stop>");
			
			/**
			 * The Frame Structure
			 * <start> --- <esc> --- <stop> 
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

		// Search for a start tag.  Discard anything prior to it.
		boolean        startTagFound = false;
		Iterator<Byte>             i = byteBuffer.iterator();
		while (!startTagFound && i.hasNext()) {
			byte current = i.next();
			if (current != startTag) {
				i.remove();
			} else {
				startTagFound = true;
			}
		}

		// If there is no start tag, then there is no frame.
		if (!startTagFound) {
			return null;
		}
		
		// Try to extract data while waiting for an unescaped stop tag.
		Queue<Byte> extractedBytes = new LinkedList<Byte>();
		boolean       stopTagFound = false;

		while (!stopTagFound && i.hasNext()) {

			// Grab the next byte.  If it is...
			//   (a) An escape tag: Skip over it and grab what follows as
			//                      literal data.
			//   (b) A stop tag:    Remove all processed bytes from the buffer
			//   (c) A start tag:   All that precedes is damaged, so remove it
			//                      from the buffer and restart extraction.
			//   (d) Otherwise:     Take it as literal data.
			byte current = i.next();
			
			if (current == escapeTag) {
				if (i.hasNext()) {
					current = i.next();
					extractedBytes.add(current);
				} else {
					// An escape was the last byte available, so this is not a
					// complete frame.
					return null;
				}
			} else if (current == stopTag) {

				cleanBufferUpTo(i); //including the parity byte
				stopTagFound = true;
				

			} else if (current == startTag) {

				cleanBufferUpTo(i);
				extractedBytes = new LinkedList<Byte>();
				
			} else {
				extractedBytes.add(current);
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

		return extractedData;
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

    /**
     * Calculates the CRC checksum for the provided message
     * The message is in the linked list in the form of bits
     * @param frameBits the bits of the next frame
     * Note: frameBits.remove(0) gives the first bit
     * @return
     */
    private byte calculateChecksum(int msgDataInt, int msgSize)
    {
        //determine the number of shifts to be made in total
        int shiftsMade = 0;

        //while the gen fits into remaining message
        while (shiftsMade < msgSize - numOfAppendedZeros)
        {
            
            msgDataInt = msgDataInt ^ generator;

            int to_be_shifted = numOfShifts(msgDataInt);

            msgDataInt = msgDataInt >> to_be_shifted;

            shiftsMade += to_be_shifted;
        }
        
        

        return (byte) msgDataInt;
    }

    private int numOfShifts(int msg)
    {
        //converts the byte into a binary string
        String binaryString = String.format("%8s", Integer.toBinaryString(msg & 0xFF)).replace(' ', '0');
        char[] binaryArr = binaryString.toCharArray();
        int toReturn = 0;

        for (int k = binaryArr.length - 1; k >= 0; k--)
        {
            if (binaryArr[k] == '1')
            {
                break;
            }
            else
            {
                toReturn++;
            }
            
        }
        
        
        return toReturn;
    }

    /**
     * @param frameBits the msg as in bits in a linked list
     * @return an integer representation of the message flipped
     */
    private int toDataInteger(LinkedList<Integer> frameBits)
    {
        //the highest degree of message polynomial
        int toReturn = 0;

        int highestPower = frameBits.size() - 1;

        while (frameBits.size() > 0)
        {
            int coefficient = frameBits.remove(frameBits.size() - 1);
            toReturn += coefficient * ( (int) Math.pow(2, highestPower) );
            highestPower--;
        }

        return toReturn;
    }

    /**
     * Takes the next frame to be considered
     * And return its raw data in a sequence of bits 
     * 
     * @param data the array of bytes with raw data
     * @param firstIndex the index of the first byte in the upcoming frame
     * @return
     */
    private LinkedList<Integer> nextFrameBits(byte[] data, int firstIndex)
    {
        //this will store added bits
        LinkedList<Integer> toReturn = new LinkedList<>();

        //set the current index of the byte considered
        //to the first index of the frame
        int currentByteIndex = firstIndex;

        for(int j = 0; j < frameSize; j++)
		{
            if (currentByteIndex < data.length) 
            {

                //stores the current byte coonsidered
                byte currentByte = data[currentByteIndex];

                //converts the byte into a binary string
                String binaryString = String.format("%8s", Integer.toBinaryString(currentByte & 0xFF)).replace(' ', '0');

                //converting the binary string into a char array
                char[] dataBits = binaryString.toCharArray();

                //goes through the binary string and adds bits to the LinkedList
                for (int charIndex = 0; charIndex < dataBits.length; charIndex++)
                {
                    toReturn.add(Character.getNumericValue(dataBits[charIndex]));
                }

                currentByteIndex++;
            }
		}


        return toReturn;
    }

    // ===============================================================
    // DATA MEMBERS
    // ===============================================================


    //the size of the frame (only raw data considered)
    private final int frameSize = 8;

    //flipped generator
    private final int generator = 0b1100001;
    private final int numOfAppendedZeros = 6;

    // ===============================================================
    // The start tag, stop tag, and the escape tag.
    private final byte startTag  = (byte)'{';
    private final byte stopTag   = (byte)'}';
	private final byte escapeTag = (byte)'\\';
	
    // ===============================================================



// ===================================================================
} // class ParityDataLinkLayer
// ===================================================================
