package com.lapstore.web.shop;

import jakarta.servlet.http.HttpSession;

import com.lapstore.common.exception.BusinessException;
import com.lapstore.sales.dto.CartView;
import com.lapstore.sales.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SH-03 giỏ hàng. */
@Controller
@RequestMapping("/shop/cart")
class ShopCartController {
    private static final int MAX_QTY = 10;

    private final CartService carts;

    ShopCartController(CartService carts) { this.carts = carts; }

    @GetMapping
    String view(HttpSession session, Model model) {
        CartView cart = carts.view(CartSession.token(session));
        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());
        return "shop/cart";
    }

    @PostMapping("/items")
    String add(@RequestParam long variantId, @RequestParam(defaultValue = "1") int quantity,
               HttpSession session, RedirectAttributes ra) {
        if (quantity < 1 || quantity > MAX_QTY) {
            throw new BusinessException("Số lượng phải từ 1 đến " + MAX_QTY + ".");
        }
        carts.addItem(CartSession.token(session), variantId, quantity);
        ra.addFlashAttribute("success", "Đã thêm vào giỏ hàng.");
        return "redirect:/shop/cart";
    }

    @PostMapping("/items/{id}/delete")
    String remove(@PathVariable long id, HttpSession session, RedirectAttributes ra) {
        carts.removeItem(CartSession.token(session), id);
        ra.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ.");
        return "redirect:/shop/cart";
    }
}
