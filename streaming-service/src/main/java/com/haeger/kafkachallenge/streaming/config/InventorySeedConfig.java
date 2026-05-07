package com.haeger.kafkachallenge.streaming.config;

import com.haeger.kafkachallenge.streaming.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.streaming.inventory.entity.Product;
import com.haeger.kafkachallenge.streaming.inventory.entity.ProductCategory;
import com.haeger.kafkachallenge.streaming.inventory.repository.InventoryItemRepository;
import com.haeger.kafkachallenge.streaming.inventory.repository.ProductCategoryRepository;
import com.haeger.kafkachallenge.streaming.inventory.repository.ProductRepository;
import com.haeger.kafkachallenge.streaming.inventory.service.ProductEventFactory;
import com.haeger.kafkachallenge.streaming.streams.topology.ProtoEventPublisher;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class InventorySeedConfig implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(InventorySeedConfig.class);

    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductEventFactory productEventFactory;
    private final ProtoEventPublisher eventPublisher;

    @Value("${app.streaming.publish-seed-events:false}")
    private boolean publishSeedEvents;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (inventoryItemRepository.count() == 0) {
            LOG.info("Seeding streaming-service inventory");
            seedInventory();
        } else {
            LOG.info("Streaming-service inventory already seeded");
        }

        if (publishSeedEvents) {
            inventoryItemRepository.findAllBy().forEach(item ->
                eventPublisher.publishProductUpserted(productEventFactory.toProductUpserted(item)));
        }
    }

    private void seedInventory() {
        Map<String, ProductCategory> categories = new HashMap<>();
        for (CategorySeed seed : categorySeeds()) {
            ProductCategory category = categoryRepository.findByCode(seed.code())
                .orElseGet(() -> categoryRepository.save(ProductCategory.builder()
                    .code(seed.code())
                    .name(seed.name())
                    .sortOrder(seed.sortOrder())
                    .build()));
            categories.put(seed.code(), category);
        }

        Map<String, Product> products = new HashMap<>();
        for (ProductSeed seed : productSeeds()) {
            Product product = productRepository.findBySku(seed.sku())
                .orElseGet(() -> productRepository.save(Product.builder()
                    .sku(seed.sku())
                    .name(seed.name())
                    .price(seed.price())
                    .specs(seed.specs())
                    .category(categories.get(seed.categoryCode()))
                    .build()));
            products.put(seed.sku(), product);
        }

        inventoryItemRepository.saveAll(productSeeds().stream()
            .map(seed -> InventoryItem.builder()
                .product(products.get(seed.sku()))
                .quantity(seed.quantity())
                .build())
            .toList());
    }

    private List<CategorySeed> categorySeeds() {
        return List.of(
            new CategorySeed("RAM", "Memory", 1),
            new CategorySeed("CPU", "Processors", 2),
            new CategorySeed("MOBO", "Motherboards", 3),
            new CategorySeed("GPU", "Graphics Cards", 4),
            new CategorySeed("PSU", "Power Supplies", 5),
            new CategorySeed("STORAGE", "Storage", 6),
            new CategorySeed("COOLING", "Cooling", 7),
            new CategorySeed("CASE", "Cases", 8)
        );
    }

    private List<ProductSeed> productSeeds() {
        return List.of(
            new ProductSeed("RAM-DDR5-32-6000", "Corsair Vengeance DDR5 32GB 6000", "RAM",
                new BigDecimal("539.99"), "2x16GB, CL36, XMP/EXPO", 12),
            new ProductSeed("RAM-DDR5-64-5600", "G.Skill Ripjaws DDR5 64GB 5600", "RAM",
                new BigDecimal("629.99"), "2x32GB, CL40", 8),
            new ProductSeed("RAM-DDR4-16-3200", "Kingston Fury DDR4 16GB 3200", "RAM",
                new BigDecimal("449.99"), "2x8GB, CL16", 3),
            new ProductSeed("CPU-AMD-7800X3D", "AMD Ryzen 7 7800X3D", "CPU",
                new BigDecimal("399.99"), "8C/16T, 4.2-5.0GHz, AM5", 10),
            new ProductSeed("CPU-AMD-5600", "AMD Ryzen 5 5600", "CPU",
                new BigDecimal("129.99"), "6C/12T, 3.5-4.4GHz, AM4", 15),
            new ProductSeed("CPU-INTEL-14700K", "Intel Core i7-14700K", "CPU",
                new BigDecimal("419.99"), "20C/28T, 5.6GHz boost, LGA1700", 6),
            new ProductSeed("MOBO-AM5-B650", "MSI B650 Tomahawk WiFi", "MOBO",
                new BigDecimal("219.99"), "AM5, ATX, WiFi 6E, DDR5", 7),
            new ProductSeed("MOBO-AM4-B550", "ASUS TUF Gaming B550-Plus", "MOBO",
                new BigDecimal("149.99"), "AM4, ATX, PCIe 4.0", 11),
            new ProductSeed("MOBO-LGA1700-Z790", "Gigabyte Z790 Aorus Elite AX", "MOBO",
                new BigDecimal("249.99"), "LGA1700, ATX, DDR5, WiFi 6E", 5),
            new ProductSeed("GPU-NV-RTX4070S", "NVIDIA GeForce RTX 4070 SUPER", "GPU",
                new BigDecimal("599.99"), "12GB GDDR6X", 4),
            new ProductSeed("GPU-NV-RTX4080S", "NVIDIA GeForce RTX 4080 SUPER", "GPU",
                new BigDecimal("999.99"), "16GB GDDR6X", 3),
            new ProductSeed("GPU-AMD-RX7800XT", "AMD Radeon RX 7800 XT", "GPU",
                new BigDecimal("499.99"), "16GB GDDR6", 6),
            new ProductSeed("PSU-750-GOLD", "Seasonic Focus GX-750", "PSU",
                new BigDecimal("119.99"), "750W, 80+ Gold, fully modular", 9),
            new ProductSeed("PSU-850-GOLD", "Corsair RM850x", "PSU",
                new BigDecimal("139.99"), "850W, 80+ Gold, fully modular", 10),
            new ProductSeed("PSU-1000-PLAT", "EVGA SuperNOVA 1000 P6", "PSU",
                new BigDecimal("219.99"), "1000W, 80+ Platinum", 4),
            new ProductSeed("STORAGE-SSD-1TB-NVME", "Samsung 990 EVO 1TB", "STORAGE",
                new BigDecimal("89.99"), "NVMe PCIe 4.0, up to 5,000MB/s", 20),
            new ProductSeed("STORAGE-SSD-2TB-NVME", "WD Black SN850X 2TB", "STORAGE",
                new BigDecimal("169.99"), "NVMe PCIe 4.0, heatsink", 12),
            new ProductSeed("STORAGE-HDD-4TB", "Seagate BarraCuda 4TB", "STORAGE",
                new BigDecimal("79.99"), "3.5in, 5400RPM", 18),
            new ProductSeed("COOLING-AIR-NH-D15", "Noctua NH-D15", "COOLING",
                new BigDecimal("109.99"), "Dual tower air cooler", 14),
            new ProductSeed("COOLING-AIO-240", "Arctic Liquid Freezer II 240", "COOLING",
                new BigDecimal("89.99"), "240mm AIO, PWM", 9),
            new ProductSeed("CASE-ATX-NZXT-H7", "NZXT H7 Flow", "CASE",
                new BigDecimal("129.99"), "ATX mid tower, high airflow", 5),
            new ProductSeed("CASE-ATX-FRAC-MESHIFY2", "Fractal Meshify 2", "CASE",
                new BigDecimal("159.99"), "ATX mid tower, mesh front", 6)
        );
    }

    private record CategorySeed(String code, String name, int sortOrder) {
    }

    private record ProductSeed(String sku, String name, String categoryCode, BigDecimal price,
                               String specs, int quantity) {
    }
}
