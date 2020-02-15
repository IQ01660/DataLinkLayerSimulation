// =============================================================================
/**
 * Transmits bits across a medium.
 * 
 * @file   PhysicalLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   August 2018, original September 2004
 */
public class PhysicalLayer {
// =============================================================================



    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================



    // =========================================================================
    /**
     * Create a physical layer and connect it to the given medium.
     *
     * @param medium The medium on which this physical layer will transmit and
     *               receive.
     * @return the newly created physical layer.
     */
    public static PhysicalLayer create (Medium medium) {

	PhysicalLayer physicalLayer = new PhysicalLayer(medium);
	return physicalLayer;

    } // create ()
    // =========================================================================



    // =========================================================================
    /**
     * The constructor.  Attach the new physical layer to the given medium.
     *
     * @param medium The medium through which this physical layer will signal.
     */
    public PhysicalLayer (Medium medium) {

	// Connect the client to the media.
	this.medium = medium;
	medium.register(this);

    } // PhysicalLayer ()
    // =========================================================================



    // =========================================================================
    /**
     * Allow a data link layer to register as the client of this physical layer.
     *
     * @param  client The data link layer that will use this physical layer.
     * @throws RuntimeException if there already is a client registered.
     */
    public void register (DataLinkLayer client) {

	// Is there already a client registered?
	if (this.client != null) {
	    throw new RuntimeException("Attempt to double-register");
	}

	// Hold a pointer to the client.
	this.client = client;

    } // register ()
    // =========================================================================



    // =========================================================================
    /**
     * Send a client's bit via the medium.
     *
     * @param bit The bit value to send.
     */
    public void send (boolean bit) {

	medium.transmit(this, bit);

    } // send ()
    // =========================================================================



    // =========================================================================
    /**
     * Called by the medium to deliver a bit, which is then in turn delivered to
     * the client data link layer.
     *
     * @param bit The bit received from the medium.
     */
    public void receive (boolean bit) {

	client.receive(bit);

    }
    // ===============================================================



    // ===============================================================
    // DATA MEMBERS

    /** The medium to which this layer is connected. */
    private Medium medium;

    /** The data link layer above this physical layer. */
    private DataLinkLayer client;
    // ===============================================================



// ===================================================================
} // class PhysicalLayer
// ===================================================================
