package com.bgpay.bgai.utils;

import com.bgpay.bgai.exception.FlinkJobException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class FlinkJobManager {
    private final String flinkRestUrl;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private BiConsumer<String, String> jobStatusCallback;

    public FlinkJobManager(String flinkRestUrl) {
        this.flinkRestUrl = flinkRestUrl;
        this.restTemplate = new RestTemplate();
    }

    /** 上传 jar 包，返回 jarId */
    public String uploadJar(String jarPath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("jarfile", new FileSystemResource(jarPath));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        Map resp = restTemplate.postForObject(flinkRestUrl + "/jars/upload", requestEntity, Map.class);
        String filename = (String) resp.get("filename");
        // filename like /opt/flink/usrlib/your-job.jar, jarId is the file name
        return filename.substring(filename.lastIndexOf("/") + 1);
    }

    /** 提交作业，支持更多参数，返回 jobId */
    public String runJar(String jarId, String entryClass, String programArgs, Integer parallelism,
                        String savepointPath, Boolean allowNonRestoredState, List<String> programArgsList, String jobName) throws FlinkJobException {
        try {
            Map<String, Object> params = new HashMap<>();
            if (entryClass != null) params.put("entryClass", entryClass);
            if (programArgs != null) params.put("programArgs", programArgs);
            if (parallelism != null) params.put("parallelism", parallelism);
            if (savepointPath != null) params.put("savepointPath", savepointPath);
            if (allowNonRestoredState != null) params.put("allowNonRestoredState", allowNonRestoredState);
            if (programArgsList != null) params.put("programArgsList", programArgsList);
            if (jobName != null) params.put("jobName", jobName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
            Map resp = restTemplate.postForObject(flinkRestUrl + "/jars/" + jarId + "/run", entity, Map.class);
            if (resp == null || !resp.containsKey("jobid")) throw new FlinkJobException("Flink runJar failed: " + resp);
            return (String) resp.get("jobid");
        } catch (Exception e) {
            throw new FlinkJobException("Flink runJar error", e);
        }
    }

    /** 查询所有作业 */
    public List<Map<String, Object>> listJobs() {
        Map resp = restTemplate.getForObject(flinkRestUrl + "/jobs/overview", Map.class);
        return (List<Map<String, Object>>) resp.get("jobs");
    }

    /** 查询作业状态 */
    public String getJobStatus(String jobId) {
        Map resp = restTemplate.getForObject(flinkRestUrl + "/jobs/" + jobId, Map.class);
        return (String) resp.get("state");
    }

    /** 取消作业 */
    public void cancelJob(String jobId) {
        restTemplate.patchForObject(flinkRestUrl + "/jobs/" + jobId, null, Map.class);
    }

    /** 获取作业异常信息 */
    public Map getJobExceptions(String jobId) throws FlinkJobException {
        try {
            return restTemplate.getForObject(flinkRestUrl + "/jobs/" + jobId + "/exceptions", Map.class);
        } catch (Exception e) {
            throw new FlinkJobException("Flink getJobExceptions error", e);
        }
    }

    /** 获取作业标准输出日志 */
    public String getJobStdout(String jobId) throws FlinkJobException {
        try {
            return restTemplate.getForObject(flinkRestUrl + "/jobs/" + jobId + "/stdout", String.class);
        } catch (Exception e) {
            throw new FlinkJobException("Flink getJobStdout error", e);
        }
    }

    /** 获取作业标准错误日志 */
    public String getJobStderr(String jobId) throws FlinkJobException {
        try {
            return restTemplate.getForObject(flinkRestUrl + "/jobs/" + jobId + "/stderr", String.class);
        } catch (Exception e) {
            throw new FlinkJobException("Flink getJobStderr error", e);
        }
    }

    /** 注册作业状态回调（jobId, status） */
    public void registerJobStatusCallback(BiConsumer<String, String> callback) {
        this.jobStatusCallback = callback;
    }

    /** 启动作业状态监控，定时回调 */
    public void startJobStatusMonitor(String jobId, long intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String status = getJobStatus(jobId);
                if (jobStatusCallback != null) {
                    jobStatusCallback.accept(jobId, status);
                }
                if ("FINISHED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
                    scheduler.shutdown();
                }
            } catch (Exception e) {
                // ignore
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }
}