package com.example.kite.dto;


public record MarginResult(
        double totalMargin,
        double marginPerShare,
        double leverage,
        double var,
        double exposure
) {
}
