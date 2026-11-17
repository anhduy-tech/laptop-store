package com.lapstore.sales.dto;

/** Một máy trong đơn quầy. price = listPrice - discount; secondsLeft = thời gian giữ máy còn lại. */
public record CounterLine(String serial, String variantName, long listPrice, long discount, long price, long secondsLeft) {}
