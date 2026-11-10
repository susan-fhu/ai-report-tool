import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        try {
            SalesSummary summary = SalesAggregator.aggregate("sales.csv");
            ReportWriter.printSummary(summary);
            ReportWriter.writeReport(summary, "report.csv");
            ReportWriter.writeAiInput(summary, "ai_input.txt");
        } catch (IOException | NumberFormatException e) {
            System.out.println("データ処理エラー");
            e.printStackTrace();
            return;
        }
        GeminiTest.analyze();
    }
}
