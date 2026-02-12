package com.eidd.dto;

import java.util.List;

public record ClassRoomPlan(long classRoomId, String classRoomNom, List<TablePlanDto> tables) {
}
