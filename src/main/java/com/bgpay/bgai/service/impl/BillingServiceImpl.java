package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.entity.*;
import com.bgpay.bgai.exception.BillingException;
import com.bgpay.bgai.service.*;
import com.google.common.collect.Lists;
import com.sun.jdi.request.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.bgpay.bgai.entity.PriceConstants.*;

/**
 * This service class is responsible for handling billing-related operations,
 * including processing batches of usage records, calculating costs, and saving usage records.
 */
@Component
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {
    // Service for caching price information
    private final PriceCacheService priceCache;
    // Service for handling usage records
    private final UsageRecordService usageRecordService;
    // Template for managing transactions
    private final TransactionTemplate transactionTemplate;

    // Time zone of Beijing
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    // Start time of the discount period
    private static final LocalTime DISCOUNT_START = LocalTime.of(0, 30);
    // End time of the discount period
    private static final LocalTime DISCOUNT_END = LocalTime.of(8, 30);

    /**
     * Asynchronously processes a batch of usage calculation DTOs.
     * This method will retry up to 3 times if it fails, with a 1-second backoff between each attempt.
     *
     * @param batch A list of UsageCalculationDTO objects to be processed.
     */
    @Override
    @Async("billingExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processBatch(List<UsageCalculationDTO> batch, String userId) {
        // Process each record in the batch in parallel
        batch.parallelStream()
                .forEach(dto -> processSingleRecord(dto, userId));
    }

    /**
     * Processes a single usage calculation DTO.
     * Checks if the record has been processed before, calculates costs, and saves the usage record.
     *
     * @param dto A single UsageCalculationDTO object to be processed.
     */
    public void processSingleRecord(UsageCalculationDTO dto, String userId) {
        checkProcessed(dto.getChatCompletionId());

        try {
            ZonedDateTime beijingTime = convertToBeijingTime(dto.getCreatedAt());
            String timePeriod = determineTimePeriod(beijingTime);
            // Calculate the input cost
            BigDecimal inputCost = calculateInputCost(dto, timePeriod);
            // Calculate the output cost
            BigDecimal outputCost = calculateOutputCost(dto, timePeriod);

            // Save the usage record
            saveUsageRecord(dto, inputCost, outputCost, userId,timePeriod);
        } catch (Exception e) {
            // Log the error if processing fails
            log.error("Process failed for {}", dto.getChatCompletionId(), e);
            // Throw a BillingException if an error occurs
            throw new BillingException("Billing processing failed", e);
        }
    }

    /**
     * Calculates the output cost based on the usage and time period.
     *
     * @param usage      The UsageCalculationDTO containing usage information.
     * @param timePeriod The time period (discount or standard).
     * @return The calculated output cost.
     */
    private BigDecimal calculateOutputCost(UsageCalculationDTO usage, String timePeriod) {
        // Create a price query object for output
        PriceQuery query = new PriceQuery(
                usage.getModelType(),
                timePeriod,
                null,
                OUTPUT_TYPE
        );

        PriceConfig config = priceCache.getPriceConfig(query);
        if (config == null) {
            throw new BillingException("Price config not found");
        } else if (!(config instanceof PriceConfig)) { // 冗余检查，实际由 RedisTemplate 保证类型
            log.error("Invalid cache data type: {}", config.getClass());
            priceCache.refreshCacheByModel(usage.getModelType()); // 触发缓存刷新
            return calculateOutputCost(usage, timePeriod); // 重试
        }
        return calculateCost(usage.getCompletionTokens(), config.getPrice());
    }

    /**
     * Calculates the cost based on the number of tokens and the price per million tokens.
     *
     * @param tokens         The number of tokens.
     * @param pricePerMillion The price per million tokens.
     * @return The calculated cost.
     */
    private BigDecimal calculateCost(int tokens, BigDecimal pricePerMillion) {
        return BigDecimal.valueOf(tokens)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(pricePerMillion)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Converts the given UTC time to Beijing time.
     *
     * @param utcTime The UTC time to be converted.
     * @return The converted Beijing time.
     */
    private ZonedDateTime convertToBeijingTime(LocalDateTime utcTime) {
        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(BEIJING_ZONE);
    }

    /**
     * Determines the time period (discount or standard) based on the given Beijing time.
     *
     * @param beijingTime The Beijing time.
     * @return The determined time period ("discount" or "standard").
     */
    private String determineTimePeriod(ZonedDateTime beijingTime) {
        LocalDate date = beijingTime.toLocalDate();

        ZonedDateTime discountStart = ZonedDateTime.of(date, DISCOUNT_START, BEIJING_ZONE);
        ZonedDateTime discountEnd = ZonedDateTime.of(date, DISCOUNT_END, BEIJING_ZONE);

        if (discountEnd.isBefore(discountStart)) {
            discountEnd = discountEnd.plusDays(1);
        }

        return (beijingTime.isAfter(discountStart) && beijingTime.isBefore(discountEnd))
                ? "discount" : "standard";
    }

    /**
     * Calculates the input cost based on the usage and time period.
     *
     * @param dto        The UsageCalculationDTO containing usage information.
     * @param timePeriod The time period (discount or standard).
     * @return The calculated input cost.
     */
    private BigDecimal calculateInputCost(UsageCalculationDTO dto, String timePeriod) {
        // Determine the cache status based on the number of cached tokens
        String cacheStatus = dto.getPromptCacheHitTokens() > 0 ? CACHE_HIT : CACHE_MISS;
        // Create a price query object for input
        PriceQuery query = new PriceQuery(dto.getModelType(), timePeriod,
                cacheStatus, INPUT_TYPE);

        // Get the price configuration from the cache
        PriceConfig config = priceCache.getPriceConfig(query);

        // Calculate the total number of tokens
        int totalTokens = dto.getPromptCacheHitTokens() +
                dto.getPromptCacheMissTokens();
        // Calculate the cost based on the total number of tokens and the price
        return calculateTokenCost(totalTokens, config.getPrice());
    }

    /**
     * Calculates the token cost based on the number of tokens and the price.
     *
     * @param tokens The number of tokens.
     * @param price  The price per token.
     * @return The calculated token cost.
     */
    private BigDecimal calculateTokenCost(int tokens, BigDecimal price) {
        return BigDecimal.valueOf(tokens)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP)
                .multiply(price)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Checks if a record with the given completion ID has been processed.
     * Throws a DuplicateRequestException if the record has been processed.
     *
     * @param completionId The completion ID to check.
     */
    private void checkProcessed(String completionId) {
        if (usageRecordService.findByCompletionId(completionId) != null) {
            throw new DuplicateRequestException("Request has been processed");
        }
    }

    /**
     * Saves the usage record to the database within a transaction.
     * If the record already exists, it logs a warning and returns.
     *
     * @param dto        The UsageCalculationDTO containing usage information.
     * @param inputCost  The calculated input cost.
     * @param outputCost The calculated output cost.
     */
    @Transactional(timeout = 30, rollbackFor = Exception.class)
    public void saveUsageRecord(UsageCalculationDTO dto,
                                BigDecimal inputCost,
                                BigDecimal outputCost,
                                String userId,
                                String timePeriod) {
        if (usageRecordService.existsByCompletionId(dto.getChatCompletionId())) {
            log.warn("重复请求: {}", dto.getChatCompletionId());
            return;
        }

        // 记录详细日志
        log.info("计费记录 - 会话ID:{} 模型类型:{} 输入费用:{} 输出费用:{}",
                dto.getChatCompletionId(),
                dto.getModelType(),
                inputCost,
                outputCost);

        UsageRecord record = convertToEntity(dto, inputCost, outputCost, userId,timePeriod);
        usageRecordService.insetUsageRecord(record);
    }

    /**
     * Processes a large batch of usage calculation DTOs by dividing them into smaller sub - batches.
     * Applies rate limiting using a semaphore.
     *
     * @param batch A large list of UsageCalculationDTO objects to be processed.
     */
    public void processLargeBatch(List<UsageCalculationDTO> batch, String userId) {
        int BATCH_SIZE = 500;
        List<List<UsageCalculationDTO>> partitions = Lists.partition(batch, BATCH_SIZE);
        partitions.parallelStream().forEach(subBatch -> {
            try {
                if (!acquireSemaphore()) {
                    throw new BillingException();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                transactionTemplate.execute(status -> {
                    processBatch(subBatch, userId);
                    return null;
                });
            } finally {
                releaseSemaphore();
            }
        });
    }
    private final Semaphore semaphore = new Semaphore(20);

    /**
     * Tries to acquire a permit from the semaphore within 1 second.
     *
     * @return true if the permit is acquired, false otherwise.
     * @throws InterruptedException If the thread is interrupted while waiting for the permit.
     */
    private boolean acquireSemaphore() throws InterruptedException {
        return semaphore.tryAcquire(1, TimeUnit.SECONDS);
    }

    /**
     * Releases a permit back to the semaphore.
     */
    private void releaseSemaphore() {
        semaphore.release();
    }
    /**
     * Retrieves the price version based on the provided usage calculation DTO and time period.
     *
     * This method constructs a price query using the model type from the DTO, the given time period,
     * a null cache status, and the output type. It then fetches the corresponding price configuration
     * from the price cache service. If the price configuration is found, it extracts and returns the
     * price version. If the configuration is null, a BillingException is thrown. If the retrieved
     * object is not of the expected PriceConfig type, an error is logged, and the cache for the
     * relevant model is refreshed.
     *
     * @param dto        The UsageCalculationDTO containing the model type information.
     * @param timePeriod The time period for which the price configuration is to be retrieved.
     * @return The price version from the retrieved price configuration.
     * @throws BillingException If the price configuration is not found in the cache.
     */
    private Integer getPriceVersion(UsageCalculationDTO dto, String timePeriod) {

        // Create a price query object for output
        PriceQuery query = new PriceQuery(
                dto.getModelType(),
                timePeriod,
                null,
                OUTPUT_TYPE
        );

        PriceConfig config = priceCache.getPriceConfig(query);

        if (config == null) {
            throw new BillingException("Price config not found");
        } else if (!(config instanceof PriceConfig)) {
            log.error("refresh Cache config by ModelType: {}", dto.getModelType());
            priceCache.refreshCacheByModel(dto.getModelType());
        }
        Integer cachedVersion =config.getVersion();
        log.info("Output price config priceVersion: {}", cachedVersion);
        return cachedVersion;
    }
    /**
     * Converts a UsageCalculationDTO object to a UsageRecord entity.
     *
     * @param dto        The UsageCalculationDTO to be converted.
     * @param inputCost  The calculated input cost.
     * @param outputCost The calculated output cost.
     * @return The converted UsageRecord entity.
     */
    private UsageRecord convertToEntity(UsageCalculationDTO dto, BigDecimal inputCost, BigDecimal outputCost, String userId,String timePeriod) {
        UsageRecord record = new UsageRecord();
        record.setModelType(dto.getModelType());
        record.setChatCompletionId(dto.getChatCompletionId());
        record.setUserId(userId);
        record.setInputCost(inputCost);
        record.setOutputCost(outputCost);
        Integer priceVersion=getPriceVersion(dto,timePeriod);
        record.setPriceVersion(priceVersion);
        record.setCalculatedAt(LocalDateTime.now());
        return record;
    }
}