package com.eidd.dto;

import java.util.List;

public record GroupeCreateRequest(List<List<Long>> groupes, List<String> noms) {
}
