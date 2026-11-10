import java.util.Map;

public class SalesSummary {
    private final Map<String, Integer> sales;
    private final int totalSales;

    public SalesSummary(Map<String, Integer> sales, int totalSales) {
        this.sales = sales;
        this.totalSales = totalSales;
    }

    public Map<String, Integer> getSales() {
        return sales;
    }

    public int getTotalSales() {
        return totalSales;
    }
}
