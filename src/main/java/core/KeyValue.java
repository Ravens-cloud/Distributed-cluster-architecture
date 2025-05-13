package core;

import java.io.Serializable;

public class KeyValue implements Serializable {
    private static final long serialVersionUID = 1L;

    public String key;
    public String value;

    public KeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key + ":" + value;
    }
}
