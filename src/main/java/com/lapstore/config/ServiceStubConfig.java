package com.lapstore.config;

import java.lang.reflect.Proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lapstore.aftersales.service.AftersalesService;
import com.lapstore.catalog.service.CatalogService;
import com.lapstore.catalog.service.PriceHistoryService;
import com.lapstore.chat.service.ChatService;
import com.lapstore.common.exception.ServiceNotReadyException;
import com.lapstore.inventory.service.StockHoldService;
import com.lapstore.reporting.service.ReportingService;
import com.lapstore.sales.service.CampaignService;
import com.lapstore.sales.service.CartService;
import com.lapstore.sales.service.CounterCheckoutService;
import com.lapstore.sales.service.OnlineOrderService;
import com.lapstore.sales.service.OrderQueryService;
import com.lapstore.sales.service.OrderService;
import com.lapstore.staff.service.StaffService;

@Configuration
class ServiceStubConfig {

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, m, args) -> {
            if (m.getDeclaringClass() == Object.class) {
                return switch (m.getName()) {
                    case "toString" -> type.getSimpleName() + "(stub)";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            throw new ServiceNotReadyException(type.getSimpleName() + "." + m.getName());
        });
    }

    @Bean
    @ConditionalOnMissingBean
    CatalogService catalogService() {
        return stub(CatalogService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    PriceHistoryService priceHistoryService() {
        return stub(PriceHistoryService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    CartService cartService() {
        return stub(CartService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    OnlineOrderService onlineOrderService() {
        return stub(OnlineOrderService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    OrderQueryService orderQueryService() {
        return stub(OrderQueryService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    OrderService orderService() {
        return stub(OrderService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    CounterCheckoutService counterCheckoutService() {
        return stub(CounterCheckoutService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    CampaignService campaignService() {
        return stub(CampaignService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    StockHoldService stockHoldService() {
        return stub(StockHoldService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    ChatService chatService() {
        return stub(ChatService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    AftersalesService aftersalesService() {
        return stub(AftersalesService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    StaffService staffService() {
        return stub(StaffService.class);
    }

    @Bean
    @ConditionalOnMissingBean
    ReportingService reportingService() {
        return stub(ReportingService.class);
    }
}
