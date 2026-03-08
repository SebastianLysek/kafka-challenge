package com.haeger.kafkachallenge.inventory.config;

import com.haeger.kafkachallenge.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.inventory.entity.Product;
import com.haeger.kafkachallenge.inventory.entity.ProductCategory;
import com.haeger.kafkachallenge.inventory.repository.InventoryItemRepository;
import com.haeger.kafkachallenge.inventory.service.ProductCategoryService;
import com.haeger.kafkachallenge.inventory.service.ProductService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventorySeedConfig {

    private static final Logger LOG = LoggerFactory.getLogger(InventorySeedConfig.class);

    @Bean
    CommandLineRunner seedInventory(ProductCategoryService categoryService,
                                    ProductService productService,
                                    InventoryItemRepository inventoryItemRepository) {
        return args -> {
            boolean hasInventory = inventoryItemRepository.count() > 0;
            boolean hasProducts = productService.hasProducts();
            boolean hasCategories = categoryService.hasCategories();

            if (hasInventory && hasProducts && hasCategories) {
                LOG.info("Inventory already seeded");
                return;
            }

            LOG.info("Seeding inventory");
            Map<String, ProductCategory> categories = new HashMap<>();
            for (CategorySeed seed : categorySeeds()) {
                ProductCategory category = categoryService.findByCode(seed.code())
                    .orElseGet(() -> categoryService.save(ProductCategory.builder()
                        .code(seed.code())
                        .name(seed.name())
                        .sortOrder(seed.sortOrder())
                        .build()));
                categories.put(seed.code(), category);
            }

            Map<String, Product> products = new HashMap<>();
            for (ProductSeed seed : productSeeds()) {
                Product product = productService.findBySku(seed.sku())
                    .orElseGet(() -> productService.save(Product.builder()
                        .sku(seed.sku())
                        .name(seed.name())
                        .price(seed.price())
                        .specs(seed.specs())
                        .category(categories.get(seed.categoryCode()))
                        .build()));
                products.put(seed.sku(), product);
            }

            if (!hasInventory) {
                List<InventoryItem> inventoryItems = productSeeds().stream()
                    .map(seed -> InventoryItem.builder()
                        .product(products.get(seed.sku()))
                        .quantity(seed.quantity())
                        .build())
                    .toList();
                inventoryItemRepository.saveAll(inventoryItems);
                LOG.info("Inventory seeded");
            }
        };
    }

    /**
     * Provides a list of predefined {@code CategorySeed} objects, each representing
     * metadata for a product category. These seeds include category code, name, and sort order,
     * and are used during the initialization or seeding of the inventory system.
     *
     * @return a list of {@code CategorySeed} objects containing preconfigured category metadata.
     */
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

    /**
     * Generates and returns a list of predefined {@code ProductSeed} objects. Each {@code ProductSeed} represents
     * a product definition with attributes such as SKU, name, category code, price, specifications, and quantity.
     * These seeds are designed for initializing or populating the product inventory during the application setup.
     *
     * @return a list of {@code ProductSeed} objects containing preconfigured product data.
     */
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

    /**
     * Represents a category seed used for initializing product category data within the inventory system.
     * This record encapsulates metadata for a product category, including a unique code, display name,
     * and sort order.
     *
     * <ul>
     *   <li>{@code code} - A unique identifier for the category.</li>
     *   <li>{@code name} - A descriptive name of the category.</li>
     *   <li>{@code sortOrder} - An integer representing the order in which the category should appear.</li>
     * </ul>
     */
    private record CategorySeed(String code, String name, int sortOrder) {}

    /**
     * A record representing a product seed used for initializing or populating
     * the product inventory during the application setup phase. Each instance of
     * {@code ProductSeed} encapsulates metadata and properties necessary
     * for defining a product.
     *
     * The fields of the record are:
     * - SKU (stock keeping unit): A unique identifier for the product.
     * - Name: The name or title of the product.
     * - Category Code: A code representing the category to which the product belongs.
     * - Price: The price assigned to the product.
     * - Specs: Specifications or additional details about the product.
     * - Quantity: The initial quantity of the product available in the inventory.
     */
    private record ProductSeed(String sku, String name, String categoryCode, BigDecimal price,
                               String specs, int quantity) {}
}
