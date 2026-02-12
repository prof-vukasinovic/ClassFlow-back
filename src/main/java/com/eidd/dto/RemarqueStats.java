package com.eidd.dto;

import java.util.Map;

public record RemarqueStats(int total, Map<Long, Integer> byEleve, Map<Long, Integer> byClassRoom) {
}
