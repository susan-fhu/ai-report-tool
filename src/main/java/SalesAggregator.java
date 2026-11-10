import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SalesAggregator {
    public static SalesSummary aggregate(String inputFile) throws IOException {
        Map<String, Integer> sales = new LinkedHashMap<>();
        int totalSales = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            br.readLine(); // ヘッダー行を読み飛ばす
            int lineNumber = 1;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    throw new IOException("CSVの" + lineNumber + "行目：空行です。");
                }

                String[] data = line.split(",", -1);
                if (data.length < 4) {
                    throw new IOException("CSVの" + lineNumber + "行目：列数が不足しています（4列必要です）。");
                }

                String product = data[1];
                int quantity;
                int price;
                try {
                    quantity = Integer.parseInt(data[2]);
                    price = Integer.parseInt(data[3]);
                } catch (NumberFormatException e) {
                    throw new IOException("CSVの" + lineNumber + "行目：数量・単価はintの範囲内の整数で入力してください。", e);
                }

                if (quantity < 0 || price < 0) {
                    throw new IOException("CSVの" + lineNumber + "行目：数量・単価に負の値は指定できません。");
                }

                int amount = quantity * price;

                sales.put(product, sales.getOrDefault(product, 0) + amount);
                totalSales += amount;
            }
        }

        return new SalesSummary(sales, totalSales);
    }
}
