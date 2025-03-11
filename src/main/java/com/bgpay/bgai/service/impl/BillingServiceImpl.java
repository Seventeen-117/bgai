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
import java.util.stream.IntStream;

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
    public void processBatch(List<UsageCalculationDTO> batch) {
        // Process each record in the batch in parallel
        batch.parallelStream()
                .forEach(this::processSingleRecord);
    }

    /**
     * Processes a single usage calculation DTO.
     * Checks if the record has been processed before, calculates costs, and saves the usage record.
     *
     * @param dto A single UsageCalculationDTO object to be processed.
     */
    private void processSingleRecord(UsageCalculationDTO dto) {
        // Check if the record has been processed
        checkProcessed(dto.getChatCompletionId());

        try {
            // Convert the UTC time to Beijing time
            ZonedDateTime beijingTime = convertToBeijingTime(dto.getCreatedAt());
            // Determine the time period (discount or standard)
            String timePeriod = determineTimePeriod(beijingTime);

            // Calculate the input cost
            BigDecimal inputCost = calculateInputCost(dto, timePeriod);
            // Calculate the output cost
            BigDecimal outputCost = calculateOutputCost(dto, timePeriod);

            // Save the usage record
            saveUsageRecord(dto, inputCost, outputCost);
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
                PriceConstants.OUTPUT_TYPE
        );

        // Get the price configuration from the cache
        PriceConfig config = priceCache.getPriceConfig(query);

        // Calculate the cost based on the number of tokens and the price per million tokens
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
                .divide(PriceConstants.ONE_MILLION, 6, RoundingMode.HALF_UP)
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
        // Get the local time from the Beijing time
        LocalTime time = beijingTime.toLocalTime();
        // Get the local date from the Beijing time
        LocalDate date = beijingTime.toLocalDate();

        // Create ZonedDateTime objects for the start and end of the discount period
        ZonedDateTime discountStart = ZonedDateTime.of(date, DISCOUNT_START, BEIJING_ZONE);
        ZonedDateTime discountEnd = ZonedDateTime.of(date, DISCOUNT_END, BEIJING_ZONE);

        // If the end time is before the start time, it means the discount period crosses midnight
        if (discountEnd.isBefore(discountStart)) {
            discountEnd = discountEnd.plusDays(1);
        }

        // Determine if the current time is within the discount period
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
        String cacheStatus = dto.getPromptCacheHitTokens() > 0 ? "hit" : "miss";
        // Create a price query object for input
        PriceQuery query = new PriceQuery(dto.getModelType(), timePeriod,
                cacheStatus, "input");
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
                .divide(PriceConstants.ONE_MILLION, 6, RoundingMode.HALF_UP)
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
                                BigDecimal outputCost) {
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

        UsageRecord record = convertToEntity(dto, inputCost, outputCost);
        usageRecordService.insetUsageRecord(record);
    }

    /**
     * Processes a large batch of usage calculation DTOs by dividing them into smaller sub - batches.
     * Applies rate limiting using a semaphore.
     *
     * @param batch A large list of UsageCalculationDTO objects to be processed.
     */
    public void processLargeBatch(List<UsageCalculationDTO> batch) {
        // Define the size of each sub - batch
        int BATCH_SIZE = 500;
        // Partition the large batch into smaller sub - batches
        List<List<UsageCalculationDTO>> partitions = Lists.partition(batch, BATCH_SIZE);

        // Process each sub - batch in parallel
        partitions.parallelStream().forEach(subBatch -> {
            try {
                // Try to acquire a permit from the semaphore for rate limiting
                if (!acquireSemaphore()) {
                    // Throw a BillingException if the permit cannot be acquired
                    throw new BillingException();
                }
            } catch (InterruptedException e) {
                // Throw a RuntimeException if the thread is interrupted
                throw new RuntimeException(e);
            }
            try {
                // Execute the processing of the sub - batch within a transaction
                transactionTemplate.execute(status -> {
                    processBatch(subBatch);
                    return null;
                });
            } finally {
                // Release the permit from the semaphore
                releaseSemaphore();
            }
        });
    }

    // Semaphore for rate limiting, with a maximum of 20 concurrent requests
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
     * Converts a UsageCalculationDTO object to a UsageRecord entity.
     *
     * @param dto        The UsageCalculationDTO to be converted.
     * @param inputCost  The calculated input cost.
     * @param outputCost The calculated output cost.
     * @return The converted UsageRecord entity.
     */
    private UsageRecord convertToEntity(UsageCalculationDTO dto, BigDecimal inputCost, BigDecimal outputCost) {
        UsageRecord record = new UsageRecord();
        // Set the model ID (temporarily set to 1, can be modified according to actual situation)
        record.setModelId(1);
        // Set the chat completion ID
        record.setChatCompletionId(dto.getChatCompletionId());
        // Set the input cost
        record.setInputCost(inputCost);
        // Set the output cost
        record.setOutputCost(outputCost);
        // Set the price version (temporarily set to 1, can be modified according to actual situation)
        record.setPriceVersion(1);
        // Set the calculation time
        record.setCalculatedAt(LocalDateTime.now());
        return record;
    }
}