package com.pahanaedu.billing.controller;

import com.pahanaedu.billing.entity.Bill;
import com.pahanaedu.billing.service.BillingService;
import com.pahanaedu.billing.service.CustomerService;
import com.pahanaedu.billing.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
public class BillController {
    private final BillingService billing;
    private final CustomerService customers;
    private final ItemService items;

    public BillController(BillingService billing, CustomerService customers, ItemService items) {
        this.billing = billing;
        this.customers = customers;
        this.items = items;
    }

    @GetMapping("/bills")
    public String billsPage(Model model) {
        model.addAttribute("customers", customers.all());
        model.addAttribute("items", items.all());
        return "bills";
    }

    @PostMapping("/bills/create")
    public String createBillFromForm(@RequestParam Long customerId, @RequestParam Long itemId, @RequestParam int quantity, Model model) {
        Bill bill = billing.createBill(customerId, itemId, quantity);
        model.addAttribute("bill", bill);
        return "invoice";
    }

    // REST
    @PostMapping("/api/bills/calculate")
    @ResponseBody
    public ResponseEntity<Bill> calc(@RequestParam Long customerId, @RequestParam Long itemId, @RequestParam int quantity) {
        return ResponseEntity.ok(billing.createBill(customerId, itemId, quantity));
    }
}
