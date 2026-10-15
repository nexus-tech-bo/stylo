package com.example.styloflow.service.impl;

import com.example.styloflow.dto.report.DailySalesResponse;
import com.example.styloflow.dto.report.SalesSummaryResponse;
import com.example.styloflow.dto.report.StylistTotalResponse;
import com.example.styloflow.dto.report.TopItemResponse;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.repository.ReportRepository;
import com.example.styloflow.service.ReportService;
import com.example.styloflow.util.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    static final String CSV_HEADER = "id;date;cashier;customer;subtotal;discount;total;tax;payment_method;status";
    private static final String CSV_EOL = "\r\n";
    private static final DateTimeFormatter CSV_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DateRange.ZONE);

    private final ReportRepository reportRepository;

    @Override
    public SalesSummaryResponse findSummary(DateRange range) {
        var totals = reportRepository.findSalesTotals(range.start(), range.end());
        var salesTotal = money(totals.getSalesTotal());
        var averageTicket = totals.getSalesCount() == 0
                ? money(null)
                : salesTotal.divide(BigDecimal.valueOf(totals.getSalesCount()), 2, RoundingMode.HALF_UP);
        var discounts = money(totals.getDiscounts())
                .add(money(reportRepository.sumLineDiscounts(range.start(), range.end())));
        var byPaymentMethod = reportRepository.findTotalsByPaymentMethod(range.start(), range.end()).stream()
                .map(p -> new SalesSummaryResponse.PaymentMethodTotal(p.getMethod(), p.getCount(), money(p.getTotal())))
                .toList();
        return new SalesSummaryResponse(range.from(), range.to(), totals.getSalesCount(), salesTotal, averageTicket,
                discounts, money(totals.getTax()), totals.getVoidedSales(), byPaymentMethod);
    }

    @Override
    public List<DailySalesResponse> findSalesByDay(DateRange range) {
        var byDay = reportRepository.findDailyTotals(DateRange.ZONE.getId(), range.start(), range.end()).stream()
                .collect(Collectors.toMap(d -> LocalDate.parse(d.getDay()), Function.identity()));
        return range.from().datesUntil(range.to().plusDays(1))
                .map(day -> {
                    var totals = byDay.get(day);
                    return totals == null
                            ? new DailySalesResponse(day, 0, money(null))
                            : new DailySalesResponse(day, totals.getSalesCount(), money(totals.getTotal()));
                })
                .toList();
    }

    @Override
    public List<StylistTotalResponse> findStylistTotals(DateRange range) {
        return stylistTotals(range, null);
    }

    @Override
    public List<StylistTotalResponse> findStylistTotalsByUserId(DateRange range, Long userId) {
        return stylistTotals(range, userId);
    }

    @Override
    public List<TopItemResponse> findTopItems(DateRange range, ItemType type, int limit) {
        return reportRepository.findTopItems(type, range.start(), range.end(), Limit.of(limit)).stream()
                .map(t -> new TopItemResponse(t.getId(), t.getName(), t.getQuantity(), money(t.getTotal())))
                .toList();
    }

    @Override
    public void writeSalesCsv(DateRange range, PrintWriter out) {
        out.print(CSV_HEADER + CSV_EOL);
        try (var rows = reportRepository.streamSalesForCsv(range.start(), range.end())) {
            rows.forEach(r -> out.print(String.join(";",
                    r.getId().toString(),
                    CSV_DATE.format(r.getCreatedAt()),
                    csvText(r.getCashier()),
                    csvText(r.getCustomer()),
                    money(r.getSubtotal()).toPlainString(),
                    money(r.getDiscount()).toPlainString(),
                    money(r.getTotal()).toPlainString(),
                    money(r.getTax()).toPlainString(),
                    r.getPaymentMethod().name(),
                    r.getStatus().name()) + CSV_EOL));
        }
        out.flush();
    }

    private List<StylistTotalResponse> stylistTotals(DateRange range, Long stylistId) {
        return reportRepository.findStylistTotals(range.start(), range.end(), stylistId).stream()
                .map(s -> new StylistTotalResponse(s.getStylistId(), s.getStylist(), s.getServices(), money(s.getTotal()),
                        money(s.getCommissionRate()), money(s.getCommission())))
                .toList();
    }

    /** 2 decimals, {@code null} (SUM of no rows) as zero. */
    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Free text cell: neutralizes spreadsheet formulas ({@code = + - @} prefix) and quotes values containing
     * the separator, quotes or line breaks.
     */
    static String csvText(String value) {
        if (value == null) return "";
        var text = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")
                ? "\"" + text.replace("\"", "\"\"") + "\""
                : text;
    }
}
