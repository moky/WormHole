/* license: https://mit-license.org
 *
 *  STUN: Session Traversal Utilities for NAT
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.stun.valus;

import java.nio.charset.Charset;

import chat.dim.stun.attributes.AttributeValue;
import chat.dim.tlv.Data;
import chat.dim.tlv.Length;
import chat.dim.tlv.Tag;

public class SoftwareValue extends AttributeValue {

    private final String description;

    public SoftwareValue(Data data, String description) {
        super(data);
        this.description = description;
    }

    public SoftwareValue(byte[] bytes, String description) {
        super(bytes);
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public static SoftwareValue create(String description) {
        byte[] data = description.getBytes(Charset.forName("UTF-8"));
        int tail = data.length & 3;
        if (tail > 0) {
            int len = data.length + (4 - tail);
            byte[] buffer = new byte[len];
            System.arraycopy(data, 0, buffer, 0, data.length);
            data = buffer;
        }
        return new SoftwareValue(data, description);
    }

    public static SoftwareValue parse(Data data, Tag type, Length length) {
        String desc = data.toString();
        return new SoftwareValue(data, desc.trim());
    }
}
