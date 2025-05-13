package job;

import core.KeyValue;
import java.util.List;

public class WordCountJob extends MapReduceJob {
    @Override
    public List<KeyValue> map(String line) {
        List<KeyValue> results = new java.util.ArrayList<>();
        String[] words = line.split("\\s+"); // 按空白字符分割单词

        for (String word : words) {
            if (!word.isEmpty()) {
                results.add(new KeyValue(word, "1")); // 输出 (word, 1)
            }
        }
        return results;
    }

    @Override
    public KeyValue reduce(String key, List<String> values) {
        int sum = values.stream()
                .mapToInt(Integer::parseInt)
                .sum(); // 累加所有值
        return new KeyValue(key, String.valueOf(sum));
    }
}