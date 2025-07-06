package com.bgpay.bgai.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据比较工具类
 * 用于比较服务返回数据与数据库数据是否一致
 */
public class DataComparisonUtils {

    private static final Logger log = LoggerFactory.getLogger(DataComparisonUtils.class);
    
    /**
     * 比较两个对象的字段值是否相等
     * 
     * @param serviceData 服务返回的数据对象
     * @param dbData 数据库数据对象
     * @param fieldNames 需要比较的字段名列表，为空则比较所有字段
     * @param <T> 对象类型
     * @return 比较结果，包含不一致的字段信息
     */
    public static <T> ComparisonResult compareObjects(T serviceData, T dbData, String... fieldNames) {
        ComparisonResult result = new ComparisonResult();
        if (serviceData == null && dbData == null) {
            result.setMatched(true);
            return result;
        }
        
        if (serviceData == null || dbData == null) {
            result.setMatched(false);
            result.addMismatch("整体对象", serviceData, dbData);
            return result;
        }
        
        try {
            List<String> fieldsToCompare;
            if (fieldNames == null || fieldNames.length == 0) {
                // 比较所有字段
                fieldsToCompare = getAllFieldNames(serviceData.getClass());
            } else {
                fieldsToCompare = Arrays.asList(fieldNames);
            }
            
            for (String fieldName : fieldsToCompare) {
                Field field = findField(serviceData.getClass(), fieldName);
                if (field == null) {
                    log.warn("字段{}在{}中不存在", fieldName, serviceData.getClass().getName());
                    continue;
                }
                
                field.setAccessible(true);
                Object serviceValue = field.get(serviceData);
                Object dbValue = field.get(dbData);
                
                if (!areValuesEqual(serviceValue, dbValue)) {
                    result.addMismatch(fieldName, serviceValue, dbValue);
                }
            }
            
        } catch (Exception e) {
            log.error("比较对象时发生错误", e);
            result.setError(e);
        }
        
        result.setMatched(result.getMismatches().isEmpty());
        return result;
    }
    
    /**
     * 比较两个集合中对象的字段值是否相等
     * 
     * @param serviceDataList 服务返回的数据集合
     * @param dbDataList 数据库数据集合
     * @param keyExtractor 提取对象标识键的函数，用于匹配对象
     * @param fieldNames 需要比较的字段名列表，为空则比较所有字段
     * @param <T> 对象类型
     * @param <K> 键类型
     * @return 比较结果，包含不一致的对象和字段信息
     */
    public static <T, K> CollectionComparisonResult<T, K> compareCollections(
            Collection<T> serviceDataList,
            Collection<T> dbDataList,
            KeyExtractor<T, K> keyExtractor,
            String... fieldNames) {
        
        CollectionComparisonResult<T, K> result = new CollectionComparisonResult<>();
        
        if (serviceDataList == null && dbDataList == null) {
            result.setMatched(true);
            return result;
        }
        
        if (serviceDataList == null || dbDataList == null) {
            result.setMatched(false);
            result.setMissingCount(serviceDataList == null ? (dbDataList.size()) : 0);
            result.setExtraCount(dbDataList == null ? (serviceDataList.size()) : 0);
            return result;
        }
        
        // 创建数据库数据的映射，用于快速查找
        Map<K, T> dbDataMap = new HashMap<>();
        for (T item : dbDataList) {
            K key = keyExtractor.extractKey(item);
            dbDataMap.put(key, item);
        }
        
        // 创建服务数据的映射，用于快速查找
        Map<K, T> serviceDataMap = new HashMap<>();
        for (T item : serviceDataList) {
            K key = keyExtractor.extractKey(item);
            serviceDataMap.put(key, item);
        }
        
        // 检查服务数据中的每个项目
        for (Map.Entry<K, T> entry : serviceDataMap.entrySet()) {
            K key = entry.getKey();
            T serviceItem = entry.getValue();
            T dbItem = dbDataMap.get(key);
            
            if (dbItem == null) {
                // 数据库中缺少此项
                result.addExtraItem(key, serviceItem);
            } else {
                // 比较对象字段
                ComparisonResult itemResult = compareObjects(serviceItem, dbItem, fieldNames);
                if (!itemResult.isMatched()) {
                    result.addMismatchedItem(key, serviceItem, dbItem, itemResult);
                }
            }
        }
        
        // 检查数据库中有但服务数据中没有的项目
        for (Map.Entry<K, T> entry : dbDataMap.entrySet()) {
            K key = entry.getKey();
            if (!serviceDataMap.containsKey(key)) {
                result.addMissingItem(key, entry.getValue());
            }
        }
        
        result.setMatched(
                result.getMismatchedItems().isEmpty() && 
                result.getMissingItems().isEmpty() && 
                result.getExtraItems().isEmpty());
        
        return result;
    }
    
    /**
     * 断言两个对象的指定字段相等
     * 
     * @param serviceData 服务返回的数据对象
     * @param dbData 数据库数据对象
     * @param fieldNames 需要比较的字段名列表，为空则比较所有字段
     * @param <T> 对象类型
     */
    public static <T> void assertObjectsEqual(T serviceData, T dbData, String... fieldNames) {
        ComparisonResult result = compareObjects(serviceData, dbData, fieldNames);
        
        if (!result.isMatched()) {
            StringBuilder sb = new StringBuilder("对象比较失败:\n");
            for (FieldMismatch mismatch : result.getMismatches()) {
                sb.append(String.format("字段 '%s' 不匹配: 服务值='%s', 数据库值='%s'\n", 
                        mismatch.getFieldName(), mismatch.getServiceValue(), mismatch.getDbValue()));
            }
            Assert.fail(sb.toString());
        }
    }
    
    /**
     * 断言两个集合中的对象相等
     * 
     * @param serviceDataList 服务返回的数据集合
     * @param dbDataList 数据库数据集合
     * @param keyExtractor 提取对象标识键的函数
     * @param fieldNames 需要比较的字段名列表，为空则比较所有字段
     * @param <T> 对象类型
     * @param <K> 键类型
     */
    public static <T, K> void assertCollectionsEqual(
            Collection<T> serviceDataList,
            Collection<T> dbDataList,
            KeyExtractor<T, K> keyExtractor,
            String... fieldNames) {
        
        CollectionComparisonResult<T, K> result = compareCollections(
                serviceDataList, dbDataList, keyExtractor, fieldNames);
        
        if (!result.isMatched()) {
            StringBuilder sb = new StringBuilder("集合比较失败:\n");
            
            if (!result.getMissingItems().isEmpty()) {
                sb.append("服务数据中缺少的项: ").append(result.getMissingItems().size()).append("\n");
                for (Map.Entry<K, T> entry : result.getMissingItems().entrySet()) {
                    sb.append("  键: ").append(entry.getKey()).append("\n");
                }
            }
            
            if (!result.getExtraItems().isEmpty()) {
                sb.append("服务数据中多余的项: ").append(result.getExtraItems().size()).append("\n");
                for (Map.Entry<K, T> entry : result.getExtraItems().entrySet()) {
                    sb.append("  键: ").append(entry.getKey()).append("\n");
                }
            }
            
            if (!result.getMismatchedItems().isEmpty()) {
                sb.append("字段值不匹配的项: ").append(result.getMismatchedItems().size()).append("\n");
                for (ItemMismatch<K, T> mismatch : result.getMismatchedItems()) {
                    sb.append("  键: ").append(mismatch.getKey()).append("\n");
                    for (FieldMismatch fieldMismatch : mismatch.getResult().getMismatches()) {
                        sb.append("    字段 '").append(fieldMismatch.getFieldName())
                          .append("' 不匹配: 服务值='").append(fieldMismatch.getServiceValue())
                          .append("', 数据库值='").append(fieldMismatch.getDbValue()).append("'\n");
                    }
                }
            }
            
            Assert.fail(sb.toString());
        }
    }
    
    /**
     * 判断两个值是否相等，处理特殊类型的比较
     */
    private static boolean areValuesEqual(Object value1, Object value2) {
        if (value1 == value2) {
            return true;
        }
        
        if (value1 == null || value2 == null) {
            return false;
        }
        
        // 处理BigDecimal的比较
        if (value1 instanceof BigDecimal && value2 instanceof BigDecimal) {
            return ((BigDecimal) value1).compareTo((BigDecimal) value2) == 0;
        }
        
        // 处理时间的比较
        if (value1 instanceof LocalDateTime && value2 instanceof LocalDateTime) {
            // 可以根据需要调整精度
            return ((LocalDateTime) value1).isEqual((LocalDateTime) value2);
        }
        
        // 处理数组的比较
        if (value1.getClass().isArray() && value2.getClass().isArray()) {
            return Arrays.deepEquals(new Object[]{value1}, new Object[]{value2});
        }
        
        // 处理集合的比较
        if (value1 instanceof Collection && value2 instanceof Collection) {
            return new HashSet<>((Collection<?>) value1).equals(new HashSet<>((Collection<?>) value2));
        }
        
        // 默认比较
        return value1.equals(value2);
    }
    
    /**
     * 获取类的所有字段名
     */
    private static List<String> getAllFieldNames(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                fieldNames.add(field.getName());
            }
            clazz = clazz.getSuperclass();
        }
        return fieldNames;
    }
    
    /**
     * 递归查找类的字段，包括父类
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * 对象比较结果
     */
    public static class ComparisonResult {
        private boolean matched;
        private Exception error;
        private final List<FieldMismatch> mismatches = new ArrayList<>();
        
        public boolean isMatched() {
            return matched;
        }
        
        public void setMatched(boolean matched) {
            this.matched = matched;
        }
        
        public Exception getError() {
            return error;
        }
        
        public void setError(Exception error) {
            this.error = error;
        }
        
        public List<FieldMismatch> getMismatches() {
            return mismatches;
        }
        
        public void addMismatch(String fieldName, Object serviceValue, Object dbValue) {
            mismatches.add(new FieldMismatch(fieldName, serviceValue, dbValue));
        }
    }
    
    /**
     * 字段不匹配信息
     */
    public static class FieldMismatch {
        private final String fieldName;
        private final Object serviceValue;
        private final Object dbValue;
        
        public FieldMismatch(String fieldName, Object serviceValue, Object dbValue) {
            this.fieldName = fieldName;
            this.serviceValue = serviceValue;
            this.dbValue = dbValue;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public Object getServiceValue() {
            return serviceValue;
        }
        
        public Object getDbValue() {
            return dbValue;
        }
    }
    
    /**
     * 集合比较结果
     */
    public static class CollectionComparisonResult<T, K> {
        private boolean matched;
        private final Map<K, T> missingItems = new HashMap<>();
        private final Map<K, T> extraItems = new HashMap<>();
        private final List<ItemMismatch<K, T>> mismatchedItems = new ArrayList<>();
        private int missingCount;
        private int extraCount;
        
        public boolean isMatched() {
            return matched;
        }
        
        public void setMatched(boolean matched) {
            this.matched = matched;
        }
        
        public Map<K, T> getMissingItems() {
            return missingItems;
        }
        
        public Map<K, T> getExtraItems() {
            return extraItems;
        }
        
        public List<ItemMismatch<K, T>> getMismatchedItems() {
            return mismatchedItems;
        }
        
        public void addMissingItem(K key, T item) {
            missingItems.put(key, item);
            missingCount++;
        }
        
        public void addExtraItem(K key, T item) {
            extraItems.put(key, item);
            extraCount++;
        }
        
        public void addMismatchedItem(K key, T serviceItem, T dbItem, ComparisonResult result) {
            mismatchedItems.add(new ItemMismatch<>(key, serviceItem, dbItem, result));
        }
        
        public int getMissingCount() {
            return missingCount;
        }
        
        public void setMissingCount(int missingCount) {
            this.missingCount = missingCount;
        }
        
        public int getExtraCount() {
            return extraCount;
        }
        
        public void setExtraCount(int extraCount) {
            this.extraCount = extraCount;
        }
    }
    
    /**
     * 项目不匹配信息
     */
    public static class ItemMismatch<K, T> {
        private final K key;
        private final T serviceItem;
        private final T dbItem;
        private final ComparisonResult result;
        
        public ItemMismatch(K key, T serviceItem, T dbItem, ComparisonResult result) {
            this.key = key;
            this.serviceItem = serviceItem;
            this.dbItem = dbItem;
            this.result = result;
        }
        
        public K getKey() {
            return key;
        }
        
        public T getServiceItem() {
            return serviceItem;
        }
        
        public T getDbItem() {
            return dbItem;
        }
        
        public ComparisonResult getResult() {
            return result;
        }
    }
    
    /**
     * 键提取器接口
     */
    public interface KeyExtractor<T, K> {
        K extractKey(T item);
    }
}
