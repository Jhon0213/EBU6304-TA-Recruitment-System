package edu.bupt.ta.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.bupt.ta.dto.AiJobMatchDTO;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.util.JsonUtils;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DeepSeekJobMatchService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12);
    private static final int MAX_CANDIDATE_JOBS = 20;
    private static final String DEFAULT_API_KEY = "sk-42e5fdc8fa8e439c86fde2298cc79fd7";
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int MAX_CV_TEXT_CHARS = 6_000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public List<AiJobMatchDTO> rankJobs(ApplicantProfile profile, ResumeInfo resume, List<Job> jobs) {
        String apiKey = resolveConfig("deepseek.api.key", "DEEPSEEK_API_KEY", DEFAULT_API_KEY);
        if (apiKey.isBlank() || jobs == null || jobs.isEmpty()) {
            return List.of();
        }

        try {
            String baseUrl = resolveConfig("deepseek.base.url", "DEEPSEEK_BASE_URL", DEFAULT_BASE_URL);
            String model = resolveConfig("deepseek.model", "DEEPSEEK_MODEL", DEFAULT_MODEL);
            String requestBody = buildRequestBody(model, profile, resume, jobs.stream()
                    .limit(MAX_CANDIDATE_JOBS)
                    .toList());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/v1/chat/completions"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return parseResponse(response.body());
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    private String buildRequestBody(String model, ApplicantProfile profile, ResumeInfo resume, List<Job> jobs) throws IOException {
        ObjectNode root = JsonUtils.mapper().createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.1);
        root.put("max_tokens", 900);

        ArrayNode messages = root.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "You score TA applicant/job fit. Return strict JSON only.");
        messages.addObject()
                .put("role", "user")
                .put("content", buildPrompt(profile, resume, jobs));

        return JsonUtils.mapper().writeValueAsString(root);
    }

    private String buildPrompt(ApplicantProfile profile, ResumeInfo resume, List<Job> jobs) throws IOException {
        ObjectNode input = JsonUtils.mapper().createObjectNode();
        ObjectNode applicant = input.putObject("applicant");
        applicant.put("fullName", value(profile == null ? null : profile.getFullName()));
        applicant.put("programme", value(profile == null ? null : profile.getProgramme()));
        applicant.put("year", profile == null ? 0 : profile.getYear());
        applicant.putPOJO("relevantModules", resume == null ? List.of() : safeList(resume.getRelevantModules()));
        applicant.putPOJO("technicalSkills", resume == null ? List.of() : safeList(resume.getTechnicalSkills()));
        applicant.putPOJO("languageSkills", resume == null ? List.of() : safeList(resume.getLanguageSkills()));
        applicant.put("experienceText", value(resume == null ? null : resume.getExperienceText()));
        applicant.put("personalStatement", value(resume == null ? null : resume.getPersonalStatement()));
        applicant.putPOJO("availability", resume == null ? List.of() : safeList(resume.getAvailability()));
        applicant.put("maxWeeklyHours", resume == null ? 0 : resume.getMaxWeeklyHours());
        applicant.put("uploadedCvFileName", value(resume == null ? null : resume.getCvFileName()));
        applicant.put("uploadedCvText", extractUploadedCvText(resume));

        ArrayNode jobNodes = input.putArray("jobs");
        for (Job job : jobs) {
            ObjectNode node = jobNodes.addObject();
            node.put("jobId", value(job.getJobId()));
            node.put("title", value(job.getTitle()));
            node.put("moduleCode", value(job.getModuleCode()));
            node.put("moduleName", value(job.getModuleName()));
            node.put("description", value(job.getDescription()));
            node.putPOJO("requiredSkills", safeList(job.getRequiredSkills()));
            node.putPOJO("preferredSkills", safeList(job.getPreferredSkills()));
            node.put("weeklyHours", job.getWeeklyHours());
            node.put("minimumAcademicGrade", value(job.getMinimumAcademicGrade()));
        }

        return """
                First summarize the applicant's strengths from the structured profile and uploaded CV text.
                Then score each job from 0 to 100 for this applicant. Consider CV skills, related modules,
                experience, availability, weekly hours, and job requirements.
                Return exactly this JSON shape with the two best jobs only:
                {"matches":[{"jobId":"J001","score":95,"reason":"short reason"}]}
                Do not include markdown or extra text.

                Input:
                """ + JsonUtils.mapper().writeValueAsString(input);
    }

    private List<AiJobMatchDTO> parseResponse(String body) throws IOException {
        JsonNode root = JsonUtils.mapper().readTree(body);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            return List.of();
        }

        JsonNode parsed = JsonUtils.mapper().readTree(extractJson(content));
        JsonNode matches = parsed.path("matches");
        if (!matches.isArray()) {
            return List.of();
        }

        List<AiJobMatchDTO> results = new ArrayList<>();
        for (JsonNode item : matches) {
            String jobId = item.path("jobId").asText("");
            if (jobId.isBlank()) {
                continue;
            }
            int score = Math.max(0, Math.min(100, item.path("score").asInt(0)));
            String reason = item.path("reason").asText("");
            results.add(new AiJobMatchDTO(jobId, score, reason));
        }
        return results.stream()
                .sorted(Comparator.comparingInt(AiJobMatchDTO::score).reversed())
                .limit(2)
                .toList();
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String resolveConfig(String propertyName, String envName, String fallback) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private String extractUploadedCvText(ResumeInfo resume) {
        if (resume == null || resume.getCvStoredPath() == null || resume.getCvStoredPath().isBlank()) {
            return "";
        }
        Path path = Path.of(resume.getCvStoredPath());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return "";
        }

        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            String text;
            if (fileName.endsWith(".docx")) {
                text = extractDocxText(path);
            } else if (fileName.endsWith(".pdf")) {
                text = extractPdfText(path);
            } else {
                text = Files.readString(path);
            }
            return limitText(text);
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    private String extractDocxText(Path path) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    zip.transferTo(out);
                    String xml = out.toString(StandardCharsets.UTF_8);
                    return xml.replaceAll("<w:tab\\s*/>", " ")
                            .replaceAll("</w:p>", "\n")
                            .replaceAll("<[^>]+>", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'")
                            .replaceAll("\\s+", " ")
                            .trim();
                }
            }
        }
        return "";
    }

    private String extractPdfText(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String content = new String(bytes, StandardCharsets.ISO_8859_1);
        Matcher matcher = Pattern.compile("\\(([^\\r\\n()]{3,})\\)").matcher(content);
        StringBuilder text = new StringBuilder();
        while (matcher.find() && text.length() < MAX_CV_TEXT_CHARS) {
            text.append(matcher.group(1).replace("\\)", ")").replace("\\(", "(")).append(' ');
        }
        if (text.isEmpty()) {
            for (String token : content.split("[^A-Za-z0-9+#.@/-]+")) {
                if (token.length() >= 3) {
                    text.append(token).append(' ');
                }
                if (text.length() >= MAX_CV_TEXT_CHARS) {
                    break;
                }
            }
        }
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    private String limitText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_CV_TEXT_CHARS
                ? normalized
                : normalized.substring(0, MAX_CV_TEXT_CHARS);
    }
}
