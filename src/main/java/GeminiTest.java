import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class GeminiTest {
    public static void main(String[] args) throws Exception {
        analyze();
    }

    public static void analyze() throws Exception {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("APIキーが設定されていません。環境変数GEMINI_API_KEYを設定してください。");
        }

        String prompt = Files.readString(Path.of("ai_input.txt"));

        ObjectMapper requestMapper = new ObjectMapper();
        String json = requestMapper.writeValueAsString(
                requestMapper.createObjectNode()
                        .put("model", "gemini-3.5-flash-lite")
                        .put("input", prompt));
        

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/interactions"))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response;
        try {
            int attempt = 1;
            while (true) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 503 || attempt == 3) {
                    break;
                }

                int waitSeconds = attempt * 2;
                System.out.println("HTTP 503：" + waitSeconds + "秒後に再試行します（リクエスト "
                        + (attempt + 1) + "/3回目）。");
                Thread.sleep(waitSeconds * 1000L);
                attempt++;
            }
        } catch (HttpTimeoutException e) {
            throw new IOException("Gemini APIとの通信が制限時間を超えました。通信環境を確認し、時間をおいて再実行してください。");
        } catch (IOException e) {
            // 元の例外や応答本文は、秘密情報のログ出力を避けるため含めない。
            throw new IOException("Gemini APIに接続できませんでした。インターネット接続や接続先の状態を確認してください。");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Gemini APIとの通信または再試行の待機が中断されました。分析は完了していません。");
        }

        System.out.println("HTTPステータス：" + response.statusCode());

        if (response.statusCode() == 200) {
        
            Files.writeString(
                Path.of("analysis.json"),
                response.body(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        
            System.out.println("analysis.json を保存しました！");

ObjectMapper mapper = new ObjectMapper();

JsonNode root = mapper.readTree(response.body());

String analysis = "";

for (JsonNode step : root.path("steps")) {
    if ("model_output".equals(step.path("type").asText())) {
        for (JsonNode content : step.path("content")) {
            if ("text".equals(content.path("type").asText())) {
                analysis += content.path("text").asText();
            }
        }
    }
}

if (!analysis.isBlank()) {
    Files.writeString(Path.of("analysis.txt"), analysis);
    System.out.println("analysis.txt を保存しました！");
} else {
    throw new IOException("AIの分析文章が見つかりませんでした。分析は完了していません。");
}

        
        } else {
            printSafeErrorDetails(response.body());
            String reason;
            int status = response.statusCode();
            if (status == 400) {
                reason = "送信内容が受け付けられませんでした。リクエストの形式や指定内容を確認してください。";
            } else if (status == 401 || status == 403) {
                reason = "認証またはアクセス権限の問題が考えられます。APIキーの設定や利用権限を確認してください。";
            } else if (status == 429) {
                reason = "利用回数や利用枠の上限に達した可能性があります。利用枠を確認し、時間をおいて再実行してください。";
            } else if (status >= 500 && status <= 599) {
                reason = "Gemini API側で問題が発生した可能性があります。時間をおいて再実行してください。";
            } else {
                reason = "予期しない応答が返されました。APIの設定やサービスの状態を確認してください。";
            }
            throw new IOException("Gemini APIの分析に失敗しました（HTTP " + status + "）。" + reason);
        }
        
    }
    // APIが入力内容をエラーに含める場合もあるため、確認済みの定型値だけを表示する。
    private static void printSafeErrorDetails(String body) {
        String code = "取得できませんでした";
        String message = "取得できませんでした";
        try {
            if (body != null && !body.isBlank()) {
                JsonNode root = new ObjectMapper().readTree(body);
                JsonNode error = root == null ? null : root.get("error");
                if (error != null && error.isObject()) {
                    JsonNode codeNode = error.path("code");
                    if (codeNode.isIntegralNumber() && codeNode.canConvertToInt()
                            && codeNode.intValue() >= 400 && codeNode.intValue() <= 599) {
                        code = codeNode.asText();
                    } else if (codeNode.isTextual()) {
                        code = switch (codeNode.asText()) {
                            case "invalid_request", "failed_precondition", "out_of_range",
                                 "parameter_unknown", "authentication", "payment_required",
                                 "permission_denied", "not_found", "model_not_found",
                                 "already_exists", "aborted", "rate_limit_exceeded",
                                 "quota_exceeded", "too_many_requests", "cancelled",
                                 "api_error", "unimplemented", "service_unavailable",
                                 "deadline_exceeded", "UNAVAILABLE", "INTERNAL",
                                 "RESOURCE_EXHAUSTED", "INVALID_ARGUMENT",
                                 "UNAUTHENTICATED", "PERMISSION_DENIED", "NOT_FOUND",
                                 "DEADLINE_EXCEEDED" -> codeNode.asText();
                            default -> "安全のため未確認の値を非表示";
                        };
                    }

                    JsonNode messageNode = error.path("message");
                    if (messageNode.isTextual() && !messageNode.asText().isBlank()) {
                        message = switch (messageNode.asText()) {
                            case "This model is currently experiencing high demand. Spikes in demand are usually temporary. Please try again later.",
                                 "The service is currently unavailable.",
                                 "The service is temporarily unavailable.",
                                 "Service unavailable.",
                                 "Internal server error.",
                                 "Internal error encountered.",
                                 "API key not valid. Please pass a valid API key." -> messageNode.asText();
                            default -> "安全のため非表示（入力内容を含まないと確認できる定型文ではありません）";
                        };
                    }
                }
            }
        } catch (IOException e) {
            // JSON解析の例外にも本文が含まれ得るため、例外自体は表示しない。
        }
        System.err.println("Gemini error.code：" + code);
        System.err.println("Gemini error.message：" + message);
    }
}
