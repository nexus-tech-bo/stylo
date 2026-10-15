package com.example.styloflow.mapper;

import com.example.styloflow.dto.sale.SaleItemResponse;
import com.example.styloflow.dto.sale.SaleResponse;
import com.example.styloflow.dto.sale.SaleSummaryResponse;
import com.example.styloflow.entity.Sale;
import com.example.styloflow.entity.SaleItem;
import com.example.styloflow.enums.CashRegisterStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class, imports = CashRegisterStatus.class)
public interface SaleMapper {
    @Mapping(target = "date", source = "createdAt")
    @Mapping(target = "cashRegisterId", source = "cashRegister.id")
    @Mapping(target = "cashier", source = "cashier.name")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customer", source = "customer.name")
    @Mapping(target = "customerTaxId", source = "customer.taxId")
    @Mapping(target = "change", source = "changeAmount")
    @Mapping(target = "voidedBy", source = "voidedBy.name")
    SaleResponse toResponse(Sale sale);

    @Mapping(target = "stylistId", source = "stylist.id")
    @Mapping(target = "stylist", source = "stylist.name")
    SaleItemResponse toItemResponse(SaleItem item);

    @Mapping(target = "date", source = "createdAt")
    @Mapping(target = "cashier", source = "cashier.name")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customer", source = "customer.name")
    @Mapping(target = "cashRegisterOpen", expression = "java(sale.getCashRegister().getStatus() == CashRegisterStatus.OPEN)")
    SaleSummaryResponse toSummary(Sale sale);
}
