package org.projectodd.nodej.bindings.buffer;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.dynjs.exception.ThrowException;
import org.dynjs.runtime.AbstractNativeFunction;
import org.dynjs.runtime.DynObject;
import org.dynjs.runtime.ExecutionContext;
import org.dynjs.runtime.GlobalObject;
import org.dynjs.runtime.Types;
import org.projectodd.nodej.bindings.buffer.prototype.Copy;
import org.projectodd.nodej.bindings.buffer.prototype.Fill;
import org.projectodd.nodej.bindings.buffer.prototype.ToString;
import org.projectodd.nodej.bindings.buffer.prototype.Write;

public class Buffer extends DynObject {
    private final byte[] buffer;
    
    public enum Encoding {
        UTF8, UCS2, ASCII, BASE64, BINARY
    }
    
    public Buffer(final GlobalObject globalObject, long length) {
        super(globalObject);
        buffer = new byte[(int) length];
        setClassName("SlowBuffer");
        put(null, "length", length, false);
        defineReadOnlyProperty(globalObject, "copy", new Copy(globalObject));
        defineReadOnlyProperty(globalObject, "fill", new Fill(globalObject));
        defineReadOnlyProperty(globalObject, "utf8Write", new Write(globalObject, Buffer.Encoding.UTF8));
        defineReadOnlyProperty(globalObject, "asciiWrite", new Write(globalObject, Buffer.Encoding.ASCII));
        defineReadOnlyProperty(globalObject, "toString", new ToString(globalObject));
        defineReadOnlyProperty(globalObject, "byteAt", new AbstractNativeFunction(globalObject) {
            
            @Override
            public Object call(ExecutionContext context, Object self, Object... args) {
                if (!(args[0] instanceof Long)) {
                    throw new ThrowException(context, "Arg must be an integer.");
                }
                int index = Types.toNumber(context, args[0]).intValue();
                if (index < 0 || buffer.length <= index) {
                    return Types.UNDEFINED;
                }
                return buffer[index];
            }
        });
    }
    
    public String toString() {
        try {
            return new String(buffer, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new String(buffer);
        }
    }
    
    public void fill(byte b, int offset, int end) {
        if (offset >= buffer.length ) {
            return;
        }
        if (end > buffer.length) {
            throw new ThrowException(null, "end cannot be longer than length");
        }
        for (int i=offset; i < end; i++) {
            buffer[i] = b;
        }
    }
    
    public long copy(byte[] source, int targetStart, int sourceStart, int sourceEnd) {
        if (sourceEnd == sourceStart) {
            return 0L;
        }
        if (sourceStart > sourceEnd) {
            throw new ThrowException(null, "sourceEnd < sourceStart");
        }
        if (targetStart >= buffer.length) {
            throw new ThrowException(null, "targetStart out of bounds");
        }
        if (sourceStart >= source.length) {
            throw new ThrowException(null, "sourceStart out of bounds");
        }
        if (sourceEnd > source.length) {
            throw new ThrowException(null, "sourceEnd out of bounds");
        }
        int counter = 0;
        for (int i = sourceStart; i < sourceEnd; i++) {
            buffer[targetStart+counter] = source[i];
            counter++;
        }
        return counter;
    }
    
    public byte[] getBuffer() {
        return this.buffer;
    }

    public long write(String string, Encoding encoding, int offset, int maxLength) {
        int length = string.length();
        if (length == 0) { return 0; }
        if (length > 0 && offset > buffer.length) {
            throw new ThrowException(null, "Offset is out of bounds");
        }
        long bytesWritten = 0;
        switch(encoding) {
        case UTF8: 
            bytesWritten = this.copy(string.getBytes(Charset.forName("UTF-8")), offset, 0, maxLength); 
            break;
        case ASCII:
            bytesWritten = this.copy(string.getBytes(Charset.forName("US-ASCII")), offset, 0, maxLength); 
            break;
        }
        return bytesWritten;
    }
}