/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 17, 2004
 *
 */
package v9t9.engine.cpu;


/**
 * @author ejs
 */
public interface Operand {
    // Operand changes
    public static final int OP_DEST_FALSE = 0;
    public static final int OP_DEST_TRUE = 1;
    public static final int OP_DEST_KILLED = 2;
}
