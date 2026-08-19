package com.example.kite.dto;


import java.util.List;


@lombok.Data
public class MarginResponse {
    private String status;
    private List<MarginData> data;
}
