package com.eidd.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.eidd.dto.RemarqueDto;
import com.eidd.dto.RemarqueRequest;
import com.eidd.dto.RemarqueStats;
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;

@Service
public class RemarqueService {
    private final Map<Long, RemarqueDto> remarques = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ClassRoomPlanService planService;

    public RemarqueService(ClassRoomPlanService planService) {
        this.planService = planService;
        seedSampleData();
    }

    public List<RemarqueDto> listAll() {
        return new ArrayList<>(remarques.values());
    }

    public RemarqueDto getById(long id) {
        return remarques.get(id);
    }

    public List<RemarqueDto> listByEleveId(long eleveId) {
        return remarques.values().stream()
            .filter(remarque -> remarque.eleveId() != null && remarque.eleveId() == eleveId)
            .toList();
    }

    public List<RemarqueDto> listByClassRoomId(long classRoomId) {
        return remarques.values().stream()
            .filter(remarque -> remarque.classRoomId() != null && remarque.classRoomId() == classRoomId)
            .toList();
    }

    public RemarqueDto create(RemarqueRequest request) {
        long id = idGenerator.getAndIncrement();
        Instant now = Instant.now();
        RemarqueDto created = new RemarqueDto(id, normalizeIntitule(request.intitule()), request.eleveId(), request.classRoomId(), now);
        remarques.put(id, created);
        return created;
    }

    public RemarqueDto update(long id, RemarqueRequest request) {
        RemarqueDto existing = remarques.get(id);
        if (existing == null) {
            return null;
        }

        String intitule = request.intitule() == null ? existing.intitule() : normalizeIntitule(request.intitule());
        Long eleveId = request.eleveId() == null ? existing.eleveId() : request.eleveId();
        Long classRoomId = request.classRoomId() == null ? existing.classRoomId() : request.classRoomId();

        RemarqueDto updated = new RemarqueDto(id, intitule, eleveId, classRoomId, existing.createdAt());
        remarques.put(id, updated);
        return updated;
    }

    public boolean delete(long id) {
        return remarques.remove(id) != null;
    }

    public RemarqueStats stats() {
        Map<Long, Integer> byEleve = new LinkedHashMap<>();
        Map<Long, Integer> byClassRoom = new LinkedHashMap<>();

        for (RemarqueDto remarque : remarques.values()) {
            if (remarque.eleveId() != null) {
                byEleve.merge(remarque.eleveId(), 1, Integer::sum);
            }
            if (remarque.classRoomId() != null) {
                byClassRoom.merge(remarque.classRoomId(), 1, Integer::sum);
            }
        }

        return new RemarqueStats(remarques.size(), byEleve, byClassRoom);
    }

    private String normalizeIntitule(String intitule) {
        return intitule == null ? null : intitule.trim();
    }

    private void seedSampleData() {
        List<ClassRoom> classRooms = planService.getClassRooms();
        if (classRooms.isEmpty()) {
            return;
        }

        for (ClassRoom classRoom : classRooms) {
            List<Eleve> eleves = classRoom.getEleves().getEleves();
            addSeedRemarque(classRoom.getId(), eleves, 0, "Participation active");
            addSeedRemarque(classRoom.getId(), eleves, 1, "Bon travail");
        }
    }

    private void addSeedRemarque(long classRoomId, List<Eleve> eleves, int index, String intitule) {
        if (eleves.size() > index) {
            Eleve eleve = eleves.get(index);
            create(new RemarqueRequest(intitule, eleve.getId(), classRoomId));
        }
    }
}
