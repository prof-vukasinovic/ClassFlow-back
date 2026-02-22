package com.eidd.dto;

import java.util.List;

public record GroupeUpdateRequest(List<Long> addEleveIds, List<Long> removeEleveIds, String nom) {
}
