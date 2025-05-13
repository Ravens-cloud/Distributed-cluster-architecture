package job;

import core.KeyValue;
import java.util.*;

public abstract class MapReduceJob {
    public abstract List<KeyValue> map(String line);
    public abstract KeyValue reduce(String key, List<String> values);
}
