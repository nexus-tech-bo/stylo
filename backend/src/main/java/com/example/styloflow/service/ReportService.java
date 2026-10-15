package com.example.styloflow.service;

import com.example.styloflow.dto.report.DailySalesResponse;
import com.example.styloflow.dto.report.SalesSummaryResponse;
import com.example.styloflow.dto.report.StylistTotalResponse;
import com.example.styloflow.dto.report.TopItemResponse;
import com.example.styloflow.enums.ItemType;
import com.example.styloflow.util.DateRange;

import java.io.PrintWriter;
import java.util.List;

/** Sales reports. Only completed sales count, except in the CSV export and {@code voidedSales}. */
public interface ReportService {
    SalesSummaryResponse findSummary(DateRange range);

    /** One entry per day of the range, days without sales included with zeros. */
    List<DailySalesResponse> findSalesByDay(DateRange range);

    List<StylistTotalResponse> findStylistTotals(DateRange range);

    /** The user's own row, or an empty list when they performed no services in the range. */
    List<StylistTotalResponse> findStylistTotalsByUserId(DateRange range, Long userId);

    List<TopItemResponse> findTopItems(DateRange range, ItemType type, int limit);

    /** Writes every sale of the range (completed and voided) as {@code ;}-separated CSV with a header row. */
    void writeSalesCsv(DateRange range, PrintWriter out);
}
