package com.pahanaedu.billing.controller;

import com.pahanaedu.billing.entity.Customer;
import com.pahanaedu.billing.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping
public class CustomerController {
    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service = service; }

    // UI pages
    @GetMapping("/customers")
    public String customersPage(Model model) {
        model.addAttribute("customers", service.all());
        model.addAttribute("customer", new Customer());
        return "customers";
    }

    @PostMapping("/customers")
    public String createCustomerFromForm(@ModelAttribute Customer customer) {
        service.create(customer);
        return "redirect:/customers";
    }

    @PostMapping("/customers/{id}")
    public String updateCustomerFromForm(@PathVariable Long id, @ModelAttribute Customer c) {
        service.update(id, c);
        return "redirect:/customers";
    }

    @PostMapping("/customers/{id}/delete")
    public String deleteCustomerFromForm(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/customers";
    }

    // REST API
    @GetMapping("/api/customers")
    @ResponseBody
    public List<Customer> all() { return service.all(); }

    @PostMapping("/api/customers")
    @ResponseBody
    public Customer create(@RequestBody Customer c) { return service.create(c); }

    @PutMapping("/api/customers/{id}")
    @ResponseBody
    public Customer update(@PathVariable Long id, @RequestBody Customer c) { return service.update(id, c); }

    @DeleteMapping("/api/customers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
