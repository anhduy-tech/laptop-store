package com.lapstore.web.warehouse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** WH-00 menu kho & khuyến mãi. */
@Controller
class WarehouseMenuController {

    @GetMapping({"/staff/warehouse", "/staff/warehouse/"})
    String index() {
        return "warehouse/index";
    }
}
