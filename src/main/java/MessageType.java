// An pseudo-enum of message prefixes that indicate what type of message a message is
// The associated character is inserted before the message content.
// It's not possible to use an actual enum here with a character constructor because the switch statements in the client
// and server only work on compile-time constants.
public class MessageType {
    public final static char SET_NAME = 'N'; // Used by the client to declare their name
    public final static char NAME_ACK = 'A'; // Used by the server to acknowledge the client sending their name
    public final static char CALCULATION_REQUEST = 'C'; // Used by the client to send an equation to be calculated
    public final static char CALCULATION_RESULT = 'R'; // Used by the server to reply with the result of a calculation
    public final static char END = 'E'; // Used by the client to close the connection
    public final static char ERROR = 'X'; // Used by either party when an error occurs, such as an invalid name in SET_NAME
}
