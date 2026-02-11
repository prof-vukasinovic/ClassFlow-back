package com.eidd.dto;

import java.util.List;

import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;
import com.eidd.model.Table;

public record ClassRoomPlan(ClassRoom classRoom, List<Eleve> eleves, List<Table> tables) {
}
