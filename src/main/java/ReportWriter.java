import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ReportWriter {
    public static void printSummary(SalesSummary summary) {
        System.out.println("=== 売上集計レポート ===");

        for (Map.Entry<String, Integer> entry : summary.getSales().entrySet()) {
            System.out.println(
                entry.getKey() + "：" + entry.getValue() + "円"
            );
        }

        System.out.println("----------------");
        System.out.println("合計売上：" + summary.getTotalSales() + "円");
    }

    public static void writeReport(SalesSummary summary, String outputFile)
            throws IOException {
        try (BufferedWriter writer =
                new BufferedWriter(new FileWriter(outputFile))) {

            writer.write("商品名,売上金額");
            writer.newLine();

            for (Map.Entry<String, Integer> entry : summary.getSales().entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }

            writer.write("合計," + summary.getTotalSales());
            writer.newLine();

            System.out.println("report.csv を作成しました！");

        }
    }

    public static void writeAiInput(SalesSummary summary, String outputFile)
            throws IOException {
        try (BufferedWriter writer =
                new BufferedWriter(new FileWriter(outputFile))) {

            writer.write("あなたは店舗の売上分析担当者です。");
            writer.newLine();
            writer.write("以下の売上データを分析してください。");
            writer.newLine();
            writer.write("売上の特徴と改善に向けた検討事項を説明してください。");
            writer.newLine();
            writer.write("データにない原因を断定しないでください。");
            writer.newLine();
            writer.newLine();

            for (Map.Entry<String, Integer> entry : summary.getSales().entrySet()) {
                writer.write(entry.getKey() + "：" + entry.getValue() + "円");
                writer.newLine();
            }

            writer.write("合計売上：" + summary.getTotalSales() + "円");
            writer.newLine();

            System.out.println("ai_input.txt を作成しました！");
        }
    }
}
