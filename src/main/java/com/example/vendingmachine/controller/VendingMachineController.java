package com.example.vendingmachine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.vendingmachine.service.VendingMachineService;

import jakarta.servlet.http.HttpSession;

@Controller
public class VendingMachineController {

    private static final String ADMIN_PASSWORD = "1234";

    private final VendingMachineService vendingMachineService;

    public VendingMachineController(
            VendingMachineService vendingMachineService) {

        this.vendingMachineService = vendingMachineService;
    }

    // ==============================
    // User page
    // ==============================

    @GetMapping("/")
    public String index(Model model) {

        model.addAttribute(
                "products",
                vendingMachineService.getProducts());

        model.addAttribute(
                "insertedMoney",
                vendingMachineService.getInsertedMoney());

        return "index";
    }

    @PostMapping("/money")
    public String insertMoney(
            @RequestParam int money,
            Model model) {

        String message =
                vendingMachineService.insertMoney(money);

        model.addAttribute("message", message);

        return index(model);
    }

    @PostMapping("/purchase")
    public String purchase(
            @RequestParam Integer productId,
            Model model) {

        String message =
                vendingMachineService.purchase(productId);

        model.addAttribute("message", message);

        return index(model);
    }

    @PostMapping("/return-money")
    public String returnMoney(Model model) {

        String message =
                vendingMachineService.returnMoney();

        model.addAttribute("message", message);

        return index(model);
    }

    // ==============================
    // Admin login
    // ==============================

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String adminLogin(
            @RequestParam String password,
            HttpSession session,
            Model model) {

        if (ADMIN_PASSWORD.equals(password)) {

            session.setAttribute("adminLoggedIn", true);

            return "redirect:/admin";
        }

        model.addAttribute(
                "error",
                "パスワードが違います。");

        return "admin-login";
    }

    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session) {

        session.invalidate();

        return "redirect:/";
    }

    // ==============================
    // Admin dashboard
    // ==============================

    @GetMapping("/admin")
    public String adminPage(
            HttpSession session,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        addAdminData(model);

        return "admin";
    }

    // ==============================
    // Product management
    // ==============================

    @PostMapping("/admin/product/add")
    public String addProduct(
            @RequestParam String productName,
            @RequestParam int price,
            @RequestParam int stock,
            HttpSession session,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        String message =
                vendingMachineService.addProduct(
                        productName,
                        price,
                        stock);

        model.addAttribute("message", message);

        addAdminData(model);

        return "admin";
    }

    @PostMapping("/admin/product/replenish")
    public String replenishProduct(
            @RequestParam Integer productId,
            @RequestParam int quantity,
            HttpSession session,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        String message =
                vendingMachineService.replenishStock(
                        productId,
                        quantity);

        model.addAttribute("message", message);

        addAdminData(model);

        return "admin";
    }

    @PostMapping("/admin/product/delete")
    public String deleteProduct(
            @RequestParam Integer productId,
            HttpSession session,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        String message =
                vendingMachineService.deleteProduct(productId);

        model.addAttribute("message", message);

        addAdminData(model);

        return "admin";
    }

    // ==============================
    // Money management
    // ==============================

    @PostMapping("/admin/money/replenish")
    public String replenishMoney(
            @RequestParam Integer moneyType,
            @RequestParam int count,
            HttpSession session,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        String message =
                vendingMachineService.replenishMoney(
                        moneyType,
                        count);

        model.addAttribute("message", message);

        addAdminData(model);

        return "admin";
    }

    @PostMapping("/admin/money/availability")
    public String updateMoneyAvailability(
            @RequestParam Integer moneyType,
            @RequestParam boolean available,
            HttpSession session,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        String message =
                vendingMachineService.updateMoneyAvailability(
                        moneyType,
                        available);

        model.addAttribute("message", message);

        addAdminData(model);

        return "admin";
    }

    // ==============================
    // Helper methods
    // ==============================

    private boolean isAdminLoggedIn(
            HttpSession session) {

        Boolean loggedIn =
                (Boolean) session.getAttribute(
                        "adminLoggedIn");

        return loggedIn != null && loggedIn;
    }

    private void addAdminData(Model model) {

        model.addAttribute(
                "products",
                vendingMachineService.getProducts());

        model.addAttribute(
                "moneyStocks",
                vendingMachineService.getMoneyStocks());

        model.addAttribute(
                "orders",
                vendingMachineService.getOrders());

        model.addAttribute(
                "sales",
                vendingMachineService.getSales());

        model.addAttribute(
                "totalSales",
                vendingMachineService.getTotalSales());

        model.addAttribute(
                "productCount",
                vendingMachineService.getProductCount());

        model.addAttribute(
                "orderCount",
                vendingMachineService.getOrderCount());

        model.addAttribute(
                "soldOutCount",
                vendingMachineService.getSoldOutCount());
    }
}