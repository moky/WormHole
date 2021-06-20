/* license: https://mit-license.org
 *
 *  DMTP: Direct Message Transfer Protocol
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
package chat.dim.dmtp.fields;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import chat.dim.dmtp.values.BinaryValue;
import chat.dim.dmtp.values.StringValue;
import chat.dim.dmtp.values.TimestampValue;
import chat.dim.dmtp.values.TypeValue;
import chat.dim.type.ByteArray;
import chat.dim.type.MutableData;

public class FieldsValue extends FieldValue implements Map<String, Object> {

    private final Map<String, Object> dictionary;

    public FieldsValue(ByteArray data, List<Field> fields) {
        super(data);
        dictionary = new HashMap<>();
        for (Field item : fields) {
            put(item.tag, item.value);
        }
    }

    public FieldsValue(List<Field> fields) {
        this(build(fields), fields);
    }

    private static ByteArray build(List<Field> fields) {
        int length = 0;
        for (Field item : fields) {
            length += item.getLength();
        }
        MutableData data = new MutableData(length);
        for (Field item : fields) {
            data.append(item);
        }
        return data;
    }

    @SuppressWarnings("unused")
    public static FieldsValue parse(ByteArray data, FieldName type, FieldLength length) {
        // parse fields
        List<Field> fields = Field.parseAll(data);
        return new FieldsValue(data, fields);
    }

    private void put(FieldName tag, FieldValue value) {
        if (value == null) {
            dictionary.remove(tag.name);
        } else {
            dictionary.put(tag.name, value);
        }
    }

    public Object get(FieldName tag) {
        return get(tag.name);
    }

    protected String getStringValue(FieldName tag) {
        StringValue value = (StringValue) get(tag.name);
        if (value == null) {
            return null;
        }
        return value.string;
    }

    protected int getTypeValue(FieldName tag) {
        TypeValue value = (TypeValue) get(tag.name);
        if (value == null) {
            return 0;
        }
        return value.value;
    }

    protected long getTimestampValue(FieldName tag) {
        TimestampValue value = (TimestampValue) get(tag.name);
        if (value == null) {
            return 0;
        }
        return value.value;
    }

    protected ByteArray getBinaryValue(FieldName tag) {
        return (BinaryValue) get(tag.name);
    }

    //
    //  Map interfaces
    //

    @Override
    public String toString() {
        return dictionary.toString();
    }

    @Override
    public int size() {
        return dictionary.size();
    }

    @Override
    public boolean isEmpty() {
        return dictionary.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof FieldName) {
            key = ((FieldName) key).name;
        }
        return dictionary.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return dictionary.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        if (key instanceof FieldName) {
            key = ((FieldName) key).name;
        }
        return dictionary.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("immutable!");
        //return dictionary.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("immutable!");
        //return dictionary.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("immutable!");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("immutable!");
    }

    @Override
    public Set<String> keySet() {
        return dictionary.keySet();
    }

    @Override
    public Collection<Object> values() {
        return dictionary.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return dictionary.entrySet();
    }
}
