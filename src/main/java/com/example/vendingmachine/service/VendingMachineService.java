package com.example.vendingmachine.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.vendingmachine.model.MoneyStock;
import com.example.vendingmachine.model.Order;
import com.example.vendingmachine.model.Product;
import com.example.vendingmachine.model.Sale;
import com.example.vendingmachine.repository.MoneyStockRepository;
import com.example.vendingmachine.repository.OrderRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.repository.SaleRepository;

@Service
public class VendingMachineService {

    private static final int MAX_INSERT_MONEY = 1990;

    private static final int[] MONEY_TYPES = {
            1000, 500, 100, 50, 10
    };

    private final ProductRepository productRepository;
    private final MoneyStockRepository moneyStockRepository;
    private final OrderRepository orderRepository;
    private final SaleRepository saleRepository;

    /*
     * 現在の利用者が投入している合計金額
     */
    private int insertedMoney = 0;

    /*
     * 現在の利用者が投入した金種と枚数
     *
     * 例:
     * 500円を1枚、100円を2枚投入
     * {500=1, 100=2}
     */
    private final Map<Integer, Integer> insertedMoneyDetails =
            new HashMap<>();

    public VendingMachineService(
            ProductRepository productRepository,
            MoneyStockRepository moneyStockRepository,
            OrderRepository orderRepository,
            SaleRepository saleRepository) {

        this.productRepository = productRepository;
        this.moneyStockRepository = moneyStockRepository;
        this.orderRepository = orderRepository;
        this.saleRepository = saleRepository;
    }

    // =====================================
    // 商品情報
    // =====================================

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public long getProductCount() {
        return productRepository.count();
    }

    public long getSoldOutCount() {

        return productRepository.findAll()
                .stream()
                .filter(product ->
                        product.getStock() != null
                                && product.getStock() <= 0)
                .count();
    }

    // =====================================
    // 購入履歴・売上
    // =====================================

    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    public List<Sale> getSales() {
        return saleRepository.findAll();
    }

    public long getOrderCount() {
        return orderRepository.count();
    }

    public int getTotalSales() {

        return saleRepository.findAll()
                .stream()
                .mapToInt(sale ->
                        sale.getSalesAmount() == null
                                ? 0
                                : sale.getSalesAmount())
                .sum();
    }

    // =====================================
    // 金種情報
    // =====================================

    /*
     * 管理者画面では1円と5円を表示しない
     */
    public List<MoneyStock> getMoneyStocks() {

        return moneyStockRepository.findAll()
                .stream()
                .filter(money ->
                        money.getMoneyType() != null
                                && money.getMoneyType() >= 10)
                .sorted((a, b) ->
                        Integer.compare(
                                a.getMoneyType(),
                                b.getMoneyType()))
                .toList();
    }

    public int getInsertedMoney() {
        return insertedMoney;
    }

    // =====================================
    // お金投入
    // =====================================

    public String insertMoney(int money) {

        MoneyStock moneyStock =
                moneyStockRepository.findById(money)
                        .orElse(null);

        /*
         * 1円・5円・登録されていない金種
         */
        if (moneyStock == null) {
            return money + "円はご利用不可です。";
        }

        /*
         * 管理者が利用不可に設定した金種
         */
        if (!Boolean.TRUE.equals(
                moneyStock.getAvailable())) {

            return money + "円はご利用不可です。";
        }

        /*
         * 合計投入金額が1990円を超える場合
         */
        if (insertedMoney + money
                > MAX_INSERT_MONEY) {

            return "投入上限は1990円です。"
                    + money
                    + "円はご利用不可です。";
        }

        /*
         * 現在の投入金額を増やす
         */
        insertedMoney += money;

        /*
         * 利用者が投入した金種と枚数を記録
         *
         * この時点ではDBのmoney_stockには
         * まだ追加しません。
         */
        insertedMoneyDetails.merge(
                money,
                1,
                Integer::sum);

        return money
                + "円を投入しました。"
                + "現在の投入金額は"
                + insertedMoney
                + "円です。";
    }

    // =====================================
    // 商品購入
    // =====================================

    @Transactional
    public String purchase(Integer productId) {

        Product product =
                productRepository.findById(productId)
                        .orElse(null);

        if (product == null) {
            return "商品が見つかりません。";
        }

        if (product.getStock() == null
                || product.getStock() <= 0) {

            return "売り切れです。";
        }

        if (insertedMoney < product.getPrice()) {

            return "投入金額が不足しています。"
                    + "必要金額は"
                    + product.getPrice()
                    + "円です。";
        }

        int change =
                insertedMoney - product.getPrice();

        /*
         * 現在の金種在庫と、
         * 利用者が投入したお金を合わせて
         * お釣りを作れるか確認
         */
        Map<Integer, Integer> changePlan =
                createChangePlan(change);

        if (changePlan == null) {

            return "釣銭不足のため購入できません。"
                    + "投入金額を返却してください。";
        }

        /*
         * 商品在庫を1個減らす
         */
        product.setStock(
                product.getStock() - 1);

        productRepository.save(product);

        /*
         * 購入履歴を登録
         */
        Order order = new Order();

        order.setProductId(
                product.getProductId());

        order.setPurchasePrice(
                product.getPrice());

        order.setPurchaseDate(
                LocalDateTime.now());

        order = orderRepository.save(order);

        /*
         * 売上を登録
         */
        Sale sale = new Sale();

        sale.setOrderId(
                order.getOrderId());

        sale.setProductId(
                product.getProductId());

        sale.setSalesAmount(
                product.getPrice());

        saleRepository.save(sale);

        /*
         * 利用者が投入したお金を
         * 金種在庫に追加する
         */
        addInsertedMoneyToStock();

        /*
         * お釣りとして使用する金種を
         * 金種在庫から減らす
         */
        dispenseChange(changePlan);

        String changeDetail =
                createChangeMessage(changePlan);

        /*
         * 利用者の投入情報をリセット
         */
        clearInsertedMoney();

        if (change == 0) {

            return product.getProductName()
                    + "を購入しました。"
                    + "お釣りはありません。";
        }

        return product.getProductName()
                + "を購入しました。"
                + "お釣りは"
                + change
                + "円です。"
                + changeDetail;
    }

    // =====================================
    // 釣銭計算
    // =====================================

    private Map<Integer, Integer> createChangePlan(
            int change) {

        Map<Integer, Integer> changePlan =
                new LinkedHashMap<>();

        if (change == 0) {
            return changePlan;
        }

        /*
         * DBの金種在庫を取得
         */
        Map<Integer, Integer> availableStocks =
                new HashMap<>();

        for (int moneyType : MONEY_TYPES) {

            MoneyStock moneyStock =
                    moneyStockRepository
                            .findById(moneyType)
                            .orElse(null);

            int databaseStock = 0;

            if (moneyStock != null
                    && moneyStock.getStockCount()
                    != null) {

                databaseStock =
                        moneyStock.getStockCount();
            }

            /*
             * 利用者が投入したお金も、
             * 購入成立後は釣銭として利用可能
             */
            int insertedCount =
                    insertedMoneyDetails
                            .getOrDefault(
                                    moneyType,
                                    0);

            availableStocks.put(
                    moneyType,
                    databaseStock
                            + insertedCount);
        }

        int remaining = change;

        /*
         * 大きい金種から順番に使用する
         */
        for (int moneyType : MONEY_TYPES) {

            int stockCount =
                    availableStocks.getOrDefault(
                            moneyType,
                            0);

            int requiredCount =
                    remaining / moneyType;

            int useCount =
                    Math.min(
                            requiredCount,
                            stockCount);

            if (useCount > 0) {

                changePlan.put(
                        moneyType,
                        useCount);

                remaining -=
                        moneyType * useCount;
            }
        }

        /*
         * 残りが0でなければ、
         * お釣りを完全に作れない
         */
        if (remaining != 0) {
            return null;
        }

        return changePlan;
    }

    // =====================================
    // 投入金を金種在庫へ追加
    // =====================================

    private void addInsertedMoneyToStock() {

        for (Map.Entry<Integer, Integer> entry
                : insertedMoneyDetails.entrySet()) {

            Integer moneyType =
                    entry.getKey();

            Integer insertedCount =
                    entry.getValue();

            MoneyStock moneyStock =
                    moneyStockRepository
                            .findById(moneyType)
                            .orElse(null);

            if (moneyStock == null) {
                continue;
            }

            int currentStock =
                    moneyStock.getStockCount()
                            == null
                            ? 0
                            : moneyStock
                                    .getStockCount();

            moneyStock.setStockCount(
                    currentStock
                            + insertedCount);

            moneyStockRepository.save(
                    moneyStock);
        }
    }

    // =====================================
    // お釣りを金種在庫から減らす
    // =====================================

    private void dispenseChange(
            Map<Integer, Integer> changePlan) {

        for (Map.Entry<Integer, Integer> entry
                : changePlan.entrySet()) {

            Integer moneyType =
                    entry.getKey();

            Integer useCount =
                    entry.getValue();

            MoneyStock moneyStock =
                    moneyStockRepository
                            .findById(moneyType)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            moneyType
                                                    + "円の金種在庫がありません。"));

            int currentStock =
                    moneyStock.getStockCount()
                            == null
                            ? 0
                            : moneyStock
                                    .getStockCount();

            if (currentStock < useCount) {

                throw new IllegalStateException(
                        moneyType
                                + "円の釣銭在庫が不足しています。");
            }

            moneyStock.setStockCount(
                    currentStock - useCount);

            moneyStockRepository.save(
                    moneyStock);
        }
    }

    // =====================================
    // お釣り表示メッセージ
    // =====================================

    private String createChangeMessage(
            Map<Integer, Integer> changePlan) {

        if (changePlan.isEmpty()) {
            return "";
        }

        StringBuilder message =
                new StringBuilder(" 内訳：");

        for (Map.Entry<Integer, Integer> entry
                : changePlan.entrySet()) {

            message.append(
                    entry.getKey());

            message.append("円×");

            message.append(
                    entry.getValue());

            message.append("枚 ");
        }

        return message.toString();
    }

    // =====================================
    // お金返却
    // =====================================

    public String returnMoney() {

        if (insertedMoney == 0) {
            return "返却するお金はありません。";
        }

        int returnedMoney =
                insertedMoney;

        /*
         * 投入金はまだDBに追加されていないため、
         * リセットするだけで返却処理になる
         */
        clearInsertedMoney();

        return returnedMoney
                + "円を返却しました。";
    }

    private void clearInsertedMoney() {

        insertedMoney = 0;
        insertedMoneyDetails.clear();
    }

    // =====================================
    // 商品追加
    // =====================================

    public String addProduct(
            String productName,
            int price,
            int stock) {

        if (productName == null
                || productName.isBlank()) {

            return "商品名を入力してください。";
        }

        if (price <= 0) {
            return "価格は1円以上にしてください。";
        }

        if (stock < 0) {
            return "在庫は0以上にしてください。";
        }

        Product product = new Product();

        product.setProductName(
                productName.trim());

        product.setPrice(price);
        product.setStock(stock);

        productRepository.save(product);

        return productName
                + "を追加しました。";
    }

    // =====================================
    // 商品在庫補充
    // =====================================

    public String replenishStock(
            Integer productId,
            int quantity) {

        Product product =
                productRepository.findById(productId)
                        .orElse(null);

        if (product == null) {
            return "商品が見つかりません。";
        }

        if (quantity <= 0) {
            return "補充数は1以上にしてください。";
        }

        int currentStock =
                product.getStock() == null
                        ? 0
                        : product.getStock();

        product.setStock(
                currentStock + quantity);

        productRepository.save(product);

        return product.getProductName()
                + "を"
                + quantity
                + "個補充しました。";
    }

    // =====================================
    // 商品削除
    // =====================================

    public String deleteProduct(
            Integer productId) {

        if (!productRepository
                .existsById(productId)) {

            return "商品が見つかりません。";
        }

        try {

            productRepository.deleteById(
                    productId);

            return "商品を削除しました。";

        } catch (Exception e) {

            return "購入履歴があるため、"
                    + "この商品は削除できません。";
        }
    }

    // =====================================
    // 金種在庫補充
    // =====================================

    public String replenishMoney(
            Integer moneyType,
            int count) {

        if (count <= 0) {
            return "補充枚数は1枚以上にしてください。";
        }

        MoneyStock moneyStock =
                moneyStockRepository
                        .findById(moneyType)
                        .orElse(null);

        if (moneyStock == null) {
            return "金種が見つかりません。";
        }

        int currentStock =
                moneyStock.getStockCount()
                        == null
                        ? 0
                        : moneyStock
                                .getStockCount();

        moneyStock.setStockCount(
                currentStock + count);

        moneyStockRepository.save(
                moneyStock);

        return moneyType
                + "円を"
                + count
                + "枚補充しました。";
    }

    // =====================================
    // 金種の利用可否設定
    // =====================================

    public String updateMoneyAvailability(
            Integer moneyType,
            boolean available) {

        MoneyStock moneyStock =
                moneyStockRepository
                        .findById(moneyType)
                        .orElse(null);

        if (moneyStock == null) {
            return "金種が見つかりません。";
        }

        moneyStock.setAvailable(available);

        moneyStockRepository.save(
                moneyStock);

        if (available) {

            return moneyType
                    + "円を利用可能に設定しました。";
        }

        return moneyType
                + "円をご利用不可に設定しました。";
    }
}