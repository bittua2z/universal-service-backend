package com.example.universalservice.controller;

import com.example.universalservice.model.Stock;
import com.example.universalservice.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*") // Allow all origins for local development
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllStocks() {
        List<Stock> stocks = stockService.getAllStocks();
        List<Map<String, Object>> response = stocks.stream().map(stock -> {
            String base64Image = Base64.getEncoder().encodeToString(stock.getImage());
            Map<String, Object> map = new HashMap<>();
            map.put("id", stock.getId());
            map.put("image", "data:image/jpeg;base64," + base64Image);
            map.put("price", stock.getPrice());
            map.put("detail", stock.getDetail());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStockById(@PathVariable Long id) {
        Stock stock = stockService.getStockById(id);
        if (stock == null) {
            return ResponseEntity.notFound().build();
        }
        String base64Image = Base64.getEncoder().encodeToString(stock.getImage());
        Map<String, Object> response = new HashMap<>();
        response.put("id", stock.getId());
        response.put("image", "data:image/jpeg;base64," + base64Image);
        response.put("price", stock.getPrice());
        response.put("detail", stock.getDetail());
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> addStock(@RequestParam("file") MultipartFile file, @RequestParam("price") double price, @RequestParam("detail") String detail) throws IOException {
        if (!file.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().body("Invalid file type. Only images allowed.");
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            return ResponseEntity.badRequest().body("File too large. Max 5MB.");
        }
        byte[] imageBytes = file.getBytes();
        Stock stock = new Stock(imageBytes, price);
        stock.setDetail(detail.replaceAll("<script[^>]*>([\\s\\S]*?)</script>", "")); // Basic script tag removal for XSS
        Stock savedStock = stockService.saveStock(stock);
        return ResponseEntity.ok("Stock added successfully");
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateStock(@PathVariable Long id, @RequestParam(value = "file", required = false) MultipartFile file, @RequestParam("price") double price, @RequestParam("detail") String detail) throws IOException {
        Stock stock = stockService.getStockById(id);
        if (stock == null) {
            return ResponseEntity.notFound().build();
        }
        if (file != null && !file.isEmpty()) {
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body("Invalid file type. Only images allowed.");
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File too large. Max 5MB.");
            }
            byte[] imageBytes = file.getBytes();
            stock.setImage(imageBytes);
        }
        stock.setPrice(price);
        stock.setDetail(detail.replaceAll("<script[^>]*>([\\s\\S]*?)</script>", "")); // Basic XSS prevention
        Stock savedStock = stockService.saveStock(stock);
        return ResponseEntity.ok("Stock updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }
}
