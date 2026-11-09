package com.lapstore.catalog.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Khởi động: bảo đảm có nhóm/định nghĩa CPU, GPU, RAM, ổ cứng trong attribute_* và nạp giá trị từ biến thể hiện có. */
@Component
class SpecBootstrap implements ApplicationRunner {
    private final SpecService specs;

    SpecBootstrap(SpecService specs) {
        this.specs = specs;
    }

    @Override
    public void run(ApplicationArguments args) {
        specs.initialize();
    }
}
