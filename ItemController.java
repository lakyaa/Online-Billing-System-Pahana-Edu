package com.pahanaedu.billing.controller;

import com.pahanaedu.billing.entity.Item;
import com.pahanaedu.billing.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping
public class ItemController {
    private final ItemService service;
    public ItemController(ItemService service) { this.service = service; }

    // UI pages
    @GetMapping("/items")
    public String itemsPage(Model model) {
        model.addAttribute("items", service.all());
        model.addAttribute("item", new Item());
        return "items";
    }

    @PostMapping("/items")
    public String createItemFromForm(@ModelAttribute Item item) {
        service.create(item);
        return "redirect:/items";
    }

    @PostMapping("/items/{id}")
    public String updateItemFromForm(@PathVariable Long id, @ModelAttribute Item item) {
        service.update(id, item);
        return "redirect:/items";
    }

    @PostMapping("/items/{id}/delete")
    public String deleteItemFromForm(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/items";
    }

    // REST API
    @GetMapping("/api/items")
    @ResponseBody
    public List<Item> all() { return service.all(); }

    @PostMapping("/api/items")
    @ResponseBody
    public Item create(@RequestBody Item i) { return service.create(i); }

    @PutMapping("/api/items/{id}")
    @ResponseBody
    public Item update(@PathVariable Long id, @RequestBody Item i) { return service.update(id, i); }

    @DeleteMapping("/api/items/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
